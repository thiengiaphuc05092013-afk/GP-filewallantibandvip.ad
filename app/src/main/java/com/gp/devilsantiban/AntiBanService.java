package com.gp.devilsantiban;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import java.io.*;
import java.util.*;

public class AntiBanService extends VpnService implements Runnable {
    private ParcelFileDescriptor vpn;
    private Thread thread;
    private volatile boolean running = false;
    private static final String CHANNEL_ID = "devils_antiban";

    private static final Set<String> BLOCKED = new HashSet<>(Arrays.asList(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "google-analytics.com", "googletagmanager.com", "adnxs.com",
        "rubiconproject.com", "openx.net", "pubmatic.com", "criteo.com",
        "taboola.com", "outbrain.com", "amazon-adsystem.com",
        "app-measurement.com", "crashlytics.com", "appsflyer.com",
        "adjust.com", "branch.io", "graph.facebook.com",
        "analytics.facebook.com", "unity3d.com", "sentry.io",
        "bugsnag.com", "amplitude.com", "mixpanel.com", "segment.com",
        "flurry.com", "quantserve.com", "scorecardresearch.com",
        "telemetry.garena.com", "analytics.garena.com", "track.garena.com",
        "log.garena.com", "crash.garena.com", "bugly.garena.com",
        "stat.garena.com", "metric.garena.com", "report.garena.com",
        "anticheatexpert.com"
    ));

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            thread = new Thread(this);
            thread.start();
        }
        return START_STICKY;
    }

    @Override
    public void run() {
        try {
            Builder builder = new Builder();
            builder.setSession("Devils AntiBan");
            builder.addAddress("10.8.0.2", 32);
            builder.addRoute("0.0.0.0", 0);
            builder.addDnsServer("1.1.1.1");
            builder.addDnsServer("8.8.8.8");
            builder.setMtu(1500);

            vpn = builder.establish();
            if (vpn == null) return;

            FileInputStream in = new FileInputStream(vpn.getFileDescriptor());
            FileOutputStream out = new FileOutputStream(vpn.getFileDescriptor());
            byte[] packet = new byte[32767];

            while (running) {
                int len = in.read(packet);
                if (len <= 0) continue;
                String domain = parseDomain(packet, len);
                if (domain != null && isBlocked(domain)) continue;
                out.write(packet, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeVpn();
        }
    }

    private String parseDomain(byte[] packet, int len) {
        if (len < 40) return null;
        if ((packet[9] & 0xFF) != 17) return null;
        StringBuilder sb = new StringBuilder();
        int pos = 40;
        int maxLen = Math.min(len, 512);
        while (pos < maxLen && packet[pos] != 0) {
            int segLen = packet[pos] & 0xFF;
            if (segLen == 0) break;
            pos++;
            if (pos + segLen > maxLen) break;
            if (sb.length() > 0) sb.append(".");
            for (int i = 0; i < segLen; i++) sb.append((char) packet[pos + i]);
            pos += segLen;
        }
        return sb.toString().toLowerCase();
    }

    private boolean isBlocked(String domain) {
        if (domain == null || domain.isEmpty()) return false;
        for (String b : BLOCKED) {
            if (domain.equals(b) || domain.endsWith("." + b)) return true;
        }
        return false;
    }

    private void closeVpn() {
        try { if (vpn != null) vpn.close(); } catch (Exception e) {}
        vpn = null;
        running = false;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (thread != null) thread.interrupt();
        closeVpn();
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Devils AntiBan", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder nb;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) nb = new Notification.Builder(this, CHANNEL_ID);
        else nb = new Notification.Builder(this);
        return nb.setContentTitle("💀 Devils AntiBan")
            .setContentText("Đang bảo vệ...")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pi)
            .build();
    }
      }
