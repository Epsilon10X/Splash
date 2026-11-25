package com.splash.offline;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.widget.RemoteViews;

public class SplashBiomeWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds);
    }

    public static void updateAllWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences prefs = context.getSharedPreferences("splash_widget", Context.MODE_PRIVATE);
        String biomeName = prefs.getString("biomeName", "Valle Alpino");
        String levelLabel = prefs.getString("levelLabel", "Nv. 1");
        String streakLabel = prefs.getString("streakLabel", "0 días");
        boolean isDark = prefs.getBoolean("isDark", false);

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, launchIntent, flags);
        Bitmap biomeBitmap = SplashWidgetProvider.loadBiomeBitmap(context);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_splash_biome);
            views.setInt(R.id.widget_biome_root, "setBackgroundResource", isDark ? R.drawable.widget_card_dark : R.drawable.widget_card_light);
            if (biomeBitmap != null) {
                views.setImageViewBitmap(R.id.widget_biome_2x2_img, biomeBitmap);
            } else {
                views.setImageViewResource(R.id.widget_biome_2x2_img, R.mipmap.ic_launcher);
            }
            views.setTextViewText(R.id.widget_biome_name, biomeName);
            views.setTextColor(R.id.widget_biome_name, isDark ? Color.parseColor("#F1F4F1") : Color.parseColor("#1F2420"));
            views.setTextViewText(R.id.widget_biome_level, levelLabel + " · " + streakLabel);
            views.setTextColor(R.id.widget_biome_level, isDark ? Color.parseColor("#7BC47F") : Color.parseColor("#1F7A4C"));
            views.setOnClickPendingIntent(R.id.widget_biome_root, pendingIntent);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
