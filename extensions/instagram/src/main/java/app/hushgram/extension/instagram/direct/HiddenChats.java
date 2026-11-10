/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.direct;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.UnaryOperator;

import app.hushgram.extension.instagram.settings.FamilyNames;
import app.hushgram.extension.instagram.settings.Settings;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.L10n;
import app.hushgram.extension.shared.diagnostics.HookStatus;
import app.hushgram.extension.shared.settings.HushgramPause;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The chats "Lock your messages" hides, one at a time.
 *
 * <p>A hidden chat is left out of the list of chats the inbox draws its rows from ({@link #inbox}),
 * out of the chats each folder tab counts its unread ones from ({@link #folder}) and the ones the
 * unread badge reads ({@link #filter}), out of the inbox search's results and message matches ({@link #searchResults},
 * {@link #searchHits}), out of the recent searches it lists before you type ({@link #recents}), and a push for it isn't posted at all ({@link ChatLocks#track} asks {@link #hides}). The list is
 * {@link Settings#HIDDEN_CHATS}, kept in the same lines as the locked chats ({@link ChatList}): the
 * account a chat was hidden on, its thread id and the name it had there. The ids never leave the
 * phone. A chat's long-press menu hides or shows it ({@link #menuOpening}); HushGram settings also
 * add the chat you opened last and take a chat off the list, for the account in front
 * ({@link ChatAccounts}) and no other.
 *
 * <p>Each account's inbox, search and badge leave out the chats hidden on that account. A push is
 * dropped for a chat hidden on the account it was sent to. While no account is known (before the
 * first screen, or on one whose session can't be read) every account's hidden chats are left out,
 * and when another account comes to the front each inbox drawn is handed its list again for it.
 *
 * <p>A list with a chat on it is the switch. Paused, the setting answers an empty list, so every
 * hidden chat is back until HushGram is turned on again; the settings screen still shows what was
 * chosen ({@link #saved}).
 *
 * <p>A chat ticked or unticked in HushGram settings shows in the inbox as soon as you're back
 * ({@link #changed}): each inbox that drew its rows is handed Instagram's own list again without the
 * chats hidden now, and draws them again.
 *
 * <p>Nothing here throws into Instagram. A list that can't be read hides nothing, and a summary
 * whose id can't be read stays in the inbox.
 */
public final class HiddenChats {
    /** What is counted in the diagnostic report. */
    static final String LEFT_OUT = "hidden chats left out of the inbox";
    static final String LEFT_OUT_FOLDERS = "hidden chats left out of folder unread counts";
    static final String LEFT_OUT_BADGE = "hidden chats left out of the unread badge";
    static final String LEFT_OUT_SEARCH = "hidden chats left out of search";
    static final String LEFT_OUT_RECENTS = "hidden chats left out of recent searches";
    static final String SILENCED = "hidden chat notifications dropped";
    static final String REDRAWN = "inbox redrawn after hidden chats changed";

    /** Steps a failure is reported under. */
    static final String LIST = "hidden chats";
    static final String FILTER = "inbox filter";
    static final String REDRAW = "inbox redraw";
    static final String FOLDER_FILTER = "folder count filter";
    static final String BADGE_FILTER = "unread badge filter";
    static final String SEARCH = "inbox search filter";
    static final String RECENTS = "recent searches filter";

    /** Instagram's own names for a chat as the inbox search lists it, and for a chat whose messages matched. */
    private static final String SHARE_TARGET = "com.instagram.model.direct.DirectShareTarget";
    private static final String HIT_THREAD = "com.instagram.model.direct.DirectMessageSearchThread";
    private static final String HIT_MESSAGE = "com.instagram.model.direct.DirectMessageSearchMessage";

    /** Reads the thread id of one of the inbox's thread summaries. Tests put a fake in. */
    interface Reader {
        String threadId(Object summary);
    }

    static volatile Reader reader = HiddenChats::threadId;

    /** Reads the thread id of a chat in the inbox search's results. Tests put a fake in. */
    static volatile Reader searchReader = HiddenChats::targetThreadId;

    /** Hands an inbox holder a list and has it draw its rows from it. Tests put a fake in. */
    interface Redrawer {
        void redraw(Object holder, List<Object> summaries);
    }

    static volatile Redrawer redrawer = HiddenChats::redraw;

    /** True while the Messages inbox is showing only chats hidden for the active account. */
    private static volatile boolean hiddenOnlyInbox;
    /** The hide/show action appended to the chat menu that is currently opening. */
    private static volatile String menuButton;
    private static volatile boolean menuButtonHides;
    private static final String INBOX_ICON_TAG = "hushgram_hidden_inbox_icon";
    private static final String INBOX_TITLE_BOX = "action_bar_title_notifications_container";
    private static final String INBOX_TITLE = "igds_action_bar_title";
    private static final WeakHashMap<View, Boolean> watchedDecor = new WeakHashMap<>();
    private static final String MESSAGES_TAB = "direct_tab";
    private static final WeakHashMap<View, Boolean> unreadDotWatched = new WeakHashMap<>();
    private static final WeakHashMap<View, GradientDrawable> unreadDots = new WeakHashMap<>();
    private static final Map<String, Set<String>> unreadByAccount = new HashMap<>();
    /** Unread ids already present when a chat was hidden, or cleared when it was opened. */
    private static final Map<String, Set<String>> dismissedUnreadByAccount = new HashMap<>();
    /** Whether dismissed IDs have been restored from the phone's private settings. */
    private static boolean dismissedUnreadLoaded;
    private static volatile WeakReference<Activity> lastActivity = new WeakReference<>(null);
    private static long lastHiddenMessageHapticAt;

    private static final String[] HIDE_MENU_BUTTONS = {"HIDE_FROM_PRIMARY", "HIDE_FROM_MESSAGES"};
    private static final String[] SHOW_MENU_BUTTONS = {"SHOW_IN_PRIMARY", "SHOW_IN_MESSAGES"};

    private HiddenChats() {
    }

    /** Adds an available Instagram hide/show action to the chat's long-press menu. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List menuOpening(List buttons, Object menu) {
        menuButton = null;
        try {
            if (!Utils.settingsReady() || HushgramPause.isPaused() || buttons == null || buttons.isEmpty() || menu == null) {
                return buttons;
            }
            Object session = sessionIn(menu);
            String account = session == null ? null : InstagramChats.accountId(session);
            if (account == null || account.isEmpty()) return buttons;

            boolean show = hiddenOnly() && account.equals(ChatAccounts.active());
            Class<? extends Enum> type = (Class<? extends Enum>) buttons.get(0).getClass();
            Set<String> present = new java.util.HashSet<>();
            for (Object button : buttons) present.add(String.valueOf(button));
            for (String candidate : show ? SHOW_MENU_BUTTONS : HIDE_MENU_BUTTONS) {
                if (present.contains(candidate)) continue;
                List extended = new ArrayList(buttons);
                extended.add(Enum.valueOf(type, candidate));
                menuButton = candidate;
                menuButtonHides = !show;
                return extended;
            }
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, "hidden chat menu", t);
        }
        return buttons;
    }

    /** Handles the hide/show menu action. True keeps Instagram from handling that action. */
    public static boolean onMenuButton(Object session, Object button, Object key) {
        try {
            String selected = menuButton;
            if (selected == null || button == null || !selected.equals(String.valueOf(button)) || session == null || key == null) {
                return false;
            }
            String account = InstagramChats.accountId(session);
            String id = InstagramChats.threadId(key);
            if (account == null || account.isEmpty() || id == null || id.isEmpty()) return false;
            boolean hide = menuButtonHides;
            menuButton = null;
            boolean saved = hide ? add(account, id, "") : remove(account, id);
            if (!saved) return true;
            Utils.showToastShort(L10n.t(hide ? "Chat hidden" : "Chat shown again"));
            return true;
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, "hidden chat menu", t);
            return false;
        }
    }

    /** Keeps a lock icon beside Messages' title while the inbox is showing hidden chats. */
    public static void watchInboxIndicator(Activity activity) {
        try {
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
            View decor = activity.getWindow().getDecorView();
            synchronized (watchedDecor) {
                if (watchedDecor.put(decor, Boolean.TRUE) != null) return;
            }
            int boxId = activity.getResources().getIdentifier(INBOX_TITLE_BOX, "id", activity.getPackageName());
            int titleId = activity.getResources().getIdentifier(INBOX_TITLE, "id", activity.getPackageName());
            if (boxId == 0 || titleId == 0) return;
            decor.getViewTreeObserver().addOnPreDrawListener(() -> {
                if (hiddenOnlyInbox || hasInboxIndicator(decor, boxId)) syncInboxIndicator(decor, boxId, titleId);
                return true;
            });
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, "hidden inbox indicator", t);
        }
    }

    /** Watches the Messages tab for a red in-app dot for unread hidden chats. */
    public static void watchHiddenUnreadDot(Activity activity) {
        try {
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
            lastActivity = new WeakReference<>(activity);
            View decor = activity.getWindow().getDecorView();
            synchronized (unreadDotWatched) {
                if (unreadDotWatched.put(decor, Boolean.TRUE) != null) return;
            }
            int tabId = activity.getResources().getIdentifier(MESSAGES_TAB, "id", activity.getPackageName());
            if (tabId == 0) return;
            decor.getViewTreeObserver().addOnPreDrawListener(() -> {
                syncHiddenUnreadDot(decor, tabId);
                return true;
            });
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, "hidden unread dot", t);
        }
    }

    /** An unread snapshot includes only chats Instagram considers unread; keep its ids for this account. */
    private static void rememberUnread(String account, List<Object> summaries) {
        if (account == null || account.isEmpty()) return;
        loadDismissedUnread();
        Set<String> unread = new HashSet<>();
        if (summaries != null) {
            for (Object summary : summaries) {
                String id = idOf(summary);
                if (id != null) unread.add(id);
            }
        }
        synchronized (unreadByAccount) {
            unreadByAccount.put(account, unread);
            Set<String> dismissed = dismissedUnreadByAccount.get(account);
            if (dismissed != null) {
                // Keep the dismissal until a new message push clears it. Instagram can replay an
                // older unread snapshot after reporting the conversation as read; pruning the
                // dismissal from that snapshot made the dot return despite there being no new message.
                saveDismissedUnreadLocked();
            }
        }
        refreshHiddenUnreadDot();
    }

    /** Keeps unread hidden chat ids noticed on a dropped push until Instagram's next unread snapshot. */
    private static void rememberHiddenPush(Collection<String> pushIds, String account, Set<String> hidden) {
        if (account == null || account.isEmpty()) return;
        loadDismissedUnread();
        synchronized (unreadByAccount) {
            Set<String> unread = unreadByAccount.computeIfAbsent(account, ignored -> new HashSet<>());
            for (String id : pushIds) {
                if (hidden.contains(id)) {
                    unread.add(id);
                    Set<String> dismissed = dismissedUnreadByAccount.get(account);
                    if (dismissed != null) {
                        dismissed.remove(id);
                        if (dismissed.isEmpty()) dismissedUnreadByAccount.remove(account);
                    }
                }
            }
            saveDismissedUnreadLocked();
        }
        vibrateForHiddenMessageInApp();
        refreshHiddenUnreadDot();
    }

    /** Haptic-only feedback for a hidden message; never vibrates while Instagram is in the background. */
    private static void vibrateForHiddenMessageInApp() {
        if (!Utils.settingsReady() || !Settings.VIBRATE_HIDDEN_CHAT_MESSAGES.get()) return;
        Activity activity = Utils.getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        View decor = activity.getWindow().getDecorView();
        decor.post(() -> {
            try {
                if (activity.isFinishing() || activity.isDestroyed() || !activity.hasWindowFocus()) return;
                long now = SystemClock.elapsedRealtime();
                synchronized (HiddenChats.class) {
                    if (lastHiddenMessageHapticAt != 0L && now - lastHiddenMessageHapticAt < 1200L) return;
                    lastHiddenMessageHapticAt = now;
                }
                Vibrator vibrator = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        ? activity.getSystemService(VibratorManager.class).getDefaultVibrator()
                        : (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator == null || !vibrator.hasVibrator()) return;
                vibrator.vibrate(VibrationEffect.createOneShot(35L, VibrationEffect.DEFAULT_AMPLITUDE));
            } catch (Throwable t) {
                HookStatus.threw(FamilyNames.MESSAGES_LOCK, "hidden message haptic", t);
            }
        });
    }

    static boolean hasHiddenUnread() {
        loadDismissedUnread();
        String account = ChatAccounts.active();
        if (account == null || account.isEmpty()) return false;
        Set<String> hidden = idsFor(account);
        if (hidden.isEmpty()) return false;
        synchronized (unreadByAccount) {
            Set<String> unread = unreadByAccount.get(account);
            if (unread == null) return false;
            Set<String> dismissed = dismissedUnreadByAccount.get(account);
            for (String id : unread) {
                if (hidden.contains(id) && (dismissed == null || !dismissed.contains(id))) return true;
            }
        }
        return false;
    }

    /** Refreshes the tab dot on the UI thread after an unread snapshot or a hidden-chat change. */
    private static void refreshHiddenUnreadDot() {
        Activity activity = lastActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        View decor = activity.getWindow().getDecorView();
        decor.post(() -> {
            try {
                int tabId = activity.getResources().getIdentifier(MESSAGES_TAB, "id", activity.getPackageName());
                if (tabId != 0) syncHiddenUnreadDot(decor, tabId);
            } catch (Throwable t) {
                HookStatus.threw(FamilyNames.MESSAGES_LOCK, "hidden unread dot", t);
            }
        });
    }

    /** Uses the tab's overlay so its ordinary Instagram unread count remains intact. */
    private static void syncHiddenUnreadDot(View decor, int tabId) {
        try {
            View tab = decor.findViewById(tabId);
            if (tab == null || !tab.isShown()) return;
            GradientDrawable dot = unreadDots.get(tab);
            if (!hasHiddenUnread()) {
                if (dot != null) {
                    tab.getOverlay().remove(dot);
                    unreadDots.remove(tab);
                }
                return;
            }
            if (dot == null) {
                dot = new GradientDrawable();
                dot.setShape(GradientDrawable.OVAL);
                dot.setColor(Color.rgb(255, 48, 64));
                dot.setStroke(dp(tab, 1), Color.WHITE);
                unreadDots.put(tab, dot);
                tab.getOverlay().add(dot);
            }
            int size = dp(tab, 8);
            int x = tab.getWidth() / 2 + dp(tab, 9);
            int y = dp(tab, 4);
            if (x + size > tab.getWidth()) x = Math.max(0, tab.getWidth() - size - dp(tab, 4));
            dot.setBounds(x, y, x + size, y + size);
            tab.invalidate();
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, "hidden unread dot", t);
        }
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    private static boolean hasInboxIndicator(View decor, int boxId) {
        View box = decor.findViewById(boxId);
        return box instanceof ViewGroup && ((ViewGroup) box).findViewWithTag(INBOX_ICON_TAG) != null;
    }

    private static void syncInboxIndicator(View decor, int boxId, int titleId) {
        try {
            View box = decor.findViewById(boxId);
            if (!(box instanceof LinearLayout)) return;
            LinearLayout row = (LinearLayout) box;
            View existing = row.findViewWithTag(INBOX_ICON_TAG);
            if (!hiddenOnlyInbox) {
                if (existing != null) row.removeView(existing);
                return;
            }
            View title = row.findViewById(titleId);
            if (existing != null || !(title instanceof TextView)) return;
            TextView text = (TextView) title;
            ImageView icon = new ImageView(row.getContext());
            icon.setTag(INBOX_ICON_TAG);
            icon.setImageResource(android.R.drawable.ic_lock_lock);
            icon.setColorFilter(text.getCurrentTextColor(), PorterDuff.Mode.SRC_IN);
            icon.setContentDescription(L10n.t("Hidden inbox"));
            int size = Math.max(1, Math.round(text.getTextSize() * 1.1f));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.gravity = Gravity.CENTER_VERTICAL;
            params.setMarginStart(Math.round(text.getTextSize() * 0.3f));
            row.addView(icon, row.indexOfChild(title) + 1, params);
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, "hidden inbox indicator", t);
        }
    }

    /** Reads the session field the inbox menu object keeps, without naming Instagram's obfuscated class. */
    private static Object sessionIn(Object owner) {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!field.getType().getName().equals("com.instagram.common.session.UserSession")) continue;
                try {
                    field.setAccessible(true);
                    return field.get(owner);
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * The thread id of a thread summary, or null when it has none. The patch writes the body at
     * patch time, from the summary's own chat key getter and the key's thread id field, and a build
     * without it answers null, so no chat is hidden from the inbox.
     */
    public static String threadId(Object summary) {
        return null;
    }

    // ---------------------------------------------------------------- the inbox

    /**
     * Asked with the inbox's holder and the list of chats it is about to keep and turn into rows,
     * each time Instagram hands it one: the one read from the phone at start, a fresh page from the
     * server, a pull to refresh, a switch of folder tab. In the regular inbox, hidden chats are left
     * out. In hidden-only mode, every chat except the hidden ones is left out. Instagram's own list
     * is left as it was, so the inbox's state, its next page and what it saves to the phone still
     * have every chat.
     * The holder is remembered weakly with Instagram's own list, so a change to the hidden chats can
     * draw it again ({@link #changed}).
     */
    public static List<Object> inbox(Object holder, List<Object> summaries) {
        try {
            HookStatus.invoked(FamilyNames.MESSAGES_LOCK);
            Set<String> hidden = ids();
            List<String> pins = PinnedChats.ids();
            boolean onlyHidden = hiddenOnly();
            remember(holder, summaries, hidden, pins, onlyHidden);
            if (summaries == null || summaries.isEmpty()) return summaries;
            if (onlyHidden) return only(summaries, hidden);
            return PinnedChats.first(without(summaries, hidden, LEFT_OUT), pins);
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, FILTER, t);
            return summaries;
        }
    }

    /**
     * Asked with one folder tab's chats, which Instagram counts the tab's unread chats from (the
     * "1 unread message" on a folder pill). The same as {@link #inbox}, so a hidden chat's unread
     * messages don't count on any tab.
     */
    public static List<Object> folder(List<Object> summaries) {
        try {
            HookStatus.invoked(FamilyNames.MESSAGES_LOCK);
            if (summaries == null || summaries.isEmpty()) return summaries;
            return without(summaries, ids(), LEFT_OUT_FOLDERS);
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, FOLDER_FILTER, t);
            return summaries;
        }
    }

    /** [summaries] as they are when none is [hidden], and otherwise a copy without those, counted when [counted] isn't null. */
    private static List<Object> without(List<Object> summaries, Set<String> hidden, String counted) {
        if (hidden.isEmpty() || summaries.isEmpty()) return summaries;
        ArrayList<Object> shown = shown(summaries, hidden);
        if (shown.size() == summaries.size()) return summaries;
        if (counted != null) HookStatus.counted(FamilyNames.MESSAGES_LOCK, counted);
        return shown;
    }

    /**
     * Asked with the thread summaries Instagram's unread badge snapshot is about to read. A list
     * with no hidden chat in it comes back as it is. Otherwise a copy without the hidden ones does,
     * and Instagram's own list is left as it was.
     */
    public static ArrayList<Object> filter(ArrayList<Object> summaries) {
        try {
            HookStatus.invoked(FamilyNames.MESSAGES_LOCK);
            rememberUnread(ChatAccounts.active(), summaries);
            if (summaries == null || summaries.isEmpty()) return summaries;
            Set<String> hidden = ids();
            if (hidden.isEmpty()) return summaries;
            ArrayList<Object> shown = shown(summaries, hidden);
            if (shown.size() != summaries.size()) HookStatus.counted(FamilyNames.MESSAGES_LOCK, LEFT_OUT_BADGE);
            return shown;
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, BADGE_FILTER, t);
            return summaries;
        }
    }

    /** A new list of the summaries whose chats aren't hidden, in their order. */
    private static ArrayList<Object> shown(List<Object> summaries, Set<String> hidden) {
        ArrayList<Object> shown = new ArrayList<>(summaries.size());
        for (Object summary : summaries) {
            String id = idOf(summary);
            if (id == null || !hidden.contains(id)) shown.add(summary);
        }
        return shown;
    }

    /** The chats on the hidden list, in Instagram's order; rows without a readable id stay private. */
    private static List<Object> only(List<Object> summaries, Set<String> hidden) {
        if (hidden.isEmpty() || summaries.isEmpty()) return Collections.emptyList();
        ArrayList<Object> shown = new ArrayList<>(summaries.size());
        for (Object summary : summaries) {
            String id = idOf(summary);
            if (id != null && hidden.contains(id)) shown.add(summary);
        }
        return shown;
    }

    static String idOf(Object summary) {
        try {
            String id = reader.threadId(summary);
            return id == null || id.trim().isEmpty() ? null : id.trim();
        } catch (Throwable t) {
            // One summary that can't be read stays in the inbox; the rest are still filtered.
            return null;
        }
    }

    // ---------------------------------------------------------------- drawing the inbox again

    /**
     * Hands the inbox holder [holder] the list [summaries] and runs its row factory on it: the two
     * steps Instagram's own subscriber takes each time the inbox's state hands the holder a list. The
     * patch writes the body at patch time, from the holder's list field and its own starter, and a
     * build without it does nothing. The starter does nothing while its inbox is paused or gone, and
     * the inbox runs it again when it resumes, so the rows are drawn from this list then.
     */
    public static void redraw(Object holder, List<Object> summaries) {
    }

    /** An inbox holder Instagram handed a list, that list, and the chats that were hidden from it. */
    private static final class Drawn {
        final WeakReference<Object> holder;
        final List<Object> summaries;
        Set<String> hidden;
        List<String> pins;
        boolean onlyHidden;

        Drawn(Object holder, List<Object> summaries, Set<String> hidden, List<String> pins, boolean onlyHidden) {
            this.holder = new WeakReference<>(holder);
            this.summaries = summaries;
            this.hidden = hidden;
            this.pins = pins;
            this.onlyHidden = onlyHidden;
        }
    }

    /** The inbox holders Instagram handed a list, one entry each. A holder Instagram dropped goes the next time it's read. */
    private static final ArrayList<Drawn> drawn = new ArrayList<>();

    /**
     * Keeps [holder] with Instagram's own list [summaries] and the chats [hidden] from it, in place of
     * what it had. A null list forgets the holder. Nothing is kept while HushGram is paused or its
     * settings aren't ready, so nothing is drawn again then either.
     */
    private static void remember(Object holder, List<Object> summaries, Set<String> hidden, List<String> pins,
                                 boolean onlyHidden) {
        if (holder == null || !Utils.settingsReady() || HushgramPause.isPaused()) return;
        synchronized (drawn) {
            for (Iterator<Drawn> it = drawn.iterator(); it.hasNext(); ) {
                Object kept = it.next().holder.get();
                if (kept == null || kept == holder) it.remove();
            }
            if (summaries != null) drawn.add(new Drawn(holder, summaries, hidden, pins, onlyHidden));
        }
    }

    /** Switches the inbox filter and redraws the open inbox from Instagram's retained full chat list. */
    public static void toggleHiddenOnlyInbox() {
        if (!Utils.settingsReady() || HushgramPause.isPaused() || ChatAccounts.active() == null
                || !Settings.DOUBLE_TAP_HIDDEN_CHATS.get()) return;
        hiddenOnlyInbox = !hiddenOnlyInbox;
        changed();
    }

    /** Leaving Messages ends the temporary hidden-only view; returning to Messages starts on the usual inbox. */
    static void showRegularInbox() {
        if (!hiddenOnlyInbox) return;
        hiddenOnlyInbox = false;
        changed();
    }

    /** Whether a temporary hidden-only inbox is active, clearing it if its setting is no longer valid. */
    static boolean isShowingHiddenOnlyInbox() {
        return hiddenOnly();
    }

    /** Private-inbox mode is valid only for the signed-in account and while its switch is active. */
    private static boolean hiddenOnly() {
        if (!hiddenOnlyInbox) return false;
        if (!Utils.settingsReady() || HushgramPause.isPaused() || ChatAccounts.active() == null
                || !Settings.DOUBLE_TAP_HIDDEN_CHATS.get()) {
            hiddenOnlyInbox = false;
            return false;
        }
        return true;
    }

    /**
     * Asked after the hidden chats were saved. Each inbox holder Instagram still has, whose rows the
     * change affects, is handed its list again with the current inbox filter and draws its rows from
     * it ({@link #redraw}). Nothing runs while HushGram is paused or its
     * settings aren't ready, for a holder whose rows stay the same (nothing hidden before or after,
     * the same chats hidden, or a chat that isn't in its list), or for one Instagram dropped. No
     * listener or timer is left behind: this runs once for each change and is done.
     */
    static void changed() {
        try {
            if (!Utils.settingsReady() || HushgramPause.isPaused()) return;
            List<Object> holders = new ArrayList<>();
            List<List<Object>> lists = new ArrayList<>();
            synchronized (drawn) {
                if (drawn.isEmpty()) return;
                Set<String> now = ids();
                List<String> nowPins = PinnedChats.ids();
                boolean nowOnlyHidden = hiddenOnly();
                for (Iterator<Drawn> it = drawn.iterator(); it.hasNext(); ) {
                    Drawn one = it.next();
                    Object holder = one.holder.get();
                    if (holder == null) {
                        it.remove();
                        continue;
                    }
                    if (one.hidden.equals(now) && one.pins.equals(nowPins) && one.onlyHidden == nowOnlyHidden) continue;
                    List<Object> before = rows(one.summaries, one.hidden, one.pins, one.onlyHidden);
                    List<Object> after = rows(one.summaries, now, nowPins, nowOnlyHidden);
                    one.hidden = now;
                    one.pins = nowPins;
                    one.onlyHidden = nowOnlyHidden;
                    if (sameRows(before, after)) continue;
                    holders.add(holder);
                    lists.add(after);
                }
            }
            for (int i = 0; i < holders.size(); i++) {
                try {
                    redrawer.redraw(holders.get(i), lists.get(i));
                    HookStatus.counted(FamilyNames.MESSAGES_LOCK, REDRAWN);
                } catch (Throwable t) {
                    HookStatus.threw(FamilyNames.MESSAGES_LOCK, REDRAW, t);
                }
            }
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, REDRAW, t);
        }
    }

    private static List<Object> rows(List<Object> summaries, Set<String> hidden, List<String> pins,
                                     boolean onlyHidden) {
        return onlyHidden ? only(summaries, hidden) : PinnedChats.first(without(summaries, hidden, null), pins);
    }

    /** The same summaries in the same order. */
    private static boolean sameRows(List<Object> a, List<Object> b) {
        if (a == b) return true;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) return false;
        }
        return true;
    }

    /**
     * Asks {@link #changed} on the main thread, where Instagram hands the inbox its lists: at once
     * when it's asked there, as a screen coming to the front with another account is.
     */
    static void changedSoon() {
        Utils.runOnMainThreadNowOrLater(HiddenChats::changed);
    }

    /** Instagram dropped every inbox holder, as the garbage collector does once its screens are gone. */
    static void holdersDroppedForTests() {
        synchronized (drawn) {
            for (Drawn one : drawn) one.holder.clear();
        }
    }

    /** How many inbox holders are kept now. */
    static int keptForTests() {
        synchronized (drawn) {
            return drawn.size();
        }
    }

    // ---------------------------------------------------------------- search

    /**
     * The thread id of a chat in the inbox search's results, or null when it has none. The patch
     * writes the body at patch time, from Instagram's own result type, and a build without it
     * answers null, so no result is hidden. A result that is only a person has no thread, and stays.
     */
    public static String targetThreadId(Object result) {
        return null;
    }

    /**
     * Asked with the results Instagram's inbox search is about to turn into rows. A list with no
     * hidden chat in it comes back as it is, and otherwise a copy without the hidden ones does.
     * People who aren't a chat yet stay, so a hidden chat doesn't stop you finding its person.
     */
    public static List<Object> searchResults(List<Object> results) {
        try {
            HookStatus.invoked(FamilyNames.MESSAGES_LOCK);
            if (results == null || results.isEmpty()) return results;
            Set<String> hidden = ids();
            if (hidden.isEmpty()) return results;
            List<Object> shown = new ArrayList<>(results.size());
            for (Object result : results) {
                if (!inHiddenChat(result, hidden)) shown.add(result);
            }
            if (shown.size() == results.size()) return results;
            HookStatus.counted(FamilyNames.MESSAGES_LOCK, LEFT_OUT_SEARCH);
            return shown;
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, SEARCH, t);
            return results;
        }
    }

    /** The same for the messages the server found: the ones said in a hidden chat are left out. */
    public static ArrayList<Object> searchHits(ArrayList<Object> hits) {
        try {
            HookStatus.invoked(FamilyNames.MESSAGES_LOCK);
            if (hits == null || hits.isEmpty()) return hits;
            Set<String> hidden = ids();
            if (hidden.isEmpty()) return hits;
            ArrayList<Object> shown = new ArrayList<>(hits.size());
            for (Object hit : hits) {
                if (!inHiddenChat(hit, hidden)) shown.add(hit);
            }
            if (shown.size() == hits.size()) return hits;
            HookStatus.counted(FamilyNames.MESSAGES_LOCK, LEFT_OUT_SEARCH);
            return shown;
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, SEARCH, t);
            return hits;
        }
    }

    /**
     * Asked with the recent searches Instagram's inbox search lists before anything is typed. An
     * entry is either a chat or a wrapper holding one. A person or another kind of search has no
     * chat and stays. A list with no hidden chat in it comes back as it is, and otherwise a copy
     * without the hidden ones does.
     */
    public static List<Object> recents(List<Object> entries) {
        try {
            HookStatus.invoked(FamilyNames.MESSAGES_LOCK);
            if (entries == null || entries.isEmpty()) return entries;
            Set<String> hidden = ids();
            if (hidden.isEmpty()) return entries;
            List<Object> shown = new ArrayList<>(entries.size());
            for (Object entry : entries) {
                if (!recentInHiddenChat(entry, hidden)) shown.add(entry);
            }
            if (shown.size() == entries.size()) return entries;
            HookStatus.counted(FamilyNames.MESSAGES_LOCK, LEFT_OUT_RECENTS);
            return shown;
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, RECENTS, t);
            return entries;
        }
    }

    /**
     * The entry is a chat on the list, or holds one in a field of Instagram's chat type. The
     * wrapper's field name changes with each build and its type doesn't. An entry that can't be
     * read stays.
     */
    private static boolean recentInHiddenChat(Object entry, Set<String> hidden) {
        try {
            if (entry == null) return false;
            Object target = isShareTarget(entry.getClass()) ? entry : heldShareTarget(entry);
            if (target == null) return false;
            String id = searchReader.threadId(target);
            return id != null && hidden.contains(id.trim());
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isShareTarget(Class<?> type) {
        for (; type != null; type = type.getSuperclass()) {
            if (SHARE_TARGET.equals(type.getName())) return true;
        }
        return false;
    }

    private static Object heldShareTarget(Object entry) throws IllegalAccessException {
        for (Class<?> type = entry.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || !isShareTarget(field.getType())) continue;
                field.setAccessible(true);
                Object value = field.get(entry);
                if (value != null) return value;
            }
        }
        return null;
    }

    /** The result is a chat on the list, or a message said in one. A result that can't be read isn't. */
    private static boolean inHiddenChat(Object result, Set<String> hidden) {
        try {
            if (result == null) return false;
            for (Class<?> type = result.getClass(); type != null; type = type.getSuperclass()) {
                String name = type.getName();
                if (SHARE_TARGET.equals(name)) {
                    String id = searchReader.threadId(result);
                    return id != null && hidden.contains(id.trim());
                }
                if (HIT_THREAD.equals(name) || HIT_MESSAGE.equals(name)) return carriesAny(result, type, hidden);
            }
        } catch (Throwable t) {
            // One result that can't be read stays in the search; the rest are still filtered.
        }
        return false;
    }

    /**
     * A message match keeps its chat's thread id in one of its text fields, under a name that
     * changes with each Instagram build. A thread id is a long number nothing else in the match
     * looks like, so any text field holding a hidden chat's is the chat.
     */
    private static boolean carriesAny(Object hit, Class<?> type, Set<String> hidden) throws IllegalAccessException {
        for (java.lang.reflect.Field field : type.getDeclaredFields()) {
            if (field.getType() != String.class || java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            Object value = field.get(hit);
            if (value instanceof String && hidden.contains(((String) value).trim())) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- notifications

    /**
     * Some id a push carries (its deep link's chat id, its thread id or its thread IG id) is a chat
     * hidden on [account], the one the push was sent to, or on any account when that isn't known.
     * Counted when it is, so the diagnostic report shows pushes that were dropped.
     */
    static boolean hides(Collection<String> pushIds, @Nullable String account) {
        return hides(pushIds, account, true);
    }

    /** Drops a push for a hidden chat; only a confirmed message push may add the in-app unread dot. */
    static boolean hides(Collection<String> pushIds, @Nullable String account, boolean messagePush) {
        try {
            Set<String> hidden = idsFor(account);
            if (hidden.isEmpty()) return false;
            for (String id : pushIds) {
                if (hidden.contains(id)) {
                    HookStatus.counted(FamilyNames.MESSAGES_LOCK, SILENCED);
                    if (messagePush) rememberHiddenPush(pushIds, account, hidden);
                    return true;
                }
            }
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, LIST, t);
        }
        return false;
    }

    // ---------------------------------------------------------------- the list

    /**
     * The list as last parsed, with the text it came from, and the ids it hides for the account they
     * were last asked for, so a list's worth of rows parse it once.
     */
    private static final class Parsed {
        final String raw;
        final List<ChatList.Row> rows;
        @Nullable final String account;
        final Set<String> ids;

        Parsed(String raw, List<ChatList.Row> rows, @Nullable String account) {
            this.raw = raw;
            this.rows = rows;
            this.account = account;
            this.ids = Collections.unmodifiableSet(ChatList.ids(rows, account));
        }
    }

    private static volatile Parsed parsed;

    /** The thread ids hidden now for the account in front, or for every account while none is known. */
    private static Set<String> ids() {
        return idsFor(ChatAccounts.active());
    }

    /**
     * The thread ids hidden now for [account], or for every account when it's null, none while
     * HushGram is paused; parsed again only when the list or the account changed.
     */
    private static Set<String> idsFor(@Nullable String account) {
        if (!Utils.settingsReady()) return Collections.emptySet();
        String text = Settings.HIDDEN_CHATS.get();
        if (text.isEmpty()) return Collections.emptySet();
        Parsed kept = parsed;
        if (kept == null || !kept.raw.equals(text)) {
            kept = new Parsed(text, Collections.unmodifiableList(ChatList.parse(text)), account);
            parsed = kept;
        } else if (!Objects.equals(kept.account, account)) {
            kept = new Parsed(text, kept.rows, account);
            parsed = kept;
        }
        return kept.ids;
    }

    /** The chats hidden now on [account], oldest first: none while HushGram is paused or for an account that isn't known. */
    public static List<ChatLocks.Chat> chats(@Nullable String account) {
        return ChatList.shown(rows(false), account);
    }

    /** The same for the account in front. */
    public static List<ChatLocks.Chat> chats() {
        return chats(ChatAccounts.active());
    }

    /** The chats hidden on [account] as chosen, paused or not: none for an account that isn't known. The settings screen shows these. */
    public static List<ChatLocks.Chat> saved(@Nullable String account) {
        return ChatList.shown(rows(true), account);
    }

    /** The same for the account in front. */
    public static List<ChatLocks.Chat> saved() {
        return saved(ChatAccounts.active());
    }

    /** Usernames named by this account's hidden chats, for public search and suggestions only. */
    public static Set<String> searchUsernames() {
        try {
            if (!Utils.settingsReady() || !Settings.HIDE_HIDDEN_CHATS_FROM_SEARCH.get()) return Collections.emptySet();
            String account = ChatAccounts.active();
            if (empty(account)) return Collections.emptySet();
            Set<String> names = new HashSet<>();
            for (ChatList.Row row : rows(false)) {
                if (!row.protects(account) || row.name == null) continue;
                String name = row.name.trim();
                while (name.startsWith("@")) name = name.substring(1).trim();
                name = name.toLowerCase(Locale.ROOT);
                if (validUsername(name)) names.add(name);
            }
            return names.isEmpty() ? Collections.emptySet() : names;
        } catch (Throwable failure) {
            HookStatus.threw(FamilyNames.HIDDEN_CHAT_SEARCH, "hidden chat search names", failure);
            return Collections.emptySet();
        }
    }

    private static boolean validUsername(String name) {
        if (name.isEmpty() || name.length() > 30) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '_')) return false;
        }
        return true;
    }

    /** Every account's hidden chats, as they are now or, with [chosen], as chosen whether paused or not. */
    private static List<ChatList.Row> rows(boolean chosen) {
        try {
            if (Utils.settingsReady()) {
                return ChatList.parse(chosen ? Settings.HIDDEN_CHATS.savedValue() : Settings.HIDDEN_CHATS.get());
            }
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, LIST, t);
        }
        return new ArrayList<>();
    }

    /** The chat with [id] is hidden now on [account], or on any account when it's null. */
    public static boolean listed(@Nullable String account, String id) {
        return ChatList.protects(rows(false), account, id);
    }

    /** The same for the account in front, or any account while none is known. */
    public static boolean listed(String id) {
        return listed(ChatAccounts.active(), id);
    }

    /** The chat with [id] is on [account]'s chosen list, or any account's when it's null, paused or not. */
    public static boolean savedListed(@Nullable String account, String id) {
        return ChatList.protects(rows(true), account, id);
    }

    /** The last chat opened on [account], if it isn't hidden there. */
    public static ChatLocks.Chat lastOpened(@Nullable String account) {
        return ChatLocks.lastOpenedUnless(account, HiddenChats::savedListed);
    }

    /** The same for the account in front. */
    public static ChatLocks.Chat lastOpened() {
        return lastOpened(ChatAccounts.active());
    }

    /**
     * Hides a chat on [account], or renames it on that account's list, and the inbox leaves it out.
     * False, with nothing changed, for an account that isn't known, before the settings are read, or
     * when the list couldn't be saved.
     */
    public static boolean add(@Nullable String account, String id, String name) {
        if (empty(account) || empty(id)) return false;
        boolean saved = change(rows -> ChatList.added(rows, account, id, name));
        if (saved) {
            dismissUnread(account, id);
            changedSoon();
            refreshHiddenUnreadDot();
        }
        return saved;
    }

    /** The same for the account in front. */
    public static boolean add(String id, String name) {
        return add(ChatAccounts.active(), id, name);
    }

    /**
     * Brings a chat back on [account], in the inbox too, and on no other account. False, with the
     * chat still hidden, for an account that isn't known, before the settings are read, or when the
     * list couldn't be saved.
     */
    public static boolean remove(@Nullable String account, String id) {
        if (empty(account) || empty(id)) return false;
        boolean saved = change(rows -> ChatList.removed(rows, account, id));
        if (saved) {
            synchronized (unreadByAccount) {
                Set<String> dismissed = dismissedUnreadByAccount.get(account);
                if (dismissed != null) {
                    dismissed.remove(id);
                    if (dismissed.isEmpty()) dismissedUnreadByAccount.remove(account);
                    saveDismissedUnreadLocked();
                }
            }
            changedSoon();
            refreshHiddenUnreadDot();
        }
        return saved;
    }

    /** The same for the account in front. */
    public static boolean remove(String id) {
        return remove(ChatAccounts.active(), id);
    }

    /**
     * [account], signed in, found its chat [id] under [name]: a chat hidden on it takes the name, and
     * one hidden from before accounts were gets a line of its own for it ({@link ChatList#named}).
     */
    static void named(String account, String id, String name) {
        if (empty(account) || empty(id)) return;
        change(rows -> ChatList.named(rows, account, id, name));
    }

    /**
     * Saves the list [edit] makes of the one chosen, read fresh, paused or not. True when it's saved
     * or there was nothing to change. A list that can't be read or saved is left as it was.
     */
    private static boolean change(UnaryOperator<List<ChatList.Row>> edit) {
        try {
            if (!Utils.settingsReady()) return false;
            List<ChatList.Row> rows = ChatList.parse(Settings.HIDDEN_CHATS.savedValue());
            List<ChatList.Row> changed = edit.apply(rows);
            return changed == rows || Settings.HIDDEN_CHATS.save(ChatList.format(changed));
        } catch (Throwable t) {
            HookStatus.threw(FamilyNames.MESSAGES_LOCK, LIST, t);
            return false;
        }
    }

    /** Another account came to the front: the list is parsed again for it. */
    static void accountChanged() {
        parsed = null;
        hiddenOnlyInbox = false;
        refreshHiddenUnreadDot();
    }

    /** Opening a hidden chat clears its in-app dot without sending or changing Instagram read receipts. */
    static void opened(String account, String id) {
        if (empty(account) || empty(id)) return;
        dismissUnread(account, id);
        refreshHiddenUnreadDot();
    }

    private static void dismissUnread(String account, String id) {
        loadDismissedUnread();
        synchronized (unreadByAccount) {
            Set<String> unread = unreadByAccount.get(account);
            if (unread != null && unread.contains(id)) {
                dismissedUnreadByAccount.computeIfAbsent(account, ignored -> new HashSet<>()).add(id);
                saveDismissedUnreadLocked();
            }
        }
    }

    /** Restores per-account read acknowledgements so stale unread snapshots stay dismissed after restart. */
    private static void loadDismissedUnread() {
        synchronized (unreadByAccount) {
            if (dismissedUnreadLoaded) return;
            dismissedUnreadLoaded = true;
            try {
                JSONObject saved = new JSONObject(Settings.HIDDEN_CHAT_UNREAD_DISMISSED.savedValue());
                java.util.Iterator<String> accounts = saved.keys();
                while (accounts.hasNext()) {
                    String account = accounts.next();
                    JSONArray ids = saved.optJSONArray(account);
                    if (ids == null) continue;
                    Set<String> kept = new HashSet<>();
                    for (int i = 0; i < ids.length(); i++) {
                        String id = ids.optString(i, "");
                        if (!id.isEmpty()) kept.add(id);
                    }
                    if (!kept.isEmpty()) dismissedUnreadByAccount.put(account, kept);
                }
            } catch (Throwable ignored) {
                dismissedUnreadByAccount.clear();
            }
        }
    }

    /** Caller holds unreadByAccount. The value is device-local and excluded from settings exports. */
    private static void saveDismissedUnreadLocked() {
        try {
            JSONObject saved = new JSONObject();
            for (Map.Entry<String, Set<String>> entry : dismissedUnreadByAccount.entrySet()) {
                if (entry.getValue().isEmpty()) continue;
                JSONArray ids = new JSONArray();
                for (String id : entry.getValue()) ids.put(id);
                saved.put(entry.getKey(), ids);
            }
            Settings.HIDDEN_CHAT_UNREAD_DISMISSED.save(saved.toString());
        } catch (Throwable ignored) {
            // The in-memory state still works for this process if storage is unavailable.
        }
    }

    private static boolean empty(String text) {
        return text == null || text.trim().isEmpty();
    }

    /** Back to how a fresh start finds it. */
    static void resetForTests() {
        reader = HiddenChats::threadId;
        searchReader = HiddenChats::targetThreadId;
        redrawer = HiddenChats::redraw;
        parsed = null;
        hiddenOnlyInbox = false;
        synchronized (unreadByAccount) {
            unreadByAccount.clear();
            dismissedUnreadByAccount.clear();
            dismissedUnreadLoaded = false;
        }
        synchronized (drawn) {
            drawn.clear();
        }
    }
}
