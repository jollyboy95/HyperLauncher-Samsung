package au.steve.hyperlauncher.compat;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.provider.MediaStore;

public final class AuroraActivity extends Activity {
    private AuroraSceneView scene;
    private float downY;
    private boolean torchOn;
    private String torchId;
    private CameraManager cameras;
    private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable tick=new Runnable(){public void run(){if(scene!=null)scene.invalidate();handler.postDelayed(this,10000);}};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        setShowWhenLocked(true);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        FrameLayout root=new FrameLayout(this);
        scene=new AuroraSceneView(this,false);
        root.addView(scene,new FrameLayout.LayoutParams(-1,-1));
        android.view.GestureDetector gestures=new android.view.GestureDetector(this,new android.view.GestureDetector.SimpleOnGestureListener(){
            @Override public void onLongPress(MotionEvent e){startActivity(new Intent(AuroraActivity.this,AuroraSettingsActivity.class));}
        });
        scene.setOnTouchListener((v,e)->{
            gestures.onTouchEvent(e);
            if(e.getAction()==MotionEvent.ACTION_DOWN){downY=e.getY();return true;}
            if(e.getAction()==MotionEvent.ACTION_UP){
                if(downY-e.getY()>60*getResources().getDisplayMetrics().density)unlock();
                return true;
            }
            return true;
        });

        cameras=(CameraManager)getSystemService(CAMERA_SERVICE);
        TextView flash=shortcut("✦");
        FrameLayout.LayoutParams fl=new FrameLayout.LayoutParams(dp(58),dp(58),Gravity.BOTTOM|Gravity.LEFT);
        fl.leftMargin=dp(28);fl.bottomMargin=dp(48);root.addView(flash,fl);
        flash.setContentDescription("Toggle flashlight");
        flash.setOnClickListener(v->toggleTorch());
        TextView camera=shortcut("◎");
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(58),dp(58),Gravity.BOTTOM|Gravity.RIGHT);
        cp.rightMargin=dp(28);cp.bottomMargin=dp(48);root.addView(camera,cp);
        camera.setContentDescription("Open camera");
        camera.setOnClickListener(v->{
            try{startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));}
            catch(Exception ex){android.widget.Toast.makeText(this,"Camera unavailable",0).show();}
        });
        setContentView(root);
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView shortcut(String symbol){
        TextView view=new TextView(this);
        view.setText(symbol);view.setTextSize(27);view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        GradientDrawable glass=new GradientDrawable();
        glass.setColor(0x70435B78);glass.setCornerRadius(dp(30));glass.setStroke(dp(1),0x70FFFFFF);
        view.setBackground(glass);
        return view;
    }
    private void toggleTorch(){
        try{
            if(torchId==null){
                for(String id:cameras.getCameraIdList()){
                    CameraCharacteristics ch=cameras.getCameraCharacteristics(id);
                    if(Boolean.TRUE.equals(ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))
                      &&Integer.valueOf(CameraCharacteristics.LENS_FACING_BACK).equals(ch.get(CameraCharacteristics.LENS_FACING))){
                        torchId=id;break;
                    }
                }
            }
            if(torchId==null){android.widget.Toast.makeText(this,"Flashlight unavailable",0).show();return;}
            cameras.setTorchMode(torchId,!torchOn);torchOn=!torchOn;
        }catch(Exception ex){android.widget.Toast.makeText(this,"Flashlight unavailable",0).show();}
    }
    private void unlock(){
        KeyguardManager km=getSystemService(KeyguardManager.class);
        if(km!=null&&km.isKeyguardLocked()){
            km.requestDismissKeyguard(this,new KeyguardManager.KeyguardDismissCallback(){
                @Override public void onDismissSucceeded(){finish();}
            });
        }else finish();
    }
    @Override protected void onResume(){super.onResume();scene.reload();handler.removeCallbacks(tick);handler.post(tick);}
    @Override protected void onPause(){
        handler.removeCallbacks(tick);
        if(torchOn&&torchId!=null){try{cameras.setTorchMode(torchId,false);}catch(Exception ignored){}torchOn=false;}
        super.onPause();
    }
}