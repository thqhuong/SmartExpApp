/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { Product, StorageMethod } from './types';

// Helper to get date offsets easily
declare global {
  interface DateConstructor {
    withDateOffset(days: number): number;
  }
}
Date.withDateOffset = (days: number) => Date.now() + days * 24 * 60 * 60 * 1000;

export const INITIAL_PRODUCTS: Product[] = [
  {
    id: '1',
    name: 'Fresh Milk',
    category: 'Dairy',
    unit: '1 Gal',
    storage: StorageMethod.REFRIGERATOR,
    expiryDate: new Date(Date.now() + 1 * 24 * 60 * 60 * 1000).toISOString(), // Tomorrow
    createdAt: new Date().toISOString(),
    imageUrl: 'https://images.unsplash.com/photo-1550583724-125581f77833?auto=format&fit=crop&q=80&w=200',
  },
  {
    id: '2',
    name: 'Whole Wheat Bread',
    category: 'Pantry',
    unit: '1 Loaf',
    storage: StorageMethod.ROOM_TEMP,
    expiryDate: new Date(Date.withDateOffset(5)).toISOString(),
    createdAt: new Date().toISOString(),
    imageUrl: 'https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&q=80&w=200',
  },
  {
    id: '3',
    name: 'Baby Spinach',
    category: 'Produce',
    unit: '200g',
    storage: StorageMethod.REFRIGERATOR,
    expiryDate: new Date(Date.withDateOffset(2)).toISOString(),
    createdAt: new Date().toISOString(),
    imageUrl: 'https://images.unsplash.com/photo-1576045057995-568f588f82fb?auto=format&fit=crop&q=80&w=200',
  },
  {
    id: '4',
    name: 'Frozen Peas',
    category: 'Vegetables',
    unit: '1 Bag',
    storage: StorageMethod.FREEZE,
    expiryDate: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString(),
    createdAt: new Date().toISOString(),
    imageUrl: 'https://images.unsplash.com/photo-1590779033100-9f60705a2f3b?auto=format&fit=crop&q=80&w=200',
  },
  {
    id: '5',
    name: 'Greek Yogurt',
    category: 'Dairy',
    unit: '500g',
    storage: StorageMethod.REFRIGERATOR,
    expiryDate: new Date(Date.withDateOffset(10)).toISOString(),
    createdAt: new Date().toISOString(),
    imageUrl: 'https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&q=80&w=200',
  }
];
