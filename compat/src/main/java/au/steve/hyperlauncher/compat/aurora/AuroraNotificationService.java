package au.steve.hyperlauncher.compat.aurora;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class AuroraNotificationService extends NotificationListenerService {
    @Override public void onListenerConnected() {
        NotificationHub.get().setConnected(true);
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active == null) return;
            for (StatusBarNotification sbn : active) NotificationHub.get().upsert(sbn, appName(sbn));
        } catch (SecurityException ignored) {
            NotificationHub.get().setConnected(false);
        }
    }

    @Override public void onListenerDisconnected() {
        NotificationHub.get().setConnected(false);
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        NotificationHub.get().upsert(sbn, appName(sbn));
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn != null) NotificationHub.get().remove(sbn.getKey());
    }

    private String appName(StatusBarNotification sbn) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(sbn.getPackageName(), 0);
            return pm.getApplicationLabel(info).toString();
        } catch (Exception e) {
            return sbn.getPackageName();
        }
    }
}
