package au.steve.hyperlauncher.compat.aurora;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Editing uses a separate preferences file; Cancel never has to undo live service state. */
public final class EditorDraft {
    private EditorDraft() {}
    public static AuroraConfig open(Context context, boolean restore) {
        SharedPreferences draft = context.getSharedPreferences("aurora-editor-draft", Context.MODE_PRIVATE);
        if (!restore) copy(new AuroraConfig(context).prefs(), draft, false);
        return new AuroraConfig(draft);
    }
    public static void apply(Context context, AuroraConfig draft) {
        copy(draft.prefs(), new AuroraConfig(context).prefs(), true);
    }
    private static void copy(SharedPreferences source, SharedPreferences target, boolean preserveRuntime) {
        String wakeStatus = target.getString("autoWakeStatus", null);
        SharedPreferences.Editor editor = target.edit().clear();
        for (Map.Entry<String, ?> entry : source.getAll().entrySet()) put(editor, entry.getKey(), entry.getValue());
        if (preserveRuntime && wakeStatus != null) editor.putString("autoWakeStatus", wakeStatus);
        editor.apply();
    }
    @SuppressWarnings("unchecked")
    private static void put(SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof String) editor.putString(key, (String)value);
        else if (value instanceof Integer) editor.putInt(key, (Integer)value);
        else if (value instanceof Long) editor.putLong(key, (Long)value);
        else if (value instanceof Float) editor.putFloat(key, (Float)value);
        else if (value instanceof Boolean) editor.putBoolean(key, (Boolean)value);
        else if (value instanceof Set) editor.putStringSet(key, new HashSet<>((Set<String>)value));
    }
}
