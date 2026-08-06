import React, { useState } from 'react';
import { useWallets } from '../hooks/useWallets';
import { useCategories } from '../hooks/useCategories';
import { usePaymentMethods } from '../hooks/usePaymentMethods';
import CategoryForm from '../components/CategoryForm';
import './Settings.css';

const Settings = () => {
  const { wallets, addWallet, deleteWallet } = useWallets();
  const { categories, addCategory, deleteCategory } = useCategories();
  const { paymentMethods, addPaymentMethod, deletePaymentMethod } = usePaymentMethods();
  
  const [showCategoryForm, setShowCategoryForm] = useState(false);
  
  // Wallet state
  const [newWalletName, setNewWalletName] = useState('');
  const [newWalletColor, setNewWalletColor] = useState('#6366f1');
  
  // Payment Method state
  const [pmType, setPmType] = useState('credit');
  const [pmDetails, setPmDetails] = useState('');
  const [pmBank, setPmBank] = useState('');
  const [pmNetwork, setPmNetwork] = useState('Visa');
  
  const handleAddWallet = async () => {
    if (!newWalletName) return;
    await addWallet({ name: newWalletName, color: newWalletColor, balance: 0 });
    setNewWalletName('');
  };
  
  const handleAddPaymentMethod = async () => {
    if (!pmDetails) return;
    let label = '';
    if (pmType === 'credit') {
      const bankStr = pmBank ? `${pmBank} ` : '';
      label = `Credit • ${bankStr}${pmNetwork} ${pmDetails}`;
    } else if (pmType === 'debit') {
      label = `Debit (${pmDetails})`;
    } else {
      label = pmDetails;
    }
    
    await addPaymentMethod({ type: pmType, label });
    setPmDetails('');
    setPmBank('');
  };

  const handleSeedCategories = async () => {
    // 1. Clean up any duplicates caused by the roulette bug
    const categoryCounts = {};
    for (const c of categories) {
      const lowerName = c.name.toLowerCase();
      if (!categoryCounts[lowerName]) {
        categoryCounts[lowerName] = [];
      }
      categoryCounts[lowerName].push(c);
    }
    
    // For any category with more than 1 entry, delete the extras
    for (const name in categoryCounts) {
      const copies = categoryCounts[name];
      if (copies.length > 1) {
        // Keep the first one, delete the rest
        for (let i = 1; i < copies.length; i++) {
          await deleteCategory(copies[i].id);
        }
      }
    }

    // 2. Add any missing default categories
    const cats = [
      { name: 'Rent', icon: '🏠', sort_order: 1 },
      { name: 'Restaurant', icon: '🍔', sort_order: 2 },
      { name: 'Grocery', icon: '🛒', sort_order: 3 },
      { name: 'Subscription', icon: '🔄', sort_order: 4 },
      { name: 'Transportation', icon: '🚌', sort_order: 5 },
      { name: 'Work/School', icon: '💼', sort_order: 6 },
      { name: 'Bills', icon: '🧾', sort_order: 7 },
      { name: 'Household Items', icon: '🧻', sort_order: 8 },
      { name: 'Technology', icon: '💻', sort_order: 9 },
      { name: 'Healthcare', icon: '🏥', sort_order: 10 },
      { name: 'Service', icon: '🛠️', sort_order: 11 },
      { name: 'Clothing', icon: '👕', sort_order: 12 }
    ];
    
    // We need to re-check against the deduped list
    const remainingNames = new Set(categories.map(c => c.name.toLowerCase()));
    for (const c of cats) {
      if (!remainingNames.has(c.name.toLowerCase())) {
        await addCategory(c);
      }
    }
    alert('Categories seeded and duplicates cleaned up successfully!');
  };

  return (
    <div className="settings-container">
      <header className="page-header">
        <h1>Settings</h1>
      </header>
      
      <div className="settings-grid">
        <section className="settings-section">
          <h2>Wallets</h2>
          <div className="settings-card">
            <div className="list-group">
              {wallets.map(wallet => (
                <div key={wallet.id} className="list-item">
                  <div className="item-info">
                    <span className="color-swatch" style={{backgroundColor: wallet.color}}></span>
                    <span>{wallet.name}</span>
                  </div>
                  {!wallet.is_default && (
                    <button className="action-btn delete" onClick={() => deleteWallet(wallet.id)}>🗑️</button>
                  )}
                </div>
              ))}
            </div>
            
            <div className="add-inline">
              <input 
                type="color" 
                value={newWalletColor} 
                onChange={(e) => setNewWalletColor(e.target.value)} 
                className="color-picker"
              />
              <input 
                type="text" 
                placeholder="Wallet Name" 
                value={newWalletName}
                onChange={(e) => setNewWalletName(e.target.value)}
              />
              <button className="add-btn small" onClick={handleAddWallet}>Add</button>
            </div>
          </div>
        </section>
        
        <section className="settings-section">
          <h2>Payment Methods</h2>
          <div className="settings-card">
            <div className="list-group">
              {paymentMethods.map(pm => (
                <div key={pm.id} className="list-item">
                  <div className="item-info">
                    <span className={`pm-type-badge ${pm.type}`}>{pm.type}</span>
                    <span>{pm.label}</span>
                  </div>
                  <button className="action-btn delete" onClick={() => deletePaymentMethod(pm.id)}>🗑️</button>
                </div>
              ))}
            </div>
            
            <div className="add-inline pm-add">
              <select value={pmType} onChange={(e) => setPmType(e.target.value)}>
                <option value="credit">Credit Card</option>
                <option value="debit">Debit Card</option>
                <option value="cash">Cash/E-Wallet</option>
              </select>
              {pmType === 'credit' ? (
                <>
                  <input 
                    type="text" 
                    placeholder="Bank (e.g. BPI)" 
                    value={pmBank}
                    onChange={(e) => setPmBank(e.target.value)}
                  />
                  <select value={pmNetwork} onChange={(e) => setPmNetwork(e.target.value)}>
                    <option value="Visa">Visa</option>
                    <option value="Mastercard">Mastercard</option>
                    <option value="Amex">Amex</option>
                  </select>
                  <input 
                    type="text" 
                    placeholder="Last 4 digits" 
                    value={pmDetails}
                    onChange={(e) => setPmDetails(e.target.value)}
                    maxLength={4}
                  />
                </>
              ) : (
                <input 
                  type="text" 
                  placeholder="Bank/App Name" 
                  value={pmDetails}
                  onChange={(e) => setPmDetails(e.target.value)}
                />
              )}
              <button className="add-btn small" onClick={handleAddPaymentMethod}>Add</button>
            </div>
          </div>
        </section>
        
        <section className="settings-section full-width">
          <div className="section-header">
          <h2>Categories</h2>
          <div style={{display: 'flex', gap: '8px'}}>
            <button className="add-btn small" onClick={() => setShowCategoryForm(true)}>+ Category</button>
            <button className="add-btn small" style={{backgroundColor: '#10b981'}} onClick={handleSeedCategories}>+ Seed Defaults</button>
          </div>
        </div>
          <div className="settings-card categories-grid">
            {categories.map(cat => (
              <div key={cat.id} className="category-item">
                <span className="cat-icon">{cat.icon}</span>
                <span className="cat-name">{cat.name}</span>
                {!cat.is_default && (
                  <button className="action-btn delete small" onClick={() => deleteCategory(cat.id)}>✕</button>
                )}
              </div>
            ))}
          </div>
        </section>
      </div>
      
      {showCategoryForm && (
        <CategoryForm 
          isOpen={showCategoryForm}
          onClose={() => setShowCategoryForm(false)}
          onSave={async (data) => {
            await addCategory(data);
            setShowCategoryForm(false);
          }}
        />
      )}
    </div>
  );
};

export default Settings;
