/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { Menu, Bell } from 'lucide-react';
import { motion } from 'motion/react';

export default function TopBar() {
  return (
    <header className="sticky top-0 z-40 bg-surface-low border-b border-transparent">
      <div className="flex justify-between items-center px-6 py-4 max-w-2xl mx-auto">
        <motion.button 
          whileTap={{ scale: 0.9 }}
          className="p-2 text-brand-primary hover:bg-slate-100 rounded-full transition-colors"
        >
          <Menu size={24} />
        </motion.button>
        
        <h1 className="text-xl font-bold tracking-tight text-slate-900">SmartExp</h1>
        
        <motion.button 
          whileTap={{ scale: 0.9 }}
          className="p-2 text-brand-primary hover:bg-slate-100 rounded-full transition-colors relative"
        >
          <Bell size={24} />
          <span className="absolute top-2 right-2 w-2 h-2 bg-red-500 rounded-full border border-white"></span>
        </motion.button>
      </div>
    </header>
  );
}
