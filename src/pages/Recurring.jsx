import React, { useState, useMemo } from 'react';
import { motion } from 'motion/react';
import {
  Plus,
  Pencil,
  Trash2,
  Repeat,
  TrendingUp,
  Receipt,
  CalendarClock,
  Clock
} from 'lucide-react';
import { useRecurring } from '../hooks/useRecurring';
import { useWallets } from '../hooks/useWallets';
import { useCategories } from '../hooks/useCategories';
import { usePaymentMethods } from '../hooks/usePaymentMethods';
import { useFeedback } from '../context/FeedbackContext';
import { formatCurrency, formatDate } from '../utils/format';
import RecurringForm from '../components/RecurringForm';
import './Recurring.css';

const getOrdinal = (day) => {
  const num = Number(day) || 1;
  const mod100 = num % 100;
  if (mod100 >= 11 && mod100 <= 13) return `${num}th`;
  switch (num % 10) {
    case 1:
      return `${num}st`;
    case 2:
      return `${num}nd`;
    case 3:
      return `${num}rd`;
    default:
      return `${num}th`;
  }
};

const Recurring = () => {
  const {
    recurring,
    loading,
    addRecurring,
    updateRecurring,
    deleteRecurring,
    toggleRecurringStatus
  } = useRecurring();
  const { wallets } = useWallets();
  const { categories } = useCategories();
  const { paymentMethods } = usePaymentMethods();
  const { showToast, confirm } = useFeedback();

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);

  const handleEdit = (item) => {
    setEditingItem(item);
    setIsFormOpen(true);
  };

  const handleDelete = async (item) => {
    const label = item.description || item.categories?.name || 'this recurring rule';
    const confirmed = await confirm({
      title: 'Delete Recurring Rule',
      message: `Remove "${label}" (${formatCurrency(item.amount)})? Future automatic postings will stop.`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      variant: 'danger'
    });
    if (!confirmed) return;

    const result = await deleteRecurring(item.id);
    if (result?.error) {
      showToast(`Failed to delete recurring rule: ${result.error}`, 'error');
    } else {
      showToast('Recurring rule deleted', 'success');
    }
  };

  const handleToggleStatus = async (item) => {
    const nextActive = !item.is_active;
    const result = await toggleRecurringStatus(item.id, nextActive);
    if (result?.error) {
      showToast(`Failed to update status: ${result.error}`, 'error');
    } else {
      showToast(
        nextActive ? 'Automation resumed' : 'Automation paused',
        nextActive ? 'success' : 'info'
      );
    }
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingItem(null);
  };

  const summary = useMemo(() => {
    let monthlyIncome = 0;
    let monthlyExpenses = 0;
    let activeCount = 0;
    let hasInterval = false;

    recurring.forEach((item) => {
      if (item.is_active === false) return;
      activeCount += 1;

      const amt = Number(item.amount) || 0;
      const isInterval = item.frequency_type === 'interval' && Number(item.interval_days) > 0;
      if (isInterval) hasInterval = true;
      const monthlyNormalized = isInterval ? amt * (30 / Number(item.interval_days)) : amt;

      if (item.type === 'income') {
        monthlyIncome += monthlyNormalized;
      } else {
        monthlyExpenses += monthlyNormalized;
      }
    });

    return {
      monthlyIncome,
      monthlyExpenses,
      activeCount,
      totalCount: recurring.length,
      hasInterval
    };
  }, [recurring]);

  return (
    <div className="recurring-page">
      <header className="rec-header">
        <h1>Recurring Transactions</h1>
        <button
          type="button"
          className="rec-add-btn"
          onClick={() => {
            setEditingItem(null);
            setIsFormOpen(true);
          }}
        >
          <Plus size={16} strokeWidth={2.4} aria-hidden="true" />
          <span>Add Recurring</span>
        </button>
      </header>

      <section className="rec-kpi-strip" aria-label="Recurring summary">
        <div className="rec-kpi-card">
          <div className="rec-kpi-top">
            <span className="rec-kpi-label">Monthly Recurring Income</span>
            <span className="rec-kpi-icon rec-kpi-icon--emerald" aria-hidden="true">
              <TrendingUp size={16} />
            </span>
          </div>
          <div className="rec-kpi-value rec-kpi-value--emerald">
            {formatCurrency(summary.monthlyIncome)}
          </div>
          <span className="rec-kpi-note">
            {summary.hasInterval ? 'Est. 30-day cycle' : 'Active monthly rules'}
          </span>
        </div>

        <div className="rec-kpi-card">
          <div className="rec-kpi-top">
            <span className="rec-kpi-label">Monthly Recurring Expenses</span>
            <span className="rec-kpi-icon rec-kpi-icon--violet" aria-hidden="true">
              <Receipt size={16} />
            </span>
          </div>
          <div className="rec-kpi-value rec-kpi-value--danger">
            {formatCurrency(summary.monthlyExpenses)}
          </div>
          <span className="rec-kpi-note">
            {summary.hasInterval ? 'Est. 30-day cycle' : 'Active monthly rules'}
          </span>
        </div>

        <div className="rec-kpi-card">
          <div className="rec-kpi-top">
            <span className="rec-kpi-label">Active Automations</span>
            <span className="rec-kpi-icon rec-kpi-icon--emerald" aria-hidden="true">
              <Repeat size={16} />
            </span>
          </div>
          <div className="rec-kpi-value">
            {summary.activeCount}
            <span className="rec-kpi-sub"> of {summary.totalCount}</span>
          </div>
          <span className="rec-kpi-note">Auto-posted on visit when due</span>
        </div>
      </section>

      {loading && recurring.length === 0 ? (
        <div className="rec-grid" aria-busy="true" aria-label="Loading recurring rules">
          {[0, 1, 2].map((idx) => (
            <div key={idx} className="rec-skeleton-card skeleton" />
          ))}
        </div>
      ) : recurring.length === 0 ? (
        <div className="rec-empty-card">
          <div className="rec-empty-icon" aria-hidden="true">
            <Repeat size={26} />
          </div>
          <h2>No recurring rules yet</h2>
          <p>
            Automate rent, salaries, and subscriptions so Money Tracker logs and projects them
            automatically.
          </p>
          <button
            type="button"
            className="rec-add-btn"
            onClick={() => setIsFormOpen(true)}
          >
            <Plus size={16} strokeWidth={2.4} aria-hidden="true" />
            <span>Create first automation</span>
          </button>
        </div>
      ) : (
        <div className="rec-grid">
          {recurring.map((item, idx) => {
            const isIncome = item.type === 'income';
            const isInterval = item.frequency_type === 'interval';
            const isActive = item.is_active !== false;
            const intervalDays = Number(item.interval_days) || 30;

            return (
              <motion.article
                key={item.id}
                className={`rec-card ${!isActive ? 'is-paused' : ''}`}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{
                  duration: 0.22,
                  delay: Math.min(idx * 0.03, 0.24),
                  ease: [0.16, 1, 0.3, 1]
                }}
              >
                <div className="rec-card-header">
                  <div className="rec-card-main">
                    <h3>{item.description || item.categories?.name || 'Recurring Transaction'}</h3>
                    <div className="rec-card-meta">
                      <span className="rec-wallet-badge">
                        <span
                          className="rec-wallet-dot"
                          style={{ backgroundColor: item.wallets?.color || '#28C991' }}
                          aria-hidden="true"
                        />
                        <span>{item.wallets?.name || 'No Wallet'}</span>
                      </span>

                      <span className={`rec-type-pill ${isIncome ? 'is-income' : 'is-expense'}`}>
                        {isIncome ? 'Income' : 'Expense'}
                      </span>

                      {item.categories && (
                        <span className="rec-category-pill">
                          <span aria-hidden="true">{item.categories.icon}</span>
                          <span>{item.categories.name}</span>
                        </span>
                      )}
                    </div>
                  </div>

                  <div className={`rec-amount ${isIncome ? 'is-income' : 'is-expense'}`}>
                    {isIncome ? '+' : '-'}
                    {formatCurrency(item.amount)}
                  </div>
                </div>

                <div className="rec-schedule-box">
                  <div className="rec-schedule-row">
                    <CalendarClock size={14} className="rec-schedule-icon" aria-hidden="true" />
                    {isInterval ? (
                      <span>
                        Every <strong>{intervalDays}</strong> {intervalDays === 1 ? 'day' : 'days'}
                        {item.start_date
                          ? ` • from ${formatDate(item.start_date, 'MMM d, yyyy')}`
                          : ''}
                      </span>
                    ) : (
                      <span>
                        Posts on the <strong>{getOrdinal(item.day_of_month)}</strong> of every month
                      </span>
                    )}
                  </div>
                  <div className="rec-schedule-sub">
                    <Clock size={13} aria-hidden="true" />
                    <span>
                      Last posted:{' '}
                      {item.last_posted_date
                        ? formatDate(item.last_posted_date, 'MMM d, yyyy')
                        : 'Never'}
                    </span>
                  </div>
                </div>

                <div className="rec-card-footer">
                  <div className="rec-switch-wrap">
                    <button
                      type="button"
                      role="switch"
                      aria-checked={isActive}
                      aria-label={`Toggle ${item.description || 'recurring rule'} active status`}
                      className={`rec-switch ${isActive ? 'is-active' : ''}`}
                      onClick={() => handleToggleStatus(item)}
                    >
                      <motion.span
                        className="rec-switch-thumb"
                        layout
                        transition={{ type: 'spring', stiffness: 520, damping: 32 }}
                      />
                    </button>
                    <span className={`rec-status-label ${isActive ? 'is-active' : ''}`}>
                      {isActive ? 'Active' : 'Paused'}
                    </span>
                  </div>

                  <div className="rec-card-actions">
                    <button
                      type="button"
                      className="rec-action-btn rec-action-btn--edit"
                      onClick={() => handleEdit(item)}
                      aria-label={`Edit ${item.description || 'recurring rule'}`}
                    >
                      <Pencil size={14} aria-hidden="true" />
                      <span>Edit</span>
                    </button>
                    <button
                      type="button"
                      className="rec-action-btn rec-action-btn--delete"
                      onClick={() => handleDelete(item)}
                      aria-label={`Delete ${item.description || 'recurring rule'}`}
                    >
                      <Trash2 size={14} aria-hidden="true" />
                      <span>Delete</span>
                    </button>
                  </div>
                </div>
              </motion.article>
            );
          })}
        </div>
      )}

      <RecurringForm
        isOpen={isFormOpen}
        recurring={editingItem}
        wallets={wallets}
        categories={categories}
        paymentMethods={paymentMethods}
        onClose={handleCloseForm}
        onSave={async (data) => {
          const result = editingItem
            ? await updateRecurring(editingItem.id, data)
            : await addRecurring(data);

          if (result?.error) {
            showToast(`Error saving recurring rule: ${result.error}`, 'error');
          } else {
            showToast(
              editingItem ? 'Recurring rule updated' : 'Recurring rule created',
              'success'
            );
            handleCloseForm();
          }
        }}
      />
    </div>
  );
};

export default Recurring;
