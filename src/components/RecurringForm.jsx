import React, { useState, useEffect } from 'react';
import './RecurringForm.css';

const RecurringForm = ({ isOpen, onClose, onSave, recurring, wallets = [], categories = [], paymentMethods = [] }) => {
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [walletId, setWalletId] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [paymentMethodId, setPaymentMethodId] = useState('');
  const [dayOfMonth, setDayOfMonth] = useState(1);
  const [isActive, setIsActive] = useState(true);
  const [type, setType] = useState('expense');
  const [frequencyType, setFrequencyType] = useState('monthly');
  const [startDate, setStartDate] = useState('');
  const [intervalDays, setIntervalDays] = useState(30);

  useEffect(() => {
    if (recurring) {
      setAmount(recurring.amount);
      setDescription(recurring.description || '');
      setWalletId(recurring.wallet_id || '');
      setCategoryId(recurring.category_id || '');
      setPaymentMethodId(recurring.payment_method_id || '');
      setDayOfMonth(recurring.day_of_month || 1);
      setIsActive(recurring.is_active !== false);
      setType(recurring.type || 'expense');
      setFrequencyType(recurring.frequency_type || 'monthly');
      setStartDate(recurring.start_date || '');
      setIntervalDays(recurring.interval_days || 30);
    } else {
      setAmount('');
      setDescription('');
      setWalletId(wallets.length > 0 ? wallets[0].id : '');
      setCategoryId(categories.length > 0 ? categories[0].id : '');
      setPaymentMethodId(paymentMethods.length > 0 ? paymentMethods[0].id : '');
      setDayOfMonth(1);
      setIsActive(true);
      setType('expense');
      setFrequencyType('monthly');
      setStartDate('');
      setIntervalDays(30);
    }
  }, [recurring, wallets, categories, paymentMethods, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!amount || amount <= 0) return;

    onSave({
      amount: parseFloat(amount),
      description,
      wallet_id: walletId,
      category_id: type === 'income' ? null : categoryId,
      payment_method_id: type === 'income' ? null : paymentMethodId,
      day_of_month: frequencyType === 'monthly' ? dayOfMonth : null,
      is_active: isActive,
      type,
      frequency_type: frequencyType,
      start_date: frequencyType === 'interval' ? startDate : null,
      interval_days: frequencyType === 'interval' ? parseInt(intervalDays, 10) : null
    });
  };

  const handleOverlayClick = (e) => {
    if (e.target.className === 'modal-overlay') {
      onClose();
    }
  };

  const days = Array.from({ length: 31 }, (_, i) => i + 1);

  return (
    <div className="modal-overlay" onClick={handleOverlayClick}>
      <div className="modal-content slide-up recurring-modal">
        <h2>{recurring ? 'Edit Recurring Transaction' : 'Add Recurring Transaction'}</h2>
        <form onSubmit={handleSubmit} className="recurring-form">
          <div className="type-toggle-group">
            <button 
              type="button" 
              className={`type-btn ${type === 'expense' ? 'expense-active' : ''}`}
              onClick={() => setType('expense')}
            >
              Expense
            </button>
            <button 
              type="button" 
              className={`type-btn ${type === 'income' ? 'income-active' : ''}`}
              onClick={() => setType('income')}
            >
              Income
            </button>
          </div>
          
          <div className={`form-group amount-group ${type === 'income' ? 'income-amount-group' : ''}`}>
            <span className="currency-symbol">₱</span>
            <input
              type="number"
              step="0.01"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="0.00"
              required
              className={`amount-input ${type === 'income' ? 'income-amount-input' : ''}`}
            />
          </div>

          <div className="form-group">
            <label>Description</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g. Netflix Subscription"
              required
            />
          </div>

          <div className="form-row">
            <div className={`form-group ${type === 'income' ? 'full-width' : ''}`}>
              <label>Wallet</label>
              <select value={walletId} onChange={(e) => setWalletId(e.target.value)} required>
                <option value="" disabled>Select Wallet</option>
                {wallets.map(w => (
                  <option key={w.id} value={w.id}>{w.name}</option>
                ))}
              </select>
            </div>
            
            {type === 'expense' && (
              <div className="form-group">
                <label>Payment Method</label>
                <select value={paymentMethodId} onChange={(e) => setPaymentMethodId(e.target.value)} required>
                  <option value="" disabled>Select Method</option>
                  {paymentMethods.map(pm => (
                    <option key={pm.id} value={pm.id}>{pm.label}</option>
                  ))}
                </select>
              </div>
            )}
          </div>

          <div className="form-row">
            {type === 'expense' && (
              <div className="form-group">
                <label>Category</label>
                <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)} required>
                  <option value="" disabled>Select Category</option>
                  {categories.map(c => (
                    <option key={c.id} value={c.id}>{c.icon} {c.name}</option>
                  ))}
                </select>
              </div>
            )}
            <div className={`form-group toggle-group ${type === 'income' ? 'full-width' : ''}`}>
              <label>Status</label>
              <div className="status-toggle" onClick={() => setIsActive(!isActive)}>
                <div className={`toggle-switch ${isActive ? 'active' : ''}`}>
                  <div className="toggle-thumb"></div>
                </div>
                <span>{isActive ? 'Active' : 'Paused'}</span>
              </div>
            </div>
          </div>

          <div className="form-group">
            <label>Frequency Type</label>
            <div className="type-toggle-group">
              <button 
                type="button" 
                className={`type-btn ${frequencyType === 'monthly' ? 'income-active' : ''}`}
                onClick={() => setFrequencyType('monthly')}
                style={{flex: 1}}
              >
                Monthly (Fixed Day)
              </button>
              <button 
                type="button" 
                className={`type-btn ${frequencyType === 'interval' ? 'expense-active' : ''}`}
                onClick={() => setFrequencyType('interval')}
                style={{flex: 1}}
              >
                Custom Interval
              </button>
            </div>
          </div>

          {frequencyType === 'monthly' ? (
            <div className="form-group">
              <label>Day of Month (1-31)</label>
              <div className="day-picker">
                {days.map(day => (
                  <button
                    key={day}
                    type="button"
                    className={`day-btn ${day === dayOfMonth ? 'selected' : ''}`}
                    onClick={() => setDayOfMonth(day)}
                  >
                    {day}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <div className="form-row">
              <div className="form-group">
                <label>Start Date</label>
                <input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  required={frequencyType === 'interval'}
                />
              </div>
              <div className="form-group">
                <label>Trigger Every X Days</label>
                <input
                  type="number"
                  min="1"
                  value={intervalDays}
                  onChange={(e) => setIntervalDays(e.target.value)}
                  required={frequencyType === 'interval'}
                />
              </div>
            </div>
          )}

          <div className="modal-actions">
            <button type="button" onClick={onClose} className="btn-cancel">Cancel</button>
            <button type="submit" className="btn-save">Save</button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RecurringForm;
