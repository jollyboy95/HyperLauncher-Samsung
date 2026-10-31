package au.steve.hyperlauncher.compat;

import android.app.Notification;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import au.steve.hyperlauncher.compat.aurora.*;
import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class NotificationDismissalTest {
    private StatusBarNotification notification(Context context, int id, int flags) {
        Notification n = new Notification.Builder(context, "test").setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Message " + id).build();
        n.flags |= flags;
        return new StatusBarNotification(context.getPackageName(), context.getPackageName(), id, null,
                1, 1, 0, n, android.os.Process.myUserHandle(), id);
    }
    @Test public void serviceCancelsActualNotificationBeforeRemovingCard() {
        NotificationHub hub = NotificationHub.get(); hub.setConnected(false);
        var controller = Robolectric.buildService(AuroraNotificationService.class).create();
        try {
            var service = controller.get(); var shadow = Shadows.shadowOf(service);
            String key = shadow.addActiveNotification(notification(service, 1, 0));
            service.onListenerConnected(); assertEquals(1, hub.snapshot().size());
            assertEquals(NotificationHub.DismissResult.REQUESTED, hub.dismiss(hub.snapshot().get(0)));
            assertEquals(0, service.getActiveNotifications().length);
            assertEquals("Wait for framework confirmation; do not merely hide cards", 1, hub.snapshot().size());
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(500, java.util.concurrent.TimeUnit.MILLISECONDS);
            assertTrue(hub.snapshot().isEmpty());
        } finally { controller.destroy(); }
    }
    @Test public void clearAllIncludesCardsBeyondDisplayLimitAndRetainsProtectedNotifications() {
        NotificationHub hub = NotificationHub.get(); hub.setConnected(false);
        var controller = Robolectric.buildService(AuroraNotificationService.class).create();
        try {
            var service = controller.get(); var shadow = Shadows.shadowOf(service);
            for (int i=0;i<12;i++) shadow.addActiveNotification(notification(service,i,0));
            String protectedKey = shadow.addActiveNotification(notification(service,99,Notification.FLAG_NO_CLEAR));
            shadow.addActiveNotification(notification(service,100,Notification.FLAG_ONGOING_EVENT));
            service.onListenerConnected(); assertEquals(8, hub.snapshot().size());
            assertEquals(NotificationHub.DismissResult.REQUESTED, hub.clearAll());
            assertEquals(2, service.getActiveNotifications().length);
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(500, java.util.concurrent.TimeUnit.MILLISECONDS);
            assertEquals(1, hub.snapshot().size()); assertEquals(protectedKey,hub.snapshot().get(0).key);
            assertEquals(NotificationHub.DismissResult.NOT_CLEARABLE,hub.dismiss(hub.snapshot().get(0)));
        } finally { controller.destroy(); }
    }
    @Test public void disconnectedListenerReturnsVisibleFailureInsteadOfRemovingCards() {
        NotificationHub hub = NotificationHub.get(); hub.setConnected(false);
        hub.upsert(notification(RuntimeEnvironment.getApplication(),1,0), "Messages");
        assertEquals(NotificationHub.DismissResult.UNAVAILABLE,hub.dismiss(hub.snapshot().get(0)));
        assertEquals(NotificationHub.DismissResult.UNAVAILABLE,hub.clearAll());
        hub.setDismissAction(key -> { throw new SecurityException("Revoked"); });
        assertEquals(NotificationHub.DismissResult.UNAVAILABLE,hub.dismiss(hub.snapshot().get(0)));
        assertEquals(1,hub.snapshot().size()); hub.setConnected(false);
    }
    private void event(View view,long time,int action,float x,float y) {
        MotionEvent event=MotionEvent.obtain(0,time,action,x,y,0);view.dispatchTouchEvent(event);event.recycle();
    }
    @Test public void horizontalSwipeSurvivesDiagonalMovementInsideScrollableList() {
        Context context=RuntimeEnvironment.getApplication();
        NotificationScrollView scroll=new NotificationScrollView(context);
        LinearLayout list=new LinearLayout(context);list.setOrientation(LinearLayout.VERTICAL);
        TextView row=new TextView(context);row.setText("Notification");row.setClickable(true);
        int[] dismissals={0};row.setOnTouchListener(new NotificationSwipeListener(context,()->dismissals[0]++));
        list.addView(row,new LinearLayout.LayoutParams(360,120));
        list.addView(new View(context),new LinearLayout.LayoutParams(360,1000));scroll.addView(list);
        scroll.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY));
        scroll.layout(0,0,360,400);
        event(scroll,0,MotionEvent.ACTION_DOWN,20,30);
        event(scroll,20,MotionEvent.ACTION_MOVE,130,55);
        event(scroll,40,MotionEvent.ACTION_UP,220,65);
        assertEquals(1,dismissals[0]);assertEquals(0,row.getTranslationX(),0);assertEquals(1,row.getAlpha(),0);
    }
    @Test public void verticalDragAndCancelledSwipeDoNotDismissOrOpen() {
        Context context=RuntimeEnvironment.getApplication();View row=new View(context);row.layout(0,0,360,100);
        int[] cleared={0},opened={0};row.setOnClickListener(v->opened[0]++);
        row.setOnTouchListener(new NotificationSwipeListener(context,()->cleared[0]++));
        event(row,0,MotionEvent.ACTION_DOWN,20,20);event(row,20,MotionEvent.ACTION_MOVE,25,100);event(row,40,MotionEvent.ACTION_UP,25,120);
        event(row,50,MotionEvent.ACTION_DOWN,20,20);event(row,70,MotionEvent.ACTION_MOVE,180,25);event(row,90,MotionEvent.ACTION_CANCEL,200,25);
        assertEquals(0,cleared[0]);assertEquals(0,opened[0]);assertEquals(0,row.getTranslationX(),0);
    }
    private View findDescription(View view, String description, boolean prefix) {
        CharSequence text = view.getContentDescription();
        if (text != null && (prefix ? text.toString().startsWith(description) : text.toString().equals(description))) return view;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup)view;
            for (int i=0;i<group.getChildCount();i++) {
                View found = findDescription(group.getChildAt(i),description,prefix); if(found!=null)return found;
            }
        }
        return null;
    }
    @Test public void cardClearButtonAndClearAllReachNotificationService() {
        Context context=RuntimeEnvironment.getApplication();
        AuroraConfig config=new AuroraConfig(context);config.prefs().edit().clear().putBoolean("referenceStyleV1",true).commit();
        NotificationHub hub=NotificationHub.get();hub.setConnected(false);
        var service=Robolectric.buildService(AuroraNotificationService.class).create();
        try {
            var shadow=Shadows.shadowOf(service.get());
            shadow.addActiveNotification(notification(context,1,0));shadow.addActiveNotification(notification(context,2,0));
            service.get().onListenerConnected();
            try(var activity=Robolectric.buildActivity(AuroraActivity.class).setup().visible()) {
                View root=activity.get().getWindow().getDecorView();
                View clear=findDescription(root,"Clear notification from ",true);assertNotNull(clear);clear.performClick();
                assertEquals(1,service.get().getActiveNotifications().length);
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(500,java.util.concurrent.TimeUnit.MILLISECONDS);
                View clearAll=findDescription(root,"Clear all dismissible notifications",false);assertNotNull(clearAll);clearAll.performClick();
                assertEquals(0,service.get().getActiveNotifications().length);
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(500,java.util.concurrent.TimeUnit.MILLISECONDS);
                assertNull(findDescription(root,"Clear notification from ",true));
            }
        } finally {service.destroy();}
    }
    @Test public void ongoingUpdateCannotLeaveAnUndismissibleStaleCard() {
        Context context=RuntimeEnvironment.getApplication();NotificationHub hub=NotificationHub.get();hub.setConnected(false);
        StatusBarNotification initial=notification(context,1,0);hub.upsert(initial,"Messages");assertEquals(1,hub.snapshot().size());
        initial.getNotification().flags|=Notification.FLAG_ONGOING_EVENT;
        hub.upsert(initial,"Messages");assertTrue(hub.snapshot().isEmpty());
    }

    @Test public void swipeUpExpandsCollapsedStackWithoutOpeningOrDismissing() {
        Context context=RuntimeEnvironment.getApplication();
        AuroraConfig config=new AuroraConfig(context);
        config.prefs().edit().clear().putBoolean("referenceStyleV1",true)
                .putInt("notificationMode",1).commit();
        NotificationHub hub=NotificationHub.get();hub.setConnected(false);
        hub.upsert(notification(context,1,0),"Messages");hub.upsert(notification(context,2,0),"Messages");
        try(var activity=Robolectric.buildActivity(AuroraActivity.class).setup().visible()) {
            View root=activity.get().getWindow().getDecorView();
            View stack=findDescription(root,"Notification stack.",true);assertNotNull(stack);
            stack.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(160,View.MeasureSpec.EXACTLY));
            stack.layout(0,0,360,160);
            event(stack,0,MotionEvent.ACTION_DOWN,100,130);
            event(stack,20,MotionEvent.ACTION_MOVE,100,90);
            event(stack,40,MotionEvent.ACTION_UP,100,10);
            assertNull(findDescription(root,"Notification stack.",true));
            assertNull(stack.getParent());
            assertEquals(2,hub.snapshot().size());
        } finally {hub.setConnected(false);}
    }
    @Test public void cancelledStackDragDoesNotExpand() {
        Context context=RuntimeEnvironment.getApplication();
        NotificationScrollView scroll=new NotificationScrollView(context);
        scroll.addView(new TextView(context));scroll.layout(0,0,360,160);
        int[] expanded={0};scroll.setExpandStack(()->expanded[0]++);
        event(scroll,0,MotionEvent.ACTION_DOWN,100,130);
        event(scroll,20,MotionEvent.ACTION_MOVE,100,60);
        event(scroll,40,MotionEvent.ACTION_CANCEL,100,10);
        assertEquals(0,expanded[0]);
    }
    @Test public void hiddenAndServiceNotificationsAreRemovedButImportantSystemAlertsRemain() {
        Context context=RuntimeEnvironment.getApplication();NotificationHub hub=NotificationHub.get();hub.setConnected(false);
        int unspecified=android.service.notification.NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE;
        StatusBarNotification normal=notification(context,1,0);
        hub.upsert(normal,"Messages");assertEquals(1,hub.snapshot().size());
        hub.upsert(normal,"Messages",3,Notification.VISIBILITY_SECRET,false);assertTrue(hub.snapshot().isEmpty());
        hub.upsert(normal,"Messages",1,unspecified,false);assertTrue(hub.snapshot().isEmpty());
        hub.upsert(normal,"Messages",3,unspecified,true);assertTrue(hub.snapshot().isEmpty());
        hub.upsert(notification(context,2,Notification.FLAG_FOREGROUND_SERVICE),"Service");assertTrue(hub.snapshot().isEmpty());
        Notification n=new Notification.Builder(context,"system").setSmallIcon(android.R.drawable.ic_dialog_info)
                .setCategory(Notification.CATEGORY_SYSTEM).setContentTitle("System status").build();
        StatusBarNotification system=new StatusBarNotification("android","android",3,null,1,1,0,n,
                android.os.Process.myUserHandle(),3);
        hub.upsert(system,"Android System",2,unspecified,false);assertTrue(hub.snapshot().isEmpty());
        hub.upsert(system,"Android System",4,unspecified,false);assertEquals(1,hub.snapshot().size());
        n.category=Notification.CATEGORY_ALARM;n.flags|=Notification.FLAG_ONGOING_EVENT;
        hub.upsert(system,"Alarm",4,unspecified,false);assertEquals(1,hub.snapshot().size());
        hub.setConnected(false);
    }
    @Test public void systemUiCountNoticeIsHiddenWithoutHidingUrgentAlerts() {
        Context context=RuntimeEnvironment.getApplication();NotificationHub hub=NotificationHub.get();hub.setConnected(false);
        Notification n=new Notification.Builder(context,"system").setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("1 more notification").build();
        StatusBarNotification sbn=new StatusBarNotification("com.android.systemui","com.android.systemui",1,null,
                1,1,0,n,android.os.Process.myUserHandle(),1);
        int noOverride=android.service.notification.NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE;
        hub.upsert(sbn,"System UI");assertTrue(hub.snapshot().isEmpty());
        hub.upsert(sbn,"System UI",3,noOverride,false);assertTrue(hub.snapshot().isEmpty());
        hub.upsert(sbn,"System UI",4,noOverride,false);assertEquals(1,hub.snapshot().size());
        n.category=Notification.CATEGORY_ALARM;hub.upsert(sbn,"System UI",3,noOverride,false);
        assertEquals(1,hub.snapshot().size());hub.setConnected(false);
    }
}
