package au.steve.hyperlauncher.compat;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ScrollView;

/** Leaves horizontal card gestures with the child while retaining vertical list scrolling. */
public final class NotificationScrollView extends ScrollView {
    private float downX, downY;
    private final int slop;
    private Runnable expandStack;
    private boolean expandingStack;
    public void setExpandStack(Runnable action) { expandStack = action; }
    public NotificationScrollView(Context context) { super(context); slop = ViewConfiguration.get(context).getScaledTouchSlop(); }
    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getX(); downY = event.getY(); expandingStack = false;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float x = Math.abs(event.getX() - downX), y = Math.abs(event.getY() - downY);
            if (x > slop && x > y) return false;
            if (expandStack != null && y > slop && y > x && event.getY() < downY) {
                expandingStack = true;
                return true;
            }
        }
        return super.onInterceptTouchEvent(event);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!expandingStack) return super.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            expandingStack = false;
            if (downY - event.getY() >= 48 * getResources().getDisplayMetrics().density
                    && expandStack != null) expandStack.run();
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            expandingStack = false;
        }
        return true;
    }
}
