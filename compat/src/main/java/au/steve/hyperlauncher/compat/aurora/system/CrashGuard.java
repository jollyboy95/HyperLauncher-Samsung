package au.steve.hyperlauncher.compat.aurora.system;

/**
 * Pure-Java crash guard for the System UI layer.
 *
 * <p>Armed every time the layer is about to be shown (before it is added to Samsung's view
 * tree). If System UI dies before the layer has been visible for the healthy period, the next
 * attempt sees the leftover count. After {@link #MAX_UNHEALTHY} unhealthy attempts the layer
 * is not shown again until the user presses "Retry" in the app (new retry token). Two System UI
 * restarts are far below the threshold at which Android's RescueParty reboots the phone.</p>
 */
public final class CrashGuard {
    public static final int MAX_UNHEALTHY = 2;
    public static final long HEALTHY_MS = 20_000;

    public interface Store {
        int getInt(String key, int def);
        long getLong(String key, long def);
        void putIntNow(String key, int value);
        void putLongNow(String key, long value);
    }

    private final Store store;

    public CrashGuard(Store store) { this.store = store; }

    /** A new retry token from the app clears a previous pause. */
    public void acceptRetryToken(long token) {
        if (token != store.getLong("retryToken", 0)) {
            store.putLongNow("retryToken", token);
            store.putIntNow("unhealthy", 0);
        }
    }

    public boolean paused() { return store.getInt("unhealthy", 0) >= MAX_UNHEALTHY; }

    /** Call immediately before showing; returns false if the layer must stay off. Persists synchronously. */
    public boolean arm() {
        int n = store.getInt("unhealthy", 0);
        if (n >= MAX_UNHEALTHY) return false;
        store.putIntNow("unhealthy", n + 1);
        return true;
    }

    /** The layer drew for {@link #HEALTHY_MS} without System UI dying. */
    public void healthy() { store.putIntNow("unhealthy", 0); }
}
