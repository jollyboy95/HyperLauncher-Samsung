package au.steve.hyperlauncher.compat;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.widget.EditText;
import android.widget.ImageView;
import android.view.ScaleGestureDetector;
import au.steve.hyperlauncher.compat.aurora.SavedLooks;
import android.app.DownloadManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import au.steve.hyperlauncher.compat.aurora.AuroraConfig;
import au.steve.hyperlauncher.compat.aurora.DepthEngine;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

public final class AuroraSettingsActivity extends Activity {
    private static final String RELEASE_API = "https://api.github.com/repos/jollyboy95/HyperLauncher-Samsung/releases/latest";
    private static final String RELEASE_PAGE = "https://github.com/jollyboy95/HyperLauncher-Samsung/releases";
    private static final String[] PRESETS = {"Depth Stack", "Horizon", "Offset", "Editorial", "Soft Stack", "Split Outline", "Poster", "Staggered", "Rotated", "Cinema", "Mountain", "Magazine", "Outline Cinema"};
    private static final String[] TABS = {"Clock", "Colour", "Depth", "Wallpaper", "Features", "Designs"};
    private static final int[] CLOCK_COLOURS = {0xE8F4FBFF, 0xF2FFFFFF, 0xF2F3D5B0, 0xF2F5B6C9, 0xE8C9E4FF};
    private static final String[] COLOUR_NAMES = {"Ice", "Pearl", "Champagne", "Rose", "Lilac"};

