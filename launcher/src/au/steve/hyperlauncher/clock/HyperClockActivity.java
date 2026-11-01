/* Original HyperLauncher clock settings. SPDX-License-Identifier: Apache-2.0 */
package au.steve.hyperlauncher.clock;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import au.steve.hyperlauncher.widgets.HyperClockWidget;
import com.android.launcher3.R;

/** Style picker and live preview for the Hyper Clock widget. */
public final class HyperClockActivity extends Activity {
    private static final int INK = Color.rgb(22, 24, 29);
    private static final int MUTED = Color.rgb(105, 112, 124);
    private static final int ACCENT = Color.rgb(45, 108, 223);

    private static final String[] LAYOUTS = {
            HyperClockWidget.LAYOUT_STACKED,
            HyperClockWidget.LAYOUT_CENTERED,
            HyperClockWidget.LAYOUT_MONO,
    };
    private static final int[] TITLES = {
            R.string.hyper_clock_style_stacked,
            R.string.hyper_clock_style_centered,
            R.string.hyper_clock_style_mono,
    };
    private static final int[] DETAILS = {
            R.string.hyper_clock_style_stacked_detail,
            R.string.hyper_clock_style_centered_detail,
            R.string.hyper_clock_style_mono_detail,
    };

    private SharedPreferences prefs;
    private FrameLayout previewHost;
    private LinearLayout styleContainer;
    private CheckBox showDate;

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(HyperClockWidget.PREFS, MODE_PRIVATE);
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
        title.setText(R.string.hyper_clock_settings_title);
        title.setTextSize(32);
        title.setTextColor(INK);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.hyper_clock_settings_subtitle);
        subtitle.setTextSize(14);
        subtitle.setTextColor(MUTED);
        subtitle.setPadding(0, dp(6), 0, dp(20));
        content.addView(subtitle);

        section(content, R.string.hyper_clock_preview);
        previewHost = new FrameLayout(this);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, -2);
        previewParams.bottomMargin = dp(22);
        content.addView(previewHost, previewParams);

        section(content, R.string.hyper_clock_style_section);
        styleContainer = new LinearLayout(this);
        styleContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(styleContainer);

        showDate = new CheckBox(this);
        showDate.setText(R.string.hyper_clock_show_date);
        showDate.setTextColor(INK);
        showDate.setChecked(prefs.getBoolean(HyperClockWidget.KEY_SHOW_DATE, true));
        showDate.setPadding(0, dp(10), 0, dp(8));
        showDate.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(HyperClockWidget.KEY_SHOW_DATE, checked).apply();
            updatePreview();
            HyperClockWidget.requestUpdate(this);
        });
        content.addView(showDate);

        Button alarms = new Button(this);
        alarms.setText(R.string.hyper_clock_open_alarms);
        alarms.setAllCaps(false);
        alarms.setTextColor(Color.WHITE);
        alarms.setBackground(rounded(ACCENT, 20, 0, 0));
        alarms.setPadding(dp(18), dp(12), dp(18), dp(12));
        alarms.setOnClickListener(v -> {
            try {
                startActivity(new Intent(AlarmClock.ACTION_SHOW_ALARMS));
            } catch (Exception ignored) {
                // No alarm clock app is available.
            }
        });
        LinearLayout.LayoutParams alarmParams = new LinearLayout.LayoutParams(-1, -2);
        alarmParams.topMargin = dp(14);
        content.addView(alarms, alarmParams);

        TextView note = new TextView(this);
        note.setText(R.string.hyper_clock_note);
        note.setTextSize(11);
        note.setTextColor(MUTED);
        note.setPadding(0, dp(16), 0, 0);
        content.addView(note);

        setContentView(scroll);
        renderStyles();
        updatePreview();
    }

    private void section(LinearLayout content, int titleRes) {
        TextView header = new TextView(this);
        header.setText(titleRes);
        header.setTextSize(11);
        header.setLetterSpacing(0.12f);
        header.setTextColor(MUTED);
        header.setTypeface(null, Typeface.BOLD);
        header.setPadding(dp(6), dp(4), 0, dp(10));
        content.addView(header);
    }

    private void renderStyles() {
        styleContainer.removeAllViews();
        String selected = prefs.getString(HyperClockWidget.KEY_LAYOUT, HyperClockWidget.LAYOUT_STACKED);
        for (int index = 0; index < LAYOUTS.length; index++) {
            final String layout = LAYOUTS[index];
            boolean isSelected = layout.equals(selected);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            GradientDrawable background = rounded(isSelected ? 0xFFEAF1FF : Color.WHITE, 18);
            background.setStroke(dp(isSelected ? 2 : 1),
                    isSelected ? ACCENT : Color.rgb(231, 233, 238));
            card.setBackground(new RippleDrawable(ColorStateList.valueOf(0x222D6CDF), background, null));
            card.setOnClickListener(v -> {
                prefs.edit().putString(HyperClockWidget.KEY_LAYOUT, layout).apply();
                renderStyles();
                updatePreview();
                HyperClockWidget.requestUpdate(this);
            });

            LinearLayout text = new LinearLayout(this);
            text.setOrientation(LinearLayout.VERTICAL);
            card.addView(text, new LinearLayout.LayoutParams(0, -2, 1));

            TextView heading = new TextView(this);
            heading.setText(TITLES[index]);
            heading.setTextSize(16);
            heading.setTextColor(INK);
            heading.setTypeface(null, Typeface.BOLD);
            text.addView(heading);

            TextView detail = new TextView(this);
            detail.setText(DETAILS[index]);
            detail.setTextSize(12);
            detail.setTextColor(MUTED);
            detail.setPadding(0, dp(3), 0, 0);
            text.addView(detail);

            TextView check = new TextView(this);
            check.setText(isSelected ? "✓" : "");
            check.setTextSize(18);
            check.setTextColor(ACCENT);
            check.setPadding(dp(12), 0, 0, 0);
            card.addView(check);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.bottomMargin = dp(10);
            styleContainer.addView(card, params);

            card.setAlpha(0f);
            card.setTranslationY(dp(10));
            card.animate().alpha(1f).translationY(0f)
                    .setStartDelay(30L * index).setDuration(200L).start();
        }
    }

    private void updatePreview() {
        previewHost.removeAllViews();
        String layout = prefs.getString(HyperClockWidget.KEY_LAYOUT, HyperClockWidget.LAYOUT_STACKED);
        View preview = getLayoutInflater().inflate(HyperClockWidget.layoutResource(layout), previewHost, false);
        View date = preview.findViewById(R.id.hyper_clock_date);
        if (date != null) {
            date.setVisibility(showDate.isChecked() ? View.VISIBLE : View.GONE);
        }
        View gear = preview.findViewById(R.id.hyper_clock_settings);
        if (gear != null) gear.setVisibility(View.INVISIBLE);
        previewHost.addView(preview);
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable rounded(int color, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable drawable = rounded(color, radiusDp);
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }
}
