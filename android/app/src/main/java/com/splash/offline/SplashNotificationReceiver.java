package com.splash.offline;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class SplashNotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "splash_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            SplashNotificationScheduler.scheduleNext(context);
            return;
        }

        if (SplashNotificationScheduler.ACTION_RANDOM_PING.equals(action)) {
            ensureChannel(context);
            if (hasPermission(context) && SplashNotificationScheduler.isNowAllowed(context)) {
                String[] msg = SplashNotificationScheduler.pickRandomNotification(context);
                Intent openIntent = new Intent(context, MainActivity.class);
                openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    flags |= PendingIntent.FLAG_IMMUTABLE;
                }
                PendingIntent pi = PendingIntent.getActivity(context, 4041, openIntent, flags);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(msg[0])
                    .setContentText(msg[1])
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(msg[1]))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pi);

                try {
                    int notifId = (int) (System.currentTimeMillis() % 100000);
                    NotificationManagerCompat.from(context).notify(notifId, builder.build());
                } catch (SecurityException ignored) {
                }
            }
            SplashNotificationScheduler.scheduleNext(context);
        }
    }

    private static boolean hasPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED;
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Recordatorios Splash",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Avisos rápidos, retos y rescate anti-scroll de Splash");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
