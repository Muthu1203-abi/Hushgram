/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.misc;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import app.hushgram.extension.instagram.settings.Settings;
import app.hushgram.extension.shared.Utils;

/**
 * Shared drawing and navigation bar helpers retained for backward compatibility with
 * FrostedBackdrop, GlassTopBar, and FullScreenBars.
 * The Tab Bar itself is handled by LiquidGlassNavBarPatch.
 */
public final class GlassTabBar {
    public static final String TAB_BAR = "tab_bar";
    public static final String TAB_BAR_SHADOW = "tab_bar_shadow";
    public static final String CONTENT = "layout_container_main";
    public static final String SWIPE_PAGER = "swipeable_tab_view_pager";
    public static final String REELS_ROOT = "root_clips_layout";

    public static final int BLUR_DP = 8;
    public static final int BLUR_TRIES = 3;
    public static final long BLUR_REST_MS = 5000L;
    public static final long RECORD_EVERY_MS = 250L;
    public static final long REELS_LOOK_MS = 1000L;

    @Nullable public static Boolean animationsOnForTests;

    private static final Map<String, Integer> IDS = new HashMap<>();

    private GlassTabBar() {}

    public static void install(Context context) {
        app.southstar21.extension.instagram.liquidglass.LiquidGlassNavBarPatch.install(context);
    }

    public static boolean animationsOn() {
        Boolean forTests = animationsOnForTests;
        if (forTests != null) return forTests;
        return ValueAnimator.areAnimatorsEnabled()
                && (!Utils.settingsReady() || !Settings.GLASS_TAB_BAR_REDUCED_MOTION.get());
    }

    public static int highlight(int base) {
        return isDark(base) ? 0x40ffffff : 0x2a787880;
    }

    public static int edge(int base) {
        return isDark(base) ? 0x33ffffff : 0x1f000000;
    }

    public static void diagnose(String what, @Nullable Throwable failure) {}

    public static int id(Context context, String name) {
        synchronized (IDS) {
            Integer found = IDS.get(name);
            if (found == null) {
                found = context.getResources().getIdentifier(name, "id", context.getPackageName());
                IDS.put(name, found);
            }
            return found;
        }
    }

    @Nullable
    public static View find(View root, String name) {
        if (root == null) return null;
        int id = id(root.getContext(), name);
        return id == 0 ? null : root.findViewById(id);
    }

    public static final class BlurTries {
        private int failedInARow;
        private long restUntil = Long.MIN_VALUE;

        public boolean resting(long now) {
            return now < restUntil;
        }

        long waitLeft(long now) {
            return resting(now) ? restUntil - now : 0;
        }

        public boolean failed(long now) {
            if (++failedInARow < BLUR_TRIES) return false;
            restUntil = now + BLUR_REST_MS;
            return true;
        }

        public void worked() {
            failedInARow = 0;
            restUntil = Long.MIN_VALUE;
        }
    }

    public static boolean surfacePaths(ViewGroup parent, int count, Set<View> into) {
        into.clear();
        boolean found = false;
        for (int i = 0; i < count; i++) found |= findSurfaces(parent.getChildAt(i), into, 0);
        return found;
    }

    private static boolean findSurfaces(View view, Set<View> into, int depth) {
        if (view instanceof android.view.SurfaceView) {
            into.add(view);
            return true;
        }
        if (!(view instanceof ViewGroup) || depth > 100) return false;
        ViewGroup group = (ViewGroup) view;
        boolean found = false;
        for (int i = 0, n = group.getChildCount(); i < n; i++) found |= findSurfaces(group.getChildAt(i), into, depth + 1);
        return found;
    }

    public static void drawLeavingOutSurfaces(Canvas canvas, View view, Set<View> leftOut, int depth) {
        if (leftOut.contains(view)) return;
        if (!(view instanceof ViewGroup) || depth > 100) {
            view.draw(canvas);
            return;
        }
        ViewGroup group = (ViewGroup) view;
        int count = group.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = group.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE || child.getAlpha() == 0) continue;
            int save = canvas.save();
            canvas.translate(child.getLeft(), child.getTop());
            drawLeavingOutSurfaces(canvas, child, leftOut, depth + 1);
            canvas.restoreToCount(save);
        }
    }

    public static int tint(int base, boolean blurred, float scale) {
        int alpha = Math.round((blurred ? 0xCC : 0xF0) * Math.max(0f, Math.min(1.5f, scale)));
        return (base & 0x00FFFFFF) | (Math.min(255, alpha) << 24);
    }

    public static float opacityScale() {
        return 1.0f;
    }

    public static boolean isDark(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 < 128;
    }

    public static int navBarHeight(int height) {
        return height;
    }

    public static int navBarHeight(int height, int hidden) {
        return height;
    }

    public static int navBarHeightOverTheKeyboard(int height) {
        return height;
    }

    public static void heightsTold() {}

    @Nullable
    public static Activity activityInFront() {
        return null;
    }

    public static boolean wearsGlass(View view) {
        return false;
    }

    public static boolean wearsFloatingGlass(View view) {
        return false;
    }

    @Nullable
    public static View storyShownOnTop(View decor) {
        return null;
    }

    public static void restoreAll() {}
}
