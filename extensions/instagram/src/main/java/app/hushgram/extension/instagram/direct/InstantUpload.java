/*
 * Copyright 2026 HushGram contributors
 * https://github.com/SysAdminDoc/HushGram
 * The gallery-to-QuickSnap handoff is adapted from InstaHacks (GPL-3.0):
 * https://github.com/xdreizein666/InstaHacks
 */
package app.hushgram.extension.instagram.direct;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import app.hushgram.extension.instagram.settings.FamilyNames;
import app.hushgram.extension.instagram.settings.SettingsStatus;
import app.hushgram.extension.shared.Logger;
import app.hushgram.extension.shared.Utils;
import app.hushgram.extension.shared.diagnostics.HookStatus;

/** Gallery photo handoff for Instagram's QuickSnap camera. */
public final class InstantUpload {
    private static final int PICK_REQUEST = 0x1E5A;
    private static final long PICK_TTL_MS = 5 * 60 * 1000L;
    private static final int MAX_EDGE = 2048;
    private static volatile WeakReference<Activity> resumed = new WeakReference<>(null);
    private static volatile WeakReference<Activity> host = new WeakReference<>(null);
    private static Bitmap pending;
    private static long pendingAt;
    private static boolean pickerOpen;
    private static boolean lifecycleInstalled;
    private static View button;
    private static View watchedCamera;
    private static boolean sentForCamera;

    private InstantUpload() { }

