/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export enum StorageMethod {
  ROOM_TEMP = 'Room Temp',
  REFRIGERATOR = 'Refrigerator',
  FREEZE = 'Freeze',
}

export interface Product {
  id: string;
  name: string;
  category: string;
  unit: string;
  storage: StorageMethod;
  expiryDate: string; // ISO string
  createdAt: string; // ISO string
  imageUrl?: string;
}

export interface Recipe {
  id: string;
  title: string;
  description: string;
  savingItems: string[];
  instructions?: string;
  imageUrl?: string;
}

export type AppView = 'inventory' | 'stats' | 'add' | 'recipes' | 'settings';
