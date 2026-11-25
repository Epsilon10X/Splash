package com.splash.offline;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
        String goal2Title = prefs.getString("goal2Title", prefs.getString("nextGoal", "Respirar aire fresco"));
        String goal15Title = prefs.getString("goal15Title", "Caminar cuesta arriba");
        String themeMode = prefs.getString("themeMode", "auto");
        boolean isEn = prefs.getBoolean("isEn", false);

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, flags);
        Bitmap biomeBitmap = loadBiomeBitmap(context);

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
                views.setImageViewResource(R.id.widget_biome_img, R.mipmap.ic_launcher);
            }

            views.setTextViewText(R.id.widget_biome_chip, biomeShort);
            views.setTextViewText(R.id.widget_goal_2m_title, goal2Title);
            views.setTextViewText(R.id.widget_goal_2m_sub, isEn ? "2 min · Quick" : "2 min · Rápido");
            views.setTextViewText(R.id.widget_goal_15m_title, goal15Title);
            views.setTextViewText(R.id.widget_goal_15m_sub, isEn ? "15 min · Focus" : "15 min · Enfoque");
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
