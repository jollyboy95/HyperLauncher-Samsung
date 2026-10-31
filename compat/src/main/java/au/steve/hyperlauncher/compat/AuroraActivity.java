package au.steve.hyperlauncher.compat;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.TextView;

public final class AuroraActivity extends Activity {
    private AuroraSceneView scene;
    private float downY;
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

        setContentView(root);
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
    @Override protected void onPause(){handler.removeCallbacks(tick);super.onPause();}
}