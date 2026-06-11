/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect, useState, useRef } from 'react';
import { Product, Recipe } from '../types';
import { generateRecipes } from '../services/geminiService';

interface RecipesScreenProps {
  products: Product[];
}

interface ChatMessage {
  id: string;
  sender: 'user' | 'agent';
  text: string;
  recipes?: Recipe[];
}

export default function RecipesScreen({ products }: RecipesScreenProps) {
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [generating, setGenerating] = useState(false);
  
  const bottomRef = useRef<HTMLDivElement | null>(null);

  const expiringSoon = products.filter(p => {
    const diff = new Date(p.expiryDate).getTime() - Date.now();
    const days = Math.ceil(diff / (1000 * 60 * 60 * 24));
    return days > 0 && days <= 5;
  });

  useEffect(() => {
    const fetchInitialRecipes = async () => {
      const generated = await generateRecipes(expiringSoon);
      setRecipes(generated);
    };
    fetchInitialRecipes();
  }, [products]);

  useEffect(() => {
    // Scroll chat to bottom when message arrives
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages, generating]);

  const handleSendPrompt = async (text: string) => {
    if (!text.trim() || generating) return;

    // Add user message
    const userMsg: ChatMessage = {
      id: Math.random().toString(36).substring(2, 9),
      sender: 'user',
      text: text.trim(),
    };
    setChatMessages(prev => [...prev, userMsg]);
    setInputValue('');
    setGenerating(true);

    // AI thinking delay
    setTimeout(async () => {
      const results = await generateRecipes(expiringSoon);
      
      let responseText = "I've analyzed your inventory. Here are some smart recipes to use up your expiring items:";
      let matchedRecipes = results;

      if (text.toLowerCase().includes('vegetarian')) {
        responseText = "Here are some delicious vegetarian recipes using ingredients from your fridge:";
        matchedRecipes = results.filter(r => 
          !r.title.toLowerCase().includes('chicken') && 
          !r.title.toLowerCase().includes('meat') && 
          !r.title.toLowerCase().includes('pork')
        );
      } else if (text.toLowerCase().includes('15 minutes') || text.toLowerCase().includes('fast')) {
        responseText = "Here are some quick meals you can whip up in under 15 minutes to save your items:";
        matchedRecipes = results.filter(r => r.title.toLowerCase().includes('smoothie') || r.title.toLowerCase().includes('cheese'));
      }

      const agentMsg: ChatMessage = {
        id: Math.random().toString(36).substring(2, 9),
        sender: 'agent',
        text: responseText,
        recipes: matchedRecipes,
      };

      setChatMessages(prev => [...prev, agentMsg]);
      setGenerating(false);
    }, 1500);
  };

  const handleSurpriseMe = () => {
    handleSendPrompt("Surprise me with a random recipe using whatever I have left!");
  };

  const suggestedQuestions = [
    { text: 'Make a meal out of expiring items', icon: 'psychology', color: 'text-blue-500 bg-blue-500/10' },
    { text: 'Make a vegetarian meal', icon: 'eco', color: 'text-emerald-500 bg-emerald-500/10' },
    { text: 'What can I make in under 15 minutes?', icon: 'timer', color: 'text-rose-500 bg-rose-500/10' },
  ];

  return (
    <div className="flex flex-col gap-6 pb-24">
      {/* Gemini Live Agent Card */}
      <section className="flex flex-col items-center justify-center text-center relative py-2">
        <div className="glass-panel p-6 rounded-3xl w-full max-w-2xl flex flex-col items-center border border-white/40 dark:border-white/10 shadow-lg">
          <div className="w-24 h-24 rounded-full glass-panel border-2 flex items-center justify-center mb-4 relative overflow-hidden group border-white/50 dark:border-zinc-700 shadow-md">
            <div className="absolute inset-0 bg-gradient-to-tr from-[#4285F4]/10 via-[#9B72CB]/10 to-[#D96570]/10 group-hover:opacity-100 opacity-80 transition-opacity"></div>
            <span className="material-symbols-outlined text-4xl z-10 gemini-gradient-text" style={{ fontVariationSettings: "'FILL' 1" }}>
              auto_awesome
            </span>
            <div className="absolute w-full h-full rounded-full animate-pulse bg-white/10 dark:bg-white/5"></div>
          </div>
          <h2 className="text-2xl font-bold mb-1.5 gemini-gradient-text">Gemini Live</h2>
          <p className="text-xs text-slate-500 dark:text-zinc-400 max-w-sm">
            Your AI culinary assistant. Ask me questions, suggest custom meals, or type prompts below.
          </p>
        </div>
      </section>

      {/* Suggested Questions Grid */}
      <section>
        <h3 className="text-xs font-bold text-slate-500 dark:text-zinc-400 mb-3 uppercase tracking-widest pl-1">
          Suggested Questions
        </h3>
        <div className="flex flex-col gap-2.5">
          {suggestedQuestions.map((q, idx) => (
            <button
              key={idx}
              onClick={() => handleSendPrompt(q.text)}
              className="glass-panel hover:bg-white/80 dark:hover:bg-zinc-900/60 text-left px-4 py-3.5 rounded-2xl flex items-center gap-3 transition-all active:scale-[0.99] border border-white/40 dark:border-white/5 shadow-sm cursor-pointer select-none"
            >
              <div className={`p-1.5 rounded-lg flex items-center justify-center ${q.color}`}>
                <span className="material-symbols-outlined text-lg">{q.icon}</span>
              </div>
              <span className="text-sm font-semibold text-slate-800 dark:text-zinc-150">{q.text}</span>
            </button>
          ))}
        </div>
      </section>

      {/* Chat Messages Log */}
      {chatMessages.length > 0 && (
        <section className="flex flex-col gap-4 py-4 border-t border-slate-100 dark:border-zinc-800">
          <h3 className="text-xs font-bold text-slate-500 dark:text-zinc-400 uppercase tracking-widest pl-1">
            Agent Dialogue
          </h3>
          <div className="flex flex-col gap-4 max-h-[300px] overflow-y-auto pr-1">
            {chatMessages.map((msg) => (
              <div
                key={msg.id}
                className={`flex flex-col max-w-[85%] ${
                  msg.sender === 'user' ? 'self-end items-end' : 'self-start items-start'
                }`}
              >
                <div
                  className={`px-4 py-2.5 rounded-2xl text-sm font-semibold shadow-sm ${
                    msg.sender === 'user'
                      ? 'bg-brand-primary text-white rounded-tr-none'
                      : 'glass-card text-slate-800 dark:text-zinc-100 border border-white/50 dark:border-white/5 rounded-tl-none'
                  }`}
                >
                  {msg.text}
                </div>
                
                {/* Embedded recipes inside chat */}
                {msg.recipes && msg.recipes.length > 0 && (
                  <div className="w-full flex flex-col gap-3 mt-3">
                    {msg.recipes.map((recipe) => (
                      <RecipeCard key={recipe.id} recipe={recipe} />
                    ))}
                  </div>
                )}
              </div>
            ))}
            
            {generating && (
              <div className="flex items-center gap-2 self-start glass-card px-4 py-2.5 rounded-2xl rounded-tl-none border border-white/50 dark:border-white/5">
                <div className="w-2 h-2 bg-brand-primary rounded-full animate-bounce"></div>
                <div className="w-2 h-2 bg-brand-primary rounded-full animate-bounce delay-100"></div>
                <div className="w-2 h-2 bg-brand-primary rounded-full animate-bounce delay-200"></div>
              </div>
            )}
            
            <div ref={bottomRef}></div>
          </div>
        </section>
      )}

      {/* Input Prompter */}
      <section className="glass-panel p-3 rounded-2xl border border-white/50 dark:border-white/10 flex items-center gap-2 shadow-md">
        <input 
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSendPrompt(inputValue)}
          placeholder="Ask about recipes, storage, or cooking..."
          className="flex-1 bg-transparent border-0 outline-none text-slate-800 dark:text-zinc-100 placeholder-slate-400 text-sm font-semibold px-2 py-1"
        />
        <button 
          onClick={() => handleSendPrompt(inputValue)}
          className="w-10 h-10 rounded-xl bg-brand-primary hover:bg-brand-accent text-white flex items-center justify-center shadow-md shadow-brand-primary/20 active:scale-95 transition-transform cursor-pointer"
        >
          <span className="material-symbols-outlined text-lg">arrow_upward</span>
        </button>
      </section>

      {/* Suggested Recipes List */}
      <section className="mt-4 border-t border-slate-100 dark:border-zinc-800 pt-6">
        <div className="flex justify-between items-center mb-5">
          <h3 className="text-xl font-bold text-slate-900 dark:text-zinc-50">Smart Recipes</h3>
          <span className="text-xs font-bold text-slate-500 dark:text-zinc-400">Chef Curated</span>
        </div>

        <div className="flex flex-col gap-6">
          {recipes.map((recipe, idx) => (
            <RecipeCard key={recipe.id} recipe={recipe} isFeatured={idx === 0} />
          ))}

          {/* Surprise Me Dashed Bento Box */}
          <div
            onClick={handleSurpriseMe}
            className="glass-panel border-2 border-dashed border-brand-primary/30 rounded-2xl flex flex-col items-center justify-center p-8 text-center group cursor-pointer hover:bg-white/40 dark:hover:bg-zinc-900/20 transition-all hover:border-brand-primary active:scale-[0.99] select-none"
          >
            <div className="w-14 h-14 rounded-2xl glass-panel flex items-center justify-center mb-3 group-hover:scale-105 transition-transform shadow-sm">
              <span className="material-symbols-outlined text-3xl gemini-gradient-text">auto_awesome</span>
            </div>
            <h4 className="font-bold text-slate-900 dark:text-zinc-100 text-base mb-1">Surprise Me</h4>
            <p className="text-xs text-slate-500 dark:text-zinc-400 max-w-xs leading-normal">
              Generate a random recipe using whatever you have left in your inventory.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}

