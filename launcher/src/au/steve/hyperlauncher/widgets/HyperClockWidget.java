/* Original HyperLauncher widget. SPDX-License-Identifier: Apache-2.0 */
package au.steve.hyperlauncher.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;
import android.widget.RemoteViews;
import com.android.launcher3.R;

/** Native TextClock views keep time without minute alarms or a background service. */
public final class HyperClockWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        Intent clock = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        PendingIntent tap = PendingIntent.getActivity(context, 10, clock,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        for (int id : ids) {
            RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.hyper_clock_widget);
            view.setOnClickPendingIntent(R.id.hyper_clock_root, tap);
            manager.updateAppWidget(id, view);
        }
    }
}
