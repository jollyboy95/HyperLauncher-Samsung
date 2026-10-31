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
        return compute(preset, canvasW, canvasH, clockSize, clockY, widthWeight, spacing,
                hoursUnitWidth, minutesUnitWidth, pairUnitWidth, 1f, 0f);
    }

    public static ClockLayout compute(int preset, float canvasW, float canvasH, float clockSize,
                                      float clockY, float widthWeight, float spacing,
                                      float hoursUnitWidth, float minutesUnitWidth, float pairUnitWidth,
                                      float heightScale, float horizontalOffset) {
        boolean stacked = preset == 0 || preset == 2 || preset == 4;
        boolean showColon = preset == 1;
        boolean editorialFour = preset == 3;
        float hScale = stacked ? (0.56f + widthWeight * 0.24f)
                : editorialFour ? (0.48f + widthWeight * 0.16f) : (0.78f + widthWeight * 0.16f);
        float vScale = (stacked ? 1.40f : editorialFour ? 1.58f : 1.14f)
                * Math.max(.6f, Math.min(1.6f, heightScale));
        float requested = canvasH * (stacked ? 0.18f : editorialFour ? 0.24f : 0.14f) * clockSize / 168f;
        requested = Math.max(canvasH * 0.07f, Math.min(canvasH * 0.31f, requested));
        float maxWidth = canvasW * (preset == 2 ? 0.64f : editorialFour ? 0.88f : 0.72f);
        float unit = stacked ? Math.max(hoursUnitWidth, minutesUnitWidth) : pairUnitWidth;
        float fitted = GlyphMetrics.fitSize(requested, maxWidth, unit, hScale);
        float digitHeight = fitted * 0.72f * vScale;
        float gap = stacked ? digitHeight * (1.01f + spacing * 0.15f) : 0;
        float group = digitHeight + gap;
        float top = GlyphMetrics.clampTop(canvasH * (editorialFour ? 0.43f : 0.075f) + clockY,
                group + canvasH * 0.045f, canvasH, canvasH * 0.045f, canvasH * 0.20f);
        float dateY = top;
        float first = dateY + canvasH * 0.030f + digitHeight;
        float x = (preset == 2 ? canvasW * 0.34f : canvasW * 0.5f) + canvasW * horizontalOffset;
        return new ClockLayout(x, dateY, x, first, fitted, gap, hScale, vScale, stacked, showColon, editorialFour);
    }
}