function RecipeCard({ recipe, isFeatured }: { recipe: Recipe; isFeatured?: boolean }) {
  const [showInstructions, setShowInstructions] = useState(false);

  return (
    <div
      className={`glass-card rounded-2xl overflow-hidden shadow-md border border-white/50 dark:border-white/10 flex flex-col transition-all hover:shadow-lg ${
        isFeatured ? 'md:flex-col' : ''
      }`}
    >
      <div className={`relative h-48 overflow-hidden ${isFeatured ? 'h-64' : 'h-40'}`}>
        <img
          src={recipe.imageUrl || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&q=80&w=600&h=400'}
          alt={recipe.title}
          className="w-full h-full object-cover transition-transform duration-700 hover:scale-102"
        />
        <div className="absolute top-3 left-3 flex gap-2">
          {isFeatured && (
            <span className="px-2.5 py-1 bg-brand-primary text-white text-[9px] font-bold rounded-md shadow-sm uppercase tracking-wide">
              Featured
            </span>
          )}
          <span className="px-2.5 py-1 bg-white/80 dark:bg-zinc-900/80 backdrop-blur-sm text-slate-900 dark:text-zinc-100 text-[9px] font-bold rounded-md shadow-sm border border-white/30 dark:border-white/5">
            Saves {recipe.savingItems.length} items
          </span>
        </div>
      </div>

      <div className="p-5 flex flex-col gap-4">
        <div>
          <h4 className="text-lg font-bold text-slate-900 dark:text-zinc-100 mb-1.5 leading-snug">
            {recipe.title}
          </h4>
          <p className="text-xs text-slate-600 dark:text-zinc-300 leading-relaxed">
            {recipe.description}
          </p>
        </div>

        <div className="bg-white/40 dark:bg-zinc-900/20 border border-white/40 dark:border-white/5 rounded-xl p-3">
          <p className="text-[10px] font-bold text-brand-primary dark:text-orange-400 mb-2 uppercase tracking-wider">
            Expiring Ingredients:
          </p>
          <div className="flex flex-wrap gap-1.5">
            {recipe.savingItems.map((item) => (
              <span
                key={item}
                className="bg-brand-primary/5 dark:bg-brand-primary/10 border border-brand-primary/20 text-brand-primary dark:text-orange-400 text-[10px] font-bold px-2 py-0.5 rounded-md"
              >
                {item}
              </span>
            ))}
          </div>
        </div>

        {showInstructions && recipe.instructions && (
          <div className="text-xs text-slate-650 dark:text-zinc-450 border-t border-slate-100 dark:border-zinc-800 pt-3 animate-in fade-in duration-200">
            <p className="font-bold text-slate-800 dark:text-zinc-100 mb-2">Instructions:</p>
            <p className="whitespace-pre-line leading-relaxed">{recipe.instructions}</p>
          </div>
        )}

        <button
          onClick={() => setShowInstructions(!showInstructions)}
          className="w-full py-2.5 bg-brand-primary hover:bg-brand-accent text-white text-xs font-bold rounded-xl flex items-center justify-center gap-1.5 shadow-md shadow-brand-primary/10 active:scale-98 transition-transform cursor-pointer select-none"
        >
          <span>{showInstructions ? 'Hide Method' : 'View Recipe'}</span>
          <span className="material-symbols-outlined text-sm">
            {showInstructions ? 'expand_less' : 'arrow_forward'}
          </span>
        </button>
      </div>
    </div>
  );
}
