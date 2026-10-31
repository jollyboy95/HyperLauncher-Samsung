package au.steve.hyperlauncher.compat;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ScrollView;

/** Leaves horizontal card gestures with the child while retaining vertical list scrolling. */
public final class NotificationScrollView extends ScrollView {
    private float downX, downY;
    private final int slop;
    public NotificationScrollView(Context context) { super(context); slop = ViewConfiguration.get(context).getScaledTouchSlop(); }
    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) { downX = event.getX(); downY = event.getY(); }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float x = Math.abs(event.getX() - downX), y = Math.abs(event.getY() - downY);
            if (x > slop && x > y) return false;
        }
        return super.onInterceptTouchEvent(event);
    }
}
