package au.steve.hyperlauncher.compat.aurora.system;

import android.content.Context;
import android.content.SharedPreferences;

/** App-side switches for the System UI keyguard layer, plus the module's last check-in. */
public final class ModuleState {
    public static final String PREFS = "aurora_module";
    /** The module counts as active if System UI checked in within this window. */
    private static final long ACTIVE_WINDOW_MS = 7L * 24 * 60 * 60 * 1000;

    private final SharedPreferences prefs;

    public ModuleState(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public SharedPreferences prefs() { return prefs; }
    public boolean layerEnabled() { return prefs.getBoolean("enabled", true); }
    public boolean hideStockClock() { return prefs.getBoolean("hideStock", true); }
    public void setLayerEnabled(boolean v) { prefs.edit().putBoolean("enabled", v).apply(); }
    public void setHideStockClock(boolean v) { prefs.edit().putBoolean("hideStock", v).apply(); }
    public long lastSeen() { return prefs.getLong("lastSeen", 0); }
    /** Status reported by System UI: active, off, paused, render-error or error: ... */
    public String status() { return prefs.getString("status", ""); }
    void markSeen(String status) {
        SharedPreferences.Editor e = prefs.edit().putLong("lastSeen", System.currentTimeMillis());
        if (status != null) e.putString("status", status.length() > 80 ? status.substring(0, 80) : status);
        e.apply();
    }
    public long retryToken() { return prefs.getLong("retryToken", 0); }
    /** Lets a layer that paused itself after crashes try again. */
    public void retry() { prefs.edit().putLong("retryToken", System.currentTimeMillis()).putString("status", "retrying").apply(); }

    /** True when the System UI module is loaded and drawing Aurora inside the real keyguard. */
    public boolean systemLayerActive() {
        long seen = lastSeen();
        return layerEnabled() && "active".equals(status()) && seen > 0 && System.currentTimeMillis() - seen < ACTIVE_WINDOW_MS;
    }
}
