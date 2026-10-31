package au.steve.hyperlauncher.compat.aurora;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class AuroraNotificationService extends NotificationListenerService {
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable reconcile = this::refreshActive;
    private void refreshActive() {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active == null) return;
            java.util.Set<String> keys = new java.util.HashSet<>();
            for (StatusBarNotification item : active) keys.add(item.getKey());
            for (NotificationHub.Item item : NotificationHub.get().snapshot())
                if (!keys.contains(item.key)) NotificationHub.get().remove(item.key);
        } catch (SecurityException disconnected) { NotificationHub.get().setConnected(false); }
    }
    private void reconcileAfterDismissal() {
        main.removeCallbacks(reconcile);
        main.postDelayed(reconcile, 400);
    }
    private void dismissOne(String key) {
        cancelNotification(key);
        reconcileAfterDismissal();
    }
    private void clearDismissible() {
        StatusBarNotification[] active = getActiveNotifications();
        if (active == null) throw new IllegalStateException("Notification listener is disconnected");
        for (StatusBarNotification notification : active)
            if (notification.isClearable() && !notification.isOngoing()) cancelNotification(notification.getKey());
        reconcileAfterDismissal();
    }
    @Override public void onListenerConnected() {
        NotificationHub.get().setConnected(true);
        NotificationHub.get().setDismissAction(this::dismissOne);
        NotificationHub.get().setClearAllAction(this::clearDismissible);
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active == null) return;
            for (StatusBarNotification sbn : active) NotificationHub.get().upsert(sbn, appName(sbn));
        } catch (SecurityException ignored) {
            NotificationHub.get().setConnected(false);
        }
    }

    @Override public void onListenerDisconnected() {
        main.removeCallbacksAndMessages(null);
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
        main.removeCallbacksAndMessages(null);
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
