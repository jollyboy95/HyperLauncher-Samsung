package au.steve.hyperlauncher.compat;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.widget.EditText;
import android.widget.ImageView;
import android.view.ScaleGestureDetector;
import au.steve.hyperlauncher.compat.aurora.SavedLooks;
import au.steve.hyperlauncher.compat.aurora.EditorDraft;
import au.steve.hyperlauncher.compat.aurora.AuroraWakeService;
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
    private static final String[] PRESETS = {
            "Depth Stack", "Horizon", "Offset", "Editorial", "Soft Stack", "Split Outline",
            "Poster", "Staggered", "Rotated", "Cinema", "Mountain", "Magazine", "Outline Cinema",
            "HyperOS 4 Glass", "Movie: Up In Air", "Movie: Neon Dog", "Movie: Block", "Classic Left", "Ultra-Tall Art"
    };
    private static final String[] TABS = {"Clock", "Colour", "Depth", "Wallpaper", "Features", "Designs", "Notifications", "About"};
    private static final int[] CLOCK_COLOURS = {0xE8F4FBFF, 0xF2FFFFFF, 0xF2F3D5B0, 0xF2F5B6C9, 0xE8C9E4FF};
    private static final String[] COLOUR_NAMES = {"Ice", "Pearl", "Champagne", "Rose", "Lilac"};

    private AuroraConfig config;
    private final java.util.List<TextView> tabViews = new java.util.ArrayList<>();
    private AuroraSceneView preview;
    private AuroraWallpaperHost wallpaperHost;
    private LinearLayout sheet;
    private LinearLayout chrome;
    private FrameLayout editorRoot;
    private LinearLayout styleGallery;
    private boolean galleryMode = true;
    private TextView toolbarTitle;
    private TextView sheetHandle;
    private boolean expanded;
    private boolean dark;
    private int ink, muted, surface, card, accent;
    private ScrollView controlsScroll;
    private HorizontalScrollView categoryScroll;
    private TextView depthStatus;
    private TextView updateStatus;
    private Button downloadButton;
    private String availableApk;
    private int tab;
    private DepthEngine depthEngine;
    private int depthGeneration;
    private final Handler previewHandler = new Handler(Looper.getMainLooper());
    private final Runnable previewTicker = new Runnable() {
        public void run() {
            if (preview != null) preview.invalidate();
            previewHandler.postDelayed(this, config.showSeconds() ? 1000 : 10000);
        }
    };

    @Override public void onCreate(Bundle b) {
        boolean night = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        setTheme(night ? android.R.style.Theme_Material_NoActionBar : android.R.style.Theme_Material_Light_NoActionBar);
        super.onCreate(b);
        config = EditorDraft.open(this, b != null && b.getBoolean("restoreDraft", false));
        depthEngine = new DepthEngine(this);
        dark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        ink = dark ? 0xFFF4F5F8 : 0xFF191A1D;
        muted = dark ? 0xFFA3A7B2 : 0xFF747780;
        surface = dark ? 0xFF101114 : 0xFFF3F4F6;
        card = dark ? 0xFF202126 : 0xFFFFFFFF;
        accent = dark ? 0xFF82AEFF : 0xFF2671F5;
        expanded = b != null && b.getBoolean("expandedControls", false);
        FrameLayout root = new FrameLayout(this);
        editorRoot = root;
        root.setBackgroundColor(surface);
        getWindow().setStatusBarColor(surface);
        getWindow().setNavigationBarColor(surface);
        root.setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars()
                        | android.view.WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });

        wallpaperHost = new AuroraWallpaperHost(this, true, config);
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
        wallpaperHost.setClipToOutline(true);
        wallpaperHost.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),dp(30));
            }
        });
        wallpaperHost.setPreviewRadius(dp(30));
        wallpaperHost.setElevation(dp(5));
        root.addView(wallpaperHost, new FrameLayout.LayoutParams(dp(180), dp(360), Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        chrome = new LinearLayout(this);
        chrome.setOrientation(LinearLayout.VERTICAL);
        chrome.setPadding(dp(20), 0, dp(20), dp(12));
        chrome.setBackground(panel(surface, dp(32)));
        sheetHandle = label("⌃  All controls", 12, muted, true);
        sheetHandle.setGravity(Gravity.CENTER);
        sheetHandle.setContentDescription("Expand editor controls");
        sheetHandle.setOnClickListener(v -> { expanded = !expanded; layoutEditor(); });
        chrome.addView(sheetHandle, new LinearLayout.LayoutParams(-1, dp(40)));
        HorizontalScrollView categories = new HorizontalScrollView(this);
        categoryScroll=categories;
        categories.setHorizontalScrollBarEnabled(false);
        categories.addView(tabBar());
        chrome.addView(categories, new LinearLayout.LayoutParams(-1, dp(52)));
        sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        controlsScroll = new ScrollView(this);
        controlsScroll.setFillViewport(false);
        controlsScroll.setClipToPadding(false);
        controlsScroll.setPadding(0, dp(8), 0, dp(16));
        controlsScroll.addView(sheet);
        chrome.addView(controlsScroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(chrome, new FrameLayout.LayoutParams(-1, dp(330), Gravity.BOTTOM));
        root.addOnLayoutChangeListener((v,l,t,r,bt,ol,ot,or,ob) -> {
            if (r-l != or-ol || bt-t != ob-ot) layoutEditor();
        });
        LinearLayout toolbar = new LinearLayout(this); toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(16),dp(8),dp(16),dp(8));
        TextView cancel = label("Cancel",14,ink,true); cancel.setGravity(Gravity.CENTER);
        cancel.setBackground(touchSurface(card, dp(22)));
        cancel.setContentDescription("Cancel lock-screen changes");cancel.setOnClickListener(v -> finish());
        toolbar.addView(cancel,new LinearLayout.LayoutParams(dp(72),dp(38)));
        TextView title = label("Lock screen",18,ink,true);title.setGravity(Gravity.CENTER);
        toolbarTitle=title;title.setContentDescription("Browse lock-screen styles");
        title.setOnClickListener(v -> showGallery(true));
        toolbar.addView(title,new LinearLayout.LayoutParams(0,dp(38),1));
        TextView apply = label("Apply",14,Color.WHITE,true);apply.setGravity(Gravity.CENTER);
        apply.setBackground(touchSurface(0xFF2671F5,dp(22)));apply.setContentDescription("Apply lock-screen changes");
        apply.setOnClickListener(v -> applyAndFinish());toolbar.addView(apply,new LinearLayout.LayoutParams(dp(72),dp(38)));
        root.addView(toolbar,new FrameLayout.LayoutParams(-1,dp(56),Gravity.TOP));
        styleGallery = buildStyleGallery();
        FrameLayout.LayoutParams gp=new FrameLayout.LayoutParams(-1,-1);gp.topMargin=dp(64);
        root.addView(styleGallery,0,gp);
        setContentView(root);
        showTab(b == null ? 0 : b.getInt("editorTab", 0));
        showGallery(b == null || b.getBoolean("galleryMode",true));
        root.post(this::layoutEditor);
        maybeAutoSegment(false);
    }

    private LinearLayout tabBar() {
        LinearLayout row = new LinearLayout(this);
        tabViews.clear();
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(8));
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            TextView tabView = label(TABS[i], 14, ink, true);
            tabView.setGravity(Gravity.CENTER);
            tabView.setPadding(dp(16), dp(8), dp(16), dp(8));
            tabView.setMinHeight(dp(48));
            tabView.setBackground(touchSurface(i == tab ? (dark ? 0xFF263A57 : 0xFFE1EBFF) : surface, dp(22)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(48));
            lp.rightMargin = i == TABS.length - 1 ? 0 : dp(6);
            tabViews.add(tabView);
            tabView.setOnClickListener(v -> showTab(index));
            row.addView(tabView, lp);
        }
        return row;
    }

    private void showTab(int index) {
        tab = index;
        for(int i=0;i<tabViews.size();i++) {
            tabViews.get(i).setBackground(touchSurface(i==index?(dark?0xFF263A57:0xFFE1EBFF):surface,dp(22)));
            tabViews.get(i).setTextColor(i==index?accent:muted);
            tabViews.get(i).setSelected(i==index);
        }
        depthStatus = null;
        controlsScroll.scrollTo(0,0);
        tabViews.get(index).post(() -> categoryScroll.scrollTo(Math.max(0,tabViews.get(index).getLeft()-dp(12)),0));
        sheet.removeAllViews();
        if (index == 0) clockSheet();
        else if (index == 1) colourSheet();
        else if (index == 2) depthSheet();
        else if (index == 3) wallpaperSheet();
        else if (index == 4) effectsSheet();
        else if(index==5) designsSheet();
        else if(index==6) notificationsSheet();
        else aboutSheet();
        styleControls();
    }

    private final class PhonePreview extends ImageView {
        private final android.graphics.Path shape=new android.graphics.Path();
        PhonePreview() { super(AuroraSettingsActivity.this); }
        @Override protected void onMeasure(int widthSpec,int heightSpec) {
            int maxWidth=View.MeasureSpec.getSize(widthSpec),maxHeight=View.MeasureSpec.getSize(heightSpec);
            int width=Math.max(1,Math.min(maxWidth,Math.round(maxHeight*240f/520f)));
            setMeasuredDimension(width,Math.max(1,Math.min(maxHeight,Math.round(width*520f/240f))));
        }
        @Override public void draw(Canvas canvas) {
            int save=canvas.save();shape.reset();
            shape.addRoundRect(0,0,getWidth(),getHeight(),dp(30),dp(30),android.graphics.Path.Direction.CW);
            canvas.clipPath(shape);super.draw(canvas);canvas.restoreToCount(save);
        }
    }

    private LinearLayout buildStyleGallery() {
        LinearLayout gallery=new LinearLayout(this);gallery.setOrientation(LinearLayout.VERTICAL);
        boolean compactGallery=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        TextView heading=label("Choose your style",compactGallery?20:28,ink,true);heading.setGravity(Gravity.CENTER);
        heading.setPadding(dp(16),dp(16),dp(16),dp(6));gallery.addView(heading);
        TextView hint=label("Swipe to explore lock-screen styles",14,muted,false);hint.setGravity(Gravity.CENTER);
        gallery.addView(hint,new LinearLayout.LayoutParams(-1,dp(32)));
        if(compactGallery) hint.setVisibility(View.GONE);
        HorizontalScrollView pages=new HorizontalScrollView(this);pages.setHorizontalScrollBarEnabled(false);
        pages.setClipToPadding(false);
        int width=getResources().getDisplayMetrics().widthPixels;
        int tileWidth=Math.min(dp(360),Math.round(width*.72f));
        pages.setPadding(Math.max(dp(20),(width-tileWidth)/2),dp(16),Math.max(dp(20),(width-tileWidth)/2),dp(16));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        SavedLooks.Look source=new SavedLooks.Look("Preview",new SavedLooks(config).snapshot());
        for(int i=0;i<PRESETS.length;i++) {
            final int preset=i;
            LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);
            TextView name=label(PRESETS[i],20,ink,true);name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);tile.addView(name,new LinearLayout.LayoutParams(-1,dp(compactGallery?32:56)));
            ImageView image=new PhonePreview();image.setScaleType(ImageView.ScaleType.FIT_XY);
            AuroraConfig miniature=SavedLooks.previewConfig(this,source);miniature.applyPreset(i);
            AuroraSceneView drawing=new AuroraSceneView(this,true,miniature,512);
            drawing.layout(0,0,240,520);
            Bitmap bitmap=Bitmap.createBitmap(240,520,Bitmap.Config.ARGB_8888);
            drawing.draw(new Canvas(bitmap));drawing.releaseBitmaps();image.setImageBitmap(bitmap);
            image.setBackground(panel(dark?0xFF33353A:0xFFE3E5EB,dp(30)));
            image.setPadding(dp(3),dp(3),dp(3),dp(3));image.setClipToOutline(true);
            image.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override public void getOutline(View v,android.graphics.Outline o) {
                    o.setRoundRect(0,0,v.getWidth(),v.getHeight(),dp(30));
                }
            });
            image.setContentDescription("Preview "+PRESETS[i]);
            View.OnClickListener customise=v -> {
                if(config.clockPreset()!=preset) config.applyPreset(preset);preview.reload();
                showGallery(false);showTab(0);
            };
            image.setOnClickListener(customise);
            tile.addView(image,new LinearLayout.LayoutParams(-2,0,1));
            Button customiseButton=action("Customize",customise);
            customiseButton.setGravity(Gravity.CENTER);customiseButton.setContentDescription("Customize "+PRESETS[i]);
            LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(52));bp.topMargin=dp(12);
            tile.addView(customiseButton,bp);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(tileWidth,-1);lp.rightMargin=dp(20);
            row.addView(tile,lp);
        }
        pages.setContentDescription("Lock-screen style gallery");
        pages.setOnScrollChangeListener((v,x,y,oldX,oldY) -> {
            int chosen=Math.max(0,Math.min(PRESETS.length-1,Math.round(x/(float)(tileWidth+dp(20)))));
            if(chosen!=config.clockPreset()) {config.applyPreset(chosen);preview.reload();}
        });
        pages.addView(row,new FrameLayout.LayoutParams(-2,-1));
        gallery.addView(pages,new LinearLayout.LayoutParams(-1,0,1));
        TextView saved=label("My designs  ›",15,accent,true);saved.setGravity(Gravity.CENTER);saved.setMinHeight(dp(56));
        saved.setOnClickListener(v -> {showGallery(false);showTab(5);});gallery.addView(saved);
        pages.post(() -> pages.scrollTo(config.clockPreset()*(tileWidth+dp(20)),0));
        return gallery;
    }

    private void showGallery(boolean show) {
        if(show && !galleryMode) {
            editorRoot.removeView(styleGallery);
            styleGallery=buildStyleGallery();
            FrameLayout.LayoutParams gp=new FrameLayout.LayoutParams(-1,-1);gp.topMargin=dp(64);
            editorRoot.addView(styleGallery,0,gp);
        }
        galleryMode=show;
        styleGallery.setVisibility(show?View.VISIBLE:View.GONE);
        chrome.setVisibility(show?View.GONE:View.VISIBLE);
        wallpaperHost.setVisibility(show?View.GONE:View.VISIBLE);
        toolbarTitle.setText(show?"Lock screen":"‹  Styles");
        layoutEditor();
    }

    @Override public void onBackPressed() {
        if(!galleryMode) showGallery(true); else super.onBackPressed();
    }

    private void layoutEditor() {
        if (editorRoot == null || editorRoot.getHeight() <= 0) return;
        int h = editorRoot.getHeight() - editorRoot.getPaddingTop() - editorRoot.getPaddingBottom();
        int w = editorRoot.getWidth() - editorRoot.getPaddingLeft() - editorRoot.getPaddingRight();
        // Compact and landscape windows need a full-height controls page to remain usable.
        boolean compact = h < dp(560) || w > h;
        int sheetHeight = compact ? Math.max(dp(180),h-dp(64))
                : Math.round((h-dp(60))*(expanded?.72f:.48f));
        FrameLayout.LayoutParams cp=(FrameLayout.LayoutParams)chrome.getLayoutParams();
        if(cp.height!=sheetHeight) { cp.height=sheetHeight;chrome.setLayoutParams(cp); }
        int previewHeight=Math.max(1,h-sheetHeight-dp(82));
        FrameLayout.LayoutParams pp=(FrameLayout.LayoutParams)wallpaperHost.getLayoutParams();
        int pw=Math.min(w-dp(56),Math.round(previewHeight*w/(float)Math.max(1,h)));
        if(pp.height!=previewHeight||pp.width!=pw||pp.topMargin!=dp(68)) {
            pp.width=Math.max(1,pw);pp.height=previewHeight;pp.topMargin=dp(68);wallpaperHost.setLayoutParams(pp);
        }
        wallpaperHost.setVisibility(compact||galleryMode?View.GONE:View.VISIBLE);
        sheetHandle.setText(expanded?"⌄  Show preview":"⌃  All controls");
        sheetHandle.setContentDescription(expanded?"Collapse editor controls":"Expand editor controls");
        sheetHandle.setVisibility(compact?View.GONE:View.VISIBLE);
    }

    private void styleControls() {
        for(int i=0;i<sheet.getChildCount();i++) {
            View child=sheet.getChildAt(i);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
            lp.bottomMargin=dp(8);child.setLayoutParams(lp);
            if(child instanceof Button || child instanceof Switch) child.setMinimumHeight(dp(56));
            else if(child instanceof LinearLayout) {
                child.setBackground(panel(card,dp(22)));child.setPadding(dp(16),dp(14),dp(16),dp(14));
            }
        }
    }

    private void clockSheet() {
        sheet.addView(action("Custom signature", v -> {
            EditText input = new EditText(this); input.setSingleLine(true); input.setText(config.signature());
            new AlertDialog.Builder(this).setTitle("Signature").setView(input)
                    .setPositiveButton("Apply", (dialog, which) -> {
                        String sig = input.getText().toString().trim();
                        config.prefs().edit().putString("signature", sig).putBoolean("showSignature", true).apply();
                        preview.reload();
                    }).setNegativeButton("Cancel", null).show();
        }));
        if (config.clockPreset() == 14) {
            sheet.addView(action("Movie title (UP IN THE AIR)", v -> {
                EditText input = new EditText(this); input.setText(config.movieTitle());
                new AlertDialog.Builder(this).setTitle("Movie title").setView(input)
                        .setPositiveButton("Apply", (dialog, which) -> {
                            String title = input.getText().toString().trim();
                            config.prefs().edit().putString("movieTitle", title).apply();
                            preview.reload();
                        }).setNegativeButton("Cancel", null).show();
            }));
        }
        if (config.clockPreset() == 11) sheet.addView(action("Magazine headline", v -> {
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
        String[] materials = {"Match clock", "Filled", "Glass", "Outline", "Gradient", "Neon Wireframe"};
        sheet.addView(choiceRow("Hours", materials, config.hoursMaterial() + 1, i -> {
            config.prefs().edit().putInt("hoursMaterial", i - 1).apply(); preview.reload();
        }));
        sheet.addView(choiceRow("Minutes", materials, config.minutesMaterial() + 1, i -> {
            config.prefs().edit().putInt("minutesMaterial", i - 1).apply(); preview.reload();
        }));
        sheet.addView(choiceRow("Clock material", new String[]{"Translucent", "Glass", "Outline", "Gradient", "Neon Wireframe"},
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
        depthStatus = label(statusText(), 13, muted, false);
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
        sheet.addView(toggle("Open automatically on wake", config.autoWakeEnabled(), enabled -> {
            config.prefs().edit().putBoolean("autoWakeEnabled", enabled).apply();
            if (enabled && !AuroraWakeService.enabledInSystem(this)) autoWakeSetup();
        }));
        sheet.addView(action("Automatic wake setup", v -> autoWakeSetup()));
        sheet.addView(info(AuroraWakeService.enabledInSystem(this)
                ? "Automatic wake access is enabled. " + config.prefs().getString("autoWakeStatus", "Lock the phone, then wake it to test.")
                : "One-time setup: enable Aurora automatic lock screen in Accessibility. Screen contents are not read. Tap Apply after setup."));
        sheet.addView(action("Notification display",v -> showTab(6)));
        sheet.addView(toggle("Music controls", config.showMedia(), on ->
                config.prefs().edit().putBoolean("showMedia", on).apply()));
        sheet.addView(toggle("Battery and next alarm", config.statusWidgets(), on ->
                config.prefs().edit().putBoolean("statusWidgets", on).apply()));
        sheet.addView(toggle("Animations", config.motionEnabled(), on ->
                config.prefs().edit().putBoolean("motionEnabled",on).apply()));
        sheet.addView(toggle("Wake transition", config.wakeAnimation(), on ->
                config.prefs().edit().putBoolean("wakeAnimation", on).apply()));
        String[] shortcuts = {"Flashlight", "Camera", "Alarms", "Phone", "Settings"};
        sheet.addView(choiceRow("Left shortcut", shortcuts, config.leftShortcut(), i ->
                config.prefs().edit().putInt("leftShortcut", i).apply()));
        sheet.addView(choiceRow("Right shortcut", shortcuts, config.rightShortcut(), i ->
                config.prefs().edit().putInt("rightShortcut", i).apply()));
        sheet.addView(action("Reset all", v -> {
            config.resetAll();
            preview.reload();
            Toast.makeText(this, "Editor restored", Toast.LENGTH_SHORT).show();
            showTab(4);
        }));
    }

    private void notificationsSheet() {
        sheet.addView(notificationDisplayCards());
        sheet.addView(info("Swipe up to browse a stack. Swipe sideways to dismiss a notification, or tap to open it after unlocking."));
        sheet.addView(toggle("Show live notifications", config.showNotifications(), c ->
                config.prefs().edit().putBoolean("showNotifications", c).apply()));
        sheet.addView(toggle("Hide content until unlock", config.hideNotificationContent(), c ->
                config.prefs().edit().putBoolean("hideNotificationContent", c).apply()));
        sheet.addView(action("Notification access", v ->
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))));
        sheet.addView(info("Samsung PIN, fingerprint and face unlock stay in control. Automatic wake is optional and uses the access you enable in Accessibility."));
    }

    private void aboutSheet() {
        updateStatus = label("Installed " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")", 13, muted, false);
        sheet.addView(updateStatus);
        sheet.addView(action("Check for updates", v -> checkUpdates()));
        downloadButton = action("Download update", v -> downloadUpdate());
        downloadButton.setEnabled(false);
        sheet.addView(downloadButton);
        sheet.addView(action("Open GitHub releases", v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_PAGE)))));
        sheet.addView(action("Preview lock screen", v -> applyAndFinish()));
    }

    private void autoWakeSetup() {
        new AlertDialog.Builder(this).setTitle("Automatic lock screen")
                .setMessage("Enable Aurora automatic lock screen under Accessibility → Installed apps. Aurora only detects screen wake and lock state; it cannot read screen contents or unlock your phone. Return here and tap Apply after enabling access.\n\nIf Samsung blocks this for a downloaded APK, open App info → More → Allow restricted settings first.")
                .setPositiveButton("Open Accessibility", (dialog, which) -> {
                    config.prefs().edit().putBoolean("autoWakeEnabled", true).apply();
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                }).setNegativeButton("Not now", null).show();
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
            tile.addView(label(look.name, 14, ink, true));
            AuroraSceneView miniature = new AuroraSceneView(this, true, SavedLooks.previewConfig(this, look));
            miniature.layout(0, 0, 240, 520);
            Bitmap bitmap = Bitmap.createBitmap(240, 520, Bitmap.Config.ARGB_8888);
            miniature.draw(new Canvas(bitmap));
            miniature.releaseBitmaps();
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
        sheet.addView(action("Done · preview lock screen", v -> applyAndFinish()));
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
        if (config.videoWallpaper() != null || !config.depthEnabled() || wallpaper == null
                || config.foreground() != null && !force) return;
        setDepthStatus("Identifying foreground subject…");
        final int generation = ++depthGeneration;
        depthEngine.segmentWallpaperAsync(Uri.parse(wallpaper), config.maskThreshold(), force, new DepthEngine.Callback() {
            @Override public void onStatus(String message) { runOnUiThread(() -> { if (!isFinishing() && !isDestroyed() && generation == depthGeneration) setDepthStatus(message); }); }
            @Override public void onComplete(File maskFile, String message) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || generation != depthGeneration || !wallpaper.equals(config.wallpaper())) return;
                    config.prefs().edit().putString("autoForeground", maskFile.getAbsolutePath())
                            .putString("depthStatus", message).apply();
                    preview.reload();
                    setDepthStatus(message);
                });
            }
            @Override public void onFailure(String message) {
                runOnUiThread(() -> { if (!isFinishing() && !isDestroyed() && generation == depthGeneration && wallpaper.equals(config.wallpaper())) setDepthStatus(message); });
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
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(label(name,15,ink,true),new LinearLayout.LayoutParams(0,-2,1));
        TextView reading=label(String.valueOf(value),13,muted,false);heading.addView(reading);
        box.addView(heading);
        SeekBar bar = new SeekBar(this);
        bar.setMinimumHeight(dp(48));
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        bar.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));
        bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(dark?0xFF41444D:0xFFE5E7EC));
        bar.setContentDescription(name);
        bar.setMax(max);
        bar.setProgress(Math.max(0, Math.min(max, value)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int progress, boolean user) { if (user) { reading.setText(String.valueOf(progress)); listener.on(progress); } }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        box.addView(bar);
        return box;
    }

    private Switch toggle(String name, boolean on, ToggleListener listener) {
        Switch view = new Switch(this);
        view.setText(name);
        view.setTextColor(ink);
        view.setTextSize(15);
        view.setSwitchMinWidth(dp(48));
        view.setTrackTintList(new android.content.res.ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},
                new int[]{accent,dark?0xFF484B55:0xFFD9DCE3}));
        view.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        view.setChecked(on);
        view.setBackground(touchSurface(card,dp(22)));
        view.setPadding(dp(16), dp(12), dp(16), dp(12));
        view.setOnCheckedChangeListener((v, c) -> listener.on(c));
        return view;
    }

    private View notificationDisplayCards() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dp(4), 0, dp(10));

        TextView heading = label("Notification display", 15, ink, true);
        heading.setPadding(0, 0, 0, dp(8));
        container.addView(heading);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        String[] titles = {"List", "Stack", "Count"};
        int current = config.notificationMode();

        for (int i = 0; i < 3; i++) {
            final int mode = i;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(6), dp(10), dp(6), dp(10));
            boolean selected = (current == mode);

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(selected ? (dark?0xFF263A57:0xFFE1EBFF) : surface);
            bg.setCornerRadius(dp(16));
            bg.setStroke(dp(selected ? 2 : 1), selected ? accent : (dark?0xFF484B55:0xFFD9DCE3));
            card.setBackground(bg);

            FrameLayout phone = new FrameLayout(this);
            phone.setBackground(panel(0x66182838, dp(8)));

            LinearLayout glyph = new LinearLayout(this);
            glyph.setOrientation(LinearLayout.VERTICAL);
            glyph.setGravity(Gravity.CENTER);

            if (mode == 0) {
                for (int b = 0; b < 3; b++) {
                    View bar = new View(this);
                    bar.setBackground(panel(0xCCFFFFFF, dp(2)));
                    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(26), dp(4));
                    bp.bottomMargin = dp(3);
                    glyph.addView(bar, bp);
                }
            } else if (mode == 1) {
                View bar = new View(this);
                bar.setBackground(panel(0xFFFFFFFF, dp(3)));
                glyph.addView(bar, new LinearLayout.LayoutParams(dp(26), dp(8)));
                View lip = new View(this);
                lip.setBackground(panel(0x66FFFFFF, dp(2)));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(20), dp(3));
                lp.topMargin = dp(2);
                glyph.addView(lip, lp);
            } else {
                View pill = new View(this);
                pill.setBackground(panel(0xFFFFFFFF, dp(3)));
                glyph.addView(pill, new LinearLayout.LayoutParams(dp(14), dp(5)));
            }

            phone.addView(glyph, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
            card.addView(phone, new LinearLayout.LayoutParams(dp(44), dp(54)));

            TextView labelView = label(titles[mode], 12, selected ? accent : ink, true);
            labelView.setPadding(0, dp(6), 0, 0);
            card.addView(labelView);

            card.setSelected(selected);
            card.setContentDescription(titles[mode]+" notification display");
            card.setOnClickListener(v -> {
                config.prefs().edit().putInt("notificationMode", mode).apply();
                showTab(tab);
            });

            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
            if (i > 0) cp.leftMargin = dp(8);
            row.addView(card, cp);
        }
        container.addView(row);
        return container;
    }

    private LinearLayout choiceRow(String title, String[] names, int selected, ChoiceListener listener) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(label(title, 15, ink, true));
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setPadding(0,dp(12),0,0);
        LinearLayout row = new LinearLayout(this);
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            TextView chip = label(names[i], 14, i == selected ? accent : ink, true);
            chip.setPadding(dp(16), dp(12), dp(16), dp(12));
            chip.setMinHeight(dp(48));
            chip.setGravity(Gravity.CENTER);
            chip.setSelected(i==selected);
            chip.setBackground(touchSurface(i == selected ? (dark?0xFF263A57:0xFFE1EBFF) : surface, dp(18)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(8);
            chip.setOnClickListener(v -> {
                listener.on(index);
                for (int child = 0; child < row.getChildCount(); child++) {
                    TextView option=(TextView)row.getChildAt(child);
                    option.setBackground(touchSurface(child==index?(dark?0xFF263A57:0xFFE1EBFF):surface,dp(18)));
                    option.setTextColor(child==index?accent:ink);option.setSelected(child==index);
                }
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
        button.setTextColor(ink);
        button.setTextSize(15);
        button.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        button.setPadding(dp(16),0,dp(16),0);
        button.setMinHeight(dp(56));
        button.setElevation(0);button.setStateListAnimator(null);
        button.setBackground(touchSurface(card, dp(22)));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(56));
        lp.topMargin = dp(6);
        button.setLayoutParams(lp);
        return button;
    }

    private TextView info(String text) {
        TextView v = label(text, 13, muted, false);
        v.setLineSpacing(dp(3),1f);
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
        d.setStroke(dp(1), dark?0x1AFFFFFF:0x09000000);
        return d;
    }

    private android.graphics.drawable.Drawable touchSurface(int color,int radius) {
        return new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(
                dark?0x22FFFFFF:0x16000000),panel(color,radius),panel(Color.WHITE,radius));
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

    private void applyAndFinish() {
        EditorDraft.apply(this,config);
        finish();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("restoreDraft",true);state.putBoolean("expandedControls",expanded);
        state.putInt("editorTab",tab);state.putBoolean("galleryMode",galleryMode);super.onSaveInstanceState(state);
    }

    @Override protected void onResume() {
        super.onResume();
        AuroraWakeService.editorVisible = true;
        wallpaperHost.resume();
        previewHandler.removeCallbacks(previewTicker);
        previewHandler.post(previewTicker);
        if (preview != null) preview.reload();
        if (tab == 4 && sheet != null) showTab(4);
    }

    @Override protected void onPause() {
        AuroraWakeService.editorVisible = false;
        wallpaperHost.pause();
        previewHandler.removeCallbacks(previewTicker);
        super.onPause();
    }

    @Override protected void onDestroy() {
        depthGeneration++;
        if (depthEngine != null) depthEngine.close();
        super.onDestroy();
    }
}
