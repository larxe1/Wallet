import React, { useState, useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { motion } from 'motion/react';
import {
  Plus,
  Search,
  X,
  Pencil,
  Trash2,
  TrendingUp,
  ArrowUpDown,
  Layers,
  Wallet,
  RotateCcw
} from 'lucide-react';
import { useIncome } from '../hooks/useIncome';
import { useWallets } from '../hooks/useWallets';
import { useFeedback } from '../context/FeedbackContext';
import { formatCurrency, formatDate } from '../utils/format';
import IncomeForm from '../components/IncomeForm';
import MonthSelector from '../components/MonthSelector';
import WalletFilter from '../components/WalletFilter';
import './Income.css';

const Income = () => {
  const currentDate = new Date();
  const [searchParams, setSearchParams] = useSearchParams();
  const urlQuery = searchParams.get('q') || '';

  const [selectedMonth, setSelectedMonth] = useState(currentDate.getMonth());
  const [selectedYear, setSelectedYear] = useState(currentDate.getFullYear());
  const [selectedWalletId, setSelectedWalletId] = useState(null);
  const [searchQuery, setSearchQuery] = useState(urlQuery);
  const [sortOrder, setSortOrder] = useState('date_desc');
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingIncome, setEditingIncome] = useState(null);

  const { income, loading, deleteIncome, addIncome, updateIncome } = useIncome(
    selectedMonth,
    selectedYear
  );
  const { wallets } = useWallets();
  const { showToast, confirm } = useFeedback();

  useEffect(() => {
    setSearchQuery(urlQuery);
  }, [urlQuery]);

  const walletMap = useMemo(() => {
    const map = new Map();
    wallets.forEach((w) => map.set(w.id, w));
    return map;
  }, [wallets]);

  const handleEdit = (inc) => {
    setEditingIncome(inc);
    setIsFormOpen(true);
  };

  const handleDelete = async (inc) => {
    const confirmed = await confirm({
      title: 'Delete Income Record',
      message: `Remove "${inc.description || 'Income'}" (${formatCurrency(inc.amount)})? This action cannot be undone.`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      variant: 'danger'
    });
    if (!confirmed) return;

    const result = await deleteIncome(inc.id);
    if (result?.error) {
      showToast(`Failed to delete income: ${result.error}`, 'error');
    } else {
      showToast('Income record deleted', 'success');
    }
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingIncome(null);
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
    handleClearSearch();
  };

  const hasActiveFilters = Boolean(selectedWalletId || searchQuery.trim());

  const filteredIncome = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    const list = income.filter((inc) => {
      const matchWallet = !selectedWalletId || inc.wallet_id === selectedWalletId;
      if (!matchWallet) return false;
      if (!query) return true;

      const wallet = walletMap.get(inc.wallet_id);
      const haystack = [
        inc.description,
        wallet?.name,
        String(inc.amount),
        formatCurrency(inc.amount)
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return haystack.includes(query);
    });

    list.sort((a, b) => {
      if (sortOrder === 'date_desc') {
        return (b.income_date || '').localeCompare(a.income_date || '');
      }
      if (sortOrder === 'date_asc') {
        return (a.income_date || '').localeCompare(b.income_date || '');
      }
      if (sortOrder === 'amount_desc') return Number(b.amount) - Number(a.amount);
      if (sortOrder === 'amount_asc') return Number(a.amount) - Number(b.amount);
      return 0;
    });

    return list;
  }, [income, selectedWalletId, searchQuery, sortOrder, walletMap]);

  const totalAmount = useMemo(
    () => filteredIncome.reduce((sum, i) => sum + Number(i.amount || 0), 0),
    [filteredIncome]
  );

  const averageAmount =
    filteredIncome.length > 0 ? totalAmount / filteredIncome.length : 0;

  return (
    <div className="income-page">
      <header className="inc-header">
        <h1>Income</h1>
        <div className="inc-header-actions">
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
            className="inc-add-btn"
            onClick={() => {
              setEditingIncome(null);
              setIsFormOpen(true);
            }}
          >
            <Plus size={16} strokeWidth={2.4} aria-hidden="true" />
            <span>Add Income</span>
          </button>
        </div>
      </header>

      <section className="inc-kpi-strip" aria-label="Income summary">
        <div className="inc-kpi-card">
          <div className="inc-kpi-top">
            <span className="inc-kpi-label">Filtered Income</span>
            <span className="inc-kpi-icon inc-kpi-icon--emerald" aria-hidden="true">
              <TrendingUp size={16} />
            </span>
          </div>
          <div className="inc-kpi-value inc-kpi-value--emerald">
            {formatCurrency(totalAmount)}
          </div>
        </div>

        <div className="inc-kpi-card">
          <div className="inc-kpi-top">
            <span className="inc-kpi-label">Deposits</span>
            <span className="inc-kpi-icon inc-kpi-icon--violet" aria-hidden="true">
              <Layers size={16} />
            </span>
          </div>
          <div className="inc-kpi-value">
            {filteredIncome.length}
            {hasActiveFilters && income.length !== filteredIncome.length && (
              <span className="inc-kpi-sub"> of {income.length}</span>
            )}
          </div>
        </div>

        <div className="inc-kpi-card">
          <div className="inc-kpi-top">
            <span className="inc-kpi-label">Avg Deposit</span>
            <span className="inc-kpi-icon inc-kpi-icon--emerald" aria-hidden="true">
              <Wallet size={16} />
            </span>
          </div>
          <div className="inc-kpi-value">{formatCurrency(averageAmount)}</div>
        </div>
      </section>

      <section className="inc-filters-panel" aria-label="Income filters">
        <div className="inc-filters-row">
          <WalletFilter
            wallets={wallets}
            selectedWalletId={selectedWalletId}
            onChange={setSelectedWalletId}
          />
          {hasActiveFilters && (
            <button
              type="button"
              className="inc-reset-btn"
              onClick={handleResetFilters}
            >
              <RotateCcw size={14} aria-hidden="true" />
              <span>Reset filters</span>
            </button>
          )}
        </div>

        <div className="inc-controls-row">
          <div className="inc-search-wrap">
            <Search size={15} className="inc-search-icon" aria-hidden="true" />
            <input
              type="search"
              className="inc-search-input"
              placeholder="Search income description or wallet..."
              aria-label="Search income"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            {searchQuery && (
              <button
                type="button"
                className="inc-search-clear"
                onClick={handleClearSearch}
                aria-label="Clear search"
              >
                <X size={14} />
              </button>
            )}
          </div>

          <div className="inc-sort-wrap">
            <ArrowUpDown size={14} className="inc-sort-icon" aria-hidden="true" />
            <select
              className="inc-sort-select"
              aria-label="Sort income"
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

      {loading && income.length === 0 ? (
        <div className="inc-table-shell">
          <div className="inc-skeleton-list" aria-busy="true" aria-label="Loading income">
            {[0, 1, 2, 3].map((row) => (
              <div key={row} className="inc-skeleton-row skeleton" />
            ))}
          </div>
        </div>
      ) : filteredIncome.length === 0 ? (
        <div className="inc-empty-card">
          <div className="inc-empty-icon" aria-hidden="true">
            <TrendingUp size={26} />
          </div>
          <h2>No income found</h2>
          <p>
            {hasActiveFilters
              ? 'No income records match your active search or wallet filter.'
              : 'No income recorded for this month yet.'}
          </p>
          {hasActiveFilters ? (
            <button type="button" className="inc-secondary-btn" onClick={handleResetFilters}>
              Clear filters
            </button>
          ) : (
            <button
              type="button"
              className="inc-add-btn"
              onClick={() => setIsFormOpen(true)}
            >
              <Plus size={16} strokeWidth={2.4} aria-hidden="true" />
              <span>Add first income</span>
            </button>
          )}
        </div>
      ) : (
        <>
          <div className="inc-table-shell">
            <table className="inc-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Description</th>
                  <th>Wallet</th>
                  <th className="inc-col-right">Amount</th>
                  <th className="inc-col-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredIncome.map((inc, idx) => {
                  const wallet = walletMap.get(inc.wallet_id);

                  return (
                    <motion.tr
                      key={inc.id}
                      initial={{ opacity: 0, y: 6 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{
                        duration: 0.2,
                        delay: Math.min(idx * 0.02, 0.2),
                        ease: [0.16, 1, 0.3, 1]
                      }}
                    >
                      <td className="inc-date-cell">
                        {formatDate(inc.income_date, 'MMM dd, yyyy')}
                      </td>
                      <td className="inc-desc-cell">
                        {inc.description || (
                          <span className="inc-muted-text">No description</span>
                        )}
                      </td>
                      <td>
                        <span className="inc-wallet-pill">
                          <span
                            className="inc-wallet-dot"
                            style={{ backgroundColor: wallet?.color || '#28C991' }}
                            aria-hidden="true"
                          />
                          <span>{wallet ? wallet.name : 'Unknown'}</span>
                        </span>
                      </td>
                      <td className="inc-col-right inc-amount-cell">
                        +{formatCurrency(inc.amount)}
                      </td>
                      <td className="inc-col-right">
                        <div className="inc-row-actions">
                          <button
                            type="button"
                            className="inc-icon-btn inc-icon-btn--edit"
                            onClick={() => handleEdit(inc)}
                            aria-label={`Edit ${inc.description || 'income'}`}
                            title="Edit income"
                          >
                            <Pencil size={15} />
                          </button>
                          <button
                            type="button"
                            className="inc-icon-btn inc-icon-btn--delete"
                            onClick={() => handleDelete(inc)}
                            aria-label={`Delete ${inc.description || 'income'}`}
                            title="Delete income"
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
                  <td colSpan={3} className="inc-total-label">
                    Total ({filteredIncome.length}{' '}
                    {filteredIncome.length === 1 ? 'deposit' : 'deposits'})
                  </td>
                  <td className="inc-col-right inc-total-value">
                    +{formatCurrency(totalAmount)}
                  </td>
                  <td />
                </tr>
              </tfoot>
            </table>
          </div>

          <div className="inc-mobile-list" aria-label="Income list">
            {filteredIncome.map((inc, idx) => {
              const wallet = walletMap.get(inc.wallet_id);

              return (
                <motion.article
                  key={inc.id}
                  className="inc-mobile-card"
                  initial={{ opacity: 0, y: 6 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{
                    duration: 0.2,
                    delay: Math.min(idx * 0.02, 0.2),
                    ease: [0.16, 1, 0.3, 1]
                  }}
                >
                  <div className="inc-mobile-top">
                    <div className="inc-mobile-title">
                      {inc.description || (
                        <span className="inc-muted-text">No description</span>
                      )}
                    </div>
                    <div className="inc-mobile-amount">+{formatCurrency(inc.amount)}</div>
                  </div>

                  <div className="inc-mobile-bottom">
                    <div className="inc-mobile-meta">
                      <span className="inc-wallet-pill">
                        <span
                          className="inc-wallet-dot"
                          style={{ backgroundColor: wallet?.color || '#28C991' }}
                          aria-hidden="true"
                        />
                        <span>{wallet ? wallet.name : 'Unknown'}</span>
                      </span>
                      <span className="inc-mobile-date">
                        {formatDate(inc.income_date, 'MMM dd, yyyy')}
                      </span>
                    </div>

                    <div className="inc-row-actions">
                      <button
                        type="button"
                        className="inc-icon-btn inc-icon-btn--edit"
                        onClick={() => handleEdit(inc)}
                        aria-label={`Edit ${inc.description || 'income'}`}
                      >
                        <Pencil size={15} />
                      </button>
                      <button
                        type="button"
                        className="inc-icon-btn inc-icon-btn--delete"
                        onClick={() => handleDelete(inc)}
                        aria-label={`Delete ${inc.description || 'income'}`}
                      >
                        <Trash2 size={15} />
                      </button>
                    </div>
                  </div>
                </motion.article>
              );
            })}

            <div className="inc-mobile-total">
              <span>
                Total ({filteredIncome.length}{' '}
                {filteredIncome.length === 1 ? 'deposit' : 'deposits'})
              </span>
              <span className="inc-mobile-total-val">+{formatCurrency(totalAmount)}</span>
            </div>
          </div>
        </>
      )}

      <IncomeForm
        isOpen={isFormOpen}
        income={editingIncome}
        defaultWalletId={selectedWalletId || undefined}
        onClose={handleCloseForm}
        onSave={async (data) => {
          if (editingIncome) {
            const res = await updateIncome(editingIncome.id, data);
            if (res?.error) {
              showToast(`Failed to update income: ${res.error}`, 'error');
              return;
            }
            showToast('Income updated', 'success');
          } else {
            const res = await addIncome(data);
            if (res?.error) {
              showToast(`Failed to add income: ${res.error}`, 'error');
              return;
            }
            showToast('Income added', 'success');
          }
          handleCloseForm();
        }}
        wallets={wallets}
      />
    </div>
  );
};

export default Income;
