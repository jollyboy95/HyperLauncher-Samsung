package au.steve.hyperlauncher.compat.aurora.core;

/** Display policy kept separate from Android view construction. */
public final class NotificationDisplay {
    public static final int LIST = 0, STACK = 1, COUNT = 2;
    private NotificationDisplay() {}
    public static int visibleCards(int mode, boolean expanded, int count) {
        if (count <= 0) return 0;
        if (expanded || mode == LIST) return Math.min(8, count);
        return mode == COUNT ? 0 : 1;
    }
    public static boolean showCount(int mode, int count) {
        return count > 0 && (mode == COUNT || mode == STACK && count > 1);
    }
    public static float dateBaseline(float height, float contentTop) {
        float y = Math.min(height * .87f, contentTop - height * .02f);
        return y < height * .62f ? height * .065f : y;
    }
}
