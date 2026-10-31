package au.steve.hyperlauncher.compat.aurora;
import au.steve.hyperlauncher.compat.aurora.core.NotificationDisplay;
import org.junit.Test;
import static org.junit.Assert.*;
public class NotificationDisplayTest {
    @Test public void displayModesHaveDistinctCollapsedBehaviour() {
        assertEquals(5, NotificationDisplay.visibleCards(0, false, 5));
        assertEquals(1, NotificationDisplay.visibleCards(1, false, 5));
        assertEquals(0, NotificationDisplay.visibleCards(2, false, 5));
        assertEquals(5, NotificationDisplay.visibleCards(2, true, 5));
        assertFalse(NotificationDisplay.showCount(0, 5));
        assertTrue(NotificationDisplay.showCount(2, 1));
        assertFalse(NotificationDisplay.showCount(2, 0));
    }
    @Test public void footerDateAvoidsNotificationContentWithoutMovingClock() {
        assertEquals(1113.6f, NotificationDisplay.dateBaseline(1280, Float.MAX_VALUE), .1f);
        assertTrue(NotificationDisplay.dateBaseline(1280, 950) < 950 - 20);
        assertTrue(NotificationDisplay.dateBaseline(1280, 650) < 650 - 20);
    }
}
