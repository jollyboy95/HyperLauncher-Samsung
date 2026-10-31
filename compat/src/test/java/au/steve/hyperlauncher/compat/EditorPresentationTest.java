package au.steve.hyperlauncher.compat;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import au.steve.hyperlauncher.compat.aurora.AuroraConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="w392dp-h850dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class EditorPresentationTest {
    private View find(View root,String text) {
        if(root instanceof TextView && text.equals(((TextView)root).getText().toString())) return root;
        if(root instanceof ViewGroup) for(int i=0;i<((ViewGroup)root).getChildCount();i++) {
            View result=find(((ViewGroup)root).getChildAt(i),text);if(result!=null)return result;
        }
        return null;
    }
    private View description(View root,String text) {
        if(text.contentEquals(root.getContentDescription()==null?"":root.getContentDescription())) return root;
        if(root instanceof ViewGroup) for(int i=0;i<((ViewGroup)root).getChildCount();i++) {
            View result=description(((ViewGroup)root).getChildAt(i),text);if(result!=null)return result;
        }
        return null;
    }
    private void layout(View root,int width,int height) {
        for(int pass=0;pass<3;pass++) {
            root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
            root.layout(0,0,width,height);
            org.robolectric.shadows.ShadowLooper.idleMainLooper();
        }
    }
    private void capture(View root,String name) throws Exception {
        String directory=System.getenv("AURORA_QA_DIR");if(directory==null)return;
        Bitmap bitmap=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        try(var out=new java.io.FileOutputStream(new java.io.File(directory,name+".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
        }
        bitmap.recycle();
    }
    private void prepare() {
        AuroraConfig live=new AuroraConfig(RuntimeEnvironment.getApplication());
        live.prefs().edit().clear().putBoolean("referenceStyleV1",true).putInt("clockPreset",0)
                .putInt("clockY",113).putInt("clockSize",170).putBoolean("motionEnabled",false)
                .putBoolean("depthEnabled",false).commit();
        String wallpaper=System.getenv("AURORA_QA_WALLPAPER");
        if(wallpaper!=null)live.prefs().edit().putString("wallpaper",wallpaper).commit();
    }
    @Test public void galleryCustomiseAndApplyPreserveExistingClockChoices() throws Exception {
        prepare();
        try(var controller=Robolectric.buildActivity(AuroraSettingsActivity.class).setup().visible()) {
            var activity=controller.get();View root=activity.getWindow().getDecorView();layout(root,392,850);
            capture(root,"v27-gallery-light");
            assertNotNull(find(root,"Choose your style"));
            description(root,"Customize Depth Stack").performClick();layout(root,392,850);
            capture(root,"v27-editor-light");
            find(root,"Features").performClick();find(root,"Notification display").performClick();layout(root,392,850);
            capture(root,"v27-notifications-light");
            assertNotNull(description(root,"Stack notification display"));
            description(root,"Stack notification display").performClick();
            assertTrue(description(root,"Stack notification display").isSelected());
            description(root,"Expand editor controls").performClick();layout(root,392,850);
            assertNotNull(description(root,"Collapse editor controls"));
            capture(root,"v27-notifications-expanded-light");
            description(root,"Apply lock-screen changes").performClick();
            AuroraConfig live=new AuroraConfig(activity);assertEquals(113,live.clockY());assertEquals(170,live.clockSize());
            assertEquals(1,live.notificationMode());
        }
    }
    @Test @Config(qualifiers="w392dp-h850dp-night-mdpi")
    public void darkEditorAndCompactWindowKeepControlsAccessible() throws Exception {
        prepare();
        try(var controller=Robolectric.buildActivity(AuroraSettingsActivity.class).setup().visible()) {
            View root=controller.get().getWindow().getDecorView();layout(root,392,850);
            capture(root,"v27-gallery-dark");description(root,"Customize Depth Stack").performClick();layout(root,392,850);
            capture(root,"v27-editor-dark");find(root,"Notifications").performClick();layout(root,392,850);
            capture(root,"v27-notifications-dark");
        }
    }
    @Test @Config(qualifiers="w760dp-h360dp-land-night-mdpi")
    public void compactWindowKeepsControlsBelowToolbar() throws Exception {
        prepare();
        try(var controller=Robolectric.buildActivity(AuroraSettingsActivity.class).setup().visible()) {
            View root=controller.get().getWindow().getDecorView();layout(root,760,360);
            description(root,"Customize Depth Stack").performClick();find(root,"Notifications").performClick();
            layout(root,760,360);capture(root,"v27-compact-dark");
            assertEquals(760,root.getWidth());assertEquals(360,root.getHeight());
            View apply=description(root,"Apply lock-screen changes");
            assertTrue(apply.isShown());
            int[] button=new int[2],controls=new int[2];
            apply.getLocationOnScreen(button);description(root,"Stack notification display").getLocationOnScreen(controls);
            assertTrue("Controls remain below the toolbar",controls[1]>=button[1]+apply.getHeight());
        }
    }
    @Test public void applyingFromGalleryUsesTheBrowsedStyle() {
        prepare();
        try(var controller=Robolectric.buildActivity(AuroraSettingsActivity.class).setup().visible()) {
            View root=controller.get().getWindow().getDecorView();layout(root,392,850);
            android.widget.HorizontalScrollView gallery=(android.widget.HorizontalScrollView)description(root,"Lock-screen style gallery");
            ViewGroup cards=(ViewGroup)gallery.getChildAt(0);
            gallery.scrollTo(cards.getChildAt(1).getLeft(),0);
            description(root,"Apply lock-screen changes").performClick();
            assertEquals(1,new AuroraConfig(controller.get()).clockPreset());
        }
    }
    @Test @Config(sdk=28)
    public void cancelDiscardsNotificationStyleOnOldestSupportedAndroid() {
        prepare();AuroraConfig live=new AuroraConfig(RuntimeEnvironment.getApplication());
        int before=live.notificationMode();
        try(var controller=Robolectric.buildActivity(AuroraSettingsActivity.class).setup().visible()) {
            View root=controller.get().getWindow().getDecorView();layout(root,392,850);
            description(root,"Customize Depth Stack").performClick();find(root,"Notifications").performClick();
            description(root,"Count notification display").performClick();
            description(root,"Cancel lock-screen changes").performClick();
            assertEquals(before,live.notificationMode());
        }
    }
}
