/*
 * Copyright 2026 Hushfacebook contributors
 * https://github.com/SysAdminDoc/Hushfacebook
 *
 * Modified for HushGram (Instagram), 2026.
 */
package app.hushgram.extension.instagram.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.biometrics.BiometricManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.DialogPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.DocumentsContract;
import android.preference.TwoStatePreference;
import android.text.Layout;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.io.File;
import java.text.NumberFormat;
import java.lang.ref.WeakReference;
import java.text.Normalizer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import app.hushgram.extension.instagram.direct.CallBlockMode;
import app.hushgram.extension.instagram.direct.ChatAccounts;
import app.hushgram.extension.instagram.direct.ChatLocks;
import app.hushgram.extension.instagram.direct.CollectionLocks;
import app.hushgram.extension.instagram.direct.HiddenChats;
import app.hushgram.extension.instagram.direct.PinnedChats;
import app.hushgram.extension.instagram.direct.LockDelay;
import app.hushgram.extension.instagram.direct.MessageBox;
import app.hushgram.extension.instagram.direct.MessagesLock;
import app.hushgram.extension.instagram.direct.OnlineHistory;
import app.hushgram.extension.instagram.direct.PinLock;
import app.hushgram.extension.instagram.direct.UnlockWith;
import app.hushgram.extension.instagram.download.DownloadQuality;
import app.hushgram.extension.instagram.media.PlaybackQuality;
import app.hushgram.extension.instagram.media.TapToPlayScope;
import app.hushgram.extension.instagram.media.ResumePlayback;
import app.hushgram.extension.instagram.feed.HiddenAccounts;
import app.hushgram.extension.instagram.feed.AmbientSpeed;
import app.hushgram.extension.instagram.feed.AmbientSpread;
import app.hushgram.extension.instagram.feed.CaptionLines;
import app.hushgram.extension.instagram.feed.CornerSize;
import app.hushgram.extension.instagram.feed.LikeAnimation;
import app.hushgram.extension.instagram.feed.MenuItems;
import app.hushgram.extension.instagram.feed.FeedSeekShape;
import app.hushgram.extension.instagram.feed.OverlayLook;
import app.hushgram.extension.instagram.feed.SeekShape;
import app.hushgram.extension.instagram.misc.CustomIcon;
import app.hushgram.extension.instagram.misc.HeaderPicture;
import app.hushgram.extension.instagram.misc.HeaderText;
import app.hushgram.extension.instagram.misc.MediaCache;
import app.hushgram.extension.instagram.misc.NotificationGroups;
import app.hushgram.extension.instagram.misc.SettingsRows;
import app.hushgram.extension.instagram.misc.SpoofLocation;
import app.hushgram.extension.instagram.share.ShareSheetActions;
import app.hushgram.extension.instagram.share.SharingDomain;
import app.hushgram.extension.instagram.stories.StoryBarThickness;
import app.hushgram.extension.instagram.stories.StorySeekBarPosition;
import app.hushgram.extension.instagram.stories.StoryPriority;
import app.hushgram.extension.instagram.stories.StoryEmoji;
import app.hushgram.extension.instagram.stories.StoryFonts;
import app.hushgram.extension.instagram.stories.StoryRingColor;
import app.hushgram.extension.instagram.stories.UploadBitrate;
import app.hushgram.extension.instagram.stories.UploadScope;
import app.hushgram.extension.instagram.stories.StoryRingSize;
import app.hushgram.extension.instagram.stories.StoryViewers;
import app.hushgram.extension.instagram.stories.StoryRingThickness;
import app.hushgram.extension.instagram.stories.StoryMentionsLook;
import app.hushgram.extension.instagram.stories.StoryTimeMode;
import app.hushgram.extension.instagram.stories.TrayBadge;
import app.hushgram.extension.instagram.profile.FollowerHistory;
import app.hushgram.extension.instagram.profile.FollowLabelPlace;
import app.hushgram.extension.instagram.profile.NotificationKinds;
import app.hushgram.extension.instagram.profile.ProfileTabs;
import app.hushgram.extension.instagram.misc.GlassCorners;
import app.hushgram.extension.instagram.misc.GlassHeight;
import app.hushgram.extension.instagram.misc.GlassLift;
import app.hushgram.extension.instagram.misc.GlassReelsHide;
import app.hushgram.extension.instagram.misc.TabIconSize;
import app.hushgram.extension.instagram.misc.AccentHue;
import app.hushgram.extension.instagram.misc.AppProxy;
import app.hushgram.extension.instagram.misc.CustomAccent;
import app.hushgram.extension.instagram.misc.MaterialYou;
import app.hushgram.extension.instagram.misc.AppIconChoice;
import app.hushgram.extension.instagram.misc.EmojiChoice;
import app.hushgram.extension.instagram.misc.EmojiFonts;
import app.hushgram.extension.instagram.misc.FontChoice;
import app.hushgram.extension.instagram.misc.Fonts;
import app.hushgram.extension.instagram.misc.HapticStyle;
import app.hushgram.extension.instagram.reels.BurnInMode;
import app.hushgram.extension.instagram.reels.BurnInWait;
import app.hushgram.extension.instagram.reels.CaptionLanguage;
import app.hushgram.extension.instagram.reels.CaptionWordReels;
import app.hushgram.extension.instagram.reels.CaptionWordStats;
import app.hushgram.extension.instagram.reels.HiddenBars;
import app.hushgram.extension.instagram.reels.HiddenSounds;
import app.hushgram.extension.instagram.reels.ReelTapChoice;
import app.hushgram.extension.instagram.reels.ScrollBreakTime;
import app.hushgram.extension.instagram.feed.HomeSwipe;
import app.hushgram.extension.instagram.reels.StartTab;
import app.hushgram.extension.instagram.reels.TabBarLayoutChoice;
import app.hushgram.extension.instagram.reels.TabOrder;
import app.hushgram.extension.instagram.download.DownloadHistory;
import app.hushgram.extension.instagram.profile.Unfollowers;
import app.hushgram.extension.instagram.profile.SentRequests;
import app.hushgram.extension.instagram.download.FileNameTemplate;
import app.hushgram.extension.instagram.download.PostDetails;
import app.hushgram.extension.instagram.download.HoldFingers;
import app.hushgram.extension.instagram.download.HoldTime;
import app.hushgram.extension.instagram.download.ReelSoundButton;
import app.hushgram.extension.instagram.download.SaveControl;
import app.hushgram.extension.instagram.download.SaveLeftovers;
import app.hushgram.extension.instagram.download.SaveFolder;
import app.hushgram.extension.instagram.download.SaveTree;
import app.hushgram.extension.instagram.download.ProfileHistory;
import app.hushgram.extension.instagram.download.SavedProfiles;
import app.hushgram.extension.instagram.download.ViewingHistory;
import app.hushgram.extension.shared.L10n;
import app.hushgram.extension.shared.Logger;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.settings.BaseSettings;
import app.hushgram.extension.shared.settings.BooleanSetting;
import app.hushgram.extension.shared.settings.HushgramPause;
import app.hushgram.extension.shared.settings.IntegerSetting;
import app.hushgram.extension.shared.settings.Setting;
import app.hushgram.extension.shared.settings.EnumSetting;
import app.hushgram.extension.shared.settings.StringSetting;
import app.hushgram.extension.shared.settings.preference.AbstractPreferenceFragment;
import app.hushgram.extension.shared.settings.preference.ClearLogBufferPreference;
import app.hushgram.extension.shared.settings.preference.ExportDiagnosticReportPreference;
import app.hushgram.extension.shared.settings.preference.ExportStatus;
import app.hushgram.extension.shared.settings.preference.ImmediateAction;
import app.hushgram.extension.shared.settings.preference.LogBufferManager;

/**
 * The preference list, built in code rather than from an XML resource so the bundle adds no
 * resources to Instagram. A switch appears only when its patch is in this build
 * ({@link PatchFamily}); a patch that works entirely at patch time gets a line saying so, and what
 * Pause can't reach is listed under the Pause switch. Switches are keyed by their setting, which is
 * how the shared fragment keeps them in sync with stored values. Every word is read from
 * {@link L10n} in the phone's language; the product names and the address stay as they are.
 */
@SuppressWarnings("deprecation")
public final class HushgramPreferenceFragment extends AbstractPreferenceFragment {
    /** The repository as a link, and as a person reads it. */
    static final String SOURCE_URL = "https://github.com/SysAdminDoc/HushGram";
    static final String SOURCE_ADDRESS = SOURCE_URL.substring(SOURCE_URL.indexOf("://") + 3);
    /** Where Support HushGram goes: the maintainer's Ko-fi page. */
    static final String SUPPORT_URL = "https://ko-fi.com/X8K126YVER";
    static final String SUPPORT_ADDRESS = SUPPORT_URL.substring(SUPPORT_URL.indexOf("://") + 3);
    /** The key of Support HushGram, the settings home's last row. */
    static final String SUPPORT_ROW_KEY = "action_support_hushgram";
    /** The row right above Support HushGram that opens a filled-in bug report on GitHub. */
    static final String REPORT_ROW_KEY = "action_report_problem";
    /** Lock these settings (#301), right under Pause HushGram. */
    static final String LOCK_ROW_KEY = "hushgram_settings_lock_row";
    /** The English of the row listing what Pause can't reach, and its key in {@link L10n}. */
    static final String STAYS_WHILE_PAUSED = "Stays in while paused";
    /** The Before you sign in notice's key, which finds it on the screen. */
    static final String SIGN_IN_NOTICE_KEY = "hushgram_sign_in_notice";
    static final String CLEAR_MEDIA_CACHE_NOW = "hushgram_clear_media_cache_now";
    static final String CLEAR_DOWNLOAD_HISTORY = "hushgram_clear_download_history";
    /** The row listing who left your Followers list, and the row that forgets them. */
    static final String UNFOLLOWERS_ROW = "hushgram_unfollowers_row";
    static final String FOLLOWER_HISTORY_ROW = "hushgram_follower_history_row";
    static final String VIEWING_HISTORY_ROW = "hushgram_viewing_history_row";
    static final String CLEAR_VIEWING_HISTORY = "hushgram_clear_viewing_history";
    static final String PROFILE_HISTORY_ROW = "hushgram_profile_history_row";
    static final String CLEAR_PROFILE_HISTORY = "hushgram_clear_profile_history";
    static final String ONLINE_HISTORY_ROW = "hushgram_online_history_row";
    static final String CLEAR_ONLINE_HISTORY = "hushgram_clear_online_history";
    static final String STORY_VIEWERS_ROW = "hushgram_story_viewers_row";
    static final String CLEAR_STORY_VIEWERS = "hushgram_clear_story_viewers";
    static final String SAVED_PROFILES_ROW = "hushgram_saved_profiles_row";
    static final String CLEAR_UNFOLLOWERS = "hushgram_clear_unfollowers";
    /** The row listing the follow requests still waiting, and the row that forgets them. */
    static final String SENT_REQUESTS_ROW = "hushgram_sent_requests_row";
    static final String CLEAR_SENT_REQUESTS = "hushgram_clear_sent_requests";
    /** The row that lists the chats locked one at a time. */
    static final String LOCKED_CHATS_ROW = "hushgram_locked_chats_row";
    /** The row that lists the Saved collections locked one at a time. */
    static final String LOCKED_COLLECTIONS_ROW = "hushgram_locked_collections_row";
    /** The row that lists the chats hidden one at a time. */
    static final String HIDDEN_CHATS_ROW = "hushgram_hidden_chats_row";
    /** The row that lists the chats pinned to the top of the inbox. */
    static final String PINNED_CHATS_ROW = "hushgram_pinned_chats_row";
    /** The row that sets or changes the HushGram PIN. */
    static final String CHANGE_PIN_ROW = "hushgram_change_pin_row";
    static final String HIDDEN_ACCOUNTS_ROW = "hushgram_hidden_accounts_row";
    static final String HIDDEN_SOUNDS_ROW = "hushgram_hidden_sounds_row";
    /** The row that lists the font files added for story text. */
    static final String STORY_FONTS_ROW = "hushgram_story_fonts_row";
    static final String GHOST_PICKS_ROW = "hushgram_ghost_picks_row";
    private static final String SCREEN_KEY = "hushgram_settings_root";
    /** Stable category keys. Keep the old prefix for saved restart and recreation state. */
    static final String CATEGORY_ROW_KEY = "hushgram_category_page_";

