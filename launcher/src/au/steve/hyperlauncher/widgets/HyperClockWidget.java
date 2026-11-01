/* Original HyperLauncher widget. SPDX-License-Identifier: Apache-2.0 */
package au.steve.hyperlauncher.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.AlarmClock;
import android.view.View;
import android.widget.RemoteViews;

import au.steve.hyperlauncher.clock.HyperClockActivity;
import com.android.launcher3.R;

/** Native TextClock views keep time without minute alarms or a background service. */
public final class HyperClockWidget extends AppWidgetProvider {
    public static final String PREFS = "hyper_clock";
    public static final String KEY_LAYOUT = "layout";
    public static final String KEY_SHOW_DATE = "show_date";
    public static final String LAYOUT_STACKED = "stacked";
    public static final String LAYOUT_CENTERED = "centered";
    public static final String LAYOUT_MONO = "mono";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            manager.updateAppWidget(id, build(context));
        }
    }

    public static void requestUpdate(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, HyperClockWidget.class));
        if (ids.length == 0) return;
        Intent intent = new Intent(context, HyperClockWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }

    public static int layoutResource(String layout) {
        if (LAYOUT_CENTERED.equals(layout)) return R.layout.hyper_clock_widget_centered;
        if (LAYOUT_MONO.equals(layout)) return R.layout.hyper_clock_widget_mono;
        return R.layout.hyper_clock_widget;
    }

    public static RemoteViews build(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String layout = prefs.getString(KEY_LAYOUT, LAYOUT_STACKED);
        boolean showDate = prefs.getBoolean(KEY_SHOW_DATE, true);
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutResource(layout));

        Intent clock = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        PendingIntent tap = PendingIntent.getActivity(context, 10, clock,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.hyper_clock_root, tap);

        Intent style = new Intent(context, HyperClockActivity.class);
        style.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent styleTap = PendingIntent.getActivity(context, 13, style,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.hyper_clock_settings, styleTap);

        views.setViewVisibility(R.id.hyper_clock_date, showDate ? View.VISIBLE : View.GONE);
        return views;
    }
}
