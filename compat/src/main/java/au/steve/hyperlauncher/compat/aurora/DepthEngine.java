package au.steve.hyperlauncher.compat.aurora;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.net.Uri;
import au.steve.hyperlauncher.compat.aurora.core.DepthCompositor;
import au.steve.hyperlauncher.compat.aurora.core.LabelMask;
import au.steve.hyperlauncher.compat.aurora.core.SubjectRegion;
import au.steve.hyperlauncher.compat.aurora.core.MaskMath;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** On-device PASCAL VOC DeepLab segmentation. Class 12 is dog. */
public final class DepthEngine {
    public interface Callback {
        void onStatus(String message);
        void onComplete(File maskFile, String message);
        void onFailure(String message);
    }

    private static final String MODEL = "deeplabv3.tflite";
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "aurora-depth");
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    private final Context context;
    private Interpreter interpreter;
    private ByteBuffer modelBuffer;

    public DepthEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    public void segmentAsync(Uri wallpaper, Bitmap source, float threshold, Callback callback) {
        WORKER.execute(() -> {
            try {
                callback.onStatus("Identifying foreground subject…");
                File out = segment(wallpaper, source, threshold);
                if (out == null) callback.onFailure("No foreground subject was found. You can still pick a PNG cutout.");
                else callback.onComplete(out, "Depth subject ready");
            } catch (Exception | LinkageError e) {
                callback.onFailure("Automatic depth unavailable: " + e.getMessage());
            }
        });
    }

    /** Owns the sampled bitmap until inference completes. Callbacks run on the worker. */
    public void segmentWallpaperAsync(Uri wallpaper, float threshold, boolean force, Callback callback) {
        WORKER.execute(() -> {
            Bitmap source = null;
            try {
                File cached = cachedMask(wallpaper);
                if (cached != null && !force) {
                    callback.onComplete(cached, "Depth subject ready");
                    return;
                }
                callback.onStatus("Identifying foreground subject…");
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                try (InputStream in = context.getContentResolver().openInputStream(wallpaper)) {
                    BitmapFactory.decodeStream(in, null, options);
                }
                options.inSampleSize = 1;
                while (options.outWidth / options.inSampleSize > 1800
                        || options.outHeight / options.inSampleSize > 1800) options.inSampleSize *= 2;
                options.inJustDecodeBounds = false;
                try (InputStream in = context.getContentResolver().openInputStream(wallpaper)) {
                    source = BitmapFactory.decodeStream(in, null, options);
                }
                if (source == null) throw new IllegalStateException("Unable to read wallpaper");
                File out = segment(wallpaper, source, threshold);
                if (out == null) callback.onFailure("No foreground subject was found. Try a PNG cutout.");
                else callback.onComplete(out, "Depth subject ready");
            } catch (Exception | LinkageError e) {
                callback.onFailure("Automatic depth unavailable: " + e.getMessage());
            } finally {
                if (source != null) source.recycle();
            }
        });
    }

    public static boolean isCurrentMask(String path) {
        return path != null && new File(path).getName().startsWith("depth-v4-");
    }

    public File cachedMask(Uri wallpaper) {
        if (wallpaper == null) return null;
        File file = maskFile(wallpaper);
        return file.isFile() && file.length() > 0 ? file : null;
    }

    public synchronized void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }

    private synchronized File segment(Uri wallpaper, Bitmap source, float threshold) throws Exception {
        if (source == null || source.getWidth() <= 0) return null;
        ensureInterpreter();
        Tensor output = interpreter.getOutputTensor(0);
        int[] dimensions = LabelMask.hwcDimensions(output.shape());
        int height = dimensions[0], width = dimensions[1], classes = dimensions[2];
        float[] confidence = infer(source, height, width, classes);
        SubjectRegion region = SubjectRegion.locate(confidence, width, height, source.getWidth(), source.getHeight());
        byte[] full;
        if (region != null) {
            Bitmap crop = Bitmap.createBitmap(source, region.left, region.top, region.width(), region.height());
            float[] detailed;
            try {
                detailed = infer(crop, height, width, classes);
            } finally {
                if (crop != source) crop.recycle();
            }
            // Retain the coarse result if the focused pass cannot find a subject.
            boolean found = false;
            for (float value : detailed) if (value >= 0.5f) { found = true; break; }
            if (found) {
                float[] projected = region.project(detailed, width, height, source.getWidth(), source.getHeight());
                full = MaskMath.refine(projected, source.getWidth(), source.getHeight(), threshold);
            } else {
                full = DepthCompositor.upsampleNearest(MaskMath.refine(confidence, width, height, threshold),
                        width, height, source.getWidth(), source.getHeight());
            }
        } else {
            boolean found = false;
            for (float value : confidence) if (value >= 0.2f) { found = true; break; }
            if (!found) return null;
            full = DepthCompositor.upsampleNearest(MaskMath.refine(confidence, width, height, threshold),
                    width, height, source.getWidth(), source.getHeight());
        }
        int[] pixels = new int[source.getWidth() * source.getHeight()];
        source.getPixels(pixels, 0, source.getWidth(), 0, 0, source.getWidth(), source.getHeight());
        int[] composed = DepthCompositor.apply(pixels, full, source.getWidth(), source.getHeight());
        Bitmap mask = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        mask.setPixels(composed, 0, source.getWidth(), 0, 0, source.getWidth(), source.getHeight());
        File file = maskFile(wallpaper);
        try (FileOutputStream out = new FileOutputStream(file)) {
            if (!mask.compress(Bitmap.CompressFormat.PNG, 100, out)) throw new IllegalStateException("Unable to save mask");
        }
        mask.recycle();
        return file;
    }

    private float[] infer(Bitmap source, int height, int width, int classes) {
        Tensor input = interpreter.getInputTensor(0);
        int[] shape = input.shape();
        Bitmap scaled = Bitmap.createScaledBitmap(source, shape[2], shape[1], true);
        try {
            Object inputBuffer = buildInput(input, scaled);
            Tensor output = interpreter.getOutputTensor(0);
            if (output.dataType() != DataType.FLOAT32) throw new IllegalStateException("Expected float DeepLab output");
            ByteBuffer raw = ByteBuffer.allocateDirect(output.numBytes()).order(ByteOrder.nativeOrder());
            interpreter.run(inputBuffer, raw);
            raw.rewind();
            float[] flat = new float[height * width * classes];
            raw.asFloatBuffer().get(flat);
            return LabelMask.selectSubject(flat, height, width, classes);
        } finally {
            if (scaled != source) scaled.recycle();
        }
    }

    private Object buildInput(Tensor input, Bitmap scaled) {
        int[] shape = input.shape();
        int h = shape[1];
        int w = shape[2];
        if (input.dataType() == DataType.UINT8) {
            ByteBuffer buf = ByteBuffer.allocateDirect(h * w * 3).order(ByteOrder.nativeOrder());
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int p = scaled.getPixel(x, y);
                    buf.put((byte) Color.red(p));
                    buf.put((byte) Color.green(p));
                    buf.put((byte) Color.blue(p));
                }
            }
            buf.rewind();
            return buf;
        }
        ByteBuffer buf = ByteBuffer.allocateDirect(h * w * 3 * 4).order(ByteOrder.nativeOrder());
        float mean = 127.5f;
        float std = 127.5f;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = scaled.getPixel(x, y);
                buf.putFloat((Color.red(p) - mean) / std);
                buf.putFloat((Color.green(p) - mean) / std);
                buf.putFloat((Color.blue(p) - mean) / std);
            }
        }
        buf.rewind();
        return buf;
    }

    private File maskFile(Uri wallpaper) {
        String name = "depth-v4-" + Integer.toHexString(String.valueOf(wallpaper).hashCode()) + ".png";
        return new File(context.getFilesDir(), name);
    }

    private synchronized void ensureInterpreter() throws Exception {
        if (interpreter != null) return;
        File model = new File(context.getFilesDir(), MODEL);
        if (!model.isFile() || model.length() == 0) {
            try (InputStream in = context.getAssets().open(MODEL); FileOutputStream out = new FileOutputStream(model)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
        }
        byte[] bytes = Files.readAllBytes(model.toPath());
        modelBuffer = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
        modelBuffer.put(bytes).rewind();
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(2);
        interpreter = new Interpreter(modelBuffer, options);
    }
}
