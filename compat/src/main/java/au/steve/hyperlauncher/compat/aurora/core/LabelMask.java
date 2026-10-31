package au.steve.hyperlauncher.compat.aurora.core;

/**
 * Turns DeepLab PASCAL VOC logits into a subject confidence map.
 * Class 12 is dog, so animal wallpapers (including pugs) are first-class.
 */
public final class LabelMask {
    public static final int BACKGROUND = 0;
    public static final int BIRD = 3;
    public static final int CAT = 8;
    public static final int COW = 10;
    public static final int DOG = 12;
    public static final int HORSE = 13;
    public static final int PERSON = 15;
    public static final int SHEEP = 17;

    private static final int[] PRIORITY = {DOG, CAT, HORSE, SHEEP, COW, BIRD, PERSON};

    private LabelMask() {}

    /** The bundled PASCAL VOC model returns [batch, height, width, classes]. */
    public static int[] hwcDimensions(int[] shape) {
        if (shape == null || shape.length != 4 || shape[0] != 1
                || shape[1] <= 0 || shape[2] <= 0 || shape[3] != 21) {
            throw new IllegalArgumentException("Expected DeepLab output [1, height, width, 21]");
        }
        return new int[]{shape[1], shape[2], shape[3]};
    }

    public static boolean isSubjectClass(int cls) {
        for (int p : PRIORITY) if (p == cls) return true;
        return false;
    }

    /**
     * @param logits HWC layout, length = height * width * classes
     * @return per-pixel confidence in 0..1 for the chosen foreground subject
     */
    public static float[] selectSubject(float[] logits, int height, int width, int classes) {
        return selectSubject(logits, height, width, classes, false);
    }

    public static float[] selectSubject(float[] logits, int height, int width, int classes, boolean softEdges) {
        if (height <= 0 || width <= 0 || classes <= 1 || logits == null
                || logits.length != (long) height * width * classes) {
            throw new IllegalArgumentException("Invalid logits");
        }
        int pixels = height * width;
        int[] argmax = new int[pixels];
        float[] peak = new float[pixels];
        int[] counts = new int[classes];
        for (int i = 0; i < pixels; i++) {
            int base = i * classes;
            int best = 0;
            float high = logits[base];
            for (int c = 1; c < classes; c++) {
                float v = logits[base + c];
                if (v > high) {
                    high = v;
                    best = c;
                }
            }
            argmax[i] = best;
            peak[i] = high;
            counts[best]++;
        }
        int chosen = chooseClass(counts, pixels);
        float[] confidence = new float[pixels];
        if (chosen <= 0) return confidence;
        for (int i = 0; i < pixels; i++) {
            if (!softEdges && argmax[i] != chosen) continue;
            // Keep soft evidence at boundaries, even if background narrowly wins.
            // Hard argmax gating erased dark ears before edge refinement could see them.
            double sum = 0;
            for (int c = 0; c < classes; c++) sum += Math.exp(logits[i * classes + c] - peak[i]);
            confidence[i] = (float) (Math.exp(logits[i * classes + chosen] - peak[i]) / sum);
        }

        return confidence;
    }

    public static int chooseClass(int[] counts, int pixels) {
        int minArea = Math.max(8, pixels / 250);
        for (int cls : PRIORITY) {
            if (cls < counts.length && counts[cls] >= minArea) return cls;
        }
        int best = 0;
        int high = minArea - 1;
        for (int c = 1; c < counts.length; c++) {
            // Cars, plants and other VOC objects can also sit in front of the clock.
            if (counts[c] > high) {
                high = counts[c];
                best = c;
            }
        }
        return best;
    }

    public static boolean isFurniture(int cls) {
        return cls == 1 || cls == 2 || cls == 4 || cls == 5 || cls == 6 || cls == 7
                || cls == 9 || cls == 11 || cls == 14 || cls == 16 || cls == 18
                || cls == 19 || cls == 20;
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
