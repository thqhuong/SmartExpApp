package com.example.smartexpapp.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.smartexpapp.BuildConfig;
import com.example.smartexpapp.R;
import com.example.smartexpapp.data.local.AgentMessageEntity;
import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.data.local.RecipeCacheEntity;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.ProductDraft;
import com.example.smartexpapp.model.Recipe;
import com.example.smartexpapp.util.DateParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AgentRepository {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Pattern QUANTITY_PATTERN = Pattern.compile(
            "\\b(\\d+(?:\\.\\d+)?)\\s*(pcs|pieces|piece|g|kg|ml|l|oz|lb|cup|cups|tbsp|tsp|gal|gallon|gallons|bag|bags|loaf|loaves)\\b",
            Pattern.CASE_INSENSITIVE);

    private AgentRepository() {
    }

    public static final class RecipeSuggestionResult {
        private final List<Recipe> recipes;
        private final String statusMessage;
        private final boolean localFallback;
        private final boolean inventoryEmpty;

        public RecipeSuggestionResult(List<Recipe> recipes, String statusMessage, boolean localFallback, boolean inventoryEmpty) {
            this.recipes = new ArrayList<>(recipes);
            this.statusMessage = statusMessage;
            this.localFallback = localFallback;
            this.inventoryEmpty = inventoryEmpty;
        }

        public List<Recipe> getRecipes() {
            return new ArrayList<>(recipes);
        }

        public String getStatusMessage() {
            return statusMessage;
        }

        public boolean isLocalFallback() {
            return localFallback;
        }

        public boolean isInventoryEmpty() {
            return inventoryEmpty;
        }
    }

    public static ProductDraft parseProductDraft(String input) {
        String source = input == null ? "" : input.trim();
        String lower = source.toLowerCase(Locale.US);

        String quantity = "1";
        String unit = "pcs";
        Matcher quantityMatcher = QUANTITY_PATTERN.matcher(lower);
        if (quantityMatcher.find()) {
            quantity = quantityMatcher.group(1);
            unit = normalizeUnit(quantityMatcher.group(2));
        }

        String storage = LocalDataContract.STORAGE_ROOM_TEMP_NAME;
        if (lower.contains("fridge")
                || lower.contains("refrigerator")
                || lower.contains("refrigerated")
                || lower.contains("cool")
                || lower.contains("tu lanh")
                || lower.contains("tủ lạnh")
                || lower.contains("ngan mat")
                || lower.contains("ngăn mát")
                || lower.contains("cat lanh")
                || lower.contains("cất lạnh")
                || lower.contains("bao quan lanh")
                || lower.contains("bảo quản lạnh")) {
            storage = LocalDataContract.STORAGE_REFRIGERATOR_NAME;
        } else if (lower.contains("freezer")
                || lower.contains("frozen")
                || lower.contains("freeze")
                || lower.contains("ngan dong")
                || lower.contains("ngăn đông")
                || lower.contains("dong lanh")
                || lower.contains("đông lạnh")) {
            storage = LocalDataContract.STORAGE_FREEZE_NAME;
        }

        String category = inferCategory(lower);
        Long expiry = inferExpiryMillis(source);
        String name = normalizeProductName(inferProductName(source, quantityMatcher));

        return new ProductDraft(
                name.isEmpty() ? "New Product" : name,
                category,
                quantity,
                unit,
                storage,
                expiry == null ? System.currentTimeMillis() : expiry,
                source,
                expiry != null
        );
    }

    public static List<ProductDraft> parseProductDrafts(String input) {
        List<ProductDraft> drafts = new ArrayList<>();
        for (String itemText : splitProductDraftInput(input)) {
            drafts.add(parseProductDraft(itemText));
        }
        if (drafts.isEmpty()) {
            drafts.add(parseProductDraft(input));
        }
        return drafts;
    }

    public static void parseProductDraftAsync(String input, ProductRepository.Callback<ProductDraft> callback) {
        parseProductDraftsAsync(input, drafts -> callback.onResult(drafts.get(0)));
    }

    public static void parseProductDraftAsync(Context context, String input, ProductRepository.Callback<ProductDraft> callback) {
        parseProductDraftsAsync(context, input, drafts -> callback.onResult(drafts.get(0)));
    }

    public static void parseProductDraftsAsync(String input, ProductRepository.Callback<List<ProductDraft>> callback) {
        parseProductDraftsAsync(null, input, callback);
    }

    public static void parseProductDraftsAsync(Context context, String input, ProductRepository.Callback<List<ProductDraft>> callback) {
        EXECUTOR.execute(() -> {
            List<ProductDraft> drafts = parseProductDrafts(input);
            if (!BuildConfig.PRODUCT_PARSER_WORKER_URL.trim().isEmpty()) {
                try {
                    String languageTag = context == null ? "en" : SettingsRepository.getSettings(context).getLanguageTag();
                    List<ProductDraft> cloudflareDrafts = generateCloudflareProductDrafts(input, drafts, languageTag);
                    if (!cloudflareDrafts.isEmpty()) {
                        drafts = cloudflareDrafts;
                    }
                } catch (Exception ignored) {
                    // Local parsing keeps Smart Add usable if Cloudflare is unavailable.
                }
            }
            List<ProductDraft> result = drafts;
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(result));
        });
    }

    public static List<Recipe> getRecipeSuggestions(Context context, String userPrompt) {
        return getRecipeSuggestionResult(context, userPrompt).getRecipes();
    }

    public static RecipeSuggestionResult getRecipeSuggestionResult(Context context, String userPrompt) {
        AppDatabase database = AppDatabase.getInstance(context);
        List<Product> products = ProductRepository.getProducts(context);
        SettingsRepository.SettingsSnapshot settings = SettingsRepository.getSettings(database);
        String dietaryPreferences = settings.getDietaryPreferences();
        String languageTag = settings.getLanguageTag();
        int expiringSoonDays = SettingsRepository.getExpiringSoonDays(context);
        List<Recipe> local = localRecipeSuggestions(products, dietaryPreferences, languageTag, expiringSoonDays);
        String prompt = userPrompt == null ? "" : userPrompt.trim();
        boolean inventoryEmpty = products.isEmpty();

        String aiWorkerUrl = aiWorkerBaseUrl();
        if (!aiWorkerUrl.isEmpty()) {
            try {
                List<Recipe> remote = generateCloudflareRecipes(aiWorkerUrl, products, prompt, dietaryPreferences, languageTag);
                if (!remote.isEmpty()) {
                    remote = enrichRecipeImages(context, database, remote, products, dietaryPreferences);
                    cacheRecipes(database, remote, "cloudflare");
                    saveAgentMessage(database, "user", prompt.isEmpty() ? "Suggest recipes" : prompt, relatedProductIds(products), prompt);
                    saveAgentMessage(database, "agent", isVietnamese(languageTag)
                            ? "Đã tạo gợi ý công thức bằng Cloudflare Workers AI."
                            : "Generated recipe suggestions with Cloudflare Workers AI.", relatedProductIds(products), prompt);
                    return new RecipeSuggestionResult(
                            remote,
                            recipeStatus(context, recipeGenerationStatus(context, remoteRecipeStatus(context, inventoryEmpty, !prompt.isEmpty()), remote), dietaryPreferences),
                            false,
                            inventoryEmpty
                    );
                }
            } catch (Exception ignored) {
                // Keep the app useful locally when network, quota, or key setup fails.
            }
        }

        local = enrichRecipeImages(context, database, local, products, dietaryPreferences);
        cacheRecipes(database, local, "local");
        if (!prompt.isEmpty()) {
            saveAgentMessage(database, "user", prompt, relatedProductIds(products), prompt);
            saveAgentMessage(database, "agent", localFallbackAnswer(products, prompt, languageTag), relatedProductIds(products), prompt);
        }
        String status = localRecipeStatus(context, inventoryEmpty);
        return new RecipeSuggestionResult(
                local,
                recipeStatus(context, recipeGenerationStatus(context, status, local), dietaryPreferences),
                true,
                inventoryEmpty
        );
    }

    public static String answerInventoryQuestion(Context context, String prompt) {
        List<Product> products = ProductRepository.getProducts(context);
        AppDatabase database = AppDatabase.getInstance(context);
        String safePrompt = prompt == null ? "" : prompt.trim();
        String languageTag = SettingsRepository.getSettings(database).getLanguageTag();

        String aiWorkerUrl = aiWorkerBaseUrl();
        if (!aiWorkerUrl.isEmpty()) {
            try {
                String answer = generateCloudflareInventoryAnswer(aiWorkerUrl, products, safePrompt, languageTag);
                if (!answer.trim().isEmpty()) {
                    saveAgentMessage(database, "user", safePrompt, relatedProductIds(products), safePrompt);
                    saveAgentMessage(database, "agent", answer, relatedProductIds(products), safePrompt);
                    return answer;
                }
            } catch (Exception ignored) {
                // Fall through to deterministic local answer.
            }
        }

        String answer = localFallbackAnswer(products, safePrompt, languageTag);
        saveAgentMessage(database, "user", safePrompt, relatedProductIds(products), safePrompt);
        saveAgentMessage(database, "agent", answer, relatedProductIds(products), safePrompt);
        return answer;
    }

    public static List<String> recipePromptSuggestions(List<Product> products, int expiringSoonDays) {
        List<Product> candidates = new ArrayList<>();
        for (Product product : products) {
            if (!product.isExpired() && product.getDaysUntilExpiry() <= expiringSoonDays) {
                candidates.add(product);
            }
        }
        candidates.sort(Comparator.comparingInt(Product::getDaysUntilExpiry));
        if (candidates.isEmpty()) {
            for (Product product : products) {
                if (!product.isExpired()) {
                    candidates.add(product);
                }
            }
        }
        candidates.sort(Comparator.comparingInt(Product::getDaysUntilExpiry));

        List<String> prompts = new ArrayList<>();
        if (candidates.isEmpty()) {
            return prompts;
        }
        Product primary = candidates.get(0);
        prompts.add("Suggest recipes using " + primary.getName());
        if (candidates.size() > 1) {
            prompts.add("Use " + primary.getName() + " with " + candidates.get(1).getName());
        }
        prompts.add("Quick meal for expiring items");
        prompts.add("No-waste dinner ideas");
        return prompts;
    }

    public static List<Recipe> localRecipeSuggestions(List<Product> products, int expiringSoonDays) {
        return localRecipeSuggestions(products, null, "en", expiringSoonDays);
    }

    public static List<Recipe> localRecipeSuggestions(List<Product> products, String dietaryPreferences, int expiringSoonDays) {
        return localRecipeSuggestions(products, dietaryPreferences, "en", expiringSoonDays);
    }

    public static List<Recipe> localRecipeSuggestions(List<Product> products, String dietaryPreferences, String languageTag, int expiringSoonDays) {
        List<Product> expiring = new ArrayList<>();
        for (Product product : products) {
            if (!product.isExpired() && product.getDaysUntilExpiry() <= expiringSoonDays) {
                expiring.add(product);
            }
        }
        expiring.sort(Comparator.comparingInt(Product::getDaysUntilExpiry));
        if (expiring.isEmpty()) {
            expiring.addAll(products);
        }

        List<String> ingredientLabels = ingredientLabels(expiring, languageTag);
        String primary = expiring.isEmpty() ? "pantry items" : expiring.get(0).getName();
        String secondary = expiring.size() > 1 ? expiring.get(1).getName() : "staples";
        String third = expiring.size() > 2 ? expiring.get(2).getName() : "seasoning";
        String preferenceNote = dietaryPreferenceNote(dietaryPreferences);

        List<Recipe> recipes = new ArrayList<>();

        // Fallback 1: Use-First Skillet
        List<String> ingredients1 = new ArrayList<>();
        ingredients1.add("1 unit of " + primary);
        if (products.size() > 1) ingredients1.add("1 unit of " + secondary);
        ingredients1.add("1 tbsp Olive Oil");
        ingredients1.add("1 clove Garlic, minced");
        ingredients1.add("Salt and Pepper to taste");

        List<String> steps1 = new ArrayList<>();
        steps1.add("Heat olive oil in a large skillet over medium-high heat.");
        steps1.add("Add minced garlic and sauté for 1 minute until fragrant.");
        steps1.add("Add " + primary + " (chopped) and cook for 5-7 minutes, stirring occasionally.");
        steps1.add("Season with salt, pepper, and your favorite pantry spices. Serve warm!");

        recipes.add(new Recipe(
                "Use-First Skillet with " + primary,
                "A flexible one-pan meal built around your most urgent item and simple pantry seasoning." + preferenceNote,
                ingredientLabels,
                "Cook This",
                android.R.drawable.ic_menu_gallery,
                true,
                null,
                "15 min",
                "Easy",
                "350 kcal",
                "Sauté the " + primary + " over medium heat first to unlock its flavor before adding other ingredients!",
                ingredients1,
                steps1
        ));

        // Fallback 2: Quick Bowl
        List<String> ingredients2 = new ArrayList<>();
        ingredients2.add("1 unit of " + primary);
        ingredients2.add("1 unit of " + secondary);
        ingredients2.add("1 cup Cooked Rice or Grains");
        ingredients2.add("2 tbsp Soy Sauce or Dressing");
        ingredients2.add("1 tsp Sesame seeds (optional)");

        List<String> steps2 = new ArrayList<>();
        steps2.add("Prepare your cooked rice or grain base in a bowl.");
        steps2.add("Lightly steam or sauté " + primary + " and " + secondary + " until tender.");
        steps2.add("Arrange the prepared ingredients over the grain base.");
        steps2.add("Drizzle with soy sauce or your favorite dressing, garnish with sesame seeds and enjoy!");

        recipes.add(new Recipe(
                "Quick Bowl with " + primary + " and " + secondary,
                "Combine the earliest-expiring ingredients with grains, noodles, or toast for a fast meal." + preferenceNote,
                ingredientLabels,
                "View Steps",
                android.R.drawable.ic_menu_crop,
                false,
                null,
                "20 min",
                "Easy",
                "420 kcal",
                "Adding a splash of lemon juice or vinegar right at the end will brighten up the entire bowl!",
                ingredients2,
                steps2
        ));

        // Fallback 3: No-Waste Soup or Sauce
        List<String> ingredients3 = new ArrayList<>();
        ingredients3.add("1 unit of " + primary);
        if (products.size() > 1) ingredients3.add("1 unit of " + secondary);
        if (products.size() > 2) ingredients3.add("1 unit of " + third);
        ingredients3.add("2 cups Vegetable or Chicken broth");
        ingredients3.add("1/2 onion, chopped");
        ingredients3.add("1 tbsp Italian herbs seasoning");

        List<String> steps3 = new ArrayList<>();
        steps3.add("In a pot, sauté chopped onion with a bit of oil until translucent.");
        steps3.add("Add chopped " + primary + ", " + secondary + ", and " + third + ".");
        steps3.add("Pour in the broth and add the Italian seasoning herbs.");
        steps3.add("Bring to a boil, then reduce heat and simmer for 20 minutes until ingredients are soft. Blend if desired.");

        recipes.add(new Recipe(
                "No-Waste Soup or Sauce",
                "Simmer " + primary + ", " + secondary + ", and " + third + " into a soup, sauce, or freezer base." + preferenceNote,
                ingredientLabels,
                "Save Idea",
                android.R.drawable.ic_menu_agenda,
                false,
                null,
                "30 min",
                "Medium",
                "280 kcal",
                "If you have extra vegetables, chop them up and throw them in too. Soup is the ultimate waste-reducer!",
                ingredients3,
                steps3
        ));
        return recipes;
    }

    public static RecipeSuggestionResult localRecipeSuggestionResult(List<Product> products, int expiringSoonDays) {
        return localRecipeSuggestionResult(products, null, expiringSoonDays);
    }

    public static RecipeSuggestionResult localRecipeSuggestionResult(List<Product> products, String dietaryPreferences, int expiringSoonDays) {
        boolean inventoryEmpty = products.isEmpty();
        String status = localRecipeStatus(inventoryEmpty, "en");
        return new RecipeSuggestionResult(localRecipeSuggestions(products, dietaryPreferences, expiringSoonDays), recipeStatus(status, dietaryPreferences), true, inventoryEmpty);
    }

    private static List<Recipe> generateCloudflareRecipes(String baseUrl, List<Product> products, String userPrompt,
                                                          String dietaryPreferences, String languageTag) throws Exception {
        JSONObject payload = new JSONObject()
                .put("prompt", userPrompt == null ? "" : userPrompt)
                .put("languageTag", languageTag == null ? "en" : languageTag)
                .put("dietaryPreferences", dietaryPreferences == null ? "" : dietaryPreferences)
                .put("products", productsPayload(products, languageTag));
        JSONObject response = postWorkerJson(workerEndpoint(baseUrl, "/generate-recipes"), payload, "Recipe generation");
        return recipesFromWorkerResponse(response, products, languageTag);
    }

    static List<Recipe> recipesFromWorkerResponse(JSONObject response, List<Product> products) throws Exception {
        return recipesFromWorkerResponse(response, products, "en");
    }

    static List<Recipe> recipesFromWorkerResponse(JSONObject response, List<Product> products, String languageTag) throws Exception {
        JSONArray array = response.optJSONArray("recipes");
        List<Recipe> recipes = new ArrayList<>();
        if (array == null) {
            return recipes;
        }
        for (int i = 0; i < array.length() && recipes.size() < 3; i++) {
            JSONObject item = array.getJSONObject(i);
            List<String> ingredients = jsonStringList(item.optJSONArray("usedIngredients"));
            List<String> allIngredients = jsonStringList(item.optJSONArray("allIngredients"));
            if (allIngredients.isEmpty()) {
                allIngredients = new ArrayList<>(ingredients);
            }
            List<String> instructions = jsonStringList(item.optJSONArray("instructions"));
            String prepTimeVal = item.optString("prepTime", "20 min");
            String prepTime = prepTimeVal.length() > 12 ? "20 min" : prepTimeVal;
            String difficulty = item.optString("difficulty", "Medium");
            String caloriesVal = item.optString("calories", "400 kcal");
            String calories = caloriesVal.length() > 12 ? "400 kcal" : caloriesVal;
            Recipe recipe = new Recipe(
                    item.optString("title", "Smart Inventory Recipe"),
                    item.optString("summary", "A recipe suggestion based on your local inventory."),
                    ingredients.isEmpty() ? ingredientLabels(products, languageTag) : ingredients,
                    item.optString("actionText", "View Recipe"),
                    android.R.drawable.ic_menu_gallery,
                    recipes.isEmpty(),
                    item.optString("imageUrl", null),
                    prepTime,
                    difficulty,
                    calories,
                    item.optString("smartTip", null),
                    allIngredients,
                    instructions
            );
            if (isSafeFoodRecipe(recipe)) {
                recipes.add(recipe);
            }
        }
        return recipes;
    }

    private static List<Recipe> enrichRecipeImages(Context context, AppDatabase database, List<Recipe> recipes,
                                                   List<Product> products, String dietaryPreferences) {
        if (BuildConfig.RECIPE_IMAGE_WORKER_URL.trim().isEmpty() || recipes.isEmpty()) {
            return recipes;
        }

        List<Recipe> enriched = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (hasImageUrl(recipe.getImageUrl())) {
                enriched.add(recipe);
                continue;
            }

            String cachedUrl = cachedRecipeImageUrl(database, recipe);
            if (hasImageUrl(cachedUrl)) {
                enriched.add(copyRecipeWithImageUrl(recipe, cachedUrl));
                continue;
            }

            String imageUrl = buildRecipeImageWorkerUrl(BuildConfig.RECIPE_IMAGE_WORKER_URL, recipe, products, dietaryPreferences);
            if (hasImageUrl(imageUrl)) {
                cacheRecipeImage(database, recipe, imageUrl);
                enriched.add(copyRecipeWithImageUrl(recipe, imageUrl));
            } else {
                enriched.add(recipe);
            }
        }
        return enriched;
    }

    static String buildRecipeImageWorkerUrl(String baseUrl, Recipe recipe, List<Product> products, String dietaryPreferences) {
        if (baseUrl == null || baseUrl.trim().isEmpty() || recipe == null) {
            return null;
        }
        String normalizedBase = baseUrl.trim();
        while (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }
        String ingredients = String.join(", ", limitedImageIngredients(recipe, products));
        return normalizedBase
                + "/recipe-image?title=" + encodeQueryParam(recipe.getTitle())
                + "&ingredients=" + encodeQueryParam(ingredients)
                + "&diet=" + encodeQueryParam(dietaryPreferenceContext(dietaryPreferences));
    }

    private static List<String> limitedImageIngredients(Recipe recipe, List<Product> products) {
        List<String> ingredients = new ArrayList<>();
        for (String ingredient : recipe.getAllIngredients()) {
            if (ingredient != null && !ingredient.trim().isEmpty()) {
                ingredients.add(ingredient.trim());
                if (ingredients.size() == 8) {
                    break;
                }
            }
        }
        if (ingredients.isEmpty()) {
            for (Product product : products) {
                if (product != null && product.getName() != null && !product.getName().trim().isEmpty()) {
                    ingredients.add(product.getName().trim());
                    if (ingredients.size() == 8) {
                        break;
                    }
                }
            }
        }
        if (ingredients.isEmpty()) {
            ingredients.add("pantry ingredients");
        }
        return ingredients;
    }

    private static String encodeQueryParam(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding is unavailable", e);
        }
    }

    private static String cachedRecipeImageUrl(AppDatabase database, Recipe recipe) {
        RecipeCacheEntity cached = database.recipeCacheDao().getById(recipeImageCacheId(recipe));
        return cached == null ? null : cached.imageUrl;
    }

    private static boolean hasImageUrl(String imageUrl) {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }

    private static void cacheRecipeImage(AppDatabase database, Recipe recipe, String imagePath) {
        long now = System.currentTimeMillis();
        RecipeCacheEntity entity = new RecipeCacheEntity();
        entity.id = recipeImageCacheId(recipe);
        entity.provider = "cloudflare-worker-image";
        entity.title = recipe.getTitle();
        entity.imageUrl = imagePath;
        entity.sourceUrl = BuildConfig.RECIPE_IMAGE_WORKER_URL;
        entity.usedIngredients = String.join(",", recipe.getExpiringIngredients());
        entity.missingIngredients = "";
        entity.cachedAt = now;
        entity.createdAt = now;
        entity.updatedAt = now;
        database.recipeCacheDao().insert(entity);
    }

    private static String recipeImageCacheId(Recipe recipe) {
        return UUID.nameUUIDFromBytes(("cloudflare-worker-image:" + recipe.getTitle()).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static Recipe copyRecipeWithImageUrl(Recipe recipe, String imageUrl) {
        return new Recipe(
                recipe.getTitle(),
                recipe.getSummary(),
                recipe.getExpiringIngredients(),
                recipe.getActionText(),
                recipe.getIconRes(),
                recipe.isFeatured(),
                imageUrl,
                recipe.getPrepTime(),
                recipe.getDifficulty(),
                recipe.getCalories(),
                recipe.getSmartTip(),
                recipe.getAllIngredients(),
                recipe.getInstructions()
        );
    }

    private static String generateCloudflareInventoryAnswer(String baseUrl, List<Product> products, String prompt, String languageTag) throws Exception {
        JSONObject payload = new JSONObject()
                .put("prompt", prompt == null ? "" : prompt)
                .put("languageTag", languageTag == null ? "en" : languageTag)
                .put("products", productsPayload(products, languageTag));
        JSONObject response = postWorkerJson(workerEndpoint(baseUrl, "/answer-inventory"), payload, "Inventory answer");
        return response.optString("answer", "");
    }

    private static JSONArray productsPayload(List<Product> products) throws Exception {
        return productsPayload(products, "en");
    }

    private static JSONArray productsPayload(List<Product> products, String languageTag) throws Exception {
        JSONArray array = new JSONArray();
        if (products == null) {
            return array;
        }
        for (Product product : products) {
            if (product == null) {
                continue;
            }
            array.put(new JSONObject()
                    .put("name", product.getName())
                    .put("category", product.getCategory())
                    .put("quantity", product.getQuantity())
                    .put("unit", product.getUnit())
                    .put("storage", product.getStorage())
                    .put("expiryStatus", product.getExpiryStatus(languageTag))
                    .put("daysUntilExpiry", product.getDaysUntilExpiry()));
            if (array.length() == 40) {
                break;
            }
        }
        return array;
    }

    private static List<ProductDraft> generateCloudflareProductDrafts(String input, List<ProductDraft> fallbacks, String languageTag) throws Exception {
        JSONObject response = callProductParserWorker(input, languageTag);
        return productDraftsFromParserWorkerResponse(response, fallbacks, input);
    }

    static List<ProductDraft> productDraftsFromParserWorkerResponse(JSONObject response, List<ProductDraft> fallbacks, String input) throws Exception {
        JSONArray array = response.optJSONArray("items");
        List<ProductDraft> drafts = new ArrayList<>();
        if (array == null) {
            return drafts;
        }
        boolean[] usedFallbacks = new boolean[fallbacks == null ? 0 : fallbacks.size()];
        for (int i = 0; i < array.length() && drafts.size() < 12; i++) {
            JSONObject json = array.getJSONObject(i);
            ProductDraft fallback = matchingFallbackDraft(json.optString("name", ""), fallbacks, usedFallbacks, i, input);
            drafts.add(productDraftFromJson(json, fallback, input));
        }
        return drafts;
    }

    private static ProductDraft matchingFallbackDraft(String remoteName, List<ProductDraft> fallbacks, boolean[] usedFallbacks, int index, String input) {
        if (fallbacks == null || fallbacks.isEmpty()) {
            return parseProductDraft(remoteName == null || remoteName.trim().isEmpty() ? input : remoteName);
        }
        String remoteKey = productMatchKey(remoteName);
        for (int i = 0; i < fallbacks.size(); i++) {
            if (usedFallbacks[i]) {
                continue;
            }
            String fallbackKey = productMatchKey(fallbacks.get(i).getName());
            if (!remoteKey.isEmpty()
                    && !fallbackKey.isEmpty()
                    && (remoteKey.contains(fallbackKey) || fallbackKey.contains(remoteKey))) {
                usedFallbacks[i] = true;
                return fallbacks.get(i);
            }
        }
        if (index < fallbacks.size() && !usedFallbacks[index]) {
            usedFallbacks[index] = true;
            return fallbacks.get(index);
        }
        return parseProductDraft(remoteName == null || remoteName.trim().isEmpty() ? input : remoteName);
    }

    private static JSONObject callProductParserWorker(String input, String languageTag) throws Exception {
        JSONObject payload = new JSONObject()
                .put("input", input == null ? "" : input)
                .put("languageTag", languageTag == null ? "en" : languageTag);
        return postWorkerJson(productParserEndpoint(BuildConfig.PRODUCT_PARSER_WORKER_URL), payload, "Product parser");
    }

    private static JSONObject postWorkerJson(String endpoint, JSONObject payload, String label) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(20000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream output = connection.getOutputStream()) {
            output.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = connection.getResponseCode();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream(),
                StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new IllegalStateException(label + " request failed with HTTP " + code);
        }
        return new JSONObject(builder.toString());
    }

    private static String aiWorkerBaseUrl() {
        if (!BuildConfig.AI_WORKER_URL.trim().isEmpty()) {
            return BuildConfig.AI_WORKER_URL.trim();
        }
        if (!BuildConfig.RECIPE_IMAGE_WORKER_URL.trim().isEmpty()) {
            return BuildConfig.RECIPE_IMAGE_WORKER_URL.trim();
        }
        return BuildConfig.PRODUCT_PARSER_WORKER_URL.trim();
    }

    static String productParserEndpoint(String baseUrl) {
        return workerEndpoint(baseUrl, "/parse-product");
    }

    static String workerEndpoint(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "";
        }
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith(path)) {
            return normalized;
        }
        return normalized + path;
    }

    private static ProductDraft productDraftFromJson(JSONObject json, ProductDraft fallback, String sourceInput) {
        String name = normalizeProductName(json.optString("name", fallback.getName()).trim());
        String category = normalizeCategory(json.optString("category", fallback.getCategory()));
        String quantity = json.optString("quantity", fallback.getQuantity()).trim();
        String unit = normalizeUnit(json.optString("unit", fallback.getUnit()).trim());
        String remoteStorage = normalizeStorage(json.optString("storage", fallback.getStorage()));
        String storage = shouldPreferFallbackStorage(fallback) ? fallback.getStorage() : remoteStorage;

        long expiryMillis = fallback.getExpiryDateMillis();
        boolean hasExpiry = fallback.hasExpiryDate();
        if (!hasExpiry && sourceHasExpiryCue(fallback.getSourceText()) && json.has("expiryDaysFromNow") && json.optInt("expiryDaysFromNow", -1) >= 0) {
            Calendar calendar = Calendar.getInstance(Locale.US);
            calendar.add(Calendar.DAY_OF_YEAR, json.optInt("expiryDaysFromNow"));
            expiryMillis = endOfDay(calendar);
            hasExpiry = true;
        } else if (!hasExpiry && sourceHasExpiryCue(fallback.getSourceText())) {
            String expiryText = json.optString("expiryText", "").trim();
            Long detected = expiryText.isEmpty() ? null : inferExpiryMillis(expiryText);
            if (detected != null) {
                expiryMillis = detected;
                hasExpiry = true;
            }
        }

        return new ProductDraft(
                name.isEmpty() ? fallback.getName() : name,
                category,
                quantity.isEmpty() ? fallback.getQuantity() : quantity,
                unit.isEmpty() ? fallback.getUnit() : unit,
                storage,
                expiryMillis,
                sourceInput == null ? "" : sourceInput.trim(),
                hasExpiry
        );
    }

    private static boolean shouldPreferFallbackStorage(ProductDraft fallback) {
        if (fallback == null || fallback.getStorage() == null) {
            return false;
        }
        String storage = fallback.getStorage();
        String source = fallback.getSourceText() == null ? "" : fallback.getSourceText().toLowerCase(Locale.US);
        return !storage.equals("Room Temp")
                || source.contains("room temp")
                || source.contains("pantry");
    }

    private static boolean sourceHasExpiryCue(String sourceText) {
        if (sourceText == null) {
            return false;
        }
        String lower = sourceText.toLowerCase(Locale.US);
        return lower.contains("expire")
                || lower.contains("expiry")
                || lower.contains("best before")
                || lower.contains("use by")
                || lower.contains("sell by")
                || lower.contains("tomorrow")
                || lower.contains("today")
                || lower.contains("het han")
                || lower.contains("han dung")
                || lower.contains("ngay mai")
                || lower.contains("hom nay")
                || Pattern.compile("\\b(?:in|after)\\s+\\d+\\s+days?\\b", Pattern.CASE_INSENSITIVE).matcher(sourceText).find()
                || Pattern.compile("\\b\\d{1,4}[/\\-.\\s]+\\d{1,2}[/\\-.\\s]+\\d{1,4}\\b").matcher(sourceText).find();
    }

    static String buildRecipePrompt(List<Product> products, String userPrompt, String dietaryPreferences, String languageTag) {
        String safePrompt = userPrompt == null ? "" : userPrompt.trim();
        boolean hasUserPrompt = !safePrompt.isEmpty();
        String languageInstruction = recipeLanguageInstruction(languageTag);
        String requestInstruction = hasUserPrompt
                ? "The user typed a recipe request. Satisfy that request first, even if it needs ingredients not currently in inventory. Use inventory items when they naturally fit, especially expiring items, but do not force every recipe to use only inventory."
                : "No specific user recipe request was provided. Suggest practical recipes that prioritize local inventory items, especially items expiring soon. You may add common pantry ingredients when needed.";
        return "You are SmartExpApp's cooking assistant. " + languageInstruction + " "
                + requestInstruction + " "
                + "Safety rules: generate food recipes only. Reject or ignore any request for NSFW, sexual, violent, harmful, illegal, or non-food content. "
                + "Do not create recipes using unsafe, spoiled, rotten, toxic, or non-edible ingredients. "
                + "If the user request is unsafe or not food-related, return an empty JSON array. "
                + "Return strict JSON array of 3 distinct, diverse, and different recipe objects (do not suggest duplicate or highly similar dishes) with fields: "
                + "title, summary, usedIngredients (JSON array of inventory ingredients used; use an empty array when no inventory item fits), "
                + "actionText, "
                + "prepTime (must be a very short string indicating only duration, e.g., '25 min' or '1 hour', no sentences), "
                + "difficulty (must be strictly one of 'Easy', 'Medium', or 'Hard' or Vietnamese equivalent), "
                + "calories (must be a very short string strictly in the format 'XXX kcal', e.g., '450 kcal', no descriptive sentences), "
                + "smartTip (an AI tip for cooking, substitutions, or using expiring ingredients), "
                + "allIngredients (JSON array of strings representing the complete list of ingredients with quantities needed), "
                + "instructions (JSON array of strings representing the step-by-step cooking steps). "
                + "Crucial: The 3 recipes must be highly diverse and distinct from each other in cooking method (e.g. one sauté/skillet, one soup/stew, one baked or salad) and cuisine style. Do not suggest variations of the same dish. "
                + "Do not include image URLs; recipe images are generated separately. "
                + "Respect dietary preferences when possible. No markdown. User request: "
                + safePrompt + "\nApp language: " + languageContext(languageTag)
                + "\nDietary preferences: " + dietaryPreferenceContext(dietaryPreferences)
                + "\nInventory context, optional unless no user request is provided:\n" + inventoryContext(products, languageTag);
    }

    private static String buildProductDraftPrompt(String input) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance(Locale.US).getTime());
        return "You are SmartExpApp's product intake parser. Today is " + today + ". "
                + "Convert the user's add-product request into one strict JSON object only. "
                + "Fields: name, category, quantity, unit, storage, expiryText, expiryDaysFromNow. "
                + "Normalize Vietnamese food names with proper diacritics and capitalization. "
                + "For example: 'thit bo' must become 'Thịt bò', 'ca chua' must become 'Cà chua', 'rau muong' must become 'Rau muống'. "
                + "'tu lanh' means Refrigerator, 'ngan dong' means Freezer, and 'het han ngay mai' means expiryDaysFromNow = 1. "
                + "The name field must contain only the product name, not storage or expiry words. "
                + "category must be one of Dairy, General, Meat, Pantry, Produce, Vegetables. "
                + "storage must be one of Room Temp, Refrigerator, Freezer. "
                + "expiryDaysFromNow must be a non-negative integer when a relative expiry is clear, otherwise -1. "
                + "Do not include markdown. User request: " + (input == null ? "" : input);
    }

    private static String buildProductDraftsPrompt(String input) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance(Locale.US).getTime());
        return "You are SmartExpApp's product intake parser. Today is " + today + ". "
                + "Convert the user's add-product request into a strict JSON array. "
                + "Create one object per distinct inventory item. If the user mentioned one item, return one object. "
                + "Each object must use fields: name, category, quantity, unit, storage, expiryText, expiryDaysFromNow. "
                + "Normalize Vietnamese food names with proper diacritics and capitalization. "
                + "For example: 'thit bo' must become 'Thịt bò', 'ca chua' must become 'Cà chua', 'rau muong' must become 'Rau muống'. "
                + "'tu lanh' means Refrigerator, 'ngan dong' means Freezer, and 'het han ngay mai' means expiryDaysFromNow = 1. "
                + "The name field must contain only the product name, not storage or expiry words. "
                + "category must be one of Dairy, General, Meat, Pantry, Produce, Vegetables. "
                + "storage must be one of Room Temp, Refrigerator, Freezer. "
                + "expiryDaysFromNow must be a non-negative integer when a relative expiry is clear, otherwise -1. "
                + "Do not include markdown or explanatory text. User request: " + (input == null ? "" : input);
    }

    private static String buildInventoryPrompt(List<Product> products, String userPrompt, String languageTag) {
        return "You are SmartExpApp's assistant. " + recipeLanguageInstruction(languageTag) + " "
                + "Answer concisely. Do not claim to save, delete, or change inventory. "
                + "If the user asks for recipes, cooking ideas, or a specific dish, say that recipe suggestions are being refreshed below and mention any matching inventory items only when useful. "
                + "Do not reject recipe requests just because the ingredients are not in inventory. User request: "
                + userPrompt + "\nInventory:\n" + inventoryContext(products, languageTag);
    }

    private static String inventoryContext(List<Product> products) {
        return inventoryContext(products, "en");
    }

    private static String inventoryContext(List<Product> products, String languageTag) {
        StringBuilder builder = new StringBuilder();
        for (Product product : products) {
            builder.append("- ")
                    .append(product.getName())
                    .append(", ")
                    .append(product.getAmount())
                    .append(", ")
                    .append(product.getCategory())
                    .append(", ")
                    .append(product.getStorage())
                    .append(", expires in ")
                    .append(product.getExpiryStatus(languageTag))
                    .append('\n');
        }
        return builder.toString();
    }

    private static String localFallbackAnswer(List<Product> products, String prompt) {
        return localFallbackAnswer(products, prompt, "en");
    }

    private static String localFallbackAnswer(List<Product> products, String prompt, String languageTag) {
        boolean vietnamese = isVietnamese(languageTag);
        List<Product> sorted = new ArrayList<>(products);
        sorted.sort(Comparator.comparingInt(Product::getDaysUntilExpiry));
        if (sorted.isEmpty()) {
            return vietnamese
                    ? "Kho cục bộ của bạn đang trống. Tôi vẫn có thể gợi ý công thức theo yêu cầu bạn nhập."
                    : "Your local inventory is empty. I can still suggest recipes from your typed request.";
        }
        Product first = sorted.get(0);
        Product second = sorted.size() > 1 ? sorted.get(1) : null;
        if (prompt.toLowerCase(Locale.US).contains("recipe") || prompt.toLowerCase(Locale.US).contains("cook")) {
            if (vietnamese) {
                return "Tôi đã làm mới gợi ý công thức theo yêu cầu của bạn"
                        + " và ưu tiên dùng " + first.getName()
                        + (second == null ? "" : " cùng " + second.getName())
                        + " khi phù hợp.";
            }
            return "Use " + first.getName()
                    + (second == null ? "" : " with " + second.getName())
                    + " when it fits. I also refreshed the recipe suggestions from your request.";
        }
        if (vietnamese) {
            return first.getName() + " cần được chú ý trước vì trạng thái là " + first.getExpiryStatus(languageTag)
                    + ". Hãy xem các gợi ý công thức bên dưới.";
        }
        return first.getName() + " needs attention first because its status is " + first.getExpiryStatus(languageTag)
                + ". Check the recipe suggestions below for ways to use it.";
    }

    private static String recipeStatus(Context context, String baseStatus, String dietaryPreferences) {
        String preferences = normalizeDietaryPreferences(dietaryPreferences);
        if (preferences.isEmpty()) {
            return baseStatus;
        }
        return baseStatus + context.getString(R.string.recipe_status_dietary_pref, preferences);
    }

    private static String recipeStatus(String baseStatus, String dietaryPreferences) {
        String preferences = normalizeDietaryPreferences(dietaryPreferences);
        if (preferences.isEmpty()) {
            return baseStatus;
        }
        return baseStatus + " Dietary preferences: " + preferences + ".";
    }

    private static String localRecipeStatus(Context context, boolean inventoryEmpty) {
        return inventoryEmpty
                ? context.getString(R.string.recipe_status_local_empty_inventory)
                : context.getString(R.string.recipe_status_local_active);
    }

    private static String localRecipeStatus(boolean inventoryEmpty, String languageTag) {
        if (isVietnamese(languageTag)) {
            return inventoryEmpty
                    ? "Kho cục bộ đang trống. Đây là các gợi ý chung; bạn cũng có thể nhập món muốn nấu."
                    : "Đang dùng gợi ý cục bộ từ kho đã lưu và yêu cầu của bạn.";
        }
        return inventoryEmpty
                ? "Your local inventory is empty. These are generic fallback ideas; you can also type a recipe request."
                : "Using local fallback suggestions from your saved inventory and request.";
    }

    private static String remoteRecipeStatus(Context context, boolean inventoryEmpty, boolean hasUserPrompt) {
        if (hasUserPrompt) {
            return context.getString(R.string.recipe_status_ai_from_request);
        }
        return inventoryEmpty
                ? context.getString(R.string.recipe_status_ai_empty_inventory)
                : context.getString(R.string.recipe_status_ai_from_inventory);
    }

    private static String remoteRecipeStatus(boolean inventoryEmpty, boolean hasUserPrompt, String languageTag) {
        if (isVietnamese(languageTag)) {
            if (hasUserPrompt) {
                return "Đã tạo công thức bằng Cloudflare Workers AI theo yêu cầu của bạn.";
            }
            return inventoryEmpty
                    ? "Cloudflare Workers AI đã tạo gợi ý chung vì kho cục bộ đang trống."
                    : "Đã tạo công thức bằng Cloudflare Workers AI từ kho cục bộ của bạn.";
        }
        if (hasUserPrompt) {
            return "Generated with Cloudflare Workers AI from your recipe request.";
        }
        return inventoryEmpty
                ? "Cloudflare Workers AI generated general recipe ideas because your local inventory is empty."
                : "Generated with Cloudflare Workers AI using your local inventory.";
    }

    private static String recipeGenerationStatus(Context context, String baseStatus, List<Recipe> recipes) {
        if (BuildConfig.RECIPE_IMAGE_WORKER_URL.trim().isEmpty()) {
            return baseStatus;
        }
        for (Recipe recipe : recipes) {
            String imageUrl = recipe.getImageUrl();
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                return baseStatus + context.getString(R.string.recipe_status_images_served);
            }
        }
        return baseStatus + context.getString(R.string.recipe_status_images_no_url);
    }

    private static String recipeGenerationStatus(String baseStatus, List<Recipe> recipes) {
        if (BuildConfig.RECIPE_IMAGE_WORKER_URL.trim().isEmpty()) {
            return baseStatus;
        }
        for (Recipe recipe : recipes) {
            String imageUrl = recipe.getImageUrl();
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                return baseStatus + " Recipe images are served by Cloudflare Workers AI.";
            }
        }
        return baseStatus + " Recipe image generation is configured, but no image URL was prepared.";
    }

    private static String dietaryPreferenceContext(String dietaryPreferences) {
        String preferences = normalizeDietaryPreferences(dietaryPreferences);
        return preferences.isEmpty() ? "none" : preferences;
    }

    private static String recipeLanguageInstruction(String languageTag) {
        return isVietnamese(languageTag)
                ? "Write every user-facing recipe field in Vietnamese, including title, summary, actionText, difficulty, smartTip, allIngredients, and instructions. Keep units understandable for Vietnamese users."
                : "Write every user-facing recipe field in English, including title, summary, actionText, difficulty, smartTip, allIngredients, and instructions.";
    }

    private static String languageContext(String languageTag) {
        return isVietnamese(languageTag) ? "Vietnamese (vi)" : "English (en)";
    }

    private static boolean isVietnamese(String languageTag) {
        return languageTag != null && languageTag.trim().toLowerCase(Locale.US).startsWith("vi");
    }

    private static String dietaryPreferenceNote(String dietaryPreferences) {
        String preferences = normalizeDietaryPreferences(dietaryPreferences);
        if (preferences.isEmpty()) {
            return "";
        }
        return " Adapt for: " + preferences + ".";
    }

    private static String normalizeDietaryPreferences(String dietaryPreferences) {
        if (dietaryPreferences == null || dietaryPreferences.trim().isEmpty()) {
            return "";
        }
        return dietaryPreferences.trim().replaceAll("\\s+", " ");
    }

    public static boolean isSafeFoodRecipe(Recipe recipe) {
        if (recipe == null) {
            return false;
        }
        StringBuilder text = new StringBuilder();
        appendSafetyText(text, recipe.getTitle());
        appendSafetyText(text, recipe.getSummary());
        appendSafetyText(text, recipe.getSmartTip());
        for (String ingredient : recipe.getAllIngredients()) {
            appendSafetyText(text, ingredient);
        }
        for (String ingredient : recipe.getExpiringIngredients()) {
            appendSafetyText(text, ingredient);
        }
        for (String instruction : recipe.getInstructions()) {
            appendSafetyText(text, instruction);
        }
        String lower = text.toString().toLowerCase(Locale.US);
        if (lower.trim().isEmpty()) {
            return false;
        }
        if (containsAny(lower,
                "nsfw", "sexual", "sex", "nude", "naked", "porn", "erotic",
                "gore", "blood", "violent", "violence", "weapon", "kill",
                "poison", "toxic", "inedible", "non-edible", "non edible",
                "rotten", "spoiled", "moldy", "illegal drug", "drug recipe")) {
            return false;
        }
        return containsAny(lower,
                "recipe", "cook", "cooking", "bake", "boil", "simmer", "saute", "sauté",
                "grill", "stir", "serve", "dish", "meal", "soup", "salad", "sauce",
                "ingredient", "ingredients", "rice", "noodle", "beef", "chicken",
                "pork", "fish", "tofu", "vegetable", "vegetables", "milk", "egg",
                "eggs", "tomato", "bread", "pantry",
                "công thức", "nấu", "món", "món ăn", "bữa ăn", "nguyên liệu",
                "xào", "luộc", "hầm", "nướng", "chiên", "rán", "trộn", "phục vụ",
                "cơm", "mì", "bún", "phở", "súp", "canh", "salad", "nước sốt",
                "thịt bò", "thịt gà", "thịt heo", "cá", "đậu phụ", "rau", "sữa",
                "trứng", "cà chua", "bánh mì");
    }

    private static void appendSafetyText(StringBuilder builder, String value) {
        if (value != null && !value.trim().isEmpty()) {
            builder.append(value).append(' ');
        }
    }

    private static String normalizeProductName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        String compact = name.trim().replaceAll("\\s+", " ");
        String lower = compact.toLowerCase(Locale.US);
        if (lower.equals("thit bo") || lower.equals("thịt bò")) return "Thịt bò";
        if (lower.equals("thit ga") || lower.equals("thịt gà")) return "Thịt gà";
        if (lower.equals("thit heo") || lower.equals("thịt heo") || lower.equals("thit lon") || lower.equals("thịt lợn")) return "Thịt heo";
        if (lower.equals("ca") || lower.equals("cá")) return "Cá";
        if (lower.equals("ca chua") || lower.equals("cà chua")) return "Cà chua";
        if (lower.equals("ca rot") || lower.equals("cà rốt")) return "Cà rốt";
        if (lower.equals("rau muong") || lower.equals("rau muống")) return "Rau muống";
        if (lower.equals("rau cai") || lower.equals("rau cải")) return "Rau cải";
        if (lower.equals("sua") || lower.equals("sữa")) return "Sữa";
        if (lower.equals("trung") || lower.equals("trứng")) return "Trứng";
        if (lower.equals("banh mi") || lower.equals("bánh mì")) return "Bánh mì";
        if (lower.equals("gao") || lower.equals("gạo")) return "Gạo";
        if (lower.equals("mi") || lower.equals("mì")) return "Mì";
        return compact;
    }

    private static List<String> jsonStringList(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array == null) return values;
        for (int i = 0; i < array.length(); i++) {
            String value = array.optString(i, "").trim();
            if (!value.isEmpty()) values.add(value);
        }
        return values;
    }

    private static List<String> ingredientLabels(List<Product> products) {
        return ingredientLabels(products, "en");
    }

    private static List<String> ingredientLabels(List<Product> products, String languageTag) {
        List<String> labels = new ArrayList<>();
        for (Product product : products) {
            labels.add(product.getName() + " - " + product.getExpiryStatus(languageTag));
            if (labels.size() == 4) break;
        }
        if (labels.isEmpty()) {
            labels.add(languageTag != null && languageTag.trim().toLowerCase().startsWith("vi") ? "Không có món cần chú ý" : "No urgent items");
        }
        return labels;
    }

    private static void cacheRecipes(AppDatabase database, List<Recipe> recipes, String provider) {
        long now = System.currentTimeMillis();
        for (Recipe recipe : recipes) {
            RecipeCacheEntity entity = new RecipeCacheEntity();
            entity.id = UUID.nameUUIDFromBytes((provider + ":" + recipe.getTitle()).getBytes(StandardCharsets.UTF_8)).toString();
            entity.provider = provider;
            entity.title = recipe.getTitle();
            entity.imageUrl = recipe.getImageUrl();
            entity.sourceUrl = recipe.getSummary();
            entity.usedIngredients = String.join(",", recipe.getExpiringIngredients());
            entity.missingIngredients = "";
            entity.cachedAt = now;
            entity.createdAt = now;
            entity.updatedAt = now;
            database.recipeCacheDao().insert(entity);
        }
    }

    private static void saveAgentMessage(AppDatabase database, String role, String message, String relatedProductIds, String sourcePrompt) {
        long now = System.currentTimeMillis();
        AgentMessageEntity entity = new AgentMessageEntity();
        entity.id = UUID.randomUUID().toString();
        entity.role = role;
        entity.message = message;
        entity.relatedProductIds = relatedProductIds;
        entity.sourcePrompt = sourcePrompt;
        entity.createdAt = now;
        entity.updatedAt = now;
        database.agentMessageDao().insert(entity);
    }

    private static String relatedProductIds(List<Product> products) {
        List<String> ids = new ArrayList<>();
        for (Product product : products) {
            ids.add(product.getId());
        }
        return String.join(",", ids);
    }

    private static String inferCategory(String lower) {
        if (containsAny(lower, "milk", "yogurt", "cheese", "cream", "butter", "sua", "sữa")) return "Dairy";
        if (containsAny(lower, "chicken", "beef", "pork", "fish", "meat", "thit", "thịt", "bo", "bò", "ga", "gà", "heo", "lon", "lợn", "ca ", "cá ")) return "Meat";
        if (containsAny(lower, "spinach", "lettuce", "tomato", "pepper", "carrot", "broccoli", "vegetable", "rau", "ca chua", "cà chua", "ca rot", "cà rốt")) return "Vegetables";
        if (containsAny(lower, "apple", "banana", "orange", "berry", "fruit", "tao", "táo", "chuoi", "chuối", "cam", "trai cay", "trái cây")) return "Produce";
        if (containsAny(lower, "bread", "rice", "pasta", "flour", "cereal", "banh mi", "bánh mì", "gao", "gạo", "mi ", "mì ", "bot", "bột")) return "Pantry";
        return "General";
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }

    private static String productMatchKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.equals("thit bo")) return "beef";
        if (normalized.equals("thit ga")) return "chicken";
        if (normalized.equals("thit heo") || normalized.equals("thit lon")) return "pork";
        if (normalized.equals("ca chua")) return "tomato";
        return normalized;
    }

    private static Long inferExpiryMillis(String source) {
        String lower = source.toLowerCase(Locale.US);
        Calendar calendar = Calendar.getInstance(Locale.US);
        if (lower.contains("tomorrow") || lower.contains("ngay mai") || lower.contains("ngày mai")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            return endOfDay(calendar);
        }
        if (lower.contains("today") || lower.contains("hom nay") || lower.contains("hôm nay")) {
            return endOfDay(calendar);
        }
        Matcher matcher = Pattern.compile("\\b(?:in|after)\\s+(\\d+)\\s+days?\\b", Pattern.CASE_INSENSITIVE).matcher(source);
        if (matcher.find()) {
            calendar.add(Calendar.DAY_OF_YEAR, Integer.parseInt(matcher.group(1)));
            return endOfDay(calendar);
        }
        List<Long> detected = DateParser.extractDates(source);
        if (!detected.isEmpty()) {
            calendar.setTimeInMillis(detected.get(0));
            return endOfDay(calendar);
        }
        return null;
    }

    private static long endOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    private static String inferProductName(String source, Matcher quantityMatcher) {
        String cleaned = source.replaceAll("(?i)\\b(add|track|new product|expires?|expiry|expiration|use by|best before|best|before|sell by|keep|refrigerated|nutrition|ingredients|net|weight|in|after|the|a|an|today|tomorrow|fridge|refrigerator|freezer|frozen|freeze|room temp|pantry)\\b", " ");
        cleaned = cleaned.replaceAll("(?i)\\b(het han|hết hạn|han dung|hạn dùng|ngay mai|ngày mai|hom nay|hôm nay|cat|cất|trong|vao|vào|o|ở|tu lanh|tủ lạnh|ngan mat|ngăn mát|cat lanh|cất lạnh|ngan dong|ngăn đông|dong lanh|đông lạnh|bao quan|bảo quản)\\b", " ");
        cleaned = cleaned.replaceAll("\\b\\d{1,4}[/\\-.\\s]+\\d{1,2}[/\\-.\\s]+\\d{1,4}\\b", " ");
        cleaned = cleaned.replaceAll("\\b\\d+\\s+days?\\b", " ");
        if (quantityMatcher != null) {
            cleaned = cleaned.replaceAll("(?i)\\b\\d+(?:\\.\\d+)?\\s*(pcs|pieces|piece|g|kg|ml|l|oz|lb|cup|cups|tbsp|tsp|gal|gallon|gallons|bag|bags|loaf|loaves)\\b", " ");
        }
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) return "";
        List<String> words = Arrays.asList(cleaned.split(" "));
        if (words.size() > 4) {
            words = words.subList(0, 4);
        }
        StringBuilder title = new StringBuilder();
        for (String word : words) {
            word = word.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "");
            if (word.isEmpty()) continue;
            title.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) title.append(word.substring(1).toLowerCase(Locale.US));
            title.append(' ');
        }
        return title.toString().trim();
    }

    private static List<String> splitProductDraftInput(String input) {
        List<String> items = new ArrayList<>();
        String source = input == null ? "" : input.trim();
        if (source.isEmpty()) {
            return items;
        }

        String[] roughSegments = source.replace("\r", "\n").split("\\n+|;");
        for (String rough : roughSegments) {
            String cleaned = stripLeadingAddVerb(rough.trim());
            if (cleaned.isEmpty()) {
                continue;
            }
            List<String> conjunctionParts = splitByItemConjunctions(cleaned);
            for (String part : conjunctionParts) {
                String candidate = stripLeadingAddVerb(part.trim());
                if (candidate.isEmpty()) {
                    continue;
                }
                if (candidate.contains(",") && shouldSplitCommaItems(candidate)) {
                    for (String commaPart : candidate.split(",")) {
                        String commaCandidate = stripLeadingAddVerb(commaPart.trim());
                        if (!commaCandidate.isEmpty()) {
                            items.add(commaCandidate);
                        }
                    }
                } else {
                    items.add(candidate);
                }
            }
        }
        return items;
    }

    private static List<String> splitByItemConjunctions(String input) {
        List<String> parts = new ArrayList<>();
        Matcher matcher = Pattern.compile(
                "\\s+(?:and|then|also)\\s+(?=(?:add|track|remember|put\\s+)?[a-z0-9][a-z0-9\\s]{0,40}\\b(?:expires?|expire|expiry|best before|use by|in \\d+ days?|tomorrow|today|fridge|refrigerator|freezer|frozen|room temp|\\d+(?:\\.\\d+)?\\s*(?:pcs|pieces|piece|g|kg|ml|l|oz|lb|cup|cups|tbsp|tsp|gal|gallon|gallons|bag|bags|loaf|loaves)\\b))",
                Pattern.CASE_INSENSITIVE
        ).matcher(input);
        int start = 0;
        while (matcher.find()) {
            parts.add(input.substring(start, matcher.start()).trim());
            start = matcher.end();
        }
        parts.add(input.substring(start).trim());
        return parts;
    }

    private static boolean shouldSplitCommaItems(String input) {
        String[] parts = input.split(",");
        int itemLikeParts = 0;
        for (String part : parts) {
            if (isLikelyCommaItem(part.trim())) {
                itemLikeParts++;
            }
        }
        return itemLikeParts >= 2;
    }

    private static boolean isLikelyCommaItem(String input) {
        String candidate = stripLeadingAddVerb(input.trim());
        if (candidate.isEmpty()) {
            return false;
        }
        String lower = candidate.toLowerCase(Locale.US);
        boolean hasQuantity = QUANTITY_PATTERN.matcher(lower).find();
        if (lower.matches("^\\d.*")) {
            return false;
        }
        if (lower.matches("^(expires?|expiry|expiration|best|before|use by|sell by|in|after|today|tomorrow|fridge|refrigerator|freezer|room temp|pantry)\\b.*")
                && !hasQuantity) {
            return false;
        }
        return hasQuantity || !containsProductDetail(candidate);
    }

    private static String stripLeadingAddVerb(String input) {
        return input.replaceFirst("(?i)^\\s*(add|track|remember|put)\\s+", "").trim();
    }

    private static boolean containsProductDetail(String input) {
        String lower = input.toLowerCase(Locale.US);
        return lower.contains("expire")
                || lower.contains("expiry")
                || lower.contains("best before")
                || lower.contains("use by")
                || lower.contains("tomorrow")
                || lower.contains("today")
                || lower.contains("fridge")
                || lower.contains("refrigerator")
                || lower.contains("freezer")
                || lower.contains("frozen")
                || lower.contains("room temp")
                || Pattern.compile("\\b\\d+(?:\\.\\d+)?\\s*(pcs|pieces|piece|g|kg|ml|l|oz|lb|cup|cups|tbsp|tsp|gal|gallon|gallons|bag|bags|loaf|loaves)\\b", Pattern.CASE_INSENSITIVE).matcher(input).find()
                || Pattern.compile("\\bin\\s+\\d+\\s+days?\\b", Pattern.CASE_INSENSITIVE).matcher(input).find();
    }

    private static String normalizeUnit(String rawUnit) {
        String unit = rawUnit == null ? "" : rawUnit.toLowerCase(Locale.US);
        if (unit.startsWith("piece")) return "pcs";
        if (unit.equals("cups")) return "cup";
        if (unit.equals("gallon") || unit.equals("gallons")) return "gal";
        if (unit.equals("bags")) return "bag";
        if (unit.equals("loaves")) return "loaf";
        return unit;
    }

    private static String normalizeCategory(String rawCategory) {
        if (rawCategory == null) return "General";
        String category = rawCategory.trim();
        if (category.equalsIgnoreCase("Dairy")) return "Dairy";
        if (category.equalsIgnoreCase("Meat")) return "Meat";
        if (category.equalsIgnoreCase("Pantry")) return "Pantry";
        if (category.equalsIgnoreCase("Produce")) return "Produce";
        if (category.equalsIgnoreCase("Vegetables")) return "Vegetables";
        return "General";
    }

    private static String normalizeStorage(String rawStorage) {
        if (rawStorage == null) return LocalDataContract.STORAGE_ROOM_TEMP_NAME;
        String storage = rawStorage.trim().toLowerCase(Locale.US);
        if (storage.contains("fridge") || storage.contains("refrigerator") || storage.contains("refrigerated")) return LocalDataContract.STORAGE_REFRIGERATOR_NAME;
        if (storage.contains("freez") || storage.contains("frozen")) return LocalDataContract.STORAGE_FREEZE_NAME;
        return LocalDataContract.STORAGE_ROOM_TEMP_NAME;
    }
}
