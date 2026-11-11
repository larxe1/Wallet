import React, { useState, useEffect, useId } from 'react';
import { format } from 'date-fns';
import { motion, AnimatePresence } from 'motion/react';
import { X, Calculator } from 'lucide-react';
import { parseLocalDate } from '../utils/format';
import InlineCalculator from './InlineCalculator';
import './IncomeForm.css';

const IncomeForm = ({
  isOpen,
  onClose,
  onSave,
  income,
  wallets = [],
  defaultWalletId,
  defaultDate,
}) => {
  const uid = useId();
  const titleId = `income-modal-title-${uid}`;
  const amountId = `income-amount-${uid}`;
  const descId = `income-desc-${uid}`;
  const walletFieldId = `income-wallet-${uid}`;
  const dateFieldId = `income-date-${uid}`;

  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [walletId, setWalletId] = useState('');
  const [date, setDate] = useState(format(new Date(), 'yyyy-MM-dd'));
  const [showCalc, setShowCalc] = useState(false);

  useEffect(() => {
    if (!isOpen) {
      setShowCalc(false);
      return;
    }

    if (income) {
      setAmount(income.amount !== undefined && income.amount !== null ? String(income.amount) : '');
      setDescription(income.description || '');
      setWalletId(income.wallet_id || '');
      const rawDate = income.income_date || income.date;
      setDate(rawDate ? format(parseLocalDate(rawDate), 'yyyy-MM-dd') : format(new Date(), 'yyyy-MM-dd'));
    } else {
      const matchedDefaultWallet =
        defaultWalletId && defaultWalletId !== 'all' && wallets.some((w) => w.id === defaultWalletId)
          ? defaultWalletId
          : '';
      setAmount('');
      setDescription('');
      setWalletId(matchedDefaultWallet || (wallets.length > 0 ? wallets[0].id : ''));
      setDate(defaultDate ? format(parseLocalDate(defaultDate), 'yyyy-MM-dd') : format(new Date(), 'yyyy-MM-dd'));
    }
    setShowCalc(false);
  }, [income, wallets, defaultWalletId, defaultDate, isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  const handleSubmit = (e) => {
    e.preventDefault();
    const numericAmount = Number(amount);
    if (!numericAmount || numericAmount <= 0) return;

    onSave({
      amount: numericAmount,
      description,
      wallet_id: walletId,
      income_date: date,
    });
  };

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          className="ww-inc-backdrop"
          onClick={handleOverlayClick}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.18 }}
        >
          <motion.div
            role="dialog"
            aria-modal="true"
            aria-labelledby={titleId}
            className="ww-inc-panel"
            initial={{ opacity: 0, y: 14, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 10, scale: 0.97 }}
            transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
          >
            <div className="ww-inc-header">
              <h2 id={titleId}>{income ? 'Edit Income' : 'Add Income'}</h2>
              <button
                type="button"
                className="ww-inc-close"
                onClick={onClose}
                aria-label="Close modal"
              >
                <X size={18} strokeWidth={2.2} aria-hidden="true" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="ww-inc-form">
              <div className="ww-inc-field">
                <label htmlFor={amountId} className="ww-inc-sr-only">Amount (PHP)</label>
                <div className="ww-inc-amount-box">
                  <span className="ww-inc-currency" aria-hidden="true">₱</span>
                  <div className="amount-with-calc">
                    <input
                      id={amountId}
                      type="number"
                      step="0.01"
                      min="0.01"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                      placeholder="0.00"
                      required
                      className="ww-inc-amount-input"
                      autoFocus
                    />
                    <button
                      type="button"
                      className={`calc-toggle-btn ${showCalc ? 'active' : ''}`}
                      onMouseDown={(e) => e.stopPropagation()}
                      onClick={() => setShowCalc((prev) => !prev)}
                      aria-label="Toggle inline calculator"
                      aria-expanded={showCalc}
                      title="Open calculator"
                    >
                      <Calculator size={16} strokeWidth={2.2} aria-hidden="true" />
                    </button>

                    <AnimatePresence>
                      {showCalc && (
                        <InlineCalculator
                          onResult={(val) => {
                            setAmount(val);
                            setShowCalc(false);
                          }}
                          onClose={() => setShowCalc(false)}
                        />
                      )}
                    </AnimatePresence>
                  </div>
                </div>
              </div>

              <div className="ww-inc-field">
                <label htmlFor={descId}>Description</label>
                <input
                  id={descId}
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="e.g. Salary, Freelance, Bonus"
                />
              </div>

              <div className="ww-inc-row">
                <div className="ww-inc-field">
                  <label htmlFor={walletFieldId}>Wallet</label>
                  <select
                    id={walletFieldId}
                    value={walletId}
                    onChange={(e) => setWalletId(e.target.value)}
                    required
                  >
                    <option value="" disabled>Select Wallet</option>
                    {wallets.map((w) => (
                      <option key={w.id} value={w.id}>{w.name}</option>
                    ))}
                  </select>
                </div>

                <div className="ww-inc-field">
                  <label htmlFor={dateFieldId}>Date</label>
                  <input
                    id={dateFieldId}
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="ww-inc-actions">
                <button type="button" onClick={onClose} className="ww-inc-cancel">
                  Cancel
                </button>
                <button type="submit" className="ww-inc-save">
                  {income ? 'Save Changes' : 'Save Income'}
                </button>
              </div>
            </form>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

export default IncomeForm;
