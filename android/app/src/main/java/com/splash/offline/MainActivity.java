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
import android.util.Base64;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
import java.io.File;
import java.io.FileOutputStream;
import org.json.JSONObject;

public class MainActivity extends BridgeActivity {
    private static final String CHANNEL_ID = "splash_reminders";
    private static final int REQ_POST_NOTIFICATIONS = 1001;
    private int nextNotifId = 200;
    private volatile int pendingTimerMinutes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        enableHighRefreshRate();
        createNotificationChannel();
        requestNotificationPermissionIfNeeded();
        SplashNotificationScheduler.scheduleNext(this);
        handleTimerIntent(getIntent(), false);

        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDark = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        applySystemBarsAppearance(isDark, isDark ? "#000000" : "#FAF8F3");

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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleTimerIntent(intent, true);
    }

    private void handleTimerIntent(Intent intent, boolean dispatchImmediately) {
        if (intent == null) return;
        int mins = intent.getIntExtra("splash_timer_min", 0);
        if (mins <= 0) return;
        pendingTimerMinutes = mins;
        intent.removeExtra("splash_timer_min");

        if (dispatchImmediately && getBridge() != null && getBridge().getWebView() != null) {
            final int targetMins = mins;
            WebView webView = getBridge().getWebView();
            webView.post(() -> {
                String js = "window.dispatchEvent(new CustomEvent('splash-widget-timer',{detail:{minutes:" + targetMins + "}}));";
                webView.evaluateJavascript(js, null);
                pendingTimerMinutes = 0;
            });
        }
    }

    @SuppressWarnings("deprecation")
    private void enableHighRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        Window window = getWindow();
        if (window == null) return;
        try {
            Display display = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display = getDisplay();
            }
            if (display == null) {
                display = getWindowManager().getDefaultDisplay();
            }
            if (display == null) return;

            Display.Mode currentMode = display.getMode();
            Display.Mode[] modes = display.getSupportedModes();
            Display.Mode bestMode = currentMode;
            for (Display.Mode mode : modes) {
                if (mode.getPhysicalWidth() == currentMode.getPhysicalWidth()
                        && mode.getPhysicalHeight() == currentMode.getPhysicalHeight()
                        && mode.getRefreshRate() > bestMode.getRefreshRate()) {
                    bestMode = mode;
                }
            }
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.preferredDisplayModeId = bestMode.getModeId();
            lp.preferredRefreshRate = bestMode.getRefreshRate();
            window.setAttributes(lp);
        } catch (Throwable ignored) {
        }
    }

    private void applySystemBarsAppearance(boolean isDark, String bgHex) {
        int barBgColor;
        try {
            barBgColor = Color.parseColor(bgHex != null && !bgHex.isEmpty() ? bgHex : (isDark ? "#000000" : "#FAF8F3"));
        } catch (IllegalArgumentException e) {
            barBgColor = isDark ? Color.parseColor("#000000") : Color.parseColor("#FAF8F3");
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

    private void refreshAllWidgets() {
        AppWidgetManager mgr = AppWidgetManager.getInstance(this);
        int[] ids4x2 = mgr.getAppWidgetIds(new ComponentName(this, SplashWidgetProvider.class));
        if (ids4x2 != null && ids4x2.length > 0) {
            SplashWidgetProvider.updateAllWidgets(this, mgr, ids4x2);
        }
        int[] ids2x2 = mgr.getAppWidgetIds(new ComponentName(this, SplashBiomeWidgetProvider.class));
        if (ids2x2 != null && ids2x2.length > 0) {
            SplashBiomeWidgetProvider.updateAllWidgets(this, mgr, ids2x2);
        }
        int[] idsQuick = mgr.getAppWidgetIds(new ComponentName(this, SplashQuickWidgetProvider.class));
        if (idsQuick != null && idsQuick.length > 0) {
            SplashQuickWidgetProvider.updateAllWidgets(this, mgr, idsQuick);
        }
    }

    private class SplashNativeBridge {
        @JavascriptInterface
        public int consumePendingTimerMinutes() {
            int mins = pendingTimerMinutes;
            pendingTimerMinutes = 0;
            return mins;
        }

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
        public void syncNotificationSchedule(String jsonStr) {
            SplashNotificationScheduler.savePayload(MainActivity.this, jsonStr);
            SplashNotificationScheduler.scheduleNext(MainActivity.this);
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
                .commit();
            runOnUiThread(MainActivity.this::refreshAllWidgets);
        }

        @JavascriptInterface
        public void updateWidgetRich(String jsonStr) {
            if (jsonStr == null || jsonStr.isEmpty()) return;
            try {
                JSONObject obj = new JSONObject(jsonStr);
                String b64 = obj.optString("biomePngBase64", "");
                if (!b64.isEmpty()) {
                    byte[] pngBytes = Base64.decode(b64, Base64.DEFAULT);
                    File outFile = new File(getFilesDir(), "widget_biome.png");
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        fos.write(pngBytes);
                        fos.flush();
                    }
                }
                SharedPreferences prefs = getSharedPreferences("splash_widget", Context.MODE_PRIVATE);
                prefs.edit()
                    .putString("biomeName", obj.optString("biomeName", "Valle Alpino"))
                    .putString("biomeShort", obj.optString("biomeShort", "Valle"))
                    .putString("levelLabel", obj.optString("levelLabel", "Nv. 1"))
                    .putString("streakLabel", obj.optString("streakLabel", "0 días"))
                    .putString("goal2Title", obj.optString("goal2Title", "Respirar aire fresco"))
                    .putString("goal15Title", obj.optString("goal15Title", "Caminar cuesta arriba"))
                    .putString("weatherLabel", obj.optString("weatherLabel", "Radiante"))
                    .putBoolean("isDark", obj.optBoolean("isDark", false))
                    .putString("themeMode", obj.optString("themeMode", "auto"))
                    .putBoolean("isEn", obj.optBoolean("isEn", false))
                    .commit();
                runOnUiThread(MainActivity.this::refreshAllWidgets);
            } catch (Throwable ignored) {
            }
        }
    }
}
