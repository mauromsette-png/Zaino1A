package it.scuola.zaino;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.ZonedDateTime;

/** Keeps exactly one alarm pending: the next occurrence of the reminder time. */
public final class ReminderScheduler {

    private ReminderScheduler() {}

    private static PendingIntent alarmIntent(Context ctx) {
        Intent i = new Intent(ctx, ReminderReceiver.class);
        return PendingIntent.getBroadcast(ctx, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** True when Android lets the app ring at the exact minute (always true below Android 12). */
    public static boolean exactAllowed(Context ctx) {
        if (Build.VERSION.SDK_INT < 31) return true;
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        return am != null && am.canScheduleExactAlarms();
    }

    public static void schedule(Context ctx) {
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        if (am == null) return;
        PendingIntent pi = alarmIntent(ctx);
        am.cancel(pi);
        if (!Store.reminderEnabled(ctx)) return;

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = now
                .withHour(Store.reminderHour(ctx))
                .withMinute(Store.reminderMinute(ctx))
                .withSecond(0)
                .withNano(0);
        if (!next.isAfter(now)) next = next.plusDays(1);
        long when = next.toInstant().toEpochMilli();

        try {
            if (exactAllowed(ctx)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } else {
                // Without the "alarms & reminders" permission Android may deliver it a few minutes late.
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }
}
