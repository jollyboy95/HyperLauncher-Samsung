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
import au.steve.hyperlauncher.compat.aurora.core.NotificationDisplay;
import au.steve.hyperlauncher.compat.aurora.core.ClockLayout;
import au.steve.hyperlauncher.compat.aurora.core.ColorMath;
import au.steve.hyperlauncher.compat.aurora.core.GlassBlur;
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
    private Bitmap frosted;
    private final int decodeLimit;
    private Bitmap foreground;
    private boolean preview;
    private boolean editing;
    private int dragClockStart;
    private float dragClockX;
    private boolean videoReady;
    private boolean drawingClock;
    private int materialOverride = -1;
    private float bottomContentTop = Float.MAX_VALUE;
    private float frameZoom = 1f, frameFocusX = .5f, frameFocusY = .5f;
    private final long motionStart = android.os.SystemClock.uptimeMillis();
    private String loadedBackground = "";
    private String loadedForeground = "";
    private int displayedPreset;
    private float transition = 1f;
    private ValueAnimator animator;

    public AuroraSceneView(Context context, boolean preview) {
        this(context, preview, new AuroraConfig(context));
    }

    public AuroraSceneView(Context context, boolean preview, AuroraConfig settings) {
        this(context, preview, settings, 1800);
    }
    public AuroraSceneView(Context context, boolean preview, AuroraConfig settings, int imageLimit) {
        super(context);
        decodeLimit = imageLimit;
        this.preview = preview;
        config = settings;
        displayedPreset = config.clockPreset();
        setLayerType(LAYER_TYPE_HARDWARE, null);
        reload();
    }

    public void setEditing(boolean value) {
        editing = value;
        invalidate();
    }

    private int clockOffset(float height) {
        if (preview && !config.prefs().contains("clockYRatio"))
            return Math.round(config.clockY() * height / getResources().getDisplayMetrics().heightPixels);
        return config.clockYForHeight(height);
    }

    public void startClockDrag() {
        dragClockStart = clockOffset(getHeight());
        dragClockX = config.clockX();
    }

    public int safeClockY(int desired) {
        float h = Math.max(1, getHeight());
        return Math.max((int) (-h * 0.08f), Math.min((int) (h * 0.42f), desired));
    }

    public void moveClock(float deltaX, float deltaY) {
        config.saveClockPosition(dragClockX + deltaX / Math.max(1, getWidth()),
                safeClockY(dragClockStart + Math.round(deltaY)), getHeight());
        invalidate();
    }

    public void setBottomContentTop(float y) {
        if (bottomContentTop != y) { bottomContentTop = y; invalidate(); }
    }

    public void setVideoReady(boolean value) { videoReady = value; invalidate(); }
    public boolean isVideoReady() { return videoReady; }

    public void moveClock(float delta) {
        int y = safeClockY(dragClockStart + Math.round(delta));
        config.prefs().edit().putInt("clockY", y).apply();
        invalidate();
    }

    public void animatePreset(int preset) {
        if (!config.motionEnabled() || !ValueAnimator.areAnimatorsEnabled()) {
            displayedPreset=preset; transition=1;invalidate();return;
        }
        if (preset == displayedPreset && transition >= 1f) {
            invalidate();
            return;
        }
        if (animator != null) animator.cancel();
        displayedPreset = preset;
        transition = 0f;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(420);
        animator.setInterpolator(new android.view.animation.PathInterpolator(.2f,.85f,.2f,1f));
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
                int max = decodeLimit;
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

    private Bitmap decodeFile(File file) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), o);
        o.inSampleSize = 1;
        int max = decodeLimit;
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
            if (frosted != null) frosted.recycle();
            frosted = null;
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
                frameZoom, frameFocusX, frameFocusY);
        paint.reset();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap, null, new RectF(t.dx, t.dy, t.dx + bitmap.getWidth() * t.scale,
                t.dy + bitmap.getHeight() * t.scale), paint);
    }

    public void drawFrostedBackdrop(Canvas canvas, View owner) {
        if (background == null || videoReady || getWidth() == 0 || getHeight() == 0) {
            canvas.drawColor(0xFF172432); return;
        }
        if (frosted == null) {
            float scale = Math.min(1, Math.min(180f/background.getWidth(), 400f/background.getHeight()));
            int width = Math.max(1,Math.round(background.getWidth()*scale));
            int height = Math.max(1,Math.round(background.getHeight()*scale));
            Bitmap small = Bitmap.createScaledBitmap(background,width,height,true);
            int[] pixels = new int[width*height]; small.getPixels(pixels,0,width,0,0,width,height);
            frosted = Bitmap.createBitmap(GlassBlur.blur(pixels,width,height,3),width,height,Bitmap.Config.ARGB_8888);
            if (small != background) small.recycle();
        }
        int[] origin = new int[2], position = new int[2];getLocationInWindow(origin);owner.getLocationInWindow(position);
        int save = canvas.save();canvas.translate(origin[0]-position[0],origin[1]-position[1]);
        Paint backdrop = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        WallpaperTransform transform = WallpaperTransform.compute(frosted.getWidth(),frosted.getHeight(),getWidth(),getHeight(),
                frameZoom,frameFocusX,frameFocusY);
        canvas.drawBitmap(frosted,null,new RectF(transform.dx,transform.dy,
                transform.dx+frosted.getWidth()*transform.scale,transform.dy+frosted.getHeight()*transform.scale),backdrop);
        canvas.restoreToCount(save);
    }

    private int clockColour() {
        int manual = config.clockColor();
        int alpha = Math.round(config.clockOpacity() * 255f * (.65f + .35f * transition));
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
        int a = Math.round(config.clockOpacity() * 255f * (.65f + .35f * transition));
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
        int material = drawingClock && !videoReady ? (materialOverride >= 0 ? materialOverride : config.clockMaterial()) : 0;
        if (material == 1 || material == 3) {
            int alpha = (color >>> 24) & 255;
            int light = (alpha << 24) | 0x00FFFFFF;
            int shade = (Math.round(alpha * .65f) << 24) | (color & 0x00FFFFFF);
            paint.setShader(new LinearGradient(0, baseline - size, size * .35f, baseline,
                    new int[]{light, shade, color}, new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP));
        } else if (material == 2) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(.8f, size * .005f));
        } else if (material == 4) {
            // Neon / Double-line outline
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2.5f, size * 0.018f));
            paint.setColor((color & 0x00FFFFFF) | 0x38000000);
            c.drawText(s, x, baseline, paint);

            paint.setStrokeWidth(Math.max(1.3f, size * 0.009f));
            paint.setColor(color);
            c.drawText(s, x, baseline, paint);

            paint.setStrokeWidth(Math.max(0.6f, size * 0.003f));
            paint.setColor((color & 0x00FFFFFF) | 0x99FFFFFF);
            c.drawText(s, x, baseline, paint);
            return;
        }
        c.drawText(s, x, baseline, paint);
        if (material == 1) {
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(.6f, size * .003f));
            paint.setColor((Math.round(((color >>> 24) & 255) * .8f) << 24) | 0x00FFFFFF);
            c.drawText(s, x, baseline, paint);
        }
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;
        float motion = config.photoMotion() && config.motionEnabled() && !preview && !videoReady && ValueAnimator.areAnimatorsEnabled()
                ? (android.os.SystemClock.uptimeMillis() - motionStart) / 1000f : 0;
        frameZoom = config.wallpaperZoom() * (motion == 0 ? 1f : 1.025f);
        frameFocusX = config.wallpaperFocusX() + (motion == 0 ? 0 : (float) Math.sin(motion * .15f) * .025f);
        frameFocusY = config.wallpaperFocusY() + (motion == 0 ? 0 : (float) Math.cos(motion * .12f) * .015f);
        if (videoReady) c.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        else c.drawColor(0xFF071018);
        if (!videoReady && background != null) imageCover(c, background, width, height);
        else if (!videoReady) {
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
        String[] dateFormats = {"EEE, d MMM", "EEEE, d MMMM", "dd / MM / yyyy", ""};
        String date = config.dateStyle() == 3 ? "" : new SimpleDateFormat(
                dateFormats[Math.max(0, Math.min(2, config.dateStyle()))], Locale.getDefault()).format(now).toUpperCase(Locale.getDefault());
        String digits = new SimpleDateFormat(config.clock12() ? "hhmm" : "HHmm", Locale.getDefault()).format(now);
        String hours = digits.substring(0, 2);
        String minutes = digits.substring(2);
        Typeface face = clockFace();
        Typeface dateFace = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        measure.setTypeface(face);
        measure.setTextSize(100f);
        measure.setLetterSpacing(-0.07f);
        ClockLayout layout = ClockLayout.compute(config.clockPreset(), width, height, config.clockSize(),
                clockOffset(height), config.clockWidth(), config.clockSpacing(),
                measure.measureText(hours) / 100f, measure.measureText(minutes) / 100f,
                measure.measureText(config.clockPreset() == 3 ? digits : hours + ":" + minutes) / 100f,
                config.clockStretch(), config.clockX());
        int color = clockColour();
        int dateColor = (Math.round(Math.max(.72f, config.clockOpacity()) * 255) << 24) | (color & 0x00FFFFFF);
        if (config.clockPreset() < 5) text(c, date, layout.dateX, layout.dateY, Math.max(11f, height * 0.012f), dateColor, dateFace, true, 0.14f);

        drawingClock = true;
        c.save();
        c.translate(layout.clockX, 0);
        c.scale(layout.horizontalScale, layout.verticalScale);
        if (config.clockPreset() >= 5) {
            // Poster templates have their own full-screen coordinates.
        } else if (layout.showColon) {
            text(c, hours + ":" + minutes, 0f, layout.firstBaseline / layout.verticalScale, layout.fittedSize, color, face, true, -0.06f);
        } else if (layout.editorialFour) {
            text(c, digits, 0f, layout.firstBaseline / layout.verticalScale, layout.fittedSize, color, face, true, -0.08f);
        } else {
            materialOverride = config.hoursMaterial();
            text(c, hours, 0f, layout.firstBaseline / layout.verticalScale, layout.fittedSize, color, face, true, -0.08f);
            materialOverride = config.minutesMaterial();
            text(c, minutes, 0f, (layout.firstBaseline + layout.lineHeight) / layout.verticalScale, layout.fittedSize, color, face, true, -0.08f);
        }
        c.restore();
        drawingClock = false;
        materialOverride = -1;
        if (config.clockPreset() >= 5) {
            drawPoster(c, digits, color, false, width, height);
            if (!config.outlineOverSubject()) drawPoster(c, digits, color, true, width, height);
        }
        if (config.showSeconds()) {
            text(c, new SimpleDateFormat("ss", Locale.getDefault()).format(now), layout.clockX + width * .22f,
                    layout.firstBaseline, height * .025f, color, face, true, .02f);
        }

        if (!videoReady && foreground != null) {
            paint.reset();
            paint.setAntiAlias(true);
            paint.setFilterBitmap(true);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            WallpaperTransform t = WallpaperTransform.compute(foreground.getWidth(), foreground.getHeight(), width, height,
                    frameZoom, frameFocusX, frameFocusY);
            c.drawBitmap(foreground, null, new RectF(t.dx, t.dy,
                    t.dx + foreground.getWidth() * t.scale, t.dy + foreground.getHeight() * t.scale), paint);
        }

        c.restoreToCount(clockLayer);
        if (config.clockPreset() >= 5) {
            if (config.outlineOverSubject()) drawPoster(c, digits, color, true, width, height);
            if (config.dateStyle() != 3 && config.clockPreset() < 13) {
                if (config.clockPreset() == 6) {
                    String day = new SimpleDateFormat("EEEE", Locale.getDefault()).format(now).toUpperCase(Locale.getDefault());
                    for (int i = 0; i < day.length(); i++) text(c, day.substring(i, i + 1), width * .92f,
                            height * .12f + i * height * .025f, height * .019f, dateColor, dateFace, true, 0);
                }
                text(c, date, width * .5f, config.clockPreset() == 10 ? height * .35f
                        : NotificationDisplay.dateBaseline(height, bottomContentTop), height * .014f, dateColor, dateFace, true, .04f);
            }
        }

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
        if (motion != 0 && isShown()) postInvalidateDelayed(33);
    }

    private void drawPoster(Canvas c, String digits, int color, boolean front, float w, float h) {
        int preset = config.clockPreset();
        Typeface face = Typeface.create(Typeface.create("sans-serif-condensed", Typeface.NORMAL),
                Math.round(preset == 9 ? 100 + config.clockWeight() * 500 : 650 + config.clockWeight() * 250), false);
        c.save();
        if(preset==15) {
            au.steve.hyperlauncher.compat.aurora.core.PosterFit fit=
                    au.steve.hyperlauncher.compat.aurora.core.PosterFit.neon(w,h,config.clockSize(),
                            config.clockStretch(),w*config.clockX(),clockOffset(h),bottomContentTop);
            c.translate(fit.x,fit.y);c.scale(fit.scaleX,fit.scaleY);
        } else {
            c.translate(w * config.clockX(), clockOffset(h));
            c.scale(config.clockSize() / 168f, config.clockStretch(), w / 2, h * .3f);
        }
        drawingClock = true;
        materialOverride = front ? (config.minutesMaterial() < 0 ? 2 : config.minutesMaterial())
                : (config.hoursMaterial() < 0 ? 0 : config.hoursMaterial());
        String pair = front ? digits.substring(2) : digits.substring(0, 2);
        if (preset == 9 || preset == 10) {
            if (!front) boxText(c, digits, new RectF(w*.07f,h*.08f,w*.93f,
                    h*(preset == 9 ? .65f : .31f)), color, face);
        } else if (preset == 11) {
            if (front) {
                materialOverride = config.hoursMaterial() < 0 ? 0 : config.hoursMaterial();
                Typeface serif = Typeface.create("serif", Typeface.ITALIC);
                boxText(c, digits.substring(0,2) + ":" + digits.substring(2),
                        new RectF(w*.20f,h*.55f,w*.80f,h*.63f), color, serif);
            } else {
                String title = config.headline();
                if (title.isEmpty()) title = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date()).toUpperCase(Locale.getDefault());
                materialOverride = 0;
                boxText(c, title, new RectF(w*.08f,h*.09f,w*.92f,h*.15f),
                        (color & 0x00FFFFFF) | 0x66000000, Typeface.create("serif", Typeface.NORMAL));
            }
        } else if (preset == 12) {
            materialOverride = front ? (config.minutesMaterial() < 0 ? 2 : config.minutesMaterial())
                    : (config.hoursMaterial() < 0 ? 2 : config.hoursMaterial());
            boxText(c, pair, front ? new RectF(w*.13f,h*.46f,w*.87f,h*.81f)
                    : new RectF(w*.13f,h*.09f,w*.87f,h*.43f), color, face);
        } else if (preset == 5) {
            boxText(c, pair, front ? new RectF(w*.50f,h*.18f,w*.94f,h*.55f)
                    : new RectF(w*.06f,h*.10f,w*.49f,h*.55f), color, face);
        } else if (preset == 6) {
            boxText(c, pair, front ? new RectF(w*.13f,h*.46f,w*.87f,h*.81f)
                    : new RectF(w*.13f,h*.09f,w*.87f,h*.43f), color, face);
        } else if (preset == 7) {
            float top = front ? .45f : .18f;
            boxText(c, pair.substring(0,1), new RectF(w*.08f,h*top,w*.48f,h*(top+.23f)), color, face);
            boxText(c, pair.substring(1), new RectF(w*.52f,h*(top+.08f),w*.92f,h*(top+.31f)), color, face);
        } else if (preset == 8) {
            c.translate(w*.40f, h*.10f);
            c.rotate(90);
            boxText(c, pair, front ? new RectF(h*.23f,0,h*.45f,w*.32f)
                    : new RectF(0,0,h*.22f,w*.32f), color, face);
        } else if (preset == 13) {
            // HyperOS 4 Glass Frame with custom signature
            float left = w * 0.08f;
            float right = w * 0.92f;
            float top = h * 0.125f;
            float bottom = h * 0.395f;
            RectF frame = new RectF(left, top, right, bottom);
            float corner = 28f * getResources().getDisplayMetrics().density;
            if (!front) {
                paint.reset();
                paint.setAntiAlias(true);
                paint.setShader(new LinearGradient(left, top, right, bottom,
                        new int[]{0x33FFFFFF, 0x14FFFFFF, 0x22FFFFFF},
                        new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
                c.drawRoundRect(frame, corner, corner, paint);
                paint.setShader(null);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(1.5f, w * 0.0035f));
                paint.setColor(0x55FFFFFF);
                c.drawRoundRect(frame, corner, corner, paint);
                paint.setStyle(Paint.Style.FILL);

                if (config.dateStyle() != 3) {
                    Typeface dateFace = Typeface.create("sans-serif-medium", Typeface.NORMAL);
                    String dateStr = new SimpleDateFormat("EEE, d MMMM", Locale.getDefault()).format(new Date()).toUpperCase(Locale.getDefault());
                    text(c, dateStr, w * 0.5f, top + (bottom - top) * 0.22f, h * 0.015f, 0xDDEEFAFF, dateFace, true, 0.10f);
                }

                materialOverride = 1; // Glass material
                Typeface clockTypeface = Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), 380, false);
                boxText(c, digits.substring(0, 2) + ":" + digits.substring(2),
                        new RectF(w * 0.14f, top + (bottom - top) * 0.26f, w * 0.86f, top + (bottom - top) * 0.76f),
                        color, clockTypeface);
                materialOverride = -1;

                if (config.showSignature()) {
                    String sig = config.signature();
                    if (sig != null && !sig.trim().isEmpty()) {
                        Typeface sigFace = Typeface.create("sans-serif-light", Typeface.NORMAL);
                        text(c, "✦  " + sig.trim() + "  ✦", w * 0.5f, top + (bottom - top) * 0.90f, h * 0.013f, 0xBBEDF5FF, sigFace, true, 0.08f);
                    }
                }
            } else if (config.outlineOverSubject()) {
                paint.reset();
                paint.setAntiAlias(true);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(1.8f, w * 0.004f));
                paint.setColor(0x77FFFFFF);
                c.drawRoundRect(frame, corner, corner, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        } else if (preset == 14) {
            // Movie: Up In The Air (top bold title + bottom right movie time badge)
            if (!front) {
                String title = config.movieTitle();
                if (title == null || title.trim().isEmpty()) title = "UP IN\nTHE AIR";
                String[] lines = title.split("\n");
                Typeface movieFace = Typeface.create(Typeface.create("sans-serif-condensed", Typeface.NORMAL), 900, false);
                float startY = h * 0.10f;
                float lineStep = h * 0.075f;
                for (int i = 0; i < lines.length && i < 3; i++) {
                    String line = lines[i].trim().toUpperCase(Locale.getDefault());
                    if (!line.isEmpty()) {
                        boxText(c, line, new RectF(w * 0.08f, startY + i * lineStep, w * 0.65f, startY + (i + 0.92f) * lineStep), 0xEEFFFFFF, movieFace);
                    }
                }
            } else {
                Date now = new Date();
                String dateStr = new SimpleDateFormat("d.M", Locale.getDefault()).format(now);
                String timeStr = digits.substring(0, 2) + ":" + digits.substring(2);
                String dayStr = new SimpleDateFormat("EEE", Locale.getDefault()).format(now).toUpperCase(Locale.getDefault());

                Typeface badgeFace = Typeface.create("sans-serif-condensed", Typeface.BOLD);
                float rx = w * 0.78f;
                text(c, dateStr, rx, h * 0.725f, h * 0.022f, 0xCCFFFFFF, badgeFace, true, 0.06f);
                int timeColor = (color & 0x00FFFFFF) == 0x00FFFFFF ? 0xFFFF453A : color;
                text(c, timeStr, rx, h * 0.775f, h * 0.046f, timeColor, badgeFace, true, -0.04f);
                text(c, dayStr, rx, h * 0.812f, h * 0.020f, 0xCCFFFFFF, badgeFace, true, 0.14f);
            }
        } else if (preset == 15) {
            // Movie: Neon Dog / Double Outline with vertical date capsule pill
            materialOverride = 4; // Neon double outline
            Typeface neonFace = Typeface.create(Typeface.create("sans-serif-condensed", Typeface.NORMAL), 800, false);
            boxText(c, pair, front ? new RectF(w * 0.10f, h * 0.44f, w * 0.90f, h * 0.79f)
                    : new RectF(w * 0.10f, h * 0.09f, w * 0.90f, h * 0.44f), color, neonFace);
            materialOverride = -1;

            if (front) {
                Date now = new Date();
                String dayStr = new SimpleDateFormat("EEE", Locale.getDefault()).format(now).toUpperCase(Locale.getDefault());
                String dateStr = new SimpleDateFormat("d/M", Locale.getDefault()).format(now);

                float pl = w * 0.78f, pt = h * 0.075f, pr = w * 0.92f, pb = h * 0.170f;
                RectF pill = new RectF(pl, pt, pr, pb);
                float radius = (pr - pl) * 0.5f;

                paint.reset();
                paint.setAntiAlias(true);
                paint.setColor(0x55000000);
                c.drawRoundRect(pill, radius, radius, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(1.2f, w * 0.003f));
                paint.setColor(0x66FFFFFF);
                c.drawRoundRect(pill, radius, radius, paint);
                paint.setStyle(Paint.Style.FILL);

                Typeface capFace = Typeface.create("sans-serif-medium", Typeface.NORMAL);
                text(c, dateStr, (pl + pr) * 0.5f, pt + (pb - pt) * 0.44f, h * 0.016f, 0xFFFFFFFF, capFace, true, 0.04f);
                text(c, dayStr, (pl + pr) * 0.5f, pt + (pb - pt) * 0.82f, h * 0.015f, 0xCCFFFFFF, capFace, true, 0.08f);
            }
        } else if (preset == 16) {
            // Movie: Left Cyan Block Poster (snowboarder)
            int blockColor = (color & 0x00FFFFFF) == 0x00FFFFFF ? 0xFF00E5FF : color;
            Typeface blockFace = Typeface.create(Typeface.create("sans-serif-condensed", Typeface.NORMAL), 900, false);
            if (!front) {
                Date now = new Date();
                String dayStr = new SimpleDateFormat("EEE", Locale.getDefault()).format(now).toUpperCase(Locale.getDefault());
                String dateStr = new SimpleDateFormat("d/M", Locale.getDefault()).format(now);

                boxText(c, digits, new RectF(w * 0.08f, h * 0.11f, w * 0.48f, h * 0.24f), blockColor, blockFace);
                boxText(c, dayStr, new RectF(w * 0.08f, h * 0.245f, w * 0.48f, h * 0.315f), blockColor, blockFace);
                boxText(c, dateStr, new RectF(w * 0.08f, h * 0.320f, w * 0.48f, h * 0.380f), blockColor, blockFace);
            } else if (config.outlineOverSubject()) {
                materialOverride = 2; // Outline
                Date now = new Date();
                String dayStr = new SimpleDateFormat("EEE", Locale.getDefault()).format(now).toUpperCase(Locale.getDefault());
                String dateStr = new SimpleDateFormat("d/M", Locale.getDefault()).format(now);

                boxText(c, digits, new RectF(w * 0.08f, h * 0.11f, w * 0.48f, h * 0.24f), blockColor, blockFace);
                boxText(c, dayStr, new RectF(w * 0.08f, h * 0.245f, w * 0.48f, h * 0.315f), blockColor, blockFace);
                boxText(c, dateStr, new RectF(w * 0.08f, h * 0.320f, w * 0.48f, h * 0.380f), blockColor, blockFace);
                materialOverride = -1;
            }
        } else if (preset == 17) {
            // Classic Left (clean modern digits on left + date & signature)
            if (!front) {
                Date now = new Date();
                String dateStr = new SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(now).toUpperCase(Locale.getDefault());
                Typeface cleanFace = Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), 700, false);
                Typeface dateFace = Typeface.create("sans-serif-medium", Typeface.NORMAL);

                text(c, dateStr, w * 0.08f, h * 0.22f, h * 0.016f, 0xDDEEFF, dateFace, false, 0.10f);

                String timeStr = digits.substring(0, 2) + ":" + digits.substring(2);
                boxText(c, timeStr, new RectF(w * 0.07f, h * 0.24f, w * 0.62f, h * 0.36f), color, cleanFace);

                if (config.showSignature()) {
                    String sig = config.signature();
                    if (sig != null && !sig.trim().isEmpty()) {
                        Typeface sigFace = Typeface.create("sans-serif-light", Typeface.NORMAL);
                        text(c, sig.trim(), w * 0.08f, h * 0.395f, h * 0.015f, 0xCCFFFFFF, sigFace, false, 0.04f);
                    }
                }
            } else if (config.outlineOverSubject()) {
                materialOverride = 2;
                Typeface cleanFace = Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), 700, false);
                String timeStr = digits.substring(0, 2) + ":" + digits.substring(2);
                boxText(c, timeStr, new RectF(w * 0.07f, h * 0.24f, w * 0.62f, h * 0.36f), color, cleanFace);
                materialOverride = -1;
            }
        } else if (preset == 18) {
            // Ultra-Tall Art (4 stretched digits spanning screen with date pill)
            Typeface tallFace = Typeface.create(Typeface.create("sans-serif-condensed", Typeface.NORMAL), 200, false);
            float digitW = w * 0.21f;
            float topY = h * 0.08f;
            float botY = h * 0.72f;

            for (int i = 0; i < 4; i++) {
                String digit = digits.substring(i, i + 1);
                float dx = w * 0.06f + i * (digitW + w * 0.015f);
                boxText(c, digit, new RectF(dx, topY, dx + digitW, botY), color, tallFace);
            }

            if (front) {
                Date now = new Date();
                String dateStr = new SimpleDateFormat("d/M", Locale.getDefault()).format(now);
                float pl = w * 0.08f, pt = h * 0.76f, pr = w * 0.26f, pb = h * 0.805f;
                RectF pill = new RectF(pl, pt, pr, pb);
                float rad = (pb - pt) * 0.5f;

                paint.reset();
                paint.setAntiAlias(true);
                paint.setColor(0x55000000);
                c.drawRoundRect(pill, rad, rad, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(1.2f, w * 0.003f));
                paint.setColor(0x66FFFFFF);
                c.drawRoundRect(pill, rad, rad, paint);
                paint.setStyle(Paint.Style.FILL);

                Typeface pillFace = Typeface.create("sans-serif-medium", Typeface.NORMAL);
                text(c, dateStr, (pl + pr) * 0.5f, pt + (pb - pt) * 0.70f, h * 0.016f, 0xFFFFFFFF, pillFace, true, 0.06f);
            }
        }
        materialOverride = -1;
        drawingClock = false;
        c.restore();
    }

    private void boxText(Canvas c, String value, RectF box, int color, Typeface face) {
        measure.setTypeface(face); measure.setTextSize(100); measure.setLetterSpacing(-.03f);
        android.graphics.Rect bounds = new android.graphics.Rect();
        measure.getTextBounds(value, 0, value.length(), bounds);
        if (bounds.width() <= 0 || bounds.height() <= 0) return;
        c.save(); c.translate(box.left, box.top);
        c.scale(box.width() / bounds.width(), box.height() / bounds.height());
        text(c, value, -bounds.left, -bounds.top, 100, color, face, false, -.03f);
        c.restore();
    }

    public void releaseBitmaps() {
        if (background != null) { background.recycle(); background=null; }
        if (foreground != null) { foreground.recycle(); foreground=null; }
        if (frosted != null) { frosted.recycle(); frosted=null; }
    }

    @Override protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    private static int mix(int color, float amount) {
        int a = Math.round(((color >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, amount)));
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
