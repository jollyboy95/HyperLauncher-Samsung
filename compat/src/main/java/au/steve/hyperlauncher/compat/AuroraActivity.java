package au.steve.hyperlauncher.compat;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.provider.AlarmClock;
import android.widget.SeekBar;
import android.widget.Button;
import android.widget.ScrollView;
import au.steve.hyperlauncher.compat.aurora.LockMediaController;
import au.steve.hyperlauncher.compat.aurora.core.NotificationDisplay;
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
import au.steve.hyperlauncher.compat.aurora.AuroraWakeService;
import au.steve.hyperlauncher.compat.aurora.DepthEngine;
import java.io.File;
import au.steve.hyperlauncher.compat.aurora.NotificationHub;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class AuroraActivity extends Activity {
    private AuroraSceneView scene;
    private boolean wakeReceiverRegistered;
    private final android.content.BroadcastReceiver wakeExit = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context context, Intent intent) {
            if ((getIntent().getBooleanExtra(AuroraWakeService.EXTRA_AUTO, false) || config.autoWakeEnabled())
                    && (AuroraWakeService.ACTION_UNLOCKED.equals(intent.getAction()) || Intent.ACTION_SCREEN_OFF.equals(intent.getAction()))) finish();
        }
    };
    private LinearLayout notices;
    private AuroraWallpaperHost wallpaperHost;
    private LockMediaController media;
    private boolean notificationsExpanded;
    private int lastNotificationMode = -1;
    private boolean lastLocked;
    private long lastStatusMinute = -1;
    private TextView leftShortcutView, rightShortcutView;
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
            long minute = System.currentTimeMillis() / 60000;
            if (lastLocked != locked() || minute != lastStatusMinute) {
                lastLocked = locked(); lastStatusMinute = minute;
                renderNotices(NotificationHub.get().snapshot());
            }
            handler.postDelayed(this, config.showSeconds() ? 1000 : 10000);
        }
    };
    private final NotificationHub.Listener noticeListener = this::renderNotices;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        config = new AuroraConfig(this);
        android.content.IntentFilter wakeFilter = new android.content.IntentFilter(AuroraWakeService.ACTION_UNLOCKED);
        wakeFilter.addAction(Intent.ACTION_SCREEN_OFF);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(wakeExit, wakeFilter, android.content.Context.RECEIVER_NOT_EXPORTED);
        // The signature permission protects API 28–32 too; those releases ignore the newer flag bit.
        else registerReceiver(wakeExit, wakeFilter, "au.steve.hyperlauncher.compat.permission.INTERNAL", null,
                android.content.Context.RECEIVER_NOT_EXPORTED);
        wakeReceiverRegistered = true;
        setShowWhenLocked(true);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        wallpaperHost = new AuroraWallpaperHost(this, false);
        scene = wallpaperHost.scene;
        root.addView(wallpaperHost, new FrameLayout.LayoutParams(-1, -1));
        media = new LockMediaController(this, () -> renderNotices(NotificationHub.get().snapshot()));
        notices = new LinearLayout(this);
        notices.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        np.leftMargin = dp(22);
        np.rightMargin = dp(22);
        np.bottomMargin = dp(118);
        root.addView(notices, np);
        notices.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                scene.setBottomContentTop(notices.getChildCount() == 0 ? root.getHeight() : top));

        android.view.GestureDetector gestures = new android.view.GestureDetector(this,
                new android.view.GestureDetector.SimpleOnGestureListener() {
                    @Override public void onLongPress(MotionEvent e) {
                        afterUnlock(() -> startActivity(new Intent(AuroraActivity.this, AuroraSettingsActivity.class)));
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
        leftShortcutView = flash;
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(dp(58), dp(58), Gravity.BOTTOM | Gravity.START);
        fl.setMargins(dp(28), 0, 0, dp(36));
        flash.setContentDescription("Toggle flashlight");
        flash.setOnClickListener(v -> runShortcut(config.leftShortcut()));
        root.addView(flash, fl);

        TextView camera = shortcut("◎");
        rightShortcutView = camera;
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(58), dp(58), Gravity.BOTTOM | Gravity.END);
        cp.setMargins(0, 0, dp(28), dp(36));
        camera.setContentDescription("Open camera");
        camera.setOnClickListener(v -> runShortcut(config.rightShortcut()));
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
        boolean locked = false;
        KeyguardManager km = getSystemService(KeyguardManager.class);
        if (km != null) locked = km.isKeyguardLocked() || km.isDeviceLocked();
        boolean redact = locked && config.hideNotificationContent();
        if (config.statusWidgets()) addStatusWidgets();
        if (config.showMedia() && media != null && media.hasSession()) addMediaCard(redact);
        if (!config.showNotifications() || items == null || items.isEmpty()) return;
        boolean canClear = false;
        for (NotificationHub.Item item : items) if (item.dismissible) { canClear=true;break; }
        int mode = config.notificationMode();
        if (mode != lastNotificationMode) { notificationsExpanded=false;lastNotificationMode=mode; }
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        TextView count=label(NotificationDisplay.showCount(mode,items.size())
                ? items.size() + (items.size()==1?" notification":" notifications") + "  ·  " + (notificationsExpanded?"Collapse":"Expand")
                : "Notifications",12,0xDBE8F0FB,true);
        count.setPadding(dp(10),0,dp(8),0);count.setMinHeight(dp(48));count.setGravity(Gravity.CENTER_VERTICAL);
        if(NotificationDisplay.showCount(mode,items.size())) count.setOnClickListener(v->{
            notificationsExpanded=!notificationsExpanded;renderNotices(NotificationHub.get().snapshot());
            if(config.motionEnabled()&&android.animation.ValueAnimator.areAnimatorsEnabled()){
                notices.setAlpha(.7f);notices.animate().alpha(1).setDuration(220).start();
            }
        });
        header.addView(count,new LinearLayout.LayoutParams(0,dp(48),1));
        if(canClear){
            TextView clear=label("Clear all",12,0xFFE7F2FF,true);clear.setGravity(Gravity.CENTER);
            clear.setContentDescription("Clear all dismissible notifications");
            clear.setOnClickListener(v->dismissalFeedback(NotificationHub.get().clearAll()));
            header.addView(clear,new LinearLayout.LayoutParams(dp(72),dp(48)));
        }
        notices.addView(header);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int limit = NotificationDisplay.visibleCards(mode, notificationsExpanded, items.size());
        if (limit == 0) return;
        for (int i=0;i<items.size()&&i<limit;i++) {
            View front=card(items.get(i),redact);
            if(mode==NotificationDisplay.STACK&&!notificationsExpanded&&items.size()>1){
                FrameLayout stack=new FrameLayout(this);stack.setPadding(0,0,0,dp(12));
                for(int layer=2;layer>=1;layer--){
                    View back=new View(this);back.setBackground(new GlassBackdrop(scene,back));back.setAlpha(layer==2?.38f:.65f);
                    back.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                    FrameLayout.LayoutParams slab=new FrameLayout.LayoutParams(-1,dp(64),Gravity.BOTTOM);
                    slab.leftMargin=slab.rightMargin=dp(layer*7);slab.bottomMargin=dp((2-layer)*5);stack.addView(back,slab);
                }
                FrameLayout.LayoutParams face=new FrameLayout.LayoutParams(-1,-2);face.bottomMargin=dp(8);stack.addView(front,face);
                list.addView(stack,new LinearLayout.LayoutParams(-1,-2));
            }else list.addView(front);
        }
        ScrollView scroll = new NotificationScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(list);
        int rows = Math.min(limit, items.size());
        notices.addView(scroll, new LinearLayout.LayoutParams(-1, rows > 2 ? dp(190) : -2));
    }

    private boolean locked() {
        KeyguardManager km = getSystemService(KeyguardManager.class);
        return km != null && (km.isKeyguardLocked() || km.isDeviceLocked());
    }

    private void afterUnlock(Runnable action) {
        KeyguardManager km = getSystemService(KeyguardManager.class);
        if (km != null && locked()) {
            km.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                @Override public void onDismissSucceeded() { action.run(); }
            });
        } else action.run();
    }

    private void runShortcut(int shortcut) {
        if (shortcut == 0) { toggleTorch(); return; }
        Intent intent = shortcut == 1 ? new Intent(locked() ? MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
                : MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                : shortcut == 2 ? new Intent(AlarmClock.ACTION_SHOW_ALARMS)
                : shortcut == 3 ? new Intent(Intent.ACTION_DIAL) : new Intent(android.provider.Settings.ACTION_SETTINGS);
        Runnable open = () -> { try { startActivity(intent); } catch (Exception e) {
            Toast.makeText(this, "Shortcut unavailable", Toast.LENGTH_SHORT).show(); } };
        if (shortcut == 1) open.run(); else afterUnlock(open);
    }

    private void updateShortcuts() {
        String[] symbols = {"✦", "◎", "◷", "☎", "⚙"};
        String[] names = {"Flashlight", "Camera", "Alarms", "Phone", "Settings"};
        int left = Math.max(0, Math.min(4, config.leftShortcut()));
        int right = Math.max(0, Math.min(4, config.rightShortcut()));
        leftShortcutView.setText(symbols[left]); leftShortcutView.setContentDescription(names[left]);
        rightShortcutView.setText(symbols[right]); rightShortcutView.setContentDescription(names[right]);
    }

    private void addStatusWidgets() {
        Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = battery == null ? -1 : battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery == null ? 100 : battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        String text = level < 0 ? "" : Math.round(level * 100f / Math.max(1, scale)) + "% battery";
        AlarmManager alarms = getSystemService(AlarmManager.class);
        AlarmManager.AlarmClockInfo next = alarms == null ? null : alarms.getNextAlarmClock();
        if (next != null) text += "   ·   Alarm " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(next.getTriggerTime()));
        if (!text.isEmpty()) {
            TextView widgets = label(text, 11, 0xDDE7F2FF, true);
            widgets.setGravity(Gravity.CENTER);
            widgets.setPadding(dp(8), dp(6), dp(8), dp(6));
            widgets.setOnClickListener(v -> runShortcut(2));
            notices.addView(widgets);
        }
    }

    private void addMediaCard(boolean redact) {
        MediaMetadata data = media.metadata();
        PlaybackState state = media.playback();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.setBackground(new GlassBackdrop(scene,card));
        String title = data == null ? "Now playing" : String.valueOf(data.getText(MediaMetadata.METADATA_KEY_TITLE));
        if (title.equals("null")) title = "Now playing";
        CharSequence artist = data == null ? null : data.getText(MediaMetadata.METADATA_KEY_ARTIST);
        if (!redact && data != null) {
            android.graphics.Bitmap art = data.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (art == null) art = data.getBitmap(MediaMetadata.METADATA_KEY_ART);
            if (art != null) {
                ImageView artwork = new ImageView(this); artwork.setImageBitmap(art);
                artwork.setScaleType(ImageView.ScaleType.CENTER_CROP); artwork.setContentDescription("Album artwork");
                card.addView(artwork, new LinearLayout.LayoutParams(dp(48), dp(48)));
            }
        }
        card.addView(label(redact ? "Unlock to view media" : title, 14, Color.WHITE, true));
        if (!redact && artist != null) card.addView(label(artist.toString(), 11, 0xCCDEE9F5, false));
        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        addMediaButton(controls, "⏮", "Previous track", () -> media.previous());
        boolean playing = state != null && state.getState() == PlaybackState.STATE_PLAYING;
        addMediaButton(controls, playing ? "Ⅱ" : "▶", playing ? "Pause" : "Play", () -> media.playPause());
        addMediaButton(controls, "⏭", "Next track", () -> media.next());
        card.addView(controls);
        if (!redact && data != null && state != null && data.getLong(MediaMetadata.METADATA_KEY_DURATION) > 0
                && (state.getActions() & PlaybackState.ACTION_SEEK_TO) != 0) {
            long duration = data.getLong(MediaMetadata.METADATA_KEY_DURATION);
            SeekBar seek = new SeekBar(this);
            seek.setMax(1000);
            seek.setProgress((int) Math.min(1000, state.getPosition() * 1000 / duration));
            seek.setContentDescription("Playback position");
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s, int value, boolean user) { if (user) media.seek(duration * value / 1000); }
                public void onStartTrackingTouch(SeekBar s) {}
                public void onStopTrackingTouch(SeekBar s) {}
            });
            card.addView(seek);
        }
        notices.addView(card);
    }

    private void addMediaButton(LinearLayout row, String symbol, String description, Runnable action) {
        TextView button = label(symbol, 22, Color.WHITE, true);
        button.setGravity(Gravity.CENTER); button.setContentDescription(description);
        button.setOnClickListener(v -> action.run());
        row.addView(button, new LinearLayout.LayoutParams(dp(64), dp(48)));
    }

    private GradientDrawable glass() {
        GradientDrawable glass = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xAA304458, 0x8A152331});
        glass.setCornerRadius(dp(22)); glass.setStroke(dp(1), 0x55FFFFFF);
        return glass;
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
        row.setBackground(new GlassBackdrop(scene,row));
        row.setOnClickListener(v -> {
            if (item.contentIntent == null) return;
            afterUnlock(() -> { try { item.contentIntent.send(); } catch (PendingIntent.CanceledException e) {
                Toast.makeText(this, "Notification is no longer available", Toast.LENGTH_SHORT).show();
            } });
        });
        row.setOnTouchListener(new NotificationSwipeListener(this,
                () -> dismissalFeedback(NotificationHub.get().dismiss(item))));
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
        if (item.dismissible) {
            TextView clear = label("×", 24, 0xFFE7F2FF, true);
            clear.setGravity(Gravity.CENTER);
            clear.setContentDescription("Clear notification from " + item.appName);
            clear.setOnClickListener(v -> dismissalFeedback(NotificationHub.get().dismiss(item)));
            row.addView(clear, new LinearLayout.LayoutParams(dp(48), dp(48)));
        }
        return row;
    }

    private void dismissalFeedback(NotificationHub.DismissResult result) {
        if (result == NotificationHub.DismissResult.NOT_CLEARABLE)
            Toast.makeText(this, "Android keeps this notification active", Toast.LENGTH_SHORT).show();
        else if (result == NotificationHub.DismissResult.UNAVAILABLE)
            Toast.makeText(this, "Notification access disconnected. Re-enable it in Features.", Toast.LENGTH_LONG).show();
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
        AuroraWakeService.screenVisible = true;
        if (getIntent().getBooleanExtra(AuroraWakeService.EXTRA_AUTO, false))
            config.prefs().edit().putString("autoWakeStatus", "Automatic launch active").apply();
        immersive();
        wallpaperHost.resume();
        updateShortcuts();
        media.start();
        if (config.motionEnabled() && config.wakeAnimation() && android.animation.ValueAnimator.areAnimatorsEnabled()) {
            wallpaperHost.setScaleX(1.025f);wallpaperHost.setScaleY(1.025f);
            wallpaperHost.animate().scaleX(1).scaleY(1).setDuration(650)
                    .setInterpolator(new android.view.animation.PathInterpolator(.18f,.8f,.2f,1)).start();
            scene.setAlpha(.82f);scene.animate().alpha(1).setDuration(450).start();
        }
        ensureDepth();
        handler.removeCallbacks(tick);
        handler.post(tick);
        NotificationHub.get().addListener(noticeListener);
    }

    private void ensureDepth() {
        String wallpaper = config.wallpaper();
        if (config.videoWallpaper() != null || !config.depthEnabled() || wallpaper == null || config.foreground() != null
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
        if (wakeReceiverRegistered) { unregisterReceiver(wakeExit); wakeReceiverRegistered = false; }
        if (depthEngine != null) depthEngine.close();
        super.onDestroy();
    }

    @Override protected void onPause() {
        AuroraWakeService.screenVisible = false;
        wallpaperHost.pause();
        wallpaperHost.animate().cancel();wallpaperHost.setScaleX(1);wallpaperHost.setScaleY(1);
        notices.animate().cancel();notices.setAlpha(1);
        media.stop();
        scene.animate().cancel();
        scene.setAlpha(1);
        handler.removeCallbacks(tick);
        NotificationHub.get().removeListener(noticeListener);
        if (torchOn && torchId != null) {
            try { cameras.setTorchMode(torchId, false); } catch (Exception ignored) {}
            torchOn = false;
        }
        super.onPause();
    }
}
