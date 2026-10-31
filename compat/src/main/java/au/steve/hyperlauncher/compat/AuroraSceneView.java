package au.steve.hyperlauncher.compat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.net.Uri;
import android.view.View;
import au.steve.hyperlauncher.compat.aurora.core.WallpaperTransform;
import au.steve.hyperlauncher.compat.aurora.core.ColorMath;
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
    public int safeClockY(int desired){
        int preset=prefs.getInt("clockPreset",0);
        float size=prefs.getInt("clockSize",160)*(preset==1?0.89f:preset==3?0.72f:1.12f);
        // Keep both rows inside the composition, even at maximum font size.
        int minimum=(int)Math.ceil(size*0.82f+54f-170f);
        int maximum=(int)Math.floor(720f-170f-(preset==1?30f:preset==3?size*0.1f:size*0.77f));
        return Math.max(minimum,Math.min(Math.max(minimum,maximum),desired));
    }
    public void moveClock(float delta){
        int y=safeClockY(dragClockStart+Math.round(delta*844f/Math.max(1,getHeight())));
        prefs.edit().putInt("clockY",y).apply();invalidate();
    }
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
        WallpaperTransform t=WallpaperTransform.compute(bitmap.getWidth(),bitmap.getHeight(),w,h,
            prefs.getFloat("wallpaperZoom",1f),prefs.getFloat("wallpaperFocusX",.5f),
            prefs.getFloat("wallpaperFocusY",.5f));
        paint.reset();paint.setAntiAlias(true);paint.setFilterBitmap(true);
        canvas.drawBitmap(bitmap,null,new RectF(t.dx,t.dy,t.dx+bitmap.getWidth()*t.scale,
            t.dy+bitmap.getHeight()*t.scale),paint);
    }
    private int clockColour(){
        int manual=prefs.getInt("clockColor",0xBFD0E9FF);
        if(!prefs.getBoolean("autoClockColor",false)||background==null)return manual;
        int w=background.getWidth(),h=background.getHeight();
        int[] samples=new int[144];int k=0;
        for(int y=0;y<12;y++)for(int x=0;x<12;x++)
            samples[k++]=background.getPixel(Math.min(w-1,(x*2+1)*w/24),
                Math.min(h-1,(y*2+1)*h/24));
        return ColorMath.fromSamples(samples);
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
        String date=new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(new Date());
        Typeface dateFace=Typeface.create("sans-serif-medium",Typeface.NORMAL);
        String digits=new SimpleDateFormat(prefs.getBoolean("clock12",false)?"hhmm":"HHmm",Locale.getDefault()).format(new Date());
        int style=prefs.getInt("clockStyle",0);
        Typeface clockFace=Typeface.create(style==0?"sans-serif-condensed":style==1?"sans-serif-light":"sans-serif",style==0?Typeface.BOLD:Typeface.NORMAL);
        int preset=prefs.getInt("clockPreset",0);
        float size=prefs.getInt("clockSize",160);
        int color=clockColour();
        // Measure the actual glyphs so the date and clock form one non-overlapping group.
        Paint measure=new Paint(Paint.ANTI_ALIAS_FLAG);
        measure.setTypeface(clockFace);
        float x=preset==2?145f:195f;
        String hours=digits.substring(0,2),minutes=digits.substring(2);
        float requested=size*(preset==1?.65f:preset==3?.72f:1f);
        float maxWidth=preset==1?355f:preset==3?355f:315f;
        float widest=preset==1?measure.measureText(hours+":"+minutes):
            preset==3?measure.measureText(digits):Math.max(measure.measureText(hours),measure.measureText(minutes));
        measure.setTextSize(1f);
        widest=preset==1?measure.measureText(hours+":"+minutes):
            preset==3?measure.measureText(digits):Math.max(measure.measureText(hours),measure.measureText(minutes));
        float fitted=Math.min(requested,maxWidth/Math.max(.01f,widest));
        float lineHeight=fitted*.83f;
        float clockHeight=preset==0||preset==2?fitted+lineHeight:fitted;
        float groupHeight=clockHeight+34f;
        float top=Math.max(42f,Math.min(722f-groupHeight,116f+safeClockY(prefs.getInt("clockY",0))));
        float dateY=top+12f;
        float firstBaseline=top+25f+fitted*.80f;
        text(c,date,x,dateY,12,0xFFE6F1FF,dateFace,true);
        if(preset==1){
            text(c,hours+":"+minutes,195f,firstBaseline,fitted,color,clockFace,true);
        }else if(preset==3){
            text(c,digits,195f,firstBaseline,fitted,color,clockFace,true);
        }else{
            text(c,hours,x,firstBaseline,fitted,color,clockFace,true);
            text(c,minutes,x,firstBaseline+lineHeight,fitted,color,clockFace,true);
        }
        if(editing){
            paint.reset();paint.setAntiAlias(true);paint.setColor(0x668DC9FF);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);
            c.drawRoundRect(new RectF(20f,Math.max(20f,top-8f),370f,
                Math.min(745f,firstBaseline+(preset==0||preset==2?lineHeight:0)+12f)),16,16,paint);
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