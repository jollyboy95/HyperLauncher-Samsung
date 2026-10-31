package au.steve.hyperlauncher.compat;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import au.steve.hyperlauncher.compat.aurora.AuroraConfig;
import au.steve.hyperlauncher.compat.aurora.DepthEngine;
import au.steve.hyperlauncher.compat.aurora.core.ClockLayout;
import au.steve.hyperlauncher.compat.aurora.core.ColorMath;
import au.steve.hyperlauncher.compat.aurora.core.WallpaperTransform;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Shared renderer: wallpaper, clock, then segmented foreground. */
public final class AuroraSceneView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint measure = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final AuroraConfig config;
    private Bitmap background;
    private Bitmap foreground;
    private boolean preview;
    private boolean editing;
    private int dragClockStart;
    private String loadedBackground = "";
    private String loadedForeground = "";
    private int displayedPreset;
    private float transition = 1f;
    private ValueAnimator animator;

    public AuroraSceneView(Context context, boolean preview) {
        super(context);
        this.preview = preview;
        config = new AuroraConfig(context);
        displayedPreset = config.clockPreset();
        setLayerType(LAYER_TYPE_HARDWARE, null);
        reload();
    }

    public void setEditing(boolean value) {
        editing = value;
        invalidate();
    }

    public void startClockDrag() {
        dragClockStart = config.clockY();
    }

    public int safeClockY(int desired) {
        float h = Math.max(1, getHeight());
        return Math.max((int) (-h * 0.08f), Math.min((int) (h * 0.42f), desired));
    }

    public void moveClock(float delta) {
        int y = safeClockY(dragClockStart + Math.round(delta));
        config.prefs().edit().putInt("clockY", y).apply();
        invalidate();
    }

    public void animatePreset(int preset) {
        if (preset == displayedPreset && transition >= 1f) {
            invalidate();
            return;
        }
        if (animator != null) animator.cancel();
        displayedPreset = preset;
        transition = 0f;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(420);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            transition = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private Bitmap load(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            if (value.startsWith("/") || value.startsWith("file:")) {
                File file = value.startsWith("file:") ? new File(Uri.parse(value).getPath()) : new File(value);
                return decodeFile(file);
            }
            try (InputStream bounds = getContext().getContentResolver().openInputStream(Uri.parse(value))) {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(bounds, null, o);
                o.inSampleSize = 1;
                int max = 1800;
                while (o.outWidth / o.inSampleSize > max || o.outHeight / o.inSampleSize > max) o.inSampleSize *= 2;
                o.inJustDecodeBounds = false;
                try (InputStream actual = getContext().getContentResolver().openInputStream(Uri.parse(value))) {
                    return BitmapFactory.decodeStream(actual, null, o);
                }
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Bitmap decodeFile(File file) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), o);
        o.inSampleSize = 1;
        int max = 1800;
        while (o.outWidth / o.inSampleSize > max || o.outHeight / o.inSampleSize > max) o.inSampleSize *= 2;
        o.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), o);
    }

    public void reload() {
        String bg = config.wallpaper();
        String fg = null;
        if (config.depthEnabled()) {
            String auto = config.autoForeground();
            if (DepthEngine.isCurrentMask(auto)) fg = auto;
        }
        if (config.depthEnabled() && config.foreground() != null) fg = config.foreground();
        if (!String.valueOf(bg).equals(loadedBackground)) {
            if (background != null) background.recycle();
            background = load(bg);
            loadedBackground = String.valueOf(bg);
        }
        String foregroundKey = String.valueOf(fg);
        if (fg != null && fg.startsWith("/")) foregroundKey += ":" + new File(fg).lastModified();
        if (!foregroundKey.equals(loadedForeground)) {
            if (foreground != null) foreground.recycle();
            foreground = load(fg);
            loadedForeground = foregroundKey;
        }
        int preset = config.clockPreset();
        if (preset != displayedPreset) animatePreset(preset);
        else invalidate();
    }

    private void imageCover(Canvas canvas, Bitmap bitmap, float w, float h) {
        WallpaperTransform t = WallpaperTransform.compute(bitmap.getWidth(), bitmap.getHeight(), w, h,
                config.wallpaperZoom(), config.wallpaperFocusX(), config.wallpaperFocusY());
        paint.reset();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap, null, new RectF(t.dx, t.dy, t.dx + bitmap.getWidth() * t.scale,
                t.dy + bitmap.getHeight() * t.scale), paint);
    }

    private int clockColour() {
        int manual = config.clockColor();
        int alpha = Math.round(config.clockOpacity() * 255f);
        if (!config.autoClockColor() || background == null) return (alpha << 24) | (manual & 0x00FFFFFF);
        int w = background.getWidth();
        int h = background.getHeight();
        int[] samples = new int[144];
        int k = 0;
        for (int y = 0; y < 12; y++) {
            for (int x = 0; x < 12; x++) {
                samples[k++] = background.getPixel(Math.min(w - 1, (x * 2 + 1) * w / 24),
                        Math.min(h - 1, (y * 2 + 1) * h / 24));
            }
        }
        int auto = ColorMath.fromSamples(samples);
        int a = Math.round(config.clockOpacity() * 255f);
        return (a << 24) | (auto & 0x00FFFFFF);
    }

    private Typeface clockFace() {
        int style = config.clockStyle();
        float weight = config.clockWeight();
        String family = style == 2 ? "serif" : "sans-serif-condensed";
        int androidWeight = Math.round((style == 1 ? 100f : 180f) + weight * 320f);
        return Typeface.create(Typeface.create(family, Typeface.NORMAL), androidWeight, false);
    }

    private void text(Canvas c, String s, float x, float baseline, float size, int color, Typeface face, boolean center, float tracking) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setSubpixelText(true);
        paint.setColor(color);
        paint.setTypeface(face);
        paint.setTextSize(size);
        paint.setLetterSpacing(tracking);
        paint.setTextAlign(center ? Paint.Align.CENTER : Paint.Align.LEFT);
        c.drawText(s, x, baseline, paint);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;
        c.drawColor(0xFF071018);
        if (background != null) imageCover(c, background, width, height);
        else {
            paint.reset();
            paint.setShader(new RadialGradient(width * 0.5f, height * 0.28f, height * 0.55f,
                    new int[]{0xFF2A4A6A, 0xFF0B1724}, null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, width, height, paint);
            paint.setShader(null);
        }
        float dim = config.dimAmount();
        paint.reset();
        paint.setShader(new LinearGradient(0, 0, 0, height,
                new int[]{mix(0xCC050914, dim), mix(0x14050A12, dim * 0.35f), mix(0xB0050910, dim)},
                new float[]{0f, 0.42f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, width, height, paint);
        paint.setShader(null);

        // Only the text is masked: the wallpaper keeps the same lighting across crop edges.
        int clockLayer = c.saveLayer(0, 0, width, height, null);
        Date now = new Date();
        String date = new SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now).toUpperCase(Locale.getDefault());
        String digits = new SimpleDateFormat(config.clock12() ? "hhmm" : "HHmm", Locale.getDefault()).format(now);
        String hours = digits.substring(0, 2);
        String minutes = digits.substring(2);
        Typeface face = clockFace();
        Typeface dateFace = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        measure.setTypeface(face);
        measure.setTextSize(100f);
        measure.setLetterSpacing(-0.07f);
        ClockLayout layout = ClockLayout.compute(config.clockPreset(), width, height, config.clockSize(),
                config.clockY(), config.clockWidth(), config.clockSpacing(),
                measure.measureText(hours) / 100f, measure.measureText(minutes) / 100f,
                measure.measureText(config.clockPreset() == 3 ? digits : hours + ":" + minutes) / 100f);
        int color = clockColour();
        int dateColor = (Math.round(Math.max(.72f, config.clockOpacity()) * 255) << 24) | (color & 0x00FFFFFF);
        text(c, date, layout.dateX, layout.dateY, Math.max(11f, height * 0.012f), dateColor, dateFace, true, 0.14f);

        c.save();
        c.translate(layout.clockX, 0);
        c.scale(layout.horizontalScale, layout.verticalScale);
        if (layout.showColon) {
            text(c, hours + ":" + minutes, 0f, layout.firstBaseline / layout.verticalScale, layout.fittedSize, color, face, true, -0.06f);
        } else if (layout.editorialFour) {
            text(c, digits, 0f, layout.firstBaseline / layout.verticalScale, layout.fittedSize, color, face, true, -0.08f);
        } else {
            text(c, hours, 0f, layout.firstBaseline / layout.verticalScale, layout.fittedSize, color, face, true, -0.08f);
            text(c, minutes, 0f, (layout.firstBaseline + layout.lineHeight) / layout.verticalScale, layout.fittedSize, color, face, true, -0.08f);
        }
        c.restore();

        if (foreground != null) {
            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            WallpaperTransform t = WallpaperTransform.compute(foreground.getWidth(), foreground.getHeight(), width, height,
                    config.wallpaperZoom(), config.wallpaperFocusX(), config.wallpaperFocusY());
            c.drawBitmap(foreground, null, new RectF(t.dx, t.dy,
                    t.dx + foreground.getWidth() * t.scale, t.dy + foreground.getHeight() * t.scale), paint);
        }

        c.restoreToCount(clockLayer);

        if (editing) {
            paint.reset();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.5f, width * 0.004f));
            paint.setColor(0x66D7ECFF);
            float boxTop = Math.max(height * 0.03f, layout.dateY - height * 0.03f);
            float boxBot = layout.stacked
                    ? (layout.firstBaseline + layout.lineHeight + height * 0.02f)
                    : (layout.firstBaseline + height * 0.02f);
            c.drawRoundRect(new RectF(width * 0.06f, boxTop, width * 0.94f, boxBot), 28, 28, paint);
        }

        if (!preview) {
            paint.reset();
            paint.setAntiAlias(true);
            paint.setColor(0x55FFFFFF);
            float cx = width * 0.5f;
            float cy = height * 0.905f;
            c.drawRoundRect(new RectF(cx - 18, cy, cx + 18, cy + 5), 4, 4, paint);
            text(c, "SWIPE UP TO UNLOCK", cx, height * 0.94f, Math.max(10f, height * 0.013f), 0xCCE4EEF8, dateFace, true, 0.22f);
        }
    }

    private static int mix(int color, float amount) {
        int a = Math.round(((color >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, amount)));
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
