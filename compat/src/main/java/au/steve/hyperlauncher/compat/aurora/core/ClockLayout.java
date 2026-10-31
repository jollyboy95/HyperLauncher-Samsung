package au.steve.hyperlauncher.compat.aurora.core;

/** Layout numbers for HyperOS-inspired clock presets on an arbitrary canvas. */
public final class ClockLayout {
    public final float dateX;
    public final float dateY;
    public final float clockX;
    public final float firstBaseline;
    public final float fittedSize;
    public final float lineHeight;
    public final float horizontalScale;
    public final float verticalScale;
    public final boolean stacked;
    public final boolean showColon;
    public final boolean editorialFour;

    public ClockLayout(float dateX, float dateY, float clockX, float firstBaseline, float fittedSize,
                       float lineHeight, float horizontalScale, float verticalScale,
                       boolean stacked, boolean showColon, boolean editorialFour) {
        this.dateX = dateX;
        this.dateY = dateY;
        this.clockX = clockX;
        this.firstBaseline = firstBaseline;
        this.fittedSize = fittedSize;
        this.lineHeight = lineHeight;
        this.horizontalScale = horizontalScale;
        this.verticalScale = verticalScale;
        this.stacked = stacked;
        this.showColon = showColon;
        this.editorialFour = editorialFour;
    }

    public static ClockLayout compute(int preset, float canvasW, float canvasH, float clockSize,
                                      float clockY, float widthWeight, float spacing,
                                      float hoursUnitWidth, float minutesUnitWidth, float pairUnitWidth) {
        boolean stacked = preset == 0 || preset == 2 || preset == 4;
        boolean showColon = preset == 1;
        boolean editorialFour = preset == 3;
        float hScale = stacked ? (0.52f + widthWeight * 0.18f)
                : editorialFour ? (0.58f + widthWeight * 0.16f)
                : (0.86f + widthWeight * 0.12f);
        float vScale = stacked ? 1.18f : editorialFour ? 1.24f : 1.04f;
        float requested = clockSize * (stacked ? 0.22f : editorialFour ? 0.18f : 0.14f) * (canvasH / 160f);
        requested = Math.max(canvasH * 0.08f, Math.min(canvasH * 0.34f, requested));
        float maxWidth = canvasW * (preset == 2 ? 0.62f : 0.78f);
        float unit = stacked ? Math.max(hoursUnitWidth, minutesUnitWidth)
                : showColon ? pairUnitWidth : pairUnitWidth;
        float fitted = GlyphMetrics.fitSize(requested, maxWidth, unit, hScale);
        float gap = GlyphMetrics.lineGap(fitted, spacing);
        float group = GlyphMetrics.stackedHeight(fitted, spacing, stacked || editorialFour && false);
        if (editorialFour) group = fitted * vScale * 0.92f;
        else if (!stacked) group = fitted * vScale * 0.92f;
        else group = fitted * 0.84f * vScale + gap;
        float top = GlyphMetrics.clampTop(canvasH * 0.11f + clockY, group + canvasH * 0.045f, canvasH,
                canvasH * 0.045f, canvasH * 0.22f);
        float dateY = top + canvasH * 0.018f;
        float first = dateY + canvasH * 0.028f + fitted * 0.72f * vScale;
        float x = preset == 2 ? canvasW * 0.34f : canvasW * 0.5f;
        return new ClockLayout(x, dateY, x, first, fitted, gap, hScale, vScale, stacked, showColon, editorialFour);
    }
}
