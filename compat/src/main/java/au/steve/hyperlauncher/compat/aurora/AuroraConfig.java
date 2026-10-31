package au.steve.hyperlauncher.compat.aurora;

import android.content.Context;
import android.content.SharedPreferences;

public final class AuroraConfig {
    public static final String PREFS = "aurora";

    private final SharedPreferences prefs;

    public AuroraConfig(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean("referenceStyleV1", false)) {
            applyPreset(0);
            prefs.edit().putBoolean("referenceStyleV1", true).apply();
        }
    }

    public SharedPreferences prefs() {
        return prefs;
    }

    public int clockPreset() { return prefs.getInt("clockPreset", 0); }
    public int clockStyle() { return prefs.getInt("clockStyle", 0); }
    public int clockSize() { return prefs.getInt("clockSize", 168); }
    public int clockY() { return prefs.getInt("clockY", 0); }
    public int clockColor() { return prefs.getInt("clockColor", 0xE8F4FBFF); }
    public boolean autoClockColor() { return prefs.getBoolean("autoClockColor", true); }
    public boolean clock12() { return prefs.getBoolean("clock12", false); }
    public float clockWidth() { return prefs.getFloat("clockWidth", 0.48f); }
    public float clockWeight() { return prefs.getFloat("clockWeight", 0.20f); }
    public float clockSpacing() { return prefs.getFloat("clockSpacing", 0.35f); }
    public float clockOpacity() { return prefs.getFloat("clockOpacity", 0.64f); }
    public float wallpaperZoom() { return prefs.getFloat("wallpaperZoom", 1f); }
    public float wallpaperFocusX() { return prefs.getFloat("wallpaperFocusX", 0.5f); }
    public float wallpaperFocusY() { return prefs.getFloat("wallpaperFocusY", 0.5f); }
    public float dimAmount() { return prefs.getFloat("dimAmount", 0.18f); }
    public boolean depthEnabled() { return prefs.getBoolean("depthEnabled", true); }
    public float maskFeather() { return prefs.getFloat("maskFeather", 0.5f); }
    public float maskThreshold() { return prefs.getFloat("maskThreshold", 0.30f); }
    public boolean hideNotificationContent() { return prefs.getBoolean("hideNotificationContent", true); }
    public boolean showNotifications() { return prefs.getBoolean("showNotifications", true); }
    public String wallpaper() { return prefs.getString("wallpaper", null); }
    public String foreground() { return prefs.getString("foreground", null); }
    public String autoForeground() { return prefs.getString("autoForeground", null); }
    public String depthStatus() { return prefs.getString("depthStatus", ""); }

    public void applyPreset(int preset) {
        prefs.edit().putInt("clockPreset", preset).putInt("clockStyle", preset == 3 ? 2 : 0)
                .putInt("clockSize", 168).putInt("clockY", 0)
                .putFloat("clockWidth", .48f).putFloat("clockWeight", .20f)
                .putFloat("clockSpacing", .35f).putFloat("clockOpacity", preset == 4 ? .46f : .64f)
                .putBoolean("autoClockColor", true).apply();
    }

    public void resetClock() {
        applyPreset(0);
        prefs.edit()
                .putInt("clockPreset", 0)
                .putInt("clockStyle", 0)
                .putInt("clockSize", 168)
                .putInt("clockY", 0)
                .putInt("clockColor", 0xE8F4FBFF)
                .putBoolean("autoClockColor", true)
                .putBoolean("clock12", false)
                .putFloat("clockWidth", 0.48f)
                .putFloat("clockWeight", 0.20f)
                .putFloat("clockSpacing", 0.35f)
                .putFloat("clockOpacity", 0.64f)
                .apply();
    }

    public void resetAll() {
        resetClock();
        prefs.edit()
                .putFloat("wallpaperZoom", 1f)
                .putFloat("wallpaperFocusX", 0.5f)
                .putFloat("wallpaperFocusY", 0.5f)
                .putFloat("dimAmount", 0.18f)
                .putBoolean("depthEnabled", true)
                .putFloat("maskFeather", 0.5f)
                .putFloat("maskThreshold", 0.30f)
                .putBoolean("hideNotificationContent", true)
                .putBoolean("showNotifications", true)
                .apply();
    }
}
