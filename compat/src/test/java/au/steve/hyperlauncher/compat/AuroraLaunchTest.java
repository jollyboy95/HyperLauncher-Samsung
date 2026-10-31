package au.steve.hyperlauncher.compat;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import au.steve.hyperlauncher.compat.aurora.AuroraConfig;
import org.robolectric.RuntimeEnvironment;
import static org.junit.Assert.*;
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
    @Test public void staleMaskIsIgnoredAndDepthToggleHidesForeground() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        AuroraConfig config = new AuroraConfig(context);
        config.prefs().edit().clear().commit();
        Bitmap solid = Bitmap.createBitmap(200, 400, Bitmap.Config.ARGB_8888);
        solid.eraseColor(Color.RED);
        File oldMask = new File(context.getFilesDir(), "depth-old.png");
        File currentMask = new File(context.getFilesDir(), "depth-v3-current.png");
        for (File file : new File[]{oldMask, currentMask}) {
            try (FileOutputStream out = new FileOutputStream(file)) {
                solid.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
        }
        AuroraSceneView scene = new AuroraSceneView(context, true);
        scene.layout(0, 0, 200, 400);
        Bitmap rendered = Bitmap.createBitmap(200, 400, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(rendered);
        scene.draw(canvas);
        int baseline = rendered.getPixel(20, 350);
        config.prefs().edit().putString("autoForeground", oldMask.getAbsolutePath()).commit();
        scene.reload();
        scene.draw(canvas);
        assertEquals(baseline, rendered.getPixel(20, 350));
        config.prefs().edit().putString("autoForeground", currentMask.getAbsolutePath()).commit();
        scene.reload();
        scene.draw(canvas);
        assertEquals(Color.RED, rendered.getPixel(20, 350));
        config.prefs().edit().putBoolean("depthEnabled", false).commit();
        scene.reload();
        scene.draw(canvas);
        assertEquals(baseline, rendered.getPixel(20, 350));
        config.prefs().edit().clear().commit();
    }

}
