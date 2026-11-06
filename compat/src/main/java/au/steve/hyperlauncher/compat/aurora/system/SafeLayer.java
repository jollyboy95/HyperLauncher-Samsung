package au.steve.hyperlauncher.compat.aurora.system;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.widget.FrameLayout;

/**
 * Container for the Aurora renderer inside System UI. Any exception while measuring, laying out
 * or drawing hides the layer instead of propagating and crashing System UI.
 */
final class SafeLayer extends FrameLayout {
    private static final String TAG = "AuroraKeyguard";
    private final Runnable onFailure;
    private boolean failed;

    SafeLayer(Context context, Runnable onFailure) {
        super(context);
        this.onFailure = onFailure;
        setWillNotDraw(true);
    }

    boolean failed() { return failed; }

    private void fail(Throwable t) {
        if (failed) return;
        failed = true;
        Log.e(TAG, "Aurora layer failed and was hidden", t);
        post(() -> {
            setVisibility(GONE);
            if (onFailure != null) onFailure.run();
        });
    }

    @Override protected void onMeasure(int w, int h) {
        try {
            super.onMeasure(w, h);
        } catch (Throwable t) {
            setMeasuredDimension(0, 0);
            fail(t);
        }
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        try { super.onLayout(changed, l, t, r, b); } catch (Throwable e) { fail(e); }
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        if (failed) return;
        int save = canvas.save();
        try {
            super.dispatchDraw(canvas);
        } catch (Throwable t) {
            fail(t);
        } finally {
            canvas.restoreToCount(save);
        }
    }
}
