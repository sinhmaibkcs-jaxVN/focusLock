package com.focuslock.app;

import android.content.*;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (Store.enabled(c)) {
            try { c.startForegroundService(new Intent(c, GuardService.class)); }
            catch (Exception ignored) { }
        }
    }
}