    /** Called by the app's existing settings bootstrap after patch status has been loaded. */
    public static synchronized void install(Context context) {
        Context application = context == null ? null : context.getApplicationContext();
        if (lifecycleInstalled || !(application instanceof Application) || !SettingsStatus.instantUpload()) return;
        lifecycleInstalled = true;
        ((Application) application).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity activity) {
                resumed = new WeakReference<>(activity);
                if (host.get() == activity && !pickerOpen) {
                    Utils.runOnMainThreadDelayed(() -> {
                        if (host.get() == activity && !cameraViewPresent(activity)) removeButton();
                    }, 100);
                }
            }
            @Override public void onActivityPaused(Activity activity) {
                if (resumed.get() == activity) resumed = new WeakReference<>(null);
                if (!pickerOpen && host.get() == activity) removeButton();
            }
            @Override public void onActivityDestroyed(Activity activity) {
                if (host.get() == activity) {
                    pickerOpen = false;
                    removeButton();
                    if (watchedCamera != null) watchedCamera.removeOnAttachStateChangeListener(cameraWatcher);
                    watchedCamera = null;
                    sentForCamera = false;
                    host = new WeakReference<>(null);
                }
            }
            @Override public void onActivityCreated(Activity a, android.os.Bundle b) { }
            @Override public void onActivityStarted(Activity a) { }
            @Override public void onActivityStopped(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a, android.os.Bundle b) { }
        });
    }

    /** Called from QuickSnap's no-argument camera lifecycle methods. */
    public static void cameraReady() {
        if (!SettingsStatus.instantUpload()) return;
        Activity activity = resumed.get();
        if (activity == null || !activity.getClass().getName().contains("ModalActivity")) return;
        Utils.runOnMainThreadDelayed(() -> showButton(activity), 150);
    }

    /** Opens Android's photo picker; the injected FragmentActivity result hook receives its result. */
    private static void showButton(Activity activity) {
        try {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            View decor = activity.getWindow().getDecorView();
            if (!(decor instanceof ViewGroup)) return;
            int cameraId = activity.getResources().getIdentifier("camera_view", "id", activity.getPackageName());
            View camera = cameraId == 0 ? null : decor.findViewById(cameraId);
            if (camera == null || !camera.isShown()) {
                if (host.get() == activity) removeButton();
                return;
            }
            if (host.get() != activity) removeButton();
            watchCamera(camera);
            if (sentForCamera) return;
            if (button != null && button.getParent() == decor) return;
            TextView chip = new TextView(activity);
            chip.setText("Gallery");
            chip.setTextColor(0xffffffff);
            chip.setTextSize(14);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(activity, 18), dp(activity, 11), dp(activity, 18), dp(activity, 11));
            chip.setBackground(makeChipBackground(activity));
            chip.setElevation(dp(activity, 8));
            chip.setContentDescription("Choose a photo from gallery for Instant");
            chip.setOnClickListener(view -> {
                try {
                    Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                    pick.setType("image/*");
                    pick.addCategory(Intent.CATEGORY_OPENABLE);
                    pickerOpen = true;
                    host = new WeakReference<>(activity);
                    activity.startActivityForResult(Intent.createChooser(pick, "Choose a photo"), PICK_REQUEST);
                } catch (Throwable failure) {
                    pickerOpen = false;
                    report("open gallery", failure);
                }
            });
            FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.END);
            layout.topMargin = dp(activity, 112);
            layout.setMarginEnd(dp(activity, 18));
            ((ViewGroup) decor).addView(chip, layout);
            host = new WeakReference<>(activity);
            button = chip;
        } catch (Throwable failure) {
            report("show gallery button", failure);
        }
    }

    /** Receives only the unique request launched by this patch. */
    public static void onActivityResult(Activity activity, int request, int result, Intent data) {
        if (request != PICK_REQUEST) return;
        pickerOpen = false;
        if (result != Activity.RESULT_OK || data == null || data.getData() == null) {
            if (host.get() == activity) Utils.runOnMainThread(() -> showButton(activity));
            return;
        }
        Uri uri = data.getData();
        Utils.runOnBackgroundThread(() -> {
            Bitmap decoded = decode(activity, uri);
            Utils.runOnMainThread(() -> {
                if (decoded == null) {
                    report("read selected image", new IllegalStateException("The selected photo could not be read"));
                    showButton(activity);
                    return;
                }
                synchronized (InstantUpload.class) {
                    if (pending != null && !pending.isRecycled()) pending.recycle();
                    pending = decoded;
                    pendingAt = android.os.SystemClock.elapsedRealtime();
                }
                showButton(activity);
                HookStatus.invoked(FamilyNames.INSTANT_UPLOAD);
            });
        });
    }

    /** Called immediately before Instagram's camera pipeline; null preserves its original bitmap. */
    public static synchronized Bitmap takePending(Context context, Bitmap captured, Object owner) {
        if (!SettingsStatus.instantUpload() || context == null || captured == null || owner == null ||
                !owner.getClass().getName().equals("com.instagram.quicksnap.camera.domain.QuickSnapCameraViewModel")) return null;
        if (pending == null) return null;
        if (android.os.SystemClock.elapsedRealtime() - pendingAt > PICK_TTL_MS) {
            pending.recycle();
            pending = null;
            return null;
        }
        Bitmap selected = pending;
        pending = null;
        pendingAt = 0;
        sentForCamera = true;
        Utils.runOnMainThread(InstantUpload::removeButton);
        return selected;
    }

    /** Keeps Instagram's two-image handoff independent when a build supplies a secondary bitmap. */
    public static Bitmap duplicate(Bitmap selected) {
        if (selected == null) return null;
        try {
            Bitmap.Config config = selected.getConfig();
            Bitmap copy = selected.copy(config == null ? Bitmap.Config.ARGB_8888 : config, false);
            return copy == null ? selected : copy;
        } catch (Throwable failure) {
            report("copy selected image", failure);
            return selected;
        }
    }

    private static Bitmap decode(Context context, Uri uri) {
        try {
            android.graphics.ImageDecoder.Source source = android.graphics.ImageDecoder.createSource(context.getContentResolver(), uri);
            return android.graphics.ImageDecoder.decodeBitmap(source, (decoder, info, sourceValue) -> {
                decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);
                int width = info.getSize().getWidth(), height = info.getSize().getHeight();
                float ratio = Math.min(1f, MAX_EDGE / (float) Math.max(width, height));
                decoder.setTargetSize(Math.max(1, Math.round(width * ratio)), Math.max(1, Math.round(height * ratio)));
            });
        } catch (Throwable failure) {
            report("decode gallery photo", failure);
            return null;
        }
    }

    private static android.graphics.drawable.Drawable makeChipBackground(Context context) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(0xdd242424);
        shape.setCornerRadius(dp(context, 24));
        shape.setStroke(dp(context, 1), 0x66ffffff);
        return shape;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static void removeButton() {
        View old = button;
        button = null;
        if (old != null && old.getParent() instanceof ViewGroup) ((ViewGroup) old.getParent()).removeView(old);
    }

    private static boolean cameraViewPresent(Activity activity) {
        try {
            View decor = activity.getWindow().getDecorView();
            int id = activity.getResources().getIdentifier("camera_view", "id", activity.getPackageName());
            View camera = id == 0 ? null : decor.findViewById(id);
            return camera != null && camera.isShown();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void watchCamera(View camera) {
        if (watchedCamera == camera) return;
        if (watchedCamera != null) watchedCamera.removeOnAttachStateChangeListener(cameraWatcher);
        watchedCamera = camera;
        sentForCamera = false;
        camera.addOnAttachStateChangeListener(cameraWatcher);
    }

    private static final View.OnAttachStateChangeListener cameraWatcher = new View.OnAttachStateChangeListener() {
        @Override public void onViewAttachedToWindow(View view) { }
        @Override public void onViewDetachedFromWindow(View view) {
            if (watchedCamera == view) {
                watchedCamera.removeOnAttachStateChangeListener(this);
                watchedCamera = null;
                sentForCamera = false;
                removeButton();
            }
        }
    };

    private static void report(String action, Throwable failure) {
        try { HookStatus.threw(FamilyNames.INSTANT_UPLOAD, action, failure); }
        catch (Throwable ignored) { Logger.printException(() -> "Instant gallery " + action, failure); }
    }
}
