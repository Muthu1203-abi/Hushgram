/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.direct;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

import app.hushgram.extension.instagram.settings.FamilyNames;
import app.hushgram.extension.instagram.settings.Settings;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.diagnostics.HookStatus;

/**
 * Double-tap Messages for hidden chats (#358).
 *
 * <p>Every touch on an Instagram screen passes through {@code IgFragmentActivity.dispatchTouchEvent},
 * and the patch hands each one here before Instagram sees it. A tap on Messages' button, the tab
 * bar's {@link #DIRECT_TAB} or the {@link #HEADER_INBOX} at the top of Home, goes on to Instagram as
 * usual and opens Messages. A second tap close to it within the double-tap time is kept from
 * Instagram, down to its last event, since by then Messages is open and something else may sit under
 * the finger (the header button's spot is New message there). When that finger comes up, the inbox
 * switches between hidden chats only and the regular inbox, after the phone's lock while a messages
 * lock is on.
 *
 * <p>Anything else is only read: a scroll, a long press, a second finger or a slow second tap goes to
 * Instagram exactly as it was. Main thread only.
 */
public final class HiddenChatsTap {
    /** The id name of Messages' button on the tab bar. */
    static final String DIRECT_TAB = "direct_tab";

    /** The id name of Messages' button at the top of Home, on the bar layout that has one there. */
    static final String HEADER_INBOX = "action_bar_inbox_button";
    private static final String TAB_BAR = "tab_bar";

    /** Other main tabs: leaving Messages resets the temporary hidden-only inbox view. */
    private static final String[] OTHER_TABS = {"feed_tab", "clips_tab", "search_tab", "profile_tab", "activity_tab", "create_tab"};

    /** Whether a point of [activity]'s window is on one of Messages' buttons. */
    interface Buttons {
        boolean at(Activity activity, float x, float y);
    }

    static volatile Buttons buttons = HiddenChatsTap::onMessagesButton;
    static volatile Consumer<Activity> opener = HiddenChatsTap::open;

    private static WeakReference<Activity> tapActivity = new WeakReference<>(null);
    private static Application watchedApplication;
    private static Application.ActivityLifecycleCallbacks lifecycleCallbacks;
    private static int startedActivities;
    /** When the last tap on Messages' button came up, or {@link Long#MIN_VALUE} when there is none to pair. */
    private static long tapUp = Long.MIN_VALUE;
    private static float tapX;
    private static float tapY;

    private static boolean onButton;
    private static boolean moved;
    private static boolean swallowing;
    private static long downTime;
    private static float downX;
    private static float downY;

    private HiddenChatsTap() { }

