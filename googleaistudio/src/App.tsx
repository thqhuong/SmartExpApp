/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import TopBar from './components/TopBar';
import BottomNav from './components/BottomNav';
import InventoryScreen from './screens/InventoryScreen';
import StatsScreen from './screens/StatsScreen';
import AddProductScreen from './screens/AddProductScreen';
import RecipesScreen from './screens/RecipesScreen';
import SettingsScreen from './screens/SettingsScreen';
import SignInScreen from './screens/SignInScreen';
import HelpSupportScreen from './screens/HelpSupportScreen';
import NotificationSettingsScreen from './screens/NotificationSettingsScreen';
import AccountDetailsScreen from './screens/AccountDetailsScreen';
import { Product, AppView } from './types';
import { INITIAL_PRODUCTS } from './constants';

export default function App() {
  // Theme state
  const [darkMode, setDarkMode] = useState<boolean>(() => {
    const saved = localStorage.getItem('smartexp_dark_mode');
    return saved === 'true';
  });

  // Auth state
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(() => {
    const saved = localStorage.getItem('smartexp_logged_in');
    return saved === 'true';
  });

  // Products state
  const [products, setProducts] = useState<Product[]>(() => {
    const saved = localStorage.getItem('smartexp_products');
    return saved ? JSON.parse(saved) : INITIAL_PRODUCTS;
  });

  // Active view state
  const [activeView, setActiveView] = useState<AppView>(() => {
    if (localStorage.getItem('smartexp_logged_in') !== 'true') return 'signin';
    const saved = localStorage.getItem('smartexp_active_view');
    return (saved as AppView) || 'stats'; // Default to Dashboard (stats)
  });

  // Sync products to local storage
  useEffect(() => {
    localStorage.setItem('smartexp_products', JSON.stringify(products));
  }, [products]);

  // Sync theme to document class and local storage
  useEffect(() => {
    localStorage.setItem('smartexp_dark_mode', String(darkMode));
    if (darkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [darkMode]);

  // Sync active view to local storage
  useEffect(() => {
    localStorage.setItem('smartexp_active_view', activeView);
  }, [activeView]);

  const handleToggleDarkMode = () => {
    setDarkMode(!darkMode);
  };

  const handleSignIn = () => {
    setIsLoggedIn(true);
    localStorage.setItem('smartexp_logged_in', 'true');
    setActiveView('stats'); // Navigate to Dashboard after Sign In
  };

  const handleSignOut = () => {
    setIsLoggedIn(false);
    localStorage.setItem('smartexp_logged_in', 'false');
    setActiveView('signin');
  };

  const addProduct = (product: Product) => {
    setProducts([product, ...products]);
    setActiveView('inventory');
  };

  const renderView = () => {
    if (!isLoggedIn) {
      return <SignInScreen onSignIn={handleSignIn} />;
    }

    switch (activeView) {
      case 'inventory': 
        return <InventoryScreen products={products} onUpdateProducts={setProducts} />;
      case 'stats': 
        return <StatsScreen products={products} onNavigate={setActiveView} />;
      case 'add': 
        return <AddProductScreen onAdd={addProduct} />;
      case 'recipes': 
        return <RecipesScreen products={products} />;
      case 'settings': 
        return (
          <SettingsScreen 
            onNavigate={setActiveView} 
            onSignOut={handleSignOut}
            darkMode={darkMode}
            onToggleDarkMode={handleToggleDarkMode}
          />
        );
      case 'help-support':
        return <HelpSupportScreen onNavigate={setActiveView} />;
      case 'notification-settings':
        return <NotificationSettingsScreen onNavigate={setActiveView} />;
      case 'account-details':
        return <AccountDetailsScreen onNavigate={setActiveView} />;
      case 'signin':
        return <SignInScreen onSignIn={handleSignIn} />;
      default: 
        return <StatsScreen products={products} onNavigate={setActiveView} />;
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-zinc-950 text-slate-900 dark:text-zinc-150 transition-colors duration-300 relative">
      
      {/* Decorative Orbs in background */}
      <div className="blob blob-1"></div>
      <div className="blob blob-2"></div>
      <div className="blob blob-3"></div>

      {/* Conditionally render Gemini glow for Agent view */}
      {activeView === 'recipes' && (
        <>
          <div className="gemini-glow glow-1"></div>
          <div className="gemini-glow glow-2"></div>
          <div className="gemini-glow glow-3"></div>
        </>
      )}

      <TopBar 
        darkMode={darkMode} 
        onToggleDarkMode={handleToggleDarkMode} 
        onNavigate={setActiveView}
        isLoggedIn={isLoggedIn}
      />
      
      <main className="flex-1 max-w-2xl mx-auto w-full px-5 pt-20 pb-6 overflow-x-hidden relative z-10">
        <AnimatePresence mode="wait">
          <motion.div
            key={activeView}
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -15 }}
            transition={{ duration: 0.25, ease: 'easeOut' }}
          >
            {renderView()}
          </motion.div>
        </AnimatePresence>
      </main>

      <BottomNav activeView={activeView} onViewChange={setActiveView} />
      
      {/* Spacer for bottom nav */}
      {isLoggedIn && <div className="h-20" />}
    </div>
  );
}
