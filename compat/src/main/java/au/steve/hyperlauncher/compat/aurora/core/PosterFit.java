package au.steve.hyperlauncher.compat.aurora.core;

/** Fits the neon clock and its date into the space above live lock-screen content. */
public final class PosterFit {
    public final float scaleX, scaleY, x, y;
    private PosterFit(float sx,float sy,float x,float y) { scaleX=sx;scaleY=sy;this.x=x;this.y=y; }
    public static PosterFit neon(float width,float height,float size,float stretch,float offsetX,float offsetY,float contentTop) {
        float bottom=Math.min(height*.80f,contentTop>0?contentTop-height*.035f:height*.80f);
        bottom=Math.max(height*.30f,bottom);
        float top=height*.06f;
        float sx=Math.min(size/168f,1.1f);
        float sy=Math.min(stretch,(bottom-top)/(height*.715f));
        float x=width*.5f*(1-sx)+offsetX;
        float y=height*.3f*(1-sy)+offsetY;
        x=Math.max(width*.04f-width*.10f*sx,Math.min(width*.96f-width*.92f*sx,x));
        y=Math.max(top-height*.075f*sy,Math.min(bottom-height*.79f*sy,y));
        return new PosterFit(sx,sy,x,y);
    }
}
