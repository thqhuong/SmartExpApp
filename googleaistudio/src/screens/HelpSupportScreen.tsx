/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from 'react';
import { AppView } from '../types';

interface HelpSupportScreenProps {
  onNavigate: (view: AppView) => void;
}

export default function HelpSupportScreen({ onNavigate }: HelpSupportScreenProps) {
  const [openFaq, setOpenFaq] = useState<number | null>(null);
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const toggleFaq = (idx: number) => {
    setOpenFaq(openFaq === idx ? null : idx);
  };

  const handleContact = (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitted(true);
    setSubject('');
    setMessage('');
    setTimeout(() => setSubmitted(false), 4000);
  };

  const faqs = [
    {
      q: 'How does SmartExp track expiration dates?',
      a: 'SmartExp saves your items with their expiration dates and categorizes them automatically. It groups items into Expired (red), Expiring Soon (orange), and Safe (gray) categories, triggering notifications before items go bad.',
    },
    {
      q: 'What is the AI Agent and how does it help?',
      a: 'The AI Agent (powered by Gemini) acts as your kitchen assistant. You can ask it for recipes that use up expiring ingredients in your fridge, inquire about optimal food storage tips, or get answers to custom culinary questions.',
    },
    {
      q: 'How does OCR/Expiry Scanning work?',
      a: 'When adding a product, tap the camera icon to scan. The app runs text recognition to identify expiration date patterns on packaging, allowing you to quickly confirm and log items without typing.',
    },
    {
      q: 'Can I sync my inventory across multiple devices?',
      a: 'Currently, SmartExp is local-first to guarantee lightning-fast performance and privacy. Cloud backup and multi-device synchronization will be introduced in a future update.',
    },
  ];

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
          <h2 className="text-2xl font-bold text-slate-900 dark:text-zinc-50">Help & Support</h2>
          <p className="text-sm text-slate-500 dark:text-zinc-400">Find answers or contact our support team</p>
        </div>
      </div>

      {/* FAQs Section */}
      <section className="flex flex-col gap-3">
        <h3 className="text-lg font-bold text-slate-900 dark:text-zinc-100 px-1">Frequently Asked Questions</h3>
        <div className="glass-card rounded-3xl overflow-hidden border border-white/50 dark:border-white/10 p-4 flex flex-col gap-2">
          {faqs.map((faq, idx) => {
            const isOpen = openFaq === idx;
            return (
              <div 
                key={idx} 
                className="border-b border-slate-100 dark:border-zinc-800 last:border-0 pb-2 last:pb-0"
              >
                <button
                  onClick={() => toggleFaq(idx)}
                  className="w-full flex items-center justify-between text-left py-3 px-2 font-semibold text-slate-800 dark:text-zinc-100 hover:text-brand-primary transition-colors select-none text-sm cursor-pointer"
                >
                  <span>{faq.q}</span>
                  <span className="material-symbols-outlined text-slate-400">
                    {isOpen ? 'expand_less' : 'expand_more'}
                  </span>
                </button>
                
                {isOpen && (
                  <div className="px-2 pb-3 text-xs text-slate-500 dark:text-zinc-400 leading-relaxed">
                    {faq.a}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </section>

      {/* Contact Form */}
      <section className="flex flex-col gap-3">
        <h3 className="text-lg font-bold text-slate-900 dark:text-zinc-100 px-1">Contact Support</h3>
        <form 
          onSubmit={handleContact} 
          className="glass-card rounded-3xl p-6 border border-white/50 dark:border-white/10 flex flex-col gap-4"
        >
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-zinc-400 mb-1.5">
              Subject
            </label>
            <input 
              type="text" 
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              className="w-full px-4 py-3 bg-white/50 dark:bg-black/20 border border-slate-200 dark:border-zinc-800 rounded-xl text-slate-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all text-sm font-semibold"
              placeholder="How can we help you?"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-zinc-400 mb-1.5">
              Message
            </label>
            <textarea 
              rows={4}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              className="w-full px-4 py-3 bg-white/50 dark:bg-black/20 border border-slate-200 dark:border-zinc-800 rounded-xl text-slate-900 dark:text-zinc-100 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all text-sm font-semibold resize-none"
              placeholder="Describe your issue in detail..."
              required
            />
          </div>

          <div className="flex items-center justify-between mt-2">
            <button 
              type="submit"
              className="px-6 py-3 bg-brand-primary hover:bg-brand-accent text-white font-bold rounded-xl shadow-lg shadow-brand-primary/20 active:scale-98 transition-all text-sm cursor-pointer"
            >
              Send Message
            </button>

            {submitted && (
              <span className="text-xs font-bold text-emerald-500 bg-emerald-500/10 px-3 py-1.5 rounded-lg border border-emerald-500/20 flex items-center gap-1">
                <span className="material-symbols-outlined text-sm">check_circle</span>
                Support message sent successfully!
              </span>
            )}
          </div>
        </form>
      </section>
    </div>
  );
}
