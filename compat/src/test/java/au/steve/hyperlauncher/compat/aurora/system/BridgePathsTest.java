package au.steve.hyperlauncher.compat.aurora.system;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import org.junit.Test;

public class BridgePathsTest {
    @Test public void bridgeUriKeepsMaskFileNameForRendererChecks() {
        String uri = BridgePaths.bridgeUri("autoForeground", "/data/user/0/x/files/depth/depth-v5-abc.png", 42);
        assertTrue(uri.startsWith("content://" + BridgePaths.AUTHORITY + "/file/autoForeground/"));
        // DepthEngine.isCurrentMask uses new File(value).getName()
        assertTrue(new File(uri).getName().startsWith("depth-v5-"));
    }

    @Test public void contentUrisAreSanitised() {
        assertEquals("image_3A1234", BridgePaths.fileName("content://media/external/images/media/image%3A1234"));
        assertEquals("image", BridgePaths.fileName(null));
    }

    @Test public void onlyImageKeysResolve() {
        assertEquals("wallpaper", BridgePaths.keyOf(Arrays.asList("file", "wallpaper", "1f", "a.jpg")));
        assertNull(BridgePaths.keyOf(Arrays.asList("file", "signature", "1", "x")));
        assertNull(BridgePaths.keyOf(Arrays.asList("prefs")));
    }

    @Test public void versionChangesWithTimestamp() {
        assertNotEquals(BridgePaths.versionOf("/a.png", 1), BridgePaths.versionOf("/a.png", 2));
        assertEquals(BridgePaths.versionOf("content://x", 0), BridgePaths.versionOf("content://x", 0));
    }
}
