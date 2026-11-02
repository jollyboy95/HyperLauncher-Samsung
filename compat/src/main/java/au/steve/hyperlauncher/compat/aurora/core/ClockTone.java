package au.steve.hyperlauncher.compat.aurora.core;

/** Wallpaper-aware, contrast-guaranteed clock ink for the sampled clock region. */
public final class ClockTone {
    /** Chosen ink plus whether the scene view should add a soft legibility shadow. */
    public static final class Selection {
        public final int ink;
        public final boolean shadow;
        Selection(int ink, boolean shadow) { this.ink = ink; this.shadow = shadow; }
    }

    private ClockTone() {}

    static float targetContrast() { return 3.4f; }

    /** Picks ink hue-tinted from the composited backdrop samples with a guaranteed contrast floor. */
    public static Selection select(int[] samples) {
        if (samples == null || samples.length == 0)
            return new Selection(0xFFE9ECF2, true);
        float[] luminance = new float[samples.length];
        float[] bins = new float[36];
        float weight = 0;
        for (int i = 0; i < samples.length; i++) {
            int p = samples[i] & 0x00FFFFFF;
            luminance[i] = relativeLuminance(p);
            float[] hsl = toHsl((p >>> 16) & 255, (p >>> 8) & 255, p & 255);
            float w = hsl[1] * (1 - Math.abs(hsl[2] - .5f) * 1.6f);
            if (w > 0) { bins[Math.min(35, (int) (hsl[0] / 10f))] += w; weight += w; }
        }
        java.util.Arrays.sort(luminance);
        float backdrop = luminance[luminance.length / 2];
        int best = 0; float high = -1;
        for (int i = 0; i < 36; i++) {
            float v = bins[i] + .5f * (bins[(i + 35) % 36] + bins[(i + 1) % 36]);
            if (v > high) { high = v; best = i; }
        }
        boolean tinted = weight / samples.length >= .04f;
        float hue = tinted ? best * 10f + 5f : 210f;
        Selection light = tune(hue, tinted ? .26f : 0f, true, backdrop);
        Selection dark = tune(hue, tinted ? .38f : 0f, false, backdrop);
        float cLight = contrastRatio(relativeLuminance(light.ink), backdrop);
        float cDark = contrastRatio(relativeLuminance(dark.ink), backdrop);
        boolean preferLight = (cLight >= targetContrast() && cLight * .9f >= cDark)
                || (cLight >= cDark && cLight < targetContrast());
        Selection picked = preferLight ? light : dark;
        boolean shadow = picked.shadow || backdrop > .38f;
        return new Selection(picked.ink, shadow);
    }

    /** Pushes a light or dark candidate toward its extreme until the contrast target is met. */
    private static Selection tune(float hue, float saturation, boolean light, float backdrop) {
        float level = light ? .86f : .14f;
        int ink = ink(hue, saturation, level);
        float best = contrastRatio(relativeLuminance(ink), backdrop);
        for (int step = 0; step < 14 && best < targetContrast(); step++) {
            level = light ? Math.min(1f, level + .05f) : Math.max(0f, level - .05f);
            int candidate = ink(hue, saturation, level);
            float score = contrastRatio(relativeLuminance(candidate), backdrop);
            if (score < best) break;
            best = score; ink = candidate;
        }
        if (best < targetContrast()) {
            int extreme = light ? 0xFFFFFFFF : 0xFF05070C;
            if (contrastRatio(relativeLuminance(extreme), backdrop) > best) ink = extreme;
        }
        float chosen = contrastRatio(relativeLuminance(ink), backdrop);
        return new Selection(ink | 0xFF000000, chosen < targetContrast());
    }

    public static float relativeLuminance(int rgb) {
        return .2126f * channel(((rgb >>> 16) & 255) / 255f)
                + .7152f * channel(((rgb >>> 8) & 255) / 255f)
                + .0722f * channel((rgb & 255) / 255f);
    }

    private static float channel(float value) {
        return value <= .04045f ? value / 12.92f : (float) Math.pow((value + .055f) / 1.055f, 2.4);
    }

    public static float contrastRatio(float lumA, float lumB) {
        float hi = Math.max(lumA, lumB) + .05f;
        float lo = Math.max(.0001f, Math.min(lumA, lumB) + .05f);
        return Math.max(1f, hi / lo);
    }

    public static float contrastRatio(int colourA, int colourB) {
        return contrastRatio(relativeLuminance(colourA), relativeLuminance(colourB));
    }

    private static float[] toHsl(int r, int g, int b) {
        float a = r / 255f, c = g / 255f, d = b / 255f;
        float max = Math.max(a, Math.max(c, d)), min = Math.min(a, Math.min(c, d));
        float delta = max - min;
        float h = 0, l = (max + min) / 2;
        float s = delta == 0 ? 0 : delta / (1 - Math.abs(2 * l - 1));
        if (delta > 0) {
            if (max == a) h = ((c - d) / delta) % 6;
            else if (max == c) h = (d - a) / delta + 2;
            else h = (a - c) / delta + 4;
            h = (h * 60 + 360) % 360;
        }
        return new float[]{h, s, l};
    }

    private static int ink(float h, float s, float l) {
        float c = (1 - Math.abs(2 * l - 1)) * s, x = c * (1 - Math.abs((h / 60) % 2 - 1)), m = l - c / 2;
        float r = 0, g = 0, b = 0;
        if (h < 60) { r = c; g = x; }
        else if (h < 120) { r = x; g = c; }
        else if (h < 180) { g = c; b = x; }
        else if (h < 240) { g = x; b = c; }
        else if (h < 300) { r = x; b = c; }
        else { r = c; b = x; }
        return 0xFF000000 | (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }
}
