import React, { useState, useEffect, useId } from 'react';
import { format } from 'date-fns';
import { motion, AnimatePresence } from 'motion/react';
import { X, ArrowDownRight, ArrowUpRight, Calendar, Repeat } from 'lucide-react';
import { parseLocalDate } from '../utils/format';
import './RecurringForm.css';

const RecurringForm = ({
  isOpen,
  onClose,
  onSave,
  recurring,
  wallets = [],
  categories = [],
  paymentMethods = [],
}) => {
  const uid = useId();
  const titleId = `rec-modal-title-${uid}`;
  const amountId = `rec-amount-${uid}`;
  const descId = `rec-desc-${uid}`;
  const walletFieldId = `rec-wallet-${uid}`;
  const methodFieldId = `rec-method-${uid}`;
  const categoryFieldId = `rec-category-${uid}`;
  const statusSwitchId = `rec-status-${uid}`;
  const startDateId = `rec-start-${uid}`;
  const intervalDaysId = `rec-interval-${uid}`;

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
    if (!isOpen) return;

    if (recurring) {
      setAmount(recurring.amount !== undefined && recurring.amount !== null ? String(recurring.amount) : '');
      setDescription(recurring.description || '');
      setWalletId(recurring.wallet_id || (wallets.length > 0 ? wallets[0].id : ''));
      setCategoryId(recurring.category_id || (categories.length > 0 ? categories[0].id : ''));
      setPaymentMethodId(recurring.payment_method_id || (paymentMethods.length > 0 ? paymentMethods[0].id : ''));
      setDayOfMonth(recurring.day_of_month || 1);
      setIsActive(recurring.is_active !== false);
      setType(recurring.type || 'expense');
      setFrequencyType(recurring.frequency_type || 'monthly');
      setStartDate(
        recurring.start_date ? format(parseLocalDate(recurring.start_date), 'yyyy-MM-dd') : ''
      );
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
      setStartDate(format(new Date(), 'yyyy-MM-dd'));
      setIntervalDays(30);
    }
  }, [recurring, wallets, categories, paymentMethods, isOpen]);

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
      type,
      amount: numericAmount,
      description,
      wallet_id: walletId,
      category_id: type === 'income' ? null : categoryId,
      payment_method_id: type === 'income' ? null : paymentMethodId,
      frequency_type: frequencyType,
      day_of_month: frequencyType === 'monthly' ? Number(dayOfMonth) : null,
      start_date: frequencyType === 'interval' ? startDate : null,
      interval_days: frequencyType === 'interval' ? parseInt(intervalDays, 10) : null,
      is_active: isActive,
    });
  };

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  const days = Array.from({ length: 31 }, (_, i) => i + 1);

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          className="ww-rec-backdrop"
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
            className="ww-rec-panel"
            initial={{ opacity: 0, y: 14, scale: 0.97 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 10, scale: 0.97 }}
            transition={{ duration: 0.22, ease: [0.16, 1, 0.3, 1] }}
          >
            <div className="ww-rec-header">
              <h2 id={titleId}>
                {recurring ? 'Edit Recurring Transaction' : 'Add Recurring Transaction'}
              </h2>
              <button
                type="button"
                className="ww-rec-close"
                onClick={onClose}
                aria-label="Close modal"
              >
                <X size={18} strokeWidth={2.2} aria-hidden="true" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="ww-rec-form">
              <div className="ww-segmented" role="group" aria-label="Transaction type">
                {['expense', 'income'].map((itemType) => {
                  const selected = type === itemType;
                  return (
                    <button
                      key={itemType}
                      type="button"
                      className={`ww-segmented-btn ${selected ? `selected-${itemType}` : ''}`}
                      aria-pressed={selected}
                      onClick={() => setType(itemType)}
                    >
                      {selected && (
                        <motion.span
                          layoutId={`rec-type-pill-${uid}`}
                          className={`ww-segmented-bg ${itemType}`}
                          transition={{ type: 'spring', stiffness: 440, damping: 34 }}
                        />
                      )}
                      <span className="ww-segmented-label">
                        {itemType === 'expense' ? (
                          <ArrowUpRight size={15} strokeWidth={2.2} aria-hidden="true" />
                        ) : (
                          <ArrowDownRight size={15} strokeWidth={2.2} aria-hidden="true" />
                        )}
                        <span>{itemType === 'expense' ? 'Expense' : 'Income'}</span>
                      </span>
                    </button>
                  );
                })}
              </div>

              <div className="ww-rec-field">
                <label htmlFor={amountId} className="ww-rec-sr-only">Amount (PHP)</label>
                <div className={`ww-rec-amount-box ${type === 'income' ? 'income' : 'expense'}`}>
                  <span className="ww-rec-currency" aria-hidden="true">₱</span>
                  <input
                    id={amountId}
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="0.00"
                    required
                    className={`ww-rec-amount-input ${type === 'income' ? 'income' : 'expense'}`}
                    autoFocus
                  />
                </div>
              </div>

              <div className="ww-rec-field">
                <label htmlFor={descId}>Description</label>
                <input
                  id={descId}
                  type="text"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="e.g. Netflix Subscription, Monthly Rent"
                  required
                />
              </div>

              <div className="ww-rec-row">
                <div className={`ww-rec-field ${type === 'income' ? 'full-span' : ''}`}>
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

                {type === 'expense' && (
                  <div className="ww-rec-field">
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
                )}
              </div>

              <div className="ww-rec-row">
                {type === 'expense' && (
                  <div className="ww-rec-field">
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
                )}

                <div className={`ww-rec-field ${type === 'income' ? 'full-span' : ''}`}>
                  <label htmlFor={statusSwitchId}>Status</label>
                  <div className="ww-rec-status-box">
                    <button
                      id={statusSwitchId}
                      type="button"
                      role="switch"
                      aria-checked={isActive}
                      className={`ww-rec-switch ${isActive ? 'active' : ''}`}
                      onClick={() => setIsActive((prev) => !prev)}
                    >
                      <motion.span
                        className="ww-rec-switch-thumb"
                        layout
                        transition={{ type: 'spring', stiffness: 520, damping: 34 }}
                      />
                    </button>
                    <span className={`ww-rec-status-text ${isActive ? 'active' : ''}`}>
                      {isActive ? 'Active' : 'Paused'}
                    </span>
                  </div>
                </div>
              </div>

              <div className="ww-rec-field">
                <label>Schedule Frequency</label>
                <div className="ww-segmented" role="group" aria-label="Schedule frequency">
                  {[
                    { id: 'monthly', label: 'Monthly (Fixed Day)', Icon: Calendar },
                    { id: 'interval', label: 'Custom Interval', Icon: Repeat },
                  ].map(({ id, label, Icon }) => {
                    const selected = frequencyType === id;
                    return (
                      <button
                        key={id}
                        type="button"
                        className={`ww-segmented-btn ${selected ? 'selected-freq' : ''}`}
                        aria-pressed={selected}
                        onClick={() => {
                          setFrequencyType(id);
                          if (id === 'interval' && !startDate) {
                            setStartDate(format(new Date(), 'yyyy-MM-dd'));
                          }
                        }}
                      >
                        {selected && (
                          <motion.span
                            layoutId={`rec-freq-pill-${uid}`}
                            className="ww-segmented-bg freq"
                            transition={{ type: 'spring', stiffness: 440, damping: 34 }}
                          />
                        )}
                        <span className="ww-segmented-label">
                          <Icon size={14} strokeWidth={2.2} aria-hidden="true" />
                          <span>{label}</span>
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {frequencyType === 'monthly' ? (
                <div className="ww-rec-field">
                  <label>Day of Month (1–31)</label>
                  <div className="day-picker" role="group" aria-label="Day of month">
                    {days.map((day) => (
                      <button
                        key={day}
                        type="button"
                        className={`day-btn ${day === dayOfMonth ? 'selected' : ''}`}
                        aria-pressed={day === dayOfMonth}
                        onClick={() => setDayOfMonth(day)}
                      >
                        {day}
                      </button>
                    ))}
                  </div>
                </div>
              ) : (
                <div className="ww-rec-row">
                  <div className="ww-rec-field">
                    <label htmlFor={startDateId}>Start Date</label>
                    <input
                      id={startDateId}
                      type="date"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      required={frequencyType === 'interval'}
                    />
                  </div>
                  <div className="ww-rec-field">
                    <label htmlFor={intervalDaysId}>Trigger Every X Days</label>
                    <input
                      id={intervalDaysId}
                      type="number"
                      min="1"
                      value={intervalDays}
                      onChange={(e) => setIntervalDays(e.target.value)}
                      required={frequencyType === 'interval'}
                    />
                  </div>
                </div>
              )}

              <div className="ww-rec-actions">
                <button type="button" onClick={onClose} className="ww-rec-cancel">
                  Cancel
                </button>
                <button type="submit" className="ww-rec-save">
                  {recurring ? 'Save Changes' : 'Save Recurring'}
                </button>
              </div>
            </form>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

export default RecurringForm;
