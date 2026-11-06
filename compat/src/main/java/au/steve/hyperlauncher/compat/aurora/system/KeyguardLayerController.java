package au.steve.hyperlauncher.compat.aurora.system;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import au.steve.hyperlauncher.compat.AuroraSceneView;
import au.steve.hyperlauncher.compat.aurora.AuroraConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Runs inside System UI. Places the app's own {@link AuroraSceneView} (the exact renderer used by
 * the app and editor) at the bottom of Samsung's keyguard root, so Samsung's notifications,
 * lock icon, fingerprint UI, shortcuts and bouncer all stay above it and keep working.
 *
 * <p>Safety: all work is caught; a crash guard pauses the layer after three System UI starts
 * that never reached 15 s of healthy drawing, until the user saves something in the app.</p>
 */
final class KeyguardLayerController {
    private static final String TAG = "AuroraKeyguard";
    private static final WeakHashMap<ViewGroup, KeyguardLayerController> ATTACHED = new WeakHashMap<>();
    private static HandlerThread worker;

    private static final String[] STOCK_ID_HINTS = {"clock", "status_view", "status_area", "facewidget", "face_widget", "keyguard_slice", "smartspace"};
    private static final String[] STOCK_CLASS_HINTS = {"FaceWidget", "Clock", "KeyguardStatus", "KeyguardSlice", "Smartspace"};
    private static final String[] NEVER_HIDE = {"lock_icon", "lockicon", "shortcut", "notification", "indication", "bouncer", "udfps", "aurora"};

    private final ViewGroup root;
    private final SnapshotPrefs prefs = new SnapshotPrefs();
    private final AuroraSceneView scene;
    /** Container actually inserted into Samsung's ConstraintLayout; always carries an id. */
    private final SafeLayer layer;
    private final CrashGuard crashGuard;
    private boolean shown, paused;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Handler bg;
    private final SharedPreferences guard;
    private final Set<View> hidden = Collections.newSetFromMap(new WeakHashMap<>());
    private boolean hideStock, dumped;

    static void attach(ViewGroup root) {
        if (ATTACHED.containsKey(root)) return;
        KeyguardLayerController c = new KeyguardLayerController(root);
        ATTACHED.put(root, c);
        c.start();
    }

