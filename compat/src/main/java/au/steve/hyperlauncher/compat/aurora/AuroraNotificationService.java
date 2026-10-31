package au.steve.hyperlauncher.compat.aurora;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class AuroraNotificationService extends NotificationListenerService {
    @Override public void onListenerConnected() {
        NotificationHub.get().setConnected(true);
        NotificationHub.get().setDismissAction(this::cancelNotification);
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active == null) return;
            for (StatusBarNotification sbn : active) NotificationHub.get().upsert(sbn, appName(sbn));
        } catch (SecurityException ignored) {
            NotificationHub.get().setConnected(false);
        }
    }

    @Override public void onListenerDisconnected() {
        NotificationHub.get().setDismissAction(null);
        NotificationHub.get().setConnected(false);
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        NotificationHub.get().upsert(sbn, appName(sbn));
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn != null) NotificationHub.get().remove(sbn.getKey());
    }

    @Override public void onDestroy() {
        NotificationHub.get().setDismissAction(null);
        NotificationHub.get().setConnected(false);
        super.onDestroy();
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
