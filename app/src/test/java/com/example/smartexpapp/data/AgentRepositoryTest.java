package com.example.smartexpapp.data;

import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.ProductDraft;
import com.example.smartexpapp.model.Recipe;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    public void localRecipesPrioritizeExpiringInventory() {
        List<Product> products = Arrays.asList(
                product("pasta", "Pantry", 20),
                product("Spinach", "Vegetables", 1),
                product("Milk", "Dairy", 3)
        );

        List<Recipe> recipes = AgentRepository.localRecipeSuggestions(products);

        assertFalse(recipes.isEmpty());
        assertTrue(recipes.get(0).getTitle().contains("Spinach"));
        assertTrue(recipes.get(0).getExpiringIngredients().get(0).contains("Spinach"));
    }

    @Test
    public void localRecipesHandleEmptyInventory() {
        List<Recipe> recipes = AgentRepository.localRecipeSuggestions(Collections.emptyList());

        assertEquals(3, recipes.size());
        assertTrue(recipes.get(0).getTitle().contains("pantry items"));
        assertEquals("No urgent items", recipes.get(0).getExpiringIngredients().get(0));
    }

    @Test
    public void localRecipeResultMarksEmptyInventoryFallback() {
        AgentRepository.RecipeSuggestionResult result =
                AgentRepository.localRecipeSuggestionResult(Collections.emptyList());

        assertTrue(result.isLocalFallback());
        assertTrue(result.isInventoryEmpty());
        assertTrue(result.getStatusMessage().contains("local inventory is empty"));
        assertEquals(3, result.getRecipes().size());
    }

    @Test
    public void localRecipeResultMarksInventoryBackedFallback() {
        AgentRepository.RecipeSuggestionResult result =
                AgentRepository.localRecipeSuggestionResult(Collections.singletonList(product("Milk", "Dairy", 2)));

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
                        "vegetarian, dairy-free"
                );

        assertTrue(result.getStatusMessage().contains("Dietary preferences: vegetarian, dairy-free"));
        assertTrue(result.getRecipes().get(0).getSummary().contains("Adapt for: vegetarian, dairy-free"));
    }

    @Test
    public void localRecipesNormalizeDietaryPreferenceWhitespace() {
        List<Recipe> recipes = AgentRepository.localRecipeSuggestions(
                Collections.singletonList(product("Spinach", "Vegetables", 1)),
                "  low   sodium  "
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

        List<String> prompts = AgentRepository.recipePromptSuggestions(products);

        assertFalse(prompts.isEmpty());
        assertEquals("Suggest recipes using Spinach", prompts.get(0));
        assertEquals("Use Spinach with Milk", prompts.get(1));
        assertTrue(prompts.contains("Quick meal for expiring items"));
    }

    @Test
    public void recipePromptSuggestionsHideWhenInventoryHasNoCookableItems() {
        List<String> prompts = AgentRepository.recipePromptSuggestions(
                Collections.singletonList(product("Expired Bread", "Pantry", -1))
        );

        assertTrue(prompts.isEmpty());
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
