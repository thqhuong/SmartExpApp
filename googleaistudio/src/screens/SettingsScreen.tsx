/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { AppView } from '../types';

interface SettingsScreenProps {
  onNavigate: (view: AppView) => void;
  onSignOut: () => void;
  darkMode: boolean;
  onToggleDarkMode: () => void;
}

export default function SettingsScreen({ 
  onNavigate, 
  onSignOut, 
  darkMode, 
  onToggleDarkMode 
}: SettingsScreenProps) {

  return (
    <div className="flex flex-col gap-6 pb-24">
      {/* User Profile Hero Section */}
      <section className="glass-panel rounded-3xl p-6 flex flex-col items-center gap-4 shadow-xl text-center border border-white/50 dark:border-white/10">
        <div className="relative">
          <div className="w-28 h-28 rounded-full overflow-hidden glass-panel border-4 border-white dark:border-zinc-800 p-1 shadow-lg">
            <img 
              alt="Alex Johnson" 
              className="w-full h-full object-cover rounded-full" 
              src="https://lh3.googleusercontent.com/aida-public/AB6AXuDNTMBFUmSIR5PdZh7UJTe1c57dQJCQebC5Goo5mXw5Q8oNbOPzUer4FtLGcSbX974Umm99SDIGO5LwSH8bpy33aLSYAKTBYeRWMGtW95_Z2CNWR8UYvT8oGwJsYoXYJmwf2Qy-E-4dFnLvZmvCYM773LeMp4p8hOBdubPhCxnLs05suu96cusJ_FIPclX08nDiYFalMNaKamSNXEyrXv1BiK7H0sdT5EURjgwxlhI6HsgQQSCRK4wF1Ay1Y7qYb-AyvPJy32Zx-d8e"
            />
          </div>
          <div 
            onClick={() => onNavigate('account-details')}
            className="absolute bottom-1 right-1 bg-brand-primary text-white p-2 rounded-full shadow-md border border-white/50 dark:border-zinc-800 cursor-pointer active:scale-95 transition-transform flex items-center justify-center"
          >
            <span className="material-symbols-outlined text-[18px]">edit</span>
          </div>
        </div>
        <div>
          <h2 className="text-xl font-bold text-slate-900 dark:text-zinc-50">Alex Johnson</h2>
          <p className="text-xs text-slate-500 dark:text-zinc-400">alex.johnson@smartexp.io</p>
          <div className="mt-3 flex justify-center gap-2">
            <span className="glass-card bg-white/40 dark:bg-zinc-805/40 px-3.5 py-1 rounded-full text-[10px] font-bold text-brand-primary flex items-center gap-1 border border-white/50 dark:border-white/5">
              <span className="material-symbols-outlined text-[12px]" style={{ fontVariationSettings: "'FILL' 1" }}>verified_user</span>
              Pro Member
            </span>
            <span className="glass-card bg-white/40 dark:bg-zinc-805/40 px-3.5 py-1 rounded-full text-[10px] font-bold text-slate-500 dark:text-zinc-400 flex items-center gap-1 border border-white/50 dark:border-white/5">
              <span className="material-symbols-outlined text-[12px]">location_on</span>
              New York, NY
            </span>
          </div>
        </div>
      </section>

      {/* Gemini Live Quota progress */}
      <div className="glass-card rounded-2xl p-5 flex flex-col gap-3 shadow-md border border-white/50 dark:border-white/10">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-brand-primary/10 rounded-xl text-brand-primary border border-brand-primary/20 flex items-center justify-center">
            <span className="material-symbols-outlined">auto_awesome</span>
          </div>
          <div>
            <h3 className="font-bold text-slate-900 dark:text-zinc-100 text-sm">Gemini Live Quota</h3>
            <p className="text-xs text-slate-500 dark:text-zinc-400">Daily usage tracking</p>
          </div>
        </div>
        <div className="w-full bg-slate-200 dark:bg-zinc-800 rounded-full h-2 overflow-hidden border border-black/5 dark:border-white/5">
          <div 
            className="bg-brand-primary h-full rounded-full shadow-[0_0_8px_rgba(255,140,0,0.3)]" 
            style={{ width: '90%' }}
          ></div>
        </div>
        <div className="flex justify-between text-[10px] font-bold text-slate-500 dark:text-zinc-450 uppercase tracking-wider">
          <span>90% used</span>
          <span>10 remaining</span>
        </div>
      </div>

      {/* Settings Options Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Toggle Dark Mode Option (Inline Settings Toggle) */}
        <div className="glass-card rounded-2xl p-5 flex flex-col justify-between border border-white/50 dark:border-white/10 shadow-sm">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2.5 bg-indigo-500/10 rounded-xl text-indigo-500 border border-indigo-500/20 flex items-center justify-center">
                <span className="material-symbols-outlined">dark_mode</span>
              </div>
              <div>
                <h3 className="font-bold text-slate-900 dark:text-zinc-100 text-sm">Dark Theme</h3>
                <p className="text-xs text-slate-500 dark:text-zinc-400">Toggle dark UI mode</p>
              </div>
            </div>
            <label className="relative inline-flex items-center cursor-pointer select-none">
              <input 
                type="checkbox" 
                checked={darkMode}
                onChange={onToggleDarkMode}
                className="sr-only peer" 
              />
              <div className="w-11 h-6 bg-slate-200 dark:bg-zinc-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 dark:after:border-zinc-700 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-brand-primary"></div>
            </label>
          </div>
        </div>

        {/* Notifications Settings */}
        <div 
          onClick={() => onNavigate('notification-settings')}
          className="glass-card rounded-2xl p-5 flex items-center justify-between border border-white/50 dark:border-white/10 hover:bg-white/10 transition-colors cursor-pointer group shadow-sm select-none"
        >
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-blue-500/10 rounded-xl text-blue-500 border border-blue-500/20 flex items-center justify-center">
              <span className="material-symbols-outlined">notifications</span>
            </div>
            <div>
              <h3 className="font-bold text-slate-900 dark:text-zinc-100 text-sm">Notification Settings</h3>
              <p className="text-xs text-slate-500 dark:text-zinc-400">Manage alerts and updates</p>
            </div>
          </div>
          <span className="material-symbols-outlined text-slate-400 group-hover:text-brand-primary transition-colors">
            chevron_right
          </span>
        </div>

        {/* Storage Preferences */}
        <div className="glass-card rounded-2xl p-5 flex flex-col justify-between border border-white/50 dark:border-white/10 hover:bg-white/10 transition-colors group shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-3">
              <div className="p-2.5 bg-teal-500/10 rounded-xl text-teal-500 border border-teal-500/20 flex items-center justify-center">
                <span className="material-symbols-outlined">cloud</span>
              </div>
              <div>
                <h3 className="font-bold text-slate-900 dark:text-zinc-100 text-sm">Storage & Sync</h3>
                <p className="text-xs text-slate-500 dark:text-zinc-400">Cloud backup preferences</p>
              </div>
            </div>
            <span className="material-symbols-outlined text-slate-400">lock</span>
          </div>
          <div className="w-full bg-slate-200 dark:bg-zinc-800 rounded-full h-1.5 overflow-hidden">
            <div className="h-full bg-teal-500" style={{ width: '65%' }}></div>
          </div>
          <p className="text-[10px] font-bold text-slate-400 mt-2 uppercase tracking-wide">6.5 GB of 10 GB cloud storage used</p>
        </div>

        {/* Account Details */}
        <div 
          onClick={() => onNavigate('account-details')}
          className="glass-card rounded-2xl p-5 flex items-center justify-between border border-white/50 dark:border-white/10 hover:bg-white/10 transition-colors cursor-pointer group shadow-sm select-none"
        >
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-zinc-500/10 rounded-xl text-slate-650 dark:text-zinc-350 border border-slate-200 dark:border-zinc-800 flex items-center justify-center">
              <span className="material-symbols-outlined">manage_accounts</span>
            </div>
            <div>
              <h3 className="font-bold text-slate-900 dark:text-zinc-100 text-sm">Account Details</h3>
              <p className="text-xs text-slate-500 dark:text-zinc-400">Privacy and security controls</p>
            </div>
          </div>
          <span className="material-symbols-outlined text-slate-400 group-hover:text-brand-primary transition-colors">
            chevron_right
          </span>
        </div>

        {/* Help & Support */}
        <div 
          onClick={() => onNavigate('help-support')}
          className="glass-card rounded-2xl p-5 flex items-center justify-between border border-white/50 dark:border-white/10 hover:bg-white/10 transition-colors cursor-pointer group shadow-sm select-none col-span-1 md:col-span-2"
        >
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-orange-500/10 rounded-xl text-orange-500 border border-orange-500/20 flex items-center justify-center">
              <span className="material-symbols-outlined">help_center</span>
            </div>
            <div>
              <h3 className="font-bold text-slate-900 dark:text-zinc-100 text-sm">Help & Support</h3>
              <p className="text-xs text-slate-500 dark:text-zinc-400">FAQs and support contacts</p>
            </div>
          </div>
          <span className="material-symbols-outlined text-slate-400 group-hover:text-brand-primary transition-colors">
            chevron_right
          </span>
        </div>
      </div>

      {/* Sign Out Button */}
      <div className="pt-6 flex flex-col items-center gap-3 select-none">
        <button 
          onClick={onSignOut}
          className="w-full max-w-sm flex items-center justify-center gap-2 py-4 px-6 bg-red-500/10 hover:bg-red-500 text-red-600 hover:text-white border border-red-500/20 rounded-2xl font-bold transition-all active:scale-95 shadow-md group cursor-pointer"
        >
          <span className="material-symbols-outlined text-lg">logout</span>
          Sign Out
        </button>
        <p className="text-[10px] font-bold text-slate-400 dark:text-zinc-500 uppercase tracking-widest">
          Version 2.4.1 (Build 890)
        </p>
      </div>
    </div>
  );
}
