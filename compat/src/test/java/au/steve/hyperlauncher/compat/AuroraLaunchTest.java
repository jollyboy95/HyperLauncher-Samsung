package au.steve.hyperlauncher.compat;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AuroraLaunchTest {
    @Test public void launcherStartsAndDraws() {
        try (var controller = Robolectric.buildActivity(AuroraActivity.class).setup().visible()) {
            controller.pause().stop().restart().start().resume();
            View root = controller.get().getWindow().getDecorView();
            root.measure(View.MeasureSpec.makeMeasureSpec(1440, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(3120, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, 1440, 3120);
            root.draw(new Canvas(Bitmap.createBitmap(1440, 3120, Bitmap.Config.ARGB_8888)));
        }
    }
}
