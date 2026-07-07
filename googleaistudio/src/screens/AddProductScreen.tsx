/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { StorageMethod, Product } from '../types';
import { motion } from 'motion/react';
import { Plus, Archive, Refrigerator, Snowflake, Calendar } from 'lucide-react';

interface AddProductScreenProps {
  onAdd: (product: Product) => void;
}

export default function AddProductScreen({ onAdd }: AddProductScreenProps) {
  const [name, setName] = useState('');
  const [storage, setStorage] = useState<StorageMethod>(StorageMethod.ROOM_TEMP);
  const [expiryDate, setExpiryDate] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !expiryDate) return;

    const newProduct: Product = {
      id: Math.random().toString(36).substr(2, 9),
      name,
      category: 'General',
      unit: '1 pcs',
      storage,
      expiryDate: new Date(expiryDate).toISOString(),
      createdAt: new Date().toISOString(),
    };

    onAdd(newProduct);
  };

  return (
    <div className="flex flex-col gap-8 pb-24">
      <div className="flex flex-col gap-1">
        <h2 className="text-3xl font-bold tracking-tight text-slate-900">Add Product</h2>
        <p className="text-slate-500 text-sm">Enter details to track expiration.</p>
      </div>

      <form onSubmit={handleSubmit} className="bg-white border border-slate-200 rounded-3xl p-6 shadow-sm flex flex-col gap-6">
        <div className="flex flex-col gap-2">
          <label className="text-xs font-bold text-slate-900 uppercase tracking-wider pl-1">Product Name</label>
          <input 
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g., Organic Milk"
            className="w-full bg-slate-50 border border-slate-100 focus:border-brand-primary focus:ring-1 focus:ring-brand-primary rounded-xl py-4 px-4 text-lg placeholder:text-slate-400 transition-all outline-none"
          />
        </div>

        <div className="flex flex-col gap-2">
          <label className="text-xs font-bold text-slate-900 uppercase tracking-wider pl-1">Storage Method</label>
          <div className="grid grid-cols-3 gap-3 bg-slate-50 p-2 rounded-2xl border border-slate-100">
            <StorageOption 
              selected={storage === StorageMethod.ROOM_TEMP}
              onClick={() => setStorage(StorageMethod.ROOM_TEMP)}
              icon={<Archive className="mb-1" size={24} />}
              label="Room Temp"
            />
            <StorageOption 
              selected={storage === StorageMethod.REFRIGERATOR}
              onClick={() => setStorage(StorageMethod.REFRIGERATOR)}
              icon={<Refrigerator className="mb-1" size={24} />}
              label="Refrigerator"
            />
            <StorageOption 
              selected={storage === StorageMethod.FREEZE}
              onClick={() => setStorage(StorageMethod.FREEZE)}
              icon={<Snowflake className="mb-1" size={24} />}
              label="Freeze"
            />
          </div>
        </div>

        <div className="flex flex-col gap-2">
          <label className="text-xs font-bold text-slate-900 uppercase tracking-wider pl-1">Expiry Date</label>
          <div className="relative">
            <Calendar className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" size={22} />
            <input 
              type="date"
              value={expiryDate}
              onChange={(e) => setExpiryDate(e.target.value)}
              className="w-full bg-slate-50 border border-slate-100 focus:border-brand-primary focus:ring-1 focus:ring-brand-primary rounded-xl py-4 pl-12 pr-4 text-lg transition-all outline-none"
            />
          </div>
        </div>
      </form>

      <motion.button
        whileTap={{ scale: 0.98 }}
        whileHover={{ translateY: -2 }}
        onClick={handleSubmit}
        className="w-full bg-brand-primary text-white font-bold py-4 rounded-full shadow-[0_4px_20px_rgba(255,140,0,0.3)] flex justify-center items-center gap-2 text-lg uppercase transition-all"
      >
        <Plus size={24} />
        Add Product
      </motion.button>
    </div>
  );
}

function StorageOption({ selected, onClick, icon, label }: { selected: boolean, onClick: () => void, icon: any, label: string }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex flex-col items-center justify-center p-4 rounded-xl transition-all border ${
        selected 
          ? 'bg-white border-brand-primary/30 shadow-sm text-brand-primary' 
          : 'text-slate-400 border-transparent hover:bg-white/50'
      }`}
    >
      <div className={`${selected ? 'scale-110' : ''} transition-transform`}>
        {icon}
      </div>
      <span className="text-[10px] font-bold uppercase tracking-wider mt-1">{label}</span>
    </button>
  );
}
