/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { Product, Recipe } from '../types';

export async function generateRecipes(expiringProducts: Product[]): Promise<Recipe[]> {
  if (expiringProducts.length === 0) return [];

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
    return Array.isArray(data) ? data : data.recipes ?? [];
  } catch (error) {
    console.error('Failed to generate recipes:', error);
    return [];
  }
}
