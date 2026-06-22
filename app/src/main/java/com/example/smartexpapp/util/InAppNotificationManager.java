package com.example.smartexpapp.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartexpapp.R;

public class InAppNotificationManager {

    public enum Type {
        SUCCESS,
        ERROR,
        WARNING,
        INFO,
        UNDO
    }

    private static View currentNotificationView = null;
    private static final Handler autoDismissHandler = new Handler(Looper.getMainLooper());
    private static Runnable autoDismissRunnable = null;

    /**
     * Dismisses the currently showing in-app notification with a slide-down animation.
     */
    public static void dismissCurrent() {
        if (autoDismissRunnable != null) {
            autoDismissHandler.removeCallbacks(autoDismissRunnable);
            autoDismissRunnable = null;
        }

        if (currentNotificationView != null) {
            final View viewToRemove = currentNotificationView;
            currentNotificationView = null;

            // Remove click listener on action button to prevent multiple triggers during dismissal
            View actionBtn = viewToRemove.findViewById(R.id.notificationAction);
            if (actionBtn != null) {
                actionBtn.setOnClickListener(null);
            }

            View notificationBar = viewToRemove.findViewById(R.id.notificationBar);
            if (notificationBar != null) {
                notificationBar.animate()
                        .alpha(0f)
                        .setDuration(250)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .withEndAction(() -> {
                            ViewGroup parent = (ViewGroup) viewToRemove.getParent();
                            if (parent != null) {
                                parent.removeView(viewToRemove);
                            }
                        })
                        .start();
            } else {
                ViewGroup parent = (ViewGroup) viewToRemove.getParent();
                if (parent != null) {
                    parent.removeView(viewToRemove);
                }
            }
        }
    }

    /**
     * Shows a standard in-app notification without actions.
     */
    public static void show(Activity activity, String message, Type type) {
        showNotification(activity, message, type, null, null);
    }

    /**
     * Shows an undo/action in-app notification using customized resources.
     */
    public static void showUndo(Activity activity, String message, @DrawableRes int iconRes,
                                @DrawableRes int iconBgRes, @ColorRes int iconTintRes, Runnable undoAction) {
        showNotification(activity, message, Type.UNDO, activity.getString(R.string.mark_undo), undoAction);
        
        // Override standard styling using the customized icons/colors provided
        if (currentNotificationView != null) {
            FrameLayout iconContainer = currentNotificationView.findViewById(R.id.notificationIconContainer);
            ImageView icon = currentNotificationView.findViewById(R.id.notificationIcon);
            TextView actionBtn = currentNotificationView.findViewById(R.id.notificationAction);

            if (iconContainer != null) {
                iconContainer.setBackgroundResource(iconBgRes);
            }
            if (icon != null) {
                icon.setImageResource(iconRes);
                icon.setImageTintList(ColorStateList.valueOf(activity.getColor(iconTintRes)));
            }
            if (actionBtn != null) {
                actionBtn.setTextColor(activity.getColor(iconTintRes));
            }
        }
    }

    /**
     * Main builder method for generating and adding the custom notification to the view hierarchy.
     */
    public static void showNotification(Activity activity, String message, Type type,
                                        String actionLabel, Runnable action) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        // Dismiss any existing notification first to avoid overlap
        dismissCurrent();

        // Retrieve the root view group of the activity
        ViewGroup root = activity.findViewById(R.id.root);
        if (root == null) {
            root = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        }
        if (root == null) {
            root = activity.findViewById(android.R.id.content);
        }
        if (root == null) {
            return;
        }

        // Inflate the notification layout
        LayoutInflater inflater = LayoutInflater.from(activity);
        View notificationView = inflater.inflate(R.layout.dialog_inapp_notification, root, false);
        currentNotificationView = notificationView;

        // Bind layout views
        View notificationBar = notificationView.findViewById(R.id.notificationBar);
        FrameLayout iconContainer = notificationView.findViewById(R.id.notificationIconContainer);
        ImageView icon = notificationView.findViewById(R.id.notificationIcon);
        TextView msgText = notificationView.findViewById(R.id.notificationMessage);
        TextView actionBtn = notificationView.findViewById(R.id.notificationAction);

