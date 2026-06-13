package com.example.smartexpapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
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

public class RecipesActivity extends BaseActivity {
    private static RecipeSuggestionResult defaultSessionRecipeResult;
    private static boolean defaultSessionRecipeLoadAttempted;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextToSpeech textToSpeech;
    private boolean ttsReady;
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
        setupGeminiLive();
        loadDefaultRecipesOncePerSession();

        if (getIntent().getBooleanExtra("start_gemini_live", false)) {
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
            geminiLiveButton.setOnClickListener(v -> startAgentVoice());
        }
        View askByTextButton = findViewById(R.id.askByTextButton);
        if (askByTextButton != null) {
            askByTextButton.setOnClickListener(v -> showTypedPromptDialog());
        }

        startPulseAnimation();
    }

    private void loadRecipePromptChips() {
        ProductRepository.getProductsAsync(this,
                products -> renderPromptChips(AgentRepository.recipePromptSuggestions(products)),
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
        EditText input = new EditText(this);
        input.setHint(R.string.ask_agent_hint);
        input.setSingleLine(false);
        int padding = Math.round(16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);
        new AlertDialog.Builder(this)
                .setTitle(R.string.ask_agent_title)
                .setView(input)
                .setPositiveButton(R.string.ask_label, (dialog, which) -> askAgent(input.getText().toString()))
                .setNegativeButton(R.string.cancel, null)
                .show();
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

    private void loadDefaultRecipesOncePerSession() {
        if (defaultSessionRecipeResult != null) {
            renderRecipeResult(defaultSessionRecipeResult);
            return;
        }
        if (defaultSessionRecipeLoadAttempted) {
            showRecipeLoadError();
            return;
        }

        defaultSessionRecipeLoadAttempted = true;
        setRecipeState(getString(R.string.recipes_loading), false);
        executor.execute(() -> {
            try {
                RecipeSuggestionResult result = AgentRepository.getRecipeSuggestionResult(this, "");
                mainHandler.post(() -> {
                    defaultSessionRecipeResult = result;
                    renderRecipeResult(result);
                });
            } catch (Exception error) {
                mainHandler.post(() -> showRecipeLoadError());
            }
        });
    }

    private void renderRecipeResult(RecipeSuggestionResult result) {
        setRecipeState(result.getStatusMessage(), result.isInventoryEmpty() || result.isLocalFallback());
        renderRecipes(result.getRecipes());
    }

    private void renderRecipes(List<Recipe> recipes) {
        LinearLayout recipeList = findViewById(R.id.recipeList);
        recipeList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Recipe recipe : recipes) {
            View item = inflater.inflate(R.layout.item_recipe_card, recipeList, false);
            bindRecipeCard(inflater, item, recipe);
            ViewUtils.setBottomMargin(item, 16);
            recipeList.addView(item);
        }
    }

    private void showRecipeLoadError() {
        setRecipeState(getString(R.string.recipes_error), true);
        renderRecipes(new ArrayList<>());
    }

    private void setRecipeState(String message, boolean emphasized) {
        if (recipeStateText == null) {
            return;
        }
        recipeStateText.setText(message);
        recipeStateText.setTextColor(getColor(emphasized ? R.color.smart_primary : R.color.smart_secondary));
    }

    private void bindRecipeCard(LayoutInflater inflater, View item, Recipe recipe) {
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
            caloriesView.setText(recipe.getCalories().toUpperCase());
        }

        TextView difficultyView = item.findViewById(R.id.recipeCardDifficulty);
        if (difficultyView != null) {
            difficultyView.setText(recipe.getDifficulty());
        }

        TextView prepTimeView = item.findViewById(R.id.recipeCardPrepTime);
        if (prepTimeView != null) {
            prepTimeView.setText(recipe.getPrepTime());
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
