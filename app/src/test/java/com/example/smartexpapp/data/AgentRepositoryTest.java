package com.example.smartexpapp.data;

import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.ProductDraft;
import com.example.smartexpapp.model.Recipe;

import org.junit.Test;
import org.json.JSONObject;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AgentRepositoryTest {
    @Test
    public void parseProductDraftExtractsFieldsFromSmartAddText() {
        ProductDraft draft = AgentRepository.parseProductDraft(
                "Add organic milk 1 gallon expires tomorrow in the fridge");

        assertEquals("Organic Milk", draft.getName());
        assertEquals("1", draft.getQuantity());
        assertEquals("gal", draft.getUnit());
        assertEquals("Dairy", draft.getCategory());
        assertEquals("Refrigerator", draft.getStorage());
        assertTrue(draft.hasExpiryDate());
    }

    @Test
    public void parseProductDraftNormalizesVietnameseSmartAddText() {
        ProductDraft draft = AgentRepository.parseProductDraft(
                "thit bo het han ngay mai cat trong tu lanh");

        assertEquals("Thịt bò", draft.getName());
        assertEquals("Meat", draft.getCategory());
        assertEquals("Refrigerator", draft.getStorage());
        assertTrue(draft.hasExpiryDate());
    }

    @Test
    public void parseProductDraftHandlesOcrLikePackageText() {
        ProductDraft draft = AgentRepository.parseProductDraft(
                "Organic Milk\n1 gallon\nBest Before 12/31/2026\nKeep Refrigerated");

        assertEquals("Organic Milk", draft.getName());
        assertEquals("1", draft.getQuantity());
        assertEquals("gal", draft.getUnit());
        assertEquals("Dairy", draft.getCategory());
        assertEquals("Refrigerator", draft.getStorage());
        assertTrue(draft.hasExpiryDate());
    }

    @Test
    public void parseProductDraftsSplitsMultipleSmartAddItems() {
        List<ProductDraft> drafts = AgentRepository.parseProductDrafts(
                "Add milk expires tomorrow; eggs expire in 3 days; spinach in fridge expires today");

        assertEquals(3, drafts.size());
        assertEquals("Milk", drafts.get(0).getName());
        assertEquals("Eggs", drafts.get(1).getName());
        assertEquals("Spinach", drafts.get(2).getName());
        assertEquals("Refrigerator", drafts.get(2).getStorage());
    }

    @Test
    public void parseProductDraftsKeepsSingleItemAttributesTogether() {
        List<ProductDraft> drafts = AgentRepository.parseProductDrafts(
                "Add organic milk, 1 gallon, expires tomorrow, fridge");

        assertEquals(1, drafts.size());
        assertEquals("Organic Milk", drafts.get(0).getName());
        assertEquals("1", drafts.get(0).getQuantity());
        assertEquals("gal", drafts.get(0).getUnit());
        assertEquals("Refrigerator", drafts.get(0).getStorage());
    }

    @Test
    public void parseProductDraftsSplitsCommaSeparatedProductsWithoutSplittingAttributes() {
        List<ProductDraft> drafts = AgentRepository.parseProductDrafts(
                "Add eggs 12 pcs fridge, bread 1 loaf expires in 5 days, frozen chicken 2 lb");

        assertEquals(3, drafts.size());
        assertEquals("Eggs", drafts.get(0).getName());
        assertEquals("12", drafts.get(0).getQuantity());
        assertEquals("Refrigerator", drafts.get(0).getStorage());
        assertFalse(drafts.get(0).hasExpiryDate());
        assertEquals("Bread", drafts.get(1).getName());
        assertEquals("loaf", drafts.get(1).getUnit());
        assertTrue(drafts.get(1).hasExpiryDate());
        assertEquals("Chicken", drafts.get(2).getName());
        assertEquals("Freezer", drafts.get(2).getStorage());
        assertFalse(drafts.get(2).hasExpiryDate());
    }

    @Test
    public void localRecipesPrioritizeExpiringInventory() {
        List<Product> products = Arrays.asList(
                product("pasta", "Pantry", 20),
                product("Spinach", "Vegetables", 1),
                product("Milk", "Dairy", 3)
        );

        List<Recipe> recipes = AgentRepository.localRecipeSuggestions(products, 7);

        assertFalse(recipes.isEmpty());
        assertTrue(recipes.get(0).getTitle().contains("Spinach"));
        assertTrue(recipes.get(0).getExpiringIngredients().get(0).contains("Spinach"));
    }

    @Test
    public void localRecipesHandleEmptyInventory() {
        List<Recipe> recipes = AgentRepository.localRecipeSuggestions(Collections.emptyList(), 7);

        assertEquals(3, recipes.size());
        assertTrue(recipes.get(0).getTitle().contains("pantry items"));
        assertEquals("No urgent items", recipes.get(0).getExpiringIngredients().get(0));
    }

    @Test
    public void localRecipeResultMarksEmptyInventoryFallback() {
        AgentRepository.RecipeSuggestionResult result =
                AgentRepository.localRecipeSuggestionResult(Collections.emptyList(), 7);

        assertTrue(result.isLocalFallback());
        assertTrue(result.isInventoryEmpty());
        assertTrue(result.getStatusMessage().contains("local inventory is empty"));
        assertEquals(3, result.getRecipes().size());
    }

    @Test
    public void localRecipeResultMarksInventoryBackedFallback() {
        AgentRepository.RecipeSuggestionResult result =
                AgentRepository.localRecipeSuggestionResult(Collections.singletonList(product("Milk", "Dairy", 2)), 7);

        assertTrue(result.isLocalFallback());
        assertFalse(result.isInventoryEmpty());
        assertTrue(result.getStatusMessage().contains("saved inventory"));
        assertEquals(3, result.getRecipes().size());
    }

    @Test
    public void localRecipeResultIncludesDietaryPreferencesWhenProvided() {
        AgentRepository.RecipeSuggestionResult result =
                AgentRepository.localRecipeSuggestionResult(
                        Collections.singletonList(product("Tofu", "General", 2)),
                        "vegetarian, dairy-free",
                        7
                );

        assertTrue(result.getStatusMessage().contains("Dietary preferences: vegetarian, dairy-free"));
        assertTrue(result.getRecipes().get(0).getSummary().contains("Adapt for: vegetarian, dairy-free"));
    }

    @Test
    public void localRecipesNormalizeDietaryPreferenceWhitespace() {
        List<Recipe> recipes = AgentRepository.localRecipeSuggestions(
                Collections.singletonList(product("Spinach", "Vegetables", 1)),
                "  low   sodium  ",
                7
        );

        assertTrue(recipes.get(0).getSummary().contains("Adapt for: low sodium"));
    }

    @Test
    public void recipeSafetyRejectsNsfwOrNonFoodContent() {
        Recipe unsafe = new Recipe(
                "NSFW non-food idea",
                "This is not a food recipe.",
                Collections.singletonList("unsafe ingredient"),
                "View",
                android.R.drawable.ic_menu_gallery,
                false,
                null,
                "10 min",
                "Easy",
                "0 kcal",
                "NSFW content",
                Collections.singletonList("non-edible item"),
                Collections.singletonList("Do not cook this.")
        );

        assertFalse(AgentRepository.isSafeFoodRecipe(unsafe));
    }

    @Test
    public void recipeSafetyAllowsNormalFoodRecipe() {
        Recipe safe = new Recipe(
                "Beef and Tomato Soup",
                "A simple cooked meal using expiring ingredients.",
                Arrays.asList("Beef", "Tomato"),
                "View Recipe",
                android.R.drawable.ic_menu_gallery,
                true,
                null,
                "25 min",
                "Easy",
                "350 kcal",
                "Cook the beef first, then simmer with tomato.",
                Arrays.asList("Beef", "Tomato", "Salt"),
                Arrays.asList("Cook beef.", "Simmer with tomato.", "Serve hot.")
        );

        assertTrue(AgentRepository.isSafeFoodRecipe(safe));
    }

    @Test
    public void recipePromptSuggestionsUseExpiringInventoryFirst() {
        List<Product> products = Arrays.asList(
                product("Pasta", "Pantry", 20),
                product("Spinach", "Vegetables", 1),
                product("Milk", "Dairy", 3)
        );

        List<String> prompts = AgentRepository.recipePromptSuggestions(products, 7);

        assertFalse(prompts.isEmpty());
        assertEquals("Suggest recipes using Spinach", prompts.get(0));
        assertEquals("Use Spinach with Milk", prompts.get(1));
        assertTrue(prompts.contains("Quick meal for expiring items"));
    }

    @Test
    public void recipePromptSuggestionsHideWhenInventoryHasNoCookableItems() {
        List<String> prompts = AgentRepository.recipePromptSuggestions(
                Collections.singletonList(product("Expired Bread", "Pantry", -1)),
                7
        );

        assertTrue(prompts.isEmpty());
    }

    @Test
    public void recipeImageWorkerUrlEncodesRecipeContext() {
        Recipe recipe = new Recipe(
                "Tomato Basil Soup",
                "A simple soup.",
                Arrays.asList("Tomato", "Basil"),
                "View Recipe",
                android.R.drawable.ic_menu_gallery,
                true,
                null,
                "20 min",
                "Easy",
                "250 kcal",
                null,
                Arrays.asList("2 cups Tomato", "Fresh Basil", "Olive Oil"),
                Collections.singletonList("Simmer and serve.")
        );

        String url = AgentRepository.buildRecipeImageWorkerUrl(
                "https://smart-exp-recipe-images.example.workers.dev/",
                recipe,
                Collections.emptyList(),
                "vegetarian"
        );

        assertTrue(url.startsWith("https://smart-exp-recipe-images.example.workers.dev/recipe-image?"));
        assertTrue(url.contains("title=Tomato+Basil+Soup"));
        assertTrue(url.contains("ingredients=2+cups+Tomato%2C+Fresh+Basil%2C+Olive+Oil"));
        assertTrue(url.contains("diet=vegetarian"));
    }

    @Test
    public void recipeImageWorkerUrlIsBlankWhenBaseUrlMissing() {
        Recipe recipe = new Recipe(
                "Quick Rice Bowl",
                "A simple bowl.",
                Collections.singletonList("Rice"),
                "View Recipe",
                android.R.drawable.ic_menu_gallery,
                false
        );

        assertNull(AgentRepository.buildRecipeImageWorkerUrl("", recipe, Collections.emptyList(), null));
    }

    @Test
    public void workerEndpointAppendsRequestedPath() {
        assertEquals(
                "https://smart-exp-recipe-images.example.workers.dev/generate-recipes",
                AgentRepository.workerEndpoint("https://smart-exp-recipe-images.example.workers.dev/", "/generate-recipes")
        );
        assertEquals(
                "https://smart-exp-recipe-images.example.workers.dev/answer-inventory",
                AgentRepository.workerEndpoint("https://smart-exp-recipe-images.example.workers.dev/answer-inventory", "/answer-inventory")
        );
    }

    @Test
    public void recipeWorkerResponseMapsToRecipes() throws Exception {
        JSONObject response = new JSONObject("{\"recipes\":["
                + "{\"title\":\"Beef Tomato Bowl\",\"summary\":\"A quick cooked meal.\",\"usedIngredients\":[\"Beef\",\"Tomato\"],"
                + "\"actionText\":\"View Recipe\",\"prepTime\":\"20 min\",\"difficulty\":\"Easy\",\"calories\":\"420 kcal\","
                + "\"smartTip\":\"Slice beef thinly.\",\"allIngredients\":[\"200g Beef\",\"1 Tomato\"],"
                + "\"instructions\":[\"Cook beef.\",\"Add tomato.\"]}"
                + "]}");

        List<Recipe> recipes = AgentRepository.recipesFromWorkerResponse(
                response,
                Arrays.asList(product("Beef", "Meat", 1), product("Tomato", "Vegetables", 2))
        );

        assertEquals(1, recipes.size());
        assertEquals("Beef Tomato Bowl", recipes.get(0).getTitle());
        assertEquals("20 min", recipes.get(0).getPrepTime());
        assertEquals("Easy", recipes.get(0).getDifficulty());
        assertEquals("420 kcal", recipes.get(0).getCalories());
        assertEquals("Slice beef thinly.", recipes.get(0).getSmartTip());
        assertEquals(2, recipes.get(0).getAllIngredients().size());
        assertEquals(2, recipes.get(0).getInstructions().size());
    }

    @Test
    public void recipePromptUsesVietnameseWhenAppLanguageIsVietnamese() {
        String prompt = AgentRepository.buildRecipePrompt(
                Collections.singletonList(product("Thịt bò", "Meat", 2)),
                "gợi ý món tối",
                "ít muối",
                "vi"
        );

        assertTrue(prompt.contains("Write every user-facing recipe field in Vietnamese"));
        assertTrue(prompt.contains("App language: Vietnamese (vi)"));
        assertTrue(prompt.contains("Dietary preferences: ít muối"));
    }

    @Test
    public void typedRecipePromptIsNotLimitedToInventoryOnly() {
        String prompt = AgentRepository.buildRecipePrompt(
                Collections.singletonList(product("Milk", "Dairy", 2)),
                "I want chicken curry",
                "",
                "en"
        );

        assertTrue(prompt.contains("Satisfy that request first"));
        assertTrue(prompt.contains("do not force every recipe to use only inventory"));
        assertTrue(prompt.contains("use an empty array when no inventory item fits"));
        assertFalse(prompt.contains("Use only this local inventory context"));
    }

    @Test
    public void productParserEndpointAppendsParseProductPath() {
        assertEquals(
                "https://smart-exp-recipe-images.example.workers.dev/parse-product",
                AgentRepository.productParserEndpoint("https://smart-exp-recipe-images.example.workers.dev/")
        );
        assertEquals(
                "https://smart-exp-recipe-images.example.workers.dev/parse-product",
                AgentRepository.productParserEndpoint("https://smart-exp-recipe-images.example.workers.dev/parse-product")
        );
    }

    @Test
    public void parserWorkerResponseMapsToProductDrafts() throws Exception {
        JSONObject response = new JSONObject("{\"items\":["
                + "{\"name\":\"Milk\",\"category\":\"Dairy\",\"quantity\":\"1\",\"unit\":\"gal\",\"storage\":\"Refrigerator\",\"expiryText\":\"tomorrow\",\"expiryDaysFromNow\":1},"
                + "{\"name\":\"Bread\",\"category\":\"Pantry\",\"quantity\":\"1\",\"unit\":\"loaf\",\"storage\":\"Room Temp\",\"expiryText\":\"\",\"expiryDaysFromNow\":-1}"
                + "]}");

        List<ProductDraft> drafts = AgentRepository.productDraftsFromParserWorkerResponse(
                response,
                AgentRepository.parseProductDrafts("milk expires tomorrow; bread"),
                "milk expires tomorrow; bread"
        );

        assertEquals(2, drafts.size());
        assertEquals("Milk", drafts.get(0).getName());
        assertEquals("Dairy", drafts.get(0).getCategory());
        assertEquals("gal", drafts.get(0).getUnit());
        assertEquals("Refrigerator", drafts.get(0).getStorage());
        assertTrue(drafts.get(0).hasExpiryDate());
        assertEquals("Bread", drafts.get(1).getName());
        assertEquals("loaf", drafts.get(1).getUnit());
        assertFalse(drafts.get(1).hasExpiryDate());
    }

    @Test
    public void parserWorkerResponseMatchesFallbacksByNameAndKeepsLocalExpiryScope() throws Exception {
        JSONObject response = new JSONObject("{\"items\":["
                + "{\"name\":\"eggs\",\"category\":\"Dairy\",\"quantity\":\"12\",\"unit\":\"pcs\",\"storage\":\"Refrigerator\",\"expiryText\":\"\",\"expiryDaysFromNow\":5},"
                + "{\"name\":\"chicken\",\"category\":\"Meat\",\"quantity\":\"2\",\"unit\":\"lb\",\"storage\":\"Freezer\",\"expiryText\":\"\",\"expiryDaysFromNow\":5},"
                + "{\"name\":\"bread\",\"category\":\"Pantry\",\"quantity\":\"1\",\"unit\":\"loaf\",\"storage\":\"Room Temp\",\"expiryText\":\"\",\"expiryDaysFromNow\":-1}"
                + "]}");
        String input = "Add eggs 12 pcs fridge, bread 1 loaf expires in 5 days, frozen chicken 2 lb";

        List<ProductDraft> drafts = AgentRepository.productDraftsFromParserWorkerResponse(
                response,
                AgentRepository.parseProductDrafts(input),
                input
        );

        assertEquals(3, drafts.size());
        assertEquals("eggs", drafts.get(0).getName());
        assertFalse(drafts.get(0).hasExpiryDate());
        assertEquals("chicken", drafts.get(1).getName());
        assertFalse(drafts.get(1).hasExpiryDate());
        assertEquals("bread", drafts.get(2).getName());
        assertTrue(drafts.get(2).hasExpiryDate());
    }

    private Product product(String name, String category, int daysUntilExpiry) {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.add(Calendar.DAY_OF_YEAR, daysUntilExpiry);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Product(name, category, "1", "pcs", "Room Temp", calendar.getTimeInMillis(), 0);
    }
}
