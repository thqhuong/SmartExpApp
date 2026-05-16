package com.example.smartexpapp.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

public final class ViewUtils {
    private ViewUtils() {
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static void setIcon(ImageView view, @DrawableRes int drawableRes, @ColorRes int colorRes) {
        view.setImageResource(drawableRes);
        view.setColorFilter(ContextCompat.getColor(view.getContext(), colorRes));
    }

    public static void setBottomMargin(View view, int bottomDp) {
        ViewGroup.LayoutParams rawParams = view.getLayoutParams();
        if (!(rawParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) rawParams;
        params.bottomMargin = dp(view.getContext(), bottomDp);
        view.setLayoutParams(params);
    }
}
