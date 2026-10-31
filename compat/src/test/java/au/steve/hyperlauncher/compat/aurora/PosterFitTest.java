package au.steve.hyperlauncher.compat.aurora;
import au.steve.hyperlauncher.compat.aurora.core.PosterFit;
import org.junit.Test;
import static org.junit.Assert.*;
public class PosterFitTest {
    @Test public void largestNeonClockKeepsDateAndMinutesInsideAvailableSpace() {
        for(float[] size:new float[][]{{1080,2340},{392,850},{360,640}}) {
            float w=size[0],h=size[1],bottom=h*.7f;
            for(float offset:new float[]{-h,h}) {
                PosterFit f=PosterFit.neon(w,h,220,1.6f,w*.4f,offset,bottom);
                assertTrue(f.y+h*.075f*f.scaleY>=h*.06f-.1f);
                assertTrue(f.y+h*.79f*f.scaleY<=bottom-h*.035f+.1f);
                assertTrue(f.x+w*.1f*f.scaleX>=w*.04f-.1f);
                assertTrue(f.x+w*.92f*f.scaleX<=w*.96f+.1f);
            }
        }
    }
}
