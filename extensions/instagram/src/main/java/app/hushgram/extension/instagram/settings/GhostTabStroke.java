/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.settings;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import app.hushgram.extension.instagram.download.FeedDownloadButton;
import app.hushgram.extension.shared.Logger;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.diagnostics.HookStatus;
import app.hushgram.extension.shared.settings.BooleanSetting;
import app.hushgram.extension.shared.settings.HushgramPause;

/**
 * A ring round the tab whose long press turns Ghost mode on or off (#255), so the tab says Ghost
 * mode is on the way Home's header button does. It's drawn in the tint picked for the indicator, or
 * Instagram's own icon color when that's what was picked, and it's there only while Ghost mode is
 * on, HushGram isn't paused, and the tab is the one chosen for the long press.
 *
 * <p>The ring sits in the view's overlay, so it takes nothing from Instagram's icon, background,
 * ripple or selected capsule, and it never takes a touch. Every tab the tab bar factory binds is
 * watched ({@link #watch}), and a change of Ghost mode, its tint or the chosen tab repaints them at
 * once ({@link #refresh}), without waiting for Instagram to build the bar again. Main thread only.
 * Never throws into Instagram.
 */
final class GhostTabStroke {
    /** The name the diagnostic report counts this under, as the other Ghost mode hooks do. */
    static final String ROUTE = GhostHeaderButton.ROUTE;
    static final String SHOWN = "Ghost tab stroke shown";
    static final String HIDDEN = "Ghost tab stroke taken off";

    /** The tabs watched, with the name the factory bound each to. A dropped tab goes with its view. */
    private static final Map<View, String> watched = new WeakHashMap<>();
    /** The ring each watched tab shows now, or none. */
    private static final Map<View, Ring> rings = new WeakHashMap<>();

    private GhostTabStroke() { }

    /** Watches [view] as Instagram's [tab], and puts the ring on it or takes it off now. */
    static void watch(@Nullable View view, @Nullable String tab) {
        try {
            if (view == null) return;
            synchronized (watched) {
                if (tab == null || tab.isEmpty()) watched.remove(view);
                else watched.put(view, tab);
            }
            place(view, tab);
        } catch (Throwable t) {
            HookStatus.threw(ROUTE, "ghost tab stroke", t);
        }
    }

    /** Watches [view] as Instagram's [tab], as the factory names it. */
    static void watch(@Nullable View view, @Nullable Object tab) {
        watch(view, tab instanceof Enum<?> ? ((Enum<?>) tab).name() : null);
    }

    /** Forgets [view]: it's no longer a tab of Instagram's. */
    static void forget(@Nullable View view) {
        try {
            if (view == null) return;
            synchronized (watched) {
                watched.remove(view);
            }
            take(view);
        } catch (Throwable t) {
            HookStatus.threw(ROUTE, "ghost tab stroke", t);
        }
    }

    /**
     * Puts the ring on each watched tab that should show it and takes it off the others. Called
     * after Ghost mode, its tint or the tab chosen for it changed. Never throws.
     */
    static void refresh() {
        try {
            List<View> views = new ArrayList<>();
            List<String> tabs = new ArrayList<>();
            synchronized (watched) {
                for (Map.Entry<View, String> entry : watched.entrySet()) {
                    views.add(entry.getKey());
                    tabs.add(entry.getValue());
                }
            }
            for (int i = 0; i < views.size(); i++) {
                place(views.get(i), tabs.get(i));
            }
        } catch (Throwable t) {
            HookStatus.threw(ROUTE, "ghost tab stroke", t);
            Logger.printException(() -> "Ghost mode: could not repaint a tab's stroke", t);
        }
    }

    /** Whether [view] shows the ring now. */
    static boolean showingForTests(@Nullable View view) {
        if (view == null) return false;
        synchronized (rings) {
            return rings.containsKey(view);
        }
    }

    /** The ring [view] shows now, or null. */
    @Nullable
    static Ring ringForTests(@Nullable View view) {
        if (view == null) return null;
        synchronized (rings) {
            return rings.get(view);
        }
    }

    /** Forgets every watched tab and takes every ring off. */
    static void forgetForTests() {
        synchronized (watched) {
            watched.clear();
        }
        List<View> views;
        synchronized (rings) {
            views = new ArrayList<>(rings.keySet());
        }
        for (View view : views) take(view);
    }

    private static void place(View view, String tab) {
        boolean wanted = wanted(tab);
        synchronized (rings) {
            Ring ring = rings.get(view);
            if (!wanted) {
                if (ring != null) {
                    view.getOverlay().remove(ring);
                    rings.remove(view);
                    HookStatus.counted(ROUTE, HIDDEN);
                }
                return;
            }
            if (ring == null) {
                ring = new Ring(view.getResources().getDisplayMetrics().density);
                view.getOverlay().add(ring);
                rings.put(view, ring);
                HookStatus.counted(ROUTE, SHOWN);
            }
            ring.color = color(view);
            ring.invalidateSelf();
        }
    }

    private static void take(View view) {
        synchronized (rings) {
            Ring ring = rings.remove(view);
            if (ring != null) view.getOverlay().remove(ring);
        }
    }

    /** Whether [tab] is the one whose long press turns Ghost mode, and Ghost mode is on now. */
    private static boolean wanted(String tab) {
        if (tab == null || !Utils.settingsReady() || HushgramPause.isPaused()) return false;
        if (!Settings.GHOST_TAB_TARGET.get().matches(tab)) return false;
        if (!GhostModeEntry.offeredNow()) return false;
        List<BooleanSetting> switches = GhostMode.picked(GhostMode.switches(PatchFamily.inThisBuild()));
        return GhostMode.on(switches);
    }

    /** The ring's color: the picked tint, or Instagram's own icon color when Instagram is picked. */
    private static int color(View view) {
        GhostIndicatorColor tint = Settings.GHOST_INDICATOR_COLOR.get();
        return tint == GhostIndicatorColor.INSTAGRAM ? FeedDownloadButton.iconColor(view.getContext()) : tint.color;
    }

    /**
     * The ring itself: a circle round the middle of the tab, inset so the stroke stays inside the
     * view and its bounds never change. Its color is read from the setting each time it's placed.
     */
    static final class Ring extends Drawable {
        private final float unit;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        /** The color the ring is drawn in now. */
        int color;

        Ring(float density) {
            unit = density;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2 * density);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty()) return;
            float inset = paint.getStrokeWidth() / 2f + unit;
            float radius = Math.min(bounds.width(), bounds.height()) / 2f - inset;
            if (radius <= 0) return;
            paint.setColor(color);
            canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), radius, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter filter) {
            paint.setColorFilter(filter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
