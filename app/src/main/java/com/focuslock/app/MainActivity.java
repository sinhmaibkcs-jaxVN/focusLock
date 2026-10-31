package com.focuslock.app;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static long authUntil = 0;

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    @Override protected void onResume() {
        super.onResume();
        long now = System.currentTimeMillis();
        if (authUntil == Long.MAX_VALUE || now < authUntil) {
            authUntil = Long.MAX_VALUE;
            showMain();
        } else {
            showGate();
        }
    }

    @Override protected void onPause() {
        super.onPause();
        if (authUntil == Long.MAX_VALUE) authUntil = System.currentTimeMillis() + 60000;
    }

    // ---------- helpers ----------
    private TextView tv(String t, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(Color.parseColor("#222222"));
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        v.setPadding(0, dp(8), 0, dp(4));
        return v;
    }

    private Button btn(String t, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    private EditText pwField(String hint) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        e.setHint(hint);
        return e;
    }

    private String hm(int m) { return String.format("%02d:%02d", m / 60, m % 60); }

    // ---------- màn hình mật khẩu ----------
    private void showGate() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(32), dp(32), dp(32), dp(32));

        if (!Store.hasPassword(this)) {
            root.addView(tv("Tạo mật khẩu cho FocusLock (tối thiểu 4 ký tự)", 18, true));
            EditText a = pwField("Mật khẩu mới");
            EditText b = pwField("Nhập lại mật khẩu");
            root.addView(a); root.addView(b);
            root.addView(btn("Lưu", v -> {
                String x = a.getText().toString();
                if (x.length() < 4) { toast("Mật khẩu quá ngắn"); return; }
                if (!x.equals(b.getText().toString())) { toast("Hai mật khẩu không khớp"); return; }
                Store.setPassword(this, x);
                authUntil = Long.MAX_VALUE;
                showMain();
            }));
        } else {
            root.addView(tv("🔒 FocusLock", 22, true));
            EditText a = pwField("Mật khẩu");
            root.addView(a);
            root.addView(btn("Vào", v -> {
                if (Store.check(this, a.getText().toString())) {
                    authUntil = Long.MAX_VALUE;
                    showMain();
                } else { a.setText(""); toast("Sai mật khẩu"); }
            }));
        }
        setContentView(root);
    }

    // ---------- màn hình chính ----------
    private boolean hasUsage() {
        AppOpsManager ops = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    private void showMain() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(32));
        sv.addView(root);

        root.addView(tv("🔒 FocusLock", 24, true));

        // Quyền
        boolean u = hasUsage();
        boolean o = Settings.canDrawOverlays(this);
        root.addView(tv("1. Cấp quyền", 18, true));
        root.addView(tv((u ? "✅" : "❌") + " Truy cập dữ liệu sử dụng (Usage access)", 14, false));
        if (!u) root.addView(btn("Cấp quyền Usage access", v ->
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))));
        root.addView(tv((o ? "✅" : "❌") + " Hiển thị trên ứng dụng khác", 14, false));
        if (!o) root.addView(btn("Cấp quyền hiển thị trên ứng dụng khác", v ->
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())))));
        root.addView(btn("Tắt tối ưu pin cho FocusLock (khuyên dùng)", v ->
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))));

        // Bật/tắt
        root.addView(tv("2. Bảo vệ", 18, true));
        boolean on = Store.enabled(this);
        root.addView(tv(on ? "Đang BẬT" : "Đang TẮT", 14, false));
        root.addView(btn(on ? "Tắt bảo vệ" : "Bật bảo vệ", v -> {
            if (!on) {
                if (!hasUsage() || !Settings.canDrawOverlays(this)) {
                    toast("Hãy cấp đủ 2 quyền trước"); return;
                }
                Store.setEnabled(this, true);
                startForegroundService(new Intent(this, GuardService.class));
            } else {
                Store.setEnabled(this, false);
                stopService(new Intent(this, GuardService.class));
            }
            showMain();
        }));

        // Khung giờ tự do
        root.addView(tv("3. Khung giờ tự do (không cần mật khẩu, mỗi ngày)", 18, true));
        List<int[]> wins = Store.windows(this);
        if (wins.isEmpty()) root.addView(tv("Chưa có khung giờ nào", 14, false));
        for (int idx = 0; idx < wins.size(); idx++) {
            final int k = idx;
            int[] w = wins.get(idx);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView t = tv(hm(w[0]) + "  →  " + hm(w[1]), 16, false);
            row.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(btn("Xóa", v -> {
                List<int[]> l = Store.windows(this);
                l.remove(k);
                Store.setWindows(this, l);
                showMain();
            }));
            root.addView(row);
        }
        root.addView(btn("+ Thêm khung giờ", v -> addWindow()));

        // Danh sách app
        root.addView(tv("4. Chọn ứng dụng cần khóa", 18, true));
        Set<String> locked = Store.locked(this);
        for (String[] a : listApps()) {
            final String pkg = a[0];
            CheckBox cb = new CheckBox(this);
            cb.setText(a[1]);
            cb.setTextSize(16);
            cb.setChecked(locked.contains(pkg));
            cb.setOnCheckedChangeListener((bv, checked) -> {
                Set<String> s = Store.locked(this);
                if (checked) s.add(pkg); else s.remove(pkg);
                Store.setLocked(this, s);
            });
            root.addView(cb);
        }

        // Đổi mật khẩu
        root.addView(tv("5. Mật khẩu", 18, true));
        root.addView(btn("Đổi mật khẩu", v -> changePassword()));

        setContentView(sv);
    }

    private void addWindow() {
        toast("Chọn giờ BẮT ĐẦU");
        new TimePickerDialog(this, (v1, h1, m1) -> {
            toast("Chọn giờ KẾT THÚC");
            new TimePickerDialog(this, (v2, h2, m2) -> {
                int s = h1 * 60 + m1, e = h2 * 60 + m2;
                if (s == e) { toast("Giờ bắt đầu và kết thúc trùng nhau"); return; }
                List<int[]> l = Store.windows(this);
                l.add(new int[]{s, e});
                Store.setWindows(this, l);
                showMain();
            }, 9, 0, true).show();
        }, 8, 0, true).show();
    }

    private void changePassword() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(20), dp(10), dp(20), 0);
        EditText old = pwField("Mật khẩu hiện tại");
        EditText n1 = pwField("Mật khẩu mới");
        l.addView(old); l.addView(n1);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Đổi mật khẩu").setView(l)
                .setPositiveButton("Lưu", (d, w) -> {
                    if (!Store.check(this, old.getText().toString())) { toast("Sai mật khẩu hiện tại"); return; }
                    if (n1.getText().length() < 4) { toast("Mật khẩu quá ngắn"); return; }
                    Store.setPassword(this, n1.getText().toString());
                    toast("Đã đổi mật khẩu");
                })
                .setNegativeButton("Hủy", null).show();
    }

    /** Trả về danh sách {package, tên}, đã loại Cài đặt, hệ thống, launcher và chính app này */
    private List<String[]> listApps() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        Set<String> homes = new HashSet<>();
        for (ResolveInfo r : pm.queryIntentActivities(home, 0)) homes.add(r.activityInfo.packageName);

        List<String[]> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo r : pm.queryIntentActivities(main, 0)) {
            String p = r.activityInfo.packageName;
            if (!seen.add(p)) continue;
            String pl = p.toLowerCase();
            if (p.equals(getPackageName()) || homes.contains(p)) continue;
            if (pl.contains("settings") || pl.contains("systemui") || pl.contains("packageinstaller")
                    || pl.contains("permissioncontroller") || pl.equals("android")) continue;
            out.add(new String[]{p, r.loadLabel(pm).toString()});
        }
        Collections.sort(out, (a, b) -> a[1].compareToIgnoreCase(b[1]));
        return out;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
