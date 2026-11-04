/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import java.util.EnumSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import app.hushgram.extension.shared.SettingsContextRule;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.settings.BaseSettings;
import app.hushgram.extension.shared.settings.PauseForTests;

/** The About, Report a problem and Support Instagram entries stay out of the settings list and search. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 37})
@SuppressWarnings("deprecation")
public class SupportRowSettingsTest {
    @Rule public final SettingsContextRule settings = new SettingsContextRule();
    private ActivityController<Activity> controller;
    private HushgramPreferenceFragment page;

    @Before public void prepare() {
        RuntimeEnvironment.getApplication().getApplicationInfo().targetSdkVersion = 37;
        Settings.CATEGORY_PAGES.resetToDefault();
        BaseSettings.PAUSED.save(false);
        PauseForTests.resume();
        BaseSettings.SAFE_MODE.save(false);
        Settings.SIGN_IN_NOTICE_HIDDEN.save(true);
        PatchFamily.inBuildForTests = EnumSet.of(PatchFamily.HIDE_ADS, PatchFamily.FOLLOWING_FEED);
    }

    @After public void close() throws Exception {
        if (controller != null) controller.close();
        Utils.awaitBackgroundTasksForTests();
        PatchFamily.inBuildForTests = null;
        Settings.CATEGORY_PAGES.resetToDefault();
        Settings.SIGN_IN_NOTICE_HIDDEN.resetToDefault();
        shadowOf(RuntimeEnvironment.getApplication()).checkActivities(false);
        SettingsEntry.onClosedByUser();
    }

    private void open() throws Exception {
        controller = Robolectric.buildActivity(Activity.class).setup();
        page = new HushgramPreferenceFragment();
        controller.get().getFragmentManager().beginTransaction().add(android.R.id.content, page).commitNow();
        Utils.awaitBackgroundTasksForTests();
    }

    @Test public void removedEntriesStayOutOfBothLayoutsAndSearch() throws Exception {
        for (boolean categoryPages : new boolean[]{false, true}) {
            Settings.CATEGORY_PAGES.save(categoryPages);
            open();
            PreferenceScreen screen = page.getPreferenceScreen();
            assertAbsent(screen);
            assertNoAboutCategory(screen);

            for (String query : new String[]{"About", "Report a problem", "Support Instagram"}) {
                page.searchSettings(query);
                screen = page.getPreferenceScreen();
                assertAbsent(screen);
                assertNoAboutCategory(screen);
            }
            controller.close();
            controller = null;
        }
    }

    private static void assertAbsent(PreferenceScreen screen) {
        assertNull(screen.findPreference("action_report_problem"));
        assertNull(screen.findPreference("action_support_hushgram"));
    }

    private static void assertNoAboutCategory(PreferenceScreen screen) {
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference row = screen.getPreference(i);
            assertFalse("About must not be listed", "About".contentEquals(row.getTitle()));
        }
    }
}
