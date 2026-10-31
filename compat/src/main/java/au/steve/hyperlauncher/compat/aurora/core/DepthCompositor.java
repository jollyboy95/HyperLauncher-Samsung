package au.steve.hyperlauncher.compat.aurora.core;

/** Applies a refined alpha mask onto ARGB pixels without Android types. */
public final class DepthCompositor {
    private DepthCompositor() {}

    public static int[] apply(int[] source, byte[] alpha, int width, int height) {
        if (source == null || alpha == null || width <= 0 || height <= 0
                || source.length != width * height || alpha.length != width * height) {
            throw new IllegalArgumentException("Invalid compositor buffers");
        }
        int[] out = new int[source.length];
        for (int i = 0; i < source.length; i++) {
            int a = alpha[i] & 0xFF;
            int src = source[i];
            int srcA = (src >>> 24) & 0xFF;
            int combined = (srcA * a + 127) / 255;
            out[i] = (combined << 24) | (src & 0x00FFFFFF);
        }
        return out;
    }

    public static byte[] upsampleNearest(byte[] src, int sw, int sh, int dw, int dh) {
        if (sw <= 0 || sh <= 0 || dw <= 0 || dh <= 0 || src == null || src.length != sw * sh) {
            throw new IllegalArgumentException("Invalid upsample");
        }
        byte[] out = new byte[dw * dh];
        for (int y = 0; y < dh; y++) {
            int sy = Math.min(sh - 1, y * sh / dh);
            for (int x = 0; x < dw; x++) {
                int sx = Math.min(sw - 1, x * sw / dw);
                out[y * dw + x] = src[sy * sw + sx];
            }
        }
        return out;
    }
}
