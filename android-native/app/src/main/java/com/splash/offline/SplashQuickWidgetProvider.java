package com.splash.offline;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.widget.RemoteViews;

public class SplashQuickWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds);
    }

    private static PendingIntent createTimerPendingIntent(Context context, int minutes, int reqCode, String action) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(action);
        intent.putExtra("splash_timer_min", minutes);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, reqCode, intent, flags);
    }

    public static void updateAllWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences prefs = context.getSharedPreferences("splash_widget", Context.MODE_PRIVATE);
        String biomeShort = prefs.getString("biomeShort", "Valle");
        String levelLabel = prefs.getString("levelLabel", "Nv. 1");
        String weatherLabel = prefs.getString("weatherLabel", "Atardecer");
        String themeMode = prefs.getString("themeMode", "auto");

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent openAppPending = PendingIntent.getActivity(context, 2, launchIntent, flags);
        PendingIntent pi2m = createTimerPendingIntent(context, 2, 202, "com.splash.offline.TIMER_2");
        PendingIntent pi15m = createTimerPendingIntent(context, 15, 215, "com.splash.offline.TIMER_15");
        PendingIntent pi30m = createTimerPendingIntent(context, 30, 230, "com.splash.offline.TIMER_30");

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_splash_quick);
            if ("dark".equals(themeMode)) {
                views.setInt(R.id.widget_quick_root, "setBackgroundResource", R.drawable.widget_quick_bg_dark);
                views.setInt(R.id.widget_quick_btn_2m, "setBackgroundResource", R.drawable.widget_quick_chip_dark);
                views.setInt(R.id.widget_quick_btn_15m, "setBackgroundResource", R.drawable.widget_quick_chip_dark);
                views.setInt(R.id.widget_quick_btn_wk, "setBackgroundResource", R.drawable.widget_quick_chip_dark);
                int darkChipText = Color.parseColor("#F2F4F2");
                views.setTextColor(R.id.widget_quick_btn_2m, darkChipText);
                views.setTextColor(R.id.widget_quick_btn_15m, darkChipText);
                views.setTextColor(R.id.widget_quick_btn_wk, darkChipText);
            } else if ("light".equals(themeMode)) {
                views.setInt(R.id.widget_quick_root, "setBackgroundResource", R.drawable.widget_quick_bg_light);
                views.setInt(R.id.widget_quick_btn_2m, "setBackgroundResource", R.drawable.widget_quick_chip_light);
                views.setInt(R.id.widget_quick_btn_15m, "setBackgroundResource", R.drawable.widget_quick_chip_light);
                views.setInt(R.id.widget_quick_btn_wk, "setBackgroundResource", R.drawable.widget_quick_chip_light);
                int lightChipText = Color.parseColor("#282622");
                views.setTextColor(R.id.widget_quick_btn_2m, lightChipText);
                views.setTextColor(R.id.widget_quick_btn_15m, lightChipText);
                views.setTextColor(R.id.widget_quick_btn_wk, lightChipText);
            } else {
                views.setInt(R.id.widget_quick_root, "setBackgroundResource", R.drawable.widget_quick_bg_auto);
                views.setInt(R.id.widget_quick_btn_2m, "setBackgroundResource", R.drawable.widget_quick_chip_auto);
                views.setInt(R.id.widget_quick_btn_15m, "setBackgroundResource", R.drawable.widget_quick_chip_auto);
                views.setInt(R.id.widget_quick_btn_wk, "setBackgroundResource", R.drawable.widget_quick_chip_auto);
            }

            views.setTextViewText(R.id.widget_quick_sub, biomeShort + " " + levelLabel + " · " + weatherLabel);
            views.setTextViewText(R.id.widget_quick_btn_2m, "2 min");
            views.setTextViewText(R.id.widget_quick_btn_15m, "15 min");
            views.setTextViewText(R.id.widget_quick_btn_wk, "30 min");
            views.setOnClickPendingIntent(R.id.widget_quick_root, openAppPending);
            views.setOnClickPendingIntent(R.id.widget_quick_btn_2m, pi2m);
            views.setOnClickPendingIntent(R.id.widget_quick_btn_15m, pi15m);
            views.setOnClickPendingIntent(R.id.widget_quick_btn_wk, pi30m);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
