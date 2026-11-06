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
    void markSeen() { prefs.edit().putLong("lastSeen", System.currentTimeMillis()).apply(); }

    /** True when the System UI module is loaded and drawing Aurora inside the real keyguard. */
    public boolean systemLayerActive() {
        long seen = lastSeen();
        return layerEnabled() && seen > 0 && System.currentTimeMillis() - seen < ACTIVE_WINDOW_MS;
    }
}
