package it.scuola.zaino;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarms are lost on reboot, app update or clock change: set the next one again. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        ReminderScheduler.schedule(ctx);
    }
}
