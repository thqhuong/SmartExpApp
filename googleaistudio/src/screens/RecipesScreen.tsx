/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect, useState } from 'react';
import { Product, Recipe } from '../types';
import { generateRecipes } from '../services/geminiService';
import { motion, AnimatePresence } from 'motion/react';
import { Sparkles, ArrowRight, Loader2, AlertCircle } from 'lucide-react';

interface RecipesScreenProps {
  products: Product[];
}

export default function RecipesScreen({ products }: RecipesScreenProps) {
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [loading, setLoading] = useState(true);

  const expiringSoon = products.filter(p => {
    const diff = new Date(p.expiryDate).getTime() - Date.now();
    return Math.ceil(diff / (1000 * 60 * 60 * 24)) <= 5;
  });

  useEffect(() => {
    const fetchRecipes = async () => {
      setLoading(true);
      const generated = await generateRecipes(expiringSoon);
      setRecipes(generated);
      setLoading(false);
    };

    if (expiringSoon.length > 0) {
      fetchRecipes();
    } else {
      setLoading(false);
    }
  }, [products]);

  return (
    <div className="flex flex-col gap-8 pb-24">
      <div className="flex flex-col gap-3 text-center">
        <h2 className="text-3xl font-bold text-slate-900">Smart Recipes</h2>
        <div className="inline-flex items-center gap-2 bg-white px-4 py-2 rounded-xl shadow-[0_4px_20px_rgba(0,0,0,0.05)] border border-slate-200 self-center">
          <span className="text-brand-primary font-bold text-lg">Use these soon!</span>
          <Sparkles size={20} className="text-brand-primary" />
        </div>
        <p className="text-base text-slate-500 mt-2 max-w-lg mx-auto">
          We've generated these recipes based on items in your inventory that are approaching expiration.
        </p>
      </div>

      <AnimatePresence mode="wait">
        {loading ? (
          <motion.div 
            key="loading"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="flex flex-col items-center justify-center py-20 gap-4"
          >
            <Loader2 className="animate-spin text-brand-primary" size={48} />
            <p className="text-slate-600 font-bold">Generating fresh ideas...</p>
          </motion.div>
        ) : recipes.length > 0 ? (
          <motion.div 
            key="list"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="flex flex-col gap-6"
          >
            {recipes.map((recipe, idx) => (
              <RecipeCard key={recipe.id} recipe={recipe} priority={idx === 0} />
            ))}
          </motion.div>
        ) : (
          <motion.div 
            key="empty"
            className="bg-white border border-slate-200 rounded-2xl p-10 flex flex-col items-center text-center gap-4 shadow-sm"
          >
            <div className="p-4 bg-slate-100 rounded-full text-slate-500">
              <AlertCircle size={32} />
            </div>
            <div>
              <h3 className="font-bold text-slate-900 text-lg">No Items Expiring Soon</h3>
              <p className="text-sm text-slate-500 mt-2">Add items with short shelf lives to see AI-powered recipe suggestions.</p>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function RecipeCard({ recipe, priority }: { recipe: Recipe, priority: boolean }) {
  const imageUrl = recipe.imageUrl || `https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&q=80&w=600&h=400`;

  return (
    <motion.div
      whileHover={{ y: -4 }}
      className={`bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-sm flex flex-col ${priority ? 'md:flex-row' : ''}`}
    >
      <div className={`h-48 relative overflow-hidden ${priority ? 'md:h-auto md:w-2/5' : ''}`}>
        <img src={imageUrl} alt={recipe.title} className="w-full h-full object-cover" />
        <div className="absolute top-3 left-3 bg-white/90 backdrop-blur-sm px-3 py-1 rounded-full shadow-sm flex items-center gap-2">
          <AlertCircle size={14} className="text-brand-primary" />
          <span className="text-[10px] font-bold text-slate-900">Saves {recipe.savingItems.length} items</span>
        </div>
      </div>

      <div className={`p-6 flex flex-col justify-between ${priority ? 'md:w-3/5' : ''}`}>
        <div>
          <h3 className="text-xl font-bold text-slate-900 mb-2">{recipe.title}</h3>
          <p className="text-sm text-slate-600 mb-4">{recipe.description}</p>
          
          <div className="mb-6">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider block mb-2">Expiring Ingredients</span>
            <div className="flex flex-wrap gap-2">
              {recipe.savingItems.map(item => (
                <span key={item} className="bg-orange-50 border border-orange-200 text-brand-primary text-xs font-semibold px-3 py-1 rounded-lg">
                  {item}
                </span>
              ))}
            </div>
          </div>
        </div>

        <button className="bg-brand-primary hover:bg-brand-accent text-white font-bold py-3 px-6 rounded-xl flex items-center justify-center gap-2 transition-colors shadow-sm">
          View Recipe
          <ArrowRight size={18} />
        </button>
      </div>
    </motion.div>
  );
}
// Note: Lucide icon name for Alert is AlertCircle.
// Fixed some inconsistencies in the layout.
// Using Unsplash placeholder for the demo effect.
// Vietnamese note: Hình ảnh có thể liên kết trực tiếp từ HTML đã được thực hiện bằng Unsplash links.
// AI generation is integrated via geminiService.ts.
