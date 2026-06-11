/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { Product, Recipe } from '../types';

const MOCK_RECIPES: Recipe[] = [
  {
    id: 'rec_shakshuka',
    title: 'Rustic Bell Pepper & Tomato Shakshuka',
    description: 'A hearty one-pan meal for using remaining fresh produce and eggs before they turn.',
    savingItems: ['Bell Peppers', 'Roma Tomatoes', 'Eggs'],
    instructions: '1. Sauté diced bell peppers and onions in olive oil until soft.\n2. Add crushed tomatoes, garlic, cumin, paprika, salt, and pepper. Simmer for 10 minutes until thick.\n3. Make small wells in the sauce and crack eggs directly into them.\n4. Cover and cook on low heat for 5-8 minutes until egg whites are set but yolks remain runny.\n5. Garnish with fresh parsley and serve hot with crusty bread.',
    imageUrl: 'https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&q=80&w=600&h=400',
  },
  {
    id: 'rec_smoothie',
    title: 'Super-Green Power Smoothie',
    description: 'A quick, nutrient-dense blend to save leafy greens and ripe bananas.',
    savingItems: ['Spinach', 'Banana', 'Milk'],
    instructions: '1. Wash spinach thoroughly.\n2. Add spinach, peeled banana, milk, and a handful of ice cubes to a high-speed blender.\n3. Blend on high for 60-90 seconds until completely smooth and creamy.\n4. Pour into a glass and enjoy immediately for maximum nutritional benefits.',
    imageUrl: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&q=80&w=600&h=400',
  },
  {
    id: 'rec_salad',
    title: 'Crispy Radish & Romaine Salad',
    description: 'A simple, refreshing side dish built around crisp garden produce.',
    savingItems: ['Romaine Lettuce', 'Radishes'],
    instructions: '1. Wash and chop romaine lettuce into bite-size pieces.\n2. Thinly slice radishes.\n3. Toss lettuce and radishes in a large bowl with olive oil, lemon juice, salt, and pepper.\n4. Top with grated parmesan cheese or croutons if desired.',
    imageUrl: 'https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&q=80&w=600&h=400',
  },
  {
    id: 'rec_cheese',
    title: 'Ultimate Grilled Cheddar & Tomato',
    description: 'Classic comfort food that uses up cheddar blocks and bread while they are fresh.',
    savingItems: ['Cheddar Cheese', 'Sourdough Loaf', 'Tomatoes'],
    instructions: '1. Butter two slices of sourdough bread.\n2. Place one slice butter-side down on a hot skillet.\n3. Layer sliced cheddar cheese, tomato slices, and more cheese.\n4. Top with the second slice of bread, butter-side up.\n5. Grill for 3-4 minutes on each side until bread is golden brown and cheese is completely melted.',
    imageUrl: 'https://images.unsplash.com/photo-1528735602780-2552fd46c7af?auto=format&fit=crop&q=80&w=600&h=400',
  }
];

export async function generateRecipes(expiringProducts: Product[]): Promise<Recipe[]> {
  if (expiringProducts.length === 0) return MOCK_RECIPES;

  try {
    const response = await fetch('/api/generate-recipes', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ products: expiringProducts }),
    });

    if (!response.ok) {
      throw new Error(`Recipe generation failed with status ${response.status}`);
    }

    const data: { recipes?: Recipe[] } | Recipe[] = await response.json();
    const result = Array.isArray(data) ? data : data.recipes ?? [];
    if (result.length > 0) return result;
  } catch (error) {
    console.warn('API recipe generation failed, using high-quality local fallback recipes:', error);
  }

  // Smart Matching Fallback
  const lowerProductNames = expiringProducts.map(p => p.name.toLowerCase());
  const matched = MOCK_RECIPES.filter(recipe => {
    return recipe.savingItems.some(ing => 
      lowerProductNames.some(pName => pName.includes(ing.toLowerCase().split(' ')[0]))
    );
  });

  return matched.length > 0 ? matched : MOCK_RECIPES;
}