    private KeyguardLayerController(ViewGroup root) {
        this.root = root;
        synchronized (KeyguardLayerController.class) {
            if (worker == null) { worker = new HandlerThread("AuroraKeyguard"); worker.start(); }
        }
        bg = new Handler(worker.getLooper());
        Context sys = root.getContext();
        guard = sys.createDeviceProtectedStorageContext().getSharedPreferences("aurora_guard", Context.MODE_PRIVATE);
        crashGuard = new CrashGuard(new CrashGuard.Store() {
            @Override public int getInt(String k, int d) { return guard.getInt(k, d); }
            @Override public long getLong(String k, long d) { return guard.getLong(k, d); }
            @Override public void putIntNow(String k, int v) { guard.edit().putInt(k, v).commit(); }
            @Override public void putLongNow(String k, long v) { guard.edit().putLong(k, v).commit(); }
        });
        scene = new AuroraSceneView(sys, false, new AuroraConfig(prefs), 2400);
        scene.setSystemKeyguard(true);
        layer = new SafeLayer(sys, () -> { hideStock = false; restoreStock(); report("render-error"); });
        // Samsung applies ConstraintSets to KeyguardRootView; every child must have an id.
        layer.setId(View.generateViewId());
        layer.setTag("aurora_layer");
        layer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        layer.addView(scene, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void start() {
        // Nothing is inserted into Samsung's view tree here; that only happens in apply(),
        // after the crash guard has been armed.
        try {
            root.getContext().getContentResolver().registerContentObserver(ModuleBridgeProvider.BASE, true,
                    new ContentObserver(bg) { @Override public void onChange(boolean self) { load(); } });
        } catch (Throwable t) {
            Log.w(TAG, "observer", t);
        }
        bg.post(this::load);
    }

    /** Background: fetch the snapshot. Before the first unlock after boot the app's storage is
     *  locked, the provider is unavailable and Samsung's own clock simply stays visible. */
    private void load() {
        Map<String, Object> next = null;
        ContentResolver cr = root.getContext().getContentResolver();
        try (Cursor c = cr.query(ModuleBridgeProvider.PREFS, null, null, null, null)) {
            if (c != null) {
                next = new HashMap<>();
                while (c.moveToNext()) SnapshotPrefs.put(next, c.getString(0), c.getString(1), c.getString(2));
            }
        } catch (Throwable t) {
            Log.i(TAG, "Aurora settings not available yet: " + t.getMessage());
        }
        final Map<String, Object> snapshot = next;
        main.post(() -> apply(snapshot));
    }

    private void apply(Map<String, Object> snapshot) {
        try {
            if (snapshot != null && snapshot.get(BridgePaths.RETRY_TOKEN) instanceof Long)
                crashGuard.acceptRetryToken((Long) snapshot.get(BridgePaths.RETRY_TOKEN));
            boolean enabled = snapshot != null && Boolean.TRUE.equals(snapshot.get(BridgePaths.MODULE_ENABLED))
                    && snapshot.get("wallpaper") != null && !layer.failed();
            if (enabled && !shown) {
                if (!crashGuard.arm()) {
                    paused = true;
                    Log.w(TAG, "Aurora system layer paused after System UI restarts; tap Retry in Aurora > System");
                    report("paused");
                    return;
                }
                paused = false;
                prefs.replace(snapshot);
                scene.reload();
                root.addView(layer, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                root.getViewTreeObserver().addOnPreDrawListener(preDraw);
                shown = true;
                main.postDelayed(markHealthy, CrashGuard.HEALTHY_MS);
            } else if (enabled) {
                prefs.replace(snapshot);
                scene.reload();
                layer.setVisibility(View.VISIBLE);
            } else if (shown) {
                layer.setVisibility(View.GONE);
            }
            hideStock = enabled && shown && Boolean.TRUE.equals(snapshot.get(BridgePaths.HIDE_STOCK));
            if (!hideStock) restoreStock();
            report(snapshot == null ? null : enabled ? "active" : paused ? "paused" : "off");
            root.invalidate();
        } catch (Throwable t) {
            try { layer.setVisibility(View.GONE); } catch (Throwable ignored) { }
            hideStock = false;
            restoreStock();
            Log.e(TAG, "apply failed; layer hidden", t);
            report("error: " + t.getClass().getSimpleName());
        }
    }

    private final Runnable markHealthy = new Runnable() {
        @Override public void run() {
            if (layer.isAttachedToWindow() && !layer.failed()) crashGuard.healthy();
        }
    };

    /** Tells the app what the System UI layer is doing (shown in Aurora > System). */
    private void report(String status) {
        if (status == null) return;
        bg.post(() -> {
            try { root.getContext().getContentResolver().call(ModuleBridgeProvider.BASE, "hello", status, null); } catch (Throwable ignored) { }
        });
    }

    private final ViewTreeObserver.OnPreDrawListener preDraw = new ViewTreeObserver.OnPreDrawListener() {
        @Override public boolean onPreDraw() {
            try {
                if (!dumped) { dumped = true; Log.i(TAG, "Keyguard tree:\n" + dump(root, 0)); }
                if (hideStock && layer.getVisibility() == View.VISIBLE && !layer.failed()) hideStockViews(root, 0);
            } catch (Throwable t) {
                hideStock = false;
                restoreStock();
                Log.e(TAG, "stock clock hiding disabled", t);
            }
            return true;
        }
    };

    private void hideStockViews(ViewGroup parent, int depth) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View v = parent.getChildAt(i);
            if (v == layer || isProtected(v)) continue;
            if (isStockClock(v)) {
                if (v.getAlpha() != 0f) v.setAlpha(0f);
                hidden.add(v);
            } else if (v instanceof ViewGroup && depth < 3) {
                hideStockViews((ViewGroup) v, depth + 1);
            }
        }
    }

    private void restoreStock() {
        for (View v : hidden) v.setAlpha(1f);
        hidden.clear();
    }

    private static String idName(View v) {
        if (v.getId() == View.NO_ID) return "";
        try {
            return v.getResources().getResourceEntryName(v.getId());
        } catch (Resources.NotFoundException e) {
            return "";
        }
    }

    private static boolean isProtected(View v) {
        String id = idName(v).toLowerCase(Locale.ROOT), cls = v.getClass().getName().toLowerCase(Locale.ROOT);
        for (String n : NEVER_HIDE) if (id.contains(n) || cls.contains(n)) return true;
        return false;
    }

    private static boolean isStockClock(View v) {
        String id = idName(v).toLowerCase(Locale.ROOT), cls = v.getClass().getSimpleName();
        for (String h : STOCK_ID_HINTS) if (id.contains(h)) return true;
        for (String h : STOCK_CLASS_HINTS) if (cls.contains(h)) return true;
        return false;
    }

    /** One-time keyguard tree dump (logcat tag AuroraKeyguard) for tuning on new One UI builds. */
    private static String dump(View v, int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append(v.getClass().getName()).append(" #").append(idName(v))
                .append(" vis=").append(v.getVisibility()).append(" a=").append(v.getAlpha()).append('\n');
        if (v instanceof ViewGroup && depth < 6) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) sb.append(dump(g.getChildAt(i), depth + 1));
        }
        return sb.toString();
    }
}
