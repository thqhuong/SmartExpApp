package com.example.smartexpapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        bindRecipes();
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
