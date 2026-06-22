package com.example.smartexpapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.smartexpapp.data.AgentRepository;
import com.example.smartexpapp.data.AgentRepository.RecipeSuggestionResult;
import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.Recipe;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

public class RecipesActivity extends BaseActivity {
    private static RecipeSuggestionResult defaultSessionRecipeResult;
    private static boolean defaultSessionRecipeLoadAttempted;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextToSpeech textToSpeech;
    private boolean ttsReady;
    private boolean imageRefreshInFlight;
    private TextView recipeStateText;
    private ChipGroup recipePromptChipGroup;

    private final ActivityResultLauncher<Intent> agentSpeechLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                    showTypedPromptDialog();
                    return;
                }
                ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (matches == null || matches.isEmpty()) {
                    showTypedPromptDialog();
                    return;
                }
                askAgent(matches.get(0));
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipes);
        setupChrome(R.id.nav_recipes);
        recipeStateText = findViewById(R.id.recipeStateText);
        recipePromptChipGroup = findViewById(R.id.recipePromptChipGroup);
        setupTextToSpeech();
        setupVoiceAssistant();
        loadDefaultRecipesOncePerSession();

        if (getIntent().getBooleanExtra("start_voice_assistant", false)) {
            startAgentVoice();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecipePromptChips();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            ttsReady = status == TextToSpeech.SUCCESS;
            if (ttsReady) {
                textToSpeech.setLanguage(Locale.US);
            }
        });
    }

    private void setupVoiceAssistant() {
        TextView titleView = findViewById(R.id.voiceAssistantTitle);
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

        View voiceAssistantButton = findViewById(R.id.voiceAssistantButton);
        if (voiceAssistantButton != null) {
            voiceAssistantButton.setOnClickListener(v -> startAgentVoice());
        }
        View askByTextButton = findViewById(R.id.askByTextButton);
        if (askByTextButton != null) {
            askByTextButton.setOnClickListener(v -> showTypedPromptDialog());
        }
        View generateRecipesButton = findViewById(R.id.btnGenerateRecipes);
        if (generateRecipesButton != null) {
            generateRecipesButton.setOnClickListener(v -> triggerRecipeGeneration());
        }

        startPulseAnimation();
    }

    private void loadRecipePromptChips() {
        ProductRepository.getProductsAsync(this,
                products -> {
                    int expiringSoonDays = com.example.smartexpapp.data.SettingsRepository.getExpiringSoonDays(this);
                    renderPromptChips(AgentRepository.recipePromptSuggestions(products, expiringSoonDays));
                },
                error -> renderPromptChips(new ArrayList<>()));
    }

    private void renderPromptChips(List<String> prompts) {
        if (recipePromptChipGroup == null) {
            return;
        }
        recipePromptChipGroup.removeAllViews();
        recipePromptChipGroup.setVisibility(prompts.isEmpty() ? View.GONE : View.VISIBLE);
        for (String prompt : prompts) {
            Chip chip = new Chip(this);
            chip.setText(prompt);
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setTextColor(getColor(R.color.smart_on_surface));
            chip.setChipBackgroundColor(ColorStateList.valueOf(getColor(R.color.smart_surface_container)));
            chip.setChipStrokeColor(ColorStateList.valueOf(getColor(R.color.smart_glass_input_stroke)));
            chip.setChipStrokeWidth(ViewUtils.dp(this, 1));
            chip.setContentDescription("Ask: " + prompt);
            chip.setOnClickListener(v -> askAgent(prompt));
            recipePromptChipGroup.addView(chip);
        }
    }

    private void startAgentVoice() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask about recipes or expiring items");
        if (intent.resolveActivity(getPackageManager()) == null) {
            showTypedPromptDialog();
            return;
        }
        agentSpeechLauncher.launch(intent);
    }

    private void showTypedPromptDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_text_input, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setDimAmount(0.8f);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        title.setText(R.string.ask_agent_title);

        EditText input = dialogView.findViewById(R.id.dialogEditText);
        input.setHint(R.string.ask_agent_hint);

        TextView positiveText = dialogView.findViewById(R.id.btnPositiveText);
        positiveText.setText(R.string.ask_label);

        dialogView.findViewById(R.id.btnNegative).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnPositive).setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            dialog.dismiss();
            askAgent(text);
        });

        dialog.show();
    }

    private void askAgent(String prompt) {
        String safePrompt = prompt == null ? "" : prompt.trim();
        if (safePrompt.isEmpty()) {
            safePrompt = "Suggest recipes from my expiring items";
        }
        Toast.makeText(this, R.string.recipes_checking_inventory, Toast.LENGTH_SHORT).show();
        setRecipeState(getString(R.string.recipes_loading), false);
        String finalPrompt = safePrompt;
        executor.execute(() -> {
            try {
                String answer = AgentRepository.answerInventoryQuestion(this, finalPrompt);
                RecipeSuggestionResult result = AgentRepository.getRecipeSuggestionResult(this, finalPrompt);
                mainHandler.post(() -> {
                    Toast.makeText(this, answer, Toast.LENGTH_LONG).show();
                    speak(answer);
                    defaultSessionRecipeResult = result;
                    saveRecipeResultToPrefs(result);
                    renderRecipeResult(result);
                });
            } catch (Exception error) {
                mainHandler.post(() -> showRecipeLoadError());
            }
        });
    }

    private void speak(String answer) {
        if (ttsReady && answer != null && !answer.trim().isEmpty()) {
            textToSpeech.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "smartexp-agent");
        }
    }

    private void startPulseAnimation() {
        View pulseView = findViewById(R.id.voiceAssistantPulseView);
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

    private void triggerRecipeGeneration() {
        triggerRecipeGeneration(true);
    }

    private void triggerRecipeGeneration(boolean showLoadingFeedback) {
        if (showLoadingFeedback) {
            Toast.makeText(this, R.string.recipes_checking_inventory, Toast.LENGTH_SHORT).show();
            setRecipeState(getString(R.string.recipes_loading), false);
        }
        executor.execute(() -> {
            try {
                RecipeSuggestionResult result = AgentRepository.getRecipeSuggestionResult(this, "");
                mainHandler.post(() -> {
                    imageRefreshInFlight = false;
                    defaultSessionRecipeResult = result;
                    saveRecipeResultToPrefs(result);
                    renderRecipeResult(result);
                });
            } catch (Exception error) {
                mainHandler.post(() -> {
                    imageRefreshInFlight = false;
                    if (showLoadingFeedback) {
                        showRecipeLoadError();
                    }
                });
            }
        });
    }

    private void loadDefaultRecipesOncePerSession() {
        if (defaultSessionRecipeResult != null) {
            renderRecipeResult(defaultSessionRecipeResult);
            refreshRecipesIfImagesMissing(defaultSessionRecipeResult);
            return;
        }

        RecipeSuggestionResult cached = loadRecipeResultFromPrefs();
        if (cached != null) {
            defaultSessionRecipeResult = cached;
            renderRecipeResult(cached);
            refreshRecipesIfImagesMissing(cached);
            return;
        }

        if (defaultSessionRecipeLoadAttempted) {
            showRecipeLoadError();
            return;
        }

        defaultSessionRecipeLoadAttempted = true;
        triggerRecipeGeneration();
    }

    private void refreshRecipesIfImagesMissing(RecipeSuggestionResult result) {
        if (imageRefreshInFlight || result == null || BuildConfig.RECIPE_IMAGE_WORKER_URL.trim().isEmpty()) {
            return;
        }
        for (Recipe recipe : result.getRecipes()) {
            if (recipe.getImageUrl() == null || recipe.getImageUrl().trim().isEmpty()) {
                imageRefreshInFlight = true;
                triggerRecipeGeneration(false);
                return;
            }
        }
    }

    private void saveRecipeResultToPrefs(RecipeSuggestionResult result) {
        if (result == null) return;
        try {
            JSONObject obj = new JSONObject();
            obj.put("statusMessage", result.getStatusMessage());
            obj.put("localFallback", result.isLocalFallback());
            obj.put("inventoryEmpty", result.isInventoryEmpty());

            JSONArray recipesArray = new JSONArray();
            for (Recipe recipe : result.getRecipes()) {
                JSONObject rObj = new JSONObject();
                rObj.put("title", recipe.getTitle());
                rObj.put("summary", recipe.getSummary());

                JSONArray expIng = new JSONArray();
                for (String ing : recipe.getExpiringIngredients()) {
                    expIng.put(ing);
                }
                rObj.put("expiringIngredients", expIng);

                rObj.put("actionText", recipe.getActionText());
                rObj.put("iconRes", recipe.getIconRes());
                rObj.put("featured", recipe.isFeatured());
                rObj.put("imageUrl", recipe.getImageUrl() != null ? recipe.getImageUrl() : "");
                rObj.put("prepTime", recipe.getPrepTime());
                rObj.put("difficulty", recipe.getDifficulty());
                rObj.put("calories", recipe.getCalories());
                rObj.put("smartTip", recipe.getSmartTip() != null ? recipe.getSmartTip() : "");

                JSONArray allIng = new JSONArray();
                for (String ing : recipe.getAllIngredients()) {
                    allIng.put(ing);
                }
                rObj.put("allIngredients", allIng);

                JSONArray inst = new JSONArray();
                for (String step : recipe.getInstructions()) {
                    inst.put(step);
                }
                rObj.put("instructions", inst);

                recipesArray.put(rObj);
            }
            obj.put("recipes", recipesArray);

            getSharedPreferences("recipes_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("saved_recipes", obj.toString())
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private RecipeSuggestionResult loadRecipeResultFromPrefs() {
        try {
            String json = getSharedPreferences("recipes_prefs", MODE_PRIVATE).getString("saved_recipes", null);
            if (json == null) return null;

            JSONObject obj = new JSONObject(json);
            String statusMessage = obj.optString("statusMessage", "");
            boolean localFallback = obj.optBoolean("localFallback", false);
            boolean inventoryEmpty = obj.optBoolean("inventoryEmpty", false);

            JSONArray recipesArray = obj.getJSONArray("recipes");
            List<Recipe> recipes = new ArrayList<>();
            for (int i = 0; i < recipesArray.length(); i++) {
                JSONObject rObj = recipesArray.getJSONObject(i);
                String title = rObj.getString("title");
                String summary = rObj.getString("summary");

                JSONArray expIngArr = rObj.getJSONArray("expiringIngredients");
                List<String> expiringIngredients = new ArrayList<>();
                for (int j = 0; j < expIngArr.length(); j++) {
                    expiringIngredients.add(expIngArr.getString(j));
                }

                String actionText = rObj.getString("actionText");
                int iconRes = rObj.getInt("iconRes");
                boolean featured = rObj.getBoolean("featured");
                String imageUrl = rObj.optString("imageUrl", "");
                if (imageUrl.isEmpty()) imageUrl = null;

                String prepTime = rObj.optString("prepTime", "20 min");
                String difficulty = rObj.optString("difficulty", "Medium");
                String calories = rObj.optString("calories", "400 kcal");

                String smartTip = rObj.optString("smartTip", "");
                if (smartTip.isEmpty()) smartTip = null;

                JSONArray allIngArr = rObj.optJSONArray("allIngredients");
                List<String> allIngredients = new ArrayList<>();
                if (allIngArr != null) {
                    for (int j = 0; j < allIngArr.length(); j++) {
                        allIngredients.add(allIngArr.getString(j));
                    }
                } else {
                    allIngredients = expiringIngredients;
                }

                JSONArray instArr = rObj.optJSONArray("instructions");
                List<String> instructions = new ArrayList<>();
                if (instArr != null) {
                    for (int j = 0; j < instArr.length(); j++) {
                        instructions.add(instArr.getString(j));
                    }
                }

                recipes.add(new Recipe(title, summary, expiringIngredients, actionText, iconRes, featured,
                        imageUrl, prepTime, difficulty, calories, smartTip, allIngredients, instructions));
            }
            return new RecipeSuggestionResult(recipes, statusMessage, localFallback, inventoryEmpty);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void renderRecipeResult(RecipeSuggestionResult result) {
        setRecipeState(result.getStatusMessage(), result.isInventoryEmpty() || result.isLocalFallback());
        ImageLoader.prefetch(this, recipeImageUrls(result.getRecipes()));
        ProductRepository.getProductsAsync(this,
                products -> renderRecipes(result.getRecipes(), products),
                error -> renderRecipes(result.getRecipes(), new ArrayList<>()));
    }

    private List<String> recipeImageUrls(List<Recipe> recipes) {
        List<String> imageUrls = new ArrayList<>();
        for (Recipe recipe : recipes) {
            String imageUrl = recipe.getImageUrl();
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                imageUrls.add(imageUrl);
            }
        }
        return imageUrls;
    }

    private void renderRecipes(List<Recipe> recipes, List<Product> products) {
        LinearLayout recipeList = findViewById(R.id.recipeList);
        recipeList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Recipe recipe : recipes) {
            View item = inflater.inflate(R.layout.item_recipe_card, recipeList, false);
            bindRecipeCard(inflater, item, recipe, products);
            ViewUtils.setBottomMargin(item, 16);
            recipeList.addView(item);
        }
    }

    private void showRecipeLoadError() {
        setRecipeState(getString(R.string.recipes_error), true);
        renderRecipes(new ArrayList<>(), new ArrayList<>());
    }

    private void setRecipeState(String message, boolean emphasized) {
        if (recipeStateText == null) {
            return;
        }
        recipeStateText.setText(message);
        recipeStateText.setTextColor(getColor(emphasized ? R.color.smart_primary : R.color.smart_secondary));
    }

    private void bindRecipeCard(LayoutInflater inflater, View item, Recipe recipe, List<Product> products) {
        int expiringSoonDays = com.example.smartexpapp.data.SettingsRepository.getExpiringSoonDays(this);
        FrameLayout hero = item.findViewById(R.id.recipeHero);
        if (hero != null) {
            hero.setBackgroundResource(recipe.isFeatured() ? R.drawable.bg_hero_primary : R.drawable.bg_hero_neutral);
        }
        ImageView iconView = item.findViewById(R.id.recipeIcon);
        if (iconView != null) {
            ViewUtils.setIcon(iconView, recipe.getIconRes(), recipe.isFeatured() ? R.color.smart_surface : R.color.smart_secondary);
            ImageLoader.load(iconView, recipe.getImageUrl());
        }

        ((TextView) item.findViewById(R.id.recipeTitle)).setText(recipe.getTitle());
        ((TextView) item.findViewById(R.id.recipeSummary)).setText(recipe.getSummary());

        TextView caloriesView = item.findViewById(R.id.recipeCardCalories);
        if (caloriesView != null) {
            String cal = recipe.getCalories();
            if (cal != null && cal.length() > 12) {
                cal = "400 kcal";
            }
            caloriesView.setText(cal != null ? cal.toUpperCase() : "");
        }

        TextView difficultyView = item.findViewById(R.id.recipeCardDifficulty);
        if (difficultyView != null) {
            difficultyView.setText(recipe.getDifficulty());
        }

        TextView prepTimeView = item.findViewById(R.id.recipeCardPrepTime);
        if (prepTimeView != null) {
            String prep = recipe.getPrepTime();
            if (prep != null && prep.length() > 12) {
                prep = "20 min";
            }
            prepTimeView.setText(prep);
        }

        View ingredientsContainer = item.findViewById(R.id.recipeIngredientsContainer);
        ChipGroup ingredientsChipGroup = item.findViewById(R.id.recipeIngredientsChipGroup);
        if (ingredientsContainer != null && ingredientsChipGroup != null) {
            ingredientsChipGroup.removeAllViews();
            List<Product> matchedProducts = new ArrayList<>();
            for (Product product : products) {
                if (product.isExpired()) {
                    continue;
                }
                boolean matches = false;
                for (String ingredient : recipe.getAllIngredients()) {
                    String ingredientLower = ingredient.toLowerCase(Locale.US).trim();
                    String productLower = product.getName().toLowerCase(Locale.US).trim();
                    if (productLower.length() >= 3 && (ingredientLower.contains(productLower) || productLower.contains(ingredientLower))) {
                        matches = true;
                        break;
                    } else if (productLower.equalsIgnoreCase(ingredientLower)) {
                        matches = true;
                        break;
                    }
                }
                if (matches) {
                    boolean duplicate = false;
                    for (Product p : matchedProducts) {
                        if (p.getName().equalsIgnoreCase(product.getName())) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) {
                        matchedProducts.add(product);
                    }
                }
            }

            if (!matchedProducts.isEmpty()) {
                ingredientsContainer.setVisibility(View.VISIBLE);
                for (Product product : matchedProducts) {
                    View badge = inflater.inflate(R.layout.item_recipe_ingredient_badge, ingredientsChipGroup, false);
                    ImageView badgeIcon = badge.findViewById(R.id.ingredientBadgeIcon);
                    TextView badgeText = badge.findViewById(R.id.ingredientBadgeText);

                    int daysLeft = product.getDaysUntilExpiry();
                    if (daysLeft <= expiringSoonDays) {
                        badge.setBackgroundResource(R.drawable.bg_recipe_ingredient_expiring);
                        badgeText.setText(product.getName() + " (" + daysLeft + "d left)");
                        badgeText.setTextColor(getColor(R.color.smart_error));
                        ViewUtils.setIcon(badgeIcon, R.drawable.ic_clock, R.color.smart_error);
                    } else {
                        badge.setBackgroundResource(R.drawable.bg_recipe_ingredient_normal);
                        badgeText.setText(product.getName());
                        badgeText.setTextColor(getColor(R.color.smart_on_surface));
                        ViewUtils.setIcon(badgeIcon, R.drawable.ic_check_circle, R.color.smart_on_surface);
                    }
                    ingredientsChipGroup.addView(badge);
                }
            } else {
                ingredientsContainer.setVisibility(View.GONE);
            }
        }

        // Entire card is clickable to open RecipeDetailsActivity
        item.setOnClickListener(v -> {
            Intent intent = new Intent(this, RecipeDetailsActivity.class);
            intent.putExtra("extra_recipe_title", recipe.getTitle());
            intent.putExtra("extra_recipe_summary", recipe.getSummary());
            intent.putExtra("extra_recipe_image_url", recipe.getImageUrl());
            intent.putExtra("extra_recipe_prep_time", recipe.getPrepTime());
            intent.putExtra("extra_recipe_difficulty", recipe.getDifficulty());
            intent.putExtra("extra_recipe_calories", recipe.getCalories());
            intent.putExtra("extra_recipe_smart_tip", recipe.getSmartTip());
            intent.putStringArrayListExtra("extra_recipe_all_ingredients", new ArrayList<>(recipe.getAllIngredients()));
            intent.putStringArrayListExtra("extra_recipe_instructions", new ArrayList<>(recipe.getInstructions()));
            intent.putExtra("extra_recipe_featured", recipe.isFeatured());
            startActivity(intent);
        });
    }
}
