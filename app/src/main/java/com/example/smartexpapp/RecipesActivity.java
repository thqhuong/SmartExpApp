package com.example.smartexpapp;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartexpapp.data.SampleData;
import com.example.smartexpapp.model.Recipe;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;
import com.google.android.material.button.MaterialButton;

public class RecipesActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipes);
        setupChrome(R.id.nav_recipes);
        setupGeminiLive();
        bindRecipes();
    }

    private void setupGeminiLive() {
        TextView titleView = findViewById(R.id.geminiLiveTitle);
        if (titleView != null) {
            titleView.post(() -> {
                int width = titleView.getWidth();
                if (width <= 0) {
                    width = (int) titleView.getPaint().measureText(titleView.getText().toString());
                }
                if (width > 0) {
                    Shader textShader = new LinearGradient(
                            0, 0, width, 0,
                            new int[]{
                                    Color.parseColor("#4285F4"),
                                    Color.parseColor("#9B72CB"),
                                    Color.parseColor("#D96570")
                            },
                            null,
                            Shader.TileMode.CLAMP
                    );
                    titleView.getPaint().setShader(textShader);
                    titleView.invalidate();
                }
            });
        }

        View geminiLiveButton = findViewById(R.id.geminiLiveButton);
        if (geminiLiveButton != null) {
            geminiLiveButton.setOnClickListener(v -> {
                Toast.makeText(this, "Gemini Live starting...", Toast.LENGTH_SHORT).show();
            });
        }

        startPulseAnimation();
    }

    private void startPulseAnimation() {
        View pulseView = findViewById(R.id.geminiLivePulseView);
        if (pulseView == null) return;

        ScaleAnimation scaleAnim = new ScaleAnimation(
                1.0f, 1.4f,
                1.0f, 1.4f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnim.setDuration(1600);
        scaleAnim.setRepeatCount(Animation.INFINITE);
        scaleAnim.setRepeatMode(Animation.RESTART);

        AlphaAnimation alphaAnim = new AlphaAnimation(0.5f, 0.0f);
        alphaAnim.setDuration(1600);
        alphaAnim.setRepeatCount(Animation.INFINITE);
        alphaAnim.setRepeatMode(Animation.RESTART);

        AnimationSet animSet = new AnimationSet(true);
        animSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animSet.addAnimation(scaleAnim);
        animSet.addAnimation(alphaAnim);

        pulseView.startAnimation(animSet);
    }

    private void bindRecipes() {
        LinearLayout recipeList = findViewById(R.id.recipeList);
        recipeList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Recipe recipe : SampleData.recipes()) {
            View item = inflater.inflate(R.layout.item_recipe_card, recipeList, false);
            bindRecipeCard(inflater, item, recipe);
            ViewUtils.setBottomMargin(item, 16);
            recipeList.addView(item);
        }
    }

    private void bindRecipeCard(LayoutInflater inflater, View item, Recipe recipe) {
        FrameLayout hero = item.findViewById(R.id.recipeHero);
        hero.setBackgroundResource(recipe.isFeatured() ? R.drawable.bg_hero_primary : R.drawable.bg_hero_neutral);
        ViewUtils.setIcon(item.findViewById(R.id.recipeIcon), recipe.getIconRes(), recipe.isFeatured() ? R.color.smart_surface : R.color.smart_secondary);
        ImageLoader.load(item.findViewById(R.id.recipeIcon), recipe.getImageUrl());

        ((TextView) item.findViewById(R.id.recipeTitle)).setText(recipe.getTitle());
        ((TextView) item.findViewById(R.id.recipeSummary)).setText(recipe.getSummary());
        MaterialButton action = item.findViewById(R.id.recipeAction);
        action.setText(recipe.getActionText());
        action.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(recipe.isFeatured() ? R.color.smart_primary : R.color.smart_surface_container)));
        action.setTextColor(getColor(recipe.isFeatured() ? R.color.smart_on_primary : R.color.smart_on_surface));
        action.setIconTint(android.content.res.ColorStateList.valueOf(getColor(recipe.isFeatured() ? R.color.smart_on_primary : R.color.smart_on_surface)));

        LinearLayout ingredients = item.findViewById(R.id.ingredientList);
        ingredients.removeAllViews();
        for (String ingredient : recipe.getExpiringIngredients()) {
            TextView ingredientView = (TextView) inflater.inflate(R.layout.item_recipe_ingredient, ingredients, false);
            ingredientView.setText(ingredient);
            if (ingredient.contains("Today") || ingredient.contains("Tomorrow") || ingredient.contains("1 Day")) {
                ingredientView.setTextColor(getColor(R.color.smart_primary));
            }
            ingredients.addView(ingredientView);
        }
    }
}
