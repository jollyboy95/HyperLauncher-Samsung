package au.steve.hyperlauncher.compat;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.os.Environment;
import android.provider.Settings;
import org.json.JSONArray;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.view.View;
import android.widget.FrameLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
    private Button downloadButton;
    private String availableApk;
    private TextView previewClock;
    private ImageView previewWallpaper;
    private ImageView previewForeground;
    private final android.os.Handler previewHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable previewTicker = new Runnable() { public void run() { refreshPreview(); previewHandler.postDelayed(this, 15000); } };
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
        addTitle("Live preview");
        createPreview();
        addInfo("Changes appear here immediately. Your wallpaper and optional foreground cutout are shown in the preview.");
        addTitle("Clock");
        Switch hour=new Switch(this);hour.setText("12-hour clock");hour.setTextColor(Color.WHITE);
        hour.setChecked(prefs.getBoolean("clock12",false));hour.setOnCheckedChangeListener((v,c)->{prefs.edit().putBoolean("clock12",c).apply();refreshPreview();});panel.addView(hour);
        addInfo("Clock style");
        String[] styles={"HyperOS Bold","Elegant Light","Modern Rounded"};
        Spinner style=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,styles);
        style.setAdapter(adapter);
        style.setSelection(prefs.getInt("clockStyle",0));
        style.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){prefs.edit().putInt("clockStyle",position).apply();refreshPreview();}
            public void onNothingSelected(android.widget.AdapterView<?> parent){}
        });
        panel.addView(style);
        addInfo("Clock size");
        SeekBar size=new SeekBar(this);size.setMax(120);size.setProgress(prefs.getInt("clockSize",160)-80);
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int progress,boolean user){if(user){prefs.edit().putInt("clockSize",80+progress).apply();refreshPreview();}}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        });panel.addView(size);
        addTitle("Wallpaper & depth");
        addInfo("Choose your main wallpaper on the Aurora preview. You can optionally select a transparent PNG foreground cutout to overlap the clock. Foreground positioning is experimental.");
        button("Choose foreground cutout (PNG)").setOnClickListener(v->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/png");i.addCategory(Intent.CATEGORY_OPENABLE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,101);
        });
        button("Remove foreground cutout").setOnClickListener(v->{prefs.edit().remove("foreground").apply();refreshPreview();Toast.makeText(this,"Cutout removed",Toast.LENGTH_SHORT).show();});
        addTitle("Lock-screen activation");
        addInfo("Automatic display on every screen wake is not enabled. Android restricts third-party apps from launching over Samsung's secure lock screen in the background. Aurora will not claim to replace Keyguard.");
        addTitle("Software update");
        addInfo("Installed version: "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+"). Updates must be signed with Aurora's permanent signing key.");
        updateStatus=new TextView(this);updateStatus.setTextColor(Color.WHITE);panel.addView(updateStatus);
        button("Check for updates").setOnClickListener(v->checkUpdates());
        downloadButton=button("Download & install update");
        downloadButton.setEnabled(false);
        downloadButton.setOnClickListener(v->downloadUpdate());
        button("Open releases").setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(RELEASE_PAGE))));
        button("Back to Aurora").setOnClickListener(v->finish());
    }
    private void createPreview() {
        float d=getResources().getDisplayMetrics().density;
        FrameLayout frame=new FrameLayout(this);
        frame.setBackgroundColor(0xFF101D33);
        frame.setClipChildren(true);
        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,(int)(380*d));
        panel.addView(frame,layout);
        previewWallpaper=new ImageView(this);
        previewWallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(previewWallpaper,new FrameLayout.LayoutParams(-1,-1));
        View shade=new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xB900102A,0x22000B24,0xA900071A}));
        frame.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout labels=new LinearLayout(this);
        labels.setGravity(Gravity.CENTER_HORIZONTAL);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(8,(int)(38*d),8,0);
        TextView caption=new TextView(this);
        caption.setText("AURORA DEPTH");
        caption.setTextSize(10);caption.setTextColor(0xFFC1D9FF);caption.setGravity(Gravity.CENTER);
        labels.addView(caption);
        previewClock=new TextView(this);
        previewClock.setGravity(Gravity.CENTER);
        previewClock.setTextColor(0xFF80B8FF);
        previewClock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        labels.addView(previewClock);
        TextView date=new TextView(this);
        date.setText(new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(new Date()));
        date.setTextColor(Color.WHITE);date.setTextSize(14);date.setGravity(Gravity.CENTER);
        labels.addView(date);
        frame.addView(labels,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        previewForeground=new ImageView(this);
        previewForeground.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams fg=new FrameLayout.LayoutParams(-1,(int)(185*d),Gravity.TOP);
        fg.topMargin=(int)(68*d);
        frame.addView(previewForeground,fg);
        refreshPreview();
    }
    private void refreshPreview() {
        if(previewClock==null)return;
        String time=new SimpleDateFormat(prefs.getBoolean("clock12",false)?"hhmm":"HHmm",Locale.getDefault()).format(new Date());
        previewClock.setText(time.substring(0,2)+"\n"+time.substring(2));
        // Preview is approximately two-thirds the full-screen clock size.
        previewClock.setTextSize(prefs.getInt("clockSize",160)*0.55f);
        previewClock.setLineSpacing(-16*getResources().getDisplayMetrics().density,0.84f);
        previewClock.setIncludeFontPadding(false);
        int style=prefs.getInt("clockStyle",0);
        previewClock.setTypeface(Typeface.create(style==0?"sans-serif-condensed":style==1?"sans-serif-light":"sans-serif",style==0?Typeface.BOLD:Typeface.NORMAL));
        if(previewWallpaper!=null){
            String uri=prefs.getString("wallpaper",null);
            previewWallpaper.setImageDrawable(null);
            if(uri!=null)try{previewWallpaper.setImageURI(Uri.parse(uri));}catch(Exception ignored){}
        }
        if(previewForeground!=null){
            String uri=prefs.getString("foreground",null);
            previewForeground.setImageDrawable(null);
            if(uri!=null)try{previewForeground.setImageURI(Uri.parse(uri));}catch(Exception ignored){}
        }
    }
    @Override protected void onResume(){super.onResume();previewHandler.removeCallbacks(previewTicker);previewHandler.post(previewTicker);}
    @Override protected void onPause(){previewHandler.removeCallbacks(previewTicker);super.onPause();}
    private void downloadUpdate() {
        if(availableApk==null)return;
        if(!getPackageManager().canRequestPackageInstalls()){
            updateStatus.setText("Allow Aurora to install unknown apps, then try again.");
            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));
            return;
        }
        try {
            DownloadManager.Request request=new DownloadManager.Request(Uri.parse(availableApk));
            request.setTitle("Aurora Depth update");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(this,Environment.DIRECTORY_DOWNLOADS,"aurora-update.apk");
            DownloadManager manager=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
            long id=manager.enqueue(request);
            updateStatus.setText("Downloading signed APK. Tap the completed download notification to install and confirm with Android. Download ID: "+id);
            downloadButton.setEnabled(false);
        }catch(Exception e){updateStatus.setText("Unable to download update. Open Releases instead.");}
    }
    private void checkUpdates() {
        updateStatus.setText("Checking releases…");
        downloadButton.setEnabled(false);
        new Thread(()->{
            String result;
            String apkUrl=null;
            try {
                HttpURLConnection c=(HttpURLConnection)new URL(RELEASE_API).openConnection();
                c.setConnectTimeout(8000);c.setReadTimeout(8000);
                c.setRequestProperty("Accept","application/vnd.github+json");
                c.setRequestProperty("User-Agent","AuroraDepth-Android");
                int status=c.getResponseCode();
                if(status==404)result="No published Aurora release yet. Signed Actions artifacts are not Releases.";
                else if(status==403)result="GitHub API rate limit or access restriction (403). Try again later.";
                else if(status!=200)result="Update check unavailable (HTTP "+status+").";
                else {
                    ByteArrayOutputStream out=new ByteArrayOutputStream();
                    byte[] buf=new byte[4096];int n;
                    try(InputStream in=c.getInputStream()){
                        while((n=in.read(buf))!=-1 && out.size()<65536)out.write(buf,0,n);
                    }
                    JSONObject release=new JSONObject(out.toString("UTF-8"));
                    String tag=release.optString("tag_name","");
                    int remote=-1;
                    if(tag.matches("aurora-v[0-9]+"))remote=Integer.parseInt(tag.substring(8));
                    if(remote<=BuildConfig.VERSION_CODE)result="Aurora is up to date: "+BuildConfig.VERSION_NAME;
                    else {
                        JSONArray assets=release.optJSONArray("assets");
                        if(assets!=null)for(int i=0;i<assets.length();i++){
                            JSONObject asset=assets.optJSONObject(i);
                            if(asset!=null && asset.optString("name").endsWith(".apk")){
                                apkUrl=asset.optString("browser_download_url",null);
                                break;
                            }
                        }
                        result=apkUrl==null?"New release "+tag+" found, but no APK is attached.":"Update available: "+tag+"\\n"+release.optString("body","")+"\\nTap Download & install.";
                    }
                }
                c.disconnect();
            }catch(Exception e){result="Unable to check for updates. Check your connection.";}
            String message=result,found=apkUrl;
            runOnUiThread(()->{availableApk=found;downloadButton.setEnabled(found!=null);updateStatus.setText(message);});
        }).start();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==101&&result==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri u=data.getData();
            try {getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                prefs.edit().putString("foreground",u.toString()).apply();refreshPreview();Toast.makeText(this,"Cutout saved",Toast.LENGTH_SHORT).show();
            }catch(Exception e){Toast.makeText(this,"Unable to save cutout",Toast.LENGTH_SHORT).show();}
        }
    }
}