/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { AppView } from '../types';

interface TopBarProps {
  darkMode: boolean;
  onToggleDarkMode: () => void;
  onNavigate: (view: AppView) => void;
  isLoggedIn: boolean;
}

export default function TopBar({ darkMode, onToggleDarkMode, onNavigate, isLoggedIn }: TopBarProps) {
  return (
    <header className="glass-panel flex justify-between items-center px-6 h-16 w-full border-b border-white/20 dark:border-white/10 fixed top-0 left-0 z-40 transition-colors duration-300">
      <div className="flex items-center gap-4">
        <span className="material-symbols-outlined text-brand-primary cursor-pointer text-2xl select-none" onClick={() => onNavigate('stats')}>
          menu
        </span>
        <h1 
          className="text-lg font-bold text-slate-900 dark:text-zinc-100 cursor-pointer select-none"
          onClick={() => onNavigate('stats')}
        >
          SmartExpApp
        </h1>
      </div>
      
      <div className="flex items-center gap-3">
        {/* Theme Toggle Switch */}
        <label className="relative inline-flex items-center cursor-pointer select-none">
          <input 
            type="checkbox" 
            checked={darkMode}
            onChange={onToggleDarkMode}
            className="sr-only peer" 
          />
          <div className="w-14 h-7 bg-black/10 dark:bg-white/10 rounded-full peer transition-all duration-300 border border-black/5 dark:border-white/10 flex items-center px-1 justify-between relative">
            <span className="material-symbols-outlined text-[16px] text-brand-primary dark:text-zinc-500 z-10">
              light_mode
            </span>
            <span className="material-symbols-outlined text-[16px] text-slate-400 dark:text-brand-primary z-10">
              dark_mode
            </span>
            <div className="absolute left-1 w-5 h-5 bg-white dark:bg-zinc-800 rounded-full transition-all duration-300 peer-checked:translate-x-7 shadow-sm"></div>
          </div>
        </label>

        {isLoggedIn && (
          <>
            <button 
              onClick={() => onNavigate('inventory')}
              className="w-9 h-9 rounded-full bg-black/5 dark:bg-white/5 flex items-center justify-center hover:bg-black/10 dark:hover:bg-white/10 transition-colors text-slate-700 dark:text-zinc-300"
            >
              <span className="material-symbols-outlined text-xl">search</span>
            </button>
            <div 
              onClick={() => onNavigate('settings')}
              className="w-9 h-9 rounded-full overflow-hidden border-2 border-brand-primary cursor-pointer active:scale-95 transition-transform shadow-sm"
            >
              <img 
                alt="Profile" 
                className="w-full h-full object-cover" 
                src="https://lh3.googleusercontent.com/aida-public/AB6AXuDGM0w84TTaarBVuLhfum3nw3XddGgKpUMFdc-uB-KZaD-f00G-CLli38dNOjNzL18h2fyq1CRs8HajvI2oc283h0dpXTkpaduLNGhG1FE9V1K_HPe3_X64YHzJAHaHQVX8XeiZH1D3C50lTGM_bsOwrMN6MijlUp_UoacKKWuO8m807Pxpe416eMrvrFxveSTxnIF0z-vsW1TZoNaf0UW4lXoVmZuNCv8Nw-5a4UNJ9pJDCpJiqQ77rQl8_4eLqGp7BEqTo1NmtTFs"
              />
            </div>
          </>
        )}
      </div>
    </header>
  );
}
