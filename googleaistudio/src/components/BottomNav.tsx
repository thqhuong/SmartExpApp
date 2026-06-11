/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { AppView } from '../types';

interface BottomNavProps {
  activeView: AppView;
  onViewChange: (view: AppView) => void;
}

export default function BottomNav({ activeView, onViewChange }: BottomNavProps) {
  // Hide bottom nav if we are in SignIn Screen
  if (activeView === 'signin') return null;

  const items = [
    { id: 'inventory', label: 'Inventory', icon: 'inventory_2' },
    { id: 'stats', label: 'Stats', icon: 'leaderboard' },
    { id: 'add', label: 'Add', icon: 'add', isFab: true },
    { id: 'recipes', label: 'Agent', icon: 'auto_awesome' },
    { id: 'settings', label: 'Settings', icon: 'settings' },
  ] as const;

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 glass-panel rounded-t-[2.5rem] border-t border-white/60 dark:border-white/10 px-6 pb-safe h-20 flex justify-between items-center max-w-2xl mx-auto shadow-[0_-8px_32px_rgba(0,0,0,0.05)] dark:shadow-[0_-8px_32px_rgba(0,0,0,0.3)] transition-colors duration-300">
      {items.map((item) => {
        const isActive = activeView === item.id || 
          (item.id === 'settings' && ['help-support', 'notification-settings', 'account-details'].includes(activeView));
        
        if (item.isFab) {
          return (
            <div key={item.id} className="relative -top-8 select-none">
              <button
                onClick={() => onViewChange('add')}
                className="flex items-center justify-center bg-brand-primary text-white rounded-full h-16 w-16 shadow-[0_8px_32px_rgba(255,140,0,0.4)] dark:shadow-[0_8px_32px_rgba(255,140,0,0.6)] hover:scale-105 active:scale-90 transition-all border-4 border-slate-50 dark:border-zinc-950"
              >
                <span className="material-symbols-outlined text-3xl font-bold">add</span>
              </button>
            </div>
          );
        }

        return (
          <button
            key={item.id}
            onClick={() => onViewChange(item.id)}
            className={`flex flex-col items-center justify-center w-16 gap-1 select-none transition-transform duration-150 active:scale-95 ${
              isActive 
                ? 'text-brand-primary font-semibold' 
                : 'text-slate-500 dark:text-zinc-400 hover:text-brand-primary'
            }`}
          >
            <span 
              className="material-symbols-outlined text-[26px]"
              style={{ fontVariationSettings: isActive ? "'FILL' 1" : "'FILL' 0" }}
            >
              {item.icon}
            </span>
            <span className="text-[10px] font-bold uppercase tracking-wider">
              {item.label}
            </span>
          </button>
        );
      })}
    </nav>
  );
}
