package au.steve.hyperlauncher.compat.aurora.core;

/** Shared cover-crop geometry for a wallpaper and its aligned foreground mask. */
public final class WallpaperTransform {
    public final float scale, dx, dy, focusX, focusY;
    private WallpaperTransform(float s,float x,float y,float fx,float fy) {
        scale=s; dx=x; dy=y; focusX=fx; focusY=fy;
    }
    public static WallpaperTransform compute(int iw,int ih,float sw,float sh,float zoom,float fx,float fy) {
        if(iw<=0||ih<=0||sw<=0||sh<=0)return new WallpaperTransform(1,0,0,.5f,.5f);
        float s=Math.max(sw/iw,sh/ih)*Math.max(1,zoom);
        float x=resolve(fx,sw,iw*s), y=resolve(fy,sh,ih*s);
        return new WallpaperTransform(s,sw/2-x*iw*s,sh/2-y*ih*s,x,y);
    }
    public static float resolve(float focus,float screen,float scaled) {
        float half=screen/(2*scaled);
        if(half>=.5f)return .5f;
        if(Float.isNaN(focus))return .5f;
        return Math.max(half,Math.min(1-half,focus));
    }
    public float[] pan(int iw,int ih,float px,float py) {
        return new float[]{focusX-px/(iw*scale),focusY-py/(ih*scale)};
    }
}
