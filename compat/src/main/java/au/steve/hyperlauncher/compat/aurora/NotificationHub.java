package au.steve.hyperlauncher.compat.aurora;

import android.app.Notification;
import android.app.PendingIntent;
import java.util.function.Consumer;
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
        public final PendingIntent contentIntent;
        public final boolean dismissible;

        public Item(String key, String packageName, String appName, String title, String text, long when,
                    android.graphics.drawable.Icon icon) {
            this(key, packageName, appName, title, text, when, icon, null, true);
        }

        public Item(String key, String packageName, String appName, String title, String text, long when,
                    android.graphics.drawable.Icon icon, PendingIntent contentIntent, boolean dismissible) {
            this.contentIntent = contentIntent;
            this.dismissible = dismissible;
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
    private volatile Consumer<String> dismissAction;
    private volatile Runnable clearAllAction;
    public enum DismissResult { REQUESTED, NOT_CLEARABLE, UNAVAILABLE }

    public void setDismissAction(Consumer<String> action) { dismissAction = action; }
    public void setClearAllAction(Runnable action) { clearAllAction = action; }
    public DismissResult dismiss(Item item) {
        if (!item.dismissible) return DismissResult.NOT_CLEARABLE;
        Consumer<String> action = dismissAction;
        if (action == null) return DismissResult.UNAVAILABLE;
        try { action.accept(item.key); return DismissResult.REQUESTED; }
        catch (SecurityException | IllegalStateException unavailable) { return DismissResult.UNAVAILABLE; }
    }
    public DismissResult clearAll() {
        Runnable action = clearAllAction;
        if (action == null) return DismissResult.UNAVAILABLE;
        try { action.run(); return DismissResult.REQUESTED; }
        catch (SecurityException | IllegalStateException unavailable) { return DismissResult.UNAVAILABLE; }
    }

    public static NotificationHub get() {
        return INSTANCE;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean value) {
        connected = value;
        if (!value) {
            dismissAction = null; clearAllAction = null;
            synchronized (items) { items.clear(); }
            dispatch();
        }
    }

    public void upsert(StatusBarNotification sbn, String appName) {
        if (sbn == null) return;
        if (sbn.isOngoing()) { remove(sbn.getKey()); return; }
        Notification n = sbn.getNotification();
        if (n == null) return;
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) { remove(sbn.getKey()); return; }
        CharSequence title = n.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = n.extras.getCharSequence(Notification.EXTRA_TEXT);
        if (text == null) text = n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        Item item = new Item(sbn.getKey(), sbn.getPackageName(), appName == null ? sbn.getPackageName() : appName,
                title == null ? "" : title.toString(), text == null ? "" : text.toString(),
                n.when == 0 ? sbn.getPostTime() : n.when, n.getSmallIcon(), n.contentIntent, sbn.isClearable());
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
        synchronized (items) { List<Item> result = new ArrayList<>(items.values()); java.util.Collections.reverse(result); return result; }
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
