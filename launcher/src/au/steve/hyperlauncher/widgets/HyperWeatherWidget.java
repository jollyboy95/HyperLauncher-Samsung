/* Original HyperLauncher widget. SPDX-License-Identifier: Apache-2.0 */
package au.steve.hyperlauncher.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import au.steve.hyperlauncher.weather.HyperWeatherActivity;
import com.android.launcher3.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Original HyperOS-inspired weather card backed by the free Open-Meteo API.
 *
 * <p>The widget keeps the last successful reading in shared preferences, so an
 * offline device still shows useful data with an explicit offline marker.</p>
 */
public final class HyperWeatherWidget extends AppWidgetProvider {
    public static final String PREFS = "hyper_weather";
    public static final String KEY_CITY = "city_name";
    public static final String KEY_LAT = "latitude";
    public static final String KEY_LON = "longitude";
    public static final String KEY_FAHRENHEIT = "fahrenheit";

    private static final String KEY_CACHE_TEMP = "cache_temp";
    private static final String KEY_CACHE_CODE = "cache_code";
    private static final String KEY_CACHE_HIGH = "cache_high";
    private static final String KEY_CACHE_LOW = "cache_low";
    private static final String KEY_CACHE_TIME = "cache_time";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        final PendingResult pending = goAsync();
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                updateAll(app, manager, ids);
            } finally {
                pending.finish();
            }
        }, "hyper-weather-update").start();
    }

    private static void updateAll(Context context, AppWidgetManager manager, int[] ids) {
        WeatherSnapshot snapshot = load(context);
        RemoteViews views = build(context, snapshot);
        manager.updateAppWidget(ids, views);
    }

    public static void requestUpdate(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, HyperWeatherWidget.class));
        if (ids.length == 0) return;
        Intent intent = new Intent(context, HyperWeatherWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }

    private static RemoteViews build(Context context, WeatherSnapshot snapshot) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.hyper_weather_widget);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String city = prefs.getString(KEY_CITY, "");

        Intent setup = new Intent(context, HyperWeatherActivity.class);
        setup.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap = PendingIntent.getActivity(context, 12, setup,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.hyper_weather_root, tap);

        if (city == null || city.isEmpty()) {
            views.setTextViewText(R.id.hyper_weather_city, context.getString(R.string.hyper_weather_setup_title));
            views.setTextViewText(R.id.hyper_weather_temp, "--°");
            views.setTextViewText(R.id.hyper_weather_detail, context.getString(R.string.hyper_weather_setup_hint));
            views.setTextViewText(R.id.hyper_weather_updated, context.getString(R.string.hyper_weather_attribution));
            views.setImageViewResource(R.id.hyper_weather_icon, R.drawable.hyper_wx_partly);
            return views;
        }

        if (snapshot == null) {
            views.setTextViewText(R.id.hyper_weather_city, city);
            views.setTextViewText(R.id.hyper_weather_temp, "--°");
            views.setTextViewText(R.id.hyper_weather_detail, context.getString(R.string.hyper_weather_waiting));
            views.setTextViewText(R.id.hyper_weather_updated, context.getString(R.string.hyper_weather_attribution));
            views.setImageViewResource(R.id.hyper_weather_icon, R.drawable.hyper_wx_cloudy);
            return views;
        }

        views.setTextViewText(R.id.hyper_weather_city, snapshot.city);
        views.setTextViewText(R.id.hyper_weather_temp, Math.round(snapshot.temperature) + "°");
        views.setTextViewText(R.id.hyper_weather_detail,
                condition(context, snapshot.code) + "   "
                        + context.getString(R.string.hyper_weather_range,
                        Math.round(snapshot.high), Math.round(snapshot.low)));
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(snapshot.timestamp));
        views.setTextViewText(R.id.hyper_weather_updated,
                context.getString(snapshot.offline ? R.string.hyper_weather_offline : R.string.hyper_weather_updated, time));
        views.setImageViewResource(R.id.hyper_weather_icon, icon(snapshot.code));
        views.setContentDescription(R.id.hyper_weather_icon, condition(context, snapshot.code));
        return views;
    }

    private static WeatherSnapshot load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String city = prefs.getString(KEY_CITY, "");
        if (city == null || city.isEmpty()) return null;
        float lat = prefs.getFloat(KEY_LAT, Float.NaN);
        float lon = prefs.getFloat(KEY_LON, Float.NaN);
        if (Float.isNaN(lat) || Float.isNaN(lon)) return cached(context, prefs, true);
        boolean fahrenheit = prefs.getBoolean(KEY_FAHRENHEIT, false);

        try {
            String url = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                            + "&current=temperature_2m,weather_code"
                            + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                            + "&timezone=auto&forecast_days=1%s",
                    lat, lon, fahrenheit ? "&temperature_unit=fahrenheit" : "");
            JSONObject root = new JSONObject(read(url));
            JSONObject current = root.getJSONObject("current");
            JSONObject daily = root.getJSONObject("daily");
            double temperature = current.getDouble("temperature_2m");
            int code = current.getInt("weather_code");
            double high = daily.getJSONArray("temperature_2m_max").getDouble(0);
            double low = daily.getJSONArray("temperature_2m_min").getDouble(0);
            long now = System.currentTimeMillis();
            prefs.edit()
                    .putString(KEY_CACHE_TEMP, Double.toString(temperature))
                    .putInt(KEY_CACHE_CODE, code)
                    .putString(KEY_CACHE_HIGH, Double.toString(high))
                    .putString(KEY_CACHE_LOW, Double.toString(low))
                    .putLong(KEY_CACHE_TIME, now)
                    .apply();
            return new WeatherSnapshot(city, temperature, code, high, low, now, false);
        } catch (Exception ignored) {
            return cached(context, prefs, true);
        }
    }

    private static WeatherSnapshot cached(Context context, SharedPreferences prefs, boolean offline) {
        if (!prefs.contains(KEY_CACHE_TIME)) return null;
        String city = prefs.getString(KEY_CITY, "");
        if (city == null || city.isEmpty()) return null;
        try {
            return new WeatherSnapshot(
                    city,
                    Double.parseDouble(prefs.getString(KEY_CACHE_TEMP, "0")),
                    prefs.getInt(KEY_CACHE_CODE, 3),
                    Double.parseDouble(prefs.getString(KEY_CACHE_HIGH, "0")),
                    Double.parseDouble(prefs.getString(KEY_CACHE_LOW, "0")),
                    prefs.getLong(KEY_CACHE_TIME, System.currentTimeMillis()),
                    offline);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String read(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.setRequestProperty("User-Agent", "HyperLauncher/1.0");
            connection.setRequestProperty("Accept", "application/json");
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + status);
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

    private static String condition(Context context, int code) {
        if (code == 0) return context.getString(R.string.hyper_wx_clear);
        if (code == 1 || code == 2) return context.getString(R.string.hyper_wx_partly);
        if (code == 3 || code == 45 || code == 48) return context.getString(R.string.hyper_wx_cloudy);
        if (code >= 51 && code <= 67) return context.getString(R.string.hyper_wx_rain);
        if (code >= 71 && code <= 77) return context.getString(R.string.hyper_wx_snow);
        if (code >= 80 && code <= 82) return context.getString(R.string.hyper_wx_showers);
        if (code >= 85 && code <= 86) return context.getString(R.string.hyper_wx_snow);
        if (code >= 95) return context.getString(R.string.hyper_wx_storm);
        return context.getString(R.string.hyper_wx_cloudy);
    }

    private static int icon(int code) {
        if (code == 0) return R.drawable.hyper_wx_sunny;
        if (code == 1 || code == 2) return R.drawable.hyper_wx_partly;
        if (code >= 71 && code <= 77) return R.drawable.hyper_wx_snow;
        if (code >= 85 && code <= 86) return R.drawable.hyper_wx_snow;
        if (code >= 95) return R.drawable.hyper_wx_storm;
        if (code >= 51 && code <= 67) return R.drawable.hyper_wx_rain;
        if (code >= 80 && code <= 82) return R.drawable.hyper_wx_rain;
        return R.drawable.hyper_wx_cloudy;
    }

    private static final class WeatherSnapshot {
        final String city;
        final double temperature;
        final int code;
        final double high;
        final double low;
        final long timestamp;
        final boolean offline;

        WeatherSnapshot(String city, double temperature, int code, double high, double low,
                        long timestamp, boolean offline) {
            this.city = city;
            this.temperature = temperature;
            this.code = code;
            this.high = high;
            this.low = low;
            this.timestamp = timestamp;
            this.offline = offline;
        }
    }
}
