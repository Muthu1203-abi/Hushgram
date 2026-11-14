/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.misc;

import app.hushgram.extension.instagram.settings.Settings;

/** Read-only bridge used to label MetaConfig rows from Instagram's active Quick Experiments and control developer options access. */
public final class DeveloperOptions {
    private DeveloperOptions() {
    }

    /** Whether long-pressing the Home icon opens developer options. */
    public static boolean enableDevOptions() {
        return Settings.DEVELOPER_OPTIONS.value();
    }

    /** Whether long-pressing the Home icon directly opens MetaConfig / Quick Experiments. */
    public static boolean directlyOpenMetaConfig() {
        return Settings.DIRECTLY_OPEN_METACONFIG.value();
    }

    /** Returns Instagram's known parameter name, keeping its fallback for entries not in the experiment list. */
    public static String parameterLabel(Object entry, String fallback) {
        int config = getFlagConfigNative(entry);
        int index = getFlagIndexNative(entry);
        return app.hushgram.extension.instagram.entity.DeveloperOptions.parameterName(config, index, fallback);
    }

    /** Returns Instagram's known config name, keeping its fallback when this config is unknown. */
    public static String configLabel(Object entry, String fallback) {
        int config = getFlagConfigNative(entry);
        return app.hushgram.extension.instagram.entity.DeveloperOptions.configName(config, fallback);
    }

    /** Filled by the patch with a read of the schema entry's config number. */
    static int getFlagConfigNative(Object entry) {
        return -1;
    }

    /** Filled by the patch with a read of the schema entry's parameter index. */
    static int getFlagIndexNative(Object entry) {
        return -1;
    }
}
