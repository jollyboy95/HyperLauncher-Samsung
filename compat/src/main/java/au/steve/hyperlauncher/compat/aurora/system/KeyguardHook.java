package au.steve.hyperlauncher.compat.aurora.system;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed/Vector entry point (see assets/xposed_init). Runs inside com.android.systemui and
 * attaches the Aurora layer to Samsung's real keyguard. Samsung's bouncer, biometrics, lock
 * icon, notifications and shortcuts are untouched; only the stock clock/date is hidden.
 *
 * Everything is wrapped so a failure here can never crash System UI.
 */
public final class KeyguardHook implements IXposedHookLoadPackage {
    static final String SYSTEMUI = "com.android.systemui";
    /** Candidate keyguard root classes, newest first. */
    private static final String[] ROOTS = {
            "com.android.systemui.keyguard.ui.view.KeyguardRootView",
    };

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!SYSTEMUI.equals(lpparam.packageName)) return;
        if (lpparam.processName != null && !SYSTEMUI.equals(lpparam.processName)) return;
        try {
            for (String name : ROOTS) {
                Class<?> root = XposedHelpers.findClassIfExists(name, lpparam.classLoader);
                if (root == null) continue;
                XposedBridge.hookAllConstructors(root, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            KeyguardLayerController.attach((android.view.ViewGroup) param.thisObject);
                        } catch (Throwable t) {
                            XposedBridge.log("Aurora: attach failed: " + t);
                        }
                    }
                });
                XposedBridge.log("Aurora: hooked " + name);
                return;
            }
            XposedBridge.log("Aurora: no supported keyguard root class found; module inactive");
        } catch (Throwable t) {
            XposedBridge.log("Aurora: hook setup failed: " + t);
        }
    }
}
