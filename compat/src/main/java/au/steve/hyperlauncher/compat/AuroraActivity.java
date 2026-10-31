package au.steve.hyperlauncher.compat;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import au.steve.hyperlauncher.compat.aurora.AuroraConfig;
import au.steve.hyperlauncher.compat.aurora.DepthEngine;
import java.io.File;
import au.steve.hyperlauncher.compat.aurora.NotificationHub;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public final class AuroraActivity extends Activity {
    private AuroraSceneView scene;
    private LinearLayout notices;
    private float downY;
    private boolean torchOn;
    private String torchId;
    private CameraManager cameras;
    private AuroraConfig config;
    private DepthEngine depthEngine;
    private String requestedWallpaper;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        public void run() {
            if (scene != null) scene.invalidate();
            handler.postDelayed(this, 10000);
        }
    };
    private final NotificationHub.Listener noticeListener = this::renderNotices;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        config = new AuroraConfig(this);
        setShowWhenLocked(true);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        scene = new AuroraSceneView(this, false);
        root.addView(scene, new FrameLayout.LayoutParams(-1, -1));
        notices = new LinearLayout(this);
        notices.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        np.leftMargin = dp(22);
        np.rightMargin = dp(22);
        np.bottomMargin = dp(118);
        root.addView(notices, np);

        android.view.GestureDetector gestures = new android.view.GestureDetector(this,
                new android.view.GestureDetector.SimpleOnGestureListener() {
                    @Override public void onLongPress(MotionEvent e) {
                        startActivity(new Intent(AuroraActivity.this, AuroraSettingsActivity.class));
                    }
                });
        scene.setOnTouchListener((v, e) -> {
            gestures.onTouchEvent(e);
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                downY = e.getY();
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_UP) {
                if (downY - e.getY() > 72 * getResources().getDisplayMetrics().density) unlock();
                return true;
            }
            return true;
        });

        cameras = (CameraManager) getSystemService(CAMERA_SERVICE);
        TextView flash = shortcut("✦");
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(dp(58), dp(58), Gravity.BOTTOM | Gravity.START);
        fl.setMargins(dp(28), 0, 0, dp(36));
        flash.setContentDescription("Toggle flashlight");
        flash.setOnClickListener(v -> toggleTorch());
        root.addView(flash, fl);

        TextView camera = shortcut("◎");
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(58), dp(58), Gravity.BOTTOM | Gravity.END);
        cp.setMargins(0, 0, dp(28), dp(36));
        camera.setContentDescription("Open camera");
        camera.setOnClickListener(v -> {
            try {
                startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
            } catch (Exception ex) {
                Toast.makeText(this, "Camera unavailable", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(camera, cp);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int bottom = insets.getSystemWindowInsetBottom();
            fl.bottomMargin = dp(28) + bottom;
            cp.bottomMargin = dp(28) + bottom;
            np.bottomMargin = dp(96) + bottom;
            v.requestLayout();
            return insets;
        });
        setContentView(root);
        // Window insets controls require the decor installed by setContentView.
        immersive();
    }

    private void immersive() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private TextView shortcut(String symbol) {
        TextView view = new TextView(this);
        view.setText(symbol);
        view.setTextSize(24);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        GradientDrawable glass = new GradientDrawable();
        glass.setColor(0x66243344);
        glass.setCornerRadius(dp(29));
        glass.setStroke(dp(1), 0x73FFFFFF);
        view.setBackground(glass);
        view.setElevation(dp(8));
        return view;
    }

    private void toggleTorch() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 42);
            return;
        }
        try {
            if (torchId == null) {
                for (String id : cameras.getCameraIdList()) {
                    CameraCharacteristics ch = cameras.getCameraCharacteristics(id);
                    if (Boolean.TRUE.equals(ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))
                            && Integer.valueOf(CameraCharacteristics.LENS_FACING_BACK).equals(ch.get(CameraCharacteristics.LENS_FACING))) {
                        torchId = id;
                        break;
                    }
                }
            }
            if (torchId == null) {
                Toast.makeText(this, "Flashlight unavailable", Toast.LENGTH_SHORT).show();
                return;
            }
            cameras.setTorchMode(torchId, !torchOn);
            torchOn = !torchOn;
        } catch (Exception ex) {
            Toast.makeText(this, "Flashlight unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    private void unlock() {
        KeyguardManager km = getSystemService(KeyguardManager.class);
        if (km != null && km.isKeyguardLocked()) {
            km.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                @Override public void onDismissSucceeded() { finish(); }
            });
        } else finish();
    }

    private void renderNotices(List<NotificationHub.Item> items) {
        notices.removeAllViews();
        if (!config.showNotifications() || items == null || items.isEmpty()) return;
        boolean locked = false;
        KeyguardManager km = getSystemService(KeyguardManager.class);
        if (km != null) locked = km.isKeyguardLocked() || km.isDeviceLocked();
        boolean redact = locked && config.hideNotificationContent();
        int shown = 0;
        for (NotificationHub.Item item : items) {
            if (shown >= 3) break;
            notices.addView(card(item, redact), new LinearLayout.LayoutParams(-1, -2));
            shown++;
        }
    }

    private View card(NotificationHub.Item item, boolean redact) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(12);
        row.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(8);
        row.setLayoutParams(lp);
        GradientDrawable glass = new GradientDrawable();
        glass.setColor(0x7A1C2836);
        glass.setCornerRadius(dp(22));
        glass.setStroke(dp(1), 0x55FFFFFF);
        row.setBackground(glass);
        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(28), dp(28));
        ip.rightMargin = dp(12);
        if (item.icon != null) {
            try { icon.setImageDrawable(item.icon.loadDrawable(this)); }
            catch (Exception ignored) { icon.setImageDrawable(null); }
        }
        row.addView(icon, ip);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView app = label(item.appName, 11, 0xCCE7F2FF, true);
        String title = redact ? "Notification" : (item.title.isEmpty() ? item.appName : item.title);
        String body = redact ? "Unlock to view" : item.text;
        TextView head = label(title, 14, 0xFFF7FBFF, true);
        TextView msg = label(body, 13, 0xDDE4EEF8, false);
        TextView when = label(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(item.when)), 10, 0xAAD0DCEC, false);
        col.addView(app);
        col.addView(head);
        if (!body.isEmpty()) col.addView(msg);
        col.addView(when);
        row.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
        return row;
    }

    private TextView label(String text, float size, int color, boolean medium) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        v.setMaxLines(2);
        return v;
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grant) {
        super.onRequestPermissionsResult(request, permissions, grant);
        if (request == 42 && grant.length > 0 && grant[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            toggleTorch();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        immersive();
        scene.reload();
        ensureDepth();
        handler.removeCallbacks(tick);
        handler.post(tick);
        NotificationHub.get().addListener(noticeListener);
    }

    private void ensureDepth() {
        String wallpaper = config.wallpaper();
        if (!config.depthEnabled() || wallpaper == null || config.foreground() != null
                || wallpaper.equals(requestedWallpaper)) return;
        requestedWallpaper = wallpaper;
        if (depthEngine == null) depthEngine = new DepthEngine(this);
        depthEngine.segmentWallpaperAsync(Uri.parse(wallpaper), config.maskThreshold(), false, new DepthEngine.Callback() {
            @Override public void onStatus(String message) {
                config.prefs().edit().putString("depthStatus", message).apply();
            }
            @Override public void onComplete(File file, String message) {
                handler.post(() -> {
                    if (isDestroyed() || !wallpaper.equals(config.wallpaper())) return;
                    config.prefs().edit().putString("autoForeground", file.getAbsolutePath())
                            .putString("depthStatus", message).apply();
                    scene.reload();
                });
            }
            @Override public void onFailure(String message) { onStatus(message); }
        });
    }

    @Override protected void onDestroy() {
        if (depthEngine != null) depthEngine.close();
        super.onDestroy();
    }

    @Override protected void onPause() {
        handler.removeCallbacks(tick);
        NotificationHub.get().removeListener(noticeListener);
        if (torchOn && torchId != null) {
            try { cameras.setTorchMode(torchId, false); } catch (Exception ignored) {}
            torchOn = false;
        }
        super.onPause();
    }
}
