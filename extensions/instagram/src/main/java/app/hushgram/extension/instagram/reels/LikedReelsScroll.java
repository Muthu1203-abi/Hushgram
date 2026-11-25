/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.reels;

import app.hushgram.extension.instagram.settings.FamilyNames;
import app.hushgram.extension.shared.diagnostics.HookStatus;
import app.hushgram.extension.shared.settings.HushgramPause;

/** Uses Instagram's clips-specific liked feed when a liked reel opens in the full-screen viewer. */
public final class LikedReelsScroll {
    private static final String CLIPS_LIKED_SOURCE = "CLIPS_FEED_LIKED";

    private LikedReelsScroll() {
    }

    /** Returns Instagram's clips-liked source for this selected patch. */
    public static Object source(Object original) {
        try {
            if (original == null || HushgramPause.isPaused()) return original;

            Object[] sources = original.getClass().getEnumConstants();
            if (sources == null) return original;
            for (Object source : sources) {
                if (source instanceof Enum && CLIPS_LIKED_SOURCE.equals(((Enum<?>) source).name())) return source;
            }
        } catch (Throwable failure) {
            HookStatus.threw(FamilyNames.LIKED_REELS_SCROLL, "liked reels source", failure);
        }
        return original;
    }
}
