package com.focuslock.app;

import android.app.*;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import java.util.Set;

public class GuardService extends Service {
    private final Handler h = new Handler(Looper.getMainLooper());
    private UsageStatsManager usm;
    private String cur = "";
    private long lastQ;
    private long lastLaunch = 0;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            try { check(); } catch (Exception ignored) { }
            h.postDelayed(this, 600);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel("guard", "FocusLock",
                NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, "guard")
                .setContentTitle("FocusLock đang bảo vệ")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build();
        startForeground(1, n);
        lastQ = System.currentTimeMillis() - 3600000L;
        h.post(tick);
    }

    private void check() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (!pm.isInteractive()) return;

        long now = System.currentTimeMillis();
        UsageEvents ev = usm.queryEvents(lastQ, now);
        UsageEvents.Event e = new UsageEvents.Event();
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e);
            if (e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                cur = e.getPackageName();
            }
        }
        lastQ = now - 1000;

        Set<String> locked = Store.locked(this);
        if (!locked.contains(cur)) return;
        if (Store.inFreeTime(this)) return;

        Long u = Store.unlockUntil.get(cur);
        if (u != null && now < u) {
            Store.unlockUntil.put(cur, now + 8000); // gia hạn khi app vẫn đang mở
            return;
        }
        if (!LockActivity.showing && now - lastLaunch > 1200) {
            lastLaunch = now;
            Intent i = new Intent(this, LockActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("pkg", cur);
            startActivity(i);
        }
    }

    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }
    @Override public void onDestroy() { h.removeCallbacksAndMessages(null); super.onDestroy(); }
    @Override public IBinder onBind(Intent i) { return null; }
}
