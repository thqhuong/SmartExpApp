/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { Product, StorageMethod } from '../types';
import { motion } from 'motion/react';
import { Package, Thermometer, Snowflake, Refrigerator, Clock } from 'lucide-react';

interface InventoryScreenProps {
  products: Product[];
}

export default function InventoryScreen({ products }: InventoryScreenProps) {
  const getDaysLeft = (expiryDate: string) => {
    const diff = new Date(expiryDate).getTime() - Date.now();
    return Math.ceil(diff / (1000 * 60 * 60 * 24));
  };

  const getStorageIcon = (method: StorageMethod) => {
    switch (method) {
      case StorageMethod.ROOM_TEMP: return <Thermometer size={20} />;
      case StorageMethod.REFRIGERATOR: return <Refrigerator size={20} />;
      case StorageMethod.FREEZE: return <Snowflake size={20} />;
      default: return <Package size={20} />;
    }
  };

  const sortedProducts = [...products].sort((a, b) => 
    new Date(a.expiryDate).getTime() - new Date(b.expiryDate).getTime()
  );

  return (
    <div className="flex flex-col gap-6 pb-24">
      <div className="flex flex-col gap-1">
        <h2 className="text-3xl font-bold tracking-tight text-slate-900">My Inventory</h2>
      </div>

      <div className="flex space-x-2 overflow-x-auto scrollbar-hide pb-2">
        {['All', 'Room Temp', 'Cool', 'Frozen'].map((filter) => (
          <button 
            key={filter}
            className={`px-5 py-2 rounded-full whitespace-nowrap text-sm font-semibold transition-all border ${
              filter === 'All' 
                ? 'bg-slate-200 border-transparent text-slate-900' 
                : 'bg-white border-slate-300 text-slate-600 hover:bg-slate-50'
            }`}
          >
            {filter}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-4">
        {sortedProducts.map((product) => {
          const daysLeft = getDaysLeft(product.expiryDate);
          const isUrgent = daysLeft <= 3;
          
          return (
            <motion.div
              layout
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              key={product.id}
              className={`bg-white rounded-xl border ${isUrgent ? 'border-brand-primary' : 'border-slate-200'} p-4 flex flex-col gap-4 shadow-sm relative overflow-hidden`}
            >
              {isUrgent && (
                <div className="absolute top-0 right-0 bg-brand-primary text-white text-[10px] font-bold px-3 py-1 rounded-bl-lg uppercase">
                  Expiring Soon
                </div>
              )}

              <div className="flex items-center gap-4 mt-2">
                <div className={`w-12 h-12 rounded-lg flex items-center justify-center ${isUrgent ? 'bg-orange-100 text-brand-primary' : 'bg-slate-100 text-slate-500'} overflow-hidden`}>
                  {product.imageUrl ? (
                    <img src={product.imageUrl} alt={product.name} className="w-full h-full object-cover" />
                  ) : (
                    getStorageIcon(product.storage)
                  )}
                </div>
                
                <div className="flex-1">
                  <h3 className="font-bold text-slate-900 text-lg mb-1">{product.name}</h3>
                  <p className="text-sm text-slate-500">{product.category} • {product.unit}</p>
                </div>
              </div>

              <div>
                <div className="flex justify-between items-end mb-1.5">
                  <span className="text-sm text-slate-500">Expires in</span>
                  <span className={`text-base font-bold ${isUrgent ? 'text-brand-primary' : 'text-slate-900'}`}>
                    {daysLeft <= 0 ? 'Expired' : `${daysLeft} ${daysLeft === 1 ? 'Day' : 'Days'}`}
                  </span>
                </div>
                <div className="w-full h-1.5 bg-slate-200 rounded-full overflow-hidden">
                  <motion.div 
                    initial={{ width: 0 }}
                    animate={{ width: `${Math.max(0, Math.min(100, (daysLeft / 14) * 100))}%` }}
                    className={`h-full rounded-full ${isUrgent ? 'bg-brand-primary' : 'bg-slate-400'}`}
                  />
                </div>
              </div>
            </motion.div>
          );
        })}
      </div>
    </div>
  );
}
