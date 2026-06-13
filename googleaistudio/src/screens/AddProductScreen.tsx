/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState, type FormEvent } from 'react';
import { Product, StorageMethod } from '../types';

interface AddProductScreenProps {
  onAdd: (product: Product) => void;
}

export default function AddProductScreen({ onAdd }: AddProductScreenProps) {
  const [name, setName] = useState('');
  const [category, setCategory] = useState('Dairy');
  const [unit, setUnit] = useState('1 Unit');
  const [storage, setStorage] = useState<StorageMethod>(StorageMethod.REFRIGERATOR);
  
  // Default expiry date: tomorrow
  const getTomorrowDateString = () => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return tomorrow.toISOString().split('T')[0];
  };
  const [expiryDate, setExpiryDate] = useState(getTomorrowDateString());

  // OCR scanner state
  const [scanning, setScanning] = useState(false);
  const [scanSuccess, setScanSuccess] = useState(false);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    const newProduct: Product = {
      id: Math.random().toString(36).substring(2, 9),
      name: name.trim(),
      category,
      unit,
      storage,
      expiryDate: new Date(expiryDate).toISOString(),
      createdAt: new Date().toISOString(),
    };

    onAdd(newProduct);
  };

  const handleScan = () => {
    setScanning(true);
    setScanSuccess(false);

    // Simulate scanning delay
    setTimeout(() => {
      setScanning(false);
      setScanSuccess(true);
      const offset = Math.floor(Math.random() * 5) + 2; // 2 to 6 days
      const targetDate = new Date();
      targetDate.setDate(targetDate.getDate() + offset);
      setExpiryDate(targetDate.toISOString().split('T')[0]);
      // Fill name if empty
      if (!name) setName('Scanned Product');
    }, 2000);
  };

  return (
    <div className="flex flex-col gap-6 pb-24">
      <div className="flex flex-col gap-1">
        <h2 className="text-3xl font-bold tracking-tight text-slate-900 dark:text-zinc-50">Add New Product</h2>
        <p className="text-sm text-slate-500 dark:text-zinc-400">Log new items manually or scan an expiry date</p>
      </div>

      {/* Quick Scan Tool */}
      <div className="grid grid-cols-1 gap-4">
        <button
          onClick={handleScan}
          className="glass-card rounded-2xl py-4 px-3 flex flex-col items-center justify-center gap-2 border border-white/50 dark:border-white/10 hover:border-brand-primary/40 dark:hover:border-brand-primary/40 transition-all select-none cursor-pointer active:scale-95 group"
        >
          <span className="material-symbols-outlined text-3xl text-brand-primary group-hover:scale-105 transition-transform">
            photo_camera
          </span>
          <span className="text-xs font-bold text-slate-700 dark:text-zinc-300">Scan Expiry Date</span>
        </button>
      </div>

      {/* Scanner Status Overlay */}
      {scanning && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
          <div className="glass-card p-6 rounded-3xl w-80 flex flex-col items-center gap-4 text-center border border-white/40">
            <div className="w-16 h-16 rounded-full border-4 border-brand-primary border-t-transparent animate-spin flex items-center justify-center"></div>
            <h3 className="text-lg font-bold text-slate-900 dark:text-zinc-100">
              Analyzing Expiry Date...
            </h3>
            <p className="text-xs text-slate-500">Hold steady while processing packing labels</p>
          </div>
        </div>
      )}

      {/* Main Form */}
      <form onSubmit={handleSubmit} className="glass-card rounded-3xl p-6 border border-white/50 dark:border-white/10 shadow-lg flex flex-col gap-4">
        {scanSuccess && (
          <div className="flex items-center gap-2 text-xs font-bold text-emerald-500 bg-emerald-500/10 px-4 py-2.5 rounded-xl border border-emerald-500/20 select-none">
            <span className="material-symbols-outlined text-sm">check_circle</span>
            OCR date detected successfully!
          </div>
        )}

        <div>
          <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-zinc-400 mb-1.5">
            Product Name
          </label>
          <input
            type="text"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full px-4 py-3 bg-white/50 dark:bg-black/20 border border-slate-200 dark:border-zinc-800 rounded-xl text-slate-900 dark:text-zinc-100 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all text-sm font-semibold"
            placeholder="e.g., Organic Whole Milk"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-zinc-400 mb-1.5">
              Category
            </label>
            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              className="w-full px-4 py-3 bg-white/50 dark:bg-black/20 border border-slate-200 dark:border-zinc-800 rounded-xl text-slate-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all text-sm font-semibold appearance-none"
            >
              <option value="Dairy">Dairy</option>
              <option value="Pantry">Pantry</option>
              <option value="Produce">Produce</option>
              <option value="Vegetables">Vegetables</option>
              <option value="Meat">Meat</option>
              <option value="Bakery">Bakery</option>
              <option value="Beverages">Beverages</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-zinc-400 mb-1.5">
              Quantity / Unit
            </label>
            <input
              type="text"
              required
              value={unit}
              onChange={(e) => setUnit(e.target.value)}
              className="w-full px-4 py-3 bg-white/50 dark:bg-black/20 border border-slate-200 dark:border-zinc-800 rounded-xl text-slate-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all text-sm font-semibold"
              placeholder="e.g., 1 Gal, 500g"
            />
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-zinc-400 mb-1.5">
            Storage Location
          </label>
          <div className="grid grid-cols-3 gap-2">
            {[
              { type: StorageMethod.REFRIGERATOR, label: 'Refrigerator', icon: 'kitchen' },
              { type: StorageMethod.ROOM_TEMP, label: 'Pantry', icon: 'shelves' },
              { type: StorageMethod.FREEZE, label: 'Freezer', icon: 'ac_unit' },
            ].map((opt) => {
              const isSelected = storage === opt.type;
              return (
                <button
                  type="button"
                  key={opt.type}
                  onClick={() => setStorage(opt.type)}
                  className={`py-3 px-1.5 rounded-xl border flex flex-col items-center justify-center gap-1.5 text-xs font-bold transition-all select-none cursor-pointer ${
                    isSelected
                      ? 'bg-brand-primary border-transparent text-white shadow-md shadow-brand-primary/20'
                      : 'bg-white/50 dark:bg-zinc-900/40 border-slate-200 dark:border-zinc-800 text-slate-600 dark:text-zinc-300 hover:bg-slate-100 dark:hover:bg-zinc-900/60'
                  }`}
                >
                  <span className="material-symbols-outlined text-lg">{opt.icon}</span>
                  <span>{opt.label}</span>
                </button>
              );
            })}
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-zinc-400 mb-1.5">
            Expiry Date
          </label>
          <input
            type="date"
            required
            value={expiryDate}
            onChange={(e) => setExpiryDate(e.target.value)}
            className="w-full px-4 py-3 bg-white/50 dark:bg-black/20 border border-slate-200 dark:border-zinc-800 rounded-xl text-slate-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all text-sm font-semibold"
          />
        </div>

        <button
          type="submit"
          className="w-full py-3.5 bg-brand-primary hover:bg-brand-accent text-white font-bold rounded-xl shadow-lg shadow-brand-primary/20 active:scale-98 transition-all text-sm mt-3 cursor-pointer"
        >
          Add Product to Inventory
        </button>
      </form>
    </div>
  );
}
