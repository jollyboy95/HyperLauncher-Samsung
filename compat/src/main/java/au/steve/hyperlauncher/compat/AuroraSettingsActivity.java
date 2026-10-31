package au.steve.hyperlauncher.compat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import android.view.Gravity;
import android.net.Uri;
import android.content.SharedPreferences;
import java.net.*;
import java.io.*;
import org.json.JSONObject;

public final class AuroraSettingsActivity extends Activity {
    private static final String RELEASE_API = "https://api.github.com/repos/jollyboy95/HyperLauncher-Samsung/releases/latest";
    private static final String RELEASE_PAGE = "https://github.com/jollyboy95/HyperLauncher-Samsung/releases";
    private LinearLayout panel;
    private SharedPreferences prefs;
    private TextView updateStatus;
    private void addTitle(String s) { TextView v=new TextView(this);v.setText(s);v.setTextColor(0xFF9DC5FF);v.setTextSize(23);v.setPadding(0,22,0,16);panel.addView(v); }
    private void addInfo(String s) { TextView v=new TextView(this);v.setText(s);v.setTextColor(0xFFD1DFF5);v.setTextSize(14);v.setPadding(0,5,0,14);panel.addView(v); }
    private Button button(String s) { Button b=new Button(this);b.setText(s);panel.addView(b);return b; }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs=getSharedPreferences("aurora",MODE_PRIVATE);
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(0xFF07142B);
        panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
        int p=(int)(22*getResources().getDisplayMetrics().density);
        panel.setPadding(p,p,p,p);scroll.addView(panel);setContentView(scroll);
        addTitle("Aurora Depth · Settings");
        addInfo("Your Samsung fingerprint, face and PIN remain the secure unlock method.");
        addTitle("Clock");
        Switch hour=new Switch(this);hour.setText("12-hour clock");hour.setTextColor(Color.WHITE);
        hour.setChecked(prefs.getBoolean("clock12",false));hour.setOnCheckedChangeListener((v,c)->prefs.edit().putBoolean("clock12",c).apply());panel.addView(hour);
        addInfo("Clock size");
        SeekBar size=new SeekBar(this);size.setMax(60);size.setProgress(prefs.getInt("clockSize",88)-60);
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int progress,boolean user){if(user)prefs.edit().putInt("clockSize",60+progress).apply();}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        });panel.addView(size);
        addTitle("Wallpaper & depth");
        addInfo("Choose your main wallpaper on the Aurora preview. You can optionally select a transparent PNG foreground cutout to overlap the clock. Foreground positioning is experimental.");
        button("Choose foreground cutout (PNG)").setOnClickListener(v->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/png");i.addCategory(Intent.CATEGORY_OPENABLE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,101);
        });
        button("Remove foreground cutout").setOnClickListener(v->{prefs.edit().remove("foreground").apply();Toast.makeText(this,"Cutout removed",Toast.LENGTH_SHORT).show();});
        addTitle("Lock-screen activation");
        addInfo("Automatic display on every screen wake is not enabled. Android restricts third-party apps from launching over Samsung's secure lock screen in the background. Aurora will not claim to replace Keyguard.");
        addTitle("Software update");
        addInfo("Installed version: "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+"). Updates must be signed with Aurora's permanent signing key.");
        updateStatus=new TextView(this);updateStatus.setTextColor(Color.WHITE);panel.addView(updateStatus);
        button("Check GitHub Releases").setOnClickListener(v->checkUpdates());
        button("Open releases").setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(RELEASE_PAGE))));
        button("Back to Aurora").setOnClickListener(v->finish());
    }
    private void checkUpdates() {
        updateStatus.setText("Checking releases…");
        new Thread(()->{
            String result;
            try {
                HttpURLConnection c=(HttpURLConnection)new URL(RELEASE_API).openConnection();
                c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setRequestProperty("Accept","application/vnd.github+json");
                c.setRequestProperty("User-Agent","AuroraDepth-Android");
                int status=c.getResponseCode();
                if(status==404) result="No published release yet. GitHub Actions artifacts are not published releases.";
                else if(status!=200) result="Update check unavailable (HTTP "+status+").";
                else {
                    ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;
                    try(InputStream in=c.getInputStream()){while((n=in.read(buf))!=-1 && out.size()<65536)out.write(buf,0,n);}
                    JSONObject release=new JSONObject(out.toString("UTF-8"));
                    result="Latest published release: "+release.optString("tag_name","unknown")+"\nOpen releases to download. Installation still requires Android confirmation.";
                }
                c.disconnect();
            }catch(Exception e){result="Unable to check for updates. Check your internet connection.";}
            String message=result;runOnUiThread(()->updateStatus.setText(message));
        }).start();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==101&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri u=data.getData();
            try {getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                prefs.edit().putString("foreground",u.toString()).apply();Toast.makeText(this,"Cutout saved",Toast.LENGTH_SHORT).show();
            }catch(Exception e){Toast.makeText(this,"Unable to save cutout",Toast.LENGTH_SHORT).show();}
        }
    }
}