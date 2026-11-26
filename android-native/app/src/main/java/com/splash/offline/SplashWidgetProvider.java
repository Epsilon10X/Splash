package com.splash.offline;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.widget.RemoteViews;
import java.io.File;

public class SplashWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds);
    }

    public static Bitmap loadBiomeBitmap(Context context) {
        try {
            File file = new File(context.getFilesDir(), "widget_biome.png");
            if (!file.exists() || file.length() == 0) {
                return null;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void updateAllWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences prefs = context.getSharedPreferences("splash_widget", Context.MODE_PRIVATE);
        String biomeShort = prefs.getString("biomeShort", "Valle");
        String flash1Title = prefs.getString("flash1Title", prefs.getString("goal2Title", "Respirar aire fresco"));
        String flash1Sub = prefs.getString("flash1Sub", "Flash · 2 min");
        boolean flash1Done = prefs.getBoolean("flash1Done", false);
        String flash2Title = prefs.getString("flash2Title", "Postura y agua");
        String flash2Sub = prefs.getString("flash2Sub", "Flash · 2 min");
        boolean flash2Done = prefs.getBoolean("flash2Done", false);
        String themeMode = prefs.getString("themeMode", "auto");

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, flags);
        Bitmap biomeBitmap = loadBiomeBitmap(context);

        int pendingTitleColor = Color.parseColor("#FFF8EE");
        int pendingSubColor = Color.parseColor("#D8C4AA");
        int doneTitleColor = Color.parseColor("#F2FFF5");
        int doneSubColor = Color.parseColor("#A7E8B8");

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_splash);
            if ("dark".equals(themeMode)) {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_card_dark);
                views.setInt(R.id.widget_diorama_box, "setBackgroundResource", R.drawable.widget_diorama_dark);
            } else if ("light".equals(themeMode)) {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_card_light);
                views.setInt(R.id.widget_diorama_box, "setBackgroundResource", R.drawable.widget_diorama_light);
            } else {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_card_auto);
                views.setInt(R.id.widget_diorama_box, "setBackgroundResource", R.drawable.widget_diorama_auto);
            }

            if (biomeBitmap != null) {
                views.setImageViewBitmap(R.id.widget_biome_img, biomeBitmap);
            } else {
                views.setImageViewResource(R.id.widget_biome_img, R.drawable.widget_valle_max);
            }

            views.setInt(
                R.id.widget_tile_rapido,
                "setBackgroundResource",
                flash1Done ? R.drawable.widget_goal_done_light : R.drawable.widget_goal_rapido_light
            );
            views.setInt(
                R.id.widget_tile_medio,
                "setBackgroundResource",
                flash2Done ? R.drawable.widget_goal_done_light : R.drawable.widget_goal_rapido_light
            );

            views.setTextViewText(R.id.widget_biome_chip, biomeShort);
            views.setTextViewText(R.id.widget_goal_2m_title, flash1Title);
            views.setTextViewText(R.id.widget_goal_2m_sub, flash1Sub);
            views.setTextColor(R.id.widget_goal_2m_title, flash1Done ? doneTitleColor : pendingTitleColor);
            views.setTextColor(R.id.widget_goal_2m_sub, flash1Done ? doneSubColor : pendingSubColor);

            views.setTextViewText(R.id.widget_goal_15m_title, flash2Title);
            views.setTextViewText(R.id.widget_goal_15m_sub, flash2Sub);
            views.setTextColor(R.id.widget_goal_15m_title, flash2Done ? doneTitleColor : pendingTitleColor);
            views.setTextColor(R.id.widget_goal_15m_sub, flash2Done ? doneSubColor : pendingSubColor);

            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
