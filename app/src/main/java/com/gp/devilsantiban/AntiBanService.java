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
    private static int blockedCount = 0;
    private static final String CHANNEL_ID = "devils_antiban";

    // ===== DANH SÁCH CHẶN =====
    private static final Set<String> BLOCKED = new HashSet<>(Arrays.asList(
        // === ADS NETWORKS ===
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "google-analytics.com", "googletagmanager.com", "googletagservices.com",
        "adnxs.com", "rubiconproject.com", "openx.net", "pubmatic.com",
        "criteo.com", "criteo.net", "taboola.com", "outbrain.com",
        "amazon-adsystem.com", "adcolony.com", "applovin.com",
        "chartboost.com", "vungle.com", "inmobi.com", "mopub.com",
        "smaato.com", "ironsrc.com", "tapjoy.com", "fyber.com",
        "unityads.unity3d.com", "ads.unity3d.com",
        "advertising.com", "adsrvr.org", "casalemedia.com",
        "contextweb.com", "districtm.io", "indexexchange.com",
        "sovrn.com", "sharethrough.com", "triplelift.com",
        "yieldmo.com", "zemanta.com", "smartadserver.com",
        "teads.tv", "adform.net", "flashtalking.com",
        "media.net", "ads.yahoo.com", "adtechus.com",
        "247realmedia.com", "atwola.com", "tribalfusion.com",
        "turn.com", "zedo.com", "zqtk.net", "bidswitch.net",
        "doubleverify.com", "moatads.com", "adsafeprotected.com",
        
        // === TRACKING ===
        "app-measurement.com", "crashlytics.com", "firebase-settings.crashlytics.com",
        "appsflyer.com", "appsflyersdk.com", "gcdsdk.appsflyer.com",
        "skadsdkless.appsflyer.com", "conversions.appsflyer.com",
        "inapps.appsflyer.com", "attr.appsflyer.com", "launches.appsflyer.com",
        "cdn-settings.appsflyersdk.com",
        "adjust.com", "adjust.io",
        "branch.io", "app.branch.io", "api2.branch.io",
        "graph.facebook.com", "analytics.facebook.com", "fna.fbcdn.net",
        "unity3d.com", "cdp.cloud.unity3d.com", "perf-events.cloud.unity3d.com",
        "metrics-api.cloud.unity3d.com", "prd-pixel-lookup.cloud.unity3d.com",
        "analytics.tiktok.com", "business-api.tiktok.com",
        "log.byteoversea.com", "mon.byteoversea.com",
        "sentry.io", "bugsnag.com", "newrelic.com", "nr-data.net",
        "amplitude.com", "api.amplitude.com",
        "mixpanel.com", "api.mixpanel.com",
        "segment.com", "segment.io", "api.segment.io",
        "flurry.com", "data.flurry.com",
        "quantserve.com", "quantcast.com", "scorecardresearch.com",
        "comscore.com", "nielsen.com", "moatads.com",
        "hotjar.com", "static.hotjar.com", "script.hotjar.com",
        "mouseflow.com", "crazyegg.com", "kissmetrics.com",
        "bluekai.com", "demdex.net", "exelator.com", "krxd.net",
        "mathtag.com", "ml314.com", "omtrdc.net", "serving-sys.com",
        "kochava.com", "singular.net", "tenjin.io",
        "mintegral.com", "pangle.io", "sf16-pangle.com",
        
        // === GARENA TRACKING (KHÔNG CHẶN SERVER CHÍNH) ===
        "telemetry.garena.com", "analytics.garena.com", "track.garena.com",
        "tracking.garena.com", "log.garena.com", "crash.garena.com",
        "bugly.garena.com", "stat.garena.com", "metric.garena.com",
        "report.garena.com", "validation.garena.com", "audit.garena.com",
        "alert.garena.com", "guard.garena.com", "checker.garena.com",
        
        // === ANTI-CHEAT THIRD PARTY ===
        "anticheatexpert.com", "anticheatexpert.com.mtp.qq.com",
        "anticheatexpert.con",
        
        // === APP CLONE ===
        "com.dts.freefiremax", "com.multiaccounts.cloneapps"
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
                if (domain != null && isBlocked(domain)) {
                    blockedCount++;
                    continue;
                }
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
            .setContentText("Đang chặn ads + tracking | Blocked: " + blockedCount)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pi)
            .build();
    }
}
