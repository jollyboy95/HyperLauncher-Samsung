/* Original HyperLauncher wallpaper studio. SPDX-License-Identifier: Apache-2.0 */
package au.steve.hyperlauncher.wallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.launcher3.R;

/** Original on-device gradient wallpapers, no downloaded artwork required. */
public final class HyperWallpaperActivity extends Activity {
    private static final int INK = Color.rgb(22, 24, 29);
    private static final int MUTED = Color.rgb(105, 112, 124);
    private static final int ACCENT = Color.rgb(45, 108, 223);

    private static final int[] NAMES = {
            R.string.hyper_wallpaper_dawn,
            R.string.hyper_wallpaper_ocean,
            R.string.hyper_wallpaper_mint,
            R.string.hyper_wallpaper_graphite,
            R.string.hyper_wallpaper_sunset,
            R.string.hyper_wallpaper_aurora,
    };

    private static final int[][] PALETTES = {
            {0xFFF25F4B, 0xFFF6A15C, 0xFF6C4BFF},
            {0xFF071E4A, 0xFF1463B8, 0xFF64D2FF},
            {0xFF073B36, 0xFF10A37F, 0xFFB7F7D8},
            {0xFF0B1220, 0xFF2B3648, 0xFF6B7A90},
            {0xFF4A1020, 0xFFE4572E, 0xFFFFC857},
            {0xFF14103A, 0xFF7C3AED, 0xFF22D3EE},
    };

    private CheckBox alsoLock;

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(245, 246, 248));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(245, 246, 248));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(40), dp(22), dp(36));
        scroll.addView(content);

        TextView back = new TextView(this);
        back.setText("‹  Back");
        back.setTextSize(17);
        back.setTextColor(ACCENT);
        back.setPadding(0, 0, 0, dp(20));
        back.setOnClickListener(v -> finish());
        content.addView(back);

        TextView title = new TextView(this);
        title.setText(R.string.hyper_wallpaper_title);
        title.setTextSize(32);
        title.setTextColor(INK);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.hyper_wallpaper_subtitle);
        subtitle.setTextSize(14);
        subtitle.setTextColor(MUTED);
        subtitle.setPadding(0, dp(6), 0, dp(18));
        content.addView(subtitle);

        alsoLock = new CheckBox(this);
        alsoLock.setText(R.string.hyper_wallpaper_also_lock);
        alsoLock.setTextColor(INK);
        alsoLock.setPadding(0, 0, 0, dp(12));
        content.addView(alsoLock);

        TextView tapHint = new TextView(this);
        tapHint.setText(R.string.hyper_wallpaper_apply);
        tapHint.setTextSize(12);
        tapHint.setTextColor(MUTED);
        tapHint.setPadding(0, 0, 0, dp(12));
        content.addView(tapHint);

        for (int index = 0; index < NAMES.length; index++) {
            final int selected = index;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(rippleCard());
            card.setClipToOutline(true);
            card.setOnClickListener(v -> applyWallpaper(selected));

            Preview preview = new Preview(this, index);
            GradientDrawable previewMask = new GradientDrawable();
            float radius = dp(22);
            previewMask.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
            preview.setBackground(previewMask);
            preview.setClipToOutline(true);
            card.addView(preview, new LinearLayout.LayoutParams(-1, dp(168)));

            LinearLayout labelRow = new LinearLayout(this);
            labelRow.setOrientation(LinearLayout.HORIZONTAL);
            labelRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            labelRow.setPadding(dp(16), dp(12), dp(16), dp(14));

            TextView name = new TextView(this);
            name.setText(NAMES[index]);
            name.setTextSize(16);
            name.setTextColor(INK);
            name.setTypeface(null, Typeface.BOLD);
            labelRow.addView(name, new LinearLayout.LayoutParams(0, -2, 1));

            TextView apply = new TextView(this);
            apply.setText(R.string.hyper_wallpaper_apply);
            apply.setTextSize(13);
            apply.setTextColor(ACCENT);
            labelRow.addView(apply);

            card.addView(labelRow);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.bottomMargin = dp(14);
            content.addView(card, params);

            card.setAlpha(0f);
            card.setTranslationY(dp(14));
            card.animate().alpha(1f).translationY(0f)
                    .setStartDelay(40L * index).setDuration(240L).start();
        }

        Button systemPicker = new Button(this);
        systemPicker.setText(R.string.hyper_wallpaper_system);
        systemPicker.setAllCaps(false);
        systemPicker.setTextColor(Color.WHITE);
        systemPicker.setBackground(rounded(ACCENT, 20, 0, 0));
        systemPicker.setPadding(dp(18), dp(12), dp(18), dp(12));
        systemPicker.setOnClickListener(v -> openSystemPicker());
        LinearLayout.LayoutParams pickerParams = new LinearLayout.LayoutParams(-1, -2);
        pickerParams.topMargin = dp(8);
        content.addView(systemPicker, pickerParams);

        TextView note = new TextView(this);
        note.setText(R.string.hyper_wallpaper_note);
        note.setTextSize(11);
        note.setTextColor(MUTED);
        note.setPadding(0, dp(16), 0, 0);
        content.addView(note);

        setContentView(scroll);
    }

    private void applyWallpaper(int index) {
        try {
            int width = Math.max(getResources().getDisplayMetrics().widthPixels, 1080);
            int height = Math.max(getResources().getDisplayMetrics().heightPixels, 1920);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            drawWallpaper(canvas, width, height, index, paint);

            WallpaperManager manager = WallpaperManager.getInstance(this);
            manager.suggestDesiredDimensions(width, height);
            int flags = WallpaperManager.FLAG_SYSTEM
                    | (alsoLock.isChecked() ? WallpaperManager.FLAG_LOCK : 0);
            manager.setBitmap(bitmap, null, true, flags);
            bitmap.recycle();
            Toast.makeText(this, R.string.hyper_wallpaper_applied, Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, R.string.hyper_wallpaper_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void openSystemPicker() {
        try {
            startActivity(Intent.createChooser(
                    new Intent(Intent.ACTION_SET_WALLPAPER),
                    getString(R.string.hyper_wallpaper_system)));
        } catch (Exception error) {
            Toast.makeText(this, R.string.hyper_wallpaper_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private static void drawWallpaper(Canvas canvas, int width, int height, int index, Paint paint) {
        int[] palette = PALETTES[index];
        paint.setShader(new LinearGradient(0, 0, width, height,
                palette, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paint);

        float radius = Math.max(width, height) * 0.62f;
        paint.setShader(new RadialGradient(width * 0.82f, height * 0.16f, radius,
                0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paint);

        paint.setShader(new RadialGradient(width * 0.12f, height * 0.84f, radius * 0.82f,
                0x33FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paint);

        paint.setShader(new LinearGradient(0, height * 0.58f, 0, height,
                0x00000000, 0x55000000, Shader.TileMode.CLAMP));
        canvas.drawRect(0, height * 0.58f, width, height, paint);
        paint.setShader(null);
    }

    private GradientDrawable rounded(int color, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    private RippleDrawable rippleCard() {
        GradientDrawable content = rounded(Color.WHITE, 22, 1, Color.rgb(230, 232, 237));
        return new RippleDrawable(ColorStateList.valueOf(0x222D6CDF), content, null);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    private static final class Preview extends View {
        private final int index;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Preview(android.content.Context context, int index) {
            super(context);
            this.index = index;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawWallpaper(canvas, getWidth(), getHeight(), index, paint);
        }
    }
}
