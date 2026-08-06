import React, { useState } from 'react';
import { useIncome } from '../hooks/useIncome';
import { useWallets } from '../hooks/useWallets';
import IncomeForm from '../components/IncomeForm';
import MonthSelector from '../components/MonthSelector';
import WalletFilter from '../components/WalletFilter';
import { format } from 'date-fns';
import './Income.css';

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' }).format(amount);
};

const Income = () => {
  const currentDate = new Date();
  const [selectedMonth, setSelectedMonth] = useState(currentDate.getMonth());
  const [selectedYear, setSelectedYear] = useState(currentDate.getFullYear());
  const [selectedWalletId, setSelectedWalletId] = useState(null);
  const [sortOrder, setSortOrder] = useState('date_desc');
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingIncome, setEditingIncome] = useState(null);
  
  const { income, deleteIncome, addIncome, updateIncome } = useIncome(selectedMonth, selectedYear);
  const { wallets } = useWallets();
  
  const handleEdit = (inc) => {
    setEditingIncome(inc);
    setIsFormOpen(true);
  };
  
  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this income record?')) {
      await deleteIncome(id);
    }
  };
  
  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingIncome(null);
  };
  
  let filteredIncome = income.filter(i => !selectedWalletId || i.wallet_id === selectedWalletId);
  
  filteredIncome.sort((a, b) => {
    if (sortOrder === 'date_desc') return new Date(b.income_date) - new Date(a.income_date);
    if (sortOrder === 'date_asc') return new Date(a.income_date) - new Date(b.income_date);
    if (sortOrder === 'amount_desc') return b.amount - a.amount;
    if (sortOrder === 'amount_asc') return a.amount - b.amount;
    return 0;
  });

  const totalAmount = filteredIncome.reduce((sum, i) => sum + Number(i.amount), 0);
  
  const getWalletName = (id) => {
    const wallet = wallets.find(w => w.id === id);
    return wallet ? wallet.name : 'Unknown';
  };
  
  return (
    <div className="income-container">
      <header className="page-header">
        <h1>Income</h1>
        <div className="header-actions">
          <MonthSelector 
            month={selectedMonth} 
            year={selectedYear} 
            minMonth={7}
            minYear={2026}
            onChange={(m, y) => { setSelectedMonth(m); setSelectedYear(y); }} 
          />
          <button className="add-btn" onClick={() => setIsFormOpen(true)}>
            + Add Income
          </button>
        </div>
      </header>
      
      <div className="filters-section" style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', alignItems: 'center' }}>
        <WalletFilter wallets={wallets} selected={selectedWalletId} onChange={setSelectedWalletId} />
        
        <select 
          value={sortOrder} 
          onChange={(e) => setSortOrder(e.target.value)}
          style={{ padding: '8px 12px', borderRadius: '8px', border: '1px solid var(--border-color)', background: 'var(--bg-card)', color: 'var(--text-primary)' }}
        >
          <option value="date_desc">Date (Newest First)</option>
          <option value="date_asc">Date (Oldest First)</option>
          <option value="amount_desc">Amount (Highest First)</option>
          <option value="amount_asc">Amount (Lowest First)</option>
        </select>
      </div>
      
      <div className="data-table-container">
        {filteredIncome.length === 0 ? (
          <div className="empty-state">No income found for this period.</div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Description</th>
                <th>Wallet</th>
                <th className="amount-col">Amount</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredIncome.map(inc => (
                <tr key={inc.id}>
                  <td>{format(new Date(inc.income_date), 'MMM dd, yyyy')}</td>
                  <td>{inc.description}</td>
                  <td>{getWalletName(inc.wallet_id)}</td>
                  <td className="amount-col income-amount">{formatCurrency(inc.amount)}</td>
                  <td className="actions-col">
                    <button className="action-btn edit" onClick={() => handleEdit(inc)}>✎</button>
                    <button className="action-btn delete" onClick={() => handleDelete(inc.id)}>🗑</button>
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td colSpan="3" className="total-label">Total</td>
                <td className="amount-col income-amount total-amount">{formatCurrency(totalAmount)}</td>
                <td></td>
              </tr>
            </tfoot>
          </table>
        )}
      </div>
      
      {isFormOpen && (
        <IncomeForm 
          isOpen={isFormOpen}
          income={editingIncome} 
          onClose={handleCloseForm}
          onSave={async (data) => {
            if (editingIncome) {
              await updateIncome(editingIncome.id, data);
            } else {
              await addIncome(data);
            }
            handleCloseForm();
          }}
          wallets={wallets}
        />
      )}
    </div>
  );
};

export default Income;
