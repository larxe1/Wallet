import React, { useState, useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { motion } from 'motion/react';
import {
  Plus,
  Search,
  X,
  Pencil,
  Trash2,
  Receipt,
  ArrowUpDown,
  TrendingDown,
  Layers,
  RotateCcw
} from 'lucide-react';
import { format, addMonths } from 'date-fns';
import { useExpenses } from '../hooks/useExpenses';
import { useWallets } from '../hooks/useWallets';
import { useCategories } from '../hooks/useCategories';
import { usePaymentMethods } from '../hooks/usePaymentMethods';
import { useFeedback } from '../context/FeedbackContext';
import { formatCurrency, parseLocalDate, formatDate } from '../utils/format';
import ExpenseForm from '../components/ExpenseForm';
import MonthSelector from '../components/MonthSelector';
import WalletFilter from '../components/WalletFilter';
import MultiSelectDropdown from '../components/MultiSelectDropdown';
import './Expenses.css';

const Expenses = () => {
  const currentDate = new Date();
  const [searchParams, setSearchParams] = useSearchParams();
  const urlQuery = searchParams.get('q') || '';

  const [selectedMonth, setSelectedMonth] = useState(currentDate.getMonth());
  const [selectedYear, setSelectedYear] = useState(currentDate.getFullYear());
  const [selectedWalletId, setSelectedWalletId] = useState(null);
  const [selectedCategoryIds, setSelectedCategoryIds] = useState(new Set());
  const [selectedPaymentMethodIds, setSelectedPaymentMethodIds] = useState(new Set());
  const [searchQuery, setSearchQuery] = useState(urlQuery);
  const [sortOrder, setSortOrder] = useState('date_desc');
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingExpense, setEditingExpense] = useState(null);

  const {
    expenses,
    loading,
    deleteExpense,
    addExpense,
    addMultipleExpenses,
    updateExpense
  } = useExpenses(selectedMonth, selectedYear);
  const { wallets } = useWallets();
  const { categories } = useCategories();
  const { paymentMethods } = usePaymentMethods();
  const { showToast, confirm } = useFeedback();

  useEffect(() => {
    setSearchQuery(urlQuery);
  }, [urlQuery]);

  const walletMap = useMemo(() => {
    const map = new Map();
    wallets.forEach((w) => map.set(w.id, w));
    return map;
  }, [wallets]);

  const categoryMap = useMemo(() => {
    const map = new Map();
    categories.forEach((c) => map.set(c.id, c));
    return map;
  }, [categories]);

  const paymentMethodMap = useMemo(() => {
    const map = new Map();
    paymentMethods.forEach((p) => map.set(p.id, p));
    return map;
  }, [paymentMethods]);

  const handleEdit = (expense) => {
    setEditingExpense(expense);
    setIsFormOpen(true);
  };

  const handleDelete = async (expense) => {
    const cat = categoryMap.get(expense.category_id);
    const label = expense.description || (cat ? cat.name : 'this expense');
    const confirmed = await confirm({
      title: 'Delete Expense',
      message: `Remove "${label}" (${formatCurrency(expense.amount)})? This action cannot be undone.`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      variant: 'danger'
    });
    if (!confirmed) return;

    const result = await deleteExpense(expense.id);
    if (result?.error) {
      showToast(`Failed to delete expense: ${result.error}`, 'error');
    } else {
      showToast('Expense deleted', 'success');
    }
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingExpense(null);
  };

  const handleClearSearch = () => {
    setSearchQuery('');
    if (searchParams.has('q')) {
      const next = new URLSearchParams(searchParams);
      next.delete('q');
      setSearchParams(next, { replace: true });
    }
  };

  const handleResetFilters = () => {
    setSelectedWalletId(null);
    setSelectedCategoryIds(new Set());
    setSelectedPaymentMethodIds(new Set());
    handleClearSearch();
  };

  const hasActiveFilters = Boolean(
    selectedWalletId ||
      selectedCategoryIds.size > 0 ||
      selectedPaymentMethodIds.size > 0 ||
      searchQuery.trim()
  );

  const filteredExpenses = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    const list = expenses.filter((e) => {
      const matchWallet = !selectedWalletId || e.wallet_id === selectedWalletId;
      const matchCategory =
        selectedCategoryIds.size === 0 || selectedCategoryIds.has(e.category_id);
      const matchPaymentMethod =
        selectedPaymentMethodIds.size === 0 ||
        selectedPaymentMethodIds.has(e.payment_method_id);

      if (!matchWallet || !matchCategory || !matchPaymentMethod) return false;

      if (!query) return true;
      const cat = categoryMap.get(e.category_id);
      const wallet = walletMap.get(e.wallet_id);
      const method = paymentMethodMap.get(e.payment_method_id);
      const haystack = [
        e.description,
        cat?.name,
        wallet?.name,
        method?.label,
        String(e.amount),
        formatCurrency(e.amount)
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      return haystack.includes(query);
    });

    list.sort((a, b) => {
      if (sortOrder === 'date_desc') {
        return (b.expense_date || '').localeCompare(a.expense_date || '');
      }
      if (sortOrder === 'date_asc') {
        return (a.expense_date || '').localeCompare(b.expense_date || '');
      }
      if (sortOrder === 'amount_desc') return Number(b.amount) - Number(a.amount);
      if (sortOrder === 'amount_asc') return Number(a.amount) - Number(b.amount);
      return 0;
    });

    return list;
  }, [
    expenses,
    selectedWalletId,
    selectedCategoryIds,
    selectedPaymentMethodIds,
    searchQuery,
    sortOrder,
    categoryMap,
    walletMap,
    paymentMethodMap
  ]);

  const totalAmount = useMemo(
    () => filteredExpenses.reduce((sum, e) => sum + Number(e.amount || 0), 0),
    [filteredExpenses]
  );

  const averageAmount =
    filteredExpenses.length > 0 ? totalAmount / filteredExpenses.length : 0;

  return (
    <div className="expenses-page">
      <header className="exp-header">
        <h1>Expenses</h1>
        <div className="exp-header-actions">
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
          <button
            type="button"
            className="exp-add-btn"
            onClick={() => {
              setEditingExpense(null);
              setIsFormOpen(true);
            }}
          >
            <Plus size={16} strokeWidth={2.4} aria-hidden="true" />
            <span>Add Expense</span>
          </button>
        </div>
      </header>

      <section className="exp-kpi-strip" aria-label="Expense summary">
        <div className="exp-kpi-card">
          <div className="exp-kpi-top">
            <span className="exp-kpi-label">Filtered Total</span>
            <span className="exp-kpi-icon exp-kpi-icon--danger" aria-hidden="true">
              <Receipt size={16} />
            </span>
          </div>
          <div className="exp-kpi-value exp-kpi-value--danger">
            {formatCurrency(totalAmount)}
          </div>
        </div>

        <div className="exp-kpi-card">
          <div className="exp-kpi-top">
            <span className="exp-kpi-label">Entry Count</span>
            <span className="exp-kpi-icon exp-kpi-icon--emerald" aria-hidden="true">
              <Layers size={16} />
            </span>
          </div>
          <div className="exp-kpi-value">
            {filteredExpenses.length}
            {hasActiveFilters && expenses.length !== filteredExpenses.length && (
              <span className="exp-kpi-sub"> of {expenses.length}</span>
            )}
          </div>
        </div>

        <div className="exp-kpi-card">
          <div className="exp-kpi-top">
            <span className="exp-kpi-label">Avg Spend</span>
            <span className="exp-kpi-icon exp-kpi-icon--violet" aria-hidden="true">
              <TrendingDown size={16} />
            </span>
          </div>
          <div className="exp-kpi-value">{formatCurrency(averageAmount)}</div>
        </div>
      </section>

      <section className="exp-filters-panel" aria-label="Expense filters">
        <div className="exp-filters-row">
          <WalletFilter
            wallets={wallets}
            selectedWalletId={selectedWalletId}
            onChange={setSelectedWalletId}
          />
          {hasActiveFilters && (
            <button
              type="button"
              className="exp-reset-btn"
              onClick={handleResetFilters}
            >
              <RotateCcw size={14} aria-hidden="true" />
              <span>Reset filters</span>
            </button>
          )}
        </div>

        <div className="exp-controls-row">
          <div className="exp-search-wrap">
            <Search size={15} className="exp-search-icon" aria-hidden="true" />
            <input
              type="search"
              className="exp-search-input"
              placeholder="Search description, category, wallet..."
              aria-label="Search expenses"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            {searchQuery && (
              <button
                type="button"
                className="exp-search-clear"
                onClick={handleClearSearch}
                aria-label="Clear search"
              >
                <X size={14} />
              </button>
            )}
          </div>

          <MultiSelectDropdown
            options={categories.map((c) => ({ id: c.id, label: `${c.icon} ${c.name}` }))}
            selectedIds={selectedCategoryIds}
            onChange={setSelectedCategoryIds}
            placeholder="All Categories"
          />

          <MultiSelectDropdown
            options={paymentMethods.map((p) => ({ id: p.id, label: p.label }))}
            selectedIds={selectedPaymentMethodIds}
            onChange={setSelectedPaymentMethodIds}
            placeholder="All Payment Methods"
          />

          <div className="exp-sort-wrap">
            <ArrowUpDown size={14} className="exp-sort-icon" aria-hidden="true" />
            <select
              className="exp-sort-select"
              aria-label="Sort expenses"
              value={sortOrder}
              onChange={(e) => setSortOrder(e.target.value)}
            >
              <option value="date_desc">Date (Newest First)</option>
              <option value="date_asc">Date (Oldest First)</option>
              <option value="amount_desc">Amount (Highest First)</option>
              <option value="amount_asc">Amount (Lowest First)</option>
            </select>
          </div>
        </div>
      </section>

      {loading && expenses.length === 0 ? (
        <div className="exp-table-shell">
          <div className="exp-skeleton-list" aria-busy="true" aria-label="Loading expenses">
            {[0, 1, 2, 3, 4].map((row) => (
              <div key={row} className="exp-skeleton-row skeleton" />
            ))}
          </div>
        </div>
      ) : filteredExpenses.length === 0 ? (
        <div className="exp-empty-card">
          <div className="exp-empty-icon" aria-hidden="true">
            <Receipt size={26} />
          </div>
          <h2>No expenses found</h2>
          <p>
            {hasActiveFilters
              ? 'No entries match your active search or filter criteria.'
              : 'No expenses recorded for this month yet.'}
          </p>
          {hasActiveFilters ? (
            <button type="button" className="exp-secondary-btn" onClick={handleResetFilters}>
              Clear filters
            </button>
          ) : (
            <button
              type="button"
              className="exp-add-btn"
              onClick={() => setIsFormOpen(true)}
            >
              <Plus size={16} strokeWidth={2.4} aria-hidden="true" />
              <span>Add first expense</span>
            </button>
          )}
        </div>
      ) : (
        <>
          <div className="exp-table-shell">
            <table className="exp-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Description</th>
                  <th>Category</th>
                  <th>Payment Method</th>
                  <th>Wallet</th>
                  <th className="exp-col-right">Amount</th>
                  <th className="exp-col-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredExpenses.map((expense, idx) => {
                  const cat = categoryMap.get(expense.category_id);
                  const wallet = walletMap.get(expense.wallet_id);
                  const method = paymentMethodMap.get(expense.payment_method_id);

                  return (
                    <motion.tr
                      key={expense.id}
                      initial={{ opacity: 0, y: 6 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{
                        duration: 0.2,
                        delay: Math.min(idx * 0.02, 0.2),
                        ease: [0.16, 1, 0.3, 1]
                      }}
                    >
                      <td className="exp-date-cell">
                        {formatDate(expense.expense_date, 'MMM dd, yyyy')}
                      </td>
                      <td className="exp-desc-cell">
                        {expense.description || (
                          <span className="exp-muted-text">No description</span>
                        )}
                      </td>
                      <td>
                        {cat ? (
                          <span className="exp-category-pill">
                            <span className="exp-category-emoji" aria-hidden="true">
                              {cat.icon}
                            </span>
                            <span>{cat.name}</span>
                          </span>
                        ) : (
                          <span className="exp-muted-text">Unknown</span>
                        )}
                      </td>
                      <td>
                        {method ? (
                          <span className="exp-method-cell">
                            <span className={`exp-pm-tag exp-pm-tag--${method.type || 'cash'}`}>
                              {method.type || 'other'}
                            </span>
                            <span className="exp-method-label">{method.label}</span>
                          </span>
                        ) : (
                          <span className="exp-muted-text">-</span>
                        )}
                      </td>
                      <td>
                        <span className="exp-wallet-pill">
                          <span
                            className="exp-wallet-dot"
                            style={{ backgroundColor: wallet?.color || '#28C991' }}
                            aria-hidden="true"
                          />
                          <span>{wallet ? wallet.name : 'Unknown'}</span>
                        </span>
                      </td>
                      <td className="exp-col-right exp-amount-cell">
                        {formatCurrency(expense.amount)}
                      </td>
                      <td className="exp-col-right">
                        <div className="exp-row-actions">
                          <button
                            type="button"
                            className="exp-icon-btn exp-icon-btn--edit"
                            onClick={() => handleEdit(expense)}
                            aria-label={`Edit ${expense.description || 'expense'}`}
                            title="Edit expense"
                          >
                            <Pencil size={15} />
                          </button>
                          <button
                            type="button"
                            className="exp-icon-btn exp-icon-btn--delete"
                            onClick={() => handleDelete(expense)}
                            aria-label={`Delete ${expense.description || 'expense'}`}
                            title="Delete expense"
                          >
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </td>
                    </motion.tr>
                  );
                })}
              </tbody>
              <tfoot>
                <tr>
                  <td colSpan={5} className="exp-total-label">
                    Total ({filteredExpenses.length}{' '}
                    {filteredExpenses.length === 1 ? 'entry' : 'entries'})
                  </td>
                  <td className="exp-col-right exp-total-value">
                    {formatCurrency(totalAmount)}
                  </td>
                  <td />
                </tr>
              </tfoot>
            </table>
          </div>

          <div className="exp-mobile-list" aria-label="Expenses list">
            {filteredExpenses.map((expense, idx) => {
              const cat = categoryMap.get(expense.category_id);
              const wallet = walletMap.get(expense.wallet_id);
              const method = paymentMethodMap.get(expense.payment_method_id);

              return (
                <motion.article
                  key={expense.id}
                  className="exp-mobile-card"
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{
                    duration: 0.2,
                    delay: Math.min(idx * 0.02, 0.2),
                    ease: [0.16, 1, 0.3, 1]
                  }}
                >
                  <div className="exp-mobile-top">
                    <div className="exp-mobile-title">
                      {expense.description || (
                        <span className="exp-muted-text">No description</span>
                      )}
                    </div>
                    <div className="exp-mobile-amount">{formatCurrency(expense.amount)}</div>
                  </div>

                  <div className="exp-mobile-meta">
                    {cat && (
                      <span className="exp-category-pill">
                        <span className="exp-category-emoji" aria-hidden="true">
                          {cat.icon}
                        </span>
                        <span>{cat.name}</span>
                      </span>
                    )}
                    <span className="exp-wallet-pill">
                      <span
                        className="exp-wallet-dot"
                        style={{ backgroundColor: wallet?.color || '#28C991' }}
                        aria-hidden="true"
                      />
                      <span>{wallet ? wallet.name : 'Unknown'}</span>
                    </span>
                  </div>

                  <div className="exp-mobile-bottom">
                    <div className="exp-mobile-sub">
                      <span>{formatDate(expense.expense_date, 'MMM dd, yyyy')}</span>
                      {method && (
                        <>
                          <span aria-hidden="true">•</span>
                          <span>{method.label}</span>
                        </>
                      )}
                    </div>
                    <div className="exp-row-actions">
                      <button
                        type="button"
                        className="exp-icon-btn exp-icon-btn--edit"
                        onClick={() => handleEdit(expense)}
                        aria-label={`Edit ${expense.description || 'expense'}`}
                      >
                        <Pencil size={15} />
                      </button>
                      <button
                        type="button"
                        className="exp-icon-btn exp-icon-btn--delete"
                        onClick={() => handleDelete(expense)}
                        aria-label={`Delete ${expense.description || 'expense'}`}
                      >
                        <Trash2 size={15} />
                      </button>
                    </div>
                  </div>
                </motion.article>
              );
            })}

            <div className="exp-mobile-total">
              <span>
                Total ({filteredExpenses.length}{' '}
                {filteredExpenses.length === 1 ? 'entry' : 'entries'})
              </span>
              <span className="exp-mobile-total-val">{formatCurrency(totalAmount)}</span>
            </div>
          </div>
        </>
      )}

      <ExpenseForm
        isOpen={isFormOpen}
        expense={editingExpense}
        defaultWalletId={selectedWalletId || undefined}
        onClose={handleCloseForm}
        onSave={async (data) => {
          if (editingExpense) {
            const { is_installment, installment_months, ...updateData } = data;
            const res = await updateExpense(editingExpense.id, updateData);
            if (res?.error) {
              showToast(`Failed to update expense: ${res.error}`, 'error');
              return;
            }
            showToast('Expense updated', 'success');
          } else if (data.is_installment && data.installment_months > 1) {
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
              showToast(`Failed to create installments: ${res.error}`, 'error');
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
          handleCloseForm();
        }}
        wallets={wallets}
        categories={categories}
        paymentMethods={paymentMethods}
      />
    </div>
  );
};

export default Expenses;
