/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import TopBar from './components/TopBar';
import BottomNav from './components/BottomNav';
import InventoryScreen from './screens/InventoryScreen';
import StatsScreen from './screens/StatsScreen';
import AddProductScreen from './screens/AddProductScreen';
import RecipesScreen from './screens/RecipesScreen';
import SettingsScreen from './screens/SettingsScreen';
import { Product, AppView } from './types';
import { INITIAL_PRODUCTS } from './constants';

export default function App() {
  const [products, setProducts] = useState<Product[]>(INITIAL_PRODUCTS);
  const [activeView, setActiveView] = useState<AppView>('inventory');

  const addProduct = (product: Product) => {
    setProducts([product, ...products]);
    setActiveView('inventory');
  };

  const renderView = () => {
    switch (activeView) {
      case 'inventory': return <InventoryScreen products={products} />;
      case 'stats': return <StatsScreen products={products} />;
      case 'add': return <AddProductScreen onAdd={addProduct} />;
      case 'recipes': return <RecipesScreen products={products} />;
      case 'settings': return <SettingsScreen />;
      default: return <InventoryScreen products={products} />;
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-surface-low">
      <TopBar />
      
      <main className="flex-1 max-w-2xl mx-auto w-full px-5 py-6 overflow-x-hidden">
        <AnimatePresence mode="wait">
          <motion.div
            key={activeView}
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            transition={{ duration: 0.3, ease: 'easeOut' }}
          >
            {renderView()}
          </motion.div>
        </AnimatePresence>
      </main>

      <BottomNav activeView={activeView} onViewChange={setActiveView} />
      
      {/* Spacer for bottom nav */}
      <div className="h-20" />
    </div>
  );
}
