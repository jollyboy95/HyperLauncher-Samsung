package au.steve.hyperlauncher.compat.aurora;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import au.steve.hyperlauncher.compat.aurora.core.DepthCompositor;
import au.steve.hyperlauncher.compat.aurora.core.LabelMask;
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
                if (out == null) callback.onFailure("No animal or person subject was found. You can still pick a PNG cutout.");
                else callback.onComplete(out, "Depth subject ready");
            } catch (Exception e) {
                callback.onFailure("Automatic depth unavailable: " + e.getMessage());
            }
        });
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

    private File segment(Uri wallpaper, Bitmap source, float threshold) throws Exception {
        if (source == null || source.getWidth() <= 0) return null;
        ensureInterpreter();
        Tensor input = interpreter.getInputTensor(0);
        int[] inShape = input.shape();
        int ih = inShape.length >= 3 ? inShape[1] : 257;
        int iw = inShape.length >= 4 ? inShape[2] : 257;
        Bitmap scaled = Bitmap.createScaledBitmap(source, iw, ih, true);
        Object inputBuffer = buildInput(input, scaled);
        Tensor output = interpreter.getOutputTensor(0);
        int[] outShape = output.shape();
        float[][][][] logits4 = null;
        ByteBuffer raw = null;
        Object feed;
        if (outShape.length == 4) {
            logits4 = new float[outShape[0]][outShape[1]][outShape[2]][outShape[3]];
            feed = logits4;
        } else {
            raw = ByteBuffer.allocateDirect(output.numBytes()).order(ByteOrder.nativeOrder());
            feed = raw;
        }
        interpreter.run(inputBuffer, feed);
        float[] flat;
        int height;
        int width;
        int classes;
        if (logits4 != null) {
            if (outShape[3] > outShape[1] && outShape[3] > 8) {
                height = outShape[1];
                width = outShape[2];
                classes = outShape[3];
                flat = flattenHwc(logits4[0], height, width, classes);
            } else {
                classes = outShape[1];
                height = outShape[2];
                width = outShape[3];
                flat = flattenChw(logits4[0], classes, height, width);
            }
        } else {
            raw.rewind();
            height = ih;
            width = iw;
            classes = Math.max(1, output.numBytes() / (4 * height * width));
            flat = new float[height * width * classes];
            raw.asFloatBuffer().get(flat);
        }
        if (scaled != source) scaled.recycle();
        float[] confidence = LabelMask.selectSubject(flat, height, width, classes);
        boolean any = false;
        for (float v : confidence) if (v > 0.2f) { any = true; break; }
        if (!any) return null;
        byte[] refined = MaskMath.refine(shiftThreshold(confidence, threshold), width, height);
        byte[] full = DepthCompositor.upsampleNearest(refined, width, height, source.getWidth(), source.getHeight());
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

    private static float[] shiftThreshold(float[] confidence, float threshold) {
        float[] out = new float[confidence.length];
        float t = Math.max(0.05f, Math.min(0.8f, threshold));
        for (int i = 0; i < confidence.length; i++) {
            out[i] = Math.max(0f, (confidence[i] - t) / Math.max(0.05f, 1f - t) + t);
        }
        return out;
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

    private static float[] flattenHwc(float[][][] src, int h, int w, int c) {
        float[] out = new float[h * w * c];
        int i = 0;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) for (int k = 0; k < c; k++) out[i++] = src[y][x][k];
        return out;
    }

    private static float[] flattenChw(float[][][] src, int c, int h, int w) {
        float[] out = new float[h * w * c];
        int i = 0;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) for (int k = 0; k < c; k++) out[i++] = src[k][y][x];
        return out;
    }

    private File maskFile(Uri wallpaper) {
        String name = "depth-" + Integer.toHexString(String.valueOf(wallpaper).hashCode()) + ".png";
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
