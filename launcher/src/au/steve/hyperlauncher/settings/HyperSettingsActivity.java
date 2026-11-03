// Original HyperLauncher settings hub. SPDX-License-Identifier: Apache-2.0
package au.steve.hyperlauncher.settings;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import app.lawnchair.ui.preferences.PreferenceActivity;
import app.lawnchair.ui.preferences.navigation.*;

/** A home settings hub; every row opens a working launcher preference destination. */
public class HyperSettingsActivity extends Activity {
    private LinearLayout content;
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(245,245,245));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24),dp(48),dp(24),dp(32));
        scroll.addView(content);
        TextView back = new TextView(this);
        back.setText("‹  Back"); back.setTextSize(18); back.setTextColor(Color.BLACK);
        back.setPadding(0,0,0,dp(24)); back.setOnClickListener(v -> finish()); content.addView(back);
        TextView title = new TextView(this);
        title.setText("Home screen"); title.setTextSize(32); title.setTextColor(Color.BLACK);
        title.setTypeface(null, Typeface.BOLD); content.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("HyperLauncher • Make yourself at home"); subtitle.setTextColor(Color.DKGRAY);
        subtitle.setPadding(0,dp(8),0,dp(24)); content.addView(subtitle);
        row("Home screen layout", "Grid, icon size and labels", HomeScreen.INSTANCE);
        row("Icons and appearance", "Icon packs, shapes and colours", General.INSTANCE);
        row("App drawer", "Search, hidden apps and drawer layout", AppDrawer.INSTANCE);
        row("Folders", "Folder appearance and behaviour", Folders.INSTANCE);
        row("Dock", "Favourite apps and dock background", Dock.INSTANCE);
        row("Gestures", "Choose what swipes and taps do", Gestures.INSTANCE);
        row("All launcher settings", "Backup, restore and additional options", Root.INSTANCE);
        setContentView(scroll);
    }
    private void row(String title, String description, PreferenceRoute route) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(20),dp(18),dp(20),dp(18));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.WHITE); bg.setCornerRadius(dp(20)); card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(10); content.addView(card,lp);
        TextView heading = new TextView(this); heading.setText(title + "  ›"); heading.setTextSize(17); heading.setTextColor(Color.BLACK); card.addView(heading);
        TextView detail = new TextView(this); detail.setText(description); detail.setTextSize(13); detail.setTextColor(Color.DKGRAY); detail.setPadding(0,dp(5),0,0); card.addView(detail);
        card.setOnClickListener(v -> startActivity(PreferenceActivity.Companion.createIntent(this,route)));
    }
}
