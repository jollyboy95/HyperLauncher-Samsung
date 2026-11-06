package au.steve.hyperlauncher.compat.aurora.system;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import au.steve.hyperlauncher.compat.aurora.AuroraConfig;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

/**
 * Read-only bridge for the System UI keyguard module. Serves a snapshot of the Aurora settings
 * and the wallpaper / depth images (which only this app may open). Restricted to this app,
 * the system uid and com.android.systemui.
 */
public final class ModuleBridgeProvider extends ContentProvider {
    public static final Uri BASE = Uri.parse("content://" + BridgePaths.AUTHORITY);
    public static final Uri PREFS = Uri.withAppendedPath(BASE, "prefs");

    private SharedPreferences aurora;
    private ModuleState module;
    // Strong reference: SharedPreferences only holds listeners weakly.
    private final SharedPreferences.OnSharedPreferenceChangeListener changed = (p, k) -> notifyChanged();

    @Override public boolean onCreate() {
        Context c = getContext();
        aurora = c.getSharedPreferences(AuroraConfig.PREFS, Context.MODE_PRIVATE);
        module = new ModuleState(c);
        aurora.registerOnSharedPreferenceChangeListener(changed);
        module.prefs().registerOnSharedPreferenceChangeListener((p, k) -> { if (!"lastSeen".equals(k) && !"status".equals(k)) notifyChanged(); });
        return true;
    }

    private void notifyChanged() { getContext().getContentResolver().notifyChange(BASE, null); }

    private void enforceCaller() {
        int uid = Binder.getCallingUid();
        if (uid == Process.myUid() || uid == Process.SYSTEM_UID) return;
        String[] pkgs = getContext().getPackageManager().getPackagesForUid(uid);
        if (pkgs != null) for (String p : pkgs) if ("com.android.systemui".equals(p)) return;
        throw new SecurityException("Aurora settings are only available to System UI");
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        enforceCaller();
        if (!PREFS.equals(uri)) return null;
        MatrixCursor out = new MatrixCursor(new String[]{"key", "type", "value"});
        for (Map.Entry<String, ?> e : aurora.getAll().entrySet()) {
            Object v = e.getValue();
            String key = e.getKey();
            if (v instanceof String && BridgePaths.isImageKey(key)) {
                String original = (String) v;
                out.addRow(new Object[]{key, "s", BridgePaths.bridgeUri(key, original, version(original))});
            } else if (v instanceof Boolean) out.addRow(new Object[]{key, "b", ((Boolean) v) ? "1" : "0"});
            else if (v instanceof Integer) out.addRow(new Object[]{key, "i", String.valueOf(v)});
            else if (v instanceof Long) out.addRow(new Object[]{key, "l", String.valueOf(v)});
            else if (v instanceof Float) out.addRow(new Object[]{key, "f", String.valueOf(v)});
            else if (v instanceof String) out.addRow(new Object[]{key, "s", v});
            // String sets are not used by the renderer and are skipped.
        }
        out.addRow(new Object[]{BridgePaths.MODULE_ENABLED, "b", module.layerEnabled() ? "1" : "0"});
        out.addRow(new Object[]{BridgePaths.HIDE_STOCK, "b", module.hideStockClock() ? "1" : "0"});
        out.addRow(new Object[]{BridgePaths.RETRY_TOKEN, "l", String.valueOf(module.retryToken())});
        return out;
    }

    private static long version(String original) {
        long modified = 0;
        if (original.startsWith("/")) modified = new File(original).lastModified();
        else if (original.startsWith("file:")) modified = new File(Uri.parse(original).getPath()).lastModified();
        return BridgePaths.versionOf(original, modified);
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        enforceCaller();
        if (!"r".equals(mode)) throw new FileNotFoundException("read-only");
        String key = BridgePaths.keyOf(uri.getPathSegments());
        String value = key == null ? null : aurora.getString(key, null);
        if (value == null) throw new FileNotFoundException(uri.toString());
        if (value.startsWith("/")) return ParcelFileDescriptor.open(new File(value), ParcelFileDescriptor.MODE_READ_ONLY);
        if (value.startsWith("file:")) return ParcelFileDescriptor.open(new File(Uri.parse(value).getPath()), ParcelFileDescriptor.MODE_READ_ONLY);
        // A content URI this app holds a persisted grant for: open it as this app.
        long ident = Binder.clearCallingIdentity();
        try {
            ParcelFileDescriptor pfd = getContext().getContentResolver().openFileDescriptor(Uri.parse(value), "r");
            if (pfd == null) throw new FileNotFoundException(value);
            return pfd;
        } catch (SecurityException e) {
            throw new FileNotFoundException("Photo permission expired; choose the wallpaper again in Aurora");
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        enforceCaller();
        if ("hello".equals(method)) module.markSeen(arg);
        return null;
    }

    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
