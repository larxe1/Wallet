import React from 'react';
import './BalanceCard.css';

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' }).format(amount);
};

export default function BalanceCard({ walletName, walletColor = '#6366f1', totalIncome = 0, totalExpenses = 0 }) {
  const balance = totalIncome - totalExpenses;
  const isPositive = balance >= 0;

  return (
    <div className="balance-card" style={{ '--wallet-color': walletColor }}>
      <div className="bc-header">
        <h3 className="bc-title">{walletName}</h3>
      </div>
      <div className="bc-balance-section">
        <span className="bc-label">Total Balance</span>
        <h2 className={`bc-balance ${isPositive ? 'positive' : 'negative'}`}>
          {formatCurrency(balance)}
        </h2>
      </div>
      <div className="bc-details">
        <div className="bc-stat">
          <span className="bc-stat-label">Income</span>
          <span className="bc-stat-value positive">{formatCurrency(totalIncome)}</span>
        </div>
        <div className="bc-stat">
          <span className="bc-stat-label">Expenses</span>
          <span className="bc-stat-value negative">{formatCurrency(totalExpenses)}</span>
        </div>
      </div>
    </div>
  );
}
