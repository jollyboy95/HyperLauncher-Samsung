/* Original HyperLauncher settings hub. SPDX-License-Identifier: Apache-2.0 */
package au.steve.hyperlauncher.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.lawnchair.ui.preferences.PreferenceActivity;
import app.lawnchair.ui.preferences.navigation.*;
import com.android.launcher3.R;

import au.steve.hyperlauncher.wallpaper.HyperWallpaperActivity;
import au.steve.hyperlauncher.weather.HyperWeatherActivity;

/** A HyperOS-inspired home settings hub; every row opens a working destination. */
public class HyperSettingsActivity extends Activity {
    private static final int INK = Color.rgb(22, 24, 29);
    private static final int MUTED = Color.rgb(105, 112, 124);
    private static final int ACCENT = Color.rgb(45, 108, 223);
    private static final int CORAL = Color.rgb(235, 92, 73);
    private static final int MINT = Color.rgb(16, 163, 127);
    private static final int VIOLET = Color.rgb(124, 58, 237);
    private static final int AMBER = Color.rgb(217, 139, 20);

    private LinearLayout content;
    private int animationIndex;

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
        content = new LinearLayout(this);
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

        addHero();

        section(R.string.hyper_settings_section_personalise);
        row(R.string.hyper_settings_wallpaper_title, R.string.hyper_settings_wallpaper_detail, CORAL,
                () -> open(new Intent(this, HyperWallpaperActivity.class)));
        row(R.string.hyper_settings_weather_title, R.string.hyper_settings_weather_detail, AMBER,
                () -> open(new Intent(this, HyperWeatherActivity.class)));
        row(R.string.hyper_settings_home_title, R.string.hyper_settings_home_detail, ACCENT,
                () -> openPreference(HomeScreen.INSTANCE));
        row(R.string.hyper_settings_icons_title, R.string.hyper_settings_icons_detail, VIOLET,
                () -> openPreference(General.INSTANCE));
        row(R.string.hyper_settings_folders_title, R.string.hyper_settings_folders_detail, MINT,
                () -> openPreference(Folders.INSTANCE));
        row(R.string.hyper_settings_dock_title, R.string.hyper_settings_dock_detail, CORAL,
                () -> openPreference(Dock.INSTANCE));

        section(R.string.hyper_settings_section_behaviour);
        row(R.string.hyper_settings_drawer_title, R.string.hyper_settings_drawer_detail, ACCENT,
                () -> openPreference(AppDrawer.INSTANCE));
        row(R.string.hyper_settings_gestures_title, R.string.hyper_settings_gestures_detail, VIOLET,
                () -> openPreference(Gestures.INSTANCE));
        row(R.string.hyper_settings_all_title, R.string.hyper_settings_all_detail, MUTED,
                () -> openPreference(Root.INSTANCE));
        row(R.string.hyper_settings_about_title, R.string.hyper_settings_about_detail, MINT,
                this::showAbout);

        setContentView(scroll);
    }

    private void addHero() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(22), dp(22), dp(20));
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFF25F4B, 0xFFF6A15C, 0xFF6C4BFF});
        background.setCornerRadius(dp(26));
        card.setBackground(background);

        TextView title = new TextView(this);
        title.setText(R.string.hyper_settings_title);
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        card.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.hyper_settings_subtitle);
        subtitle.setTextSize(13);
        subtitle.setTextColor(0xE6FFFFFF);
        subtitle.setPadding(0, dp(6), 0, 0);
        card.addView(subtitle);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(24);
        content.addView(card, params);
        animateIn(card);
    }

    private void section(int titleRes) {
        TextView header = new TextView(this);
        header.setText(titleRes);
        header.setTextSize(11);
        header.setLetterSpacing(0.12f);
        header.setTextColor(MUTED);
        header.setTypeface(null, Typeface.BOLD);
        header.setPadding(dp(6), dp(4), 0, dp(10));
        content.addView(header);
    }

    private void row(int titleRes, int detailRes, int accent, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.setBackground(rippleCard());
        card.setClipToOutline(true);
        card.setOnClickListener(v -> action.run());

        View dot = new View(this);
        GradientDrawable dotBackground = new GradientDrawable();
        dotBackground.setColor(accent);
        dotBackground.setCornerRadius(dp(5));
        dot.setBackground(dotBackground);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(10), dp(10));
        dotParams.setMarginEnd(dp(14));
        card.addView(dot, dotParams);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        card.addView(text, new LinearLayout.LayoutParams(0, -2, 1));

        TextView heading = new TextView(this);
        heading.setText(titleRes);
        heading.setTextSize(16);
        heading.setTextColor(INK);
        heading.setTypeface(null, Typeface.BOLD);
        text.addView(heading);

        TextView detail = new TextView(this);
        detail.setText(detailRes);
        detail.setTextSize(12);
        detail.setTextColor(MUTED);
        detail.setPadding(0, dp(3), 0, 0);
        text.addView(detail);

        TextView chevron = new TextView(this);
        chevron.setText("›");
        chevron.setTextSize(22);
        chevron.setTextColor(Color.rgb(160, 166, 177));
        chevron.setPadding(dp(10), 0, 0, 0);
        card.addView(chevron);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        content.addView(card, params);
        animateIn(card);
    }

    private void animateIn(View view) {
        view.setAlpha(0f);
        view.setTranslationY(dp(12));
        view.animate().alpha(1f).translationY(0f)
                .setStartDelay(24L * animationIndex++)
                .setDuration(220L)
                .start();
    }

    private void openPreference(PreferenceRoute route) {
        open(PreferenceActivity.Companion.createIntent(this, route));
    }

    private void open(Intent intent) {
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showAbout() {
        String version;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            version = "preview";
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.hyper_settings_title)
                .setMessage("Version " + version
                        + "\n\nAn original HyperOS 4-inspired launcher built on the open-source "
                        + "Lawnchair project (Apache-2.0).\n\nWeather data and city search by "
                        + "Open-Meteo (CC BY 4.0). No API key, account or root access is used."
                        + "\n\nSamsung keeps ownership of fingerprint, face and PIN authentication, "
                        + "the system lock screen, Always On Display and system Recents.")
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private RippleDrawable rippleCard() {
        GradientDrawable card = rounded(Color.WHITE, 20);
        card.setStroke(dp(1), Color.rgb(231, 233, 238));
        return new RippleDrawable(ColorStateList.valueOf(0x222D6CDF), card, null);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }
}
