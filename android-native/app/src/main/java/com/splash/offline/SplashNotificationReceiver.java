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
import com.splash.offline.audio.SplashAudioManager;

public class SplashNotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "splash_reminders";
    public static final String EXTRA_FORCE_TEST = "force_test";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            SplashNotificationScheduler.scheduleNext(context, true);
            return;
        }

        if (SplashNotificationScheduler.ACTION_TIMER_DONE.equals(action)) {
            ensureChannel(context);
            String token = intent.getStringExtra(SplashNotificationScheduler.EXTRA_TIMER_TOKEN);
            SplashNotificationScheduler.markTimerChimePlayed(context, token);
            new SplashAudioManager(context.getApplicationContext()).playGentleCompletionChime();

            if (hasPermission(context)) {
                String title = intent.getStringExtra(SplashNotificationScheduler.EXTRA_TIMER_TITLE);
                String body = intent.getStringExtra(SplashNotificationScheduler.EXTRA_TIMER_BODY);
                if (title == null || title.isEmpty()) title = "Temporizador completado";
                if (body == null || body.isEmpty()) body = "Tiempo sin pantalla terminado.";

                Intent openIntent = new Intent(context, MainActivity.class);
                openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    flags |= PendingIntent.FLAG_IMMUTABLE;
                }
                PendingIntent pi = PendingIntent.getActivity(context, 4056, openIntent, flags);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notif_splash)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pi);

                try {
                    NotificationManagerCompat.from(context).notify(4056, builder.build());
                } catch (SecurityException ignored) {
                }
            }
            return;
        }

        if (SplashNotificationScheduler.ACTION_RANDOM_PING.equals(action)) {
            ensureChannel(context);
            boolean forceTest = intent.getBooleanExtra(EXTRA_FORCE_TEST, false);
            if (hasPermission(context) && (forceTest || SplashNotificationScheduler.isNowAllowed(context))) {
                SplashNotificationScheduler.PickedNotification picked =
                    SplashNotificationScheduler.pickRandomNotificationGoal(context);
                SplashNotificationScheduler.savePendingNotificationGoal(context, picked.goalId);

                Intent openIntent = new Intent(context, MainActivity.class);
                openIntent.setAction("com.splash.offline.OPEN_NOTIF_GOAL");
                openIntent.putExtra(SplashNotificationScheduler.EXTRA_NOTIF_GOAL_ID, picked.goalId);
                openIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                );
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    flags |= PendingIntent.FLAG_IMMUTABLE;
                }
                PendingIntent pi = PendingIntent.getActivity(context, 4041, openIntent, flags);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notif_splash)
                    .setContentTitle(picked.title)
                    .setContentText(picked.body)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(picked.body))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pi);

                try {
                    int notifId = (int) (System.currentTimeMillis() % 100000);
                    NotificationManagerCompat.from(context).notify(notifId, builder.build());
                } catch (SecurityException ignored) {
                }
            }
            if (!forceTest) {
                SplashNotificationScheduler.scheduleNext(context, true);
            }
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
            channel.setDescription("Avisos Flash, retos y enfoque de Splash");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
