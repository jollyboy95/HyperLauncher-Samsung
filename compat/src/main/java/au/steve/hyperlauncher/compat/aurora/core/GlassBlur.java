package au.steve.hyperlauncher.compat.aurora.core;

/** Linear-time separable box blur for a small cached wallpaper backdrop. */
public final class GlassBlur {
    private GlassBlur() {}
    public static int[] blur(int[] source, int width, int height, int radius) {
        if (source == null || width <= 0 || height <= 0 || source.length != width * height || radius < 0 || radius > 32)
            throw new IllegalArgumentException("Invalid blur image");
        int[] result = source.clone();
        if (radius == 0) return result;
        int[] scratch = new int[result.length];
        for (int pass = 0; pass < 3; pass++) {
            axis(result, scratch, width, height, radius, true);
            axis(scratch, result, width, height, radius, false);
        }
        return result;
    }
    private static void axis(int[] source, int[] dest, int width, int height, int radius, boolean horizontal) {
        int rows = horizontal ? height : width, columns = horizontal ? width : height;
        int count = radius * 2 + 1;
        for (int row = 0; row < rows; row++) {
            int a=0,r=0,g=0,b=0;
            for (int k=-radius;k<=radius;k++) {
                int col=Math.max(0,Math.min(columns-1,k));
                int pixel=source[horizontal ? row*width+col : col*width+row];
                a+=pixel>>>24;r+=(pixel>>>16)&255;g+=(pixel>>>8)&255;b+=pixel&255;
            }
            for (int col=0;col<columns;col++) {
                dest[horizontal ? row*width+col : col*width+row]=(a/count<<24)|(r/count<<16)|(g/count<<8)|b/count;
                int old=Math.max(0,col-radius),next=Math.min(columns-1,col+radius+1);
                int x=source[horizontal ? row*width+old : old*width+row];
                int y=source[horizontal ? row*width+next : next*width+row];
                a+=(y>>>24)-(x>>>24);r+=((y>>>16)&255)-((x>>>16)&255);
                g+=((y>>>8)&255)-((x>>>8)&255);b+=(y&255)-(x&255);
            }
        }
    }
}
