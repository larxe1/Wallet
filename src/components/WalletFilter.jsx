import React from 'react';
import './WalletFilter.css';

const WalletFilter = ({ wallets, selectedWalletId, onChange }) => {
  return (
    <div className="wallet-filter">
      <button
        className={`wallet-pill ${selectedWalletId === null ? 'active' : ''}`}
        onClick={() => onChange(null)}
        style={selectedWalletId === null ? { backgroundColor: 'var(--accent-primary)', borderColor: 'var(--accent-primary)' } : {}}
      >
        All Wallets
      </button>

      {wallets.map(wallet => {
        const isActive = selectedWalletId === wallet.id;
        const color = wallet.color || 'var(--accent-primary)';
        
        return (
          <button
            key={wallet.id}
            className={`wallet-pill ${isActive ? 'active' : ''}`}
            onClick={() => onChange(wallet.id)}
            style={{
              borderColor: color,
              backgroundColor: isActive ? color : 'transparent',
              color: isActive ? 'white' : 'var(--text-primary)'
            }}
          >
            {wallet.name}
          </button>
        );
      })}
    </div>
  );
};

export default WalletFilter;
