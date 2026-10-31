package com.focuslock.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class LockActivity extends Activity {
    static volatile boolean showing = false;
    private String target = "";
    private TextView title;
    private EditText pw;

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor("#1B1F2A"));
        root.setPadding(dp(32), dp(32), dp(32), dp(32));

        title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        pw = new EditText(this);
        pw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pw.setHint("Mật khẩu");
        pw.setTextColor(Color.WHITE);
        pw.setHintTextColor(Color.GRAY);
        pw.setGravity(Gravity.CENTER);
        root.addView(pw, new LinearLayout.LayoutParams(-1, -2));

        Button ok = new Button(this);
        ok.setText("Mở khóa");
        ok.setOnClickListener(v -> tryUnlock());
        root.addView(ok, new LinearLayout.LayoutParams(-1, -2));

        Button home = new Button(this);
        home.setText("Về màn hình chính");
        home.setOnClickListener(v -> goHome());
        root.addView(home, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
        readTarget(getIntent());
    }

    private void readTarget(Intent i) {
        target = i.getStringExtra("pkg");
        if (target == null) target = "";
        String name = target;
        try {
            PackageManager pm = getPackageManager();
            name = pm.getApplicationLabel(pm.getApplicationInfo(target, 0)).toString();
        } catch (Exception ignored) { }
        title.setText("🔒 " + name + " đang bị khóa\nNhập mật khẩu để tiếp tục");
        pw.setText("");
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        readTarget(i);
    }

    private void tryUnlock() {
        if (Store.check(this, pw.getText().toString())) {
            Store.unlockUntil.put(target, System.currentTimeMillis() + 15000);
            showing = false;
            finish();
        } else {
            pw.setText("");
            Toast.makeText(this, "Sai mật khẩu", Toast.LENGTH_SHORT).show();
        }
    }

    private void goHome() {
        Intent h = new Intent(Intent.ACTION_MAIN);
        h.addCategory(Intent.CATEGORY_HOME);
        h.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(h);
        finish();
    }

    @Override public void onBackPressed() { goHome(); }
    @Override protected void onResume() { super.onResume(); showing = true; }
    @Override protected void onPause() { super.onPause(); showing = false; }
}
