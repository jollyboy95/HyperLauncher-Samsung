package au.steve.hyperlauncher.compat.aurora;

import au.steve.hyperlauncher.compat.aurora.core.SubjectRegion;
import au.steve.hyperlauncher.compat.aurora.core.MaskMath;
import org.junit.Test;
import static org.junit.Assert.*;

public class SubjectRegionTest {
    @Test public void smallSubjectGetsFocusedCropWithContext() {
        float[] confidence = new float[100 * 100];
        for (int y = 40; y < 60; y++) for (int x = 30; x < 50; x++) confidence[y * 100 + x] = 1;
        SubjectRegion region = SubjectRegion.locate(confidence, 100, 100, 1000, 2000);
        assertNotNull(region);
        assertEquals(270, region.left);
        assertEquals(740, region.top);
        assertEquals(530, region.right);
        assertEquals(1260, region.bottom);
        float[] projected = region.project(new float[]{1, 1, 1, 1}, 2, 2, 1000, 2000);
        assertEquals(1f, projected[1000 * 1000 + 400], 0f);
        assertEquals(0f, projected[1000 * 1000 + 100], 0f);
        assertEquals(0f, projected[100 * 1000 + 400], 0f);
    }

    @Test public void upperDetailAddsContextAboveEarAndRetainsUpperSubject() {
        float[] confidence = new float[100 * 100];
        for (int y = 40; y < 80; y++) for (int x = 30; x < 50; x++) confidence[y * 100 + x] = 1;
        SubjectRegion body = SubjectRegion.locate(confidence, 100, 100, 1000, 2000);
        SubjectRegion upper = body.upperDetail(1000, 2000);
        assertTrue(upper.top < body.top);
        assertTrue(upper.left < body.left);
        assertTrue(upper.right > body.right);
        assertTrue(upper.bottom < body.bottom);
        float[] head = upper.project(new float[]{1, 1, 1, 1}, 2, 2, 1000, 2000);
        assertEquals(1f, head[(body.top - 1) * 1000 + 400], 0f);
        assertEquals(0f, head[(body.bottom - 1) * 1000 + 400], 0f);
    }

    @Test public void skipsAbsentOrAlreadyFrameFillingSubjects() {
        float[] confidence = new float[100];
        assertNull(SubjectRegion.locate(confidence, 10, 10, 600, 1200));
        java.util.Arrays.fill(confidence, 1f);
        assertNull(SubjectRegion.locate(confidence, 10, 10, 600, 1200));
    }

    @Test public void sensitivityChangesEdgeCoverageWithoutPunchingHolesInSolidSubject() {
        float[] confidence = new float[]{0, .45f, .99f};
        byte[] normal = MaskMath.refine(confidence, 3, 1, .3f);
        byte[] strict = MaskMath.refine(confidence, 3, 1, .6f);
        assertTrue((normal[1] & 255) > (strict[1] & 255));
        assertEquals(255, strict[2] & 255);
    }
}