    /** The first row, which says whether HushGram runs now and whether the next start changes that. */
    @Nullable
    private Preference statusCard;
    private boolean resumingFromOverview;

    @Nullable Row clearPositions;
    private boolean changingPositions;
    private final Handler undoRefresh = new Handler(Looper.getMainLooper());
    private final Runnable refreshUndo = () -> {
        showClearPositions();
        showConfiguration();
    };
    private static final int EXPORT_CONFIGURATION = 0x4847;
    private static final int IMPORT_CONFIGURATION = 0x4848;
    private static final int PICK_FONT = 0x484d;
    private static final int PICK_STORY_FONT = 0x484e;
    private static final int PICK_STORY_EMOJI = 0x484f;
    private static final int PICK_SAVE_FOLDER = 0x4850;
    private static final int PICK_EMOJI = 0x4851;
    private static final int PICK_ICON_PICTURE = 0x4852;
    private static final int PICK_HEADER_PICTURE = 0x4853;
    /** Framework fragment callbacks run on the main thread. Never reuse a code across pages. */
    private static int documentSequence = PICK_FONT;
    private int documentRequest;
    private int documentCode;
    private long fontRequestRevision = -1;
    @Nullable private String configurationExportToken;
    private boolean changingConfiguration;
    /** Last operation's receipt lasts for this process, including closing/reopening settings. */
    @Nullable static volatile String importFeedback;
    @Nullable Row exportConfiguration;
    @Nullable ExportRow exportDiagnostics;
    private final Runnable exportChanges = this::showConfiguration;
    private final Runnable fontChanges = () -> {
        if (getView() == null || !isAdded()) return;
        updateUIToSettingValues();
        showConfiguration();
        showRestart();
    };
    @Nullable Row importConfiguration;
    @Nullable Row undoConfiguration;
    /** Lock these settings (#301), and the rows it greyed out while the settings are locked. */
    @Nullable Row settingsLock;
    private final Set<Preference> lockedRows = new HashSet<>();
    private boolean lockHolds, showingLock, refolding;
    private final Handler lockRefresh = new Handler(Looper.getMainLooper());
    private final Runnable lockTick = this::showLock;
    /** The About section, whose rows stay open while the settings are locked. */
    @Nullable PreferenceCategory about;
    /** Ghost mode's switch, and the switches it turns, both set once the page is built. */
    @Nullable SwitchPreference ghostMode;
    List<BooleanSetting> ghostSwitches = new ArrayList<>();
    /** The row under Ghost mode that picks which switches it turns (#251), and each switch's title by key. */
    @Nullable Row ghostPicks;
    private final Map<String, CharSequence> ghostNames = new HashMap<>();
    /** The row choosing the tab whose long press turns Ghost mode (#255). */
    @Nullable NavigationRow ghostTab;

    private String searchQuery = "";
    @Nullable private SearchRow search;
    @Nullable private Row noSearchResults;
    /** The home page's last row, which opens the Ko-fi page in a browser. */
    @Nullable private Row support;
    /** Report a problem, right above Support HushGram. */
    @Nullable private Row report;
    /** Keep the actual row objects, including their values and listeners, while filtering. */
    private final Map<PreferenceCategory, List<Preference>> searchableRows = new LinkedHashMap<>();
    private final Map<Preference, String> searchAliases = new HashMap<>();
    /** The order each section and row was built with, kept while a search has it further down. */
    private final Map<Preference, Integer> builtOrders = new HashMap<>();
    /** Past every section's and row's own order, and far short of the rows that close the page. */
    private static final int RANKED_LATER = 100_000;
    /** Stable category keys, retained while search temporarily reveals other settings. */
    private final Set<String> expandedCategories = new java.util.LinkedHashSet<>();
    /** The last expanded section, used to return to it after a restart. */
    @Nullable private String lastCategory;
    /** The category on its own page while Categories on their own page is on (#525), else null. */
    @Nullable private String pageCategory;
    /** Where the list of categories was when a page opened, for Back to put it there again. */
    private int overviewPosition = -1;
    private int overviewTop;
    /** The button at the top that switches between the list of categories and one long list. */
    @Nullable private Row viewToggle;
    /** Under it, while the categories are listed: switches them between rows and a grid of tiles (#494). */
    @Nullable private Row gridToggle;
    /** The tiles, one row of the page, while the grid is on and nothing is searched. */
    @Nullable private CategoryGrid categoryGrid;
    /**
     * The chips: one for each group over the list of categories, one for each category of the group
     * on a category's own page (#642). A copy stays over the top while they're scrolled off (#650).
     */
    @Nullable private CategoryStrip categoryStrip;
    /** Each group's name, which the list of categories shows over its first one. */
    private final List<CategoryGroups.Label> groupLabels = new ArrayList<>();
    /** The group whose chip was picked, which the list of categories shows alone, or -1 for all. */
    private int shownGroup = -1;
    /** HushGram's name over the page while the black or the light look is on (#455, #762). */
    @Nullable private Preference header;
    /** Before you sign in, while no tap has hidden it. The home page's, like the status card. */
    @Nullable private Preference beforeSignIn;
    @Nullable private String restoredCategory;
    private int restoredPosition = -1;
    private int restoredTop;

    /** The page's dialogs that may still be on screen, which would outlive it. */
    private final List<Dialog> shownDialogs = new ArrayList<>();
    private final Set<Dialog> mutatingDialogs = new HashSet<>();
    private static final Map<Preference, WeakReference<HushgramPreferenceFragment>> mutationOwners = new WeakHashMap<>();
    private static WeakReference<HushgramPreferenceFragment> newPinOwner = new WeakReference<>(null);

    /** The Downloads section, where the running saves are listed, or null when no download patch is in. */
    @Nullable
    PreferenceCategory downloads;
    /** Choose a folder (#333) and Use Movies and Pictures, under Save folder. */
    @Nullable Row saveTree;
    @Nullable Row galleryFolders;
    @Nullable Row headerPicture;
    @Nullable Row removeHeaderPicture;
    @Nullable PreferenceCategory recovery;
    @Nullable private Row interruptedSaves;
    @Nullable private Row lastCarouselSave;

    /** The rows of the saves running now, by save number. */
    private final Map<Integer, SaveRow> saveRows = new HashMap<>();

    /** Keeps the rows in step with the saves while the page is showing. Saves tell it from their own thread. */
    private final SaveControl.Watcher saves = () -> Utils.runOnMainThread(this::showSaves);

    @Override
    public void onCreate(Bundle state) {
        if (state != null) {
            restoredCategory = state.getString("hushgram_open_category");
            List<String> expanded = state.getStringArrayList("hushgram_expanded_categories");
            if (expanded != null) expandedCategories.addAll(expanded);
            restoredPosition = state.getInt("hushgram_category_position", -1);
            restoredTop = state.getInt("hushgram_category_top", 0);
            shownGroup = state.getInt("hushgram_category_group", -1);
            documentRequest = state.getInt("hushgram_document_request", 0);
            // Older saved pages used the operation itself as their Android request code.
            documentCode = state.getInt("hushgram_document_code", documentRequest);
            fontRequestRevision = state.getLong("hushgram_font_revision", -1);
            if (isFontRequest(documentRequest) && !Fonts.importProcess().equals(state.getString("hushgram_font_process"))) {
                fontRequestRevision = fontSetting(documentRequest).savedRevision();
            }
            documentSequence = Math.max(documentSequence,
                    Math.max(documentCode, state.getInt("hushgram_document_sequence", 0)));
            configurationExportToken = state.getString("hushgram_configuration_export");
            if (documentRequest == EXPORT_CONFIGURATION) {
                ExportStatus.State current = ExportStatus.CONFIGURATION.state();
                if (current == null) {
                    // The process restarted with a still-pending Android picker, not export history.
                    configurationExportToken = ExportStatus.CONFIGURATION.begin(
                            L10n.t("Choose a file for the settings export."), true);
                } else if (!current.active || !current.choosing || !current.token.equals(configurationExportToken)) {
                    // This saved bundle predates a result already claimed by the original page.
                    documentRequest = 0;
                    documentCode = 0;
                }
            }
        } else if (getParentFragment() instanceof SettingsDialog) {
            // Keep the old restart marker compatible by expanding its section on the main page.
            restoredCategory = ((SettingsDialog) getParentFragment()).startPage();
        }
        super.onCreate(state);
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("hushgram_document_request", documentRequest);
        state.putInt("hushgram_document_code", documentCode);
        state.putLong("hushgram_font_revision", fontRequestRevision);
        state.putString("hushgram_font_process", Fonts.importProcess());
        state.putInt("hushgram_document_sequence", documentSequence);
        state.putString("hushgram_configuration_export", configurationExportToken);
        state.putStringArrayList("hushgram_expanded_categories", new ArrayList<>(expandedCategories));
        state.putInt("hushgram_category_group", shownGroup);
        if (lastCategory != null) state.putString("hushgram_open_category", lastCategory);
        ListView list = listView();
        boolean searching = !searchQuery.trim().isEmpty();
        View first = list == null ? null : list.getChildAt(0);
        state.putInt("hushgram_category_position", searching || list == null ? 0 : list.getFirstVisiblePosition());
        state.putInt("hushgram_category_top", searching || first == null ? 0 : first.getTop() - list.getPaddingTop());
    }

