/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState, useEffect } from 'react';
import { AppView } from '../types';

interface NotificationSettingsScreenProps {
  onNavigate: (view: AppView) => void;
}

export default function NotificationSettingsScreen({ onNavigate }: NotificationSettingsScreenProps) {
  const [settings, setSettings] = useState(() => {
    const saved = localStorage.getItem('smartexp_notification_settings');
    return saved ? JSON.parse(saved) : {
      expiryAlerts: true,
      recipeSuggestions: true,
      weeklyDigest: false,
      pushNotifications: true,
      emailNotifications: false,
      smsReminders: false,
    };
  });

  useEffect(() => {
    localStorage.setItem('smartexp_notification_settings', JSON.stringify(settings));
  }, [settings]);

  const handleToggle = (key: keyof typeof settings) => {
    setSettings((prev: any) => ({
      ...prev,
      [key]: !prev[key],
    }));
  };

  const options = [
    {
      key: 'expiryAlerts',
      title: 'Expiry Alerts',
      description: 'Immediate alerts for expiring items (tomorrow and in 2 days)',
      icon: 'notifications_active',
      color: 'text-brand-primary bg-brand-primary/10 border-brand-primary/20',
    },
    {
      key: 'recipeSuggestions',
      title: 'Recipe Recommendations',
      description: 'Suggestions by AI based on items expiring soon',
      icon: 'auto_awesome',
      color: 'text-purple-500 bg-purple-500/10 border-purple-500/20',
    },
    {
      key: 'weeklyDigest',
      title: 'Weekly Savings Digest',
      description: 'Weekly breakdown of waste prevented and money saved',
      icon: 'leaderboard',
      color: 'text-blue-500 bg-blue-500/10 border-blue-500/20',
    },
    {
      key: 'pushNotifications',
      title: 'Push Notifications',
      description: 'Receive real-time alerts on this device',
      icon: 'phonelink_ring',
      color: 'text-indigo-500 bg-indigo-500/10 border-indigo-500/20',
    },
    {
      key: 'emailNotifications',
      title: 'Email Notifications',
      description: 'Send weekly updates to alex.johnson@smartexp.io',
      icon: 'mail',
      color: 'text-teal-500 bg-teal-500/10 border-teal-500/20',
    },
    {
      key: 'smsReminders',
      title: 'SMS Reminders',
      description: 'Text messages for items expiring today (standard rates apply)',
      icon: 'sms',
      color: 'text-emerald-500 bg-emerald-500/10 border-emerald-500/20',
    },
  ] as const;

  return (
    <div className="flex flex-col gap-6 pb-24">
      {/* Header with back navigation */}
      <div className="flex items-center gap-3">
        <button 
          onClick={() => onNavigate('settings')}
          className="w-10 h-10 rounded-full glass-card hover:bg-black/5 dark:hover:bg-white/5 flex items-center justify-center text-slate-800 dark:text-zinc-200 select-none cursor-pointer"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
        <div>
          <h2 className="text-2xl font-bold text-slate-900 dark:text-zinc-50">Notification Settings</h2>
          <p className="text-sm text-slate-500 dark:text-zinc-400">Configure your expiry alert alerts and digests</p>
        </div>
      </div>

      {/* Settings Panel */}
      <div className="glass-card rounded-3xl overflow-hidden border border-white/50 dark:border-white/10 p-6 flex flex-col gap-5">
        {options.map((opt) => (
          <div key={opt.key} className="flex items-center justify-between pb-4 last:pb-0 last:border-0 border-b border-slate-100 dark:border-zinc-800">
            <div className="flex items-start gap-4 flex-1 pr-4">
              <div className={`p-2.5 rounded-xl border flex items-center justify-center flex-shrink-0 ${opt.color}`}>
                <span className="material-symbols-outlined text-xl">{opt.icon}</span>
              </div>
              <div>
                <h4 className="font-bold text-slate-900 dark:text-zinc-100">{opt.title}</h4>
                <p className="text-xs text-slate-500 dark:text-zinc-400 mt-0.5">{opt.description}</p>
              </div>
            </div>

            <label className="relative inline-flex items-center cursor-pointer select-none">
              <input 
                type="checkbox"
                checked={settings[opt.key]}
                onChange={() => handleToggle(opt.key)}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-200 dark:bg-zinc-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 dark:after:border-zinc-700 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-brand-primary"></div>
            </label>
          </div>
        ))}
      </div>
    </div>
  );
}
