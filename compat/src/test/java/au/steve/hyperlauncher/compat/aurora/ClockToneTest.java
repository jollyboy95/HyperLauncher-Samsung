package au.steve.hyperlauncher.compat.aurora;

import au.steve.hyperlauncher.compat.aurora.core.ClockTone;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClockToneTest {
    private static int[] fill(int colour) {
        int[] samples = new int[48];
        java.util.Arrays.fill(samples, colour);
        return samples;
    }

    @Test public void picksDarkInkOnBrightBackdrop() {
        ClockTone.Selection picked = ClockTone.select(fill(0xFFE8E9E4));
        assertTrue(((picked.ink >>> 16) & 255) + ((picked.ink >>> 8) & 255) + (picked.ink & 255) < 255);
        assertTrue("dark ink must clear the contrast floor",
                ClockTone.contrastRatio(picked.ink, 0xFFE8E9E4) >= 3.4f);
    }

    @Test public void picksPastelInkOnDarkBackdrop() {
        ClockTone.Selection picked = ClockTone.select(fill(0xFF101820));
        assertTrue(((picked.ink >>> 16) & 255) + ((picked.ink >>> 8) & 255) + (picked.ink & 255) > 540);
        assertTrue(ClockTone.contrastRatio(picked.ink, 0xFF101820) >= 3.4f);
        assertFalse("dark backdrops need no shadow relief", picked.shadow);
    }

    @Test public void midBackdropsClearTheFloor() {
        ClockTone.Selection picked = ClockTone.select(fill(0xFF6E7478));
        assertTrue(ClockTone.contrastRatio(picked.ink, 0xFF6E7478) >= 3.4f);
    }

    @Test public void luminousBackdropsTakeInkPlusShadowRelief() {
        ClockTone.Selection picked = ClockTone.select(fill(0xFFB0B0AA));
        assertTrue(ClockTone.contrastRatio(picked.ink, 0xFFB0B0AA) >= 3.4f);
        assertTrue(picked.shadow);
    }

    @Test public void brightBackdropStillClearsTheFloor() {
        ClockTone.Selection picked = ClockTone.select(fill(0xFFF2F1EC));
        assertTrue(ClockTone.contrastRatio(picked.ink, 0xFFF2F1EC) >= 3.4f);
    }

    @Test public void keepsWallpaperHue() {
        ClockTone.Selection picked = ClockTone.select(fill(0xFF1D4ED8));
        int blue = picked.ink & 255;
        int red = (picked.ink >>> 16) & 255;
        assertTrue("blue wallpaper should tint the ink bluish", blue > red);
        assertTrue(ClockTone.contrastRatio(picked.ink, 0xFF1D4ED8) >= 3.4f);
    }

    @Test public void neutralizesOnGrayscaleWallpapers() {
        ClockTone.Selection picked = ClockTone.select(fill(0xFF343834));
        int r = (picked.ink >>> 16) & 255, g = (picked.ink >>> 8) & 255, b = picked.ink & 255;
        assertTrue(Math.abs(r - g) <= 12 && Math.abs(g - b) <= 12);
    }

    @Test public void medianAbsorbsSparseHighlights() {
        int[] samples = fill(0xFF12181F);
        samples[0] = samples[7] = 0xFFFFFFFF;
        ClockTone.Selection picked = ClockTone.select(samples);
        assertTrue(((picked.ink >>> 16) & 255) + ((picked.ink >>> 8) & 255) + (picked.ink & 255) > 540);
    }

    @Test public void bimodalRegionTrustsTheDominantPatch() {
        int[] samples = fill(0xFF111318);
        for (int i = 0; i < samples.length / 3; i++) samples[i] = 0xFFEDECFF;
        ClockTone.Selection picked = ClockTone.select(samples);
        float ratio = ClockTone.contrastRatio(picked.ink, 0xFF111318);
        assertTrue(ratio >= 3.4f || picked.shadow);
    }

    @Test public void emptySamplesFallBackToTheReferenceInk() {
        assertEquals(0xFFE9ECF2, ClockTone.select(null).ink);
        assertEquals(0xFFE9ECF2, ClockTone.select(new int[0]).ink);
    }

    @Test public void contrastMathMatchesKnownValues() {
        assertEquals(21f, ClockTone.contrastRatio(0xFFFFFFFF, 0xFF000000), .5f);
        assertEquals(1f, ClockTone.contrastRatio(0xFF89A0B4, 0xFF89A0B4), .001f);
        assertTrue(ClockTone.contrastRatio(0xFFFFFFCC, 0xFF101820) > 3.4f);
    }
}
