/* Original HyperLauncher widget. SPDX-License-Identifier: Apache-2.0 */
package au.steve.hyperlauncher.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.provider.Settings;
import android.widget.RemoteViews;
import com.android.launcher3.R;

/** Battery snapshot updated by the OS's widget schedule and power connection events. */
public final class HyperBatteryWidget extends AppWidgetProvider {
    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (Intent.ACTION_POWER_CONNECTED.equals(action) || Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            onUpdate(context, manager, manager.getAppWidgetIds(new ComponentName(context, HyperBatteryWidget.class)));
        }
    }
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = battery == null ? -1 : battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery == null ? -1 : battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int percent = level >= 0 && scale > 0 ? Math.max(0, Math.min(100, Math.round(100f * level / scale))) : -1;
        int status = battery == null ? -1 : battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        PendingIntent tap = PendingIntent.getActivity(context, 11, new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        for (int id : ids) {
            RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.hyper_battery_widget);
            view.setTextViewText(R.id.hyper_battery_percent, percent < 0 ? "—" : context.getString(R.string.hyper_battery_percent_format, percent));
            view.setTextViewText(R.id.hyper_battery_status, context.getString(charging ? R.string.hyper_charging : R.string.hyper_battery_snapshot));
            view.setProgressBar(R.id.hyper_battery_bar, 100, Math.max(percent, 0), false);
            view.setOnClickPendingIntent(R.id.hyper_battery_root, tap);
            manager.updateAppWidget(id, view);
        }
    }
}
