package au.steve.hyperlauncher.compat;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ApkInspector {
    public static final String EXPECTED_SHA = "4b1210faa591485a60fce85783e9ffe689a724685588adb13bdc868c159259bf";
    private ApkInspector() {}
    public static JSONObject inspect(Context context, File file) throws Exception {
        JSONObject out = new JSONObject();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file.toPath())) {
            byte[] buf = new byte[65536]; int n;
            while ((n = in.read(buf)) != -1) digest.update(buf, 0, n);
        }
        StringBuilder hash = new StringBuilder();
        for (byte b : digest.digest()) hash.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        out.put("sha256", hash.toString());
        out.put("matches_selected_latest_build", EXPECTED_SHA.equals(hash.toString()));
        out.put("bytes", file.length());
        PackageInfo info = context.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_META_DATA);
        if (info != null) {
            out.put("package", info.packageName); out.put("version", info.versionName);
            out.put("min_sdk", info.applicationInfo.minSdkVersion);
            out.put("target_sdk", info.applicationInfo.targetSdkVersion);
        } else out.put("package_parse", "Android did not return package metadata; ZIP inspection still available");
        JSONArray dex = new JSONArray(); JSONObject nativeDeps = new JSONObject();
        Set<String> bundled = new HashSet<>(); TreeSet<String> dependencies = new TreeSet<>();
        try (ZipFile zip = new ZipFile(file)) {
            ArrayList<String> nativeNames = new ArrayList<>();
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.matches("classes[0-9]*\\.dex")) dex.put(name);
                if (name.startsWith("lib/arm64-v8a/") && name.endsWith(".so")) nativeNames.add(name);
            }
            Collections.sort(nativeNames);
            for (String name : nativeNames) {
                bundled.add(name.substring(name.lastIndexOf('/') + 1));
                ZipEntry entry = zip.getEntry(name);
                if (entry.getSize() > 96L * 1024 * 1024) throw new java.io.IOException("Native file exceeds inspection limit");
                try (InputStream in = zip.getInputStream(entry); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[65536]; int n;
                    while ((n = in.read(buf)) != -1) {
                        if ((long) bytes.size() + n > 96L * 1024 * 1024) throw new java.io.IOException("Native file exceeds inspection limit");
                        bytes.write(buf, 0, n);
                    }
                    java.util.List<String> deps = ElfDependencies.read(bytes.toByteArray());
                    nativeDeps.put(name, new JSONArray(deps)); dependencies.addAll(deps);
                }
            }
        }
        dependencies.removeAll(bundled);
        JSONArray xiaomi = new JSONArray();
        for (String name : dependencies) if (name.startsWith("libhyper_os_") || name.equals("libmisqlite3.so") || name.equals("librust_maml_sdk.so")) xiaomi.put(name);
        out.put("dex_files", dex); out.put("native_dependencies", nativeDeps);
        out.put("xiaomi_dependencies_not_bundled", xiaomi);
        out.put("note", "Static inspection only. Bundled or visible libraries do not prove runtime compatibility.");
        return out;
    }
}
