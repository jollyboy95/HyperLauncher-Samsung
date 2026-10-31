package au.steve.hyperlauncher.compat;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Frosted static wallpaper, soft tint and a restrained rim; text remains sharp above it. */
public final class GlassBackdrop extends Drawable {
    private final AuroraSceneView scene;
    private final View owner;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private int alpha = 255;
    public GlassBackdrop(AuroraSceneView scene, View owner) { this.scene=scene; this.owner=owner; }
    @Override public void draw(Canvas canvas) {
        RectF bounds = new RectF(getBounds());
        float density = owner.getResources().getDisplayMetrics().density;
        float radius = 24 * density;
        int save = canvas.saveLayerAlpha(bounds,alpha);
        clip.reset();clip.addRoundRect(bounds,radius,radius,Path.Direction.CW);canvas.clipPath(clip);
        scene.drawFrostedBackdrop(canvas,owner);
        paint.reset();paint.setAntiAlias(true);
        paint.setShader(new LinearGradient(0,bounds.top,bounds.width(),bounds.bottom,
                new int[]{0x7A253B50,0x96202D3B,0xA1101A26},null,Shader.TileMode.CLAMP));
        canvas.drawRect(bounds,paint);paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(density*.75f);paint.setColor(0x52FFFFFF);
        canvas.drawRoundRect(new RectF(bounds.left+density/2,bounds.top+density/2,bounds.right-density/2,bounds.bottom-density/2),radius,radius,paint);
        canvas.restoreToCount(save);
    }
    @Override public void setAlpha(int value) { alpha=value; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
