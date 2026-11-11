import React, { useId } from 'react';
import { motion } from 'motion/react';
import { Wallet } from 'lucide-react';
import './WalletFilter.css';

const WalletFilter = ({ wallets = [], selectedWalletId, selected, onChange }) => {
  const layoutGroupId = useId();
  const rawSelected = selectedWalletId !== undefined ? selectedWalletId : selected;
  const activeId = rawSelected ?? 'all';

  return (
    <div className="wallet-filter" role="group" aria-label="Filter by wallet">
      <button
        type="button"
        className={`wallet-pill ${activeId === 'all' ? 'active' : ''}`}
        aria-pressed={activeId === 'all'}
        onClick={() => onChange(null)}
      >
        {activeId === 'all' && (
          <motion.div
            layoutId={`wallet-pill-bg-${layoutGroupId}`}
            className="wallet-pill-bg"
            transition={{ type: 'spring', stiffness: 440, damping: 34 }}
          />
        )}
        <span className="wallet-pill-content">
          <Wallet size={14} strokeWidth={2.2} aria-hidden="true" />
          <span>All Wallets</span>
        </span>
      </button>

      {wallets.map((wallet) => {
        const isActive = activeId === wallet.id;
        const color = wallet.color || 'var(--accent-emerald, #28C991)';

        return (
          <button
            key={wallet.id}
            type="button"
            className={`wallet-pill ${isActive ? 'active' : ''}`}
            aria-pressed={isActive}
            onClick={() => onChange(wallet.id)}
          >
            {isActive && (
              <motion.div
                layoutId={`wallet-pill-bg-${layoutGroupId}`}
                className="wallet-pill-bg"
                transition={{ type: 'spring', stiffness: 440, damping: 34 }}
              />
            )}
            <span className="wallet-pill-content">
              <span
                className="wallet-pill-dot"
                style={{ backgroundColor: color }}
                aria-hidden="true"
              />
              <span>{wallet.name}</span>
            </span>
          </button>
        );
      })}
    </div>
  );
};

export default WalletFilter;
