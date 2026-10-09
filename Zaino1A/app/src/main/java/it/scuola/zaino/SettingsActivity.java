package it.scuola.zaino;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Reminder time, timetable editor and "always bring" items. */
public class SettingsActivity extends Activity {

    private static final String[] DAY_NAMES =
            {"Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato"};

    private final Map<DayOfWeek, EditText> dayFields = new EnumMap<>(DayOfWeek.class);
    private EditText alwaysField;
    private Button timeButton;
    private TextView exactWarning;
    private Button exactButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Impostazioni");
        setContentView(buildUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean needExact = !ReminderScheduler.exactAllowed(this);
        exactWarning.setVisibility(needExact ? View.VISIBLE : View.GONE);
        exactButton.setVisibility(needExact ? View.VISIBLE : View.GONE);
        ReminderScheduler.schedule(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
    }

    private void save() {
        for (Map.Entry<DayOfWeek, EditText> e : dayFields.entrySet()) {
            Store.setLessonsText(this, e.getKey(), e.getValue().getText().toString());
        }
        Store.setAlwaysItemsText(this, alwaysField.getText().toString());
        ReminderScheduler.schedule(this);
    }

    // ---------------------------------------------------------------- UI

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }

    private TextView heading(LinearLayout parent, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(24);
        parent.addView(t, lp);
        return t;
    }

    private TextView note(LinearLayout parent, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        parent.addView(t);
        return t;
    }

    private EditText multiline(String text) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setSingleLine(false);
        e.setGravity(Gravity.TOP | Gravity.START);
        e.setMinLines(2);
        e.setText(text);
        return e;
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(8), dp(16), dp(32));
        scroll.addView(root);

        // --- Reminder
        heading(root, "Promemoria serale");
        Switch enabled = new Switch(this);
        enabled.setText("Avvisami ogni sera con le materie del giorno dopo");
        enabled.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        enabled.setChecked(Store.reminderEnabled(this));
        enabled.setOnCheckedChangeListener((b, on) -> {
            Store.setReminderEnabled(this, on);
            ReminderScheduler.schedule(this);
        });
        root.addView(enabled);

        timeButton = new Button(this);
        updateTimeButton();
        timeButton.setOnClickListener(v -> new TimePickerDialog(this, (picker, h, m) -> {
            Store.setReminderTime(this, h, m);
            ReminderScheduler.schedule(this);
            updateTimeButton();
        }, Store.reminderHour(this), Store.reminderMinute(this), true).show());
        root.addView(timeButton);

        exactWarning = note(root, "Per ricevere l'avviso all'orario preciso, consenti \"Sveglie e promemoria\" "
                + "per questa app. Senza permesso l'avviso può arrivare con qualche minuto di ritardo.");
        exactButton = new Button(this);
        exactButton.setText("Consenti sveglie e promemoria");
        exactButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 31) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            }
        });
        root.addView(exactButton);

        Button test = new Button(this);
        test.setText("Invia una notifica di prova");
        test.setOnClickListener(v -> {
            save();
            LocalDate next = Store.nextSchoolDay(this, LocalDate.now());
            if (next == null) {
                Toast.makeText(this, "L'orario è vuoto", Toast.LENGTH_SHORT).show();
            } else if (!Notifier.show(this, next)) {
                Toast.makeText(this, "Le notifiche sono disattivate: attivale nelle impostazioni del telefono",
                        Toast.LENGTH_LONG).show();
            }
        });
        root.addView(test);

        // --- Timetable
        heading(root, "Orario delle lezioni");
        note(root, "Una materia per riga, nell'ordine delle ore. Lascia vuoto un giorno senza scuola.");
        for (int i = 0; i < DAY_NAMES.length; i++) {
            DayOfWeek day = DayOfWeek.of(i + 1);
            TextView label = new TextView(this);
            label.setText(DAY_NAMES[i]);
            label.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(12);
            root.addView(label, lp);
            EditText field = multiline(Store.lessonsText(this, day));
            dayFields.put(day, field);
            root.addView(field);
        }
        Button reset = new Button(this);
        reset.setText("Ripristina orario 1A");
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setMessage("Rimettere l'orario originale della 1A?")
                .setNegativeButton("Annulla", null)
                .setPositiveButton("Ripristina", (d, w) -> {
                    Store.resetTimetable(this);
                    for (Map.Entry<DayOfWeek, EditText> e : dayFields.entrySet()) {
                        e.getValue().setText(Store.defaultLessonsText(e.getKey()));
                    }
                    ReminderScheduler.schedule(this);
                })
                .show());
        root.addView(reset);

        // --- Always bring
        heading(root, "Da portare sempre");
        note(root, "Aggiunti alla checklist ogni giorno, uno per riga.");
        alwaysField = multiline(Store.alwaysItemsText(this));
        root.addView(alwaysField);

        return scroll;
    }

    private void updateTimeButton() {
        timeButton.setText(String.format(Locale.ITALIAN, "Orario dell'avviso: %02d:%02d",
                Store.reminderHour(this), Store.reminderMinute(this)));
    }
}
