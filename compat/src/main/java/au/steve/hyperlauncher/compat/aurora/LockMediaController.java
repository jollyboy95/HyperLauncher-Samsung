package au.steve.hyperlauncher.compat.aurora;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import java.util.List;

/** Uses real Android media sessions granted through notification-listener access. */
public final class LockMediaController {
    private final MediaSessionManager manager;
    private final ComponentName listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable changed;
    private MediaController controller;
    private boolean listening;
    private final MediaController.Callback callback = new MediaController.Callback() {
        @Override public void onMetadataChanged(MediaMetadata metadata) { changed.run(); }
        @Override public void onPlaybackStateChanged(PlaybackState state) { changed.run(); }
        @Override public void onSessionDestroyed() { refresh(); }
    };
    private final MediaSessionManager.OnActiveSessionsChangedListener sessions = this::choose;

    public LockMediaController(Context context, Runnable changed) {
        manager = context.getSystemService(MediaSessionManager.class);
        listener = new ComponentName(context, AuroraNotificationService.class);
        this.changed = changed;
    }

    public void start() {
        if (manager == null || listening) return;
        try {
            manager.addOnActiveSessionsChangedListener(sessions, listener, main);
            listening = true;
            refresh();
        } catch (SecurityException denied) { choose(null); }
    }

    public void stop() {
        if (manager != null && listening) manager.removeOnActiveSessionsChangedListener(sessions);
        listening = false;
        if (controller != null) controller.unregisterCallback(callback);
        controller = null;
    }

    private void refresh() {
        try { choose(manager == null ? null : manager.getActiveSessions(listener)); }
        catch (SecurityException denied) { choose(null); }
    }

    private void choose(List<MediaController> controllers) {
        MediaController next = null;
        if (controllers != null) for (MediaController candidate : controllers) {
            if (next == null) next = candidate;
            PlaybackState state = candidate.getPlaybackState();
            if (state != null && state.getState() == PlaybackState.STATE_PLAYING) { next = candidate; break; }
        }
        if (controller != null) controller.unregisterCallback(callback);
        controller = next;
        if (controller != null) controller.registerCallback(callback, main);
        changed.run();
    }

    public MediaMetadata metadata() { return controller == null ? null : controller.getMetadata(); }
    public PlaybackState playback() { return controller == null ? null : controller.getPlaybackState(); }
    public boolean hasSession() { return controller != null; }
    public void playPause() {
        if (controller == null) return;
        PlaybackState state = playback();
        if (state != null && state.getState() == PlaybackState.STATE_PLAYING) controller.getTransportControls().pause();
        else controller.getTransportControls().play();
    }
    public void previous() { if (controller != null) controller.getTransportControls().skipToPrevious(); }
    public void next() { if (controller != null) controller.getTransportControls().skipToNext(); }
    public void seek(long position) { if (controller != null) controller.getTransportControls().seekTo(position); }
}
