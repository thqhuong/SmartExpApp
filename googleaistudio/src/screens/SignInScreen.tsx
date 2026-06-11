/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from 'react';

interface SignInScreenProps {
  onSignIn: () => void;
}

export default function SignInScreen({ onSignIn }: SignInScreenProps) {
  const [email, setEmail] = useState('alex.johnson@smartexp.io');
  const [password, setPassword] = useState('password123');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSignIn();
  };

  return (
    <div className="min-h-[75vh] flex items-center justify-center px-4 py-8">
      <div className="glass-card w-full max-w-md rounded-3xl p-8 shadow-2xl border border-white/40 dark:border-white/10 flex flex-col gap-6">
        <div className="text-center">
          <div className="w-16 h-16 rounded-2xl bg-brand-primary/10 flex items-center justify-center mx-auto mb-4 border border-brand-primary/20">
            <span className="material-symbols-outlined text-4xl text-brand-primary">kitchen</span>
          </div>
          <h2 className="text-2xl font-bold text-slate-900 dark:text-zinc-50">Welcome Back</h2>
          <p className="text-sm text-slate-500 dark:text-zinc-400 mt-1">
            Sign in to track your items and generate recipes
          </p>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-zinc-400 mb-1.5">
              Email Address
            </label>
            <input 
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-4 py-3 bg-white/50 dark:bg-black/20 border border-slate-200 dark:border-zinc-800 rounded-xl text-slate-900 dark:text-zinc-100 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all text-sm"
              placeholder="name@example.com"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-zinc-400 mb-1.5">
              Password
            </label>
            <input 
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-4 py-3 bg-white/50 dark:bg-black/20 border border-slate-200 dark:border-zinc-800 rounded-xl text-slate-900 dark:text-zinc-100 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all text-sm"
              placeholder="••••••••"
              required
            />
          </div>

          <button 
            type="submit"
            className="w-full py-3.5 bg-brand-primary hover:bg-brand-accent text-white font-bold rounded-xl shadow-lg shadow-brand-primary/20 active:scale-98 transition-all text-sm mt-2 cursor-pointer"
          >
            Sign In
          </button>
        </form>

        <div className="flex items-center gap-4 py-2">
          <div className="flex-1 h-px bg-slate-200 dark:bg-zinc-800"></div>
          <span className="text-xs text-slate-400 uppercase tracking-widest font-bold">Or continue with</span>
          <div className="flex-1 h-px bg-slate-200 dark:bg-zinc-800"></div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <button 
            onClick={onSignIn}
            className="flex items-center justify-center gap-2 py-3 bg-white/50 dark:bg-zinc-900/40 border border-slate-200 dark:border-zinc-800 hover:bg-slate-100 dark:hover:bg-zinc-900/60 rounded-xl text-slate-800 dark:text-zinc-200 font-semibold text-xs active:scale-98 transition-all cursor-pointer"
          >
            <span className="material-symbols-outlined text-[18px]">verified_user</span>
            Google
          </button>
          <button 
            onClick={onSignIn}
            className="flex items-center justify-center gap-2 py-3 bg-white/50 dark:bg-zinc-900/40 border border-slate-200 dark:border-zinc-800 hover:bg-slate-100 dark:hover:bg-zinc-900/60 rounded-xl text-slate-800 dark:text-zinc-200 font-semibold text-xs active:scale-98 transition-all cursor-pointer"
          >
            <span className="material-symbols-outlined text-[18px]">lock</span>
            Apple
          </button>
        </div>
      </div>
    </div>
  );
}
