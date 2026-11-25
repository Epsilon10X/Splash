package com.splash.offline;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.RemoteViews;

public class SplashQuickWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds);
    }

    public static void updateAllWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences prefs = context.getSharedPreferences("splash_widget", Context.MODE_PRIVATE);
        String biomeShort = prefs.getString("biomeShort", "Valle");
        String levelLabel = prefs.getString("levelLabel", "Nv. 1");
        String weatherLabel = prefs.getString("weatherLabel", "Radiante");
        boolean isEn = prefs.getBoolean("isEn", false);

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 2, launchIntent, flags);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_splash_quick);
            views.setTextViewText(R.id.widget_quick_sub, biomeShort + " " + levelLabel + " · " + weatherLabel);
            views.setTextViewText(R.id.widget_quick_btn_2m, "2 min");
            views.setTextViewText(R.id.widget_quick_btn_15m, "15 min");
            views.setTextViewText(R.id.widget_quick_btn_wk, isEn ? "Weekly" : "Semanal");
            views.setOnClickPendingIntent(R.id.widget_quick_root, pendingIntent);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
