/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { Package, BarChart3, PlusCircle, Utensils, Settings } from 'lucide-react';
import { motion } from 'motion/react';
import { AppView } from '../types';

interface BottomNavProps {
  activeView: AppView;
  onViewChange: (view: AppView) => void;
}

const NAV_ITEMS = [
  { id: 'inventory', label: 'Items', icon: Package },
  { id: 'stats', label: 'Home', icon: BarChart3 },
  { id: 'add', label: 'Add', icon: PlusCircle },
  { id: 'recipes', label: 'Meals', icon: Utensils },
  { id: 'settings', label: 'Profile', icon: Settings },
] as const;

export default function BottomNav({ activeView, onViewChange }: BottomNavProps) {
  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 bg-white border-t border-slate-100 pb-safe shadow-[0_-4px_20px_rgba(0,0,0,0.03)]">
      <div className="flex justify-around items-center h-20 max-w-2xl mx-auto px-4">
        {NAV_ITEMS.map((item) => {
          const Icon = item.icon;
          const isActive = activeView === item.id;
          
          return (
            <button
              key={item.id}
              onClick={() => onViewChange(item.id)}
              className="flex flex-col items-center justify-center flex-1 relative group"
            >
              <motion.div
                animate={{ 
                  scale: isActive ? 1.05 : 1,
                  color: isActive ? '#ff8c00' : '#8c8c8c' 
                }}
                className={`flex flex-col items-center px-3 py-2 rounded-xl transition-all duration-300 ${isActive ? 'bg-brand-primary/10 border border-brand-primary/20' : 'hover:bg-slate-50'}`}
              >
                <Icon size={22} strokeWidth={isActive ? 2.5 : 2} />
                <span className="text-[10px] font-bold mt-1 uppercase tracking-widest">
                  {item.label}
                </span>
              </motion.div>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
// Note: Lucide icons for Inventory and Analytics might need names from recent versions.
// Using standard library names.
