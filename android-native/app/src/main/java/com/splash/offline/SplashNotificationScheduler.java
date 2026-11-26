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
    public static final String KEY_PENDING_NOTIF_GOAL_ID = "pendingNotifGoalId";
    public static final String KEY_PENDING_NOTIF_GOAL_TS = "pendingNotifGoalTs";
    public static final String KEY_DAILY_PLAN_KEY = "dailyPlanKey";
    public static final String KEY_DAILY_PLAN_TIMES = "dailyPlanTimes";
    public static final String ACTION_RANDOM_PING = "com.splash.offline.ACTION_RANDOM_PING";
    public static final String ACTION_TIMER_DONE = "com.splash.offline.ACTION_TIMER_DONE";
    public static final String EXTRA_TIMER_TITLE = "timer_title";
    public static final String EXTRA_TIMER_BODY = "timer_body";
    public static final String EXTRA_TIMER_TOKEN = "timer_token";
    public static final String EXTRA_NOTIF_GOAL_ID = "splash_notif_goal_id";
    private static final int ALARM_REQ_CODE = 4040;
    private static final int TIMER_REQ_CODE = 4055;
    private static final long PENDING_NOTIF_MAX_AGE_MS = 24L * 60L * 60L * 1000L;
    private static final String[] DAY_KEYS = {"sun", "mon", "tue", "wed", "thu", "fri", "sat"};

    public static final class PickedNotification {
        public final int goalId;
        public final String title;
        public final String body;

        public PickedNotification(int goalId, String title, String body) {
            this.goalId = goalId;
            this.title = title;
            this.body = body;
        }
    }

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
        int dailyCount = 4;
        if (payload != null) {
            dailyCount = Math.max(2, Math.min(7, payload.optInt("dailyCount", 4)));
        }

        Calendar base = Calendar.getInstance();
        Random rng = new Random();
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String existingPlanKey = prefs.getString(KEY_DAILY_PLAN_KEY, "");
        String existingPlanTimesRaw = prefs.getString(KEY_DAILY_PLAN_TIMES, "");

        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            Calendar dayCal = (Calendar) base.clone();
            dayCal.add(Calendar.DAY_OF_YEAR, dayOffset);
            dayCal.set(Calendar.HOUR_OF_DAY, 0);
            dayCal.set(Calendar.MINUTE, 0);
            dayCal.set(Calendar.SECOND, 0);
            dayCal.set(Calendar.MILLISECOND, 0);

            List<Integer> hours = getAllowedHoursForDay(payload, dayCal.get(Calendar.DAY_OF_WEEK));
            if (hours.isEmpty()) continue;

            StringBuilder hoursSig = new StringBuilder();
            for (int h : hours) {
                hoursSig.append(h).append(',');
            }
            String planKey = dayFmt.format(dayCal.getTime()) + "|" + dailyCount + "|" + hoursSig;

            List<Long> daySlots = null;
            if (!forceReschedule && planKey.equals(existingPlanKey) && !existingPlanTimesRaw.isEmpty()) {
                daySlots = parsePlanTimes(existingPlanTimesRaw);
            }
            if (daySlots == null || daySlots.isEmpty()) {
                daySlots = buildDailyRandomSlots(dayCal, hours, dailyCount, rng);
                prefs.edit()
                    .putString(KEY_DAILY_PLAN_KEY, planKey)
                    .putString(KEY_DAILY_PLAN_TIMES, joinPlanTimes(daySlots))
                    .apply();
            }

            long minWaitMs = forceReschedule ? (nowMs + 60_000L) : (nowMs + 15_000L);
            for (long slotMs : daySlots) {
                if (slotMs >= minWaitMs) {
                    prefs.edit().putLong(KEY_NEXT_TRIGGER_MS, slotMs).apply();
                    scheduleExactOrFallback(alarmManager, slotMs, pi);
                    return;
                }
            }
        }

        prefs.edit().putLong(KEY_NEXT_TRIGGER_MS, 0L).apply();
    }

    private static List<Long> buildDailyRandomSlots(Calendar dayStartCal, List<Integer> hours, int dailyCount, Random rng) {
        List<Integer> minuteOfDayPool = new ArrayList<>();
        for (int h : hours) {
            int baseMin = h * 60;
            for (int m = 0; m < 60; m++) {
                minuteOfDayPool.add(baseMin + m);
            }
        }
        int totalMins = minuteOfDayPool.size();
        List<Long> result = new ArrayList<>();
        if (totalMins == 0) return result;

        int maxPossible = Math.min(dailyCount, totalMins);
        int actualCount = maxPossible <= 2 ? maxPossible : (2 + rng.nextInt(maxPossible - 2 + 1));
        int targetCount = actualCount;
        int lastPicked = -1000;
        int minGap = (totalMins >= targetCount * 22) ? 20 : 5;

        for (int i = 0; i < targetCount; i++) {
            int segStart = (i * totalMins) / targetCount;
            int segEnd = ((i + 1) * totalMins) / targetCount - 1;
            if (segEnd < segStart) segEnd = segStart;

            int span = Math.max(1, segEnd - segStart + 1);
            int chosenIdx = segStart + rng.nextInt(span);
            int chosenMinOfDay = minuteOfDayPool.get(chosenIdx);
            if (chosenMinOfDay - lastPicked < minGap) {
                for (int probe = segStart; probe <= segEnd; probe++) {
                    int candidate = minuteOfDayPool.get(probe);
                    if (candidate - lastPicked >= minGap) {
                        chosenMinOfDay = candidate;
                        break;
                    }
                }
            }
            lastPicked = chosenMinOfDay;

            Calendar slot = (Calendar) dayStartCal.clone();
            slot.set(Calendar.HOUR_OF_DAY, chosenMinOfDay / 60);
            slot.set(Calendar.MINUTE, chosenMinOfDay % 60);
            slot.set(Calendar.SECOND, rng.nextInt(50));
            slot.set(Calendar.MILLISECOND, 0);
            result.add(slot.getTimeInMillis());
        }
        Collections.sort(result);
        return result;
    }

    private static List<Long> parsePlanTimes(String raw) {
        List<Long> list = new ArrayList<>();
        try {
            String[] parts = raw.split(",");
            for (String p : parts) {
                if (!p.isEmpty()) {
                    list.add(Long.parseLong(p));
                }
            }
        } catch (Throwable ignored) {
            list.clear();
        }
        return list;
    }

    private static String joinPlanTimes(List<Long> times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(times.get(i));
        }
        return sb.toString();
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

    public static void savePendingNotificationGoal(Context context, int goalId) {
        if (context == null || goalId <= 0) return;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PENDING_NOTIF_GOAL_ID, goalId)
            .putLong(KEY_PENDING_NOTIF_GOAL_TS, System.currentTimeMillis())
            .commit();
    }

    public static int getPendingNotificationGoalId(Context context) {
        if (context == null) return -1;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int id = prefs.getInt(KEY_PENDING_NOTIF_GOAL_ID, -1);
        long ts = prefs.getLong(KEY_PENDING_NOTIF_GOAL_TS, 0L);
        if (id <= 0) return -1;
        if (ts > 0L && (System.currentTimeMillis() - ts) > PENDING_NOTIF_MAX_AGE_MS) {
            prefs.edit().remove(KEY_PENDING_NOTIF_GOAL_ID).remove(KEY_PENDING_NOTIF_GOAL_TS).apply();
            return -1;
        }
        return id;
    }

    public static void clearPendingNotificationGoal(Context context) {
        if (context == null) return;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PENDING_NOTIF_GOAL_ID)
            .remove(KEY_PENDING_NOTIF_GOAL_TS)
            .apply();
    }

    public static PickedNotification pickRandomNotificationGoal(Context context) {
        JSONObject payload = loadPayload(context);
        boolean isEn = payload != null && "en".equals(payload.optString("lang", "es"));
        Random rng = new Random();
        boolean useMedio = rng.nextInt(4) == 0; // 25% 15-min, 75% Flash (2 min)

        String badgeQuick = payload != null && !payload.optString("quickBadge").isEmpty()
            ? payload.optString("quickBadge")
            : "Splash · Flash";
        String badgeMedio = payload != null && !payload.optString("medioBadge").isEmpty()
            ? payload.optString("medioBadge")
            : (isEn ? "Splash · Focus" : "Splash · Enfoque");

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
        int goalId = -1;
        if (pool != null && pool.length() > 0) {
            int idx = rng.nextInt(pool.length());
            JSONObject obj = pool.optJSONObject(idx);
            if (obj != null) {
                goalId = obj.optInt("id", -1);
                String pickedBody = obj.optString("body", "");
                if (!pickedBody.isEmpty()) {
                    body = pickedBody;
                }
            } else {
                String picked = pool.optString(idx, "");
                if (!picked.isEmpty()) {
                    body = picked;
                }
            }
        }
        return new PickedNotification(goalId, title, body);
    }

    public static String[] pickRandomNotification(Context context) {
        PickedNotification p = pickRandomNotificationGoal(context);
        return new String[]{p.title, p.body};
    }
}
