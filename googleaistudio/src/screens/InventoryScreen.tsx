/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from 'react';
import { Product, StorageMethod } from '../types';

interface InventoryScreenProps {
  products: Product[];
  onUpdateProducts: (newProducts: Product[]) => void;
}

export default function InventoryScreen({ products, onUpdateProducts }: InventoryScreenProps) {
  const [activeFilter, setActiveFilter] = useState<'All' | 'Room Temp' | 'Cool' | 'Frozen'>('All');
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);

  const getDaysLeft = (expiryDate: string) => {
    const diff = new Date(expiryDate).getTime() - Date.now();
    return Math.ceil(diff / (1000 * 60 * 60 * 24));
  };

  const getDaysLeftLabel = (days: number) => {
    if (days <= 0) return 'Expired';
    if (days === 1) return 'Tomorrow';
    return `${days} Days`;
  };

  const getDaysLeftClass = (days: number) => {
    if (days <= 0) return 'text-red-500 bg-red-500/10 border-red-500/10 dark:text-red-400 dark:bg-red-500/20';
    if (days <= 3) return 'text-brand-primary bg-brand-primary/10 border-brand-primary/10 dark:text-orange-400 dark:bg-orange-500/20';
    return 'text-slate-500 bg-slate-100 dark:text-zinc-400 dark:bg-zinc-800';
  };

  const handleDelete = (id: string) => {
    onUpdateProducts(products.filter((p) => p.id !== id));
    setSelectedProduct(null);
  };

  const handleAction = (id: string, actionType: 'consumed' | 'wasted' | 'donated') => {
    // In a fully persistent database, we would record an InventoryActionEntity.
    // For this UI, we remove the product from active inventory (representing action completion)
    // and can simulate success.
    onUpdateProducts(products.filter((p) => p.id !== id));
    setSelectedProduct(null);
  };

  const filteredProducts = products.filter((p) => {
    if (activeFilter === 'All') return true;
    if (activeFilter === 'Room Temp') return p.storage === StorageMethod.ROOM_TEMP;
    if (activeFilter === 'Cool') return p.storage === StorageMethod.REFRIGERATOR;
    if (activeFilter === 'Frozen') return p.storage === StorageMethod.FREEZE;
    return true;
  });

  const sortedProducts = [...filteredProducts].sort(
    (a, b) => new Date(a.expiryDate).getTime() - new Date(b.expiryDate).getTime()
  );

  return (
    <div className="flex flex-col gap-6 pb-24 relative">
      <div className="flex flex-col gap-1">
        <h2 className="text-3xl font-bold tracking-tight text-slate-900 dark:text-zinc-50">My Inventory</h2>
      </div>

      {/* Filter Row */}
      <div className="flex space-x-2 overflow-x-auto scrollbar-hide pb-2">
        {(['All', 'Room Temp', 'Cool', 'Frozen'] as const).map((filter) => {
          const isActive = activeFilter === filter;
          return (
            <button
              key={filter}
              onClick={() => setActiveFilter(filter)}
              className={`px-5 py-2.5 rounded-full whitespace-nowrap text-sm font-semibold transition-all border select-none cursor-pointer ${
                isActive
                  ? 'bg-brand-primary border-transparent text-white shadow-md shadow-brand-primary/20'
                  : 'bg-white/40 dark:bg-zinc-900/40 border-slate-200 dark:border-zinc-800 text-slate-600 dark:text-zinc-300 hover:bg-white/60 dark:hover:bg-zinc-900/60'
              }`}
            >
              {filter}
            </button>
          );
        })}
      </div>

      {/* Products Grid */}
      <div className="grid grid-cols-1 gap-4">
        {sortedProducts.length === 0 ? (
          <div className="glass-card rounded-2xl p-12 text-center text-slate-400 dark:text-zinc-500 border border-white/50 dark:border-white/10">
            <span className="material-symbols-outlined text-5xl mb-2">inventory_2</span>
            <p className="text-sm font-semibold">No products found in this category</p>
          </div>
        ) : (
          sortedProducts.map((product) => {
            const days = getDaysLeft(product.expiryDate);
            const isUrgent = days <= 3;
            const isExpired = days <= 0;
            const dateStr = new Date(product.expiryDate).toLocaleDateString('en-US', {
              month: 'short',
              day: 'numeric',
            });

            return (
              <div
                key={product.id}
                onClick={() => setSelectedProduct(product)}
                className={`glass-card rounded-2xl p-4 flex flex-col gap-4 shadow-sm relative overflow-hidden cursor-pointer select-none transition-all active:scale-[0.99] border hover:border-white/80 dark:hover:border-white/20 ${
                  isExpired
                    ? 'border-red-500/40 dark:border-red-500/30'
                    : isUrgent
                    ? 'border-brand-primary/40 dark:border-brand-primary/30'
                    : 'border-white/50 dark:border-white/10'
                }`}
              >
                {/* Expiring Soon / Expired status badge */}
                {(isExpired || isUrgent) && (
                  <div className={`absolute top-0 right-0 text-[9px] font-bold px-3 py-1 rounded-bl-lg uppercase tracking-wider ${
                    isExpired ? 'bg-red-500 text-white' : 'bg-brand-primary text-white'
                  }`}>
                    {isExpired ? 'Expired' : 'Expiring Soon'}
                  </div>
                )}

                <div className="flex items-center gap-4 mt-1.5">
                  <div className="w-14 h-14 rounded-xl bg-white dark:bg-zinc-800 flex items-center justify-center overflow-hidden border border-white dark:border-zinc-700 shadow-sm flex-shrink-0">
                    {product.imageUrl ? (
                      <img src={product.imageUrl} alt={product.name} className="w-full h-full object-cover" />
                    ) : (
                      <span className="material-symbols-outlined text-slate-400 text-2xl">restaurant</span>
                    )}
                  </div>

                  <div className="flex-grow">
                    <h3 className="font-bold text-slate-900 dark:text-zinc-100 text-base mb-0.5">{product.name}</h3>
                    <p className="text-xs text-slate-500 dark:text-zinc-400 font-medium">{product.category} • {product.unit}</p>
                  </div>
                </div>

                <div className="mt-1">
                  <div className="flex justify-between items-end mb-2">
                    <span className="text-xs font-medium text-slate-500 dark:text-zinc-400">
                      {isExpired ? 'Expired on' : 'Expires in'}
                    </span>
                    <span className={`text-sm font-bold ${getDaysLeftClass(days).split(' ')[0]}`}>
                      {isExpired ? dateStr : `${getDaysLeftLabel(days)} (${dateStr})`}
                    </span>
                  </div>
                  
                  {/* Progress bar */}
                  <div className="w-full h-2 bg-slate-200 dark:bg-zinc-800 rounded-full overflow-hidden border border-black/5 dark:border-white/5">
                    <div
                      className={`h-full rounded-full transition-all duration-300 ${
                        isExpired
                          ? 'bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.4)]'
                          : isUrgent
                          ? 'bg-brand-primary shadow-[0_0_8px_rgba(255,140,0,0.4)]'
                          : 'bg-slate-400 dark:bg-zinc-600'
                      }`}
                      style={{ width: `${isExpired ? 100 : Math.max(5, Math.min(100, (days / 14) * 100))}%` }}
                    />
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* Slide-over / Modal Details Sheet */}
      {selectedProduct && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/60 backdrop-blur-sm p-4 transition-all">
          <div 
            className="glass-card w-full max-w-md rounded-t-[2.5rem] rounded-b-xl border border-white/50 dark:border-white/10 p-6 flex flex-col gap-6 shadow-2xl animate-in slide-in-from-bottom duration-300"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Header / Drag handle handle */}
            <div className="flex flex-col items-center gap-3">
              <div className="w-12 h-1.5 bg-slate-300 dark:bg-zinc-700 rounded-full cursor-pointer" onClick={() => setSelectedProduct(null)}></div>
              <div className="flex justify-between items-center w-full">
                <h3 className="text-xl font-bold text-slate-900 dark:text-zinc-50">Item Details</h3>
                <button 
                  onClick={() => setSelectedProduct(null)}
                  className="w-8 h-8 rounded-full bg-slate-100 dark:bg-zinc-800 flex items-center justify-center text-slate-500 hover:bg-slate-200 dark:hover:bg-zinc-700 cursor-pointer"
                >
                  <span className="material-symbols-outlined text-lg">close</span>
                </button>
              </div>
            </div>

            {/* Product Profile */}
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 rounded-xl bg-white dark:bg-zinc-800 flex items-center justify-center overflow-hidden border border-white dark:border-zinc-700 shadow-sm flex-shrink-0">
                {selectedProduct.imageUrl ? (
                  <img src={selectedProduct.imageUrl} alt={selectedProduct.name} className="w-full h-full object-cover" />
                ) : (
                  <span className="material-symbols-outlined text-slate-400 text-3xl">restaurant</span>
                )}
              </div>
              <div>
                <h4 className="text-lg font-bold text-slate-900 dark:text-zinc-50">{selectedProduct.name}</h4>
                <p className="text-sm text-slate-500 dark:text-zinc-400 font-medium">
                  {selectedProduct.category} • {selectedProduct.unit}
                </p>
              </div>
            </div>

            {/* Info Table */}
            <div className="flex flex-col gap-3 py-2 border-y border-slate-100 dark:border-zinc-800">
              <div className="flex justify-between text-sm">
                <span className="text-slate-500 dark:text-zinc-400 font-medium">Storage Location</span>
                <span className="font-bold text-slate-900 dark:text-zinc-100">{selectedProduct.storage}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-500 dark:text-zinc-400 font-medium">Expiration Date</span>
                <span className="font-bold text-slate-900 dark:text-zinc-100">
                  {new Date(selectedProduct.expiryDate).toLocaleDateString('en-US', {
                    weekday: 'long',
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                  })}
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-500 dark:text-zinc-400 font-medium">Days Left</span>
                <span className={`font-bold ${getDaysLeftClass(getDaysLeft(selectedProduct.expiryDate)).split(' ')[0]}`}>
                  {getDaysLeftLabel(getDaysLeft(selectedProduct.expiryDate))}
                </span>
              </div>
            </div>

            {/* Actions Grid */}
            <div className="grid grid-cols-3 gap-2">
              <button
                onClick={() => handleAction(selectedProduct.id, 'consumed')}
                className="flex flex-col items-center justify-center py-3 px-1.5 bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 rounded-xl gap-1 text-[11px] font-bold transition-all active:scale-95 cursor-pointer"
              >
                <span className="material-symbols-outlined">check_circle</span>
                Consumed
              </button>
              <button
                onClick={() => handleAction(selectedProduct.id, 'wasted')}
                className="flex flex-col items-center justify-center py-3 px-1.5 bg-red-500/10 hover:bg-red-500/20 text-red-600 dark:text-red-400 border border-red-500/20 rounded-xl gap-1 text-[11px] font-bold transition-all active:scale-95 cursor-pointer"
              >
                <span className="material-symbols-outlined">delete_forever</span>
                Wasted
              </button>
              <button
                onClick={() => handleAction(selectedProduct.id, 'donated')}
                className="flex flex-col items-center justify-center py-3 px-1.5 bg-blue-500/10 hover:bg-blue-500/20 text-blue-600 dark:text-blue-400 border border-blue-500/20 rounded-xl gap-1 text-[11px] font-bold transition-all active:scale-95 cursor-pointer"
              >
                <span className="material-symbols-outlined">volunteer_activism</span>
                Donated
              </button>
            </div>

            {/* Delete button */}
            <button
              onClick={() => handleDelete(selectedProduct.id)}
              className="w-full py-3.5 bg-red-500/10 hover:bg-red-500 text-red-600 hover:text-white font-bold rounded-xl border border-red-500/20 transition-all flex items-center justify-center gap-2 active:scale-98 text-sm cursor-pointer"
            >
              <span className="material-symbols-outlined text-lg">delete</span>
              Delete Item
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
