package au.steve.hyperlauncher.compat.aurora.core;

/** Fits tall, narrow lock-screen glyphs without clipping. */
public final class GlyphMetrics {
    private GlyphMetrics() {}

    public static float fitSize(float requested, float maxWidth, float measuredAtUnitSize, float horizontalScale) {
        float unit = Math.max(0.01f, measuredAtUnitSize * Math.max(0.2f, horizontalScale));
        return Math.max(8f, Math.min(requested, maxWidth / unit));
    }

    public static float lineGap(float fittedSize, float spacing) {
        return fittedSize * (0.78f + Math.max(0f, Math.min(1.4f, spacing)) * 0.22f);
    }

    public static float clampTop(float desiredTop, float groupHeight, float canvasHeight, float topInset, float bottomInset) {
        float maxTop = Math.max(topInset, canvasHeight - bottomInset - groupHeight);
        return Math.max(topInset, Math.min(maxTop, desiredTop));
    }

    public static float stackedHeight(float fittedSize, float spacing, boolean stacked) {
        if (!stacked) return fittedSize * 0.92f;
        return fittedSize * 0.86f + lineGap(fittedSize, spacing);
    }
}
