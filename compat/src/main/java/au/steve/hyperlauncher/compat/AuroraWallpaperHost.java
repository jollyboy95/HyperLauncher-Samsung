package au.steve.hyperlauncher.compat;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import au.steve.hyperlauncher.compat.aurora.AuroraConfig;

/** Lifecycle-bound, muted looping video behind the shared clock renderer. */
public final class AuroraWallpaperHost extends FrameLayout implements TextureView.SurfaceTextureListener {
    public final AuroraSceneView scene;
    private final TextureView texture;
    private final AuroraConfig config;
    private MediaPlayer player;
    private Surface surface;
    private String loaded;
    private boolean active;

    public AuroraWallpaperHost(Context context, boolean preview) {
        super(context);
        setBackgroundColor(0xFF071018);
        config = new AuroraConfig(context);
        texture = new TextureView(context);
        texture.setSurfaceTextureListener(this);
        addView(texture, new FrameLayout.LayoutParams(-1, -1));
        scene = new AuroraSceneView(context, preview);
        addView(scene, new FrameLayout.LayoutParams(-1, -1));
    }

    public void resume() { active = true; reload(); }
    public void pause() { active = false; release(); }
    public void reload() {
        scene.reload();
        String uri = config.videoWallpaper();
        if (uri == null || !active || !texture.isAvailable()) { if (uri == null) release(); return; }
        if (uri.equals(loaded) && player != null) return;
        release();
        try {
            surface = new Surface(texture.getSurfaceTexture());
            player = new MediaPlayer();
            MediaPlayer current = player;
            current.setSurface(surface);
            current.setDataSource(getContext(), Uri.parse(uri));
            current.setLooping(true);
            current.setVolume(0, 0);
            current.setOnPreparedListener(p -> {
                if (player != p || !active) return;
                fit(p.getVideoWidth(), p.getVideoHeight());
                p.start();
                scene.setVideoReady(true);
            });
            current.setOnErrorListener((p, what, extra) -> { if (player == p) release(); return true; });
            loaded = uri;
            current.prepareAsync();
        } catch (Exception failure) { release(); }
    }

    private void fit(int videoWidth, int videoHeight) {
        int width = texture.getWidth(), height = texture.getHeight();
        if (width == 0 || height == 0 || videoWidth == 0 || videoHeight == 0) return;
        float scale = Math.max(width / (float) videoWidth, height / (float) videoHeight);
        Matrix matrix = new Matrix();
        matrix.setScale(videoWidth * scale / width, videoHeight * scale / height, width / 2f, height / 2f);
        texture.setTransform(matrix);
    }
    private void release() {
        if (player != null) { player.release(); player = null; }
        if (surface != null) { surface.release(); surface = null; }
        loaded = null;
        scene.setVideoReady(false);
    }
    @Override protected void onDetachedFromWindow() { pause(); super.onDetachedFromWindow(); }
    @Override public void onSurfaceTextureAvailable(SurfaceTexture s, int w, int h) { reload(); }
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture s, int w, int h) {
        if (player != null && scene.isVideoReady()) fit(player.getVideoWidth(), player.getVideoHeight());
    }
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture s) { release(); return true; }
    @Override public void onSurfaceTextureUpdated(SurfaceTexture s) {}
}
