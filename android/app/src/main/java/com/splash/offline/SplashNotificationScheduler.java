package com.splash.offline;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

public final class SplashNotificationScheduler {
    public static final String PREFS_NAME = "splash_notifs";
    public static final String KEY_PAYLOAD = "payloadJson";
    public static final String ACTION_RANDOM_PING = "com.splash.offline.ACTION_RANDOM_PING";
    private static final int ALARM_REQ_CODE = 4040;
    private static final String[] DAY_KEYS = {"sun", "mon", "tue", "wed", "thu", "fri", "sat"};

    private SplashNotificationScheduler() {}

    public static void savePayload(Context context, String jsonStr) {
        if (context == null || jsonStr == null || jsonStr.isEmpty()) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PAYLOAD, jsonStr).apply();
    }

    private static JSONObject loadPayload(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_PAYLOAD, "");
        if (raw == null || raw.isEmpty()) return null;
        try {
            return new JSONObject(raw);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<Integer> getAllowedHoursForDay(JSONObject payload, int calDayOfWeek) {
        String dayKey = DAY_KEYS[(calDayOfWeek - 1 + 7) % 7];
        List<Integer> hours = new ArrayList<>();
        try {
            JSONObject sched = payload != null ? payload.optJSONObject("schedule") : null;
            if (sched != null && sched.has(dayKey)) {
                JSONObject dayCfg = sched.optJSONObject(dayKey);
                if (dayCfg == null || !dayCfg.optBoolean("enabled", false)) {
                    return hours;
                }
                JSONArray arr = dayCfg.optJSONArray("hours");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        int h = arr.optInt(i, -1);
                        if (h >= 0 && h <= 23 && !hours.contains(h)) {
                            hours.add(h);
                        }
                    }
                }
                Collections.sort(hours);
                return hours;
            }
        } catch (Throwable ignored) {}

        // Default fallback: Mon-Fri 9..21
        if (calDayOfWeek >= Calendar.MONDAY && calDayOfWeek <= Calendar.FRIDAY) {
            for (int h = 9; h <= 21; h++) {
                hours.add(h);
            }
        }
        return hours;
    }

    public static boolean isNowAllowed(Context context) {
        JSONObject payload = loadPayload(context);
        Calendar now = Calendar.getInstance();
        int curHour = now.get(Calendar.HOUR_OF_DAY);
        List<Integer> allowed = getAllowedHoursForDay(payload, now.get(Calendar.DAY_OF_WEEK));
        return allowed.contains(curHour);
    }

    public static void scheduleNext(Context context) {
        if (context == null) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, SplashNotificationReceiver.class);
        intent.setAction(ACTION_RANDOM_PING);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(context, ALARM_REQ_CODE, intent, flags);
        alarmManager.cancel(pi);

        JSONObject payload = loadPayload(context);
        long nowMs = System.currentTimeMillis();
        long minTriggerMs = nowMs + 50L * 60L * 1000L;

        Calendar base = Calendar.getInstance();
        Random rng = new Random();

        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            Calendar dayCal = (Calendar) base.clone();
            dayCal.add(Calendar.DAY_OF_YEAR, dayOffset);
            List<Integer> hours = getAllowedHoursForDay(payload, dayCal.get(Calendar.DAY_OF_WEEK));
            if (hours.isEmpty()) continue;

            List<Integer> candidateHours = new ArrayList<>();
            for (int h : hours) {
                Calendar slotEnd = (Calendar) dayCal.clone();
                slotEnd.set(Calendar.HOUR_OF_DAY, h);
                slotEnd.set(Calendar.MINUTE, 59);
                slotEnd.set(Calendar.SECOND, 0);
                slotEnd.set(Calendar.MILLISECOND, 0);
                if (slotEnd.getTimeInMillis() > minTriggerMs) {
                    candidateHours.add(h);
                }
            }
            if (candidateHours.isEmpty()) continue;

            int span = Math.min(4, candidateHours.size());
            int chosenHour = candidateHours.get(rng.nextInt(span));

            Calendar slot = (Calendar) dayCal.clone();
            slot.set(Calendar.HOUR_OF_DAY, chosenHour);
            slot.set(Calendar.SECOND, 0);
            slot.set(Calendar.MILLISECOND, 0);

            int minMinute = 0;
            Calendar earliest = Calendar.getInstance();
            earliest.setTimeInMillis(minTriggerMs);
            if (dayOffset == 0 && chosenHour == earliest.get(Calendar.HOUR_OF_DAY)) {
                minMinute = Math.min(58, earliest.get(Calendar.MINUTE) + 1);
            }
            int chosenMin = minMinute + rng.nextInt(Math.max(1, 60 - minMinute));
            slot.set(Calendar.MINUTE, chosenMin);

            long triggerMs = Math.max(minTriggerMs, slot.getTimeInMillis());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMs, pi);
            }
            return;
        }
    }

    public static String[] pickRandomNotification(Context context) {
        JSONObject payload = loadPayload(context);
        boolean isEn = payload != null && "en".equals(payload.optString("lang", "es"));
        Random rng = new Random();
        boolean useMedio = rng.nextInt(4) == 0; // 25% 15-min, 75% 2-min quick

        String badgeQuick = payload != null && !payload.optString("quickBadge").isEmpty()
            ? payload.optString("quickBadge")
            : (isEn ? "Splash · 2 min" : "Splash · Aviso Rápido");
        String badgeMedio = payload != null && !payload.optString("medioBadge").isEmpty()
            ? payload.optString("medioBadge")
            : (isEn ? "Splash · 15 min" : "Splash · Enfoque");

        JSONArray pool = null;
        if (payload != null) {
            pool = payload.optJSONArray(useMedio ? "medioPool" : "quickPool");
            if (pool == null || pool.length() == 0) {
                useMedio = !useMedio;
                pool = payload.optJSONArray(useMedio ? "medioPool" : "quickPool");
            }
        }

        String title = useMedio ? (badgeMedio + " · 15m") : (badgeQuick + " · 20m");
        String body = isEn
            ? "Take a 2-minute pause away from the screen."
            : "Tómate 2 minutos lejos de la pantalla para cuidar tu bioma.";
        if (pool != null && pool.length() > 0) {
            String picked = pool.optString(rng.nextInt(pool.length()), "");
            if (!picked.isEmpty()) {
                body = picked;
            }
        }
        return new String[]{title, body};
    }
}
