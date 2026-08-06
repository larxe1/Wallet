import React, { useState, useEffect } from 'react';
import { format } from 'date-fns';
import './IncomeForm.css';

const IncomeForm = ({ isOpen, onClose, onSave, income, wallets = [] }) => {
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [walletId, setWalletId] = useState('');
  const [date, setDate] = useState(format(new Date(), 'yyyy-MM-dd'));

  useEffect(() => {
    if (income) {
      setAmount(income.amount);
      setDescription(income.description || '');
      setWalletId(income.wallet_id || '');
      setDate(income.date ? format(new Date(income.date), 'yyyy-MM-dd') : format(new Date(), 'yyyy-MM-dd'));
    } else {
      setAmount('');
      setDescription('');
      setWalletId(wallets.length > 0 ? wallets[0].id : '');
      setDate(format(new Date(), 'yyyy-MM-dd'));
    }
  }, [income, wallets, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!amount || amount <= 0) return;

    onSave({
      amount: parseFloat(amount),
      description,
      wallet_id: walletId,
      income_date: date
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
        <h2>{income ? 'Edit Income' : 'Add Income'}</h2>
        <form onSubmit={handleSubmit} className="income-form">
          <div className="form-group amount-group income-amount-group">
            <span className="currency-symbol">₱</span>
            <input
              type="number"
              step="0.01"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="0.00"
              required
              className="amount-input income-amount-input"
              autoFocus
            />
          </div>

          <div className="form-group">
            <label>Description</label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="e.g. Salary, Bonus"
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
              <label>Date</label>
              <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="modal-actions">
            <button type="button" onClick={onClose} className="btn-cancel">Cancel</button>
            <button type="submit" className="btn-save btn-income-save">Save</button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default IncomeForm;
