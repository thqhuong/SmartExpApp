/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { Product, StorageMethod } from '../types';
import { motion } from 'motion/react';
import { CheckCircle2, Leaf, Refrigerator, Thermometer, Snowflake } from 'lucide-react';

interface StatsScreenProps {
  products: Product[];
}

export default function StatsScreen({ products }: StatsScreenProps) {
  const expiringSoon = products.filter(p => {
    const diff = new Date(p.expiryDate).getTime() - Date.now();
    return Math.ceil(diff / (1000 * 60 * 60 * 24)) <= 3;
  }).length;

  const storageCounts = products.reduce((acc, p) => {
    acc[p.storage] = (acc[p.storage] || 0) + 1;
    return acc;
  }, {} as Record<StorageMethod, number>);

  const soonestItems = [...products].sort((a, b) => 
    new Date(a.expiryDate).getTime() - new Date(b.expiryDate).getTime()
  ).slice(0, 3);

  return (
    <div className="flex flex-col gap-8 pb-24">
      <section className="grid grid-cols-2 gap-3">
        <div className="col-span-2 bg-white border border-slate-200 rounded-xl p-4 shadow-sm flex items-center justify-between">
          <div>
            <h2 className="text-lg font-bold text-slate-900 mb-1">Items Expiring Soon</h2>
            <p className="text-sm text-slate-500">Requires immediate attention</p>
          </div>
          <div className="w-14 h-14 rounded-full bg-orange-100 flex items-center justify-center border-4 border-white shadow-sm">
            <span className="text-xl text-brand-primary font-bold">{expiringSoon}</span>
          </div>
        </div>
        
        <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-sm">
          <CheckCircle2 size={24} className="text-slate-400 mb-2" />
          <p className="text-sm text-slate-500 mb-1">Total Tracked</p>
          <div className="flex items-end gap-2">
            <h3 className="text-2xl font-bold text-slate-900 leading-none">{products.length}</h3>
          </div>
        </div>
        
        <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-sm">
          <Leaf size={24} className="text-slate-400 mb-2" />
          <p className="text-sm text-slate-500 mb-1">Waste Prevented</p>
          <div className="flex items-end gap-1">
            <h3 className="text-2xl font-bold text-slate-900 leading-none">3.2</h3>
            <span className="text-sm text-slate-500 mb-0.5">kg</span>
          </div>
        </div>
      </section>

      <section>
        <h3 className="text-lg font-bold text-slate-900 mb-3">Storage Overview</h3>
        <div className="flex flex-col gap-3">
          <StorageRow 
            icon={<Refrigerator size={18} />} 
            label="Refrigerator" 
            count={storageCounts[StorageMethod.REFRIGERATOR] || 0} 
            total={products.length}
            color="#ff8c00"
          />
          <StorageRow 
            icon={<Thermometer size={18} />} 
            label="Room Temp" 
            count={storageCounts[StorageMethod.ROOM_TEMP] || 0} 
            total={products.length}
            color="#94a3b8"
          />
          <StorageRow 
            icon={<Snowflake size={18} />} 
            label="Freezer" 
            count={storageCounts[StorageMethod.FREEZE] || 0} 
            total={products.length}
            color="#94a3b8"
          />
        </div>
      </section>

      <section className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
          <div className="px-4 py-4 border-b border-slate-100 flex justify-between items-end">
            <h2 className="text-lg font-bold text-slate-900">Expiring Soonest</h2>
            <button className="text-sm font-bold text-brand-primary">View All</button>
          </div>
          <div className="flex flex-col">
          {soonestItems.map((item, idx) => (
            <div key={item.id} className={`flex items-center gap-4 p-4 border-b border-slate-100`}>
              <div className="w-12 h-12 bg-slate-100 rounded-lg flex items-center justify-center overflow-hidden">
                <img src={item.imageUrl} alt={item.name} className="w-full h-full object-cover" />
              </div>
              <div className="flex-1">
                <div className="flex justify-between items-center mb-1">
                  <div>
                    <h4 className="font-bold text-base text-slate-900">{item.name}</h4>
                    <p className="text-sm text-slate-500">{item.storage} • {item.unit}</p>
                  </div>
                  <div className="flex flex-col items-end">
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded ${idx === 0 ? 'bg-orange-100 text-brand-primary' : 'bg-slate-100 text-slate-700'}`}>
                      {idx === 0 ? 'TOMORROW' : `IN ${idx+1} DAYS`}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          ))}
          </div>
        </section>
    </div>
  );
}

function StorageRow({ icon, label, count, total, color }: { icon: any, label: string, count: number, total: number, color: string }) {
  const percentage = total > 0 ? (count / total) * 100 : 0;
  return (
    <div className="bg-white border border-slate-200 rounded-xl p-4 flex items-center gap-4">
      <div className="w-12 h-12 rounded-lg bg-slate-100 flex items-center justify-center text-slate-500">
        {icon}
      </div>
      <div className="flex-1">
        <div className="flex justify-between items-center mb-1">
          <span className="text-base font-bold text-slate-900">{label}</span>
          <span className="text-sm text-slate-500">{count} items</span>
        </div>
        <div className="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
          <motion.div 
            initial={{ width: 0 }}
            animate={{ width: `${percentage}%` }}
            className="h-full rounded-full"
            style={{ backgroundColor: color }}
          />
        </div>
      </div>
    </div>
  );
}
