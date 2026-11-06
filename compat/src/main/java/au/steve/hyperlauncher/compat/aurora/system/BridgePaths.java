package au.steve.hyperlauncher.compat.aurora.system;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pure-Java naming rules for the System UI bridge. Image preferences (content URIs or private
 * file paths that System UI cannot open) are rewritten to provider URIs that this app serves.
 * The original file name is kept as the last segment so renderer checks such as
 * {@code DepthEngine.isCurrentMask} behave identically inside System UI.
 */
public final class BridgePaths {
    private BridgePaths() {}

    public static final String AUTHORITY = "au.steve.hyperlauncher.compat.aurora";
    public static final List<String> IMAGE_KEYS = Collections.unmodifiableList(Arrays.asList("wallpaper", "foreground", "autoForeground"));
    /** Extra rows added to the snapshot for the System UI layer itself. */
    public static final String MODULE_ENABLED = "__module_enabled";
    public static final String HIDE_STOCK = "__hide_stock_clock";
    public static final String RETRY_TOKEN = "__retry_token";

    public static boolean isImageKey(String key) { return IMAGE_KEYS.contains(key); }

    /** Last path segment of a file path or URI, restricted to safe characters. */
    public static String fileName(String original) {
        if (original == null) return "image";
        String s = original;
        int q = s.indexOf('?');
        if (q >= 0) s = s.substring(0, q);
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        int slash = s.lastIndexOf('/');
        String name = slash >= 0 ? s.substring(slash + 1) : s;
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return name.isEmpty() ? "image" : name;
    }

    /** content://AUTHORITY/file/{key}/{version}/{name} */
    public static String bridgeUri(String key, String original, long version) {
        return "content://" + AUTHORITY + "/file/" + key + "/" + Long.toHexString(version) + "/" + fileName(original);
    }

    /** Key from the path segments of a bridge URI, or null when it is not one. */
    public static String keyOf(List<String> segments) {
        if (segments == null || segments.size() < 2 || !"file".equals(segments.get(0))) return null;
        String key = segments.get(1);
        return isImageKey(key) ? key : null;
    }

    /** Stable version for a content URI string (no file timestamp available). */
    public static long versionOf(String original, long lastModified) {
        long h = 1125899906842597L;
        if (original != null) for (int i = 0; i < original.length(); i++) h = 31 * h + original.charAt(i);
        return h ^ (lastModified * 0x9E3779B97F4A7C15L);
    }
}
