package au.steve.hyperlauncher.compat;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class AuroraActivity extends Activity {
    private static final int PICK_PHOTO = 100;
    private ImageView wallpaper;
    private ImageView foreground;
    private android.content.SharedPreferences prefs;
    private float touchStartY;
    private TextView clock;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (clock != null) clock.setText(new SimpleDateFormat(prefs != null && prefs.getBoolean("clock12", false) ? "h:mm" : "HH:mm", Locale.getDefault()).format(new Date()));
            handler.postDelayed(this, 1000);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setShowWhenLocked(true);
        prefs = getSharedPreferences("aurora", MODE_PRIVATE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(6, 13, 29));
        wallpaper = new ImageView(this);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));
        String saved = prefs.getString("wallpaper", null);
        if (saved != null) try { wallpaper.setImageURI(Uri.parse(saved)); } catch (Exception ignored) {}
        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xB9000C28, 0x08000B24, 0xD900071A}));
        root.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = (int)(24 * getResources().getDisplayMetrics().density);
        content.setPadding(pad, (int)(92 * getResources().getDisplayMetrics().density), pad, pad);
        TextView heading = new TextView(this);
        heading.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date()).toUpperCase(Locale.getDefault()));
        heading.setTextColor(0xFFC1D9FF);
        heading.setTextSize(14);
        heading.setLetterSpacing(0.10f);
        heading.setGravity(Gravity.CENTER);
        content.addView(heading);
        clock = new TextView(this);
        clock.setTextColor(0xFFB2D8FF);
        clock.setLetterSpacing(-0.09f);
        clock.setIncludeFontPadding(false);
        applyClockStyle();
        clock.setTextSize(prefs.getInt("clockSize", 112));
        clock.setGravity(Gravity.CENTER);
        content.addView(clock);
        TextView date = new TextView(this);
        date.setText("");
        date.setTextColor(Color.WHITE);
        date.setTextSize(0);
        date.setLetterSpacing(0.07f);
        date.setGravity(Gravity.CENTER);
        content.addView(date);
        foreground = new ImageView(this);
        foreground.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams fgParams = new FrameLayout.LayoutParams(-1, (int)(260 * getResources().getDisplayMetrics().density), Gravity.TOP);
        fgParams.topMargin = (int)(110 * getResources().getDisplayMetrics().density);
        String cutout = prefs.getString("foreground", null);
        if (cutout != null) try { foreground.setImageURI(Uri.parse(cutout)); } catch (Exception ignored) {}
        // Overlay above clock; foreground must be a transparent PNG to create a depth illusion.
        View space = new View(this);
        content.addView(space, new LinearLayout.LayoutParams(1, 0, 1));
        TextView swipe = new TextView(this);
        swipe.setText("⌃");
        swipe.setTextColor(0xFFE4F1FF);
        swipe.setTextSize(30);
        swipe.setGravity(Gravity.CENTER);
        content.addView(swipe);
        TextView hint = new TextView(this);
        hint.setText("SWIPE TO UNLOCK");
        hint.setTextColor(0xFFC2D7F1);
        hint.setLetterSpacing(0.25f);
        hint.setTextSize(10);
        hint.setGravity(Gravity.CENTER);
        content.addView(hint);
        TextView note = new TextView(this);
        note.setText("");
        note.setTextColor(0x99C7DDF5);
        note.setTextSize(10);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0,pad/2,0,0);
        content.addView(note);
        root.setOnTouchListener((v,event)->{
            if(event.getAction()==MotionEvent.ACTION_DOWN){touchStartY=event.getY();return true;}
            if(event.getAction()==MotionEvent.ACTION_UP){
                if(touchStartY-event.getY()>65*getResources().getDisplayMetrics().density)requestSecureUnlock();
                return true;
            }
            return true;
        });
        FrameLayout.LayoutParams gearPosition = new FrameLayout.LayoutParams((int)(44*getResources().getDisplayMetrics().density),(int)(44*getResources().getDisplayMetrics().density),Gravity.TOP|Gravity.RIGHT);
        gearPosition.topMargin=(int)(32*getResources().getDisplayMetrics().density);
        gearPosition.rightMargin=pad/2;
        TextView gear=new TextView(this);
        gear.setText("⋯");
        gear.setTextColor(0xFFE0EDFF);
        gear.setTextSize(23);
        gear.setGravity(Gravity.CENTER);
        gear.setContentDescription("Aurora settings");
        gear.setOnClickListener(v->startActivity(new Intent(this,AuroraSettingsActivity.class)));
        gear.setAlpha(0.30f);
        root.addView(gear,gearPosition);
        FrameLayout.LayoutParams photoPosition = new FrameLayout.LayoutParams((int)(44*getResources().getDisplayMetrics().density),(int)(44*getResources().getDisplayMetrics().density),Gravity.TOP|Gravity.LEFT);
        photoPosition.topMargin=gearPosition.topMargin;
        photoPosition.leftMargin=pad/2;
        TextView photo=new TextView(this);
        photo.setText("▧");
        photo.setTextColor(0xFFE0EDFF);
        photo.setTextSize(24);
        photo.setGravity(Gravity.CENTER);
        photo.setContentDescription("Change wallpaper");
        photo.setOnClickListener(v->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(i,PICK_PHOTO);
        });
        photo.setVisibility(View.GONE);
        root.addView(photo,photoPosition);
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        root.addView(foreground, fgParams);
        // Keep functional shortcuts above the foreground cutout.
        root.bringChildToFront(gear);
        root.bringChildToFront(photo);
        setContentView(root);
    }
    private void applyClockStyle(){
        if(clock==null)return;
        int style=prefs.getInt("clockStyle",0);
        clock.setTypeface(Typeface.create(style==0?"sans-serif-condensed":style==1?"sans-serif-light":"sans-serif",style==0?Typeface.BOLD:Typeface.NORMAL));
    }
    private void requestSecureUnlock() {
        KeyguardManager km = getSystemService(KeyguardManager.class);
        if (km != null && km.isKeyguardLocked()) {
            km.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                @Override public void onDismissSucceeded() { finish(); }
            });
        } else finish();
    }
    @Override protected void onResume() {
        super.onResume();
        if (clock != null) { clock.setTextSize(prefs.getInt("clockSize",112)); applyClockStyle(); }
        if (foreground != null) {
            String u=prefs.getString("foreground",null);
            foreground.setImageDrawable(null);
            if(u!=null)try{foreground.setImageURI(Uri.parse(u));}catch(Exception ignored){}
        }
        handler.removeCallbacks(ticker);handler.post(ticker);
    }
    @Override protected void onPause() { handler.removeCallbacks(ticker); super.onPause(); }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PHOTO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                prefs.edit().putString("wallpaper", uri.toString()).apply();
                wallpaper.setImageURI(uri);
            } catch (Exception ignored) { }
        }
    }
}