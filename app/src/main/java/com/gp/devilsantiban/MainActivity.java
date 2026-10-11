package com.gp.devilsantiban;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {
    private TextView status, log;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        ll.setPadding(50, 50, 50, 50);
        ll.setBackgroundColor(0xFF0A0A0F);

        TextView title = new TextView(this);
        title.setText("💀 DEVILS ANTI-BAN");
        title.setTextSize(26);
        title.setTextColor(0xFFFF4444);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);

        status = new TextView(this);
        status.setText("⏳ OFF");
        status.setTextSize(18);
        status.setTextColor(0xFF00FFCC);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 0, 0, 30);

        Button btnStart = new Button(this);
        btnStart.setText("🔥 BẬT ANTI-BAN");
        btnStart.setPadding(20, 30, 20, 30);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = VpnService.prepare(MainActivity.this);
                if (i != null) startActivityForResult(i, 1);
                else startAntiBan();
            }
        });

        Button btnStop = new Button(this);
        btnStop.setText("⛔ TẮT");
        btnStop.setPadding(20, 30, 20, 30);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                stopService(new Intent(MainActivity.this, AntiBanService.class));
                status.setText("⏳ OFF");
                status.setTextColor(0xFF888888);
            }
        });

        log = new TextView(this);
        log.setText("Sẵn sàng.");
        log.setTextSize(12);
        log.setTextColor(0xFF00FF88);
        log.setPadding(20, 20, 20, 20);

        ll.addView(title);
        ll.addView(status);
        ll.addView(btnStart);
        ll.addView(btnStop);
        ll.addView(log);
        setContentView(ll);
    }

    private void startAntiBan() {
        startService(new Intent(this, AntiBanService.class));
        status.setText("✅ ANTI-BAN ACTIVE");
        status.setTextColor(0xFF00FF00);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent d) {
        super.onActivityResult(req, res, d);
        if (req == 1 && res == RESULT_OK) startAntiBan();
    }
          }
