package it.scuola.zaino;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDate;

/** Fires every evening at the chosen time; notifies only if tomorrow is a school day. */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (!Store.lessons(ctx, tomorrow.getDayOfWeek()).isEmpty()) {
            Notifier.show(ctx, tomorrow);
        }
        ReminderScheduler.schedule(ctx);
    }
}
