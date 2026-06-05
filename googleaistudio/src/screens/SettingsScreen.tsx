/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { motion } from 'motion/react';
import { ChevronRight, LogOut, Bell, Refrigerator, User, HelpCircle } from 'lucide-react';

export default function SettingsScreen() {
  return (
    <div className="flex flex-col gap-8 pb-24">
      <section className="flex flex-col items-center py-8 gap-4">
        <div className="w-24 h-24 rounded-full overflow-hidden border-2 border-slate-200 shadow-sm relative">
          <img 
            src="https://images.unsplash.com/photo-1599566150163-29194dcaad36?auto=format&fit=crop&q=80&w=200" 
            alt="Alex Johnson" 
            className="w-full h-full object-cover"
          />
        </div>
        
        <div className="text-center">
          <h2 className="text-2xl font-bold text-slate-900">Alex Johnson</h2>
          <p className="text-slate-500 text-sm mt-1">alex.johnson@example.com</p>
        </div>
        
        <motion.button
          whileTap={{ scale: 0.95 }}
          className="mt-2 px-6 py-2 bg-white border border-slate-200 text-slate-900 font-bold text-sm rounded-full shadow-sm hover:bg-slate-50 transition-colors"
        >
          Edit Profile
        </motion.button>
      </section>

      <section className="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-sm">
        <div className="px-6 py-4 bg-slate-50 border-b border-slate-200">
          <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">Preferences</span>
        </div>
        
        <SettingsItem 
          icon={<Bell size={20} />} 
          title="Notification Settings" 
          description="Manage expiry alerts and emails"
          hasToggle
        />
        <SettingsItem 
          icon={<Refrigerator size={20} />} 
          title="Storage Preferences" 
          description="Default locations and categories"
          hasChevron
        />
        <SettingsItem 
          icon={<User size={20} />} 
          title="Account Details" 
          description="Password, billing, and data"
          hasChevron
        />
        <SettingsItem 
          icon={<HelpCircle size={20} />} 
          title="Help & Support" 
          description="FAQs and contact information"
          hasChevron
          last
        />
      </section>

      <motion.button
        whileTap={{ scale: 0.98 }}
        className="w-full py-4 border border-red-500 text-red-500 font-bold text-base rounded-xl flex items-center justify-center gap-2 hover:bg-red-50 transition-all"
      >
        <LogOut size={20} />
        Sign Out
      </motion.button>
    </div>
  );
}

function SettingsItem({ icon, title, description, hasChevron, hasToggle, last }: { 
  icon: any, 
  title: string, 
  description: string, 
  hasChevron?: boolean, 
  hasToggle?: boolean,
  last?: boolean 
}) {
  return (
    <div className={`px-6 py-5 flex items-center justify-between hover:bg-slate-50 transition-colors cursor-pointer group ${!last ? 'border-b border-slate-100' : ''}`}>
      <div className="flex items-center gap-4">
        <div className="p-3 bg-slate-100 rounded-full text-slate-500 transition-colors">
          {icon}
        </div>
        <div>
          <h4 className="font-bold text-slate-900 mb-1">{title}</h4>
          <p className="text-sm text-slate-500">{description}</p>
        </div>
      </div>
      
      {hasChevron && <ChevronRight size={20} className="text-slate-400" />}
      
      {hasToggle && (
        <div className="w-12 h-7 bg-brand-primary rounded-full relative p-1 shadow-inner transition-colors">
          <div className="absolute right-1 top-1 w-5 h-5 bg-white rounded-full shadow-sm"></div>
        </div>
      )}
    </div>
  );
}
// Note: Lucide icon for Refrigerator/Kitchen might be Fridge or Refrigerator depending on version.
// Using standard library components.
