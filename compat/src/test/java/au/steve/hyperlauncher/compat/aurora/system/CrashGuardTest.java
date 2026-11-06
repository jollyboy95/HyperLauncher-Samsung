package au.steve.hyperlauncher.compat.aurora.system;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class CrashGuardTest {
    static final class MapStore implements CrashGuard.Store {
        final Map<String, Object> m = new HashMap<>();
        public int getInt(String k, int d) { Object v = m.get(k); return v instanceof Integer ? (Integer) v : d; }
        public long getLong(String k, long d) { Object v = m.get(k); return v instanceof Long ? (Long) v : d; }
        public void putIntNow(String k, int v) { m.put(k, v); }
        public void putLongNow(String k, long v) { m.put(k, v); }
    }

    @Test public void stopsAfterTwoCrashesBeforeHealthy() {
        MapStore s = new MapStore();
        // Each "process" builds a new guard over the same persisted store, like System UI restarts.
        assertTrue(new CrashGuard(s).arm());   // start 1, crashes
        assertTrue(new CrashGuard(s).arm());   // start 2, crashes
        CrashGuard third = new CrashGuard(s);
        assertTrue(third.paused());
        assertFalse(third.arm());              // never shown again: no crash loop, no RescueParty reboot
        assertFalse(new CrashGuard(s).arm());
    }

    @Test public void healthyRunResetsCount() {
        MapStore s = new MapStore();
        CrashGuard g = new CrashGuard(s);
        assertTrue(g.arm());
        g.healthy();
        assertTrue(new CrashGuard(s).arm());
        assertTrue(new CrashGuard(s).arm());
        assertFalse(new CrashGuard(s).arm());
    }

    @Test public void retryTokenClearsPauseOnlyOnce() {
        MapStore s = new MapStore();
        new CrashGuard(s).arm(); new CrashGuard(s).arm();
        CrashGuard g = new CrashGuard(s);
        g.acceptRetryToken(5);
        assertFalse(g.paused());
        assertTrue(g.arm()); assertTrue(new CrashGuard(s).arm());
        CrashGuard again = new CrashGuard(s);
        again.acceptRetryToken(5); // same token must not clear the new pause
        assertTrue(again.paused());
    }
}
