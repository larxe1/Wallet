import React, { useState } from 'react';
import { motion } from 'motion/react';
import {
  Plus,
  Trash2,
  GripVertical,
  Wallet,
  CreditCard,
  Tag,
  Sparkles,
  Check,
  Landmark,
  Banknote
} from 'lucide-react';
import { useWallets } from '../hooks/useWallets';
import { useCategories } from '../hooks/useCategories';
import { usePaymentMethods } from '../hooks/usePaymentMethods';
import { useFeedback } from '../context/FeedbackContext';
import CategoryForm from '../components/CategoryForm';
import './Settings.css';

const WALLET_SWATCHES = [
  { hex: '#28C991', label: 'Emerald' },
  { hex: '#8E7CF8', label: 'Electric Violet' },
  { hex: '#38BDF8', label: 'Sky Blue' },
  { hex: '#C8F865', label: 'Chartreuse' },
  { hex: '#F0B44A', label: 'Amber' },
  { hex: '#F06478', label: 'Rose' },
  { hex: '#14B8A6', label: 'Teal' }
];

const DEFAULT_CATEGORIES = [
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

const Settings = () => {
  const { wallets, addWallet, deleteWallet } = useWallets();
  const { categories, addCategory, deleteCategory } = useCategories();
  const {
    paymentMethods,
    addPaymentMethod,
    deletePaymentMethod,
    updatePaymentMethodOrder
  } = usePaymentMethods();
  const { showToast, confirm } = useFeedback();

  const [showCategoryForm, setShowCategoryForm] = useState(false);
  const [isSeeding, setIsSeeding] = useState(false);

  // Drag and Drop state
  const [draggedIdx, setDraggedIdx] = useState(null);
  const [dragOverIdx, setDragOverIdx] = useState(null);

  // Wallet state
  const [newWalletName, setNewWalletName] = useState('');
  const [newWalletColor, setNewWalletColor] = useState('#28C991');

  // Payment Method state
  const [pmType, setPmType] = useState('credit');
  const [pmDetails, setPmDetails] = useState('');
  const [pmBank, setPmBank] = useState('');
  const [pmNetwork, setPmNetwork] = useState('Visa');

  const handleAddWallet = async (e) => {
    if (e) e.preventDefault();
    const trimmed = newWalletName.trim();
    if (!trimmed) {
      showToast('Enter a wallet name first', 'warning');
      return;
    }

    const res = await addWallet({ name: trimmed, color: newWalletColor, balance: 0 });
    if (res?.error) {
      showToast(`Failed to add wallet: ${res.error}`, 'error');
      return;
    }
    setNewWalletName('');
    showToast(`Wallet "${trimmed}" added`, 'success');
  };

  const handleDeleteWallet = async (wallet) => {
    if (wallet.is_default) return;
    const confirmed = await confirm({
      title: 'Delete Wallet',
      message: `Remove wallet "${wallet.name}"? Transactions linked to it may be affected.`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      variant: 'danger'
    });
    if (!confirmed) return;

    const res = await deleteWallet(wallet.id);
    if (res?.error) {
      showToast(`Failed to delete wallet: ${res.error}`, 'error');
    } else {
      showToast(`Wallet "${wallet.name}" deleted`, 'success');
    }
  };

  const handleAddPaymentMethod = async (e) => {
    if (e) e.preventDefault();
    const trimmedDetails = pmDetails.trim();
    if (!trimmedDetails) {
      showToast(
        pmType === 'credit'
          ? 'Enter the card last 4 digits'
          : 'Enter a bank or app name',
        'warning'
      );
      return;
    }

    let label = '';
    if (pmType === 'credit') {
      const bankStr = pmBank.trim() ? `${pmBank.trim()} ` : '';
      label = `Credit • ${bankStr}${pmNetwork} ${trimmedDetails}`;
    } else if (pmType === 'debit') {
      label = `Debit (${trimmedDetails})`;
    } else {
      label = trimmedDetails;
    }

    const res = await addPaymentMethod({ type: pmType, label });
    if (res?.error) {
      showToast(`Failed to add payment method: ${res.error}`, 'error');
      return;
    }
    setPmDetails('');
    setPmBank('');
    showToast('Payment method added', 'success');
  };

  const handleDeletePaymentMethod = async (pm) => {
    const confirmed = await confirm({
      title: 'Delete Payment Method',
      message: `Remove "${pm.label}" from your payment methods?`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      variant: 'danger'
    });
    if (!confirmed) return;

    const res = await deletePaymentMethod(pm.id);
    if (res?.error) {
      showToast(`Failed to delete payment method: ${res.error}`, 'error');
    } else {
      showToast('Payment method deleted', 'success');
    }
  };

  const handleDeleteCategory = async (cat) => {
    if (cat.is_default) return;
    const confirmed = await confirm({
      title: 'Delete Category',
      message: `Remove category "${cat.name}"?`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      variant: 'danger'
    });
    if (!confirmed) return;

    const res = await deleteCategory(cat.id);
    if (res?.error) {
      showToast(`Failed to delete category: ${res.error}`, 'error');
    } else {
      showToast(`Category "${cat.name}" deleted`, 'success');
    }
  };

  const handleSeedCategories = async () => {
    if (isSeeding) return;
    setIsSeeding(true);
    try {
      // 1. Clean up any duplicates
      const categoryCounts = {};
      for (const c of categories) {
        const lowerName = c.name.toLowerCase();
        if (!categoryCounts[lowerName]) {
          categoryCounts[lowerName] = [];
        }
        categoryCounts[lowerName].push(c);
      }

      let removedCount = 0;
      for (const name in categoryCounts) {
        const copies = categoryCounts[name];
        if (copies.length > 1) {
          for (let i = 1; i < copies.length; i++) {
            const delRes = await deleteCategory(copies[i].id);
            if (!delRes?.error) removedCount += 1;
          }
        }
      }

      // 2. Add any missing default categories
      const remainingNames = new Set(categories.map((c) => c.name.toLowerCase()));
      let addedCount = 0;
      for (const c of DEFAULT_CATEGORIES) {
        if (!remainingNames.has(c.name.toLowerCase())) {
          const addRes = await addCategory(c);
          if (!addRes?.error) addedCount += 1;
        }
      }

      if (addedCount > 0 || removedCount > 0) {
        const parts = [];
        if (addedCount > 0) parts.push(`${addedCount} added`);
        if (removedCount > 0) parts.push(`${removedCount} duplicates cleaned`);
        showToast(`Categories synced (${parts.join(', ')})`, 'success');
      } else {
        showToast('Default categories are already up to date', 'info');
      }
    } finally {
      setIsSeeding(false);
    }
  };

  // Drag and drop handlers
  const handleDragStart = (e, index) => {
    setDraggedIdx(index);
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e, index) => {
    e.preventDefault();
    if (dragOverIdx !== index) {
      setDragOverIdx(index);
    }
  };

  const handleDrop = async (e, targetIdx) => {
    e.preventDefault();
    setDragOverIdx(null);
    if (draggedIdx === null || draggedIdx === targetIdx) {
      setDraggedIdx(null);
      return;
    }

    const newItems = [...paymentMethods];
    const [movedItem] = newItems.splice(draggedIdx, 1);
    newItems.splice(targetIdx, 0, movedItem);

    setDraggedIdx(null);
    const res = await updatePaymentMethodOrder(newItems.map((pm) => pm.id));
    if (res?.error) {
      showToast(`Failed to reorder payment methods: ${res.error}`, 'error');
    } else {
      showToast('Payment method order saved', 'success');
    }
  };

  const handleDragEnd = () => {
    setDraggedIdx(null);
    setDragOverIdx(null);
  };

  const renderPmIcon = (type) => {
    if (type === 'credit') return <CreditCard size={13} aria-hidden="true" />;
    if (type === 'debit') return <Landmark size={13} aria-hidden="true" />;
    return <Banknote size={13} aria-hidden="true" />;
  };

  return (
    <div className="settings-page">
      <header className="set-header">
        <h1>Settings</h1>
      </header>

      <div className="set-grid">
        {/* Wallets Section */}
        <section className="set-section" aria-labelledby="settings-wallets-heading">
          <div className="set-section-head">
            <div className="set-section-title">
              <span className="set-section-icon set-section-icon--emerald" aria-hidden="true">
                <Wallet size={17} />
              </span>
              <h2 id="settings-wallets-heading">Wallets</h2>
            </div>
            <span className="set-count-pill">{wallets.length}</span>
          </div>

          <div className="set-card">
            <div className="set-list">
              {wallets.length === 0 ? (
                <p className="set-empty-note">No wallets created yet.</p>
              ) : (
                wallets.map((wallet) => (
                  <div key={wallet.id} className="set-list-item">
                    <div className="set-item-left">
                      <span
                        className="set-wallet-swatch"
                        style={{ backgroundColor: wallet.color || '#28C991' }}
                        aria-hidden="true"
                      />
                      <span className="set-item-name">{wallet.name}</span>
                      {wallet.is_default && (
                        <span className="set-default-badge">Default</span>
                      )}
                    </div>
                    {!wallet.is_default && (
                      <button
                        type="button"
                        className="set-icon-btn set-icon-btn--delete"
                        onClick={() => handleDeleteWallet(wallet)}
                        aria-label={`Delete wallet ${wallet.name}`}
                        title="Delete wallet"
                      >
                        <Trash2 size={15} />
                      </button>
                    )}
                  </div>
                ))
              )}
            </div>

            <form className="set-inline-box set-wallet-form" onSubmit={handleAddWallet}>
              <div className="set-swatch-row" role="group" aria-label="Wallet color presets">
                {WALLET_SWATCHES.map((swatch) => {
                  const selected =
                    newWalletColor.toLowerCase() === swatch.hex.toLowerCase();
                  return (
                    <button
                      key={swatch.hex}
                      type="button"
                      className={`set-swatch-btn ${selected ? 'is-selected' : ''}`}
                      style={{ backgroundColor: swatch.hex }}
                      onClick={() => setNewWalletColor(swatch.hex)}
                      aria-label={`Select ${swatch.label} color`}
                      aria-pressed={selected}
                      title={swatch.label}
                    >
                      {selected && <Check size={12} strokeWidth={3} aria-hidden="true" />}
                    </button>
                  );
                })}
                <label className="set-custom-color" title="Custom color">
                  <input
                    type="color"
                    value={newWalletColor}
                    onChange={(e) => setNewWalletColor(e.target.value)}
                    aria-label="Custom wallet color"
                  />
                  <span
                    className="set-custom-color-preview"
                    style={{ backgroundColor: newWalletColor }}
                    aria-hidden="true"
                  />
                </label>
              </div>

              <div className="set-inline-row">
                <input
                  type="text"
                  className="set-input"
                  placeholder="New wallet name"
                  aria-label="New wallet name"
                  value={newWalletName}
                  onChange={(e) => setNewWalletName(e.target.value)}
                />
                <button type="submit" className="set-primary-btn">
                  <Plus size={15} strokeWidth={2.4} aria-hidden="true" />
                  <span>Add</span>
                </button>
              </div>
            </form>
          </div>
        </section>

        {/* Payment Methods Section */}
        <section className="set-section" aria-labelledby="settings-pm-heading">
          <div className="set-section-head">
            <div className="set-section-title">
              <span className="set-section-icon set-section-icon--violet" aria-hidden="true">
                <CreditCard size={17} />
              </span>
              <h2 id="settings-pm-heading">Payment Methods</h2>
            </div>
            <span className="set-count-pill">{paymentMethods.length}</span>
          </div>

          <div className="set-card">
            <div className="set-list set-pm-list">
              {paymentMethods.length === 0 ? (
                <p className="set-empty-note">No payment methods configured yet.</p>
              ) : (
                paymentMethods.map((pm, idx) => (
                  <div
                    key={pm.id}
                    className={`set-pm-item ${draggedIdx === idx ? 'is-dragging' : ''} ${
                      dragOverIdx === idx && draggedIdx !== idx ? 'is-drag-over' : ''
                    }`}
                    draggable
                    onDragStart={(e) => handleDragStart(e, idx)}
                    onDragOver={(e) => handleDragOver(e, idx)}
                    onDrop={(e) => handleDrop(e, idx)}
                    onDragEnd={handleDragEnd}
                  >
                    <div className="set-item-left">
                      <span className="set-drag-handle" aria-hidden="true" title="Drag to reorder">
                        <GripVertical size={16} />
                      </span>
                      <span className={`set-pm-badge set-pm-badge--${pm.type || 'cash'}`}>
                        {renderPmIcon(pm.type)}
                        <span>{pm.type}</span>
                      </span>
                      <span className="set-item-name">{pm.label}</span>
                    </div>
                    <button
                      type="button"
                      className="set-icon-btn set-icon-btn--delete"
                      onClick={() => handleDeletePaymentMethod(pm)}
                      aria-label={`Delete payment method ${pm.label}`}
                      title="Delete payment method"
                    >
                      <Trash2 size={15} />
                    </button>
                  </div>
                ))
              )}
            </div>

            <form className="set-inline-box set-pm-form" onSubmit={handleAddPaymentMethod}>
              <div className="set-pm-fields">
                <select
                  className="set-select"
                  aria-label="Payment method type"
                  value={pmType}
                  onChange={(e) => setPmType(e.target.value)}
                >
                  <option value="credit">Credit Card</option>
                  <option value="debit">Debit Card</option>
                  <option value="cash">Cash / E-Wallet</option>
                </select>

                {pmType === 'credit' ? (
                  <>
                    <input
                      type="text"
                      className="set-input"
                      placeholder="Bank (e.g. BPI)"
                      aria-label="Issuing bank"
                      value={pmBank}
                      onChange={(e) => setPmBank(e.target.value)}
                    />
                    <select
                      className="set-select"
                      aria-label="Card network"
                      value={pmNetwork}
                      onChange={(e) => setPmNetwork(e.target.value)}
                    >
                      <option value="Visa">Visa</option>
                      <option value="Mastercard">Mastercard</option>
                      <option value="Amex">Amex</option>
                    </select>
                    <input
                      type="text"
                      className="set-input"
                      placeholder="Last 4 digits"
                      aria-label="Last 4 digits"
                      value={pmDetails}
                      onChange={(e) => setPmDetails(e.target.value)}
                      maxLength={4}
                    />
                  </>
                ) : (
                  <input
                    type="text"
                    className="set-input set-input--wide"
                    placeholder="Bank or App Name"
                    aria-label="Bank or app name"
                    value={pmDetails}
                    onChange={(e) => setPmDetails(e.target.value)}
                  />
                )}

                <button type="submit" className="set-primary-btn">
                  <Plus size={15} strokeWidth={2.4} aria-hidden="true" />
                  <span>Add</span>
                </button>
              </div>
            </form>
          </div>
        </section>

        {/* Categories Section */}
        <section
          className="set-section set-section--full"
          aria-labelledby="settings-categories-heading"
        >
          <div className="set-section-head">
            <div className="set-section-title">
              <span className="set-section-icon set-section-icon--emerald" aria-hidden="true">
                <Tag size={17} />
              </span>
              <h2 id="settings-categories-heading">Categories</h2>
              <span className="set-count-pill">{categories.length}</span>
            </div>

            <div className="set-cat-actions">
              <button
                type="button"
                className="set-primary-btn"
                onClick={() => setShowCategoryForm(true)}
              >
                <Plus size={15} strokeWidth={2.4} aria-hidden="true" />
                <span>Category</span>
              </button>
              <button
                type="button"
                className="set-secondary-btn"
                onClick={handleSeedCategories}
                disabled={isSeeding}
              >
                <Sparkles size={15} aria-hidden="true" />
                <span>{isSeeding ? 'Syncing...' : 'Seed Defaults'}</span>
              </button>
            </div>
          </div>

          <div className="set-card set-categories-grid">
            {categories.map((cat, idx) => (
              <motion.div
                key={cat.id}
                className="set-category-chip"
                initial={{ opacity: 0, scale: 0.96 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{
                  duration: 0.18,
                  delay: Math.min(idx * 0.015, 0.18),
                  ease: [0.16, 1, 0.3, 1]
                }}
              >
                <span className="set-cat-emoji-badge" aria-hidden="true">
                  {cat.icon}
                </span>
                <span className="set-cat-name">{cat.name}</span>
                {cat.is_default ? (
                  <span className="set-cat-default-tag">Default</span>
                ) : (
                  <button
                    type="button"
                    className="set-cat-delete"
                    onClick={() => handleDeleteCategory(cat)}
                    aria-label={`Delete category ${cat.name}`}
                    title="Delete category"
                  >
                    <Trash2 size={13} />
                  </button>
                )}
              </motion.div>
            ))}
          </div>
        </section>
      </div>

      <CategoryForm
        isOpen={showCategoryForm}
        onClose={() => setShowCategoryForm(false)}
        onSave={async (data) => {
          const res = await addCategory(data);
          if (res?.error) {
            showToast(`Failed to add category: ${res.error}`, 'error');
            return;
          }
          showToast(`Category "${data.name}" added`, 'success');
          setShowCategoryForm(false);
        }}
      />
    </div>
  );
};

export default Settings;
