/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { Product, StorageMethod } from '../types';

interface StatsScreenProps {
  products: Product[];
  onNavigate: (view: any) => void;
}

export default function StatsScreen({ products, onNavigate }: StatsScreenProps) {
  const getDaysLeft = (expiryDate: string) => {
    const diff = new Date(expiryDate).getTime() - Date.now();
    return Math.ceil(diff / (1000 * 60 * 60 * 24));
  };

  const expiringSoonCount = products.filter(p => {
    const days = getDaysLeft(p.expiryDate);
    return days > 0 && days <= 3;
  }).length;

  const storageCounts = products.reduce((acc, p) => {
    acc[p.storage] = (acc[p.storage] || 0) + 1;
    return acc;
  }, {} as Record<StorageMethod, number>);

  const sortedSoonest = [...products]
    .filter(p => getDaysLeft(p.expiryDate) > 0)
    .sort((a, b) => new Date(a.expiryDate).getTime() - new Date(b.expiryDate).getTime())
    .slice(0, 3);

  const getDaysLeftLabel = (days: number) => {
    if (days <= 0) return 'EXPIRED';
    if (days === 1) return 'TOMORROW';
    return `IN ${days} DAYS`;
  };

  const getDaysLeftClass = (days: number) => {
    if (days <= 1) return 'text-red-500 bg-red-500/10 border-red-500/10 dark:text-red-400 dark:bg-red-500/20';
    if (days <= 3) return 'text-brand-primary bg-brand-primary/10 border-brand-primary/10 dark:text-orange-400 dark:bg-orange-500/20';
    return 'text-slate-500 bg-slate-100 dark:text-zinc-400 dark:bg-zinc-800';
  };

  const getStoragePercentage = (count: number) => {
    if (products.length === 0) return 0;
    return Math.min(100, (count / products.length) * 100);
  };

  return (
    <div className="flex flex-col gap-6 pb-24">
      {/* Items Expiring Soon Hero Card */}
      <section className="glass-card rounded-2xl p-6 relative overflow-hidden group flex items-center justify-between border-white/60 dark:border-white/10 shadow-lg transition-transform duration-200">
        <div>
          <p className="text-[10px] font-bold text-slate-500 dark:text-zinc-400 mb-2 uppercase tracking-widest">
            URGENT ATTENTION
          </p>
          <h2 className="text-2xl font-bold mb-1 text-brand-primary dark:text-orange-400">
            Items Expiring Soon
          </h2>
          <p className="text-sm text-slate-500 dark:text-zinc-400">
            Requires immediate attention
          </p>
        </div>
        <div className="w-20 h-20 rounded-full bg-red-500/10 dark:bg-red-500/20 shadow-[0_0_30px_rgba(239,68,68,0.15)] flex items-center justify-center relative z-10 border-4 border-white dark:border-zinc-800">
          <span className="text-3xl font-bold text-red-500 dark:text-red-400">{expiringSoonCount}</span>
        </div>
        <div className="absolute -top-4 -right-4 opacity-5 pointer-events-none text-brand-primary">
          <span className="material-symbols-outlined text-[120px]">warning</span>
        </div>
      </section>

      {/* Stats Row */}
      <div className="grid grid-cols-2 gap-4">
        <div className="glass-card rounded-2xl p-5 flex flex-col justify-between h-[120px] border-white/60 dark:border-white/10 shadow-sm">
          <div className="w-10 h-10 rounded-xl bg-white/60 dark:bg-zinc-800/60 flex items-center justify-center mb-auto border border-white dark:border-zinc-700">
            <span className="material-symbols-outlined text-brand-primary">check_circle</span>
          </div>
          <div>
            <p className="text-[10px] font-bold text-slate-500 dark:text-zinc-400 mb-1 uppercase tracking-wider">Total Tracked</p>
            <p className="text-2xl font-bold text-slate-900 dark:text-zinc-100">{products.length}</p>
          </div>
        </div>
        
        <div className="glass-card rounded-2xl p-5 flex flex-col justify-between h-[120px] border-white/60 dark:border-white/10 shadow-sm">
          <div className="w-10 h-10 rounded-xl bg-white/60 dark:bg-zinc-800/60 flex items-center justify-center mb-auto border border-white dark:border-zinc-700">
            <span className="material-symbols-outlined text-blue-500 dark:text-blue-400">eco</span>
          </div>
          <div>
            <p className="text-[10px] font-bold text-slate-500 dark:text-zinc-400 mb-1 uppercase tracking-wider">Waste Prevented</p>
            <p className="text-2xl font-bold text-slate-900 dark:text-zinc-100">
              3.2<span className="text-sm font-normal text-slate-500 dark:text-zinc-400 ml-1 lowercase">kg</span>
            </p>
          </div>
        </div>
      </div>

      {/* Storage Overview */}
      <section className="glass-card rounded-2xl p-6 border-white/60 dark:border-white/10 shadow-md">
        <div className="flex justify-between items-center mb-5">
          <h3 className="text-lg font-bold text-slate-900 dark:text-zinc-100">Storage Overview</h3>
          <span className="material-symbols-outlined text-slate-400 cursor-pointer">more_horiz</span>
        </div>
        
        <div className="flex flex-col gap-4">
          {/* Refrigerator */}
          <div className="bg-white/40 dark:bg-zinc-900/20 border border-white/40 dark:border-white/5 rounded-xl p-4 flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-white dark:bg-zinc-800 flex items-center justify-center flex-shrink-0 border border-white dark:border-zinc-700 shadow-sm text-brand-primary">
              <span className="material-symbols-outlined">kitchen</span>
            </div>
            <div className="flex-grow">
              <div className="flex justify-between items-center mb-2">
                <h4 className="font-semibold text-slate-950 dark:text-zinc-100 text-sm">Refrigerator</h4>
                <span className="text-[11px] font-bold text-slate-500 dark:text-zinc-400">
                  {storageCounts[StorageMethod.REFRIGERATOR] || 0} items
                </span>
              </div>
              <div className="w-full h-2 bg-slate-200 dark:bg-zinc-800 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-brand-primary rounded-full transition-all duration-500 shadow-[0_0_8px_rgba(255,140,0,0.3)]"
                  style={{ width: `${getStoragePercentage(storageCounts[StorageMethod.REFRIGERATOR] || 0)}%` }}
                ></div>
              </div>
            </div>
          </div>

          {/* Room Temperature */}
          <div className="bg-white/40 dark:bg-zinc-900/20 border border-white/40 dark:border-white/5 rounded-xl p-4 flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-white dark:bg-zinc-800 flex items-center justify-center flex-shrink-0 border border-white dark:border-zinc-700 shadow-sm text-blue-500">
              <span className="material-symbols-outlined">shelves</span>
            </div>
            <div className="flex-grow">
              <div className="flex justify-between items-center mb-2">
                <h4 className="font-semibold text-slate-950 dark:text-zinc-100 text-sm">Room Temperature</h4>
                <span className="text-[11px] font-bold text-slate-500 dark:text-zinc-400">
                  {storageCounts[StorageMethod.ROOM_TEMP] || 0} items
                </span>
              </div>
              <div className="w-full h-2 bg-slate-200 dark:bg-zinc-800 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-blue-500 rounded-full transition-all duration-500"
                  style={{ width: `${getStoragePercentage(storageCounts[StorageMethod.ROOM_TEMP] || 0)}%` }}
                ></div>
              </div>
            </div>
          </div>

          {/* Freezer */}
          <div className="bg-white/40 dark:bg-zinc-900/20 border border-white/40 dark:border-white/5 rounded-xl p-4 flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-white dark:bg-zinc-800 flex items-center justify-center flex-shrink-0 border border-white dark:border-zinc-700 shadow-sm text-slate-400">
              <span className="material-symbols-outlined">ac_unit</span>
            </div>
            <div className="flex-grow">
              <div className="flex justify-between items-center mb-2">
                <h4 className="font-semibold text-slate-950 dark:text-zinc-100 text-sm">Freezer</h4>
                <span className="text-[11px] font-bold text-slate-500 dark:text-zinc-400">
                  {storageCounts[StorageMethod.FREEZE] || 0} items
                </span>
              </div>
              <div className="w-full h-2 bg-slate-200 dark:bg-zinc-800 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-slate-400 rounded-full transition-all duration-500"
                  style={{ width: `${getStoragePercentage(storageCounts[StorageMethod.FREEZE] || 0)}%` }}
                ></div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Expiring Soonest List */}
      <section className="glass-card rounded-2xl p-6 border-white/60 dark:border-white/10 shadow-md">
        <div className="flex justify-between items-center mb-5">
          <h3 className="text-lg font-bold text-slate-900 dark:text-zinc-100">Expiring Soonest</h3>
          <button 
            onClick={() => onNavigate('inventory')}
            className="text-xs font-bold text-brand-primary tracking-wider uppercase hover:opacity-75 transition-opacity"
          >
            View All
          </button>
        </div>
        
        <div className="flex flex-col border border-white/40 dark:border-zinc-800/80 rounded-2xl overflow-hidden bg-white/20 dark:bg-black/10">
          {sortedSoonest.length === 0 ? (
            <div className="p-6 text-center text-sm text-slate-400">No items expiring soon!</div>
          ) : (
            sortedSoonest.map((item, idx) => {
              const days = getDaysLeft(item.expiryDate);
              const dateStr = new Date(item.expiryDate).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
              });

              return (
                <div 
                  key={item.id} 
                  className={`flex items-center gap-4 p-4 bg-white/40 dark:bg-zinc-900/30 ${
                    idx < sortedSoonest.length - 1 ? 'border-b border-white/30 dark:border-zinc-800/50' : ''
                  }`}
                >
                  <div className="w-14 h-14 rounded-xl bg-white dark:bg-zinc-800 flex items-center justify-center overflow-hidden flex-shrink-0 border border-white dark:border-zinc-700 shadow-sm">
                    {item.imageUrl ? (
                      <img src={item.imageUrl} alt={item.name} className="w-full h-full object-cover" />
                    ) : (
                      <span className="material-symbols-outlined text-slate-400">restaurant</span>
                    )}
                  </div>
                  
                  <div className="flex-grow">
                    <h4 className="font-semibold text-slate-900 dark:text-zinc-100 text-sm leading-snug">{item.name}</h4>
                    <p className="text-xs text-slate-500 dark:text-zinc-400 mt-0.5">{item.storage} • {item.unit}</p>
                  </div>
                  
                  <div className="flex flex-col items-end">
                    <span className={`text-[10px] font-bold px-2.5 py-0.5 rounded-full mb-1 border select-none uppercase ${getDaysLeftClass(days)}`}>
                      {getDaysLeftLabel(days)}
                    </span>
                    <span className="text-xs text-slate-500 dark:text-zinc-400 font-medium">{dateStr}</span>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </section>
    </div>
  );
}
