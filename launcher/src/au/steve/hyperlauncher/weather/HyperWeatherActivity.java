/* Original HyperLauncher weather settings. SPDX-License-Identifier: Apache-2.0 */
package au.steve.hyperlauncher.weather;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import au.steve.hyperlauncher.widgets.HyperWeatherWidget;
import com.android.launcher3.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** City search and unit selection for the Hyper Weather widget. */
public final class HyperWeatherActivity extends Activity {
    private static final int INK = Color.rgb(22, 24, 29);
    private static final int MUTED = Color.rgb(105, 112, 124);
    private static final int ACCENT = Color.rgb(45, 108, 223);

    private TextView current;
    private TextView status;
    private LinearLayout results;
    private Button search;
    private EditText query;
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private boolean configuring;

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        configuring = appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID;
        // A widget host only commits the placement when this activity returns RESULT_OK.
        if (configuring) setResult(RESULT_CANCELED);
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
        title.setText(R.string.hyper_weather_settings_title);
        title.setTextSize(32);
        title.setTextColor(INK);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(configuring
                ? R.string.hyper_weather_choose_city
                : R.string.hyper_weather_settings_subtitle);
        subtitle.setTextSize(14);
        subtitle.setTextColor(MUTED);
        subtitle.setPadding(0, dp(6), 0, dp(20));
        content.addView(subtitle);

        current = new TextView(this);
        current.setTextSize(13);
        current.setTextColor(ACCENT);
        current.setPadding(0, 0, 0, dp(14));
        content.addView(current);

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(searchRow, new LinearLayout.LayoutParams(-1, -2));

        query = new EditText(this);
        query.setHint(R.string.hyper_weather_city_hint);
        query.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        query.setSingleLine(true);
        query.setTextColor(INK);
        query.setHintTextColor(Color.rgb(150, 155, 165));
        query.setBackground(rounded(Color.WHITE, 18, 0, 0));
        query.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams queryParams = new LinearLayout.LayoutParams(0, -2, 1);
        queryParams.setMarginEnd(dp(10));
        searchRow.addView(query, queryParams);

        search = new Button(this);
        search.setText(R.string.hyper_weather_search);
        search.setAllCaps(false);
        search.setTextColor(Color.WHITE);
        search.setBackground(rounded(ACCENT, 18, 0, 0));
        search.setOnClickListener(v -> beginSearch());
        searchRow.addView(search, new LinearLayout.LayoutParams(-2, -2));

        SharedPreferences prefs = getSharedPreferences(HyperWeatherWidget.PREFS, MODE_PRIVATE);
        String savedCity = prefs.getString(HyperWeatherWidget.KEY_CITY, "");
        if (configuring && savedCity != null && !savedCity.isEmpty()) {
            Button useSaved = new Button(this);
            useSaved.setText(R.string.hyper_weather_use_current);
            useSaved.setAllCaps(false);
            useSaved.setTextColor(ACCENT);
            useSaved.setBackground(rounded(0xFFEAF1FF, 18, 0, 0));
            useSaved.setPadding(dp(16), dp(11), dp(16), dp(11));
            useSaved.setOnClickListener(v -> finishWithResult());
            LinearLayout.LayoutParams savedParams = new LinearLayout.LayoutParams(-1, -2);
            savedParams.topMargin = dp(12);
            content.addView(useSaved, savedParams);
        }