    /** The hook, in IgFragmentActivity.dispatchTouchEvent. True keeps the event from Instagram. */
    public static boolean touch(Activity activity, MotionEvent event) {
        try {
            if (event == null || activity == null) return false;
            watchAppLifecycle(activity);
            HiddenChats.watchHiddenUnreadDot(activity);
            return decide(activity, event.getActionMasked(), event.getX(), event.getY(), event.getEventTime());
        } catch (Throwable failure) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, "hidden chats tap", failure);
            swallowing = false;
            forget();
            return false;
        }
    }

    static boolean decide(Activity activity, int action, float x, float y, long time) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                swallowing = false;
                onButton = false;
                moved = false;
                downTime = time;
                downX = x;
                downY = y;
                // A swipe can leave Messages without touching the tab bar. If Instagram has
                // already selected another tab, clear the temporary view before a swipe back
                // starts; the previous ACTION_UP also checks after Instagram handles it.
                if (HiddenChats.isShowingHiddenOnlyInbox() && anotherTabSelected(activity)) {
                    HiddenChats.showRegularInbox();
                    forget();
                }
                if (onOtherTab(activity, x, y)) {
                    HiddenChats.showRegularInbox();
                    forget();
                }
                if (!switchedOn()) {
                    forget();
                    return false;
                }
                HookStatus.bound(FamilyNames.MESSAGES_LOCK, "hidden chats tap");
                if (pairsWithLastTap(activity, x, y, time)) {
                    forget();
                    swallowing = true;
                    return true;
                }
                onButton = buttons.at(activity, x, y);
                return false;
            case MotionEvent.ACTION_MOVE:
                if (swallowing) return true;
                if (!moved && far(x - downX, y - downY, touchSlop(activity))) moved = true;
                return false;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (swallowing) return true;
                moved = true;
                return false;
            case MotionEvent.ACTION_UP:
                if (swallowing) {
                    swallowing = false;
                    if (switchedOn()) {
                        HookStatus.invoked(FamilyNames.MESSAGES_LOCK);
                        opener.accept(activity);
                    }
                    return true;
                }
                resetAfterTabSwipe(activity);
                if (onButton && !moved && time - downTime < ViewConfiguration.getLongPressTimeout()) {
                    tapActivity = new WeakReference<>(activity);
                    tapUp = time;
                    tapX = x;
                    tapY = y;
                } else {
                    forget();
                }
                return false;
            case MotionEvent.ACTION_CANCEL:
                forget();
                if (swallowing) {
                    swallowing = false;
                    return true;
                }
                return false;
            default:
                return swallowing;
        }
    }

    private static boolean pairsWithLastTap(Activity activity, float x, float y, long time) {
        if (tapUp == Long.MIN_VALUE || tapActivity.get() != activity) return false;
        long gap = time - tapUp;
        return gap >= 0 && gap <= ViewConfiguration.getDoubleTapTimeout()
                && !far(x - tapX, y - tapY, ViewConfiguration.get(activity).getScaledDoubleTapSlop());
    }

    private static void forget() {
        tapUp = Long.MIN_VALUE;
        tapActivity = new WeakReference<>(null);
    }

    private static boolean switchedOn() {
        return Utils.settingsReady() && Settings.DOUBLE_TAP_HIDDEN_CHATS.get();
    }

    /** Leaving Instagram clears the temporary hidden-only inbox before the next foreground visit. */
    private static void watchAppLifecycle(Activity activity) {
        Application app = activity.getApplication();
        if (app == null || app == watchedApplication) return;
        watchedApplication = app;
        lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(Activity started) {
                startedActivities++;
            }

            @Override public void onActivityStopped(Activity stopped) {
                startedActivities = Math.max(0, startedActivities - 1);
                if (startedActivities == 0 && !stopped.isChangingConfigurations()) {
                    HiddenChats.showRegularInbox();
                    forget();
                }
            }

            @Override public void onActivityCreated(Activity activity, Bundle state) { }
            @Override public void onActivityResumed(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        };
        app.registerActivityLifecycleCallbacks(lifecycleCallbacks);
    }

    private static int touchSlop(Activity activity) {
        return ViewConfiguration.get(activity).getScaledTouchSlop();
    }

    private static boolean far(float dx, float dy, int slop) {
        return dx * dx + dy * dy > (float) slop * slop;
    }

    /** Whether the point is on a shown Messages button, either one. */
    static boolean onMessagesButton(Activity activity, float x, float y) {
        return on(activity, DIRECT_TAB, x, y) || on(activity, HEADER_INBOX, x, y);
    }

    private static boolean onOtherTab(Activity activity, float x, float y) {
        if (on(activity, DIRECT_TAB, x, y)) return false;
        for (String tab : OTHER_TABS) if (on(activity, tab, x, y)) return true;
        // Some Instagram builds give the remaining tabs different resource names. A tap in the
        // tab bar that isn't on Messages still means the person is leaving the hidden inbox.
        return on(activity, TAB_BAR, x, y);
    }

    /** Checks the selected tab after Instagram has handled a horizontal swipe between pages. */
    private static void resetAfterTabSwipe(Activity activity) {
        if (!HiddenChats.isShowingHiddenOnlyInbox()) return;
        View decor = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
        if (decor == null) return;
        decor.post(() -> {
            try {
                if (!activity.isFinishing() && !activity.isDestroyed()
                        && HiddenChats.isShowingHiddenOnlyInbox() && anotherTabSelected(activity)) {
                    HiddenChats.showRegularInbox();
                    forget();
                }
            } catch (Throwable failure) {
                HookStatus.threw(FamilyNames.MESSAGES_LOCK, "hidden inbox tab swipe", failure);
            }
        });
    }

    /** True when Instagram marks a visible non-Messages tab as selected. */
    private static boolean anotherTabSelected(Activity activity) {
        for (String tab : OTHER_TABS) {
            int id = activity.getResources().getIdentifier(tab, "id", activity.getPackageName());
            if (id == 0) continue;
            View button = activity.findViewById(id);
            if (button != null && button.isShown() && button.isSelected()) return true;
        }
        return false;
    }

    private static boolean on(Activity activity, String name, float x, float y) {
        int id = activity.getResources().getIdentifier(name, "id", activity.getPackageName());
        if (id == 0) return false;
        View button = activity.findViewById(id);
        if (button == null || !button.isShown()) return false;
        int[] at = new int[2];
        button.getLocationInWindow(at);
        return x >= at[0] && x < at[0] + button.getWidth() && y >= at[1] && y < at[1] + button.getHeight();
    }

    /** Switches the inbox filter once the touch is through, asking the lock first while one is on. */
    private static void open(Activity activity) {
        activity.getWindow().getDecorView().post(() -> {
            try {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                MessagesLock.confirmHiddenThen(activity, () -> {
                    HiddenChats.toggleHiddenOnlyInbox();
                    HiddenChats.watchInboxIndicator(activity);
                });
            } catch (Throwable failure) {
                HookStatus.threw(FamilyNames.MESSAGES_LOCK, "hidden chats tap", failure);
            }
        });
    }

    static void resetForTests() {
        swallowing = false;
        onButton = false;
        moved = false;
        forget();
        buttons = HiddenChatsTap::onMessagesButton;
        opener = HiddenChatsTap::open;
        if (watchedApplication != null && lifecycleCallbacks != null) {
            watchedApplication.unregisterActivityLifecycleCallbacks(lifecycleCallbacks);
        }
        watchedApplication = null;
        lifecycleCallbacks = null;
        startedActivities = 0;
    }
}
