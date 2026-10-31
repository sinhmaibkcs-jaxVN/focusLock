package com.focuslock.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.MessageDigest;
import java.util.*;

public class Store {
    /** package -> thời điểm (ms) hết hạn mở khóa */
    static final HashMap<String, Long> unlockUntil = new HashMap<>();

    static SharedPreferences p(Context c) {
        return c.getSharedPreferences("focuslock", Context.MODE_PRIVATE);
    }

    static String hash(String s) {
        try {
            MessageDigest m = MessageDigest.getInstance("SHA-256");
            byte[] b = m.digest(("fl::" + s).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    static boolean hasPassword(Context c) { return p(c).contains("pw"); }
    static void setPassword(Context c, String s) { p(c).edit().putString("pw", hash(s)).apply(); }
    static boolean check(Context c, String s) { return hash(s).equals(p(c).getString("pw", "")); }

    static Set<String> locked(Context c) {
        return new HashSet<>(p(c).getStringSet("locked", new HashSet<String>()));
    }
    static void setLocked(Context c, Set<String> s) {
        p(c).edit().putStringSet("locked", new HashSet<>(s)).apply();
    }

    static boolean enabled(Context c) { return p(c).getBoolean("enabled", false); }
    static void setEnabled(Context c, boolean v) { p(c).edit().putBoolean("enabled", v).apply(); }

    /** Khung giờ tự do: danh sách {phútBắtĐầu, phútKếtThúc} */
    static List<int[]> windows(Context c) {
        List<int[]> l = new ArrayList<>();
        String s = p(c).getString("win", "");
        if (s.isEmpty()) return l;
        for (String w : s.split(";")) {
            String[] a = w.split("-");
            l.add(new int[]{Integer.parseInt(a[0]), Integer.parseInt(a[1])});
        }
        return l;
    }
    static void setWindows(Context c, List<int[]> l) {
        StringBuilder sb = new StringBuilder();
        for (int[] w : l) {
            if (sb.length() > 0) sb.append(";");
            sb.append(w[0]).append("-").append(w[1]);
        }
        p(c).edit().putString("win", sb.toString()).apply();
    }

    static boolean inFreeTime(Context c) {
        Calendar k = Calendar.getInstance();
        int n = k.get(Calendar.HOUR_OF_DAY) * 60 + k.get(Calendar.MINUTE);
        for (int[] w : windows(c)) {
            int s = w[0], e = w[1];
            if (s == e) continue;
            if (s < e) { if (n >= s && n < e) return true; }
            else { if (n >= s || n < e) return true; } // qua nửa đêm
        }
        return false;
    }
}
