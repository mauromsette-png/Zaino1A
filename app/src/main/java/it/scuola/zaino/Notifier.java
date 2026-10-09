package it.scuola.zaino;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public final class Notifier {

    private static final String CHANNEL_ID = "promemoria";
    private static final int NOTIFICATION_ID = 1;

    private Notifier() {}

    public static void ensureChannel(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Promemoria zaino", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Ogni sera: le materie del giorno dopo");
        nm.createNotificationChannel(ch);
    }

    public static boolean permissionGranted(Context ctx) {
        return Build.VERSION.SDK_INT < 33
                || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED;
    }

    /** Shows the subjects of {@code date}. Returns false if notifications are not allowed. */
    public static boolean show(Context ctx, LocalDate date) {
        ensureChannel(ctx);
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null || !permissionGranted(ctx)) return false;

        List<String> lessons = Store.lessons(ctx, date.getDayOfWeek());
        String subjects = String.join(", ", Store.uniqueSubjects(lessons));
        boolean isTomorrow = date.equals(LocalDate.now().plusDays(1));
        String title = isTomorrow
                ? "Domani, " + Store.dateLabel(date).toLowerCase(Locale.ITALIAN)
                : Store.dateLabel(date);

        Intent open = new Intent(ctx, MainActivity.class)
                .putExtra(MainActivity.EXTRA_DATE, date.toString())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(subjects)
                .setStyle(new Notification.BigTextStyle().bigText(
                        Store.hoursText(lessons) + "\n\nTocca per preparare lo zaino."))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();
        nm.notify(NOTIFICATION_ID, n);
        return true;
    }
}
