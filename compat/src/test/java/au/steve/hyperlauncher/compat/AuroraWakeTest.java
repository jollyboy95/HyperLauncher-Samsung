package au.steve.hyperlauncher.compat;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.app.KeyguardManager;
import android.view.accessibility.AccessibilityEvent;
import au.steve.hyperlauncher.compat.aurora.*;
import au.steve.hyperlauncher.compat.aurora.core.WakeLaunchPolicy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AuroraWakeTest {
    @Test public void wakePolicyIsOncePerLockedCycleAndSkipsCallsEditorsAndUnlockedUse() {
        WakeLaunchPolicy policy=new WakeLaunchPolicy();
        assertTrue(policy.shouldLaunch(true,true,true,false,false,false));
        assertFalse(policy.shouldLaunch(false,true,true,false,false,false));
        assertFalse(policy.shouldLaunch(true,false,true,false,false,false));
        assertFalse(policy.shouldLaunch(true,true,false,false,false,false));
        assertFalse(policy.shouldLaunch(true,true,true,true,false,false));
        assertFalse(policy.shouldLaunch(true,true,true,false,true,false));
        assertFalse(policy.shouldLaunch(true,true,true,false,false,true));
        policy.launched();assertFalse(policy.shouldLaunch(true,true,true,false,false,false));
        policy.screenOff();assertTrue(policy.shouldLaunch(true,true,true,false,false,false));
        policy.userPresent();assertFalse(policy.shouldLaunch(true,true,true,false,false,false));
    }
    private Context prepare(boolean enabled,boolean locked) {
        Context context=RuntimeEnvironment.getApplication();AuroraConfig config=new AuroraConfig(context);
        config.prefs().edit().clear().putBoolean("referenceStyleV1",true).putBoolean("autoWakeEnabled",enabled).commit();
        AuroraWakeService.screenVisible=false;AuroraWakeService.editorVisible=false;
        Shadows.shadowOf(context.getSystemService(PowerManager.class)).setIsInteractive(true);
        Shadows.shadowOf(context.getSystemService(KeyguardManager.class)).setKeyguardLocked(locked);
        return context;
    }
    private void idle() {Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(250,TimeUnit.MILLISECONDS);}
    @Test public void serviceLaunchesTransientLockScreenOnceWithoutOpeningAppFirst() {
        Context context=prepare(true,true);var controller=Robolectric.buildService(AuroraWakeService.class).create();
        try {
            controller.get().onServiceConnected();idle();
            Intent launch=Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
            assertNotNull(launch);assertEquals(AuroraWakeActivity.class.getName(),launch.getComponent().getClassName());
            assertTrue(launch.getBooleanExtra(AuroraWakeService.EXTRA_AUTO,false));
            context.sendBroadcast(new Intent(Intent.ACTION_SCREEN_ON));idle();
            assertNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity());
            context.sendBroadcast(new Intent(Intent.ACTION_SCREEN_OFF));idle();
            context.sendBroadcast(new Intent(Intent.ACTION_SCREEN_ON));idle();
            assertNotNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity());
        } finally {controller.destroy();}
    }
    @Test public void serviceDoesNotLaunchWhileUnlockedOrWhenDisabled() {
        prepare(true,false);var controller=Robolectric.buildService(AuroraWakeService.class).create();
        try {controller.get().onServiceConnected();idle();assertNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity());}
        finally {controller.destroy();}
    }
    @Test public void automaticActivityExitsWhenSystemAuthenticatesUser() {
        Context context=prepare(false,false);
        Intent launch=new Intent(context,AuroraWakeActivity.class).putExtra(AuroraWakeService.EXTRA_AUTO,true);
        try(var activity=Robolectric.buildActivity(AuroraWakeActivity.class,launch).setup().visible()) {
            context.sendBroadcast(new Intent(AuroraWakeService.ACTION_UNLOCKED).setPackage(context.getPackageName()));
            org.robolectric.shadows.ShadowLooper.idleMainLooper();assertTrue(activity.get().isFinishing());
        }
    }
    @Test public void serviceConfigurationCannotReadWindowsOrPerformGestures() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        try(var parser=context.getResources().getXml(au.steve.hyperlauncher.compat.R.xml.aurora_wake_service)) {
            while(parser.next()!=org.xmlpull.v1.XmlPullParser.START_TAG){}
            String android="http://schemas.android.com/apk/res/android";
            assertFalse(parser.getAttributeBooleanValue(android,"canRetrieveWindowContent",true));
            assertFalse(parser.getAttributeBooleanValue(android,"canPerformGestures",true));
            assertEquals("com.android.systemui",parser.getAttributeValue(android,"packageNames"));
        }
    }
    @Test @Config(sdk=28) public void legacyReceiverRemainsUsableWithSignatureProtection() {
        Context context=prepare(true,false);
        try(var activity=Robolectric.buildActivity(AuroraActivity.class).setup().visible()) {
            context.sendBroadcast(new Intent(AuroraWakeService.ACTION_UNLOCKED).setPackage(context.getPackageName()));
            org.robolectric.shadows.ShadowLooper.idleMainLooper();
            assertTrue(activity.get().isFinishing());
        }
    }

}
