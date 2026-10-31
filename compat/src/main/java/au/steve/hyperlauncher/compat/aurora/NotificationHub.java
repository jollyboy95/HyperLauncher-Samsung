package au.steve.hyperlauncher.compat.aurora;

import android.app.Notification;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** Live notifications from NotificationListenerService. Never invents placeholder cards. */
public final class NotificationHub {
    public static final class Item {
        public final String key;
        public final String packageName;
        public final String appName;
        public final String title;
        public final String text;
        public final long when;
        public final android.graphics.drawable.Icon icon;

        public Item(String key, String packageName, String appName, String title, String text, long when,
                    android.graphics.drawable.Icon icon) {
            this.key = key;
            this.packageName = packageName;
            this.appName = appName;
            this.title = title;
            this.text = text;
            this.when = when;
            this.icon = icon;
        }
    }

    public interface Listener {
        void onNotificationsChanged(List<Item> items);
    }

    private static final NotificationHub INSTANCE = new NotificationHub();
    private final Map<String, Item> items = new LinkedHashMap<>();
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean connected;

    public static NotificationHub get() {
        return INSTANCE;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean value) {
        connected = value;
        if (!value) {
            synchronized (items) { items.clear(); }
            dispatch();
        }
    }

    public void upsert(StatusBarNotification sbn, String appName) {
        if (sbn == null || sbn.isOngoing()) return;
        Notification n = sbn.getNotification();
        if (n == null) return;
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        CharSequence title = n.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = n.extras.getCharSequence(Notification.EXTRA_TEXT);
        if (text == null) text = n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        Item item = new Item(sbn.getKey(), sbn.getPackageName(), appName == null ? sbn.getPackageName() : appName,
                title == null ? "" : title.toString(), text == null ? "" : text.toString(),
                n.when == 0 ? sbn.getPostTime() : n.when, n.getSmallIcon());
        synchronized (items) {
            items.remove(item.key);
            items.put(item.key, item);
            while (items.size() > 8) {
                String first = items.keySet().iterator().next();
                items.remove(first);
            }
        }
        dispatch();
    }

    public void remove(String key) {
        synchronized (items) { items.remove(key); }
        dispatch();
    }

    public List<Item> snapshot() {
        synchronized (items) { return new ArrayList<>(items.values()); }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
        listener.onNotificationsChanged(snapshot());
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void dispatch() {
        List<Item> copy = snapshot();
        main.post(() -> {
            for (Listener listener : listeners) listener.onNotificationsChanged(copy);
        });
    }
}
