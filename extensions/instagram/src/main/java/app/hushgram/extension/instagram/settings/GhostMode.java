/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import app.hushgram.extension.instagram.direct.ChatAccounts;
import app.hushgram.extension.shared.L10n;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.settings.BooleanSetting;
import app.hushgram.extension.shared.settings.Setting;

/**
 * One switch over the switches that keep what you do on Instagram to yourself. Its on/off choice is
 * stored by Instagram account; a selected feature reads that account's choice while its individual
 * switch values remain the shared fallback. Which ones it turns is your pick ({@link #picked}, #251),
 * all of them to start. Pause still wins.
 */
public final class GhostMode {
    private GhostMode() { }

    /**
     * The ones of [switches] Ghost mode turns: those {@link Settings#GHOST_MODE_PICKS} names, or all
     * of them when it names none of them.
     */
    static List<BooleanSetting> picked(List<BooleanSetting> switches) {
        Set<String> keys = pickedKeys();
        if (keys.isEmpty()) return switches;
        List<BooleanSetting> picked = new ArrayList<>();
        for (BooleanSetting setting : switches) {
            if (keys.contains(setting.key)) picked.add(setting);
        }
        return picked.isEmpty() ? switches : picked;
    }

    /** The keys {@link Settings#GHOST_MODE_PICKS} holds, as saved. */
    static Set<String> pickedKeys() {
        Set<String> keys = new LinkedHashSet<>();
        String saved = Settings.GHOST_MODE_PICKS.savedValue();
        if (saved == null) return keys;
        for (String key : saved.split(",")) {
            String trimmed = key.trim();
            if (!trimmed.isEmpty()) keys.add(trimmed);
        }
        return keys;
    }

    /**
     * Saves the ones of [switches] whose keys are in [keys] as what Ghost mode turns. All of them saves
     * nothing, the same as all, so a ghost patch added to the build later is turned too.
     */
    static void pick(List<BooleanSetting> switches, Set<String> keys) {
        StringBuilder saved = new StringBuilder();
        boolean every = true;
        for (BooleanSetting setting : switches) {
            if (!keys.contains(setting.key)) {
                every = false;
                continue;
            }
            if (saved.length() > 0) saved.append(',');
            saved.append(setting.key);
        }
        Settings.GHOST_MODE_PICKS.save(every ? "" : saved.toString());
    }

    /** The ghost switches in the order their families list, each one only when its patch is in the build. */
    static List<BooleanSetting> switches(Set<PatchFamily> build) {
        if (build == null) return Collections.emptyList();
        List<BooleanSetting> switches = new ArrayList<>();
        if (build.contains(PatchFamily.STORY_SEEN)) switches.add(Settings.VIEW_STORIES_ANONYMOUSLY);
        if (build.contains(PatchFamily.LIVE_SEEN)) switches.add(Settings.VIEW_LIVE_ANONYMOUSLY);
        if (build.contains(PatchFamily.THREAD_SEEN)) switches.add(Settings.READ_WITHOUT_SEEN_RECEIPT);
        if (build.contains(PatchFamily.DM_MEDIA_SEEN)) switches.add(Settings.VIEW_DM_MEDIA_ANONYMOUSLY);
        if (build.contains(PatchFamily.TYPING)) switches.add(Settings.HIDE_TYPING);
        if (build.contains(PatchFamily.ACTIVE_STATUS)) switches.add(Settings.HIDE_ACTIVE_STATUS);
        if (build.contains(PatchFamily.SCREENSHOT_REPORTS)) switches.add(Settings.HIDE_SCREENSHOTS);
        return switches;
    }

    /** Whether the master switch shows: only when it would turn at least two switches. */
    static boolean offered(List<BooleanSetting> switches) {
        return switches.size() >= 2;
    }

    /** The account's saved master choice, or the switches' current state before it has one. */
    static boolean on(List<BooleanSetting> switches) {
        if (Setting.isPaused()) return false;
        String account = ChatAccounts.active();
        Boolean saved = accountState(account);
        if (saved != null) return saved;
        return !switches.isEmpty() && all(switches, true);
    }

    /** Effective value for a selected Ghost mode switch on the account currently in front. */
    public static boolean enabled(BooleanSetting setting) {
        if (Setting.isPaused()) return false;
        if (!pickedKeys().contains(setting.key)) return setting.get();
        Boolean saved = accountState(ChatAccounts.active());
        return saved == null ? setting.get() : saved;
    }

    private static Boolean accountState(String account) {
        if (account == null || account.isEmpty()) return null;
        try {
            JSONObject states = new JSONObject(Settings.GHOST_MODE_ACCOUNT_STATES.savedValue());
            return states.has(account) ? states.getBoolean(account) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static boolean saveAccountState(String account, boolean on) {
        if (account == null || account.isEmpty()) return false;
        try {
            JSONObject states = new JSONObject(Settings.GHOST_MODE_ACCOUNT_STATES.savedValue());
            states.put(account, on);
            return Settings.GHOST_MODE_ACCOUNT_STATES.save(states.toString());
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean setForActiveAccount(boolean on) {
        String account = ChatAccounts.active();
        if (account == null) return false;
        if (!saveAccountState(account, on)) {
            Utils.showToastShort(L10n.t("Couldn't save Ghost mode for this account."));
        }
        return true;
    }

    /**
     * Saves the opposite of the effective account state, then refreshes the tab outline and toasts
     * which way it went. Without a known account, it keeps the older behavior of saving each switch.
     */
    static boolean flip(List<BooleanSetting> switches) {
        boolean on = !on(switches);
        String account = ChatAccounts.active();
        if (account == null) {
            for (BooleanSetting setting : switches) setting.save(on);
        } else if (!saveAccountState(account, on)) {
            Utils.showToastShort(L10n.t("Couldn't save Ghost mode for this account."));
            return !on;
        }
        NavigationSettings.refreshGhostTabIndicator();
        Utils.showToastShort(message(switches, on,
                L10n.t("Couldn't change every Ghost mode switch. Open Instagram settings to check them.")));
        return on;
    }

    /** What to tell the person after [switches] were saved as [on]: which way they went, or [failure]. */
    static String message(List<BooleanSetting> switches, boolean on, String failure) {
        return all(switches, on)
                ? on ? L10n.t("Ghost mode is on, and so is each of its switches.")
                        : L10n.t("Ghost mode is off, and so is each of its switches.")
                : failure;
    }

    /** Whether every one of [switches] is saved as [value]. */
    static boolean all(List<BooleanSetting> switches, boolean value) {
        for (BooleanSetting setting : switches) {
            if (enabled(setting) != value) return false;
        }
        return true;
    }
}
