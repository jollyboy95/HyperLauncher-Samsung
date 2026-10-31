package au.steve.hyperlauncher.compat.aurora;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import au.steve.hyperlauncher.compat.AuroraWakeActivity;
import au.steve.hyperlauncher.compat.aurora.core.WakeLaunchPolicy;

/** Optional user-enabled wake trigger. No nodes, screen text, screenshots or gestures are requested. */
public final class AuroraWakeService extends AccessibilityService {
    public static final String EXTRA_AUTO = "aurora.autoWake";
    public static final String ACTION_UNLOCKED = "au.steve.hyperlauncher.compat.SYSTEM_UNLOCKED";
    public static volatile boolean screenVisible, editorVisible;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final WakeLaunchPolicy policy = new WakeLaunchPolicy();
    private boolean registered;
    private final Runnable launch = this::launchIfLocked;
    private final BroadcastReceiver screenEvents = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                main.removeCallbacksAndMessages(null); policy.screenOff();
            } else if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                main.removeCallbacksAndMessages(null); policy.userPresent();
                sendBroadcast(new Intent(ACTION_UNLOCKED).setPackage(getPackageName()));
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) schedule();
        }
    };
    @Override public void onServiceConnected() {
        if (!registered) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF); filter.addAction(Intent.ACTION_USER_PRESENT);
            // These actions are protected system broadcasts; USER_PRESENT may come from System UI rather than UID 1000.
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenEvents, filter, Context.RECEIVER_EXPORTED);
            else registerReceiver(screenEvents, filter);
            registered = true;
        }
        status("Ready for next lock and wake"); schedule();
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && "com.android.systemui".contentEquals(event.getPackageName() == null ? "" : event.getPackageName())) schedule();
    }
    private void schedule() { main.removeCallbacks(launch); main.postDelayed(launch, 200); }
    private void launchIfLocked() {
        AuroraConfig config = new AuroraConfig(this);
        PowerManager power = getSystemService(PowerManager.class);
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        AudioManager audio = getSystemService(AudioManager.class);
        int mode = audio == null ? AudioManager.MODE_NORMAL : audio.getMode();
        boolean inCall = mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION || mode == AudioManager.MODE_RINGTONE;
        if (!policy.shouldLaunch(config.autoWakeEnabled(), power != null && power.isInteractive(),
                keyguard != null && keyguard.isKeyguardLocked(), screenVisible, editorVisible, inCall)) return;
        try {
            Intent intent = new Intent(this, AuroraWakeActivity.class).putExtra(EXTRA_AUTO, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent); policy.launched(); status("Opening automatically");
            main.postDelayed(() -> {
                if (!screenVisible) status("Launch not confirmed. Check automatic wake setup.");
            }, 1500);
        } catch (SecurityException | IllegalStateException blocked) { status("Android blocked automatic launch"); }
    }
    private void status(String value) { new AuroraConfig(this).prefs().edit().putString("autoWakeStatus", value).apply(); }
    public static boolean enabledInSystem(Context context) {
        AccessibilityManager manager = context.getSystemService(AccessibilityManager.class);
        if (manager == null) return false;
        ComponentName expected = new ComponentName(context, AuroraWakeService.class);
        for (AccessibilityServiceInfo service : manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK))
            if (service.getId() != null && expected.equals(ComponentName.unflattenFromString(service.getId()))) return true;
        return false;
    }
    @Override public void onInterrupt() { main.removeCallbacksAndMessages(null); }
    @Override public void onDestroy() {
        main.removeCallbacksAndMessages(null);
        if (registered) { unregisterReceiver(screenEvents); registered = false; }
        super.onDestroy();
    }
}