        CheckBox fahrenheit = new CheckBox(this);
        fahrenheit.setText(R.string.hyper_weather_fahrenheit);
        fahrenheit.setTextColor(INK);
        fahrenheit.setChecked(prefs.getBoolean(HyperWeatherWidget.KEY_FAHRENHEIT, false));
        fahrenheit.setPadding(0, dp(10), 0, dp(4));
        fahrenheit.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(HyperWeatherWidget.KEY_FAHRENHEIT, checked).apply();
            HyperWeatherWidget.requestUpdate(this);
        });
        content.addView(fahrenheit);

        status = new TextView(this);
        status.setTextSize(13);
        status.setTextColor(MUTED);
        status.setPadding(0, dp(8), 0, dp(10));
        content.addView(status);

        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        content.addView(results);

        TextView note = new TextView(this);
        note.setText(R.string.hyper_weather_data_note);
        note.setTextSize(11);
        note.setTextColor(MUTED);
        note.setPadding(0, dp(18), 0, 0);
        content.addView(note);

        showCurrentCity();
        setContentView(scroll);
    }

    private void showCurrentCity() {
        SharedPreferences prefs = getSharedPreferences(HyperWeatherWidget.PREFS, MODE_PRIVATE);
        String city = prefs.getString(HyperWeatherWidget.KEY_CITY, "");
        current.setText(city == null || city.isEmpty()
                ? getString(R.string.hyper_weather_no_city)
                : getString(R.string.hyper_weather_current_city, city));
    }

    private void beginSearch() {
        String text = query.getText().toString().trim();
        if (text.isEmpty()) {
            status.setText(R.string.hyper_weather_enter_city);
            return;
        }
        status.setText(R.string.hyper_weather_searching);
        search.setEnabled(false);
        results.removeAllViews();
        new Thread(() -> {
            try {
                String address = "https://geocoding-api.open-meteo.com/v1/search?name="
                        + URLEncoder.encode(text, "UTF-8") + "&count=6&language=en&format=json";
                JSONObject root = new JSONObject(read(address));
                JSONArray array = root.optJSONArray("results");
                List<Place> places = new ArrayList<>();
                if (array != null) {
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.getJSONObject(i);
                        String name = item.optString("name", "");
                        if (name.isEmpty()) continue;
                        String region = item.optString("admin1", "");
                        String country = item.optString("country", "");
                        places.add(new Place(name, region, country,
                                item.getDouble("latitude"), item.getDouble("longitude")));
                    }
                }
                runOnUiThread(() -> showPlaces(places));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    search.setEnabled(true);
                    status.setText(R.string.hyper_weather_error);
                });
            }
        }, "hyper-weather-geocode").start();
    }

    private void showPlaces(List<Place> places) {
        search.setEnabled(true);
        results.removeAllViews();
        if (places.isEmpty()) {
            status.setText(R.string.hyper_weather_no_results);
            return;
        }
        status.setText("");
        for (Place place : places) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(18), dp(14), dp(18), dp(14));
            card.setBackground(rippleCard());
            card.setOnClickListener(v -> apply(place));

            TextView heading = new TextView(this);
            heading.setText(place.name + (place.region.isEmpty() ? "" : ", " + place.region)
                    + (place.country.isEmpty() ? "" : ", " + place.country));
            heading.setTextSize(16);
            heading.setTextColor(INK);
            heading.setTypeface(null, Typeface.BOLD);
            card.addView(heading);

            TextView detail = new TextView(this);
            detail.setText(String.format(java.util.Locale.US, "%.3f, %.3f", place.latitude, place.longitude));
            detail.setTextSize(12);
            detail.setTextColor(MUTED);
            detail.setPadding(0, dp(4), 0, 0);
            card.addView(detail);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.bottomMargin = dp(10);
            results.addView(card, params);
        }
    }

    private void apply(Place place) {
        SharedPreferences prefs = getSharedPreferences(HyperWeatherWidget.PREFS, MODE_PRIVATE);
        prefs.edit()
                .putString(HyperWeatherWidget.KEY_CITY, place.name)
                .putFloat(HyperWeatherWidget.KEY_LAT, (float) place.latitude)
                .putFloat(HyperWeatherWidget.KEY_LON, (float) place.longitude)
                .apply();
        HyperWeatherWidget.requestUpdate(this);
        Toast.makeText(this, R.string.hyper_weather_saved, Toast.LENGTH_SHORT).show();
        finishWithResult();
    }

    /** Commit the host placement when this screen was opened as a widget configuration. */
    private void finishWithResult() {
        if (configuring) {
            Intent result = new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, result);
        }
        finish();
    }

    private String read(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.setRequestProperty("User-Agent", "HyperLauncher/1.0");
            connection.setRequestProperty("Accept", "application/json");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + connection.getResponseCode());
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            return body.toString();
        } finally {
            connection.disconnect();
        }
    }

    private GradientDrawable rounded(int color, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    private RippleDrawable rippleCard() {
        GradientDrawable content = rounded(Color.WHITE, 20, 1, Color.rgb(230, 232, 237));
        return new RippleDrawable(ColorStateList.valueOf(0x222D6CDF), content, null);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    private static final class Place {
        final String name;
        final String region;
        final String country;
        final double latitude;
        final double longitude;

        Place(String name, String region, String country, double latitude, double longitude) {
            this.name = name;
            this.region = region;
            this.country = country;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
