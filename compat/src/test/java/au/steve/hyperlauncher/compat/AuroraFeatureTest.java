package au.steve.hyperlauncher.compat;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.TextView;
import au.steve.hyperlauncher.compat.aurora.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AuroraFeatureTest {
    private AuroraConfig config() {
        AuroraConfig config = new AuroraConfig(RuntimeEnvironment.getApplication());
        config.prefs().edit().clear().putBoolean("referenceStyleV1", true).commit();
        return config;
    }

    @Test public void newFeaturesPreserveExistingClockPosition() {
        AuroraConfig config = config();
        config.prefs().edit().putFloat("clockX", -.18f).putInt("clockY", 95)
                .putInt("clockSize", 190).putBoolean("clock12", true).commit();
        AuroraConfig loaded = new AuroraConfig(RuntimeEnvironment.getApplication());
        assertEquals(-.18f, loaded.clockX(), 0);
        assertEquals(95, loaded.clockY()); assertEquals(190, loaded.clockSize()); assertTrue(loaded.clock12());
        loaded.saveClockPosition(.1f, 50, 500);
        assertEquals(100, loaded.clockYForHeight(1000));
    }

    @Test public void savedDesignRoundTripsNumericTypesAndPreservesPrivacy() {
        AuroraConfig config = config(); SavedLooks store = new SavedLooks(config);
        config.prefs().edit().putFloat("clockX", 0f).putFloat("clockStretch", 1f)
                .putInt("clockColor", 0xFFFFFFFF).putInt("clockPreset", 7).putString("wallpaper", "content://kept")
                .putBoolean("hideNotificationContent", false).commit();
        store.save("My design");
        config.prefs().edit().putFloat("clockX", .3f).putBoolean("hideNotificationContent", true).commit();
        store.apply(store.list().get(0));
        assertEquals(0f, config.clockX(), 0); assertEquals(1f, config.clockStretch(), 0);
        assertEquals(0xFFFFFFFF, config.clockColor()); assertEquals(7, config.clockPreset());
        assertEquals("content://kept", config.wallpaper()); assertTrue(config.hideNotificationContent());
        AuroraConfig preview = SavedLooks.previewConfig(RuntimeEnvironment.getApplication(), store.list().get(0));
        preview.prefs().edit().putFloat("clockX", .2f).commit();
        assertEquals(0f, config.clockX(), 0);
        for (int i=0; i<12; i++) store.save("Design " + i);
        assertEquals(8, store.list().size());
    }

    @Test public void allTemplatesAndMaterialsRenderAcrossPhoneSizes() {
        AuroraConfig config = config();
        for (int preset=0; preset<13; preset++) {
            config.applyPreset(preset);
            for (int material=0; material<4; material++) {
                config.prefs().edit().putInt("clockMaterial", material).putFloat("clockStretch", 1.3f)
                        .putBoolean("showSeconds", true).commit();
                AuroraSceneView scene = new AuroraSceneView(RuntimeEnvironment.getApplication(), true);
                scene.layout(0, 0, 360, 780);
                Bitmap bitmap = Bitmap.createBitmap(360, 780, Bitmap.Config.ARGB_8888);
                scene.draw(new Canvas(bitmap)); bitmap.recycle();
            }
        }
    }

    @Test public void editorStartsAndReturnsWithNoVideoOrNotificationPermission() {
        config();
        try (var controller = Robolectric.buildActivity(AuroraSettingsActivity.class).setup().visible()) {
            controller.pause().stop().restart().start().resume();
            assertNotNull(controller.get().getWindow().getDecorView());
        }
    }

    @Test public void notificationsKeepRealOpenAndDismissActionsAndNewestFirst() {
        Context context = RuntimeEnvironment.getApplication();
        NotificationHub hub = NotificationHub.get(); hub.setConnected(false);
        PendingIntent open = PendingIntent.getActivity(context, 0, new Intent(context, AuroraSettingsActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(context, "test").setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Private title").setContentText("Private body").setContentIntent(open).build();
        StatusBarNotification first = new StatusBarNotification(context.getPackageName(), context.getPackageName(), 1,
                null, 1, 1, 0, notification, android.os.Process.myUserHandle(), 1);
        StatusBarNotification second = new StatusBarNotification(context.getPackageName(), context.getPackageName(), 2,
                null, 1, 1, 0, notification, android.os.Process.myUserHandle(), 2);
        hub.upsert(first, "Test"); hub.upsert(second, "Test");
        assertEquals(second.getKey(), hub.snapshot().get(0).key);
        assertEquals(open, hub.snapshot().get(0).contentIntent);
        String[] dismissed = {null}; hub.setDismissAction(key -> dismissed[0] = key);
        hub.dismiss(hub.snapshot().get(0)); assertEquals(second.getKey(), dismissed[0]);
        hub.setDismissAction(null); hub.setConnected(false);
    }
    private StatusBarNotification notification(Context context, int id) {
        Notification notification = new Notification.Builder(context, "test").setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Private title " + id).setContentText("Private body " + id).build();
        return new StatusBarNotification(context.getPackageName(), context.getPackageName(), id,
                null, 1, 1, 0, notification, android.os.Process.myUserHandle(), id);
    }
    private TextView findText(View view, String text) {
        if (view instanceof TextView && ((TextView)view).getText().toString().contains(text)) return (TextView)view;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup)view;
            for (int i=0; i<group.getChildCount(); i++) {
                TextView found = findText(group.getChildAt(i), text); if (found != null) return found;
            }
        }
        return null;
    }
    @Test public void lockedNotificationStackHidesContentThenRevealsAfterUnlock() {
        config(); Context context = RuntimeEnvironment.getApplication();
        var keyguard = Shadows.shadowOf(context.getSystemService(KeyguardManager.class));
        keyguard.setKeyguardLocked(true); keyguard.setIsDeviceLocked(true);
        NotificationHub hub = NotificationHub.get(); hub.setConnected(false);
        hub.upsert(notification(context,1), "Messages"); hub.upsert(notification(context,2), "Messages");
        try (var activity = Robolectric.buildActivity(AuroraActivity.class).setup().visible()) {
            View root = activity.get().getWindow().getDecorView();
            assertNull(findText(root, "Private body"));
            assertNotNull(findText(root, "Unlock to view"));
            TextView expand = findText(root, "2 notifications"); assertNotNull(expand); expand.performClick();
            assertNotNull(findText(root, "Collapse"));
            keyguard.setKeyguardLocked(false); keyguard.setIsDeviceLocked(false);
            activity.pause().stop().restart().start().resume();
            assertNotNull(findText(root, "Private body 2"));
        }
        hub.setConnected(false);
    }

    @Test public void countModeDoesNotRenderNotificationCardsUntilTapped() {
        AuroraConfig config = config(); config.prefs().edit().putInt("notificationMode", 2).commit();
        Context context = RuntimeEnvironment.getApplication(); NotificationHub hub = NotificationHub.get();
        hub.setConnected(false); hub.upsert(notification(context,1), "Messages");
        try (var activity = Robolectric.buildActivity(AuroraActivity.class).setup().visible()) {
            View root = activity.get().getWindow().getDecorView();
            assertNull(findText(root, "Private body"));
            TextView count = findText(root, "1 notification"); assertNotNull(count); count.performClick();
            assertNotNull(findText(root, "Private body 1"));
        }
        hub.setConnected(false);
    }

    @Test public void mediaUsesActiveSessionMetadataAndRealTransportControls() {
        config(); Context context = RuntimeEnvironment.getApplication();
        android.media.session.MediaSession session = new android.media.session.MediaSession(context, "test");
        var shadowController = Shadows.shadowOf(session.getController());
        shadowController.setMetadata(new android.media.MediaMetadata.Builder()
                .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, "Actual song").build());
        shadowController.setPlaybackState(new android.media.session.PlaybackState.Builder()
                .setState(android.media.session.PlaybackState.STATE_PAUSED,0,0)
                .setActions(android.media.session.PlaybackState.ACTION_PLAY).build());
        Shadows.shadowOf(context.getSystemService(android.media.session.MediaSessionManager.class)).addController(session.getController());
        LockMediaController media = new LockMediaController(context, () -> {});
        media.start(); assertTrue(media.hasSession());
        assertEquals("Actual song", media.metadata().getString(android.media.MediaMetadata.METADATA_KEY_TITLE));
        media.playPause(); org.robolectric.shadows.ShadowLooper.idleMainLooper();
        assertEquals(android.media.session.PlaybackState.ACTION_PLAY,
                Shadows.shadowOf(session.getController().getTransportControls()).getLastPerformedAction());
        media.seek(12000);
        assertEquals(12000, Shadows.shadowOf(session.getController().getTransportControls()).getSeekToPositionMs());
        media.stop(); assertTrue(shadowController.getCallbacks().isEmpty()); session.release();
    }

    @Test public void saveAndApplyDesignThroughEditorGallery() {
        config();
        try (var activity = Robolectric.buildActivity(AuroraSettingsActivity.class).setup().visible()) {
            View root = activity.get().getWindow().getDecorView();
            findText(root, "Designs").performClick(); findText(root, "Save current design").performClick();
            android.app.AlertDialog dialog = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            android.widget.EditText input = findEditText(dialog.getWindow().getDecorView());
            assertNotNull(input); input.setText("My first look");
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();
            org.robolectric.shadows.ShadowLooper.idleMainLooper();
            assertNotNull(findText(root, "My first look"));
            assertEquals(1, new SavedLooks(new AuroraConfig(activity.get())).list().size());
            findText(root, "Features").performClick();
            assertNotNull(findText(root, "Music controls"));
            assertNotNull(findText(root, "Notification display"));
        }
    }
    private android.widget.EditText findEditText(View view) {
        if (view instanceof android.widget.EditText) return (android.widget.EditText)view;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup)view;
            for (int i=0;i<group.getChildCount();i++) {
                android.widget.EditText found=findEditText(group.getChildAt(i)); if(found!=null)return found;
            }
        }
        return null;
    }

}
