import React, { useState, useEffect } from 'react';
import { format } from 'date-fns';
import InlineCalculator from './InlineCalculator';
import './ExpenseForm.css';

const ExpenseForm = ({ isOpen, onClose, onSave, expense, wallets = [], categories = [], paymentMethods = [] }) => {
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [walletId, setWalletId] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [paymentMethodId, setPaymentMethodId] = useState('');
  const [date, setDate] = useState(format(new Date(), 'yyyy-MM-dd'));
  const [isInstallment, setIsInstallment] = useState(false);
  const [installmentMonths, setInstallmentMonths] = useState(2);
  const [showCalc, setShowCalc] = useState(false);

  useEffect(() => {
    if (expense) {
      setAmount(expense.amount);
      setDescription(expense.description || '');
      setWalletId(expense.wallet_id || '');
      setCategoryId(expense.category_id || '');
      setPaymentMethodId(expense.payment_method_id || '');
      setDate(expense.date ? format(new Date(expense.date), 'yyyy-MM-dd') : format(new Date(), 'yyyy-MM-dd'));
    } else {
      setAmount('');
      setDescription('');
      setWalletId(wallets.length > 0 ? wallets[0].id : '');
      setCategoryId(categories.length > 0 ? categories[0].id : '');
      setPaymentMethodId(paymentMethods.length > 0 ? paymentMethods[0].id : '');
      setDate(format(new Date(), 'yyyy-MM-dd'));
      setIsInstallment(false);
      setInstallmentMonths(2);
    }
  }, [expense, wallets, categories, paymentMethods, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!amount || amount <= 0) return;

    onSave({
      amount: parseFloat(amount),
      description,
      wallet_id: walletId,
      category_id: categoryId,
      payment_method_id: paymentMethodId,
      expense_date: date,
      is_installment: isInstallment,
      installment_months: isInstallment ? parseInt(installmentMonths, 10) : 1
    });
  };

  const handleOverlayClick = (e) => {
    if (e.target.className === 'modal-overlay') {
      onClose();
    }
  };

  return (
    <div className="modal-overlay" onClick={handleOverlayClick}>
      <div className="modal-content slide-up">
        <h2>{expense ? 'Edit Expense' : 'Add Expense'}</h2>
        <form onSubmit={handleSubmit} className="expense-form">
          <div className="form-group amount-group">
            <span className="currency-symbol">₱</span>
            <div className="amount-with-calc" style={{ position: 'relative', flex: 1 }}>
              <input
                type="number"
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="0.00"
                required
                className="amount-input"
                autoFocus
              />
              <button
                type="button"
                className={`calc-toggle-btn ${showCalc ? 'active' : ''}`}
                onClick={() => setShowCalc(!showCalc)}
                title="Open calculator"
              >
                ⌗
              </button>
              {showCalc && (
                <InlineCalculator
                  onResult={(val) => { setAmount(val); setShowCalc(false); }}
                  onClose={() => setShowCalc(false)}
                />
              )}
            </div>
          </div>

          <div className="form-group">
            <label>Description</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="What was this for?"
            />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Wallet</label>
              <select value={walletId} onChange={(e) => setWalletId(e.target.value)} required>
                <option value="" disabled>Select Wallet</option>
                {wallets.map(w => (
                  <option key={w.id} value={w.id}>{w.name}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Category</label>
              <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)} required>
                <option value="" disabled>Select Category</option>
                {categories.map(c => (
                  <option key={c.id} value={c.id}>{c.icon} {c.name}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Payment Method</label>
              <select value={paymentMethodId} onChange={(e) => setPaymentMethodId(e.target.value)} required>
                <option value="" disabled>Select Method</option>
                {paymentMethods.map(pm => (
                  <option key={pm.id} value={pm.id}>{pm.label}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Date</label>
              <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                required
              />
            </div>
          </div>
          
          {!expense && (
            <div className="form-row">
              <div className="form-group toggle-group" style={{ flex: '1', display: 'flex', flexDirection: 'row', alignItems: 'center', gap: '12px' }}>
                <label style={{ margin: 0 }}>Is this an installment plan?</label>
                <div className="status-toggle" onClick={() => setIsInstallment(!isInstallment)} style={{ marginTop: 0 }}>
                  <div className={`toggle-switch ${isInstallment ? 'active' : ''}`}>
                    <div className="toggle-thumb"></div>
                  </div>
                </div>
              </div>
              
              {isInstallment && (
                <div className="form-group" style={{ flex: '1' }}>
                  <label>Number of Months</label>
                  <input
                    type="number"
                    min="2"
                    max="60"
                    value={installmentMonths}
                    onChange={(e) => setInstallmentMonths(e.target.value)}
                    required
                  />
                </div>
              )}
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

export default ExpenseForm;
