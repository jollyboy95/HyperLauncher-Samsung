package au.steve.hyperlauncher.compat;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/** Raw coordinates stay stable as the card follows the finger. */
public final class NotificationSwipeListener implements View.OnTouchListener {
    private final Runnable dismiss;
    private final int slop;
    private final float threshold;
    private float downX, downY;
    private boolean horizontal;
    public NotificationSwipeListener(Context context, Runnable dismiss) {
        this.dismiss = dismiss;
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
        threshold = 48 * context.getResources().getDisplayMetrics().density;
    }
    @Override public boolean onTouch(View view, MotionEvent event) {
        float x = event.getRawX() - downX, y = event.getRawY() - downY;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getRawX(); downY = event.getRawY(); horizontal = false; return true;
            case MotionEvent.ACTION_MOVE:
                if (!horizontal && Math.abs(x) > slop && Math.abs(x) > Math.abs(y)) horizontal = true;
                if (horizontal) {
                    if (view.getParent() != null) view.getParent().requestDisallowInterceptTouchEvent(true);
                    view.setTranslationX(x); view.setAlpha(Math.max(.4f, 1 - Math.abs(x) / Math.max(1, view.getWidth())));
                }
                return true;
            case MotionEvent.ACTION_UP:
                boolean clear = (horizontal || Math.abs(x) > Math.abs(y)) && Math.abs(x) >= threshold;
                reset(view);
                if (clear) dismiss.run();
                else if (Math.abs(x) < slop && Math.abs(y) < slop) view.performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                reset(view); return true;
            default: return true;
        }
    }
    private void reset(View view) {
        view.setTranslationX(0); view.setAlpha(1); horizontal = false;
        if (view.getParent() != null) view.getParent().requestDisallowInterceptTouchEvent(false);
    }
}
