package au.steve.hyperlauncher.compat.aurora;

import au.steve.hyperlauncher.compat.aurora.core.ClockLayout;
import au.steve.hyperlauncher.compat.aurora.core.ColorMath;
import au.steve.hyperlauncher.compat.aurora.core.DepthCompositor;
import au.steve.hyperlauncher.compat.aurora.core.GlyphMetrics;
import au.steve.hyperlauncher.compat.aurora.core.LabelMask;
import au.steve.hyperlauncher.compat.aurora.core.MaskMath;
import au.steve.hyperlauncher.compat.aurora.core.WallpaperTransform;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuroraCoreTest {
    @Test public void fitsTallDigitsInsideCanvas() {
        float size = GlyphMetrics.fitSize(220, 300, 0.55f, 0.52f);
        assertTrue(size <= 220);
        assertTrue(size * 0.52f * 0.55f <= 300.1f);
        float top = GlyphMetrics.clampTop(900, 200, 844, 40, 180);
        assertTrue(top + 200 <= 844 - 180 + 0.01f);
    }

    @Test public void stackedClockStaysOnCanvas() {
        ClockLayout layout = ClockLayout.compute(0, 1440, 3120, 168, 0, 0.35f, 0.55f, 0.62f, 0.62f, 1.4f);
        assertTrue(layout.stacked);
        assertTrue(layout.fittedSize > 80);
        assertTrue(layout.firstBaseline < 3120);
        assertTrue(layout.horizontalScale < 0.8f);
    }

    @Test public void referenceStackLeavesMinutesVisibleAbovePortraitSubject() {
        ClockLayout layout = ClockLayout.compute(0, 592, 1280, 168, 0, .48f, .35f, .9f, .9f, 2f);
        assertTrue(layout.dateY >= 1280 * .05f);
        assertTrue(layout.firstBaseline + layout.lineHeight < 1280 * .52f);
        assertTrue(layout.verticalScale > 1.3f);
    }

    @Test public void wallpaperCoverKeepsFocus() {
        WallpaperTransform t = WallpaperTransform.compute(4000, 3000, 1440, 3120, 1.1f, 0.5f, 0.42f);
        assertTrue(t.scale > 0);
        assertEquals(0.42f, t.focusY, 0.2f);
    }

    @Test public void dogClassWinsAnimalWallpaper() {
        int h = 8, w = 8, c = 21;
        float[] logits = new float[h * w * c];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = (y * w + x) * c;
                logits[i] = 8f;
                if (x >= 2 && x <= 5 && y >= 2 && y <= 6) {
                    logits[i] = 0f;
                    logits[i + LabelMask.DOG] = 10f;
                }
            }
        }
        float[] conf = LabelMask.selectSubject(logits, h, w, c);
        int hits = 0;
        for (float v : conf) if (v > 0.5f) hits++;
        assertTrue("dog region should be selected", hits >= 8);
        assertEquals(LabelMask.DOG, LabelMask.chooseClass(new int[]{40,0,0,0,0,0,0,0,0,0,0,0,20,0,0,0,0,0,0,0,0}, 64));
    }

    @Test public void uncertainSubjectEdgesRetainSoftConfidence() {
        float[] logits = new float[8 * 8 * 21];
        java.util.Arrays.fill(logits, -15f);
        for (int i = 0; i < 64; i++) logits[i * 21 + LabelMask.DOG] = 5f;
        logits[0] = 5.1f; // Background narrowly wins at one dark boundary pixel.
        float edge = LabelMask.selectSubject(logits, 8, 8, 21, true)[0];
        assertTrue(edge > .45f && edge < .5f);
    }

    @Test public void bundledOutputUsesLastDimensionForClasses() {
        assertArrayEquals(new int[]{257, 257, 21}, LabelMask.hwcDimensions(new int[]{1, 257, 257, 21}));
        assertThrows(IllegalArgumentException.class,
                () -> LabelMask.hwcDimensions(new int[]{1, 21, 257, 257}));
    }

    @Test public void negativeLogitsStillProduceSubjectConfidence() {
        float[] logits = new float[8 * 8 * 21];
        java.util.Arrays.fill(logits, -15f);
        for (int i = 0; i < 64; i++) logits[i * 21 + LabelMask.DOG] = -2f;
        float[] confidence = LabelMask.selectSubject(logits, 8, 8, 21);
        assertTrue(confidence[0] > 0.99f);
    }

    @Test public void vehiclesAndPlantsAreEligibleDepthSubjects() {
        int[] counts = new int[21]; counts[0] = 500; counts[7] = 100;
        assertEquals(7, LabelMask.chooseClass(counts, 600));
        counts[7] = 0; counts[16] = 100;
        assertEquals(16, LabelMask.chooseClass(counts, 600));
    }

    @Test public void compositorKeepsColourAndAppliesAlpha() {
        int[] src = {0xFFFF0000, 0xFF00FF00};
        byte[] alpha = {(byte) 255, (byte) 0};
        int[] out = DepthCompositor.apply(src, alpha, 2, 1);
        assertEquals(0xFFFF0000, out[0]);
        assertEquals(0x0000FF00, out[1]);
    }

    @Test public void maskMathSoftensEdges() {
        float[] conf = new float[9];
        conf[4] = 1f;
        byte[] out = MaskMath.refine(conf, 3, 3);
        assertEquals(255, out[4] & 0xFF);
        assertTrue((out[1] & 0xFF) < 255);
    }

    @Test public void colourMathReturnsReadableClockInk() {
        int[] dark = new int[16];
        for (int i = 0; i < dark.length; i++) dark[i] = 0xFF101820;
        int lightClock = ColorMath.fromSamples(dark);
        assertTrue(((lightClock >> 16) & 255) + ((lightClock >> 8) & 255) + (lightClock & 255) > 300);
    }
}
