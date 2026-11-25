package com.splash.offline;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private static final String CHANNEL_ID = "splash_reminders";
    private static final int REQ_POST_NOTIFICATIONS = 1001;
    private int nextNotifId = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createNotificationChannel();
        requestNotificationPermissionIfNeeded();

        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDark = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        applySystemBarsAppearance(isDark, isDark ? "#111214" : "#FAF8F3");

        View content = findViewById(android.R.id.content);
        if (content != null) {
            ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
                Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
                );
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
            ViewCompat.requestApplyInsets(content);
        }

        if (getBridge() != null && getBridge().getWebView() != null) {
            WebView webView = getBridge().getWebView();
            webView.addJavascriptInterface(new SplashNativeBridge(), "SplashNative");
        }
    }

    private void applySystemBarsAppearance(boolean isDark, String bgHex) {
        int barBgColor;
        try {
            barBgColor = Color.parseColor(bgHex != null && !bgHex.isEmpty() ? bgHex : (isDark ? "#111214" : "#FAF8F3"));
        } catch (IllegalArgumentException e) {
            barBgColor = isDark ? Color.parseColor("#111214") : Color.parseColor("#FAF8F3");
        }

        Window window = getWindow();
        if (window != null) {
            window.getDecorView().setBackgroundColor(barBgColor);
            window.setStatusBarColor(barBgColor);
            window.setNavigationBarColor(barBgColor);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.setNavigationBarContrastEnforced(false);
            }
            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
            controller.setAppearanceLightStatusBars(!isDark);
            controller.setAppearanceLightNavigationBars(!isDark);
        }

        View content = findViewById(android.R.id.content);
        if (content != null) {
            content.setBackgroundColor(barBgColor);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Recordatorios Splash",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Avisos rápidos, retos y rescate anti-scroll de Splash");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isNotificationPermissionGranted()) {
            ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQ_POST_NOTIFICATIONS
            );
        }
    }

    private class SplashNativeBridge {
        @JavascriptInterface
        public void setSystemBarsTheme(boolean isDark, String bgHex) {
            runOnUiThread(() -> applySystemBarsAppearance(isDark, bgHex));
        }

        @JavascriptInterface
        public boolean hasNotificationPermission() {
            return isNotificationPermissionGranted();
        }

        @JavascriptInterface
        public void requestNotificationPermission() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_POST_NOTIFICATIONS
                    );
                }
            });
        }

        @JavascriptInterface
        public void sendNotification(String title, String body) {
            runOnUiThread(() -> {
                if (!isNotificationPermissionGranted()) {
                    requestNotificationPermissionIfNeeded();
                    return;
                }
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    flags |= PendingIntent.FLAG_IMMUTABLE;
                }
                PendingIntent pi = PendingIntent.getActivity(MainActivity.this, 0, intent, flags);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title != null && !title.isEmpty() ? title : "Splash")
                    .setContentText(body != null ? body : "")
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(body != null ? body : ""))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pi);

                try {
                    NotificationManagerCompat.from(MainActivity.this).notify(nextNotifId++, builder.build());
                } catch (SecurityException ignored) {
                }
            });
        }

        @JavascriptInterface
        public boolean pinWidget(String widgetType) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return false;
            }
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(MainActivity.this);
            if (!appWidgetManager.isRequestPinAppWidgetSupported()) {
                return false;
            }
            Class<?> targetProvider = SplashWidgetProvider.class;
            if ("2x2".equals(widgetType)) {
                targetProvider = SplashBiomeWidgetProvider.class;
            } else if ("lock".equals(widgetType)) {
                targetProvider = SplashQuickWidgetProvider.class;
            }
            ComponentName provider = new ComponentName(MainActivity.this, targetProvider);
            runOnUiThread(() -> appWidgetManager.requestPinAppWidget(provider, null, null));
            return true;
        }

        @JavascriptInterface
        public void updateWidget(String biomeName, String levelLabel, String streakLabel, String nextGoal) {
            SharedPreferences prefs = getSharedPreferences("splash_widget", Context.MODE_PRIVATE);
            prefs.edit()
                .putString("biomeName", biomeName != null ? biomeName : "Valle Alpino")
                .putString("levelLabel", levelLabel != null ? levelLabel : "Nv. 1")
                .putString("streakLabel", streakLabel != null ? streakLabel : "0 días")
                .putString("nextGoal", nextGoal != null ? nextGoal : "Respirar aire fresco")
                .apply();

            AppWidgetManager mgr = AppWidgetManager.getInstance(MainActivity.this);
            int[] ids4x2 = mgr.getAppWidgetIds(new ComponentName(MainActivity.this, SplashWidgetProvider.class));
            if (ids4x2 != null && ids4x2.length > 0) {
                SplashWidgetProvider.updateAllWidgets(MainActivity.this, mgr, ids4x2);
            }
            int[] ids2x2 = mgr.getAppWidgetIds(new ComponentName(MainActivity.this, SplashBiomeWidgetProvider.class));
            if (ids2x2 != null && ids2x2.length > 0) {
                SplashBiomeWidgetProvider.updateAllWidgets(MainActivity.this, mgr, ids2x2);
            }
            int[] idsQuick = mgr.getAppWidgetIds(new ComponentName(MainActivity.this, SplashQuickWidgetProvider.class));
            if (idsQuick != null && idsQuick.length > 0) {
                SplashQuickWidgetProvider.updateAllWidgets(MainActivity.this, mgr, idsQuick);
            }
        }
    }
}
