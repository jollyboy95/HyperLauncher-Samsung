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
        scene.setOnTouchListener((v,e)->{
            if(e.getAction()==MotionEvent.ACTION_DOWN){downY=e.getY();return true;}
            if(e.getAction()==MotionEvent.ACTION_UP){
                if(downY-e.getY()>60*getResources().getDisplayMetrics().density)unlock();
                return true;
            }
            return true;
        });
        // Long-press opens customization without permanently visible controls.
        scene.setOnLongClickListener(v->{startActivity(new Intent(this,AuroraSettingsActivity.class));return true;});
        TextView edit=new TextView(this);
        edit.setText("Customize");edit.setTextColor(0xAAFFFFFF);edit.setTextSize(12);
        edit.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams pos=new FrameLayout.LayoutParams((int)(94*getResources().getDisplayMetrics().density),(int)(42*getResources().getDisplayMetrics().density),Gravity.TOP|Gravity.RIGHT);
        pos.topMargin=(int)(30*getResources().getDisplayMetrics().density);
        root.addView(edit,pos);
        edit.setOnClickListener(v->startActivity(new Intent(this,AuroraSettingsActivity.class)));
        edit.setAlpha(0.45f);
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