/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { GoogleGenAI, Type } from '@google/genai';
import { Product, Recipe } from './types';

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY || '' });

export async function generateRecipes(expiringProducts: Product[]): Promise<Recipe[]> {
  if (expiringProducts.length === 0) return [];
  
  try {
    const itemsList = expiringProducts.map(p => `${p.name} (${p.category})`).join(', ');
    
    const prompt = `
      I have the following food items that are expiring soon: ${itemsList}.
      
      Suggest 3 delicious, minimalist recipes that specifically use at least ONE of these expiring items.
      Focus on reducing food waste. 
      
      Only return the JSON. No preamble or markdown.
    `;

    const response = await ai.models.generateContent({
      model: 'gemini-3-flash-preview',
      contents: prompt,
      config: {
        responseMimeType: 'application/json',
        responseSchema: {
          type: Type.ARRAY,
          items: {
            type: Type.OBJECT,
            properties: {
              id: { type: Type.STRING },
              title: { type: Type.STRING },
              description: { type: Type.STRING },
              savingItems: { 
                type: Type.ARRAY,
                items: { type: Type.STRING }
              },
              instructions: { type: Type.STRING }
            },
            required: ['id', 'title', 'description', 'savingItems', 'instructions']
          }
        }
      }
    });

    const text = response.text || '[]';
    return JSON.parse(text);
  } catch (error) {
    console.error('Failed to generate recipes:', error);
    return [];
  }
}
