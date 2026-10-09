package it.scuola.zaino;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Timetable, settings and checklist state, all kept in SharedPreferences. */
public final class Store {

    private static final String PREFS = "zaino";
    private static final String KEY_HOUR = "reminder_hour";
    private static final String KEY_MINUTE = "reminder_minute";
    private static final String KEY_ENABLED = "reminder_enabled";
    private static final String KEY_ALWAYS = "always_items";
    private static final String KEY_DAY_PREFIX = "day_";
    private static final String KEY_CHECK_PREFIX = "chk_";

    public static final int DEFAULT_HOUR = 19;
    public static final int DEFAULT_MINUTE = 0;
    private static final String DEFAULT_ALWAYS = "Diario\nAstuccio";

    /** Orario classe 1A, Liceo Scientifico, in vigore dal 5 ottobre 2026. Index 0 = lunedì. */
    private static final String[][] DEFAULT_1A = {
            {"Ita/Lat", "Ita/Lat", "Mat/Fis", "Storia e Geografia"},
            {"Inglese", "Ita/Lat", "Ita/Lat", "Mat/Fis", "Disegno e St. Arte"},
            {"Ita/Lat", "Ita/Lat", "Storia e Geografia", "Inglese"},
            {"Inglese", "Mat/Fis", "Mat/Fis", "Sc. Motorie", "Storia e Geografia"},
            {"Disegno e St. Arte", "Mat/Fis", "Ita/Lat", "Religione", "Scienze"},
            {"Mat/Fis", "Mat/Fis", "Scienze", "Sc. Motorie"},
            {}
    };

    private static final DateTimeFormatter LABEL =
            DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN);

    private Store() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------- timetable

    public static String defaultLessonsText(DayOfWeek day) {
        return String.join("\n", DEFAULT_1A[day.getValue() - 1]);
    }

    public static String lessonsText(Context ctx, DayOfWeek day) {
        return prefs(ctx).getString(KEY_DAY_PREFIX + day.getValue(), defaultLessonsText(day));
    }

    public static void setLessonsText(Context ctx, DayOfWeek day, String text) {
        prefs(ctx).edit().putString(KEY_DAY_PREFIX + day.getValue(), text).apply();
    }

    public static void resetTimetable(Context ctx) {
        SharedPreferences.Editor e = prefs(ctx).edit();
        for (DayOfWeek d : DayOfWeek.values()) {
            e.remove(KEY_DAY_PREFIX + d.getValue());
        }
        e.apply();
    }

    /** Lessons of a weekday in hour order (one entry per hour). Empty if no school. */
    public static List<String> lessons(Context ctx, DayOfWeek day) {
        return splitLines(lessonsText(ctx, day));
    }

    /** Each subject once, in the order it first appears in the day. */
    public static List<String> uniqueSubjects(List<String> lessons) {
        return new ArrayList<>(new LinkedHashSet<>(lessons));
    }

    public static LocalDate nextSchoolDay(Context ctx, LocalDate after) {
        for (int i = 1; i <= 7; i++) {
            LocalDate d = after.plusDays(i);
            if (!lessons(ctx, d.getDayOfWeek()).isEmpty()) return d;
        }
        return null;
    }

    public static LocalDate previousSchoolDay(Context ctx, LocalDate before) {
        for (int i = 1; i <= 7; i++) {
            LocalDate d = before.minusDays(i);
            if (!lessons(ctx, d.getDayOfWeek()).isEmpty()) return d;
        }
        return null;
    }

    // ---------------------------------------------------------------- reminder settings

    public static int reminderHour(Context ctx) {
        return prefs(ctx).getInt(KEY_HOUR, DEFAULT_HOUR);
    }

    public static int reminderMinute(Context ctx) {
        return prefs(ctx).getInt(KEY_MINUTE, DEFAULT_MINUTE);
    }

    public static void setReminderTime(Context ctx, int hour, int minute) {
        prefs(ctx).edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply();
    }

    public static boolean reminderEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_ENABLED, true);
    }

    public static void setReminderEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    // ---------------------------------------------------------------- checklist

    public static String alwaysItemsText(Context ctx) {
        return prefs(ctx).getString(KEY_ALWAYS, DEFAULT_ALWAYS);
    }

    public static void setAlwaysItemsText(Context ctx, String text) {
        prefs(ctx).edit().putString(KEY_ALWAYS, text).apply();
    }

    /** Subjects of the day (each once) followed by the things to bring every day. */
    public static List<String> checklistItems(Context ctx, LocalDate date) {
        LinkedHashSet<String> items =
                new LinkedHashSet<>(uniqueSubjects(lessons(ctx, date.getDayOfWeek())));
        items.addAll(splitLines(alwaysItemsText(ctx)));
        return new ArrayList<>(items);
    }

    public static Set<String> checked(Context ctx, LocalDate date) {
        return new HashSet<>(prefs(ctx).getStringSet(KEY_CHECK_PREFIX + date, Collections.<String>emptySet()));
    }

    public static void setChecked(Context ctx, LocalDate date, String item, boolean isChecked) {
        Set<String> set = checked(ctx, date);
        if (isChecked) set.add(item); else set.remove(item);
        prefs(ctx).edit().putStringSet(KEY_CHECK_PREFIX + date, set).apply();
    }

    public static void clearChecked(Context ctx, LocalDate date) {
        prefs(ctx).edit().remove(KEY_CHECK_PREFIX + date).apply();
    }

    /** Forget ticks for days that are already over. */
    public static void cleanupOldChecks(Context ctx) {
        LocalDate today = LocalDate.now();
        SharedPreferences.Editor e = prefs(ctx).edit();
        for (Map.Entry<String, ?> entry : prefs(ctx).getAll().entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(KEY_CHECK_PREFIX)) continue;
            try {
                LocalDate d = LocalDate.parse(key.substring(KEY_CHECK_PREFIX.length()));
                if (d.isBefore(today)) e.remove(key);
            } catch (RuntimeException ignored) {
                e.remove(key);
            }
        }
        e.apply();
    }

    // ---------------------------------------------------------------- text helpers

    /** "Martedì 13 ottobre" */
    public static String dateLabel(LocalDate date) {
        String s = LABEL.format(date);
        return s.substring(0, 1).toUpperCase(Locale.ITALIAN) + s.substring(1);
    }

    /** "1ª ora  Inglese\n2ª ora  Ita/Lat..." */
    public static String hoursText(List<String> lessons) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lessons.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(i + 1).append("ª ora   ").append(lessons.get(i));
        }
        return sb.toString();
    }

    private static List<String> splitLines(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String line : text.split("\n")) {
            String t = line.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}
