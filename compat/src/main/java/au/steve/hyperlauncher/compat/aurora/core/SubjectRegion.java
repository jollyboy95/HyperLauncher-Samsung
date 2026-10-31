package au.steve.hyperlauncher.compat.aurora.core;

/** Focuses a second model pass on coarse foreground rather than empty wallpaper. */
public final class SubjectRegion {
    public final int left, top, right, bottom;

    private SubjectRegion(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public int width() { return right - left; }
    public int height() { return bottom - top; }

    public static SubjectRegion locate(float[] confidence, int width, int height, int imageW, int imageH) {
        if (confidence == null || width <= 0 || height <= 0 || imageW <= 0 || imageH <= 0
                || confidence.length != width * height) throw new IllegalArgumentException("Invalid subject map");
        int left = width, top = height, right = -1, bottom = -1, count = 0;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if (confidence[y * width + x] < 0.2f) continue;
            left = Math.min(left, x);
            right = Math.max(right, x);
            top = Math.min(top, y);
            bottom = Math.max(bottom, y);
            count++;
        }
        if (count < 8) return null;
        int padX = Math.max(2, Math.round((right - left + 1) * 0.15f));
        int padY = Math.max(2, Math.round((bottom - top + 1) * 0.15f));
        int l = Math.max(0, (left - padX) * imageW / width);
        int t = Math.max(0, (top - padY) * imageH / height);
        int r = Math.min(imageW, (int) Math.ceil((right + 1 + padX) * (double) imageW / width));
        int b = Math.min(imageH, (int) Math.ceil((bottom + 1 + padY) * (double) imageH / height));
        if ((long) (r - l) * (b - t) >= (long) imageW * imageH * 0.85) return null;
        return new SubjectRegion(l, t, r, b);
    }

    /** Extra upper-subject detail keeps small ears and hair visible in tall subjects. */
    public SubjectRegion upperDetail(int imageW, int imageH) {
        int extra = Math.max(2, width() / 12);
        return new SubjectRegion(Math.max(0, left - extra), Math.max(0, top - extra),
                Math.min(imageW, right + extra), Math.min(imageH, top + Math.round(height() * .55f)));
    }

    /** Bilinear sampling avoids square pixel steps around the subject at display resolution. */
    public float[] project(float[] mask, int width, int height, int imageW, int imageH) {
        if (mask == null || width <= 0 || height <= 0 || mask.length != width * height
                || right > imageW || bottom > imageH) throw new IllegalArgumentException("Invalid region mask");
        float[] result = new float[imageW * imageH];
        for (int y = top; y < bottom; y++) {
            float sy = Math.max(0, Math.min(height - 1, (y - top + 0.5f) * height / height() - 0.5f));
            int y0 = (int) sy, y1 = Math.min(height - 1, y0 + 1);
            float fy = sy - y0;
            for (int x = left; x < right; x++) {
                float sx = Math.max(0, Math.min(width - 1, (x - left + 0.5f) * width / width() - 0.5f));
                int x0 = (int) sx, x1 = Math.min(width - 1, x0 + 1);
                float fx = sx - x0;
                float upper = mask[y0 * width + x0] * (1 - fx) + mask[y0 * width + x1] * fx;
                float lower = mask[y1 * width + x0] * (1 - fx) + mask[y1 * width + x1] * fx;
                result[y * imageW + x] = upper * (1 - fy) + lower * fy;
            }
        }
        return result;
    }
}
