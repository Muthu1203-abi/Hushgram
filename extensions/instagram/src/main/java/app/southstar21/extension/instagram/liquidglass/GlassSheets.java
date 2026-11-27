package app.southstar21.extension.instagram.liquidglass;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Gives Instagram's bottom sheets (comments, the share sheet, a post's menu and the other menus that slide
 * up) a Liquid Glass background: a frosted, see-through surface with rounded top corners and a light rim,
 * dark or light with the app. On Android 12 and newer the screens behind an open sheet are blurred too, so
 * the sheet reads as glass over the page rather than a pane over a dimmed one.
 *
 * <p>Instagram puts every bottom sheet in one holder over the screens ({@link #HOLDER}). On each layout of
 * the window the holder's sheets are looked at: the first wide view with a background, a few levels in, is
 * the sheet's surface and wears the glass, and wide views under it painted one solid color are cleared so
 * the glass shows through. Each view's own background is kept and given back when the sheet closes, when
 * the switch goes off, or when anything goes wrong. Nothing in Instagram's code changes.
 */
final class GlassSheets implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "GlassSheets";
    private static final String PACKAGE = "com.instagram.android";
    /** Where Instagram puts a bottom sheet over the screens. */
    static final String HOLDER = "layout_container_bottom_sheet";
    /** The dimmed backdrop Instagram lays behind a sheet in the holder. */
    static final String DIMMER = "background_dimmer";

    /** How many levels into a sheet its surface and solid layers are looked for. */
    private static final int DEPTH = 4;
    /** A view this share of the holder's width or wider is part of the sheet's surface, not a row in it. */
    private static final float WIDE = 0.9f;
    private static final float CORNER_DP = 22f;
    private static final float BLUR_DP = 18f;

    /** Glass colors, top and bottom of the sheet, and the rim, for dark and light. */
    private static final int DARK_TOP = 0xE62A3044, DARK_BOTTOM = 0xEB141926, DARK_RIM = 0x40FFFFFF;
    private static final int LIGHT_TOP = 0xE6FFFFFF, LIGHT_BOTTOM = 0xEBF1F3F8, LIGHT_RIM = 0x1F000000;

    private static final Map<Activity, GlassSheets> WATCHING = new WeakHashMap<>();

    private final WeakReference<Activity> activity;
    private final int holderId, dimmerId;
    private final float density;
    /** Each view this changed, with the background it had. */
    private final WeakHashMap<View, Drawable> before = new WeakHashMap<>();
    /** The glass this gave each surface, to tell when Instagram has put another background on. */
    private final WeakHashMap<View, Drawable> worn = new WeakHashMap<>();
    /** The screens this blurred behind an open sheet. */
    private final WeakHashMap<View, Boolean> blurred = new WeakHashMap<>();
    private WeakReference<ViewGroup> holder = new WeakReference<>(null);

    private GlassSheets(Activity activity) {
        this.activity = new WeakReference<>(activity);
        holderId = activity.getResources().getIdentifier(HOLDER, "id", PACKAGE);
        dimmerId = activity.getResources().getIdentifier(DIMMER, "id", PACKAGE);
        density = activity.getResources().getDisplayMetrics().density;
    }

    /** Whether the switch is on: the Liquid Glass bar is in use and Glass sheets is on. */
    static boolean on() {
        try {
            return LiquidGlassNavBarPatch.isActive() && LiquidGlassNavBarPatch.GLASS_SHEETS.get();
        } catch (Throwable t) {
            return false;
        }
    }

    /** Starts watching [activity]'s window for sheets, once. Never throws. */
    static void watch(Activity activity) {
        try {
            if (activity == null || WATCHING.containsKey(activity)) return;
            GlassSheets sheets = new GlassSheets(activity);
            if (sheets.holderId == 0) return;
            WATCHING.put(activity, sheets);
            activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(sheets);
        } catch (Throwable t) {
            Log.e(TAG, "Could not watch for sheets", t);
        }
    }

    /** Stops watching [activity] and gives everything back. Never throws. */
    static void forget(Activity activity) {
        try {
            GlassSheets sheets = WATCHING.remove(activity);
            if (sheets == null) return;
            sheets.restoreAll();
            activity.getWindow().getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(sheets);
        } catch (Throwable t) {
            Log.e(TAG, "Could not stop watching for sheets", t);
        }
    }

    @Override
    public void onGlobalLayout() {
        try {
            Activity owner = activity.get();
            if (owner == null) return;
            if (!on()) {
                restoreAll();
                return;
            }
            ViewGroup sheets = holder(owner);
            List<View> open = new ArrayList<>();
            if (sheets != null) {
                for (int i = 0; i < sheets.getChildCount(); i++) {
                    View sheet = sheets.getChildAt(i);
                    if (sheet.getId() == dimmerId || sheet.getVisibility() != View.VISIBLE) continue;
                    View surface = surface(sheet, sheets.getWidth());
                    if (surface == null) continue;
                    open.add(surface);
                    wear(surface);
                    clearSolids(surface, surface.getWidth(), DEPTH);
                }
            }
            // Views of sheets no longer up get their own backgrounds back.
            for (View view : new ArrayList<>(before.keySet())) {
                if (view != null && !view.isAttachedToWindow()) restore(view);
            }
            blurBehind(sheets, !open.isEmpty());
        } catch (Throwable t) {
            Log.e(TAG, "Glass sheets failed, giving the sheets back", t);
            restoreAll();
        }
    }

    private ViewGroup holder(Activity owner) {
        ViewGroup known = holder.get();
        if (known != null && known.isAttachedToWindow()) return known;
        View found = owner.getWindow().getDecorView().findViewById(holderId);
        ViewGroup group = found instanceof ViewGroup ? (ViewGroup) found : null;
        holder = new WeakReference<>(group);
        return group;
    }

    /** The sheet's surface: [sheet] or the first view under it, a few levels in, that is wide and has a background. */
    private View surface(View sheet, int holderWidth) {
        if (holderWidth <= 0) return null;
        List<View> level = new ArrayList<>();
        level.add(sheet);
        for (int depth = 0; depth < DEPTH && !level.isEmpty(); depth++) {
            List<View> next = new ArrayList<>();
            for (View view : level) {
                if (view.getVisibility() != View.VISIBLE) continue;
                boolean wide = view.getWidth() >= holderWidth * WIDE;
                if (wide && (view.getBackground() != null || worn.containsKey(view))) return view;
                if (view instanceof ViewGroup) {
                    ViewGroup group = (ViewGroup) view;
                    for (int i = 0; i < group.getChildCount(); i++) next.add(group.getChildAt(i));
                }
            }
            level = next;
        }
        return null;
    }

    /** Puts the glass on [surface], keeping its own background, unless it already wears this glass. */
    private void wear(View surface) {
        Drawable glass = worn.get(surface);
        if (glass != null && surface.getBackground() == glass) return;
        if (!before.containsKey(surface) || glass != null) before.put(surface, surface.getBackground());
        glass = glass(surface);
        worn.put(surface, glass);
        surface.setBackground(glass);
    }

    /** Clears wide views under [view] painted one opaque color, so the glass shows through them. */
    private void clearSolids(View view, int width, int depth) {
        if (depth <= 0 || !(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) continue;
            Drawable background = child.getBackground();
            if (background instanceof ColorDrawable && ((ColorDrawable) background).getAlpha() == 255
                    && child.getWidth() >= width * WIDE) {
                if (!before.containsKey(child)) before.put(child, background);
                child.setBackground(null);
            }
            clearSolids(child, width, depth - 1);
        }
    }

    private Drawable glass(View surface) {
        boolean dark = (surface.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        GradientDrawable glass = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                dark ? new int[]{DARK_TOP, DARK_BOTTOM} : new int[]{LIGHT_TOP, LIGHT_BOTTOM});
        float corner = CORNER_DP * density;
        glass.setCornerRadii(new float[]{corner, corner, corner, corner, 0, 0, 0, 0});
        glass.setStroke(Math.max(1, Math.round(density)), dark ? DARK_RIM : LIGHT_RIM);
        return glass;
    }

    /** Blurs the screens beside [sheets] while a sheet is up (Android 12 and newer), and takes the blur off after. */
    private void blurBehind(ViewGroup sheets, boolean up) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        if (!up || sheets == null || !(sheets.getParent() instanceof ViewGroup)) {
            unblur();
            return;
        }
        ViewGroup parent = (ViewGroup) sheets.getParent();
        float radius = BLUR_DP * density;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View screen = parent.getChildAt(i);
            if (screen == sheets || blurred.containsKey(screen) || screen.getVisibility() != View.VISIBLE) continue;
            screen.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP));
            blurred.put(screen, Boolean.TRUE);
        }
    }

    private void unblur() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || blurred.isEmpty()) return;
        for (View screen : new ArrayList<>(blurred.keySet())) {
            if (screen != null) screen.setRenderEffect(null);
        }
        blurred.clear();
    }

    private void restore(View view) {
        Drawable own = before.remove(view);
        Drawable glass = worn.remove(view);
        // Only what still wears this change goes back; a background Instagram has set since stays.
        if (glass != null ? view.getBackground() == glass : view.getBackground() == null) view.setBackground(own);
    }

    private void restoreAll() {
        for (View view : new ArrayList<>(before.keySet())) {
            if (view != null) restore(view);
        }
        before.clear();
        worn.clear();
        unblur();
    }
}
