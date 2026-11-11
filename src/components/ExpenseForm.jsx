import React, { useState, useEffect, useId } from 'react';
import { format } from 'date-fns';
import { motion, AnimatePresence } from 'motion/react';
import { X, Calculator, Layers } from 'lucide-react';
import { parseLocalDate } from '../utils/format';
import InlineCalculator from './InlineCalculator';
import './ExpenseForm.css';

const ExpenseForm = ({
  isOpen,
  onClose,
  onSave,
  expense,
  wallets = [],
  categories = [],
  paymentMethods = [],
  defaultWalletId,
  defaultDate,
}) => {
  const uid = useId();
  const titleId = `expense-modal-title-${uid}`;
  const amountId = `expense-amount-${uid}`;
  const descId = `expense-desc-${uid}`;
  const walletFieldId = `expense-wallet-${uid}`;
  const categoryFieldId = `expense-category-${uid}`;
  const methodFieldId = `expense-method-${uid}`;
  const dateFieldId = `expense-date-${uid}`;
  const installmentSwitchId = `expense-installment-${uid}`;
  const installmentMonthsId = `expense-months-${uid}`;

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
    if (!isOpen) {
      setShowCalc(false);
      return;
    }

    if (expense) {
      setAmount(expense.amount !== undefined && expense.amount !== null ? String(expense.amount) : '');
      setDescription(expense.description || '');
      setWalletId(expense.wallet_id || '');
      setCategoryId(expense.category_id || '');
      setPaymentMethodId(expense.payment_method_id || '');
      const rawDate = expense.expense_date || expense.date;
      setDate(rawDate ? format(parseLocalDate(rawDate), 'yyyy-MM-dd') : format(new Date(), 'yyyy-MM-dd'));
      setIsInstallment(false);
      setInstallmentMonths(2);
    } else {
      const matchedDefaultWallet =
        defaultWalletId && defaultWalletId !== 'all' && wallets.some((w) => w.id === defaultWalletId)
          ? defaultWalletId
          : '';
      setAmount('');
      setDescription('');
      setWalletId(matchedDefaultWallet || (wallets.length > 0 ? wallets[0].id : ''));
      setCategoryId(categories.length > 0 ? categories[0].id : '');
      setPaymentMethodId(paymentMethods.length > 0 ? paymentMethods[0].id : '');
      setDate(defaultDate ? format(parseLocalDate(defaultDate), 'yyyy-MM-dd') : format(new Date(), 'yyyy-MM-dd'));
      setIsInstallment(false);
      setInstallmentMonths(2);
    }
    setShowCalc(false);
  }, [expense, wallets, categories, paymentMethods, defaultWalletId, defaultDate, isOpen]);

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
      category_id: categoryId,
      payment_method_id: paymentMethodId,
      expense_date: date,
      is_installment: isInstallment,
      installment_months: isInstallment ? parseInt(installmentMonths, 10) || 2 : 1,
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
          className="ww-modal-backdrop"
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
            className="ww-modal-panel ww-expense-modal"
            initial={{ opacity: 0, y: 14, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 10, scale: 0.97 }}
            transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
          >
            <div className="ww-modal-header">
              <h2 id={titleId}>{expense ? 'Edit Expense' : 'Add Expense'}</h2>
              <button
                type="button"
                className="ww-modal-close"
                onClick={onClose}
                aria-label="Close modal"
              >
                <X size={18} strokeWidth={2.2} aria-hidden="true" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="ww-expense-form">
              <div className="ww-field">
                <label htmlFor={amountId} className="ww-sr-only">Amount (PHP)</label>
                <div className="ww-amount-box ww-amount-expense">
                  <span className="ww-currency-symbol" aria-hidden="true">₱</span>
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
                      className="ww-amount-input ww-amount-input-expense"
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

              <div className="ww-field">
                <label htmlFor={descId}>Description</label>
                <input
                  id={descId}
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="What was this for?"
                />
              </div>

              <div className="ww-form-row">
                <div className="ww-field">
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

                <div className="ww-field">
                  <label htmlFor={categoryFieldId}>Category</label>
                  <select
                    id={categoryFieldId}
                    value={categoryId}
                    onChange={(e) => setCategoryId(e.target.value)}
                    required
                  >
                    <option value="" disabled>Select Category</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>{c.icon} {c.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="ww-form-row">
                <div className="ww-field">
                  <label htmlFor={methodFieldId}>Payment Method</label>
                  <select
                    id={methodFieldId}
                    value={paymentMethodId}
                    onChange={(e) => setPaymentMethodId(e.target.value)}
                    required
                  >
                    <option value="" disabled>Select Method</option>
                    {paymentMethods.map((pm) => (
                      <option key={pm.id} value={pm.id}>{pm.label}</option>
                    ))}
                  </select>
                </div>

                <div className="ww-field">
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

              {!expense && (
                <div className="ww-installment-card">
                  <div className="ww-installment-header">
                    <div className="ww-installment-label-wrap">
                      <Layers size={15} strokeWidth={2.2} className="ww-installment-icon" aria-hidden="true" />
                      <label htmlFor={installmentSwitchId} className="ww-installment-label">
                        Installment plan
                      </label>
                    </div>

                    <button
                      id={installmentSwitchId}
                      type="button"
                      role="switch"
                      aria-checked={isInstallment}
                      className={`ww-switch ${isInstallment ? 'active' : ''}`}
                      onClick={() => setIsInstallment((prev) => !prev)}
                    >
                      <motion.span
                        className="ww-switch-thumb"
                        layout
                        transition={{ type: 'spring', stiffness: 520, damping: 34 }}
                      />
                    </button>
                  </div>

                  <AnimatePresence initial={false}>
                    {isInstallment && (
                      <motion.div
                        className="ww-installment-months"
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        transition={{ duration: 0.18 }}
                      >
                        <div className="ww-field ww-installment-months-field">
                          <label htmlFor={installmentMonthsId}>Number of Months (2–60)</label>
                          <input
                            id={installmentMonthsId}
                            type="number"
                            min="2"
                            max="60"
                            value={installmentMonths}
                            onChange={(e) => setInstallmentMonths(e.target.value)}
                            required
                          />
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>
              )}

              <div className="ww-modal-actions">
                <button type="button" onClick={onClose} className="ww-btn-cancel">
                  Cancel
                </button>
                <button type="submit" className="ww-btn-save">
                  {expense ? 'Save Changes' : 'Save Expense'}
                </button>
              </div>
            </form>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

export default ExpenseForm;
