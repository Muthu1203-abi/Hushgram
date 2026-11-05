/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.misc;

import android.app.Activity;
import android.view.View;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import app.hushgram.extension.instagram.settings.FamilyNames;
import app.hushgram.extension.instagram.settings.Settings;
import app.hushgram.extension.shared.Logger;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.diagnostics.HookStatus;

/**
 * Helper for the "Remove the empty space at the bottom" patch.
 *
 * <p>Each time Instagram lays out its window, it stores the height of the phone's navigation bar,
 * and its tab bar and screens leave that much room at the bottom. When the phone reports no
 * navigation bar there, Instagram asks the system whether the phone has one at all, and if it does,
 * stores the system's standard navigation bar height instead. On a phone whose gesture bar is
 * hidden, or in a pop-up window, that guess is the empty space under the tab bar. The patch passes
 * the guess through {@link #navigationBarHeight}.
 */
public final class BottomSpace {
    private static volatile boolean logged;
    private static final WeakHashMap<View, PageWatcher> WATCHERS = new WeakHashMap<>();

    private BottomSpace() {
    }

    /**
     * Injected right after Instagram reads the system's navigation bar height to stand in for one
     * the phone didn't report. Answers 0 while the switch is on, and the height otherwise, or when
     * anything goes wrong. Never throws, and never waits for the settings: before they're ready
     * Instagram's guess stays.
     */
    public static int navigationBarHeight(int height) {
        try {
            HookStatus.invoked(FamilyNames.BOTTOM_SPACE);
            if (Utils.settingsReady()) watchPageChanges(Utils.getActivity());
            if (height <= 0 || !Utils.settingsReady()
                    || !enabledForCurrentPage(Utils.getActivity())) return height;
            if (!logged) {
                logged = true;
                Logger.printDebug(() -> "Bottom space: left out a guessed navigation bar of " + height + " px");
            }
            return 0;
        } catch (Throwable failure) {
            HookStatus.threw(FamilyNames.BOTTOM_SPACE, "window insets", failure);
            return height;
        }
    }

    /** Uses Instagram's selected bottom tab; while a story or reel covers it, use that viewer. */
    private static boolean enabledForCurrentPage(Activity activity) {
        Page page = currentPage(activity);
        switch (page) {
            case STORIES: return Settings.REMOVE_BOTTOM_SPACE_STORIES.get();
            case FEED: return Settings.REMOVE_BOTTOM_SPACE_FEED.get();
            case MESSAGES: return Settings.REMOVE_BOTTOM_SPACE_MESSAGES.get();
            case EXPLORE: return Settings.REMOVE_BOTTOM_SPACE_EXPLORE.get();
            case REELS: return Settings.REMOVE_BOTTOM_SPACE_REELS.get();
            case PROFILE: return Settings.REMOVE_BOTTOM_SPACE_PROFILE.get();
            default: return false;
        }
    }

    private enum Page { UNKNOWN, STORIES, FEED, MESSAGES, EXPLORE, REELS, PROFILE }

    private static Page currentPage(Activity activity) {
        if (activity == null) return Page.UNKNOWN;
        View decor = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
        if (decor == null) return Page.UNKNOWN;
        // Instagram can leave a previous story viewer attached and visible in its view tree. Only
        // count the viewer that is actually on top, not any view whose visibility flag is VISIBLE.
        if (GlassTabBar.storyShownOnTop(decor) != null) return Page.STORIES;
        // A reel viewer can likewise remain attached offscreen while another tab is selected.
        View reel = find(activity, "clips_viewer_container");
        if (fills(reel, decor)) return Page.REELS;

        if (selected(activity, "feed_tab")) return Page.FEED;
        if (selected(activity, "direct_tab")) return Page.MESSAGES;
        if (selected(activity, "search_tab")) return Page.EXPLORE;
        if (selected(activity, "clips_tab")) return Page.REELS;
        if (selected(activity, "profile_tab")) return Page.PROFILE;
        return Page.UNKNOWN;
    }

    private static boolean fills(View view, View decor) {
        if (view == null || decor == null || !view.isShown()) return false;
        int width = decor.getWidth();
        int height = decor.getHeight();
        if (width <= 0 || height <= 0) return false;
        android.graphics.Rect visible = new android.graphics.Rect();
        if (!view.getGlobalVisibleRect(visible)) return false;
        return (long) visible.width() * visible.height() >= (long) width * height * 0.8f;
    }

    private static boolean selected(Activity activity, String name) {
        View tab = find(activity, name);
        return tab != null && tab.isShown() && tab.isSelected();
    }

    private static View find(Activity activity, String name) {
        int id = activity.getResources().getIdentifier(name, "id", activity.getPackageName());
        return id == 0 ? null : activity.findViewById(id);
    }

    /** Re-runs Instagram's insets listener when navigation changes the active page. */
    private static void watchPageChanges(Activity activity) {
        if (activity == null || activity.getWindow() == null) return;
        View decor = activity.getWindow().getDecorView();
        synchronized (WATCHERS) {
            if (WATCHERS.containsKey(decor)) return;
            PageWatcher watcher = new PageWatcher(activity, decor, currentPage(activity));
            decor.getViewTreeObserver().addOnGlobalLayoutListener(watcher);
            WATCHERS.put(decor, watcher);
        }
    }

    private static final class PageWatcher implements ViewTreeObserver.OnGlobalLayoutListener {
        private final WeakReference<Activity> activity;
        private final WeakReference<View> decor;
        private Page lastPage;

        PageWatcher(Activity activity, View decor, Page initialPage) {
            this.activity = new WeakReference<>(activity);
            this.decor = new WeakReference<>(decor);
            this.lastPage = initialPage;
        }

        @Override
        public void onGlobalLayout() {
            Activity owner = activity.get();
            View root = decor.get();
            if (owner == null || root == null || owner.isFinishing() || owner.isDestroyed()) return;
            Page page = currentPage(owner);
            if (page == lastPage) return;
            lastPage = page;
            root.post(root::requestApplyInsets);
        }
    }
}
