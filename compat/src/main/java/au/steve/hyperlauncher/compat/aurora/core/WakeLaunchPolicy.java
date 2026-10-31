package au.steve.hyperlauncher.compat.aurora.core;

/** At most one automatic screen per lock/wake cycle; never interrupts unlocked use. */
public final class WakeLaunchPolicy {
    private boolean launched, unlocked;
    public void screenOff() { launched = false; unlocked = false; }
    public void userPresent() { unlocked = true; }
    public boolean shouldLaunch(boolean enabled, boolean interactive, boolean locked,
                                boolean screenVisible, boolean editorVisible, boolean inCall) {
        return enabled && interactive && locked && !screenVisible && !editorVisible && !inCall && !launched && !unlocked;
    }
    public void launched() { launched = true; }
}
