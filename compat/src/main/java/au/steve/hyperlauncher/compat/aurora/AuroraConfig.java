package au.steve.hyperlauncher.compat.aurora;

import android.content.Context;
import android.content.SharedPreferences;

public final class AuroraConfig {
    public static final String PREFS = "aurora";

    private final SharedPreferences prefs;

    public AuroraConfig(Context context) {
        this(context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE));
    }

    public AuroraConfig(SharedPreferences settings) {
        prefs = settings;
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
    public int clockYForHeight(float height) {
        return prefs.contains("clockYRatio") ? Math.round(prefs.getFloat("clockYRatio", 0) * height) : clockY();
    }
    public void saveClockPosition(float x, int y, float height) {
        saveClockPosition(x, y);
        prefs.edit().putFloat("clockYRatio", y / Math.max(1, height)).apply();
    }
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

    public float clockX() { return prefs.getFloat("clockX", 0f); }
    public float clockStretch() { return prefs.getFloat("clockStretch", 1f); }
    public int hoursMaterial() { return prefs.getInt("hoursMaterial", -1); }
    public int minutesMaterial() { return prefs.getInt("minutesMaterial", -1); }
    public boolean outlineOverSubject() { return prefs.getBoolean("outlineOverSubject", true); }
    public int clockMaterial() { return prefs.getInt("clockMaterial", 0); }
    public int dateStyle() { return prefs.getInt("dateStyle", 0); }
    public boolean showSeconds() { return prefs.getBoolean("showSeconds", false); }
    public boolean motionEnabled() { return prefs.getBoolean("motionEnabled", true); }
    public boolean photoMotion() { return prefs.getBoolean("photoMotion", false); }
    public boolean autoWakeEnabled() { return prefs.getBoolean("autoWakeEnabled", false); }
    public boolean wakeAnimation() { return prefs.getBoolean("wakeAnimation", true); }
    public String videoWallpaper() { return prefs.getString("videoWallpaper", null); }
    public String signature() { return prefs.getString("signature", "Never stop exploring"); }
    public boolean showSignature() { return prefs.getBoolean("showSignature", true); }
    public String movieTitle() { return prefs.getString("movieTitle", "UP IN\nTHE AIR"); }
    public boolean showMedia() { return prefs.getBoolean("showMedia", true); }
    public int notificationMode() {
        return prefs.getInt("notificationMode", prefs.getBoolean("notificationStack", true) ? 1 : 0);
    }
    public String headline() { return prefs.getString("headline", ""); }
    public boolean notificationStack() { return prefs.getBoolean("notificationStack", true); }
    public boolean statusWidgets() { return prefs.getBoolean("statusWidgets", true); }
    public int leftShortcut() { return prefs.getInt("leftShortcut", 0); }
    public int rightShortcut() { return prefs.getInt("rightShortcut", 1); }
    public void saveClockPosition(float x, int y) {
        prefs.edit().putFloat("clockX", Math.max(-.38f, Math.min(.38f, x))).putInt("clockY", y).apply();
    }

    public void applyPreset(int preset) {
        if (preset == 13) {
            // HyperOS 4 Glass Frame
            prefs.edit().remove("clockYRatio").remove("hoursMaterial").remove("minutesMaterial")
                    .putInt("clockPreset", 13).putInt("clockStyle", 0).putInt("clockMaterial", 1)
                    .putInt("clockSize", 175).putInt("clockY", 0).putFloat("clockX", 0)
                    .putFloat("clockStretch", 1.0f).putFloat("clockWeight", 0.65f)
                    .putFloat("clockOpacity", 0.95f).putInt("clockColor", 0xFFFFFFFF)
                    .putBoolean("autoClockColor", false).putBoolean("showSignature", true).apply();
            return;
        } else if (preset == 14) {
            // Movie: Up In Air
            prefs.edit().remove("clockYRatio").remove("hoursMaterial").remove("minutesMaterial")
                    .putInt("clockPreset", 14).putInt("clockStyle", 0).putInt("clockMaterial", 0)
                    .putInt("clockSize", 168).putInt("clockY", 0).putFloat("clockX", 0)
                    .putFloat("clockStretch", 1.0f).putFloat("clockWeight", 0.90f)
                    .putFloat("clockOpacity", 1.0f).putInt("clockColor", 0xFFFF453A)
                    .putBoolean("autoClockColor", false).apply();
            return;
        } else if (preset == 15) {
            // Movie: Neon Dog / Double Outline with vertical date pill
            prefs.edit().remove("clockYRatio").remove("hoursMaterial").remove("minutesMaterial")
                    .putInt("clockPreset", 15).putInt("clockStyle", 0).putInt("clockMaterial", 4)
                    .putInt("clockSize", 172).putInt("clockY", 0).putFloat("clockX", 0)
                    .putFloat("clockStretch", 1.15f).putFloat("clockWeight", 0.40f)
                    .putFloat("clockOpacity", 0.95f).putInt("clockColor", 0xFFF7D19A)
                    .putBoolean("outlineOverSubject", true).putBoolean("autoClockColor", false).apply();
            return;
        } else if (preset == 16) {
            // Movie: Block Poster
            prefs.edit().remove("clockYRatio").remove("hoursMaterial").remove("minutesMaterial")
                    .putInt("clockPreset", 16).putInt("clockStyle", 0).putInt("clockMaterial", 0)
                    .putInt("clockSize", 168).putInt("clockY", 0).putFloat("clockX", 0)
                    .putFloat("clockStretch", 1.0f).putFloat("clockWeight", 0.95f)
                    .putFloat("clockOpacity", 1.0f).putInt("clockColor", 0xFF00E5FF)
                    .putBoolean("autoClockColor", false).apply();
            return;
        } else if (preset == 17) {
            // Classic HyperOS Left
            prefs.edit().remove("clockYRatio").remove("hoursMaterial").remove("minutesMaterial")
                    .putInt("clockPreset", 17).putInt("clockStyle", 0).putInt("clockMaterial", 0)
                    .putInt("clockSize", 180).putInt("clockY", 0).putFloat("clockX", 0)
                    .putFloat("clockStretch", 1.0f).putFloat("clockWeight", 0.85f)
                    .putFloat("clockOpacity", 0.95f).putInt("clockColor", 0xFFFFFFFF)
                    .putBoolean("autoClockColor", false).putBoolean("showSignature", true).apply();
            return;
        } else if (preset == 18) {
            // Ultra-Tall Art
            prefs.edit().remove("clockYRatio").remove("hoursMaterial").remove("minutesMaterial")
                    .putInt("clockPreset", 18).putInt("clockStyle", 0).putInt("clockMaterial", 0)
                    .putInt("clockSize", 168).putInt("clockY", 0).putFloat("clockX", 0)
                    .putFloat("clockStretch", 1.6f).putFloat("clockWeight", 0.25f)
                    .putFloat("clockOpacity", 0.90f).putInt("clockColor", 0xFFFFFFFF)
                    .putBoolean("autoClockColor", false).apply();
            return;
        }
        if (preset >= 5) {
            prefs.edit().remove("clockYRatio").remove("hoursMaterial").remove("minutesMaterial").putInt("clockPreset", preset).putInt("clockStyle", 0)
                    .putInt("clockSize", 168).putInt("clockY", 0).putFloat("clockX", 0)
                    .putFloat("clockStretch", 1).putFloat("clockWeight", preset == 9 || preset == 12 ? .25f : preset == 11 ? .2f : .95f)
                    .putFloat("clockOpacity", preset == 9 ? .78f : .92f).putInt("clockColor", 0xFFFFFFFF)
                    .putBoolean("autoClockColor", false).apply();
            return;
        }
        prefs.edit().remove("clockYRatio").remove("hoursMaterial").remove("minutesMaterial").putInt("clockPreset", preset).putInt("clockStyle", preset == 3 ? 2 : 0)
                .putInt("clockSize", 168).putInt("clockY", 0)
                .putFloat("clockWidth", .48f).putFloat("clockWeight", .20f)
                .putFloat("clockSpacing", .35f).putFloat("clockOpacity", preset == 4 ? .46f : .64f)
                .putBoolean("autoClockColor", true).apply();
    }

    public void resetClock() {
        applyPreset(0);
        prefs.edit().remove("clockX").remove("clockStretch").remove("clockMaterial").remove("hoursMaterial").remove("minutesMaterial").remove("outlineOverSubject").remove("dateStyle").remove("showSeconds")
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
        prefs.edit().remove("photoMotion").remove("wakeAnimation").remove("showMedia").remove("notificationStack").remove("notificationMode")
                .remove("statusWidgets").remove("leftShortcut").remove("rightShortcut").remove("videoWallpaper")
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
