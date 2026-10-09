package it.scuola.zaino;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Checklist to pack the school bag for the next school day. */
public class MainActivity extends Activity {

    public static final String EXTRA_DATE = "date";

    private static final int ACCENT = Color.rgb(0x2E, 0x6F, 0xD8);
    private static final int OK_GREEN = Color.rgb(0x1E, 0x8E, 0x3E);

    private LocalDate shown;
    private TextView title;
    private TextView hours;
    private TextView progress;
    private LinearLayout checklist;
    private Button prevButton;
    private Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Zaino 1A");
        Store.cleanupOldChecks(this);
        Notifier.ensureChannel(this);
        ReminderScheduler.schedule(this);
        if (Build.VERSION.SDK_INT >= 33 && !Notifier.permissionGranted(this)) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        setContentView(buildUi());
        shown = dateFrom(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        shown = dateFrom(intent);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The timetable may have been edited in the settings.
        if (shown == null || Store.lessons(this, shown.getDayOfWeek()).isEmpty()) {
            shown = Store.nextSchoolDay(this, LocalDate.now());
        }
        render();
    }

    private LocalDate dateFrom(Intent intent) {
        String s = intent == null ? null : intent.getStringExtra(EXTRA_DATE);
        if (s != null) {
            try {
                return LocalDate.parse(s);
            } catch (RuntimeException ignored) {
                // fall through to the default
            }
        }
        return Store.nextSchoolDay(this, LocalDate.now());
    }

    // ---------------------------------------------------------------- UI

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(24));
        scroll.addView(root);

        // Day selector: ◀  Martedì 13 ottobre  ▶
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        prevButton = new Button(this);
        prevButton.setText("◀");
        prevButton.setContentDescription("Giorno di scuola precedente");
        prevButton.setOnClickListener(v -> move(-1));
        nextButton = new Button(this);
        nextButton.setText("▶");
        nextButton.setContentDescription("Giorno di scuola successivo");
        nextButton.setOnClickListener(v -> move(1));
        title = new TextView(this);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        header.addView(prevButton, new LinearLayout.LayoutParams(dp(56), LinearLayout.LayoutParams.WRAP_CONTENT));
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(nextButton, new LinearLayout.LayoutParams(dp(56), LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(header);

        // Lesson order for the day
        hours = new TextView(this);
        hours.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        hours.setLineSpacing(dp(4), 1f);
        hours.setPadding(dp(16), dp(12), dp(16), dp(12));
        GradientDrawable box = new GradientDrawable();
        box.setCornerRadius(dp(12));
        box.setColor(Color.argb(0x18, 0x2E, 0x6F, 0xD8));
        hours.setBackground(box);
        LinearLayout.LayoutParams hoursLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hoursLp.topMargin = dp(12);
        root.addView(hours, hoursLp);

        TextView checklistTitle = new TextView(this);
        checklistTitle.setText("Prepara lo zaino");
        checklistTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        checklistTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams ctLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ctLp.topMargin = dp(20);
        root.addView(checklistTitle, ctLp);

        progress = new TextView(this);
        progress.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        root.addView(progress);

        checklist = new LinearLayout(this);
        checklist.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clLp.topMargin = dp(8);
        root.addView(checklist, clLp);

        // Bottom buttons
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button reset = new Button(this);
        reset.setText("Azzera");
        reset.setOnClickListener(v -> {
            if (shown != null) {
                Store.clearChecked(this, shown);
                render();
            }
        });
        Button settings = new Button(this);
        settings.setText("Impostazioni");
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        buttons.addView(reset, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        buttons.addView(settings, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bLp.topMargin = dp(24);
        root.addView(buttons, bLp);

        return scroll;
    }

    private void move(int direction) {
        if (shown == null) return;
        LocalDate d = direction > 0
                ? Store.nextSchoolDay(this, shown)
                : Store.previousSchoolDay(this, shown);
        if (d != null) {
            shown = d;
            render();
        }
    }

    private void render() {
        if (title == null) return;
        checklist.removeAllViews();
        if (shown == null) {
            title.setText("Nessuna lezione");
            hours.setText("L'orario è vuoto. Aprilo in Impostazioni per inserire le materie.");
            progress.setText("");
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
            return;
        }
        prevButton.setEnabled(true);
        nextButton.setEnabled(true);

        LocalDate today = LocalDate.now();
        String prefix = shown.equals(today.plusDays(1)) ? "Domani\n"
                : shown.equals(today) ? "Oggi\n" : "";
        title.setText(prefix + Store.dateLabel(shown));

        List<String> lessons = Store.lessons(this, shown.getDayOfWeek());
        hours.setText(Store.hoursText(lessons));

        List<String> items = Store.checklistItems(this, shown);
        Set<String> done = Store.checked(this, shown);
        for (String item : items) {
            CheckBox cb = new CheckBox(this);
            cb.setText(item);
            cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            cb.setPadding(dp(8), dp(10), dp(8), dp(10));
            cb.setChecked(done.contains(item));
            cb.setOnCheckedChangeListener((button, isChecked) -> {
                Store.setChecked(this, shown, item, isChecked);
                updateProgress(items);
            });
            checklist.addView(cb);
        }
        updateProgress(items);
    }

    private void updateProgress(List<String> items) {
        Set<String> done = Store.checked(this, shown);
        int count = 0;
        for (String item : items) if (done.contains(item)) count++;
        if (!items.isEmpty() && count == items.size()) {
            progress.setText("Zaino pronto! ✅");
            progress.setTextColor(OK_GREEN);
        } else {
            progress.setText(count + " di " + items.size() + " nello zaino");
            progress.setTextColor(ACCENT);
        }
    }
}
