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
            if (n == null) return;
            Bundle b = n.extras;
            if (b == null) return;
            CharSequence title = b.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence text = b.getCharSequence(Notification.EXTRA_TEXT);
            if (title == null && text == null) return;
            lastApp = getAppName(pkg);
            lastTitle = title != null ? title.toString() : "";
            lastText = text != null ? text.toString() : "";
            lastTime = System.currentTimeMillis();
            if (IslandService.instance != null) IslandService.instance.onNewNotification();
        } catch (Throwable ignored) {}
    }
    private String getAppName(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Throwable t) { return pkg; }
    }
}