    @Override
    public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container,
                             Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        ListView list = root == null ? null : root.findViewById(android.R.id.list);
        // The search field keeps the focus while its typing filters the rows (TypingFocusHold).
        if (list != null) TypingFocusHold.around(list);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView list = view.findViewById(android.R.id.list);
        if (list != null) {
            // Our saved position belongs to the restored page, not the previous search results.
            list.setSaveEnabled(false);
            list.setItemsCanFocus(true);
            list.setDivider(null);
            list.setDividerHeight(0);
            ScreenColors palette = ScreenColors.current();
            if (!palette.light && !palette.flat) {
                list.setBackground(LiquidGlassSurfaces.pageBackground(getContext()));
            } else {
                list.setBackgroundColor(palette.background);
            }
            // The last row ends with room under it instead of on the gesture bar, and the room
            // scrolls with the rows.
            list.setClipToPadding(false);
            list.setPaddingRelative(list.getPaddingStart(), list.getPaddingTop(), list.getPaddingEnd(),
                    Math.round(24 * list.getResources().getDisplayMetrics().density));
            // A layout calls this too, so the bar learns when the header comes and goes as well as scrolls.
            list.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
                @Override public void onScrollStateChanged(android.widget.AbsListView view, int state) {
                }

                @Override public void onScroll(android.widget.AbsListView view, int first, int count, int total) {
                    showTitle();
                    showPinnedStrip();
                }
            });
        }
        attachPinnedStrip();
        showTitle();
    }

    /** Puts the chips' pinned copy over the list, in place of an earlier build's. */
    private void attachPinnedStrip() {
        ListView list = listView();
        if (list == null || categoryStrip == null || !(list.getParent() instanceof android.widget.FrameLayout)) return;
        android.widget.FrameLayout hold = (android.widget.FrameLayout) list.getParent();
        View pinned = categoryStrip.pinned(hold.getContext());
        if (pinned.getParent() == hold) return;
        for (int i = hold.getChildCount() - 1; i >= 0; i--) {
            if (CategoryStrip.PINNED_TAG.equals(hold.getChildAt(i).getTag())) hold.removeViewAt(i);
        }
        if (pinned.getParent() instanceof android.view.ViewGroup) ((android.view.ViewGroup) pinned.getParent()).removeView(pinned);
        hold.addView(pinned, new android.widget.FrameLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.Gravity.TOP));
        showPinnedStrip();
    }

    /**
     * Shows the chips' copy over the top of the list while the strip itself has scrolled off it, so
     * the other categories stay a tap away down a long page (#650). Invisible rather than gone, so
     * a scroll doesn't lay the page out again.
     */
    private void showPinnedStrip() {
        ListView list = listView();
        View pinned = categoryStrip == null ? null : categoryStrip.pinnedIfMade();
        if (list == null || pinned == null) return;
        View row = null;
        boolean off = false;
        if (getPreferenceScreen() != null && categoryStrip.getParent() == getPreferenceScreen()) {
            int first = list.getFirstVisiblePosition();
            for (int at = 0; at < list.getCount() && at < first + list.getChildCount(); at++) {
                if (list.getItemAtPosition(at) != categoryStrip) continue;
                row = at < first ? null : list.getChildAt(at - first);
                off = at < first || row != null && row.getTop() < list.getPaddingTop();
                break;
            }
        }
        boolean shown = pinned.getVisibility() == View.VISIBLE;
        // It takes over where the strip was scrolled sideways to.
        if (off && !shown && row != null) pinned.setScrollX(row.getScrollX());
        if (off != shown) pinned.setVisibility(off ? View.VISIBLE : View.INVISIBLE);
    }

    /** A group's chip was tapped: the list of categories shows that group alone, or every one for -1. */
    private void pickGroup(int group) {
        if (shownGroup == group || shownPage() != null || !searchText(searchQuery).trim().isEmpty()) return;
        View pinned = categoryStrip == null ? null : categoryStrip.pinnedIfMade();
        boolean fromPinned = pinned != null && pinned.getVisibility() == View.VISIBLE;
        shownGroup = group;
        filterSettings();
        if (categoryGrid != null) categoryGrid.refresh();
        ListView list = listView();
        // Picked from the copy deep down the list: the chips go to the top, the group's categories under them.
        if (list != null && fromPinned) list.post(() -> {
            if (list != listView()) return;
            for (int i = 0; i < list.getCount(); i++) {
                if (list.getItemAtPosition(i) == categoryStrip) {
                    list.setSelectionFromTop(i, 0);
                    return;
                }
            }
        });
    }

    @Override public void onViewStateRestored(Bundle state) {
        super.onViewStateRestored(state);
        focusPage();
        ListView list = listView();
        if (list != null && restoredPosition >= 0) {
            int position = restoredPosition, top = restoredTop;
            restoredPosition = -1;
            list.post(() -> {
                if (list == listView()) list.setSelectionFromTop(position, top);
            });
        } else if (list != null && categoryPages() && lastCategory != null && shownPage() == null) {
            String category = lastCategory;
            list.post(() -> {
                if (list != listView()) return;
                for (int i = 0; i < list.getCount(); i++) {
                    Object item = list.getItemAtPosition(i);
                    if (item instanceof Heading && category.equals(((Heading) item).getKey())) {
                        list.setSelectionFromTop(i, 0);
                        break;
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AbstractPreferenceFragment.setRestartFeedbackPresenter(restartFeedback);
        showRestart();
        ExportStatus.CONFIGURATION.watch(exportChanges);
        ExportStatus.DIAGNOSTICS.watch(exportChanges);
        Fonts.watchImports(fontChanges);
        SaveControl.watch(saves);
        SaveLeftovers.showInterrupted(getContext());
        showSaves();
        showClearPositions();
        showConfiguration();
        showLock();
    }

    @Override
    public void onPause() {
        ExportStatus.CONFIGURATION.unwatch(exportChanges);
        ExportStatus.DIAGNOSTICS.unwatch(exportChanges);
        Fonts.unwatchImports(fontChanges);
        SaveControl.unwatch(saves);
        undoRefresh.removeCallbacks(refreshUndo);
        lockRefresh.removeCallbacks(lockTick);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        AbstractPreferenceFragment.clearRestartFeedbackPresenter(restartFeedback);
        undoRefresh.removeCallbacks(refreshUndo);
        lockRefresh.removeCallbacks(lockTick);
        for (Dialog dialog : new ArrayList<>(shownDialogs)) dialog.dismiss();
        shownDialogs.clear();
        mutatingDialogs.clear();
        Fonts.unwatchImports(fontChanges);
        if (newPinOwner.get() == this) PinLock.closeNew();
        clearPositions = null;
        exportDiagnostics = null;
        exportConfiguration = importConfiguration = undoConfiguration = null;
        recovery = null;
        interruptedSaves = null;
        // The rows outlive this view, and a search left on them would be read as how they were built.
        restoreOrders();
        if (!categoryPages()) NewRowDots.leftEverything();
        else for (PreferenceCategory group : searchableRows.keySet()) {
            if (expandedCategories.contains(group.getKey())) NewRowDots.left(group);
        }
        super.onDestroyView();
    }

    @Override public void onDestroy() {
        Activity activity = getActivity();
        boolean removed = isRemoving() || (getParentFragment() != null && getParentFragment().isRemoving());
        if (documentRequest == EXPORT_CONFIGURATION && (removed || (activity != null && activity.isFinishing()))) {
            ExportStatus.CONFIGURATION.finish(configurationExportToken, L10n.t("Settings export canceled."));
        }
        NewRowDots.stop();
        DescriptionInfo.searching = false;
        super.onDestroy();
    }

    @Override
    protected void initialize() {
        // Loads the switches before the shared fragment syncs them to the screen.
        Settings.HIDE_ADS.get();

        // Every row inflates with the theme of the context it was built with. Instagram's activity
        // theme can be light or dark, and the wrong one's text would vanish on this screen's page.
        ScreenColors.shown = null;
        Context context = themed(getContext());
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
        screen.setKey(SCREEN_KEY);
        setPreferenceScreen(screen);
        searchableRows.clear();
        searchAliases.clear();
        // The rows are built again, so the old ones' greying is no longer anyone's to undo.
        lockedRows.clear();

        viewToggle = new Row(context);
        viewToggle.setKey("hushgram_view_toggle");
        viewToggle.setPersistent(false);
        viewToggle.setOnPreferenceClickListener(tapped -> {
            Preference own = findPreference(Settings.CATEGORY_PAGES.key);
            if (own == null ? !allowMutation() : !allowMutation(own, true)) return true;
            if (own instanceof TwoStatePreference) {
                ((TwoStatePreference) own).setChecked(!Settings.CATEGORY_PAGES.savedValue());
            } else {
                Settings.CATEGORY_PAGES.save(!Settings.CATEGORY_PAGES.savedValue());
                filterSettings();
            }
            return true;
        });
        gridToggle = new Row(context);
        gridToggle.setKey("hushgram_grid_toggle");
        gridToggle.setPersistent(false);
        gridToggle.setOnPreferenceClickListener(tapped -> {
            if (!allowMutation()) return true;
            keepingScroll(() -> {
                Settings.CATEGORY_GRID.save(!Settings.CATEGORY_GRID.savedValue());
                filterSettings();
            }, gridToggle);
            return true;
        });
        categoryGrid = new CategoryGrid(context, this::openFromGrid);
        // Before the first section, after the notice and recovery notes above it.
        categoryGrid.setOrder(-10);
        categoryStrip = new CategoryStrip(context, this::switchPage, this::pickGroup);
        // Right over the categories it picks from, under the toggles; on a category's page, right
        // under Search, the only other row the page has above its own.
        categoryStrip.setOrder(-20);
        header = new SettingsHeader(context);
        header.setOrder(-60);
        Preference status = statusCard(context);
        status.setOrder(-40);
        screen.addPreference(status);
        // Search and the list switch lead the page; notices and recovery follow them.
        beforeSignIn = null;
        if (!Settings.SIGN_IN_NOTICE_HIDDEN.savedValue()) {
            Preference notice = beforeSignIn = signInNotice(context, screen);
            notice.setOrder(-30);
            screen.addPreference(notice);
        }
        viewToggle.setOrder(-45);
        screen.addPreference(viewToggle);
        gridToggle.setOrder(-29);
        search = new SearchRow(context);
        search.setOrder(-50);
        search.setKey("hushgram_settings_search");
        search.setTitle(L10n.t("Search settings"));
        search.setPersistent(false);
        // ListView skips non-selectable rows when restoring or resetting its position outside
        // touch mode. Search contains real controls, so it must remain a selectable row.
        screen.addPreference(search);
        noSearchResults = new Row(context);
        noSearchResults.setPersistent(false);
        noSearchResults.setSelectable(false);
        noSearchResults.setKey("hushgram_settings_search_empty");
        noSearchResults.setOrder(-1);
        noSearchResults.setTitle(L10n.t("No matching settings"));
        noSearchResults.setSummary(L10n.t("Try another word. Settings only appear for patches included in this install."));
        mark(noSearchResults, SettingsIcons.SEARCH);
        // The export row below reads this; registering twice keeps one.
        PatchFamily.registerDiagnostics();
        Set<PatchFamily> build = PatchFamily.inThisBuild();
        // Everyday controls first; settings access and upkeep follow them. Built in this order, since
        // some read rows an earlier one made, and each claimed for its group as it's built (#642).
        PrivacyCategory.add(this, screen, context, build);
        CategoryGroups.claim(screen, CategoryGroups.PRIVACY);
        FeedCategory.add(this, screen, context, build);
        CategoryGroups.claim(screen, CategoryGroups.CONTENT);
        MetaAiCategory.add(this, screen, context, build);
        CategoryGroups.claim(screen, CategoryGroups.PRIVACY);
        ExploreCategory.add(this, screen, context, build);
        CategoryGroups.claim(screen, CategoryGroups.CONTENT);
        MessagesCategory.add(this, screen, context, build);
        CategoryGroups.claim(screen, CategoryGroups.PRIVACY);
        ReelsCategory.add(this, screen, context, build);
        StoriesCategory.add(this, screen, context, build);
        CategoryGroups.claim(screen, CategoryGroups.CONTENT);
        PlaybackCategory.add(this, screen, context, build);
        CategoryGroups.claim(screen, CategoryGroups.MEDIA);
        SharingCategory.add(this, screen, context, build);
        CommentsCategory.add(this, screen, context, build);
        ProfilesCategory.add(this, screen, context, build);
        CategoryGroups.claim(screen, CategoryGroups.CONTENT);
        LayoutCategory.add(this, screen, context, build);
        TabBarCategory.add(this, screen, context, build);
        CategoryGroups.claim(screen, CategoryGroups.LOOK);
        NotificationsCategory.add(this, screen, context, build);
        CategoryGroups.claim(screen, CategoryGroups.CONTENT);
        StorageCategory.add(this, screen, context, build);
        DownloadsCategory.add(this, screen, context, build);
        CategoryGroups.claim(screen, CategoryGroups.MEDIA);
        ViewingHistoryCategory.add(this, screen, context, build);
        CategoryGroups.claim(screen, CategoryGroups.CONTENT);
        BatteryCategory.add(this, screen, context, build);
        CategoryGroups.claim(screen, CategoryGroups.MEDIA);
        UpdatesCategory.add(this, screen, context, build);
        EntryCategory.add(this, screen, context, build);
        PatchTimeCategory.add(this, screen, context, build);
        BackupCategory.add(this, screen, context, build);
        PauseCategory.add(this, screen, context, build);
        AboutCategory.add(this, screen, context, build);
        CategoryGroups.claim(screen, CategoryGroups.HUSHGRAM);
        CategoryGroups.arrange(screen);
        groupLabels.clear();
        for (int group = 0; group < CategoryGroups.COUNT; group++) groupLabels.add(CategoryGroups.label(context, group));

        // Outside every category, like Support HushGram below it. Every row counts toward the
        // switches it lists, the ones a folded category or a search keeps off the screen too.
        Row bug = report = new Row(context);
        bug.setKey(REPORT_ROW_KEY);
        bug.setTitle(L10n.t("Report a problem"));
        bug.setSummary(L10n.t("Opens a GitHub report with your versions and settings filled in. Add what happened."));
        bug.setPersistent(false);
        bug.setOrder(Integer.MAX_VALUE / 2 - 1);
        bug.setOnPreferenceClickListener(p -> {
            ReportProblemForm.open(p.getContext(), everyRow());
            return true;
        });
        mark(bug, SettingsIcons.BUG);

        // Outside every category, so no category section, count or saved setting changes. filterSettings
        // puts it last on the home page and in a search it matches.
        Row donate = support = new Row(context);
        donate.setKey(SUPPORT_ROW_KEY);
        // Its name alone, with no line under it.
        donate.setTitle(L10n.t("Support HushGram"));
        donate.setPersistent(false);
        // Native Preference.compareTo subtracts orders, so leave room for negative header orders.
        donate.setOrder(Integer.MAX_VALUE / 2);
        donate.setOnPreferenceClickListener(p -> {
            openSupport(p.getContext());
            return true;
        });
        mark(donate, SettingsIcons.OPENING);
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            Preference section = screen.getPreference(i);
            if (!(section instanceof PreferenceCategory)) continue;
            PreferenceCategory group = (PreferenceCategory) section;
            List<Preference> rows = new ArrayList<>();
            for (int j = 0; j < group.getPreferenceCount(); j++) {
                Preference row = group.getPreference(j);
                rows.add(row);
                String key = row.getKey();
                mutationOwners.put(row, new WeakReference<>(this));
                Preference.OnPreferenceChangeListener change = row.getOnPreferenceChangeListener();
                row.setOnPreferenceChangeListener((preference, value) -> allowMutation(preference, false)
                        && (change == null || change.onPreferenceChange(preference, value)));
                StringBuilder aliases = new StringBuilder(
                        row == ghostMode || row == ghostPicks || row == ghostTab ? "ghost mode " : "");
                for (BooleanSetting setting : ghostSwitches) {
                    if (setting.key.equals(key) && row.getTitle() != null) ghostNames.put(key, row.getTitle());
                }
                aliases.append(row == clearPositions
                        ? L10n.t("Clear remembered positions")
                        : row.getTitle() == null ? "" : row.getTitle().toString());
                // Its title follows the lock's state, so the words for every state find it.
                if (row == settingsLock) aliases.append(' ').append(L10n.t("Lock these settings"))
                        .append(' ').append(L10n.t("Ask to unlock")).append(" lock focus cooldown wait timer");
                if (L10n.t("Pure black dark mode").equals(row.getTitle())
                        || L10n.t("Pure black dark mode isn't in this build").equals(row.getTitle())) {
                    aliases.append(" amoled oled black theme");
                }
                if (Settings.SETTINGS_BLACK_LOOK.key.equals(key)) aliases.append(" amoled oled dark theme layout");
                if (Settings.SETTINGS_LIGHT_LOOK.key.equals(key)) aliases.append(" white bright day theme layout");
                if (Settings.SETTINGS_INSTAGRAM_LOOK.key.equals(key)) aliases.append(" native stock official plain flat theme layout");
                if (NotificationsCategory.movedFromProfiles(key)) aliases.append(' ').append(L10n.t("Profiles"));
                if (Settings.HIDE_REELS_TAB.key.equals(key) || Settings.SHOW_REELS_TAB.key.equals(key)) {
                    aliases.append(' ').append(L10n.t("Reels"));
                }
                if (Settings.VIEW_DM_MEDIA_ANONYMOUSLY.key.equals(key)) aliases.append(' ').append(L10n.t("Ads and privacy"));
                // These sat under Ads and privacy until they got a category of their own (#228).
                if (Settings.VIEWING_HISTORY.key.equals(key) || VIEWING_HISTORY_ROW.equals(key)
                        || CLEAR_VIEWING_HISTORY.equals(key)) {
                    aliases.append(' ').append(L10n.t("Ads and privacy"));
                }
                if (Settings.REMOVE_BUILD_EXPIRED_POPUP.key.equals(key)) aliases.append(' ').append(L10n.t("Updates"));
                // Instagram's menu calls clear mode Full screen (#480).
                if (Settings.KEEP_REEL_CLEAR_MODE.key.equals(key) || Settings.START_REEL_CLEAR_MODE.key.equals(key)
                        || Settings.QUIET_REEL_CLEAR_MODE.key.equals(key) || Settings.CLEAR_MODE_SEEK_BAR.key.equals(key)
                        || Settings.PINCH_CLEAR_MODE.key.equals(key) || Settings.HIDE_CLEAR_MODE_EXIT_BUTTON.key.equals(key)) {
                    aliases.append(' ').append(L10n.t("Full screen")).append(" fullscreen");
                }
                for (PatchFamily family : build) {
                    boolean belongs = family.switches.stream().anyMatch(setting -> setting.key.equals(key));
                    belongs |= family == PatchFamily.STORY_RING && (Settings.STORY_RING_SCALE.key.equals(key)
                            || Settings.STORY_RING_COLOR.key.equals(key) || Settings.STORY_RING_THICKNESS.key.equals(key)
                            || Settings.STORY_RING_CUSTOM.key.equals(key) || Settings.STORY_RING_CUSTOM_END.key.equals(key));
                    belongs |= family == PatchFamily.ROUNDED_POST_CORNERS && Settings.ROUNDED_CORNER_SIZE.key.equals(key);
                    belongs |= family == PatchFamily.AMBIENT_LIGHT && (Settings.AMBIENT_LIGHT_SPREAD.key.equals(key)
                            || Settings.AMBIENT_LIGHT_SPEED.key.equals(key) || Settings.AMBIENT_LIGHT_BLEND.key.equals(key));
                    belongs |= family == PatchFamily.STORY_PROGRESS_BAR && Settings.STORY_BAR_THICKNESS.key.equals(key);
                    belongs |= family == PatchFamily.STORIES_TRAY && Settings.TRAY_BADGE.key.equals(key);
                    belongs |= family == PatchFamily.REEL_TAP_AND_VOLUME && Settings.REEL_TAP_CHOICE.key.equals(key);
                    belongs |= family == PatchFamily.REEL_DECLUTTER && (Settings.BURN_IN_GUARD.key.equals(key)
                            || Settings.BURN_IN_WAIT.key.equals(key) || Settings.BURN_IN_STAY_DIM.key.equals(key)
                            || Settings.BURN_IN_LIGHT_WHEN_PAUSED.key.equals(key)
                            || Settings.BURN_IN_STORIES.key.equals(key)
                            || Settings.START_REEL_CLEAR_MODE.key.equals(key));
                    belongs |= family == PatchFamily.REEL_SCROLLING && (Settings.SCROLL_BREAK.key.equals(key)
                            || Settings.SCROLL_BREAK_LENGTH.key.equals(key)
                            || Settings.REEL_CAP_COUNT.key.equals(key) || Settings.REEL_CAP_BREAK.key.equals(key));
                    belongs |= family == PatchFamily.PLAYBACK_QUALITY && (Settings.PLAYBACK_QUALITY.key.equals(key)
                            || Settings.PLAYBACK_QUALITY_STEPS_DOWN.key.equals(key));
                    belongs |= family == PatchFamily.PLAYBACK_QUALITY && Settings.QUALITY_TAB_TARGET.key.equals(key);
                    belongs |= family == PatchFamily.TAP_TO_PLAY && (Settings.TAP_TO_PLAY_SCOPE.key.equals(key)
                            || Settings.TAP_TO_PLAY_AUTO_SCROLL_WAITS.key.equals(key));
                    belongs |= family == PatchFamily.STORY_TIME && Settings.STORY_TIME_MODE.key.equals(key);
                    belongs |= family == PatchFamily.STORY_MENTIONS && Settings.STORY_MENTIONS_LOOK.key.equals(key);
                    belongs |= family == PatchFamily.CAROUSEL_ARROWS && Settings.CAROUSEL_PILL_LOOK.key.equals(key);
                    belongs |= family == PatchFamily.FEED_SEEK_BAR && (Settings.FEED_SEEK_BAR_LOOK.key.equals(key)
                            || Settings.SEEK_BAR_SHAPE.key.equals(key)
                            || Settings.FEED_SEEK_SHAPE.key.equals(key)
                            || Settings.FEED_DOUBLE_TAP_SKIP_SECONDS.key.equals(key)
                            || Settings.FEED_SEEK_TIME.key.equals(key) || Settings.FEED_SEEK_TIME_LEFT.key.equals(key)
                            || Settings.FEED_SEEK_TIME_UNDER_TAGS.key.equals(key)
                            || Settings.FEED_SEEK_TIME_TOP.key.equals(key));
                    belongs |= family == PatchFamily.REEL_DOUBLE_TAP_SKIP
                            && Settings.DOUBLE_TAP_SKIP_REELS_SECONDS.key.equals(key);
                    belongs |= family == PatchFamily.FEED_AUTO_SCROLL && Settings.FEED_AUTO_SCROLL_SECONDS.key.equals(key);
                    belongs |= family == PatchFamily.EXPAND_CAPTIONS && Settings.CAPTION_LINES.key.equals(key);
                    belongs |= family == PatchFamily.MEDIA_CACHE && (Settings.MEDIA_CACHE_LIMIT_MB.key.equals(key)
                            || CLEAR_MEDIA_CACHE_NOW.equals(key));
                    belongs |= family == PatchFamily.STORY_SEEK_BAR && (Settings.STORY_SEEK_BAR_LOOK.key.equals(key)
                            || Settings.STORY_SEEK_BAR_POSITION.key.equals(key)
                            || Settings.STORY_SEEK_TIME.key.equals(key) || Settings.STORY_SEEK_TIME_LEFT.key.equals(key));
                    belongs |= family == PatchFamily.REEL_POST_TIME && (Settings.REEL_POST_TIME_SECONDS.key.equals(key)
                            || Settings.REEL_POST_TIME_AGO.key.equals(key));
                    belongs |= family == PatchFamily.FRIENDSHIP_STATUS && Settings.FOLLOW_LABEL_PLACE.key.equals(key);
                    belongs |= family == PatchFamily.FULL_SCREEN_BARS && Settings.FULL_SCREEN_HIDES.key.equals(key);
                    belongs |= family == PatchFamily.GLASS_TAB_BAR && (Settings.GLASS_TAB_BAR_HAPTIC_STYLE.key.equals(key)
                            || Settings.GLASS_TAB_BAR_HAPTIC_STRENGTH.key.equals(key)
                            || Settings.GLASS_TAB_BAR_TAP_SOUND_VOLUME.key.equals(key));
                    belongs |= family == PatchFamily.GLASS_TAB_BAR && (Settings.GLASS_TAB_BAR_OPACITY.key.equals(key) || Settings.GLASS_TAB_BAR_REFRACT_STRENGTH.key.equals(key)
                            || Settings.GLASS_TAB_BAR_BLUR_RADIUS.key.equals(key)
                            || Settings.GLASS_TAB_BAR_HEIGHT.key.equals(key) || Settings.GLASS_TAB_BAR_LIFT.key.equals(key)
                            || Settings.GLASS_TAB_BAR_CORNERS.key.equals(key) || Settings.GLASS_TAB_BAR_REELS_HIDE.key.equals(key));
                    belongs |= family == PatchFamily.GLASS_CHAT && (Settings.GLASS_CHAT_TOP_BAR_OPACITY.key.equals(key)
                            || Settings.GLASS_CHAT_BOX_OPACITY.key.equals(key));
                    belongs |= family == PatchFamily.VIDEO_DOWNLOAD && (Settings.HOLD_TO_DOWNLOAD.key.equals(key)
                            || Settings.HOLD_FINGERS.key.equals(key) || Settings.HOLD_TIME.key.equals(key)
                            || Settings.FEED_DOWNLOAD_BUTTON.key.equals(key) || Settings.FEED_DOWNLOAD_ALL_PAGES.key.equals(key)
                            || Settings.DOWNLOAD_CHOOSE_PAGES.key.equals(key));
                    belongs |= family == PatchFamily.REEL_DOWNLOAD && Settings.REEL_DOWNLOAD_BUTTON.key.equals(key);
                    belongs |= family == PatchFamily.REEL_DOWNLOAD && Settings.FEED_DOWNLOAD_ALL_PAGES.key.equals(key);
                    belongs |= family == PatchFamily.REEL_DOWNLOAD && Settings.REEL_DOWNLOAD_BUTTON_ASKS.key.equals(key);
                    belongs |= family == PatchFamily.REEL_DOWNLOAD && Settings.REEL_NOT_INTERESTED_BUTTON.key.equals(key);
                    belongs |= family == PatchFamily.REEL_DOWNLOAD && Settings.REEL_BLOCK_BUTTON.key.equals(key);
                    belongs |= family == PatchFamily.REEL_DOWNLOAD && Settings.TEXT_BUTTON_ON_REELS.key.equals(key);
                    belongs |= family == PatchFamily.REEL_DOWNLOAD && Settings.WHOLE_REEL_IN_REEL_MENU.key.equals(key);
                    belongs |= (family == PatchFamily.STORY_DOWNLOAD || family == PatchFamily.FULL_SCREEN_BARS)
                            && Settings.WHOLE_STORY_IN_STORY_MENU.key.equals(key);
                    belongs |= (family == PatchFamily.REEL_DOWNLOAD || family == PatchFamily.KEEP_REEL_SPEED)
                            && (Settings.SPEED_BUTTON_ON_REELS.key.equals(key) || Settings.SPEED_IN_REEL_MENU.key.equals(key));
                    belongs |= (family == PatchFamily.REEL_DOWNLOAD || family == PatchFamily.REEL_AUTO_SCROLL)
                            && Settings.AUTO_SCROLL_IN_REEL_MENU.key.equals(key);
                    belongs |= (family == PatchFamily.REEL_DOWNLOAD || family == PatchFamily.REEL_DECLUTTER)
                            && Settings.FULL_SCREEN_BUTTON_ON_REELS.key.equals(key);
                    belongs |= (family == PatchFamily.REEL_DOWNLOAD || family == PatchFamily.REEL_TAP_AND_VOLUME)
                            && (Settings.MUTE_BUTTON_ON_REELS.key.equals(key) || Settings.MUTE_BUTTON_IN_CLEAR_MODE.key.equals(key));
                    belongs |= family == PatchFamily.REEL_DOWNLOAD && (Settings.HIDE_SOUND_REELS.key.equals(key)
                            || HIDDEN_SOUNDS_ROW.equals(key) || Settings.HIDE_SOUND_HOME.key.equals(key));
                    belongs |= family == PatchFamily.ACCENT_COLOR && (Settings.ACCENT_HUE.key.equals(key)
                            || Settings.ACCENT_CUSTOM.key.equals(key));
                    belongs |= family == PatchFamily.FONT && (Settings.FONT.key.equals(key)
                            || Settings.TEXT_SIZE.key.equals(key));
                    belongs |= family == PatchFamily.HEADER_TEXT && (Settings.HEADER_TEXT_CENTER.key.equals(key)
                            || Settings.HEADER_TEXT_FONT.key.equals(key));
                    belongs |= family == PatchFamily.EMOJI_STYLE && Settings.EMOJI_FONT.key.equals(key);
                    belongs |= family == PatchFamily.STORY_FONTS && STORY_FONTS_ROW.equals(key);
                    belongs |= family == PatchFamily.APP_ICON && Settings.APP_ICON.key.equals(key);
                    belongs |= family == PatchFamily.SETTINGS_ROWS && Settings.HIDDEN_SETTINGS_ROWS.key.equals(key);
                    belongs |= family == PatchFamily.NOTIFICATION_GROUPS && Settings.MUTED_NOTIFICATION_CHANNELS.key.equals(key);
                    belongs |= family == PatchFamily.PROFILE_SUGGESTIONS && Settings.HIDDEN_NOTIFICATION_KINDS.key.equals(key);
                    belongs |= family == PatchFamily.SHARE_SHEET && Settings.HIDDEN_SHARE_SHEET_BUTTONS.key.equals(key);
                    belongs |= family == PatchFamily.PROFILE_TABS && (Settings.HIDDEN_PROFILE_TABS.key.equals(key)
                            || Settings.HIDDEN_OWN_PROFILE_TABS.key.equals(key));
                    belongs |= family == PatchFamily.MESSAGE_BOX && Settings.HIDDEN_MESSAGE_BOX_BUTTONS.key.equals(key);
                    belongs |= family == PatchFamily.MENU_ITEMS && Settings.HIDDEN_MENU_ITEMS.key.equals(key);
                    belongs |= family == PatchFamily.REELS_TAB
                            && (Settings.START_TAB.key.equals(key) || Settings.TAB_ORDER.key.equals(key)
                            || Settings.TAB_ICON_SIZE.key.equals(key));
                    belongs |= family == PatchFamily.TAB_BAR_LAYOUT && Settings.TAB_BAR_LAYOUT.key.equals(key);
                    belongs |= family == PatchFamily.SWIPE_TO_CREATE && Settings.HOME_SWIPE_OPENS.key.equals(key);
                    belongs |= family == PatchFamily.LIKE_ANIMATION && Settings.LIKE_ANIMATION.key.equals(key);
                    belongs |= family == PatchFamily.CAPTION_WORD_REELS && (Settings.REELS_BLOCKED_WORDS.key.equals(key)
                            || Settings.HIDE_CAPTION_WORD_HOME.key.equals(key)
                            || Settings.HIDE_CAPTION_WORD_COMMENTS.key.equals(key)
                            || Settings.HIDE_CAPTION_WORD_SEARCH.key.equals(key)
                            || Settings.REELS_KEPT_LANGUAGES.key.equals(key) || Settings.HIDE_OTHER_LANGUAGE_HOME.key.equals(key));
                    belongs |= family == PatchFamily.LOW_VIEW_REELS
                            && (Settings.MIN_REEL_VIEWS.key.equals(key) || Settings.MIN_REEL_LIKES.key.equals(key)
                            || Settings.MAX_REEL_VIEWS.key.equals(key) || Settings.MIN_REEL_LIKES_PER_1000.key.equals(key)
                            || Settings.MAX_REEL_LIKES.key.equals(key) || Settings.MIN_REEL_COMMENTS.key.equals(key)
                            || Settings.MAX_REEL_COMMENTS.key.equals(key) || Settings.MIN_REEL_SHARES.key.equals(key)
                            || Settings.MAX_REEL_SHARES.key.equals(key) || Settings.MIN_REEL_SAVES.key.equals(key)
                            || Settings.MAX_REEL_SAVES.key.equals(key)
                            || Settings.MIN_REEL_COMMENTS_PER_1000.key.equals(key) || Settings.MIN_REEL_SECONDS.key.equals(key)
                            || Settings.MAX_REEL_SECONDS.key.equals(key) || Settings.MAX_REEL_AGE_DAYS.key.equals(key)
                            || Settings.REELS_NEWEST_FIRST.key.equals(key) || Settings.HIDE_LOW_VIEW_HOME.key.equals(key));
                    belongs |= family == PatchFamily.STORY_UPLOAD_QUALITY && (Settings.UPLOAD_QUALITY_APPLIES_TO.key.equals(key)
                            || Settings.UPLOAD_VIDEO_BITRATE.key.equals(key) || Settings.UPLOAD_VIDEO_RESOLUTION.key.equals(key)
                            || Settings.UPLOAD_FRAME_RATE.key.equals(key) || Settings.UPLOAD_PHOTO_QUALITY.key.equals(key)
                            || Settings.UPLOAD_PHOTO_SIZE.key.equals(key));
                    belongs |= family == PatchFamily.STORY_PRIORITY && Settings.STORY_PRIORITY_ACCOUNTS.key.equals(key);
                    belongs |= family == PatchFamily.CALL_BLOCKER
                            && (Settings.CALL_BLOCK_MODE.key.equals(key) || Settings.CALL_BLOCK_ACCOUNTS.key.equals(key)
                            || Settings.CALL_BLOCK_DECLINE.key.equals(key));
                    belongs |= family == PatchFamily.MESSAGES_LOCK
                            && (Settings.LOCK_AGAIN.key.equals(key) || LOCKED_CHATS_ROW.equals(key)
                            || LOCKED_COLLECTIONS_ROW.equals(key) || HIDDEN_CHATS_ROW.equals(key) || PINNED_CHATS_ROW.equals(key) || Settings.UNLOCK_WITH.key.equals(key)
                            || CHANGE_PIN_ROW.equals(key));
                    belongs |= family == PatchFamily.RESUME_LONG_VIDEOS && row == clearPositions;
                    belongs |= (family == PatchFamily.REEL_DOWNLOAD || family == PatchFamily.STORY_DOWNLOAD
                            || family == PatchFamily.VIDEO_DOWNLOAD || family == PatchFamily.PROFILE_PICTURE
                            || family == PatchFamily.VOICE_MESSAGE)
                            && (Settings.DOWNLOAD_QUALITY.key.equals(key)
                            || Settings.SAVE_FOLDER.key.equals(key) || Settings.FILENAME_TEMPLATE.key.equals(key)
                            || Settings.DOWNLOAD_COMPATIBLE.key.equals(key) || Settings.DOWNLOAD_CONVERT.key.equals(key));
                    belongs |= (family == PatchFamily.REEL_DOWNLOAD || family == PatchFamily.STORY_DOWNLOAD)
                            && (Settings.DOWNLOAD_SKIP_SAVED.key.equals(key) || CLEAR_DOWNLOAD_HISTORY.equals(key));
                    belongs |= family == PatchFamily.VIDEO_DOWNLOAD && CLEAR_DOWNLOAD_HISTORY.equals(key);
                    belongs |= family == PatchFamily.VIDEO_DOWNLOAD && (Settings.VIEWING_HISTORY.key.equals(key)
                            || VIEWING_HISTORY_ROW.equals(key) || CLEAR_VIEWING_HISTORY.equals(key));
                    belongs |= family == PatchFamily.PROFILE_PICTURE
                            && (PROFILE_HISTORY_ROW.equals(key) || CLEAR_PROFILE_HISTORY.equals(key)
                            || SAVED_PROFILES_ROW.equals(key) || Settings.SAVED_PROFILES_TAB_TARGET.key.equals(key));
                    belongs |= family == PatchFamily.ONLINE_HISTORY
                            && (ONLINE_HISTORY_ROW.equals(key) || CLEAR_ONLINE_HISTORY.equals(key));
                    belongs |= family == PatchFamily.STORY_VIEWERS
                            && (STORY_VIEWERS_ROW.equals(key) || CLEAR_STORY_VIEWERS.equals(key));
                    belongs |= family == PatchFamily.SENT_FOLLOW_REQUESTS
                            && (SENT_REQUESTS_ROW.equals(key) || CLEAR_SENT_REQUESTS.equals(key));
                    belongs |= family == PatchFamily.FOLLOWING_FEED && Settings.FAVORITES_TAB_TARGET.key.equals(key);
                    belongs |= family == PatchFamily.RESTORE_TRUST && L10n.t("Instagram accepts this build").equals(row.getTitle());
                    belongs |= family == PatchFamily.REMOVE_AD_ID && L10n.t("Advertising ID removed").equals(row.getTitle());
                    belongs |= family == PatchFamily.PURE_BLACK && L10n.t("Pure black dark mode").equals(row.getTitle());
                    belongs |= family == PatchFamily.VERSION_CODE && L10n.t("No Play Store updates").equals(row.getTitle());
                    if (!belongs) continue;
                    aliases.append(' ').append(family.patchName);
                    if (family == PatchFamily.FOLLOWING_FEED) aliases.append(" following home feed");
                    if (Settings.REEL_AUTO_ADVANCE.key.equals(key)) aliases.append(" auto advance autoloop loop next reels");
                    if (Settings.AUTO_SCROLL_IN_REEL_MENU.key.equals(key)) aliases.append(" auto advance three dots menu toggle reels");
                    if (Settings.STOP_VIDEO_LOOPING.key.equals(key)) aliases.append(" autoloop loop replay repeat reels");
                    if (Settings.KEEP_PLAYING_IN_BACKGROUND.key.equals(key)) {
                        aliases.append(" background audio minimized screen off notification music");
                    }
                    if (family == PatchFamily.REEL_DECLUTTER || family == PatchFamily.REEL_DOWNLOAD
                            || family == PatchFamily.REEL_WATCH_HISTORY || family == PatchFamily.REELS_TAB
                            || family == PatchFamily.KEEP_REEL_SPEED || family == PatchFamily.REEL_LINKS
                            || family == PatchFamily.TAP_TO_PLAY || family == PatchFamily.REEL_ABOVE_COMMENTS
                            || family == PatchFamily.PLAYBACK_QUALITY || family == PatchFamily.RESUME_LONG_VIDEOS
                            || family == PatchFamily.REEL_TAP_AND_VOLUME || family == PatchFamily.BACKGROUND_PLAY
                            || Settings.TURN_OFF_DOUBLE_TAP_LIKE_ON_REELS.key.equals(key)) aliases.append(" reels");
                }
                searchAliases.put(row, aliases.toString());
                if (row.getIcon() == null) mark(row, SettingsIcons.forKey(key, iconOf(group)));
            }
            searchableRows.put(group, rows);
            group.setKey(CATEGORY_ROW_KEY + iconOf(group));
            mark(group, iconOf(group));
            group.setOnPreferenceClickListener(tapped -> {
                toggleCategory(group);
                return true;
            });
        }
        // After every row is known: a first open counts them all as seen, so a fresh install has no dots.
        NewRowDots.start(context, searchableRows);
        Set<String> available = new HashSet<>();
        for (PreferenceCategory group : searchableRows.keySet()) {
            available.add(group.getKey());
            NewRowDots.opens(group, group);
        }
        expandedCategories.retainAll(available);
        if (restoredCategory != null && available.contains(restoredCategory)) {
            // On their own pages, the category comes back as the page it was.
            if (ownPages()) pageCategory = restoredCategory;
            else expandedCategories.add(restoredCategory);
            lastCategory = restoredCategory;
        } else if (restoredCategory != null) restoredPosition = -1;
        restoredCategory = null;
        filterSettings();
    }

    private static String iconOf(PreferenceCategory group) {
        return group instanceof Heading ? ((Heading) group).icon : SettingsIcons.SETTINGS;
    }

    /** Whether the page lists its categories. A choice about the page, so Pause doesn't turn it off. */
    private static boolean categoryPages() {
        return Settings.CATEGORY_PAGES.savedValue();
    }

    /** Whether a tapped category opens on a page of its own (#525). */
    private static boolean ownPages() {
        return categoryPages() && Settings.CATEGORY_OWN_PAGE.savedValue();
    }

    /** The category showing on its own page, or null while the list (or a search) shows. */
    @Nullable private PreferenceCategory shownPage() {
        if (pageCategory == null || !ownPages()) return null;
        for (PreferenceCategory group : searchableRows.keySet()) {
            if (pageCategory.equals(group.getKey())) return group;
        }
        return null;
    }

    /** Opens [group] alone, remembering where the list was for Back. */
    private void openPage(PreferenceCategory group) {
        ListView list = listView();
        View first = list == null ? null : list.getChildAt(0);
        overviewPosition = list == null ? -1 : list.getFirstVisiblePosition();
        overviewTop = first == null ? 0 : first.getTop() - list.getPaddingTop();
        pageCategory = group.getKey();
        lastCategory = pageCategory;
        focusPage();
        filterSettings();
        if (list != null) list.post(() -> {
            if (list == listView()) list.setSelection(0);
        });
    }

    /**
     * A chip under Search put [section] on the page in place of the one showing (#642). The list's
     * place is the one kept when the first page opened, so Back still lands where the reader was.
     */
    private void switchPage(Heading section) {
        PreferenceCategory shown = shownPage();
        if (shown == null || shown == section || !searchText(searchQuery).trim().isEmpty()) return;
        NewRowDots.left(shown);
        pageCategory = section.getKey();
        lastCategory = pageCategory;
        focusPage();
        filterSettings();
        ListView list = listView();
        if (list != null) list.post(() -> {
            if (list == listView()) list.setSelection(0);
        });
    }

    /** Back from a category's own page: the list again, where it was. False when no page shows. */
    private boolean closePage() {
        PreferenceCategory group = shownPage();
        if (group == null) return false;
        pageCategory = null;
        if (group.getKey().equals(lastCategory)) lastCategory = null;
        NewRowDots.left(group);
        focusPage();
        filterSettings();
        if (categoryGrid != null) categoryGrid.refresh();
        ListView list = listView();
        int position = overviewPosition, top = overviewTop;
        overviewPosition = -1;
        if (list != null) list.post(() -> {
            if (list != listView()) return;
            if (position >= 0) {
                list.setSelectionFromTop(position, top);
                return;
            }
            for (int i = 0; i < list.getCount(); i++) {
                if (list.getItemAtPosition(i) == group) {
                    list.setSelectionFromTop(i, 0);
                    return;
                }
            }
        });
        return true;
    }

    /**
     * Categories on their own page was flipped from [entry], the Settings entry section. Turned on,
     * that section becomes the page the switch is on. Turned off, it stays open where it was.
     */
    void ownPagesChanged(PreferenceCategory entry) {
        boolean listing = categoryPages() && searchQuery.trim().isEmpty();
        if (Settings.CATEGORY_OWN_PAGE.savedValue()) {
            if (listing && expandedCategories.contains(entry.getKey())) {
                pageCategory = entry.getKey();
                lastCategory = pageCategory;
            } else {
                pageCategory = null;
                lastCategory = null;
            }
            overviewPosition = -1;
        } else if (pageCategory != null) {
            expandedCategories.add(pageCategory);
            lastCategory = pageCategory;
            pageCategory = null;
        }
        filterSettings();
        if (categoryGrid != null) categoryGrid.refresh();
    }

    /** Expand in place. Keep the visible anchor instead of scrolling to the top of settings. */
    private void toggleCategory(PreferenceCategory group) {
        if (!categoryPages() || !searchQuery.trim().isEmpty()) return;
        if (ownPages()) {
            openPage(group);
            return;
        }
        ListView list = listView();
        int position = list == null ? -1 : list.getFirstVisiblePosition();
        Object anchor = list == null || position < 0 || position >= list.getCount()
                ? null : list.getItemAtPosition(position);
        View first = list == null ? null : list.getChildAt(0);
        int top = first == null ? 0 : first.getTop() - list.getPaddingTop();
        if (expandedCategories.remove(group.getKey())) {
            NewRowDots.left(group);
            if (group.getKey().equals(lastCategory)) lastCategory = null;
        } else {
            expandedCategories.add(group.getKey());
            lastCategory = group.getKey();
        }
        focusPage();
        filterSettings();
        if (list != null && anchor != null) {
            list.post(() -> {
                if (list != listView()) return;
                for (int i = 0; i < list.getCount(); i++) {
                    if (list.getItemAtPosition(i) == anchor) {
                        list.setSelectionFromTop(i, top);
                        break;
                    }
                }
            });
        }
    }

    /**
     * Runs [change], then puts the list back where [anchor] was on the screen, so a switch near the
     * top doesn't move the page under the finger.
     */
    private void keepingScroll(Runnable change, Preference anchor) {
        ListView list = listView();
        android.widget.ListAdapter before = list == null ? null : list.getAdapter();
        int at = -1;
        if (before != null) {
            for (int i = 0; i < before.getCount(); i++) if (before.getItem(i) == anchor) { at = i; break; }
        }
        int top = 0;
        if (at >= 0) {
            View row = list.getChildAt(at - list.getFirstVisiblePosition());
            top = row == null ? 0 : row.getTop() - list.getPaddingTop();
            if (row == null) at = -1;
        }
        change.run();
        if (list == null || at < 0) return;
        int kept = top;
        list.post(() -> {
            if (list != listView()) return;
            for (int i = 0; i < list.getCount(); i++) {
                if (list.getItemAtPosition(i) == anchor) {
                    list.setSelectionFromTop(i, kept);
                    return;
                }
            }
        });
    }

    /**
     * A tile was tapped: its section opens on this page, below the tiles, and any other one folds,
     * as if each had a page of its own. The tile of the section already open only scrolls to it.
     */
    private void openFromGrid(Heading section) {
        if (!categoryPages() || !Settings.CATEGORY_GRID.savedValue() || !searchQuery.trim().isEmpty()) return;
        if (ownPages()) {
            openPage(section);
            return;
        }
        String key = section.getKey();
        if (!expandedCategories.contains(key)) {
            for (PreferenceCategory other : searchableRows.keySet()) {
                if (other != section && !other.getKey().equals(key) && expandedCategories.remove(other.getKey())) {
                    NewRowDots.left(other);
                }
            }
            expandedCategories.add(key);
        }
        lastCategory = key;
        focusPage();
        filterSettings();
        if (categoryGrid != null) categoryGrid.refresh();
        ListView list = listView();
        if (list == null) return;
        list.post(() -> {
            if (list != listView()) return;
            for (int i = 0; i < list.getCount(); i++) {
                if (list.getItemAtPosition(i) == section) {
                    list.setSelectionFromTop(i, 0);
                    return;
                }
            }
        });
    }

    /**
     * Back clears a search first, then leaves a category's own page. Expanded sections do not add a
     * navigation level.
     */
    boolean closeCategory() {
        return endSearch() || closePage();
    }

    /**
     * Back during a search ends it on the page it started from, as the field's own clear does.
     * From a category's page Back used to drop the category under the search instead: the matches
     * stayed, and the status card and the list switch came in above the field (S25, 2026-10-13).
     */
    private boolean endSearch() {
        if (searchText(searchQuery).trim().isEmpty()) return false;
        View view = getView();
        EditText field = view == null ? null : view.findViewWithTag("hushgram-settings-search");
        // On the screen the field's watcher filters and puts its clear button away. Scrolled off
        // it there is no field, and the next one is built from the query.
        if (field != null) field.setText("");
        if (!searchQuery.isEmpty()) searchSettings("");
        ListView list = listView();
        if (list != null) list.post(() -> list.setSelection(0));
        return true;
    }

    @Nullable private ListView listView() {
        View view = getView();
        return view == null ? null : view.findViewById(android.R.id.list);
    }

    /** Binds the rows on show again, for a choice about how every row looks. */
    void rebindRows() {
        ListView list = listView();
        if (list != null) list.invalidateViews();
    }

    /**
     * Black or Light settings page shows at once: the header, the category lines, every row's
     * colours, and the page, bar and Restart now around them (#455, #762).
     */
    void applyLook() {
        ListView list = listView();
        if (list != null) {
            ScreenColors palette = ScreenColors.current();
            if (!palette.light && !palette.flat) {
                list.setBackground(LiquidGlassSurfaces.pageBackground(getContext()));
            } else {
                list.setBackgroundColor(palette.background);
            }
        }
        if (getParentFragment() instanceof SettingsDialog) ((SettingsDialog) getParentFragment()).repaint();
        filterSettings();
        rebindRows();
    }

    /** Page entry shouldn't focus the search field or move a restored viewport to show it. */
    private void focusPage() {
        View view = getView();
        if (!(view instanceof android.view.ViewGroup)) return;
        android.view.ViewGroup body = (android.view.ViewGroup) view;
        FocusRest.on(body);
        body.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        // PreferenceFragment posts focusableViewAvailable(list) after binding. Keep initial
        // focus here; explicit focus on Search and its controls still works normally. Under a
        // keyboard the rest steps aside and this lands on the rows.
        body.setDescendantFocusability(android.view.ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        body.requestFocus();
    }

    /**
     * The bar's title: the search while there is one, a category's name on its own page, else
     * HushGram unless the header shows it.
     */
    private void showTitle() {
        if (!(getParentFragment() instanceof SettingsDialog)) return;
        boolean searching = !searchText(searchQuery).trim().isEmpty();
        PreferenceCategory page = searching ? null : shownPage();
        ((SettingsDialog) getParentFragment()).showTitle(searching ? L10n.t("Search settings")
                : page != null ? page.getTitle() : null, headerNameShown());
    }

    /**
     * Whether some of the header's HushGram name is on the screen. A search that starts scrolls its
     * field to the top and the header away, and so can a reader, and then the bar names the page.
     */
    private boolean headerNameShown() {
        ListView list = listView();
        if (list == null || header == null || header.getParent() != getPreferenceScreen()) return false;
        View name = list.findViewWithTag(SettingsHeader.NAME);
        return name != null && name.isShown() && name.getLocalVisibleRect(new android.graphics.Rect());
    }

    /** Keeps the screen's Restart now bar in step with what waits on a restart. */
    private void showRestart() {
        if (getParentFragment() instanceof SettingsDialog) {
            ((SettingsDialog) getParentFragment()).showRestart(restartActionAvailable());
        }
    }

    /** Hidden rows still participate in the shared preference synchronization contract. */
    @Override public Preference findPreference(CharSequence key) {
        Preference visible = super.findPreference(key);
        if (visible != null || key == null) return visible;
        for (List<Preference> rows : searchableRows.values()) {
            for (Preference row : rows) if (key.toString().equals(row.getKey())) return row;
        }
        return null;
    }

    /**
     * A plain row tied to a setting (Tab order) shows its own summary and saves its own value, so
     * there's nothing to sync. The shared sync would log every one as a patch error each time
     * settings opens and on every tap.
     */
    @Override
    protected void syncSettingWithPreference(Preference pref, Setting<?> setting, boolean applySettingToPreference) {
        if (pref instanceof Row) return;
        super.syncSettingWithPreference(pref, setting, applySettingToPreference);
    }

    private void restoreSearchRows() {
        PreferenceScreen screen = getPreferenceScreen();
        if (screen == null || search == null || screen.findPreference(search.getKey()) != search) return;
        for (Map.Entry<PreferenceCategory, List<Preference>> section : searchableRows.entrySet()) {
            PreferenceCategory group = section.getKey();
            if (group.getParent() != screen) screen.addPreference(group);
            for (Preference row : section.getValue()) if (row.getParent() != group) group.addPreference(row);
        }
    }

    @Override protected void updateUIToSettingValues() {
        restoreSearchRows();
        try { super.updateUIToSettingValues(); } finally { showGhostMode(); filterSettings(); }
    }

    @Override protected void updateUIAvailability() {
        restoreSearchRows();
        try { super.updateUIAvailability(); } finally { showGhostMode(); filterSettings(); }
    }

    /**
     * Ghost mode shows on only while every switch it turns is on, whichever way they got there, and
     * the row under it names the ones it turns.
     */
    private void showGhostMode() {
        List<BooleanSetting> picked = GhostMode.picked(ghostSwitches);
        SwitchPreference row = ghostMode;
        if (row != null) row.setChecked(GhostMode.on(picked));
        Row picks = ghostPicks;
        if (picks == null) return;
        if (picked.size() == ghostSwitches.size()) {
            picks.setSummary(L10n.t("All of them. Tap to leave some out, and Ghost mode leaves those as you set them."));
            return;
        }
        StringBuilder names = new StringBuilder();
        for (BooleanSetting setting : picked) {
            if (names.length() > 0) names.append(", ");
            names.append(ghostName(setting));
        }
        picks.setSummary(L10n.f("%1$s. The others stay as you set them.", names.toString()));
    }

    /** [setting]'s title as its own row shows it. */
    private CharSequence ghostName(BooleanSetting setting) {
        CharSequence name = ghostNames.get(setting.key);
        if (name != null) return name;
        Preference own = findPreference(setting.key);
        return own == null || own.getTitle() == null ? setting.key : own.getTitle();
    }

    /** The row under Messages that opens the list of message box buttons to show (#217). */
    Row messageBoxRow(Context context) {
        Row row = new Row(context);
        row.setKey(Settings.HIDDEN_MESSAGE_BOX_BUTTONS.key);
        row.setPersistent(false);
        row.setTitle(L10n.t("Buttons in the message box"));
        row.setSummary(messageBoxSummary(Settings.HIDDEN_MESSAGE_BOX_BUTTONS.savedValue()));
        row.setOnPreferenceClickListener(tapped -> {
            pickMessageBoxButtons(row);
            return true;
        });
        return row;
    }

    /** What the buttons left off in [saved] do, for the row's summary, with the plus menu switch as saved. */
    static String messageBoxSummary(String saved) {
        return messageBoxSummary(saved, PatchFamily.PLUS_MENU.inBuild() && Settings.MOVE_TO_PLUS_MENU.savedValue());
    }

    /**
     * The summary for [saved] with Gallery and microphone in the plus menu [moved] or not. Moved, the
     * bar leaves Gallery and Voice message out whatever's ticked here ({@link MessageBox}), so the
     * summary says where they went rather than calling them shown or left off.
     */
    static String messageBoxSummary(String saved, boolean moved) {
        Set<String> hidden = MessageBox.parse(saved);
        List<String> labels = new ArrayList<>();
        for (String name : MessageBox.OFFERED) {
            if (moved && ("GALLERY".equals(name) || "VOICE_MESSAGE".equals(name))) continue;
            if (hidden.contains(name)) labels.add(L10n.isolate(messageBoxLabel(name)));
        }
        if (labels.isEmpty()) {
            return moved
                    ? L10n.t("Gallery and Voice message are in the plus menu while Gallery and microphone in the plus "
                            + "menu is on. The text box and Send always stay.")
                    : L10n.t("Every button in the message box shows. Untick the ones you don't use. The text box and Send always stay.");
        }
        String left = String.join(", ", labels);
        return moved
                ? L10n.f("Left off the message box: %1$s. Gallery and Voice message are in the plus menu while Gallery "
                        + "and microphone in the plus menu is on. The text box and Send stay.", left)
                : L10n.f("Left off the message box: %1$s. The text box and Send stay.", left);
    }

    /** What the message box list calls the button Instagram names [name]. */
    static String messageBoxLabel(String name) {
        switch (name) {
            case "GALLERY":
                return L10n.t("Gallery");
            case "STICKER":
                return L10n.t("Stickers");
            default:
                return L10n.t("Voice message");
        }
    }

    /**
     * Each button Instagram can show in a chat's message box, ticked while it shows. A change saves
     * at once and shows the next time a chat opens. The text box and Send aren't listed.
     */
    void pickMessageBoxButtons(Row row) {
        Context context = getActivity();
        if (context == null) return;
        Set<String> hidden = new LinkedHashSet<>(MessageBox.parse(Settings.HIDDEN_MESSAGE_BOX_BUTTONS.savedValue()));
        List<String> names = MessageBox.OFFERED;
        CharSequence[] labels = new CharSequence[names.size()];
        boolean[] shown = new boolean[names.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = messageBoxLabel(names.get(i));
            shown[i] = !hidden.contains(names.get(i));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(L10n.t("Buttons in the message box"));
        builder.setMultiChoiceItems(labels, shown, (dialog, which, on) -> {
            if (!allowMutation(row, true)) {
                setChecked((AlertDialog) dialog, which, !hidden.contains(names.get(which)));
                return;
            }
            if (on) hidden.remove(names.get(which));
            else hidden.add(names.get(which));
            Settings.HIDDEN_MESSAGE_BOX_BUTTONS.save(MessageBox.join(hidden));
            row.setSummary(messageBoxSummary(Settings.HIDDEN_MESSAGE_BOX_BUTTONS.savedValue()));
        });
        AlertDialog dialog = show(builder.setNeutralButton(L10n.t("Show all"), null)
                .setPositiveButton(L10n.t("OK"), null));
        if (dialog == null) return;
        // A click listener of its own, so Show all ticks every button without closing the list.
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(tapped -> {
            if (!allowMutation(row, true)) return;
            hidden.clear();
            Settings.HIDDEN_MESSAGE_BOX_BUTTONS.save("");
            row.setSummary(messageBoxSummary(""));
            for (int i = 0; i < names.size(); i++) dialog.getListView().setItemChecked(i, true);
        });
    }

    /** The row under Feed that opens the list of items to keep in the three-dot menus (#697, #707). */
    Row menuItemsRow(Context context) {
        Row row = new Row(context);
        row.setKey(Settings.HIDDEN_MENU_ITEMS.key);
        row.setPersistent(false);
        row.setTitle(L10n.t("Items in the three-dot menus"));
        row.setSummary(menuItemsSummary(Settings.HIDDEN_MENU_ITEMS.savedValue()));
        row.setOnPreferenceClickListener(tapped -> {
            pickMenuItems(row);
            return true;
        });
        return row;
    }

    /** What the items left out in [saved] do, for the row's summary. */
    static String menuItemsSummary(String saved) {
        Set<String> hidden = MenuItems.parse(saved);
        List<String> labels = new ArrayList<>();
        for (String name : MenuItems.OFFERED) {
            if (hidden.contains(name)) labels.add(L10n.isolate(menuItemLabel(name)));
        }
        if (labels.isEmpty()) {
            return L10n.t("Every item shows. Untick the ones you don't want in the menus behind the three dots on "
                    + "posts, reels and stories.");
        }
        String left = String.join(", ", labels);
        return L10n.f("Left off the three-dot menus: %1$s.", left);
    }

    /** What the three-dot menu list calls the item the setting names [name]. */
    static String menuItemLabel(String name) {
        switch (name) {
            case "REPORT":
                return L10n.t("Report");
            case "MUTE":
                return L10n.t("Mute");
            case "UNFOLLOW":
                return L10n.t("Unfollow");
            case "NOT_INTERESTED":
                return L10n.t("Not interested");
            case "INTERESTED":
                return L10n.t("Interested");
            case "WHY":
                return L10n.t("Why you're seeing this post");
            case "PLAYBACK_SPEED":
                return L10n.t("Playback speed");
            case "REMIX":
                return L10n.t("Remix and sequence");
            case "REFERENCE":
                return L10n.t("Use as reference");
            case "TEMPLATE":
                return L10n.t("Use template");
            case "GET_EDITS":
                return L10n.t("Get Edits");
            case "LOOP":
                return L10n.t("Loop this video");
            case "SAVE":
                return L10n.t("Save");
            case "DOWNLOAD":
                return L10n.t("Download");
            case "DOWNLOAD_PHOTO":
                return L10n.t("Download as photo");
            case "DOWNLOAD_VIDEO":
                return L10n.t("Download as video");
            case "DOWNLOAD_COVER":
                return L10n.t("Download cover");
            case "DETAILS":
                return L10n.t("Details");
            case "ABOUT":
                return L10n.t("About this account");
            case "BLOCK":
                return L10n.t("Block");
            case "HIDE_ACCOUNT":
                return L10n.t("Hide posts from this account");
            case "STORY_LOOP":
                return L10n.t("Loop this story");
            case "PRIORITY":
                return L10n.t("Add to Stories priority");
            default:
                return L10n.t("Playback");
        }
    }

    /**
     * Each item the three-dot menus can leave out, ticked while it shows. A change saves at once and
     * shows the next time a menu opens.
     */
    void pickMenuItems(Row row) {
        Context context = getActivity();
        if (context == null) return;
        Set<String> hidden = new LinkedHashSet<>(MenuItems.parse(Settings.HIDDEN_MENU_ITEMS.savedValue()));
        List<String> names = MenuItems.OFFERED;
        CharSequence[] labels = new CharSequence[names.size()];
        boolean[] shown = new boolean[names.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = menuItemLabel(names.get(i));
            shown[i] = !hidden.contains(names.get(i));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(L10n.t("Items in the three-dot menus"));
        builder.setMultiChoiceItems(labels, shown, (dialog, which, on) -> {
            if (!allowMutation(row, true)) {
                setChecked((AlertDialog) dialog, which, !hidden.contains(names.get(which)));
                return;
            }
            if (on) hidden.remove(names.get(which));
            else hidden.add(names.get(which));
            Settings.HIDDEN_MENU_ITEMS.save(MenuItems.join(hidden));
            row.setSummary(menuItemsSummary(Settings.HIDDEN_MENU_ITEMS.savedValue()));
        });
        AlertDialog dialog = show(builder.setNeutralButton(L10n.t("Show all"), null)
                .setPositiveButton(L10n.t("OK"), null));
        if (dialog == null) return;
        // A click listener of its own, so Show all ticks every item without closing the list.
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(tapped -> {
            if (!allowMutation(row, true)) return;
            hidden.clear();
            Settings.HIDDEN_MENU_ITEMS.save("");
            row.setSummary(menuItemsSummary(""));
            for (int i = 0; i < names.size(); i++) dialog.getListView().setItemChecked(i, true);
        });
    }

    /** The row under Profiles that opens the list of tabs to show on your own profile (#296). */
    Row ownProfileTabsRow(Context context) {
        return profileTabsRow(context, true);
    }

    /** The row under Profiles that opens the list of tabs to show on other people's profiles (#254, #296). */
    Row otherProfileTabsRow(Context context) {
        return profileTabsRow(context, false);
    }

    private Row profileTabsRow(Context context, boolean own) {
        Row row = new Row(context);
        row.setKey(own ? Settings.HIDDEN_OWN_PROFILE_TABS.key : Settings.HIDDEN_PROFILE_TABS.key);
        row.setPersistent(false);
        row.setTitle(profileTabsTitle(own));
        row.setSummary(profileTabsSummary(own, savedProfileTabs(own)));
        row.setOnPreferenceClickListener(tapped -> {
            pickProfileTabs(row, own);
            return true;
        });
        return row;
    }

    /** The row's and the list's title, for your own profile when [own], else other people's. */
    static String profileTabsTitle(boolean own) {
        return own ? L10n.t("Tabs on your profile") : L10n.t("Tabs on other profiles");
    }

    /** What the user picked to leave off your own profile when [own], else off other people's. */
    static String savedProfileTabs(boolean own) {
        return own ? ProfileTabs.savedForOwn() : Settings.HIDDEN_PROFILE_TABS.savedValue();
    }

    /** What the tabs left off in [saved] do, for the row's summary, on your own profile when [own]. */
    static String profileTabsSummary(boolean own, String saved) {
        Set<String> hidden = ProfileTabs.parse(saved);
        if (hidden.isEmpty()) {
            return own
                    ? L10n.t("Every tab shows on your profile. Untick the ones you don't want. The grid of posts always stays.")
                    : L10n.t("Every tab shows on other people's profiles. Untick the ones you don't want. The grid of posts always stays.");
        }
        List<String> labels = new ArrayList<>();
        for (String key : ProfileTabs.OFFERED) {
            if (hidden.contains(key)) labels.add(L10n.isolate(profileTabLabel(key)));
        }
        String left = String.join(", ", labels);
        return own
                ? L10n.f("Left off your profile: %1$s. The grid of posts stays.", left)
                : L10n.f("Left off other profiles: %1$s. The grid of posts stays.", left);
    }

    /** What the profile tabs list calls the tab Instagram keys [key]. */
    static String profileTabLabel(String key) {
        switch (key) {
            case "profile_fan_club_grid":
                return L10n.t("Subscriptions");
            case "profile_clips":
                return L10n.t("Reels");
            case "profile_reposts":
                return L10n.t("Reposts");
            default:
                return L10n.t("Tagged");
        }
    }

    /**
     * Each tab Instagram can show on a profile, ticked while it shows, for your own profile when
     * [own], else other people's. A change saves at once and shows the next time a profile opens.
     * The grid of posts isn't listed, since it's what stays.
     */
    void pickProfileTabs(Row row, boolean own) {
        Context context = getActivity();
        if (context == null) return;
        Set<String> hidden = new LinkedHashSet<>(ProfileTabs.parse(savedProfileTabs(own)));
        List<String> keys = ProfileTabs.OFFERED;
        CharSequence[] names = new CharSequence[keys.size()];
        boolean[] checked = new boolean[keys.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = profileTabLabel(keys.get(i));
            checked[i] = !hidden.contains(keys.get(i));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(profileTabsTitle(own));
        builder.setMultiChoiceItems(names, checked, (dialog, which, on) -> {
            if (!allowMutation(row, true)) {
                setChecked((AlertDialog) dialog, which, !hidden.contains(keys.get(which)));
                return;
            }
            if (on) hidden.remove(keys.get(which));
            else hidden.add(keys.get(which));
            if (own) ProfileTabs.saveOwn(hidden);
            else ProfileTabs.saveOthers(hidden);
            row.setSummary(profileTabsSummary(own, savedProfileTabs(own)));
        });
        show(builder.setPositiveButton(L10n.t("OK"), null));
    }

    /** The row under Ghost mode that opens the list of switches it turns (#251). */
    Row ghostPicksRow(Context context) {
        Row row = new Row(context);
        row.setKey(GHOST_PICKS_ROW);
        row.setPersistent(false);
        row.setTitle(L10n.t("Switches Ghost mode turns"));
        row.setOnPreferenceClickListener(tapped -> {
            pickGhostMode();
            return true;
        });
        return row;
    }

    /**
     * Each ghost switch with a check on the ones Ghost mode turns. A change saves at once, and the
     * Ghost mode row, the Home button and the inbox long press all follow it. The last check can't
     * come off, since Ghost mode would have nothing to turn.
     */
    void pickGhostMode() {
        Context context = getActivity();
        List<BooleanSetting> switches = ghostSwitches;
        if (context == null || switches.isEmpty()) return;
        Set<String> picked = new LinkedHashSet<>();
        for (BooleanSetting setting : GhostMode.picked(switches)) picked.add(setting.key);
        CharSequence[] names = new CharSequence[switches.size()];
        boolean[] checked = new boolean[switches.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = ghostName(switches.get(i));
            checked[i] = picked.contains(switches.get(i).key);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(L10n.t("Switches Ghost mode turns"));
        builder.setMultiChoiceItems(names, checked, (dialog, which, on) -> {
            String key = switches.get(which).key;
            if (!allowMutation()) {
                setChecked((AlertDialog) dialog, which, picked.contains(key));
                return;
            }
            if (!on && picked.size() == 1 && picked.contains(key)) {
                checked[which] = true;
                setChecked((AlertDialog) dialog, which, true);
                Utils.showToastShort(L10n.t("Ghost mode needs at least one switch to turn."));
                return;
            }
            if (on) picked.add(key);
            else picked.remove(key);
            GhostMode.pick(switches, picked);
            showGhostMode();
        });
        show(builder.setPositiveButton(L10n.t("OK"), null));
    }

    /**
     * Ghost mode's row. It keeps no value: a tap saves the new value to each switch it turns, through
     * that switch's own row when the page has one, so each row shows its new state, and a toast
     * says what happened.
     */
    SwitchPreference ghostModeRow(Context context) {
        SwitchPreference row = new Toggle(context);
        row.setPersistent(false);
        row.setTitle(L10n.t("Ghost mode"));
        row.setSummary(L10n.t("Turns the switches that keep what you do to yourself on or off in one go, like View "
                + "stories anonymously and Hide that you're typing. Each one keeps its own switch."));
        row.setOnPreferenceChangeListener((preference, value) -> {
            boolean on = Boolean.TRUE.equals(value);
            List<BooleanSetting> picked = GhostMode.picked(ghostSwitches);
            for (BooleanSetting setting : picked) {
                Preference own = findPreference(setting.key);
                if (own instanceof TwoStatePreference && own.isPersistent()) ((TwoStatePreference) own).setChecked(on);
                else setting.save(on);
            }
            showGhostMode();
            Utils.showToastShort(GhostMode.message(picked, on,
                    L10n.t("Ghost mode partly changed. Check the switches below.")));
            return false;
        });
        row.setChecked(GhostMode.on(GhostMode.picked(ghostSwitches)));
        mark(row, SettingsIcons.HIDDEN);
        return row;
    }

    /**
     * [text] as search compares it: no accents o