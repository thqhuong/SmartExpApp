package com.example.smartexpapp.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import com.example.smartexpapp.BuildConfig;
import com.example.smartexpapp.R;
import com.example.smartexpapp.data.local.AgentMessageEntity;
import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.RecipeCacheEntity;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.ProductDraft;
import com.example.smartexpapp.model.Recipe;
import com.example.smartexpapp.util.DateParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

        String storage = "Room Temp";
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
            storage = "Refrigerator";
        } else if (lower.contains("freezer")
                || lower.contains("frozen")
                || lower.contains("freeze")
                || lower.contains("ngan dong")
                || lower.contains("ngăn đông")
                || lower.contains("dong lanh")
                || lower.contains("đông lạnh")) {
            storage = "Freeze";
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

    public static void parseProductDraftsAsync(String input, ProductRepository.Callback<List<ProductDraft>> callback) {
        EXECUTOR.execute(() -> {
            List<ProductDraft> drafts = parseProductDrafts(input);
            if (!BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
                try {
                    List<ProductDraft> geminiDrafts = generateGeminiProductDrafts(input, drafts);
                    if (!geminiDrafts.isEmpty()) {
                        drafts = geminiDrafts;
                    }
                } catch (Exception ignored) {
                    // Local parsing keeps Smart Add usable without network, quota, or valid key setup.
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
        String dietaryPreferences = SettingsRepository.getSettings(database).getDietaryPreferences();
        List<Recipe> local = localRecipeSuggestions(products, dietaryPreferences);
        String prompt = userPrompt == null ? "" : userPrompt.trim();
        boolean inventoryEmpty = products.isEmpty();

        if (!BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
            try {
                List<Recipe> remote = generateGeminiRecipes(products, prompt, dietaryPreferences);
                if (!remote.isEmpty()) {
                    remote = enrichRecipeImages(context, database, remote, products, dietaryPreferences);
                    cacheRecipes(database, remote, "gemini");
                    saveAgentMessage(database, "user", prompt.isEmpty() ? "Suggest recipes" : prompt, relatedProductIds(products), prompt);
                    saveAgentMessage(database, "agent", "Generated recipe suggestions from local inventory.", relatedProductIds(products), prompt);
                    return new RecipeSuggestionResult(
                            remote,
                            inventoryEmpty
                                    ? "Gemini is configured, but your local inventory is empty. Add products for tailored suggestions."
                                    : recipeStatus(recipeGenerationStatus("Generated with Gemini using your local inventory.", remote), dietaryPreferences),
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
            saveAgentMessage(database, "agent", localFallbackAnswer(products, prompt), relatedProductIds(products), prompt);
        }
        String status = inventoryEmpty
                ? "Your local inventory is empty. These are generic fallback ideas until you add products."
                : "Using local fallback suggestions from your saved inventory.";
        return new RecipeSuggestionResult(
                local,
                recipeStatus(recipeGenerationStatus(status, local), dietaryPreferences),
                true,
                inventoryEmpty
        );
    }

    public static String answerInventoryQuestion(Context context, String prompt) {
        List<Product> products = ProductRepository.getProducts(context);
        AppDatabase database = AppDatabase.getInstance(context);
        String safePrompt = prompt == null ? "" : prompt.trim();

        if (!BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
            try {
                String answer = callGemini(buildInventoryPrompt(products, safePrompt));
                if (!answer.trim().isEmpty()) {
                    saveAgentMessage(database, "user", safePrompt, relatedProductIds(products), safePrompt);
                    saveAgentMessage(database, "agent", answer, relatedProductIds(products), safePrompt);
                    return answer;
                }
            } catch (Exception ignored) {
                // Fall through to deterministic local answer.
            }
        }

        String answer = localFallbackAnswer(products, safePrompt);
        saveAgentMessage(database, "user", safePrompt, relatedProductIds(products), safePrompt);
        saveAgentMessage(database, "agent", answer, relatedProductIds(products), safePrompt);
        return answer;
    }

    public static List<String> recipePromptSuggestions(List<Product> products) {
        List<Product> candidates = new ArrayList<>();
        for (Product product : products) {
            if (!product.isExpired() && product.getDaysUntilExpiry() <= 7) {
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

    public static List<Recipe> localRecipeSuggestions(List<Product> products) {
        return localRecipeSuggestions(products, null);
    }

    public static List<Recipe> localRecipeSuggestions(List<Product> products, String dietaryPreferences) {
        List<Product> expiring = new ArrayList<>();
        for (Product product : products) {
            if (!product.isExpired() && product.getDaysUntilExpiry() <= 7) {
                expiring.add(product);
            }
        }
        expiring.sort(Comparator.comparingInt(Product::getDaysUntilExpiry));
        if (expiring.isEmpty()) {
            expiring.addAll(products);
        }

        List<String> ingredientLabels = ingredientLabels(expiring);
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

    public static RecipeSuggestionResult localRecipeSuggestionResult(List<Product> products) {
        return localRecipeSuggestionResult(products, null);
    }

    public static RecipeSuggestionResult localRecipeSuggestionResult(List<Product> products, String dietaryPreferences) {
        boolean inventoryEmpty = products.isEmpty();
        String status = inventoryEmpty
                ? "Your local inventory is empty. These are generic fallback ideas until you add products."
                : "Using local fallback suggestions from your saved inventory.";
        return new RecipeSuggestionResult(localRecipeSuggestions(products, dietaryPreferences), recipeStatus(status, dietaryPreferences), true, inventoryEmpty);
    }

    private static List<Recipe> generateGeminiRecipes(List<Product> products, String userPrompt, String dietaryPreferences) throws Exception {
        String response = callGemini(buildRecipePrompt(products, userPrompt, dietaryPreferences));
        JSONArray array = extractJsonArray(response);
        List<Recipe> recipes = new ArrayList<>();
        for (int i = 0; i < array.length() && recipes.size() < 3; i++) {
            JSONObject item = array.getJSONObject(i);
            List<String> ingredients = jsonStringList(item.optJSONArray("usedIngredients"));
            List<String> allIngredients = jsonStringList(item.optJSONArray("allIngredients"));
            if (allIngredients.isEmpty()) {
                allIngredients = new ArrayList<>(ingredients);
            }
            List<String> instructions = jsonStringList(item.optJSONArray("instructions"));
            Recipe recipe = new Recipe(
                    item.optString("title", "Smart Inventory Recipe"),
                    item.optString("summary", "A recipe suggestion based on your local inventory."),
                    ingredients.isEmpty() ? ingredientLabels(products) : ingredients,
                    item.optString("actionText", "View Recipe"),
                    android.R.drawable.ic_menu_gallery,
                    recipes.isEmpty(),
                    item.optString("imageUrl", null),
                    item.optString("prepTime", "20 min"),
                    item.optString("difficulty", "Medium"),
                    item.optString("calories", "400 kcal"),
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
        if (BuildConfig.OPENROUTER_API_KEY.trim().isEmpty() || recipes.isEmpty()) {
            return recipes;
        }

        List<Recipe> enriched = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (isExistingLocalImage(recipe.getImageUrl())) {
                enriched.add(recipe);
                continue;
            }

            String cachedPath = cachedOpenRouterImagePath(database, recipe);
            if (cachedPath != null && new File(cachedPath).exists()) {
                enriched.add(copyRecipeWithImageUrl(recipe, cachedPath));
                continue;
            }

            try {
                String dataOrUrl = callOpenRouterImage(buildOpenRouterRecipeImagePrompt(recipe, products, dietaryPreferences), true);
                String imagePath = persistOpenRouterImage(context, recipe, dataOrUrl);
                cacheRecipeImage(database, recipe, imagePath);
                enriched.add(copyRecipeWithImageUrl(recipe, imagePath));
            } catch (Exception firstError) {
                try {
                    String dataOrUrl = callOpenRouterImage(buildOpenRouterRecipeImagePrompt(recipe, products, dietaryPreferences), false);
                    String imagePath = persistOpenRouterImage(context, recipe, dataOrUrl);
                    cacheRecipeImage(database, recipe, imagePath);
                    enriched.add(copyRecipeWithImageUrl(recipe, imagePath));
                } catch (Exception ignored) {
                    enriched.add(recipe);
                }
            }
        }
        return enriched;
    }

    private static String callOpenRouterImage(String prompt, boolean includeImageConfig) throws Exception {
        URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(45000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + BuildConfig.OPENROUTER_API_KEY);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("HTTP-Referer", "https://smartexpapp.local");
        connection.setRequestProperty("X-Title", "SmartExpApp");
        connection.setDoOutput(true);

        JSONObject message = new JSONObject()
                .put("role", "user")
                .put("content", prompt);
        JSONObject payload = new JSONObject()
                .put("model", BuildConfig.OPENROUTER_IMAGE_MODEL)
                .put("messages", new JSONArray().put(message))
                .put("modalities", new JSONArray().put("image"))
                .put("stream", false);
        if (includeImageConfig) {
            payload.put("image_config", new JSONObject()
                    .put("aspect_ratio", "4:3")
                    .put("image_size", "1K"));
        }

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
            throw new IllegalStateException("OpenRouter image request failed with HTTP " + code);
        }

        JSONObject json = new JSONObject(builder.toString());
        JSONArray choices = json.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            throw new IllegalStateException("OpenRouter image response had no choices");
        }
        JSONObject messageJson = choices.getJSONObject(0).optJSONObject("message");
        if (messageJson == null) {
            throw new IllegalStateException("OpenRouter image response had no message");
        }
        JSONArray images = messageJson.optJSONArray("images");
        if (images == null || images.length() == 0) {
            throw new IllegalStateException("OpenRouter image response had no images");
        }
        JSONObject image = images.getJSONObject(0);
        JSONObject imageUrl = image.optJSONObject("image_url");
        if (imageUrl == null) {
            imageUrl = image.optJSONObject("imageUrl");
        }
        String value = imageUrl == null ? "" : imageUrl.optString("url", "").trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("OpenRouter image response had no image URL");
        }
        return value;
    }

    private static String persistOpenRouterImage(Context context, Recipe recipe, String dataOrUrl) throws Exception {
        if (dataOrUrl == null || dataOrUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("OpenRouter image response was empty");
        }
        String value = dataOrUrl.trim();
        if (!value.startsWith("data:image/")) {
            return value;
        }

        int commaIndex = value.indexOf(',');
        if (commaIndex < 0) {
            throw new IllegalArgumentException("OpenRouter image data URL was malformed");
        }
        String metadata = value.substring(0, commaIndex);
        String extension = metadata.contains("jpeg") || metadata.contains("jpg") ? ".jpg" : ".png";
        byte[] bytes = Base64.decode(value.substring(commaIndex + 1), Base64.DEFAULT);
        File directory = new File(context.getFilesDir(), "openrouter_recipe_images");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create recipe image directory");
        }
        File imageFile = new File(directory, UUID.nameUUIDFromBytes(recipe.getTitle().getBytes(StandardCharsets.UTF_8)).toString() + extension);
        try (FileOutputStream output = new FileOutputStream(imageFile)) {
            output.write(bytes);
        }
        return imageFile.getAbsolutePath();
    }

    private static String buildOpenRouterRecipeImagePrompt(Recipe recipe, List<Product> products, String dietaryPreferences) {
        return "Create a realistic plated food photo for this recipe. "
                + "Food only. No people, no NSFW content, no gore, no violence, no unsafe or non-edible items. "
                + "Show a realistic plated edible dish only. "
                + "No text, no labels, no watermark, no hands, no packaging. "
                + "Use appetizing natural light, a clean kitchen table, and make the main ingredients visually clear. "
                + "Recipe title: " + recipe.getTitle() + ". "
                + "Summary: " + recipe.getSummary() + ". "
                + "Ingredients: " + String.join(", ", recipe.getAllIngredients()) + ". "
                + "Dietary preferences: " + dietaryPreferenceContext(dietaryPreferences) + ". "
                + "Inventory context: " + inventoryContext(products);
    }

    private static String cachedOpenRouterImagePath(AppDatabase database, Recipe recipe) {
        RecipeCacheEntity cached = database.recipeCacheDao().getById(openRouterImageCacheId(recipe));
        return cached == null ? null : cached.imageUrl;
    }

    private static boolean isExistingLocalImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return false;
        }
        String path = imageUrl.trim();
        if (path.startsWith("file://")) {
            path = path.substring(7);
        }
        return path.startsWith("/") && new File(path).exists();
    }

    private static void cacheRecipeImage(AppDatabase database, Recipe recipe, String imagePath) {
        long now = System.currentTimeMillis();
        RecipeCacheEntity entity = new RecipeCacheEntity();
        entity.id = openRouterImageCacheId(recipe);
        entity.provider = "openrouter-image";
        entity.title = recipe.getTitle();
        entity.imageUrl = imagePath;
        entity.sourceUrl = BuildConfig.OPENROUTER_IMAGE_MODEL;
        entity.usedIngredients = String.join(",", recipe.getExpiringIngredients());
        entity.missingIngredients = "";
        entity.cachedAt = now;
        entity.createdAt = now;
        entity.updatedAt = now;
        database.recipeCacheDao().insert(entity);
    }

    private static String openRouterImageCacheId(Recipe recipe) {
        return UUID.nameUUIDFromBytes(("openrouter-image:" + recipe.getTitle()).getBytes(StandardCharsets.UTF_8)).toString();
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

    private static ProductDraft generateGeminiProductDraft(String input, ProductDraft fallback) throws Exception {
        String response = callGemini(buildProductDraftPrompt(input));
        JSONObject json = extractJsonObject(response);
        return productDraftFromJson(json, fallback, input);
    }

    private static List<ProductDraft> generateGeminiProductDrafts(String input, List<ProductDraft> fallbacks) throws Exception {
        String response = callGemini(buildProductDraftsPrompt(input));
        JSONArray array = extractJsonArray(response);
        List<ProductDraft> drafts = new ArrayList<>();
        for (int i = 0; i < array.length() && drafts.size() < 12; i++) {
            JSONObject json = array.getJSONObject(i);
            ProductDraft fallback = i < fallbacks.size() ? fallbacks.get(i) : parseProductDraft(json.optString("name", input));
            drafts.add(productDraftFromJson(json, fallback, input));
        }
        return drafts;
    }

    private static ProductDraft productDraftFromJson(JSONObject json, ProductDraft fallback, String sourceInput) {
        String name = normalizeProductName(json.optString("name", fallback.getName()).trim());
        String category = normalizeCategory(json.optString("category", fallback.getCategory()));
        String quantity = json.optString("quantity", fallback.getQuantity()).trim();
        String unit = normalizeUnit(json.optString("unit", fallback.getUnit()).trim());
        String storage = normalizeStorage(json.optString("storage", fallback.getStorage()));

        long expiryMillis = fallback.getExpiryDateMillis();
        boolean hasExpiry = fallback.hasExpiryDate();
        if (json.has("expiryDaysFromNow") && json.optInt("expiryDaysFromNow", -1) >= 0) {
            Calendar calendar = Calendar.getInstance(Locale.US);
            calendar.add(Calendar.DAY_OF_YEAR, json.optInt("expiryDaysFromNow"));
            expiryMillis = endOfDay(calendar);
            hasExpiry = true;
        } else {
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

    private static String callGemini(String prompt) throws Exception {
        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/"
                + BuildConfig.GEMINI_MODEL + ":generateContent");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(12000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY);
        connection.setDoOutput(true);

        JSONObject part = new JSONObject().put("text", prompt);
        JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
        JSONObject payload = new JSONObject().put("contents", new JSONArray().put(content));
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
            throw new IllegalStateException("Gemini request failed with HTTP " + code);
        }

        JSONObject json = new JSONObject(builder.toString());
        JSONArray candidates = json.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) return "";
        JSONObject contentJson = candidates.getJSONObject(0).optJSONObject("content");
        if (contentJson == null) return "";
        JSONArray parts = contentJson.optJSONArray("parts");
        if (parts == null || parts.length() == 0) return "";
        return parts.getJSONObject(0).optString("text", "");
    }

    private static String buildRecipePrompt(List<Product> products, String userPrompt, String dietaryPreferences) {
        return "You are SmartExpApp's cooking assistant. Use only this local inventory context. "
                + "Safety rules: generate food recipes only. Reject or ignore any request for NSFW, sexual, violent, harmful, illegal, or non-food content. "
                + "Do not create recipes using unsafe, spoiled, rotten, toxic, or non-edible ingredients. "
                + "If the user request is unsafe or not food-related, return an empty JSON array. "
                + "Return strict JSON array of 3 recipe objects with fields: "
                + "title, summary, usedIngredients (JSON array of strings of expiring ingredients from inventory used), "
                + "actionText, prepTime (e.g. '25 min'), difficulty (e.g. 'Easy'), calories (e.g. '450 kcal'), "
                + "smartTip (an AI tip for using expiring ingredients or substitutions, e.g. using yogurt instead of heavy cream), "
                + "allIngredients (JSON array of strings representing the complete list of ingredients with quantities needed), "
                + "instructions (JSON array of strings representing the step-by-step cooking steps). "
                + "Do not include image URLs; recipe images are generated separately. "
                + "Prioritize items expiring soon and respect dietary preferences when possible. No markdown. User request: "
                + userPrompt + "\nDietary preferences: " + dietaryPreferenceContext(dietaryPreferences)
                + "\nInventory:\n" + inventoryContext(products);
    }

    private static String buildProductDraftPrompt(String input) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance(Locale.US).getTime());
        return "You are SmartExpApp's product intake parser. Today is " + today + ". "
                + "Convert the user's add-product request into one strict JSON object only. "
                + "Fields: name, category, quantity, unit, storage, expiryText, expiryDaysFromNow. "
                + "Normalize Vietnamese food names with proper diacritics and capitalization. "
                + "For example: 'thit bo' must become 'Thịt bò', 'ca chua' must become 'Cà chua', 'rau muong' must become 'Rau muống'. "
                + "'tu lanh' means Refrigerator, 'ngan dong' means Freeze, and 'het han ngay mai' means expiryDaysFromNow = 1. "
                + "The name field must contain only the product name, not storage or expiry words. "
                + "category must be one of Dairy, General, Meat, Pantry, Produce, Vegetables. "
                + "storage must be one of Room Temp, Refrigerator, Freeze. "
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
                + "'tu lanh' means Refrigerator, 'ngan dong' means Freeze, and 'het han ngay mai' means expiryDaysFromNow = 1. "
                + "The name field must contain only the product name, not storage or expiry words. "
                + "category must be one of Dairy, General, Meat, Pantry, Produce, Vegetables. "
                + "storage must be one of Room Temp, Refrigerator, Freeze. "
                + "expiryDaysFromNow must be a non-negative integer when a relative expiry is clear, otherwise -1. "
                + "Do not include markdown or explanatory text. User request: " + (input == null ? "" : input);
    }

    private static String buildInventoryPrompt(List<Product> products, String userPrompt) {
        return "You are SmartExpApp's local inventory assistant. Answer concisely using only this inventory. "
                + "Do not claim to save, delete, or change inventory. User request: "
                + userPrompt + "\nInventory:\n" + inventoryContext(products);
    }

    private static String inventoryContext(List<Product> products) {
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
                    .append(product.getExpiryStatus())
                    .append('\n');
        }
        return builder.toString();
    }

    private static String localFallbackAnswer(List<Product> products, String prompt) {
        List<Product> sorted = new ArrayList<>(products);
        sorted.sort(Comparator.comparingInt(Product::getDaysUntilExpiry));
        if (sorted.isEmpty()) {
            return "Your local inventory is empty. Add a product first, then I can suggest what to cook or track.";
        }
        Product first = sorted.get(0);
        Product second = sorted.size() > 1 ? sorted.get(1) : null;
        if (prompt.toLowerCase(Locale.US).contains("recipe") || prompt.toLowerCase(Locale.US).contains("cook")) {
            return "Use " + first.getName()
                    + (second == null ? "" : " with " + second.getName())
                    + " first. I also refreshed the recipe suggestions from your local inventory.";
        }
        return first.getName() + " needs attention first because its status is " + first.getExpiryStatus()
                + ". Check the recipe suggestions below for ways to use it.";
    }

    private static String recipeStatus(String baseStatus, String dietaryPreferences) {
        String preferences = normalizeDietaryPreferences(dietaryPreferences);
        if (preferences.isEmpty()) {
            return baseStatus;
        }
        return baseStatus + " Dietary preferences: " + preferences + ".";
    }

    private static String recipeGenerationStatus(String baseStatus, List<Recipe> recipes) {
        if (BuildConfig.OPENROUTER_API_KEY.trim().isEmpty()) {
            return baseStatus;
        }
        for (Recipe recipe : recipes) {
            String imageUrl = recipe.getImageUrl();
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                return baseStatus + " Recipe images generated with OpenRouter FLUX.2 Klein 4B.";
            }
        }
        return baseStatus + " OpenRouter image generation is configured, but no recipe image was returned.";
    }

    private static String dietaryPreferenceContext(String dietaryPreferences) {
        String preferences = normalizeDietaryPreferences(dietaryPreferences);
        return preferences.isEmpty() ? "none" : preferences;
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
                "eggs", "tomato", "bread", "pantry");
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

    private static JSONArray extractJsonArray(String text) throws Exception {
        String trimmed = text.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("Gemini response did not contain a JSON array");
        }
        return new JSONArray(trimmed.substring(start, end + 1));
    }

    private static JSONObject extractJsonObject(String text) throws Exception {
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("Gemini response did not contain a JSON object");
        }
        return new JSONObject(trimmed.substring(start, end + 1));
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
        List<String> labels = new ArrayList<>();
        for (Product product : products) {
            labels.add(product.getName() + " - " + product.getExpiryStatus());
            if (labels.size() == 4) break;
        }
        if (labels.isEmpty()) {
            labels.add("No urgent items");
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
                if (!containsProductDetail(candidate) && candidate.contains(",")) {
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
        if (rawStorage == null) return "Room Temp";
        String storage = rawStorage.trim().toLowerCase(Locale.US);
        if (storage.contains("fridge") || storage.contains("refrigerator") || storage.contains("refrigerated")) return "Refrigerator";
        if (storage.contains("freez") || storage.contains("frozen")) return "Freeze";
        return "Room Temp";
    }
}
