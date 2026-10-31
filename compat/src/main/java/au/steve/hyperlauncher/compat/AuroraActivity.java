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
    private TextView clock;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (clock != null) clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            handler.postDelayed(this, 1000);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setShowWhenLocked(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(6, 13, 29));
        wallpaper = new ImageView(this);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));
        String saved = getPreferences(MODE_PRIVATE).getString("wallpaper", null);
        if (saved != null) try { wallpaper.setImageURI(Uri.parse(saved)); } catch (Exception ignored) {}
        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xB900102A, 0x33000B24, 0xA900071A}));
        root.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = (int)(24 * getResources().getDisplayMetrics().density);
        content.setPadding(pad, pad * 3, pad, pad);
        TextView heading = new TextView(this);
        heading.setText("AURORA DEPTH  •  PREVIEW");
        heading.setTextColor(0xFFC1D9FF);
        heading.setTextSize(12);
        heading.setGravity(Gravity.CENTER);
        content.addView(heading);
        clock = new TextView(this);
        clock.setTextColor(0xFF80B8FF);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clock.setTextSize(88);
        clock.setGravity(Gravity.CENTER);
        content.addView(clock);
        TextView date = new TextView(this);
        date.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date()));
        date.setTextColor(Color.WHITE);
        date.setTextSize(19);
        date.setGravity(Gravity.CENTER);
        content.addView(date);
        View space = new View(this);
        content.addView(space, new LinearLayout.LayoutParams(1, 0, 1));
        Button choose = new Button(this);
        choose.setText("Choose Duchess wallpaper");
        choose.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(i, PICK_PHOTO);
        });
        content.addView(choose);
        Button unlock = new Button(this);
        unlock.setText("Unlock using Samsung security");
        unlock.setOnClickListener(v -> {
            KeyguardManager km = getSystemService(KeyguardManager.class);
            if (km != null && km.isKeyguardLocked()) km.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                @Override public void onDismissSucceeded() { finish(); }
            }); else finish();
        });
        content.addView(unlock);
        TextView note = new TextView(this);
        note.setText("Safe preview only — does not replace Samsung Keyguard or run automatically on wake.");
        note.setTextColor(0xFFB3C7DF);
        note.setTextSize(12);
        note.setGravity(Gravity.CENTER);
        content.addView(note);
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
    }
    @Override protected void onResume() { super.onResume(); handler.removeCallbacks(ticker); handler.post(ticker); }
    @Override protected void onPause() { handler.removeCallbacks(ticker); super.onPause(); }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PHOTO && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getPreferences(MODE_PRIVATE).edit().putString("wallpaper", uri.toString()).apply();
                wallpaper.setImageURI(uri);
            } catch (Exception ignored) { }
        }
    }
}