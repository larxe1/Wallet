import React, { useState, useMemo, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import {
  Plus,
  X,
  Trash2,
  Repeat,
  TrendingUp,
  Receipt,
  CalendarDays
} from 'lucide-react';
import { format, addDays, addMonths, isBefore, isSameMonth } from 'date-fns';
import { useExpenses } from '../hooks/useExpenses';
import { useIncome } from '../hooks/useIncome';
import { useWallets } from '../hooks/useWallets';
import { useCategories } from '../hooks/useCategories';
import { usePaymentMethods } from '../hooks/usePaymentMethods';
import { useRecurring } from '../hooks/useRecurring';
import { useFeedback } from '../context/FeedbackContext';
import { formatCurrency, parseLocalDate, formatDate } from '../utils/format';
import CalendarView from '../components/CalendarView';
import ExpenseForm from '../components/ExpenseForm';
import IncomeForm from '../components/IncomeForm';
import MonthSelector from '../components/MonthSelector';
import WalletFilter from '../components/WalletFilter';
import './Calendar.css';

const Calendar = () => {
  const currentDate = new Date();
  const [selectedMonth, setSelectedMonth] = useState(currentDate.getMonth());
  const [selectedYear, setSelectedYear] = useState(currentDate.getFullYear());
  const [selectedWalletId, setSelectedWalletId] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null);

  const [showExpenseForm, setShowExpenseForm] = useState(false);
  const [showIncomeForm, setShowIncomeForm] = useState(false);

  const { expenses, addExpense, addMultipleExpenses, deleteExpense } = useExpenses(
    selectedMonth,
    selectedYear
  );
  const { income, addIncome, deleteIncome } = useIncome(selectedMonth, selectedYear);
  const { recurring } = useRecurring();
  const { wallets } = useWallets();
  const { categories } = useCategories();
  const { paymentMethods } = usePaymentMethods();
  const { showToast, confirm } = useFeedback();

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && selectedDate && !showExpenseForm && !showIncomeForm) {
        setSelectedDate(null);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [selectedDate, showExpenseForm, showIncomeForm]);

  const filteredExpenses = useMemo(
    () => expenses.filter((e) => !selectedWalletId || e.wallet_id === selectedWalletId),
    [expenses, selectedWalletId]
  );

  const filteredIncome = useMemo(
    () => income.filter((i) => !selectedWalletId || i.wallet_id === selectedWalletId),
    [income, selectedWalletId]
  );

  const { projectedExpenses, projectedIncome } = useMemo(() => {
    const daysInMonth = new Date(selectedYear, selectedMonth + 1, 0).getDate();
    const monthStart = new Date(selectedYear, selectedMonth, 1);
    const projExp = [];
    const projInc = [];

    recurring
      .filter((r) => r.is_active && (!selectedWalletId || r.wallet_id === selectedWalletId))
      .forEach((r) => {
        const type = r.frequency_type || 'monthly';
        const occurrences = [];

        if (type === 'monthly') {
          const day = Math.min(Number(r.day_of_month) || 1, daysInMonth);
          occurrences.push(new Date(selectedYear, selectedMonth, day));
        } else if (type === 'interval' && r.start_date && r.interval_days) {
          const step = Math.max(1, Number(r.interval_days) || 1);
          let current = parseLocalDate(r.start_date);

          if (current && !Number.isNaN(current.getTime())) {
            while (isBefore(current, monthStart)) {
              current = addDays(current, step);
            }

            while (isSameMonth(current, monthStart)) {
              occurrences.push(current);
              current = addDays(current, step);
            }
          }
        }

        occurrences.forEach((occDate, idx) => {
          const dateStr = format(occDate, 'yyyy-MM-dd');
          const proj = {
            ...r,
            id: `proj-${r.id}-${idx}`,
            isProjected: true
          };

          if (r.type === 'income') {
            proj.income_date = dateStr;
            projInc.push(proj);
          } else {
            proj.expense_date = dateStr;
            projExp.push(proj);
          }
        });
      });

    return { projectedExpenses: projExp, projectedIncome: projInc };
  }, [recurring, selectedWalletId, selectedMonth, selectedYear]);

  const allExpenses = useMemo(
    () => [...filteredExpenses, ...projectedExpenses],
    [filteredExpenses, projectedExpenses]
  );

  const allIncome = useMemo(
    () => [...filteredIncome, ...projectedIncome],
    [filteredIncome, projectedIncome]
  );

  const selectedDateObj = useMemo(
    () => (selectedDate ? parseLocalDate(selectedDate) : null),
    [selectedDate]
  );

  const dayExpenses = useMemo(
    () =>
      selectedDate
        ? allExpenses.filter(
            (e) => String(e.expense_date || '').substring(0, 10) === selectedDate
          )
        : [],
    [allExpenses, selectedDate]
  );

  const dayIncome = useMemo(
    () =>
      selectedDate
        ? allIncome.filter(
            (i) => String(i.income_date || '').substring(0, 10) === selectedDate
          )
        : [],
    [allIncome, selectedDate]
  );

  const totalDayExpense = dayExpenses.reduce((sum, e) => sum + Number(e.amount || 0), 0);
  const totalDayIncome = dayIncome.reduce((sum, i) => sum + Number(i.amount || 0), 0);
  const netDay = totalDayIncome - totalDayExpense;

  const handleDeleteIncome = async (inc) => {
    const confirmed = await confirm({
      title: 'Delete Income',
      message: `Remove "${inc.description || 'Income'}" (${formatCurrency(inc.amount)})?`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      variant: 'danger'
    });
    if (!confirmed) return;

    const res = await deleteIncome(inc.id);
    if (res?.error) {
      showToast(`Failed to delete income: ${res.error}`, 'error');
    } else {
      showToast('Income deleted', 'success');
    }
  };

  const handleDeleteExpense = async (exp) => {
    const label = exp.description || exp.categories?.name || 'Expense';
    const confirmed = await confirm({
      title: 'Delete Expense',
      message: `Remove "${label}" (${formatCurrency(exp.amount)})?`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      variant: 'danger'
    });
    if (!confirmed) return;

    const res = await deleteExpense(exp.id);
    if (res?.error) {
      showToast(`Failed to delete expense: ${res.error}`, 'error');
    } else {
      showToast('Expense deleted', 'success');
    }
  };

  return (
    <div className="calendar-page">
      <header className="calendar-page-header">
        <h1>Calendar</h1>
        <div className="calendar-page-controls">
          <MonthSelector
            month={selectedMonth}
            year={selectedYear}
            minMonth={7}
            minYear={2026}
            onChange={(m, y) => {
              setSelectedMonth(m);
              setSelectedYear(y);
            }}
          />
        </div>
      </header>

      <section className="calendar-page-filters" aria-label="Calendar filters and actions">
        <WalletFilter
          wallets={wallets}
          selectedWalletId={selectedWalletId}
          onChange={setSelectedWalletId}
        />

        <div className="calendar-page-actions">
          <button
            type="button"
            className="cal-action-btn cal-action-btn--expense"
            onClick={() => setShowExpenseForm(true)}
          >
            <Plus size={15} strokeWidth={2.4} aria-hidden="true" />
            <span>Expense</span>
          </button>
          <button
            type="button"
            className="cal-action-btn cal-action-btn--income"
            onClick={() => setShowIncomeForm(true)}
          >
            <Plus size={15} strokeWidth={2.4} aria-hidden="true" />
            <span>Income</span>
          </button>
        </div>
      </section>

      <div className="calendar-page-layout">
        <div className="calendar-page-main">
          <CalendarView
            month={selectedMonth + 1}
            year={selectedYear}
            expenses={allExpenses}
            income={allIncome}
            onDayClick={(dateStr) => setSelectedDate(dateStr)}
            selectedDate={selectedDate}
          />
        </div>

        <AnimatePresence>
          {selectedDate && selectedDateObj && (
            <motion.aside
              key="day-detail-drawer"
              className="calendar-page-drawer"
              initial={{ opacity: 0, x: 24 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 24 }}
              transition={{ duration: 0.24, ease: [0.16, 1, 0.3, 1] }}
              aria-label={`Transactions for ${formatDate(selectedDate, 'MMMM d, yyyy')}`}
            >
              <div className="cal-drawer-header">
                <div>
                  <h2>{format(selectedDateObj, 'MMM d, yyyy')}</h2>
                  <p className="cal-drawer-weekday">{format(selectedDateObj, 'EEEE')}</p>
                </div>
                <button
                  type="button"
                  className="cal-drawer-close"
                  onClick={() => setSelectedDate(null)}
                  aria-label="Close day details"
                >
                  <X size={16} />
                </button>
              </div>

              <div className="cal-drawer-summary">
                <div className="cal-drawer-row">
                  <span>Income</span>
                  <span className="cal-val-income">+{formatCurrency(totalDayIncome)}</span>
                </div>
                <div className="cal-drawer-row">
                  <span>Expenses</span>
                  <span className="cal-val-expense">-{formatCurrency(totalDayExpense)}</span>
                </div>
                <div className="cal-drawer-row cal-drawer-row--total">
                  <span>Net</span>
                  <span className={netDay >= 0 ? 'cal-val-income' : 'cal-val-expense'}>
                    {netDay >= 0 ? '+' : ''}
                    {formatCurrency(netDay)}
                  </span>
                </div>
              </div>

              <div className="cal-drawer-quick">
                <button
                  type="button"
                  className="cal-drawer-quick-btn cal-drawer-quick-btn--expense"
                  onClick={() => setShowExpenseForm(true)}
                >
                  <Plus size={14} aria-hidden="true" />
                  <span>Add Expense</span>
                </button>
                <button
                  type="button"
                  className="cal-drawer-quick-btn cal-drawer-quick-btn--income"
                  onClick={() => setShowIncomeForm(true)}
                >
                  <Plus size={14} aria-hidden="true" />
                  <span>Add Income</span>
                </button>
              </div>

              <div className="cal-drawer-tx-section">
                <h3>Transactions</h3>
                {dayIncome.length === 0 && dayExpenses.length === 0 ? (
                  <div className="cal-drawer-empty">
                    <CalendarDays size={20} aria-hidden="true" />
                    <p>No transactions recorded for this date.</p>
                  </div>
                ) : (
                  <div className="cal-drawer-tx-list">
                    {dayIncome.map((inc) => (
                      <div
                        key={inc.id}
                        className={`cal-drawer-tx cal-drawer-tx--income ${
                          inc.isProjected ? 'is-projected' : ''
                        }`}
                      >
                        <div className="cal-tx-left">
                          <span className="cal-tx-badge cal-tx-badge--income" aria-hidden="true">
                            {inc.isProjected ? <Repeat size={14} /> : <TrendingUp size={14} />}
                          </span>
                          <div className="cal-tx-text">
                            <span className="cal-tx-desc">{inc.description || 'Income'}</span>
                            <div className="cal-tx-sub">
                              {inc.wallets?.name && <span>{inc.wallets.name}</span>}
                              {inc.isProjected && (
                                <span className="cal-projected-pill">
                                  <Repeat size={10} aria-hidden="true" />
                                  Recurring
                                </span>
                              )}
                            </div>
                          </div>
                        </div>

                        <div className="cal-tx-right">
                          <span className="cal-tx-amount cal-val-income">
                            +{formatCurrency(inc.amount)}
                          </span>
                          {!inc.isProjected && (
                            <button
                              type="button"
                              className="cal-tx-delete"
                              onClick={() => handleDeleteIncome(inc)}
                              aria-label={`Delete ${inc.description || 'income'}`}
                              title="Delete income"
                            >
                              <Trash2 size={14} />
                            </button>
                          )}
                        </div>
                      </div>
                    ))}

                    {dayExpenses.map((exp) => (
                      <div
                        key={exp.id}
                        className={`cal-drawer-tx cal-drawer-tx--expense ${
                          exp.isProjected ? 'is-projected' : ''
                        }`}
                      >
                        <div className="cal-tx-left">
                          <span className="cal-tx-badge cal-tx-badge--expense" aria-hidden="true">
                            {exp.isProjected ? (
                              <Repeat size={14} />
                            ) : exp.categories?.icon ? (
                              <span className="cal-tx-cat-emoji">{exp.categories.icon}</span>
                            ) : (
                              <Receipt size={14} />
                            )}
                          </span>
                          <div className="cal-tx-text">
                            <span className="cal-tx-desc">
                              {exp.description || exp.categories?.name || 'Expense'}
                            </span>
                            <div className="cal-tx-sub">
                              {exp.categories?.name && <span>{exp.categories.name}</span>}
                              {exp.isProjected && (
                                <span className="cal-projected-pill">
                                  <Repeat size={10} aria-hidden="true" />
                                  Recurring
                                </span>
                              )}
                            </div>
                          </div>
                        </div>

                        <div className="cal-tx-right">
                          <span className="cal-tx-amount cal-val-expense">
                            -{formatCurrency(exp.amount)}
                          </span>
                          {!exp.isProjected && (
                            <button
                              type="button"
                              className="cal-tx-delete"
                              onClick={() => handleDeleteExpense(exp)}
                              aria-label={`Delete ${
                                exp.description || exp.categories?.name || 'expense'
                              }`}
                              title="Delete expense"
                            >
                              <Trash2 size={14} />
                            </button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </motion.aside>
          )}
        </AnimatePresence>
      </div>

      <ExpenseForm
        isOpen={showExpenseForm}
        defaultDate={selectedDate || undefined}
        defaultWalletId={selectedWalletId || undefined}
        onClose={() => setShowExpenseForm(false)}
        onSave={async (data) => {
          if (data.is_installment && data.installment_months > 1) {
            const { is_installment, installment_months, ...expenseData } = data;
            const startDate = parseLocalDate(expenseData.expense_date);
            const expensesToCreate = [];

            for (let i = 0; i < installment_months; i++) {
              const installmentDate = format(addMonths(startDate, i), 'yyyy-MM-dd');
              expensesToCreate.push({
                ...expenseData,
                description: `${expenseData.description} (Installment ${i + 1}/${installment_months})`,
                expense_date: installmentDate
              });
            }

            const res = await addMultipleExpenses(expensesToCreate);
            if (res?.error) {
              showToast(`Failed to add installments: ${res.error}`, 'error');
              return;
            }
            showToast(`Added ${installment_months} monthly installments`, 'success');
          } else {
            const { is_installment, installment_months, ...expenseData } = data;
            const res = await addExpense(expenseData);
            if (res?.error) {
              showToast(`Failed to add expense: ${res.error}`, 'error');
              return;
            }
            showToast('Expense added', 'success');
          }
          setShowExpenseForm(false);
        }}
        wallets={wallets}
        categories={categories}
        paymentMethods={paymentMethods}
      />

      <IncomeForm
        isOpen={showIncomeForm}
        defaultDate={selectedDate || undefined}
        defaultWalletId={selectedWalletId || undefined}
        onClose={() => setShowIncomeForm(false)}
        onSave={async (data) => {
          const res = await addIncome(data);
          if (res?.error) {
            showToast(`Failed to add income: ${res.error}`, 'error');
            return;
          }
          showToast('Income added', 'success');
          setShowIncomeForm(false);
        }}
        wallets={wallets}
      />
    </div>
  );
};

export default Calendar;
