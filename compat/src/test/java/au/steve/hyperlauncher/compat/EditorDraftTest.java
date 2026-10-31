package au.steve.hyperlauncher.compat;
import au.steve.hyperlauncher.compat.aurora.*;
import au.steve.hyperlauncher.compat.aurora.core.GlassBlur;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class EditorDraftTest {
    @Test public void editsDoNotTouchLiveSettingsUntilApplyAndRestoreSurvivesRecreation() {
        var context=RuntimeEnvironment.getApplication();var live=new AuroraConfig(context);
        live.prefs().edit().clear().putBoolean("referenceStyleV1",true).putInt("clockPreset",2)
                .putInt("clockY",123).putString("autoWakeStatus","Connected").commit();
        AuroraConfig draft=EditorDraft.open(context,false);draft.applyPreset(9);
        assertEquals(2,live.clockPreset());assertEquals(123,live.clockY());
        assertEquals(9,EditorDraft.open(context,true).clockPreset());
        assertEquals(2,EditorDraft.open(context,false).clockPreset());
        draft=EditorDraft.open(context,false);draft.applyPreset(12);
        live.prefs().edit().putString("autoWakeStatus","Ready").commit();EditorDraft.apply(context,draft);
        assertEquals(12,live.clockPreset());assertEquals("Ready",live.prefs().getString("autoWakeStatus",""));
    }
    @Test public void blurSoftensEdgesAndPreservesUniformColourWithoutMutatingSource() {
        int[] pixels=new int[9];java.util.Arrays.fill(pixels,0xFF000000);pixels[4]=0xFFFFFFFF;
        int[] blur=GlassBlur.blur(pixels,3,3,1);
        assertEquals(0xFFFFFFFF,pixels[4]);assertNotEquals(0xFFFFFFFF,blur[4]);
        assertTrue((blur[3]&255)>0);assertEquals(255,blur[4]>>>24);
        java.util.Arrays.fill(pixels,0xFF123456);assertArrayEquals(pixels,GlassBlur.blur(pixels,3,3,2));
        assertThrows(IllegalArgumentException.class,()->GlassBlur.blur(pixels,0,3,1));
    }
}
