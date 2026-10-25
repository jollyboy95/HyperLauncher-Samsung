package au.steve.hyperlauncher.compat;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int IMPORT_APK = 10, SAVE_REPORT = 11;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView output;
    private Button inspect, save;
    private volatile String report;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        page.setPadding(padding, padding, padding, padding);
        page.setBackgroundColor(Color.rgb(16, 19, 26));
        TextView title = new TextView(this); title.setText("HyperLauncher Compatibility"); title.setTextSize(25); title.setTextColor(Color.WHITE); page.addView(title);
        TextView description = new TextView(this);
        description.setText("Development prototype 0.1\nInspect the latest Xiaomi launcher and this phone. This app does not provide a home screen or install runtime files.");
        description.setTextSize(16); description.setTextColor(Color.LTGRAY); description.setPadding(0, padding, 0, padding); page.addView(description);
        inspect = new Button(this); inspect.setText("Select launcher APK");
        inspect.setOnClickListener(v -> startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE), IMPORT_APK)); page.addView(inspect);
        save = new Button(this); save.setText("Save report"); save.setEnabled(false);
        save.setOnClickListener(v -> startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, "hyperlauncher-compatibility.json"), SAVE_REPORT)); page.addView(save);
        ScrollView scroll = new ScrollView(this); output = new TextView(this); output.setTextColor(Color.WHITE); output.setTextSize(13); output.setTextIsSelectable(true);
        output.setText("Ready. Select the 8.01.02.8584 APK to inspect it. No root or storage permission is required.");
        scroll.addView(output); page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(page);
        // Keep controls clear of gesture/status bars on edge-to-edge Android releases.
        page.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(padding + insets.getSystemWindowInsetLeft(), padding + insets.getSystemWindowInsetTop(), padding + insets.getSystemWindowInsetRight(), padding + insets.getSystemWindowInsetBottom());
            return insets;
        });
    }
    private JSONObject device() throws Exception {
        JSONObject out = new JSONObject();
        out.put("manufacturer", Build.MANUFACTURER); out.put("model", Build.MODEL);
        out.put("sdk", Build.VERSION.SDK_INT); out.put("abis", new JSONArray(Arrays.asList(Build.SUPPORTED_ABIS)));
        String[] libraries = getPackageManager().getSystemSharedLibraryNames();
        boolean registered = libraries != null && Arrays.asList(libraries).contains("hyperos.rustruntime.v5");
        out.put("xiaomi_runtime_v5_registered", registered);
        JSONObject files = new JSONObject();
        for (String name : new String[]{"/system_ext/bin/hyos_spawner", "/system_ext/lib64/libhyper_os_shell.so", "/system_ext/lib64/libhyper_os_flutter.so"}) files.put(name, new File(name).canRead());
        out.put("readable_runtime_paths", files);
        out.put("visible_widget_provider_count", AppWidgetManager.getInstance(this).getInstalledProviders().size());
        out.put("interpretation", "Filesystem visibility and registered library checks are preliminary; they are not a successful native load test.");
        return out;
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        inspect.setEnabled(false); save.setEnabled(false); output.setText(request == IMPORT_APK ? "Inspecting APK…" : "Saving report…");
        worker.execute(() -> {
            File temp = null;
            try {
                if (request == IMPORT_APK) {
                    temp = File.createTempFile("launcher-", ".apk", getCacheDir());
                    try (InputStream in = getContentResolver().openInputStream(uri); OutputStream out = Files.newOutputStream(temp.toPath())) {
                        if (in == null) throw new java.io.IOException("Could not open selected file");
                        byte[] buf = new byte[65536]; long total = 0; int n;
                        while ((n = in.read(buf)) != -1) {
                            total += n; if (total > 256L * 1024 * 1024) throw new java.io.IOException("APK exceeds 256 MB inspection limit");
                            out.write(buf, 0, n);
                        }
                    }
                    report = new JSONObject().put("schema_version", 1).put("device", device()).put("apk", ApkInspector.inspect(this, temp)).toString(2);
                    show(report, true);
                } else if (request == SAVE_REPORT && report != null) {
                    try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                        if (out == null) throw new java.io.IOException("Could not create report");
                        out.write(report.getBytes(StandardCharsets.UTF_8));
                    }
                    show("Report saved.\n\n" + report, true);
                }
            } catch (Exception e) { show("Inspection/export failed: " + e.getClass().getSimpleName() + ": " + e.getMessage(), report != null); }
            finally { if (temp != null) temp.delete(); }
        });
    }
    private void show(String text, boolean canSave) {
        runOnUiThread(() -> { if (!isDestroyed()) { output.setText(text); inspect.setEnabled(true); save.setEnabled(canSave); } });
    }
    @Override protected void onDestroy() { worker.shutdown(); super.onDestroy(); }
}
