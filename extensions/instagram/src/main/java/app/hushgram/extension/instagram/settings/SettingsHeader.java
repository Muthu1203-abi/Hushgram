
/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 */
package app.hushgram.extension.instagram.settings;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import app.hushgram.extension.instagram.misc.AppIcon;
import app.hushgram.extension.instagram.misc.AppIconChoice;
import app.hushgram.extension.shared.L10n;
import app.hushgram.extension.shared.Utils;

/**
 * Premium Liquid Glass header for HushGram settings.
 */
final class SettingsHeader extends Preference {

    static final String KEY = "hushgram_settings_header";

    static final String ICON = "hushgram-header-icon";
    static final String NAME = "hushgram-header-name";
    static final String VERSION = "hushgram-header-version";

    SettingsHeader(Context context) {
        super(context);
        setKey(KEY);
        setTitle("Muthu12");
        setPersistent(false);
        setSelectable(false);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {

        Context context = getContext();

        float density = context.getResources()
                .getDisplayMetrics().density;

        ScreenColors colors = ScreenColors.current();

        // Main container
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        column.setPadding(
                Math.round(24 * density),
                Math.round(32 * density),
                Math.round(24 * density),
                Math.round(24 * density)
        );

        // Liquid Glass card
        LinearLayout glass = new LinearLayout(context);
        glass.setOrientation(LinearLayout.VERTICAL);
        glass.setGravity(Gravity.CENTER_HORIZONTAL);

        glass.setPadding(
                Math.round(24 * density),
                Math.round(24 * density),
                Math.round(24 * density),
                Math.round(22 * density)
        );

        glass.setBackground(
                LiquidGlassSurfaces.card(
                        context,
                        false,
                        colors.accent
                )
        );

        // App icon
        ImageView icon = new ImageView(context);
        icon.setTag(ICON);
        icon.setImageDrawable(icon(context, colors));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImportantForAccessibility(
                View.IMPORTANT_FOR_ACCESSIBILITY_NO
        );

        int size = Math.round(76 * density);

        glass.addView(
                icon,
                new LinearLayout.LayoutParams(size, size)
        );

        // HushGram branding
        TextView name = new TextView(context);
        name.setTag(NAME);
        name.setText("Muthu12");
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);

        name.setTypeface(
                Typeface.create(
                        "sans-serif-medium",
                        Typeface.NORMAL
                )
        );

        name.setGravity(Gravity.CENTER);
        name.setAccessibilityHeading(true);

        ShimmerTitle.wear(name);

        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        nameParams.topMargin = Math.round(14 * density);

        glass.addView(name, nameParams);

        // Version information
        TextView version = new TextView(context);
        version.setTag(VERSION);

        version.setText(
                L10n.f(
                        "Version %1$s for Instagram %2$s",
                        L10n.isolate(Utils.getPatchesReleaseVersion()),
                        L10n.isolate(Utils.getAppVersionName())
                )
        );

        version.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        version.setTextColor(colors.summary);
        version.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams versionParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        versionParams.topMargin = Math.round(6 * density);

        glass.addView(version, versionParams);

        // Add glass card to main container
        column.addView(
                glass,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        return column;
    }

    /**
     * HushGram launcher icon or fallback patched icon.
     */
    static Drawable icon(Context context, ScreenColors colors) {

        Drawable own = AppIcon.preview(
                context,
                AppIconChoice.HUSHGRAM
        );

        return own != null
                ? own
                : SettingsIcons.icon(
                        context,
                        SettingsIcons.PATCHED,
                        colors.heading
                );
    }
}
