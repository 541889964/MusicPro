package com.music.app.service;

import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotifListener extends NotificationListenerService {
    public static String lastApp = "";
    public static String lastTitle = "";
    public static String lastText = "";
    public static long lastTime = 0;

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            if (sbn == null) return;
            String pkg = sbn.getPackageName();
            if (pkg == null || pkg.equals(getPackageName())) return;
            Notification n = sbn.getNotification();
            if (n == null || n.extras == null) return;
            Bundle b = n.extras;
            CharSequence t = b.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence tx = b.getCharSequence(Notification.EXTRA_TEXT);
            lastApp = getAppName(pkg);
            lastTitle = t != null ? t.toString() : "";
            lastText = tx != null ? tx.toString() : "";
            lastTime = System.currentTimeMillis();
            if (IslandService.instance != null) IslandService.instance.onNotif();
        } catch (Throwable ignored) {}
    }

    private String getAppName(String p) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(p, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Throwable t) { return p; }
    }
}
