package com.music.app.service;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotificationListener extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            if (sbn == null) return;
            String pkg = sbn.getPackageName();
            // 过滤掉自己
            if ("com.music.app.pro".equals(pkg)) return;

            Notification n = sbn.getNotification();
            if (n == null || n.extras == null) return;
            Bundle ex = n.extras;
            CharSequence t = ex.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence x = ex.getCharSequence(Notification.EXTRA_TEXT);
            String title = t != null ? t.toString() : pkg;
            String text = x != null ? x.toString() : "";
            if (title.isEmpty() && text.isEmpty()) return;

            IslandService.notify(title, text);
        } catch (Throwable ignored) {}
    }
}
