package com.example.smartexpapp;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartexpapp.util.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public class RecipeDetailsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_details);

        // Get details from Intent
        String title = getIntent().getStringExtra("extra_recipe_title");
        String summary = getIntent().getStringExtra("extra_recipe_summary");
        String imageUrl = getIntent().getStringExtra("extra_recipe_image_url");
        String prepTime = getIntent().getStringExtra("extra_recipe_prep_time");
        String difficulty = getIntent().getStringExtra("extra_recipe_difficulty");
        String calories = getIntent().getStringExtra("extra_recipe_calories");
        String smartTip = getIntent().getStringExtra("extra_recipe_smart_tip");
        ArrayList<String> allIngredients = getIntent().getStringArrayListExtra("extra_recipe_all_ingredients");
        ArrayList<String> instructions = getIntent().getStringArrayListExtra("extra_recipe_instructions");

        if (title == null) {
            title = "Recipe Details";
        }

        // Bind views
        TextView titleText = findViewById(R.id.recipeTitleText);
        titleText.setText(title);

        ImageButton backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        ImageButton favoriteBtn = findViewById(R.id.favoriteBtn);
        setupFavoriteButton(favoriteBtn, title);

        ImageView heroImage = findViewById(R.id.recipeHeroImage);
        ImageLoader.load(heroImage, imageUrl);

        TextView prepTimeText = findViewById(R.id.recipePrepTime);
        if (prepTime != null && !prepTime.trim().isEmpty()) {
            if (prepTime.length() > 12) {
                prepTime = "20 min";
            }
            prepTimeText.setText(prepTime.toUpperCase());
        }

        TextView difficultyText = findViewById(R.id.recipeDifficulty);
        if (difficulty != null && !difficulty.trim().isEmpty()) {
            difficultyText.setText(difficulty.toUpperCase());
        }

        TextView caloriesText = findViewById(R.id.recipeCalories);
        if (calories != null && !calories.trim().isEmpty()) {
            if (calories.length() > 12) {
                calories = "400 kcal";
            }
            caloriesText.setText(calories.toUpperCase());
        }

        // Smart AI tip
        View smartTipCard = findViewById(R.id.recipeSmartTipCard);
        TextView smartTipTextView = findViewById(R.id.recipeSmartTipText);
        if (smartTip != null && !smartTip.trim().isEmpty()) {
            smartTipCard.setVisibility(View.VISIBLE);
            smartTipTextView.setText(smartTip);
        } else {
            smartTipCard.setVisibility(View.GONE);
        }

        // Ingredients Checklist
        LinearLayout ingredientsContainer = findViewById(R.id.ingredientsListContainer);
        ingredientsContainer.removeAllViews();
        if (allIngredients != null && !allIngredients.isEmpty()) {
            for (int i = 0; i < allIngredients.size(); i++) {
                String ingredient = allIngredients.get(i);
                addIngredientItem(ingredientsContainer, ingredient, i, allIngredients.size());
            }
        } else {
            TextView emptyText = new TextView(this);
            emptyText.setText(R.string.ingredients_empty);
            emptyText.setTextColor(getColor(R.color.smart_secondary));
            emptyText.setTextSize(14);
            ingredientsContainer.addView(emptyText);
        }

        // Instructions
        LinearLayout instructionsContainer = findViewById(R.id.instructionsListContainer);
        instructionsContainer.removeAllViews();
        if (instructions != null && !instructions.isEmpty()) {
            for (int i = 0; i < instructions.size(); i++) {
                String step = instructions.get(i);
                addInstructionStep(instructionsContainer, step, i);
            }
        } else {
            // Default step if none provided
            String fallbackStep = summary != null ? summary : "Cook according to the recipe description.";
            addInstructionStep(instructionsContainer, fallbackStep, 0);
        }
    }

    private void setupFavoriteButton(ImageButton favoriteBtn, String recipeTitle) {
        SharedPreferences prefs = getSharedPreferences("smart_recipes_prefs", MODE_PRIVATE);
        String favKey = "fav_" + recipeTitle;
        final boolean[] isFavorited = {prefs.getBoolean(favKey, false)};

        // Apply initial state
        if (isFavorited[0]) {
            favoriteBtn.setImageResource(R.drawable.ic_favorite_filled);
            favoriteBtn.setImageTintList(null);
        } else {
            favoriteBtn.setImageResource(R.drawable.ic_favorite_border);
            favoriteBtn.setImageTintList(ColorStateList.valueOf(getColor(R.color.smart_primary)));
        }

        favoriteBtn.setOnClickListener(v -> {
            isFavorited[0] = !isFavorited[0];
            prefs.edit().putBoolean(favKey, isFavorited[0]).apply();
            if (isFavorited[0]) {
                favoriteBtn.setImageResource(R.drawable.ic_favorite_filled);
                favoriteBtn.setImageTintList(null);
                Toast.makeText(this, R.string.favorite_saved, Toast.LENGTH_SHORT).show();
            } else {
                favoriteBtn.setImageResource(R.drawable.ic_favorite_border);
                favoriteBtn.setImageTintList(ColorStateList.valueOf(getColor(R.color.smart_primary)));
                Toast.makeText(this, R.string.favorite_removed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addIngredientItem(LinearLayout container, String ingredient, int index, int totalCount) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemLayout.setPadding(0, dp(12), 0, dp(12));

        CheckBox checkBox = new CheckBox(this);
        LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cbParams.setMarginEnd(dp(12));
        checkBox.setLayoutParams(cbParams);
        checkBox.setButtonTintList(ColorStateList.valueOf(getColor(R.color.smart_primary)));

        TextView textView = new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        textView.setText(ingredient);
        textView.setTextColor(getColor(R.color.smart_on_surface));
        textView.setTextSize(16);

        itemLayout.addView(checkBox);
        itemLayout.addView(textView);

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                textView.setAlpha(0.5f);
            } else {
                textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                textView.setAlpha(1.0f);
            }
        });

        container.addView(itemLayout);

        // Divider
        if (index < totalCount - 1) {
            View divider = new View(this);
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
            divider.setLayoutParams(divParams);
            divider.setBackgroundColor(getColor(R.color.smart_glass_stroke));
            container.addView(divider);
        }
    }

    private void addInstructionStep(LinearLayout container, String step, int index) {
        LinearLayout stepLayout = new LinearLayout(this);
        stepLayout.setOrientation(LinearLayout.HORIZONTAL);
        stepLayout.setGravity(Gravity.TOP);
        stepLayout.setPadding(0, dp(8), 0, dp(8));

        // Index circle
        FrameLayout indexFrame = new FrameLayout(this);
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(dp(28), dp(28));
        frameParams.gravity = Gravity.TOP;
        frameParams.topMargin = dp(2);
        indexFrame.setLayoutParams(frameParams);
        indexFrame.setBackgroundResource(R.drawable.bg_step_index);

        TextView indexText = new TextView(this);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        indexText.setLayoutParams(textParams);
        indexText.setText(String.valueOf(index + 1));
        indexText.setGravity(Gravity.CENTER);
        indexText.setTypeface(null, Typeface.BOLD);
        indexText.setTextColor(getColor(R.color.smart_primary));
        indexText.setTextSize(14);
        indexFrame.addView(indexText);

        // Step text
        TextView stepText = new TextView(this);
        LinearLayout.LayoutParams stepTextParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        stepTextParams.setMarginStart(dp(12));
        stepText.setLayoutParams(stepTextParams);
        stepText.setText(step);
        stepText.setTextColor(getColor(R.color.smart_on_surface));
        stepText.setTextSize(16);
        stepText.setLineSpacing(0f, 1.2f);

        stepLayout.addView(indexFrame);
        stepLayout.addView(stepText);

        container.addView(stepLayout);
    }

    private int dp(int dpValue) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dpValue * density);
    }
}