    private AuroraConfig config;
    private AuroraSceneView preview;
    private AuroraWallpaperHost wallpaperHost;
    private LinearLayout sheet;
    private TextView depthStatus;
    private TextView updateStatus;
    private Button downloadButton;
    private String availableApk;
    private int tab;
    private DepthEngine depthEngine;
    private final Handler previewHandler = new Handler(Looper.getMainLooper());
    private final Runnable previewTicker = new Runnable() {
        public void run() {
            if (preview != null) preview.invalidate();
            previewHandler.postDelayed(this, config.showSeconds() ? 1000 : 10000);
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        config = new AuroraConfig(this);
        depthEngine = new DepthEngine(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF070B12);

        wallpaperHost = new AuroraWallpaperHost(this, true);
        preview = wallpaperHost.scene;
        preview.setEditing(true);
        ScaleGestureDetector pinch = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                int size = Math.max(80, Math.min(220, Math.round(config.clockSize() * detector.getScaleFactor())));
                config.prefs().edit().putInt("clockSize", size).apply(); preview.reload(); return true;
            }
        });
        preview.setOnTouchListener(new View.OnTouchListener() {
            float down, downX;
            boolean scaling;
            public boolean onTouch(View v, MotionEvent e) {
                pinch.onTouchEvent(e);
                if (e.getPointerCount() > 1) { scaling = true; return true; }
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    down = e.getY(); downX = e.getX(); scaling = false;
                    preview.startClockDrag();
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE && !scaling) {
                    preview.moveClock(e.getX() - downX, e.getY() - down);
                    return true;
                }
                return e.getAction() == MotionEvent.ACTION_UP;
            }
        });
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-1, -1);
        pp.bottomMargin = dp(312);
        root.addView(wallpaperHost, pp);

        LinearLayout chrome = new LinearLayout(this);
        chrome.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, dp(312), Gravity.BOTTOM);
        chrome.setLayoutParams(cp);
        chrome.setPadding(dp(16), dp(12), dp(16), dp(18));
        chrome.setBackground(panel(0xEE101826, dp(28)));

        TextView title = label("AURORA DEPTH", 12, 0x99D7E8FF, true);
        title.setLetterSpacing(0.28f);
        chrome.addView(title);
        chrome.addView(presetStrip());
        chrome.addView(tabBar());
        sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(sheet);
        chrome.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(chrome);
        setContentView(root);
        showTab(0);
        maybeAutoSegment(false);
    }

    private HorizontalScrollView presetStrip() {
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int selected = config.clockPreset();
        for (int i = 0; i < PRESETS.length; i++) {
            final int preset = i;
            TextView chip = label(PRESETS[i], 12, Color.WHITE, true);
            chip.setPadding(dp(14), dp(10), dp(14), dp(10));
            chip.setBackground(panel(preset == selected ? 0xCC3A6D99 : 0x66304458, dp(18)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(8);
            lp.topMargin = dp(10);
            lp.bottomMargin = dp(10);
            chip.setOnClickListener(v -> {
                config.applyPreset(preset);
                for (int child = 0; child < row.getChildCount(); child++)
                    row.getChildAt(child).setBackground(panel(child == preset ? 0xCC3A6D99 : 0x66304458, dp(18)));
                preview.animatePreset(preset);
                preview.reload();
            });
            row.addView(chip, lp);
        }
        scroller.addView(row);
        return scroller;
    }

    private LinearLayout tabBar() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(8));
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            TextView tabView = label(TABS[i], 12, Color.WHITE, true);
            tabView.setGravity(Gravity.CENTER);
            tabView.setPadding(0, dp(8), 0, dp(8));
            tabView.setBackground(panel(i == tab ? 0xAA2C4C6A : 0x33243344, dp(14)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.rightMargin = i == TABS.length - 1 ? 0 : dp(6);
            tabView.setOnClickListener(v -> showTab(index));
            row.addView(tabView, lp);
        }
        return row;
    }

    private void showTab(int index) {
        tab = index;
        sheet.removeAllViews();
        if (index == 0) clockSheet();
        else if (index == 1) colourSheet();
        else if (index == 2) depthSheet();
        else if (index == 3) wallpaperSheet();
        else if (index == 4) effectsSheet();
        else designsSheet();
    }

    private void clockSheet() {
        sheet.addView(action("Magazine headline", v -> {
            EditText input = new EditText(this); input.setSingleLine(true); input.setText(config.headline());
            new AlertDialog.Builder(this).setTitle("Magazine headline").setView(input)
                    .setPositiveButton("Apply", (dialog, which) -> {
                        String title = input.getText().toString().trim();
                        config.prefs().edit().putString("headline", title.substring(0, Math.min(24, title.length()))).apply();
                        preview.reload();
                    }).setNegativeButton("Cancel", null).show();
        }));
        sheet.addView(slider("Digit height", config.clockSize() - 80, 140, v -> {
            config.prefs().edit().putInt("clockSize", 80 + v).apply();
            preview.reload();
        }));
        sheet.addView(slider("Digit width", Math.round(config.clockWidth() * 100), 100, v -> {
            config.prefs().edit().putFloat("clockWidth", v / 100f).apply();
            preview.reload();
        }));
        sheet.addView(slider("Weight", Math.round(config.clockWeight() * 100), 100, v -> {
            config.prefs().edit().putFloat("clockWeight", v / 100f).apply();
            preview.reload();
        }));
        sheet.addView(slider("Row spacing", Math.round(config.clockSpacing() * 100), 100, v -> {
            config.prefs().edit().putFloat("clockSpacing", v / 100f).apply();
            preview.reload();
        }));
        sheet.addView(slider("Vertical stretch", Math.round((config.clockStretch() - .6f) * 100), 100, v -> {
            config.prefs().edit().putFloat("clockStretch", .6f + v / 100f).apply(); preview.reload();
        }));
        sheet.addView(choiceRow("Date", new String[]{"Short", "Full", "Numeric", "Hidden"}, config.dateStyle(), i -> {
            config.prefs().edit().putInt("dateStyle", i).apply(); preview.reload();
        }));
        sheet.addView(toggle("Show seconds", config.showSeconds(), on -> {
            config.prefs().edit().putBoolean("showSeconds", on).apply(); preview.reload();
        }));
        sheet.addView(toggle("12-hour clock", config.clock12(), c -> {
            config.prefs().edit().putBoolean("clock12", c).apply();
            preview.reload();
        }));
        String[] styles = {"Tall Sans", "Hairline", "Editorial Serif"};
        sheet.addView(choiceRow("Typeface", styles, config.clockStyle(), i -> {
            config.prefs().edit().putInt("clockStyle", i).apply();
            preview.reload();
        }));
        sheet.addView(action("Reset clock", v -> {
            config.resetClock();
            preview.reload();
            showTab(0);
        }));
    }

    private void colourSheet() {
        String[] materials = {"Match clock", "Filled", "Glass", "Outline", "Gradient"};
        sheet.addView(choiceRow("Hours", materials, config.hoursMaterial() + 1, i -> {
            config.prefs().edit().putInt("hoursMaterial", i - 1).apply(); preview.reload();
        }));
        sheet.addView(choiceRow("Minutes", materials, config.minutesMaterial() + 1, i -> {
            config.prefs().edit().putInt("minutesMaterial", i - 1).apply(); preview.reload();
        }));
        sheet.addView(choiceRow("Clock material", new String[]{"Translucent", "Glass", "Outline", "Gradient"},
                config.clockMaterial(), i -> {
                    config.prefs().edit().putInt("clockMaterial", i).apply(); preview.reload();
                }));
        sheet.addView(action("Custom colour", v -> {
            EditText input = new EditText(this); input.setSingleLine(true);
            input.setText(String.format("#%06X", config.clockColor() & 0xFFFFFF));
            new AlertDialog.Builder(this).setTitle("Clock colour · #RRGGBB").setView(input)
                    .setPositiveButton("Apply", (dialog, which) -> {
                        try {
                            int color = Color.parseColor(input.getText().toString());
                            config.prefs().edit().putInt("clockColor", color).putBoolean("autoClockColor", false).apply(); preview.reload();
                        } catch (IllegalArgumentException invalid) { Toast.makeText(this, "Use a colour such as #A8D7FF", Toast.LENGTH_SHORT).show(); }
                    }).setNegativeButton("Cancel", null).show();
        }));
        sheet.addView(toggle("Wallpaper-aware colour", config.autoClockColor(), c -> {
            config.prefs().edit().putBoolean("autoClockColor", c).apply();
            preview.reload();
        }));
        sheet.addView(choiceRow("Manual colour", COLOUR_NAMES, colourIndex(), i -> {
            config.prefs().edit().putInt("clockColor", CLOCK_COLOURS[i]).putBoolean("autoClockColor", false).apply();
            preview.reload();
        }));
        sheet.addView(slider("Opacity", Math.round(config.clockOpacity() * 100), 100, v -> {
            config.prefs().edit().putFloat("clockOpacity", Math.max(0.25f, v / 100f)).apply();
            preview.reload();
        }));
        sheet.addView(slider("Scene dim", Math.round(config.dimAmount() * 100), 100, v -> {
            config.prefs().edit().putFloat("dimAmount", v / 100f).apply();
            preview.reload();
        }));
    }

    private void depthSheet() {
        sheet.addView(toggle("Poster outline above subject", config.outlineOverSubject(), on -> {
            config.prefs().edit().putBoolean("outlineOverSubject", on).apply(); preview.reload();
        }));
        depthStatus = label(statusText(), 13, 0xFFE3EEF8, false);
        depthStatus.setPadding(0, 0, 0, dp(8));
        sheet.addView(depthStatus);
        sheet.addView(toggle("Automatic depth", config.depthEnabled(), c -> {
            config.prefs().edit().putBoolean("depthEnabled", c).apply();
            preview.reload();
            if (c) maybeAutoSegment(true);
        }));
        sheet.addView(action("Analyse wallpaper now", v -> maybeAutoSegment(true)));
        sheet.addView(action("Choose PNG cutout", v -> pick(101, "image/png")));
        sheet.addView(action("Remove cutout", v -> {
            config.prefs().edit().remove("foreground").remove("autoForeground").putString("depthStatus", "Cutout removed").apply();
            preview.reload();
            depthStatus.setText("Cutout removed");
        }));
        sheet.addView(info("On-device depth supports people, pets, vehicles, plants and other recognised objects. For scenes the model cannot separate, choose a transparent PNG cutout. Processing stays on your phone."));
    }

    private void wallpaperSheet() {
        sheet.addView(action("Choose photo wallpaper", v -> pick(102, "image/*")));
        sheet.addView(action("Choose video wallpaper", v -> pick(103, "video/*")));
        sheet.addView(action("Use photo instead of video", v -> {
            config.prefs().edit().remove("videoWallpaper").apply(); wallpaperHost.reload();
        }));
        sheet.addView(toggle("Gentle photo motion", config.photoMotion(), on -> {
            config.prefs().edit().putBoolean("photoMotion", on).apply(); preview.reload();
        }));
        sheet.addView(info("Videos loop silently. Depth cutouts and glass clock material apply to photos. Photo motion follows the phone’s animation setting."));
        sheet.addView(slider("Zoom", Math.round((config.wallpaperZoom() - 1f) * 100), 80, v -> {
            config.prefs().edit().putFloat("wallpaperZoom", 1f + v / 100f).apply();
            preview.reload();
        }));
        sheet.addView(slider("Focus X", Math.round(config.wallpaperFocusX() * 100), 100, v -> {
            config.prefs().edit().putFloat("wallpaperFocusX", v / 100f).apply();
            preview.reload();
        }));
        sheet.addView(slider("Focus Y", Math.round(config.wallpaperFocusY() * 100), 100, v -> {
            config.prefs().edit().putFloat("wallpaperFocusY", v / 100f).apply();
            preview.reload();
        }));
        sheet.addView(info("Drag in any direction to position the clock; pinch to resize. Long-press the lock screen to return here."));
    }

    private void effectsSheet() {
        sheet.addView(choiceRow("Notification display", new String[]{"List", "Stack", "Count"},
                config.notificationMode(), mode -> config.prefs().edit().putInt("notificationMode", mode).apply()));
        sheet.addView(toggle("Music controls", config.showMedia(), on ->
                config.prefs().edit().putBoolean("showMedia", on).apply()));
        sheet.addView(toggle("Battery and next alarm", config.statusWidgets(), on ->
                config.prefs().edit().putBoolean("statusWidgets", on).apply()));
        sheet.addView(toggle("Wake transition", config.wakeAnimation(), on ->
                config.prefs().edit().putBoolean("wakeAnimation", on).apply()));
        String[] shortcuts = {"Flashlight", "Camera", "Alarms", "Phone", "Settings"};
        sheet.addView(choiceRow("Left shortcut", shortcuts, config.leftShortcut(), i ->
                config.prefs().edit().putInt("leftShortcut", i).apply()));
        sheet.addView(choiceRow("Right shortcut", shortcuts, config.rightShortcut(), i ->
                config.prefs().edit().putInt("rightShortcut", i).apply()));
        sheet.addView(info("Tap a notification to open it after unlock. Swipe a dismissible card sideways to clear it. List shows cards, Stack shows one card, and Count hides cards until expanded. Tap the count to expand. Music controls use your active player after notification access is enabled."));
        sheet.addView(toggle("Show live notifications", config.showNotifications(), c ->
                config.prefs().edit().putBoolean("showNotifications", c).apply()));
        sheet.addView(toggle("Hide content until unlock", config.hideNotificationContent(), c ->
                config.prefs().edit().putBoolean("hideNotificationContent", c).apply()));
        sheet.addView(action("Notification access", v ->
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))));
        sheet.addView(info("Samsung PIN, fingerprint and face unlock stay in control. Aurora cannot replace Keyguard or appear on every wake without system privileges."));
        sheet.addView(action("Reset all", v -> {
            config.resetAll();
            preview.reload();
            Toast.makeText(this, "Editor restored", Toast.LENGTH_SHORT).show();
            showTab(4);
        }));
        updateStatus = label("Installed " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")", 13, Color.WHITE, false);
        sheet.addView(updateStatus);
        sheet.addView(action("Check for updates", v -> checkUpdates()));
        downloadButton = action("Download update", v -> downloadUpdate());
        downloadButton.setEnabled(false);
        sheet.addView(downloadButton);
        sheet.addView(action("Open GitHub releases", v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_PAGE)))));
        sheet.addView(action("Preview lock screen", v -> finish()));
    }

    private void designsSheet() {
        SavedLooks store = new SavedLooks(config);
        sheet.addView(action("Save current design", v -> {
            EditText name = new EditText(this); name.setSingleLine(true);
            new AlertDialog.Builder(this).setTitle("Name this design").setView(name)
                    .setPositiveButton("Save", (dialog, which) -> { store.save(name.getText().toString()); showTab(5); })
                    .setNegativeButton("Cancel", null).show();
        }));
        sheet.addView(info("Swipe through saved designs. Tap a preview to apply it; long-press to delete. Up to eight designs stay on this phone."));
        HorizontalScrollView gallery = new HorizontalScrollView(this);
        LinearLayout row = new LinearLayout(this);
        for (SavedLooks.Look look : store.list()) {
            LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL);
            tile.addView(label(look.name, 12, Color.WHITE, true));
            AuroraSceneView miniature = new AuroraSceneView(this, true, SavedLooks.previewConfig(this, look));
            miniature.layout(0, 0, 240, 520);
            Bitmap bitmap = Bitmap.createBitmap(240, 520, Bitmap.Config.ARGB_8888);
            miniature.draw(new Canvas(bitmap));
            ImageView image = new ImageView(this); image.setImageBitmap(bitmap);
            image.setContentDescription("Apply " + look.name);
            image.setOnClickListener(v -> { store.apply(look); wallpaperHost.reload(); maybeAutoSegment(false); });
            image.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this).setTitle("Delete " + look.name + "?")
                        .setPositiveButton("Delete", (d, which) -> { store.delete(look.name); showTab(5); })
                        .setNegativeButton("Cancel", null).show(); return true;
            });
            tile.addView(image, new LinearLayout.LayoutParams(dp(120), dp(260)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.rightMargin = dp(12);
            row.addView(tile, lp);
        }
        gallery.addView(row); sheet.addView(gallery);
        sheet.addView(action("Done · preview lock screen", v -> finish()));
    }

    private String statusText() {
        String status = config.depthStatus();
        return status == null || status.isEmpty() ? "Automatic depth is ready when a wallpaper is selected." : status;
    }

    private int colourIndex() {
        int current = config.clockColor();
        for (int i = 0; i < CLOCK_COLOURS.length; i++) if (CLOCK_COLOURS[i] == current) return i;
        return 0;
    }

    private void pick(int code, String type) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType(type);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, code);
    }

    private void maybeAutoSegment(boolean force) {
        String wallpaper = config.wallpaper();
        if (config.videoWallpaper() != null || !config.depthEnabled() || wallpaper == null) return;
        setDepthStatus("Identifying foreground subject…");
        depthEngine.segmentWallpaperAsync(Uri.parse(wallpaper), config.maskThreshold(), force, new DepthEngine.Callback() {
            @Override public void onStatus(String message) { runOnUiThread(() -> setDepthStatus(message)); }
            @Override public void onComplete(File maskFile, String message) {
                runOnUiThread(() -> {
                    if (!wallpaper.equals(config.wallpaper())) return;
                    config.prefs().edit().putString("autoForeground", maskFile.getAbsolutePath())
                            .putString("depthStatus", message).apply();
                    preview.reload();
                    setDepthStatus(message);
                });
            }
            @Override public void onFailure(String message) {
                runOnUiThread(() -> { if (wallpaper.equals(config.wallpaper())) setDepthStatus(message); });
            }
        });
    }

    private void setDepthStatus(String message) {
        config.prefs().edit().putString("depthStatus", message).apply();
        if (depthStatus != null) depthStatus.setText(message);
    }

    private LinearLayout slider(String name, int value, int max, ProgressListener listener) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(label(name, 12, 0xFFD5E6F8, false));
        SeekBar bar = new SeekBar(this);
        bar.setMax(max);
        bar.setProgress(Math.max(0, Math.min(max, value)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int progress, boolean user) { if (user) listener.on(progress); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        box.addView(bar);
        return box;
    }

    private Switch toggle(String name, boolean on, ToggleListener listener) {
        Switch view = new Switch(this);
        view.setText(name);
        view.setTextColor(Color.WHITE);
        view.setChecked(on);
        view.setPadding(0, dp(6), 0, dp(6));
        view.setOnCheckedChangeListener((v, c) -> listener.on(c));
        return view;
    }

    private LinearLayout choiceRow(String title, String[] names, int selected, ChoiceListener listener) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(label(title, 12, 0xFFD5E6F8, false));
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        LinearLayout row = new LinearLayout(this);
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            TextView chip = label(names[i], 12, Color.WHITE, false);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            chip.setBackground(panel(i == selected ? 0xCC3A6D99 : 0x55304458, dp(14)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(8);
            chip.setOnClickListener(v -> {
                listener.on(index);
                for (int child = 0; child < row.getChildCount(); child++)
                    row.getChildAt(child).setBackground(panel(child == index ? 0xCC3A6D99 : 0x55304458, dp(14)));
            });
            row.addView(chip, lp);
        }
        scroller.addView(row);
        box.addView(scroller);
        return box;
    }

    private Button action(String name, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(name);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackground(panel(0xAA2A4A66, dp(16)));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(44));
        lp.topMargin = dp(6);
        button.setLayoutParams(lp);
        return button;
    }

    private TextView info(String text) {
        TextView v = label(text, 12, 0xBBD0DCEC, false);
        v.setPadding(0, dp(8), 0, dp(8));
        return v;
    }

    private TextView label(String text, float size, int color, boolean medium) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return v;
    }

    private GradientDrawable panel(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        d.setStroke(dp(1), 0x33FFFFFF);
        return d;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private interface ProgressListener { void on(int value); }
    private interface ToggleListener { void on(boolean value); }
    private interface ChoiceListener { void on(int value); }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if ((request == 101 || request == 102 || request == 103) && result == RESULT_OK && data != null && data.getData() != null) {
            Uri u = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (request == 101) {
                    config.prefs().edit().putString("foreground", u.toString()).apply();
                    Toast.makeText(this, "Cutout saved", Toast.LENGTH_SHORT).show();
                } else if (request == 103) {
                    config.prefs().edit().putString("videoWallpaper", u.toString()).apply();
                    wallpaperHost.reload();
                    Toast.makeText(this, "Video wallpaper saved", Toast.LENGTH_SHORT).show();
                } else {
                    config.prefs().edit().putString("wallpaper", u.toString()).remove("autoForeground").remove("foreground").remove("videoWallpaper")
                            .putString("depthStatus", "Wallpaper saved. Analysing subject…").apply();
                    maybeAutoSegment(true);
                    wallpaperHost.reload();
                }
                preview.reload();
            } catch (Exception e) {
                Toast.makeText(this, "Unable to save image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void downloadUpdate() {
        if (availableApk == null) return;
        if (!getPackageManager().canRequestPackageInstalls()) {
            updateStatus.setText("Allow Aurora to install unknown apps, then try again.");
            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
            return;
        }
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(availableApk));
            request.setTitle("Aurora Depth update");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "aurora-update.apk");
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            manager.enqueue(request);
            updateStatus.setText("Downloading signed APK. Install from the completed download notification.");
            downloadButton.setEnabled(false);
        } catch (Exception e) {
            updateStatus.setText("Unable to download update. Open Releases instead.");
        }
    }

    private void checkUpdates() {
        updateStatus.setText("Checking releases…");
        downloadButton.setEnabled(false);
        new Thread(() -> {
            String result;
            String apkUrl = null;
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(RELEASE_API).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setRequestProperty("Accept", "application/vnd.github+json");
                c.setRequestProperty("User-Agent", "AuroraDepth-Android");
                int status = c.getResponseCode();
                if (status == 404) result = "No published Aurora release yet. Use the Actions artifact until a Release exists.";
                else if (status == 403) result = "GitHub API rate limit (403). Try again later.";
                else if (status != 200) result = "Update check unavailable (HTTP " + status + ").";
                else {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int n;
                    try (InputStream in = c.getInputStream()) {
                        while ((n = in.read(buf)) != -1 && out.size() < 65536) out.write(buf, 0, n);
                    }
                    JSONObject release = new JSONObject(out.toString("UTF-8"));
                    String tag = release.optString("tag_name", "");
                    int remote = -1;
                    if (tag.matches("aurora-v[0-9]+")) remote = Integer.parseInt(tag.substring(8));
                    if (remote <= BuildConfig.VERSION_CODE) result = "Aurora is up to date: " + BuildConfig.VERSION_NAME;
                    else {
                        JSONArray assets = release.optJSONArray("assets");
                        if (assets != null) for (int i = 0; i < assets.length(); i++) {
                            JSONObject asset = assets.optJSONObject(i);
                            if (asset != null && asset.optString("name").endsWith(".apk")) {
                                apkUrl = asset.optString("browser_download_url", null);
                                break;
                            }
                        }
                        result = apkUrl == null ? "New release " + tag + " found, but no APK is attached."
                                : "Update available: " + tag;
                    }
                }
                c.disconnect();
            } catch (Exception e) {
                result = "Unable to check for updates. Check your connection.";
            }
            String message = result;
            String found = apkUrl;
            runOnUiThread(() -> {
                availableApk = found;
                downloadButton.setEnabled(found != null);
                updateStatus.setText(message);
            });
        }).start();
    }

    @Override protected void onResume() {
        super.onResume();
        wallpaperHost.resume();
        previewHandler.removeCallbacks(previewTicker);
        previewHandler.post(previewTicker);
        if (preview != null) preview.reload();
    }

    @Override protected void onPause() {
        wallpaperHost.pause();
        previewHandler.removeCallbacks(previewTicker);
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (depthEngine != null) depthEngine.close();
        super.onDestroy();
    }
}
