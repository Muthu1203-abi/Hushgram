/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.misc;

import android.app.Activity;
import android.view.View;

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
            if (height <= 0 || !Utils.settingsReady() || !Settings.REMOVE_BOTTOM_SPACE.get()
                    || !enabledForCurrentPage()) return height;
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
    private static boolean enabledForCurrentPage() {
        Activity activity = Utils.getActivity();
        if (activity == null) return false;
        View story = find(activity, "reel_viewer_root");
        if (story != null && story.isShown()) return Settings.REMOVE_BOTTOM_SPACE_STORIES.get();
        View reel = find(activity, "root_clips_layout");
        if (reel != null && reel.isShown()) return Settings.REMOVE_BOTTOM_SPACE_REELS.get();

        if (selected(activity, "feed_tab")) return Settings.REMOVE_BOTTOM_SPACE_FEED.get();
        if (selected(activity, "direct_tab")) return Settings.REMOVE_BOTTOM_SPACE_MESSAGES.get();
        if (selected(activity, "search_tab")) return Settings.REMOVE_BOTTOM_SPACE_EXPLORE.get();
        if (selected(activity, "clips_tab")) return Settings.REMOVE_BOTTOM_SPACE_REELS.get();
        if (selected(activity, "profile_tab")) return Settings.REMOVE_BOTTOM_SPACE_PROFILE.get();
        return false;
    }

    private static boolean selected(Activity activity, String name) {
        View tab = find(activity, name);
        return tab != null && tab.isShown() && tab.isSelected();
    }

    private static View find(Activity activity, String name) {
        int id = activity.getResources().getIdentifier(name, "id", activity.getPackageName());
        return id == 0 ? null : activity.findViewById(id);
    }
}
