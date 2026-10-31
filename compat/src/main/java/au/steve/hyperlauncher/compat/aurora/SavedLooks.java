package au.steve.hyperlauncher.compat.aurora;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Local-only visual snapshots; notification privacy and permissions never change with a design. */
public final class SavedLooks {
    private static final Set<String> KEYS = new HashSet<>(Arrays.asList(
            "clockPreset", "clockStyle", "clockSize", "clockX", "clockY", "clockYRatio", "clockStretch",
            "clockMaterial", "hoursMaterial", "minutesMaterial", "outlineOverSubject", "clockColor", "autoClockColor", "clock12", "clockWidth", "clockWeight",
            "clockSpacing", "clockOpacity", "dateStyle", "showSeconds", "wallpaper", "foreground",
            "autoForeground", "videoWallpaper", "wallpaperZoom", "wallpaperFocusX", "wallpaperFocusY",
            "headline", "dimAmount", "depthEnabled", "photoMotion", "wakeAnimation"));
    private static final Set<String> INTEGER_KEYS = new HashSet<>(Arrays.asList(
            "clockPreset", "clockStyle", "clockSize", "clockY", "clockMaterial", "hoursMaterial", "minutesMaterial", "outlineOverSubject", "clockColor", "dateStyle"));
    public static final class Look {
        public final String name;
        public final JSONObject values;
        public Look(String name, JSONObject values) { this.name = name; this.values = values; }
    }
    private final SharedPreferences prefs;
    public SavedLooks(AuroraConfig config) { prefs = config.prefs(); }

    public JSONObject snapshot() {
        JSONObject values = new JSONObject();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (KEYS.contains(entry.getKey())) try { values.put(entry.getKey(), entry.getValue()); }
            catch (org.json.JSONException ignored) { }
        }
        return values;
    }
    public List<Look> list() {
        List<Look> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString("savedLooks", "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject look = array.getJSONObject(i);
                result.add(new Look(look.getString("name"), look.getJSONObject("values")));
            }
        } catch (org.json.JSONException ignored) { }
        return result;
    }
    public void save(String name) {
        List<Look> looks = list();
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) clean = "Design " + (looks.size() + 1);
        if (clean.length() > 40) clean = clean.substring(0, 40);
        final String selected = clean;
        looks.removeIf(look -> look.name.equals(selected));
        looks.add(0, new Look(clean, snapshot()));
        while (looks.size() > 8) looks.remove(looks.size() - 1);
        write(looks);
    }
    public void delete(String name) {
        List<Look> looks = list(); looks.removeIf(look -> look.name.equals(name)); write(looks);
    }
    private void write(List<Look> looks) {
        JSONArray array = new JSONArray();
        for (Look look : looks) try {
            array.put(new JSONObject().put("name", look.name).put("values", look.values));
        } catch (org.json.JSONException ignored) { }
        prefs.edit().putString("savedLooks", array.toString()).apply();
    }
    public void apply(Look look) { applyValues(prefs, look.values); }
    private static void applyValues(SharedPreferences target, JSONObject values) {
        SharedPreferences.Editor editor = target.edit();
        for (String key : KEYS) {
            editor.remove(key);
            Object value = values.opt(key);
            if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
            else if (value instanceof Number) {
                if (INTEGER_KEYS.contains(key)) editor.putInt(key, ((Number) value).intValue());
                else editor.putFloat(key, ((Number) value).floatValue());
            }
            else if (value instanceof String) editor.putString(key, (String) value);
        }
        editor.apply();
    }
    public static AuroraConfig previewConfig(Context context, Look look) {
        SharedPreferences temporary = context.getSharedPreferences("aurora-look-preview", Context.MODE_PRIVATE);
        temporary.edit().clear().putBoolean("referenceStyleV1", true).apply();
        applyValues(temporary, look.values);
        return new AuroraConfig(temporary);
    }
}
