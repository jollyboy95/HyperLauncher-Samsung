package au.steve.hyperlauncher.compat.aurora;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Live playback only. A retained Android session is not evidence that music is playing. */
public final class LockMediaController {
    public static final long PAUSE_GRACE_MS = 30_000;
    private final MediaSessionManager manager;
    private final ComponentName listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable changed;
    private final Map<MediaSession.Token, Watcher> watched = new LinkedHashMap<>();
    private MediaController controller;
    private boolean listening;
    private final Runnable expire = this::selectVisible;
    private final MediaSessionManager.OnActiveSessionsChangedListener sessions = this::updateSessions;

    private final class Watcher {
        final MediaController media;
        int lastState;
        long pausedAt = -1;
        final MediaController.Callback callback = new MediaController.Callback() {
            @Override public void onMetadataChanged(MediaMetadata metadata) { selectVisible(); }
            @Override public void onPlaybackStateChanged(PlaybackState state) { selectVisible(); }
            @Override public void onSessionDestroyed() {
                media.unregisterCallback(this);
                watched.remove(media.getSessionToken());
                selectVisible();
            }
        };
        Watcher(MediaController media) {
            this.media = media;
            PlaybackState state = media.getPlaybackState();
            lastState = state == null ? PlaybackState.STATE_NONE : state.getState();
            media.registerCallback(callback, main);
        }
        int rank(long now) {
            PlaybackState state = media.getPlaybackState();
            int current = state == null ? PlaybackState.STATE_NONE : state.getState();
            if (current == PlaybackState.STATE_PAUSED && isLive(lastState)) pausedAt = now;
            if (current != PlaybackState.STATE_PAUSED) pausedAt = -1;
            lastState = current;
            if (isLive(current)) return 2;
            if (current == PlaybackState.STATE_PAUSED && pausedAt >= 0 && now - pausedAt < PAUSE_GRACE_MS) return 1;
            return 0;
        }
    }

    public LockMediaController(Context context, Runnable changed) {
        manager = context.getSystemService(MediaSessionManager.class);
        listener = new ComponentName(context, AuroraNotificationService.class);
        this.changed = changed;
    }
    public static boolean isLive(int state) {
        return state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
                || state == PlaybackState.STATE_CONNECTING || state == PlaybackState.STATE_FAST_FORWARDING
                || state == PlaybackState.STATE_REWINDING || state == PlaybackState.STATE_SKIPPING_TO_NEXT
                || state == PlaybackState.STATE_SKIPPING_TO_PREVIOUS || state == PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM;
    }
    public void start() {
        if (manager == null || listening) return;
        try {
            manager.addOnActiveSessionsChangedListener(sessions, listener, main);
            listening = true;
            updateSessions(manager.getActiveSessions(listener));
        } catch (SecurityException denied) { updateSessions(null); }
    }
    public void stop() {
        main.removeCallbacks(expire);
        if (manager != null && listening) manager.removeOnActiveSessionsChangedListener(sessions);
        listening = false;
        for (Watcher watcher : watched.values()) watcher.media.unregisterCallback(watcher.callback);
        watched.clear();
        controller = null;
    }
    private void updateSessions(List<MediaController> controllers) {
        Map<MediaSession.Token, MediaController> current = new LinkedHashMap<>();
        if (controllers != null) for (MediaController candidate : controllers) current.put(candidate.getSessionToken(), candidate);
        for (MediaSession.Token token : new ArrayList<>(watched.keySet())) {
            if (!current.containsKey(token)) {
                Watcher removed = watched.remove(token); removed.media.unregisterCallback(removed.callback);
            }
        }
        for (Map.Entry<MediaSession.Token, MediaController> entry : current.entrySet())
            if (!watched.containsKey(entry.getKey())) watched.put(entry.getKey(), new Watcher(entry.getValue()));
        selectVisible();
    }
    private void selectVisible() {
        main.removeCallbacks(expire);
        long now = SystemClock.elapsedRealtime();
        Watcher selected = null;
        int highest = 0;
        for (Watcher watcher : watched.values()) {
            int rank = watcher.rank(now);
            if (rank > highest) { highest = rank; selected = watcher; }
        }
        controller = selected == null ? null : selected.media;
        if (highest == 1) main.postDelayed(expire, Math.max(1, PAUSE_GRACE_MS - (now - selected.pausedAt)));
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
