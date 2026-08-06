import React, { useState } from 'react';
import { useRecurring } from '../hooks/useRecurring';
import { useWallets } from '../hooks/useWallets';
import { useCategories } from '../hooks/useCategories';
import { usePaymentMethods } from '../hooks/usePaymentMethods';
import RecurringForm from '../components/RecurringForm';
import './Recurring.css';

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' }).format(amount);
};

const Recurring = () => {
  const { recurring, addRecurring, updateRecurring, deleteRecurring, toggleRecurringStatus } = useRecurring();
  const { wallets } = useWallets();
  const { categories } = useCategories();
  const { paymentMethods } = usePaymentMethods();
  
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  
  const handleEdit = (item) => {
    setEditingItem(item);
    setIsFormOpen(true);
  };
  
  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this recurring expense?')) {
      await deleteRecurring(id);
    }
  };
  
  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingItem(null);
  };
  
  const getCategoryName = (id) => {
    const cat = categories.find(c => c.id === id);
    return cat ? `${cat.icon} ${cat.name}` : 'Unknown';
  };
  
  const getWalletName = (id) => {
    const wallet = wallets.find(w => w.id === id);
    return wallet ? wallet.name : 'Unknown';
  };

  return (
    <div className="recurring-container">
      <header className="page-header">
        <h1>Recurring Transactions</h1>
        <button className="add-btn" onClick={() => setIsFormOpen(true)}>
          + Add Recurring
        </button>
      </header>
      
      <div className="recurring-list">
        {recurring.length === 0 ? (
          <div className="empty-state">No recurring expenses set up.</div>
        ) : (
          recurring.map(item => (
            <div key={item.id} className={`recurring-card ${!item.is_active ? 'paused' : ''}`}>
              <div className="rc-header">
                <div className="item-main">
                  <h3>{item.description || item.categories?.name || 'Transaction'}</h3>
                  <div className="item-meta">
                    <span className="wallet-badge" style={{ color: item.wallets?.color || 'var(--text-secondary)' }}>
                      {item.wallets?.name || 'No Wallet'}
                    </span>
                    <span className={`type-badge ${item.type === 'income' ? 'income' : 'expense'}`}>
                      {item.type === 'income' ? 'Income' : 'Expense'}
                    </span>
                    {item.categories && (
                      <span className="category-badge">
                        {item.categories.icon} {item.categories.name}
                      </span>
                    )}
                  </div>
                </div>
                <div className="item-right">
                  <div className="item-amount" style={{ color: item.type === 'income' ? 'var(--accent-success)' : 'var(--accent-danger)' }}>
                    {item.type === 'income' ? '+' : '-'}{formatCurrency(item.amount)}
                  </div>
                </div>
              </div>
              
              <div className="rc-schedule">
                <p>Posts on the <strong>{item.day_of_month}{
                  item.day_of_month === 1 ? 'st' : 
                  item.day_of_month === 2 ? 'nd' : 
                  item.day_of_month === 3 ? 'rd' : 'th'
                }</strong> of every month</p>
                <p className="last-posted">Last posted: {item.last_posted_date ? item.last_posted_date : 'Never'}</p>
              </div>
              
              <div className="rc-actions">
                <div className="status-toggle">
                  <label className="switch">
                    <input 
                      type="checkbox" 
                      checked={item.is_active} 
                      onChange={() => toggleRecurringStatus(item.id, !item.is_active)} 
                    />
                    <span className="slider round"></span>
                  </label>
                  <span>{item.is_active ? 'Active' : 'Paused'}</span>
                </div>
                
                <div className="action-buttons">
                  <button className="action-btn edit" onClick={() => handleEdit(item)}>✏️ Edit</button>
                  <button className="action-btn delete" onClick={() => handleDelete(item.id)}>🗑️ Delete</button>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
      
      {isFormOpen && (
        <RecurringForm 
          isOpen={isFormOpen}
          recurring={editingItem} 
          wallets={wallets}
          categories={categories}
          paymentMethods={paymentMethods}
          onClose={handleCloseForm} 
          onSave={async (data) => {
            let result;
            if (editingItem) {
              result = await updateRecurring(editingItem.id, data);
            } else {
              result = await addRecurring(data);
            }
            
            if (result && result.error) {
              alert("Error saving: " + result.error);
            } else {
              handleCloseForm();
            }
          }}
        />
      )}
    </div>
  );
};

export default Recurring;