        if (msgText != null) {
            msgText.setText(message);
        }

        // Determine default resource settings based on notification type
        int iconRes = R.drawable.ic_info;
        int iconBgRes = R.drawable.bg_action_icon_circle;
        int iconTintRes = R.color.smart_primary;

        switch (type) {
            case SUCCESS:
                iconRes = R.drawable.ic_check_circle;
                iconBgRes = R.drawable.bg_action_icon_circle;
                iconTintRes = R.color.smart_success;
                break;
            case ERROR:
                iconRes = R.drawable.ic_close;
                iconBgRes = R.drawable.bg_action_icon_circle_delete;
                iconTintRes = R.color.smart_error;
                break;
            case WARNING:
                iconRes = R.drawable.ic_warning;
                iconBgRes = R.drawable.bg_action_icon_circle_delete;
                iconTintRes = R.color.smart_primary_container;
                break;
            case UNDO:
                iconRes = R.drawable.ic_check_circle;
                iconBgRes = R.drawable.bg_action_icon_circle;
                iconTintRes = R.color.smart_primary;
                break;
            case INFO:
            default:
                iconRes = R.drawable.ic_info;
                iconBgRes = R.drawable.bg_action_icon_circle;
                iconTintRes = R.color.smart_primary;
                break;
        }

        if (iconContainer != null) {
            iconContainer.setBackgroundResource(iconBgRes);
        }
        if (icon != null) {
            icon.setImageResource(iconRes);
            icon.setImageTintList(ColorStateList.valueOf(activity.getColor(iconTintRes)));
        }

        // Configure action button if callback and text are supplied
        if (actionBtn != null) {
            if (actionLabel != null && action != null) {
                actionBtn.setText(actionLabel);
                actionBtn.setTextColor(activity.getColor(iconTintRes));
                actionBtn.setVisibility(View.VISIBLE);
                actionBtn.setOnClickListener(v -> {
                    dismissCurrent();
                    action.run();
                });
            } else {
                actionBtn.setVisibility(View.GONE);
            }
        }

        // Attach click-to-dismiss support
        if (notificationBar != null) {
            notificationBar.setOnClickListener(v -> dismissCurrent());
        }

        // Add to root layout directly above bottomNavigation if it exists
        View bottomNav = root.findViewById(R.id.bottomNavigation);
        int index = -1;
        if (bottomNav != null) {
            index = root.indexOfChild(bottomNav);
        }

        if (index >= 0) {
            root.addView(notificationView, index);
        } else {
            root.addView(notificationView);
        }

        // Apply bottom insets if bottom navigation is not visible or doesn't exist
        final ViewGroup finalRoot = root;
        final int initialPaddingBottom = notificationView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(notificationView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            View bNav = finalRoot.findViewById(R.id.bottomNavigation);
            if (bNav == null || bNav.getVisibility() == View.GONE) {
                v.setPaddingRelative(
                        v.getPaddingStart(),
                        v.getPaddingTop(),
                        v.getPaddingEnd(),
                        initialPaddingBottom + systemBars.bottom
                );
            } else {
                v.setPaddingRelative(
                        v.getPaddingStart(),
                        v.getPaddingTop(),
                        v.getPaddingEnd(),
                        initialPaddingBottom
                );
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(notificationView);

        // Prevent parent clipping so translation downward looks clean
        root.setClipChildren(false);
        root.setClipToPadding(false);
        if (root.getParent() instanceof ViewGroup) {
            ((ViewGroup) root.getParent()).setClipChildren(false);
            ((ViewGroup) root.getParent()).setClipToPadding(false);
        }

        // Animate slide-up entrance
        if (notificationBar != null) {
            notificationBar.setTranslationY(200f);
            notificationBar.setAlpha(0f);
            notificationBar.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(350)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        // Auto dismiss setup
        autoDismissRunnable = InAppNotificationManager::dismissCurrent;
        autoDismissHandler.postDelayed(autoDismissRunnable, 4000);
    }


}
