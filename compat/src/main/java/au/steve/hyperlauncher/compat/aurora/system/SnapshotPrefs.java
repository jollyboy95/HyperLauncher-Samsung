package au.steve.hyperlauncher.compat.aurora.system;

import android.content.SharedPreferences;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Read-only SharedPreferences backed by a snapshot from {@link ModuleBridgeProvider}. Lets the
 * existing {@code AuroraConfig}/{@code AuroraSceneView} run unchanged inside System UI. Writes
 * are ignored: the app is the only place settings change.
 */
public final class SnapshotPrefs implements SharedPreferences {
    private volatile Map<String, Object> values = Collections.emptyMap();

    public void replace(Map<String, Object> next) { values = Collections.unmodifiableMap(new HashMap<>(next)); }
    public Map<String, Object> values() { return values; }

    /** Parses one provider row (key, type, value). */
    public static void put(Map<String, Object> into, String key, String type, String value) {
        if (key == null || type == null || value == null) return;
        try {
            switch (type) {
                case "b": into.put(key, "1".equals(value)); break;
                case "i": into.put(key, Integer.parseInt(value)); break;
                case "l": into.put(key, Long.parseLong(value)); break;
                case "f": into.put(key, Float.parseFloat(value)); break;
                default: into.put(key, value);
            }
        } catch (NumberFormatException ignored) { }
    }

    private <T> T get(String key, Class<T> type, T def) {
        Object v = values.get(key);
        return type.isInstance(v) ? type.cast(v) : def;
    }

    @Override public Map<String, ?> getAll() { return values; }
    @Override public String getString(String key, String def) { return get(key, String.class, def); }
    @Override public Set<String> getStringSet(String key, Set<String> def) { return def; }
    @Override public int getInt(String key, int def) { return get(key, Integer.class, def); }
    @Override public long getLong(String key, long def) { return get(key, Long.class, def); }
    @Override public float getFloat(String key, float def) { return get(key, Float.class, def); }
    @Override public boolean getBoolean(String key, boolean def) { return get(key, Boolean.class, def); }
    @Override public boolean contains(String key) { return values.containsKey(key); }
    @Override public Editor edit() { return new NoopEditor(); }
    @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }
    @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }

    private static final class NoopEditor implements Editor {
        @Override public Editor putString(String k, String v) { return this; }
        @Override public Editor putStringSet(String k, Set<String> v) { return this; }
        @Override public Editor putInt(String k, int v) { return this; }
        @Override public Editor putLong(String k, long v) { return this; }
        @Override public Editor putFloat(String k, float v) { return this; }
        @Override public Editor putBoolean(String k, boolean v) { return this; }
        @Override public Editor remove(String k) { return this; }
        @Override public Editor clear() { return this; }
        @Override public boolean commit() { return true; }
        @Override public void apply() { }
    }
}
