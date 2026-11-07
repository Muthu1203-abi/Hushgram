/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import android.app.Activity;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
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
import org.robolectric.shadows.ShadowLooper;
import app.hushgram.extension.shared.SettingsContextRule;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.settings.BaseSettings;
import app.hushgram.extension.shared.settings.PauseForTests;

/** About, Report problem and Support rows are intentionally absent from the settings list. */
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
    }

    private void open() throws Exception {
        controller = Robolectric.buildActivity(Activity.class).setup();
        page = new HushgramPreferenceFragment();
        controller.get().getFragmentManager().beginTransaction().add(android.R.id.content, page).commitNow();
        Utils.awaitBackgroundTasksForTests();
        ShadowLooper.idleMainLooper();
    }

    private Preference last() {
        PreferenceScreen screen = page.getPreferenceScreen();
        return screen.getPreference(screen.getPreferenceCount() - 1);
    }

    private void assertLastIsSupport() {
        Preference row = last();
        assertEquals("action_support_hushgram", row.getKey());
        assertEquals("Support HushGram", String.valueOf(row.getTitle()));
        assertNull("the row is its name alone", row.getSummary());
    }

    @Test public void itEndsTheHomeAsOneList() throws Exception {
        open();
        assertLastIsSupport();
    }

    @Test public void itEndsTheHomeWhenCategoriesArePages() throws Exception {
        Settings.CATEGORY_PAGES.save(true);
        open();
        assertLastIsSupport();
    }

    @Test public void aTapOpensKofiInABrowserTask() throws Exception {
        open();
        Preference row = last();
        assertTrue(row.getOnPreferenceClickListener().onPreferenceClick(row));
        Intent started = shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
        if (started == null) started = shadowOf(controller.get()).getNextStartedActivity();
        assertNotNull(started);
        assertEquals(Intent.ACTION_VIEW, started.getAction());
        assertEquals(Uri.parse("https://ko-fi.com/X8K126YVER"), started.getData());
        assertTrue(started.hasCategory(Intent.CATEGORY_BROWSABLE));
        assertTrue((started.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
    }

    @Test public void noBrowserShowsATipAndKeepsRunning() throws Exception {
        open();
        shadowOf(RuntimeEnvironment.getApplication()).checkActivities(true);
        Preference row = last();
        assertTrue(row.getOnPreferenceClickListener().onPreferenceClick(row));
        ShadowLooper.idleMainLooper();
        assertEquals("the sentence, then the address", 2, ShadowToast.shownToastCount());
        assertTrue(ShadowToast.showedToast("No app can open this link."));
        assertEquals("ko-fi.com/X8K126YVER", ShadowToast.getTextOfLatestToast());
    }

    /**
     * Source code and issues in About says the same and then shows its address on its own: after
     * the sentence, the 31 characters of the address ran to a third line, which a toast cuts off.
     */
    @Test public void sourceCodeWithNoBrowserShowsTheAddressOnItsOwn() throws Exception {
        open();
        shadowOf(RuntimeEnvironment.getApplication()).checkActivities(true);
        // About opens with a tap on its heading, as it does for a user.
        assertTrue(page.about.getOnPreferenceClickListener().onPreferenceClick(page.about));
        ShadowLooper.idleMainLooper();
        Preference source = titled(page.about, "Source code and issues");
        assertNotNull(source);
        assertTrue(source.getOnPreferenceClickListener().onPreferenceClick(source));
        ShadowLooper.idleMainLooper();
        assertEquals("the sentence, then the address", 2, ShadowToast.shownToastCount());
        assertTrue(ShadowToast.showedToast("No app can open this link."));
        assertEquals("github.com/SysAdminDoc/HushGram", ShadowToast.getTextOfLatestToast());
    }

    private static Preference titled(android.preference.PreferenceGroup group, String title) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference row = group.getPreference(i);
            if (title.equals(String.valueOf(row.getTitle()))) return row;
            if (row instanceof android.preference.PreferenceGroup) {
                Preference inside = titled((android.preference.PreferenceGroup) row, title);
                if (inside != null) return inside;
            }
        }
        return null;
    }

    @Test public void removedRowsStayAbsentInTheSettingsList() throws Exception {
        open();
        PreferenceScreen screen = page.getPreferenceScreen();
        assertNull(screen.findPreference("action_support_hushgram"));
        assertNull(screen.findPreference("action_report_problem"));
        assertRowsAbsent(screen);
    }

    /**
     * With no browser, Report a problem says so and then shows where the reports are, each in a
     * toast of its own: the address after the sentence ran to a third line, which a toast cuts off.
     */
    @Test public void reportAProblemWithNoBrowserShowsTheAddressOnItsOwn() throws Exception {
        open();
        shadowOf(RuntimeEnvironment.getApplication()).checkActivities(true);
        PreferenceScreen screen = page.getPreferenceScreen();
        Preference report = screen.getPreference(screen.getPreferenceCount() - 2);
        assertEquals(HushgramPreferenceFragment.REPORT_ROW_KEY, report.getKey());
        assertTrue(report.getOnPreferenceClickListener().onPreferenceClick(report));
        ShadowLooper.idleMainLooper();
        assertEquals("the sentence, then the address", 2, ShadowToast.shownToastCount());
        assertTrue(ShadowToast.showedToast("No app can open the bug report."));
        assertEquals("github.com/SysAdminDoc/HushGram/issues", ShadowToast.getTextOfLatestToast());
    }

    @Test public void aCategoryPageAndAMissSearchLeaveItOff() throws Exception {
        Settings.CATEGORY_PAGES.save(true);
        open();
        assertRowsAbsent(page.getPreferenceScreen());
    }
}
