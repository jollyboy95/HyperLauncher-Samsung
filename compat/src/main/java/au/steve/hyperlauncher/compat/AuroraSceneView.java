package au.steve.hyperlauncher.compat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.net.Uri;
import android.view.View;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Shared, resolution-independent renderer for the lock screen and settings preview. */
public final class AuroraSceneView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final SharedPreferences prefs;
    private Bitmap background,foreground;
    private boolean preview;
    private boolean editing;
    private int dragClockStart;
    private String loadedBackground="",loadedForeground="";
    public AuroraSceneView(Context context,boolean preview){
        super(context);this.preview=preview;
        prefs=context.getSharedPreferences("aurora",Context.MODE_PRIVATE);
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        reload();
    }
    public void setEditing(boolean value){editing=value;invalidate();}
    public void startClockDrag(){dragClockStart=prefs.getInt("clockY",0);}
    public void moveClock(float delta){int y=Math.max(-135,Math.min(250,dragClockStart+Math.round(delta*844f/Math.max(1,getHeight()))));prefs.edit().putInt("clockY",y).apply();invalidate();}
    private Bitmap load(String value){
        if(value==null)return null;
        try(InputStream stream=getContext().getContentResolver().openInputStream(Uri.parse(value))){
            BitmapFactory.Options o=new BitmapFactory.Options();
            o.inJustDecodeBounds=true;
            BitmapFactory.decodeStream(stream,null,o);
            int max=1800;
            o.inSampleSize=1;
            while(o.outWidth/o.inSampleSize>max||o.outHeight/o.inSampleSize>max)o.inSampleSize*=2;
            o.inJustDecodeBounds=false;
            try(InputStream actual=getContext().getContentResolver().openInputStream(Uri.parse(value))){
                return BitmapFactory.decodeStream(actual,null,o);
            }
        }catch(Exception ignored){return null;}
    }
    public void reload(){
        String bg=prefs.getString("wallpaper",null),fg=prefs.getString("foreground",null);
        if(!String.valueOf(bg).equals(loadedBackground)){
            background=load(bg);loadedBackground=String.valueOf(bg);
        }
        if(!String.valueOf(fg).equals(loadedForeground)){
            foreground=load(fg);loadedForeground=String.valueOf(fg);
        }
        invalidate();
    }
    private void imageCover(Canvas canvas,Bitmap bitmap,float w,float h){
        float scale=Math.max(w/bitmap.getWidth(),h/bitmap.getHeight());
        float bw=bitmap.getWidth()*scale,bh=bitmap.getHeight()*scale;
        paint.reset();paint.setAntiAlias(true);paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap,null,new RectF((w-bw)/2,(h-bh)/2,(w+bw)/2,(h+bh)/2),paint);
    }
    private void text(Canvas c,String s,float x,float baseline,float size,int color,Typeface face,boolean center){
        paint.reset();paint.setAntiAlias(true);paint.setColor(color);paint.setTypeface(face);
        paint.setTextSize(size);paint.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);
        c.drawText(s,x,baseline,paint);
    }
    @Override protected void onDraw(Canvas original){
        super.onDraw(original);
        float width=getWidth(),height=getHeight();
        if(width<=0||height<=0)return;
        // Same virtual coordinate system for both settings preview and full screen.
        Canvas c=original;
        c.save();
        c.scale(width/390f,height/844f);
        paint.setColor(0xFF061125);c.drawColor(0xFF061125);
        if(background!=null)imageCover(c,background,390,844);
        paint.setShader(new LinearGradient(0,0,0,844,new int[]{0xC8000C24,0x05000A1B,0xAF000715},null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,390,844,paint);paint.setShader(null);
        String date=new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(new Date()).toUpperCase(Locale.getDefault());
        Typeface dateFace=Typeface.create("sans-serif-medium",Typeface.NORMAL);
        text(c,date,195,82,12,0xFFE6F1FF,dateFace,true);
        String digits=new SimpleDateFormat(prefs.getBoolean("clock12",false)?"hhmm":"HHmm",Locale.getDefault()).format(new Date());
        int style=prefs.getInt("clockStyle",0);
        Typeface clockFace=Typeface.create(style==0?"sans-serif-condensed":style==1?"sans-serif-light":"sans-serif",style==0?Typeface.BOLD:Typeface.NORMAL);
        int preset=prefs.getInt("clockPreset",0);
        float size=prefs.getInt("clockSize",160)*(preset==1?0.89f:1.12f);
        float top=170+prefs.getInt("clockY",0);
        int color=prefs.getInt("clockColor",0xBFD0E9FF);
        float x=preset==2?140:195;
        if(preset==1)text(c,digits.substring(0,2)+":"+digits.substring(2),195,top+30,size*0.62f,color,clockFace,true);
        else {
            text(c,digits.substring(0,2),x,top,size,color,clockFace,true);
            text(c,digits.substring(2),x,top+size*0.77f,size,color,clockFace,true);
        }
        if(editing){
            paint.setShader(null);paint.setColor(0x668DC9FF);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);
            c.drawRoundRect(new RectF(25,top-size+10,365,top+size),16,16,paint);
            paint.setStyle(Paint.Style.FILL);
        }
        // Optional transparent PNG foreground is composited above both rows of digits.
        if(foreground!=null){
            paint.reset();paint.setAntiAlias(true);paint.setFilterBitmap(true);
            float scale=Math.max(390f/foreground.getWidth(),844f/foreground.getHeight());
            float bw=foreground.getWidth()*scale,bh=foreground.getHeight()*scale;
            c.drawBitmap(foreground,null,new RectF((390-bw)/2,(844-bh)/2,(390+bw)/2,(844+bh)/2),paint);
        }
        if(!preview){
            text(c,"⌃",195,772,29,0xFFE8F3FF,Typeface.DEFAULT,true);
            text(c,"SWIPE TO UNLOCK",195,801,10,0xFFD0DDF1,dateFace,true);
        }
        c.restore();
    }
}