package com.splash.offline;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

public final class SplashNotificationScheduler {
    public static final String PREFS_NAME = "splash_notifs";
    public static final String KEY_PAYLOAD = "payloadJson";
    public static final String KEY_NEXT_TRIGGER_MS = "nextTriggerMs";
    public static final String KEY_LAST_TIMER_CHIME_TOKEN = "lastTimerChimeToken";
    public static final String ACTION_RANDOM_PING = "com.splash.offline.ACTION_RANDOM_PING";
    public static final String ACTION_TIMER_DONE = "com.splash.offline.ACTION_TIMER_DONE";
    public static final String EXTRA_TIMER_TITLE = "timer_title";
    public static final String EXTRA_TIMER_BODY = "timer_body";
    public static final String EXTRA_TIMER_TOKEN = "timer_token";
    private static final int ALARM_REQ_CODE = 4040;
    private static final int TIMER_REQ_CODE = 4055;
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

        // Default fallback: Mon-Fri 9..21 (Mon morning 9..13)
        if (calDayOfWeek == Calendar.MONDAY) {
            for (int h = 9; h <= 13; h++) hours.add(h);
        } else if (calDayOfWeek >= Calendar.TUESDAY && calDayOfWeek <= Calendar.FRIDAY) {
            for (int h = 9; h <= 21; h++) hours.add(h);
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

    private static void scheduleExactOrFallback(AlarmManager alarmManager, long triggerMs, PendingIntent pi) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi);
                    return;
                } catch (SecurityException ignored) {
                }
            }
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi);
            } catch (SecurityException ignored) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi);
            }
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMs, pi);
        }
    }

    public static void scheduleNext(Context context) {
        scheduleNext(context, false);
    }

    public static void scheduleNext(Context context, boolean forceReschedule) {
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

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONObject payload = loadPayload(context);
        long nowMs = System.currentTimeMillis();

        if (!forceReschedule) {
            long existingMs = prefs.getLong(KEY_NEXT_TRIGGER_MS, 0L);
            if (existingMs > nowMs + 15_000L) {
                Calendar existingCal = Calendar.getInstance();
                existingCal.setTimeInMillis(existingMs);
                List<Integer> allowed = getAllowedHoursForDay(payload, existingCal.get(Calendar.DAY_OF_WEEK));
                if (allowed.contains(existingCal.get(Calendar.HOUR_OF_DAY))) {
                    // Keep existing upcoming random slot instead of pushing it forward every time the app opens
                    return;
                }
            }
        }

        alarmManager.cancel(pi);
        long minTriggerMs = nowMs + 35L * 60L * 1000L;

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
            prefs.edit().putLong(KEY_NEXT_TRIGGER_MS, triggerMs).apply();
            scheduleExactOrFallback(alarmManager, triggerMs, pi);
            return;
        }

        prefs.edit().putLong(KEY_NEXT_TRIGGER_MS, 0L).apply();
    }

    public static void scheduleTimerDone(Context context, long triggerAtWallMs, String title, String body, String token) {
        if (context == null) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, SplashNotificationReceiver.class);
        intent.setAction(ACTION_TIMER_DONE);
        intent.putExtra(EXTRA_TIMER_TITLE, title);
        intent.putExtra(EXTRA_TIMER_BODY, body);
        intent.putExtra(EXTRA_TIMER_TOKEN, token);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(context, TIMER_REQ_CODE, intent, flags);
        alarmManager.cancel(pi);
        scheduleExactOrFallback(alarmManager, Math.max(System.currentTimeMillis() + 500L, triggerAtWallMs), pi);
    }

    public static void cancelTimerDone(Context context) {
        if (context == null) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        Intent intent = new Intent(context, SplashNotificationReceiver.class);
        intent.setAction(ACTION_TIMER_DONE);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(context, TIMER_REQ_CODE, intent, flags);
        alarmManager.cancel(pi);
    }

    public static void markTimerChimePlayed(Context context, String token) {
        if (context == null || token == null) return;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_TIMER_CHIME_TOKEN, token).apply();
    }

    public static boolean consumeTimerChimePlayed(Context context, String token) {
        if (context == null || token == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String last = prefs.getString(KEY_LAST_TIMER_CHIME_TOKEN, "");
        if (token.equals(last)) {
            prefs.edit().remove(KEY_LAST_TIMER_CHIME_TOKEN).apply();
            return true;
        }
        return false;
    }

    public static String getNextScheduledLabel(Context context, boolean isEn) {
        if (context == null) return isEn ? "No active schedule" : "Sin avisos activos";
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long ms = prefs.getLong(KEY_NEXT_TRIGGER_MS, 0L);
        if (ms <= System.currentTimeMillis()) {
            return isEn ? "No active schedule" : "Sin avisos activos";
        }
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(ms);
        Calendar now = Calendar.getInstance();
        boolean isToday = target.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            && target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
        Calendar tomorrow = (Calendar) now.clone();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        boolean isTomorrow = target.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR)
            && target.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR);

        Locale loc = isEn ? Locale.ENGLISH : new Locale("es", "ES");
        String timeStr = new SimpleDateFormat("HH:mm", loc).format(new Date(ms));
        if (isToday) {
            return isEn ? ("Today at " + timeStr) : ("Hoy a las " + timeStr);
        } else if (isTomorrow) {
            return isEn ? ("Tomorrow at " + timeStr) : ("Mañana a las " + timeStr);
        } else {
            String dayStr = new SimpleDateFormat("EEE", loc).format(new Date(ms));
            return dayStr + " " + timeStr;
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

        String title = useMedio ? (badgeMedio + " · 15 min") : (badgeQuick + " · 2 min");
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
