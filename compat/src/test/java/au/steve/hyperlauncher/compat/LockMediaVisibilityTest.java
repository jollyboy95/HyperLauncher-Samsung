package au.steve.hyperlauncher.compat;

import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.*;
import android.view.View;
import android.widget.TextView;
import au.steve.hyperlauncher.compat.aurora.*;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class LockMediaVisibilityTest {
    private MediaSession session(String name,int state) {
        Context context=RuntimeEnvironment.getApplication();
        MediaSession session=new MediaSession(context,name);
        var shadow=Shadows.shadowOf(session.getController());
        shadow.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,name).build());
        shadow.setPlaybackState(playback(state));
        Shadows.shadowOf(context.getSystemService(MediaSessionManager.class)).addController(session.getController());
        return session;
    }
    private PlaybackState playback(int state) {
        return new PlaybackState.Builder().setState(state,0,0)
                .setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE).build();
    }
    private void state(MediaSession session,int state) {
        var shadow=Shadows.shadowOf(session.getController());
        PlaybackState value=playback(state);shadow.setPlaybackState(value);shadow.executeOnPlaybackStateChanged(value);
        ShadowLooper.idleMainLooper();
    }
    @Test public void retainedPausedAndStoppedSessionsDoNotShowPlayer() {
        var paused=session("Old paused song",PlaybackState.STATE_PAUSED);
        var stopped=session("Old stopped song",PlaybackState.STATE_STOPPED);
        LockMediaController media=new LockMediaController(RuntimeEnvironment.getApplication(),()->{});
        media.start();assertFalse(media.hasSession());assertNull(media.metadata());
        media.stop();paused.release();stopped.release();
    }
    @Test public void playingMediaHidesImmediatelyOnStopOrError() {
        var session=session("Live song",PlaybackState.STATE_PLAYING);
        LockMediaController media=new LockMediaController(RuntimeEnvironment.getApplication(),()->{});
        media.start();assertTrue(media.hasSession());
        state(session,PlaybackState.STATE_STOPPED);assertFalse(media.hasSession());
        state(session,PlaybackState.STATE_BUFFERING);assertTrue(media.hasSession());
        state(session,PlaybackState.STATE_ERROR);assertFalse(media.hasSession());
        media.stop();session.release();
    }
    @Test public void recentPauseExpiresWithoutAnotherNotificationOrActivityRefresh() {
        var session=session("Pause test",PlaybackState.STATE_PLAYING);int[] updates={0};
        LockMediaController media=new LockMediaController(RuntimeEnvironment.getApplication(),()->updates[0]++);
        media.start();state(session,PlaybackState.STATE_PAUSED);assertTrue(media.hasSession());
        int before=updates[0];
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(LockMediaController.PAUSE_GRACE_MS+1,TimeUnit.MILLISECONDS);
        assertFalse(media.hasSession());assertTrue("Expiry must notify the UI",updates[0]>before);
        state(session,PlaybackState.STATE_PLAYING);assertTrue(media.hasSession());
        media.stop();session.release();
    }
    @Test public void previouslyIdleSecondPlayerCanBecomeVisibleWithoutSessionListChange() {
        var first=session("Idle first player",PlaybackState.STATE_PAUSED);
        var second=session("Second player",PlaybackState.STATE_PAUSED);
        LockMediaController media=new LockMediaController(RuntimeEnvironment.getApplication(),()->{});
        media.start();assertFalse(media.hasSession());
        state(second,PlaybackState.STATE_PLAYING);assertTrue(media.hasSession());
        assertEquals("Second player",media.metadata().getString(MediaMetadata.METADATA_KEY_TITLE));
        state(first,PlaybackState.STATE_STOPPED);assertTrue(media.hasSession());
        media.stop();assertTrue(Shadows.shadowOf(first.getController()).getCallbacks().isEmpty());
        assertTrue(Shadows.shadowOf(second.getController()).getCallbacks().isEmpty());first.release();second.release();
    }
    @Test public void destroyedPlayerDisappearsImmediately() {
        var session=session("Destroyed player",PlaybackState.STATE_PLAYING);
        LockMediaController media=new LockMediaController(RuntimeEnvironment.getApplication(),()->{});
        media.start();assertTrue(media.hasSession());
        Shadows.shadowOf(session.getController()).executeOnSessionDestroyed();ShadowLooper.idleMainLooper();
        assertFalse(media.hasSession());media.stop();session.release();
    }
    private boolean containsText(View view,String text) {
        if(view instanceof TextView&&((TextView)view).getText().toString().contains(text))return true;
        if(view instanceof android.view.ViewGroup){var group=(android.view.ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)if(containsText(group.getChildAt(i),text))return true;}
        return false;
    }
    @Test public void actualLockScreenRemovesPlayerWhenPlaybackStops() {
        var context=RuntimeEnvironment.getApplication();
        new AuroraConfig(context).prefs().edit().clear().putBoolean("referenceStyleV1",true).commit();
        NotificationHub.get().setConnected(false);
        var session=session("Actual playing song",PlaybackState.STATE_PLAYING);
        try(var activity=Robolectric.buildActivity(AuroraActivity.class).setup().visible()) {
            View root=activity.get().getWindow().getDecorView();assertTrue(containsText(root,"Actual playing song"));
            state(session,PlaybackState.STATE_STOPPED);assertFalse(containsText(root,"Actual playing song"));
        }
        session.release();
    }
}
