/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState, type FormEvent } from 'react';
import { AppView } from '../types';

interface AccountDetailsScreenProps {
  onNavigate: (view: AppView) => void;
}

export default function AccountDetailsScreen({ onNavigate }: AccountDetailsScreenProps) {
  const [name, setName] = useState('Alex Johnson');
  const [email, setEmail] = useState('alex.johnson@smartexp.io');
  const [location, setLocation] = useState('New York, NY');
  const [phone, setPhone] = useState('+1 (555) 019-2834');
  const [saved, setSaved] = useState(false);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  return (
    <div className="flex flex-col gap-6 pb-24">
      {/* Header with back navigation */}
      <div className="flex items-center gap-3">
        <button 
          onClick={() => onNavigate('settings')}
          className="w-10 h-10 rounded-full glass-card hover:bg-black/5 dark:hover:bg-white/5 flex items-center justify-center text-slate-800 dark:text-zinc-200 select-none cursor-pointer"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
        <div>
          <h2 className="text-2xl font-bold text-slate-900 dark:text-zinc-50">Account Details</h2>
          <p className="text-sm text-slate-500 dark:text-zinc-400">Manage your profile and security details</p>
        </div>
      </div>

      {/* Profile Card */}
      <div className="glass-card rounded-3xl p-6 shadow-xl border border-white/50 dark:border-white/10 flex flex-col md:flex-row items-center gap-6">
        <div className="relative">
          <div className="w-24 h-24 rounded-full overflow-hidden border-4 border-brand-primary p-0.5 shadow-md">
            <img 
              alt="Alex Johnson" 
              className="w-full h-full object-cover rounded-full" 
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuDNTMBFUmSIR5PdZh7UJTe1c57dQJCQebC5Goo5mXw5Q8oNbOPzUer4FtLGcSbX974Umm99SDIGO5LwSH8bpy33aLSYAKTBYeRWMGtW95_Z2CNWR8UYvT8oGwJsYoXYJmwf2Qy-E-4dFnLvZmvCYM773LeMp4p8hOBdubPhCxnLs05suu96cusJ_FIPclX08nDiYFalMNaKamSNXEyrXv1BiK7H0sdT5EURjgwxlhI6HsgQQSCRK4wF1Ay1Y7qYb-AyvPJy32Zx-d8e"
            />
          </div>
          <div className="absolute bottom-0 right-0 bg-brand-primary text-white p-1.5 rounded-full shadow-md border-2 border-slate-50 dark:border-zinc-900 cursor-pointer active:scale-90 transition-transform flex items-center justify-center">
            <span className="material-symbols-outlined text-[14px]">edit</span>
          </div>
        </div>

        <div className="text-center md:text-left">
          <h3 className="text-xl font-bold text-slate-900 dark:text-zinc-50">{name}</h3>
          <p className="text-sm text-slate-500 dark:text-zinc-400">{email}</p>
          <div className="mt-2 flex flex-wrap justify-center md:justify-start gap-2">
            <span className="glass-card bg-white/40 dark:bg-zinc-800/40 px-3 py-1 rounded-full text-[10px] font-bold text-brand-primary flex items-center gap-1 border border-white/50 dark:border-white/5">
              <span className="material-symbols-outlined text-[12px]" style={{ fontVariationSettings: "'FILL' 1" }}>verified_user</span>
              Pro Member
            </span>
          </div>
        </div>
      </div>

      {/* Edit Form */}
      <form onSubmit={handleSubmit} className="glass-card rounded-3xl p-6 border border-white/50 dark:border-white/10 flex flex-col gap-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-zinc-400 mb-1.5">
              Full Name
            </label>
            <input 
              type="text" 
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-4 py-3 bg-white/50 dark:bg-black/20 border border-slate-200 dark:border-zinc-800 rounded-xl text-slate-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all text-sm font-semibold"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-zinc-400 mb-1.5">
              Email Address
            </label>
            <input 
              type="email" 
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-4 py-3 bg-white/50 dark:bg-black/20 border border-slate-200 dark:border-zinc-800 rounded-xl text-slate-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all text-sm font-semibold"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-zinc-400 mb-1.5">
              Phone Number
            </label>
            <input 
              type="text" 
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              className="w-full px-4 py-3 bg-white/50 dark:bg-black/20 border border-slate-200 dark:border-zinc-800 rounded-xl text-slate-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all text-sm font-semibold"
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-zinc-400 mb-1.5">
              Location
            </label>
            <input 
              type="text" 
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              className="w-full px-4 py-3 bg-white/50 dark:bg-black/20 border border-slate-200 dark:border-zinc-800 rounded-xl text-slate-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all text-sm font-semibold"
            />
          </div>
        </div>

        <div className="flex items-center justify-between mt-4">
          <button 
            type="submit"
            className="px-6 py-3 bg-brand-primary hover:bg-brand-accent text-white font-bold rounded-xl shadow-lg shadow-brand-primary/20 active:scale-98 transition-all text-sm cursor-pointer"
          >
            Save Changes
          </button>
          
          {saved && (
            <span className="text-xs font-bold text-emerald-500 bg-emerald-500/10 px-3 py-1.5 rounded-lg border border-emerald-500/20 flex items-center gap-1">
              <span className="material-symbols-outlined text-sm">check_circle</span>
              Changes saved successfully!
            </span>
          )}
        </div>
      </form>
    </div>
  );
}
