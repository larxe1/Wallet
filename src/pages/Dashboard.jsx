import React, { useState, useEffect, useMemo, useRef } from 'react';
import { Link } from 'react-router-dom';
import { format, addMonths, addDays, getDaysInMonth, isSameDay } from 'date-fns';
import { motion, AnimatePresence, useReducedMotion } from 'motion/react';
import {
  Eye,
  EyeOff,
  SlidersHorizontal,
  ChevronRight,
  ArrowDownLeft,
  ArrowUpRight,
  Receipt,
  Repeat,
  TrendingUp,
  Layers,
  AlertCircle,
  RefreshCw,
  Plus,
  CalendarClock,
} from 'lucide-react';
import { useDashboard } from '../hooks/useDashboard';
import { useExpenses } from '../hooks/useExpenses';
import { useIncome } from '../hooks/useIncome';
import { useRecurring } from '../hooks/useRecurring';
import { useWallets } from '../hooks/useWallets';
import { useCategories } from '../hooks/useCategories';
import { usePaymentMethods } from '../hooks/usePaymentMethods';
import { useFeedback } from '../context/FeedbackContext';
import { formatCurrency, formatDate, parseLocalDate } from '../utils/format';
import WalletCardStack from '../components/WalletCardStack';
import SpendingChart from '../components/SpendingChart';
import TrendChart from '../components/TrendChart';
import CategoryPieChart from '../components/CategoryPieChart';
import AnimatedNumber from '../components/AnimatedNumber';
import MonthSelector from '../components/MonthSelector';
import CategoryFilter from '../components/CategoryFilter';
import ExpenseForm from '../components/ExpenseForm';
import IncomeForm from '../components/IncomeForm';
import './Dashboard.css';

function startOfLocalDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function computeNextRecurringDate(item, todayStart) {
  const todayYmd = format(todayStart, 'yyyy-MM-dd');

  if (item.frequency_type === 'interval' && item.start_date && item.interval_days > 0) {
    let cursor = startOfLocalDay(parseLocalDate(item.start_date));
    const step = Math.max(1, Number(item.interval_days) || 1);
    let safety = 0;
    while (
      (cursor < todayStart ||
        (isSameDay(cursor, todayStart) && item.last_posted_date === todayYmd)) &&
      safety < 2000
    ) {
      cursor = addDays(cursor, step);
      safety += 1;
    }
    return cursor;
  }

  const dom = Math.max(1, Number(item.day_of_month) || 1);
  const daysInCurMonth = getDaysInMonth(todayStart);
  let candidate = new Date(
    todayStart.getFullYear(),
    todayStart.getMonth(),
    Math.min(dom, daysInCurMonth)
  );

  if (
    candidate < todayStart ||
    (isSameDay(candidate, todayStart) && item.last_posted_date === todayYmd)
  ) {
    const nextMonthBase = addMonths(
      new Date(todayStart.getFullYear(), todayStart.getMonth(), 1),
      1
    );
    const daysInNext = getDaysInMonth(nextMonthBase);
    candidate = new Date(
      nextMonthBase.getFullYear(),
      nextMonthBase.getMonth(),
      Math.min(dom, daysInNext)
    );
  }
  return candidate;
}

function buildSparklineGeometry(trendData) {
  const width = 220;
  const height = 66;
  const padX = 8;
  const padTop = 10;
  const padBottom = 10;
  const values = (trendData || []).map((d) => Math.max(0, Number(d.total) || 0));

  if (values.length === 0) {
    const linePath = `M ${padX} ${height - 14} L ${width - padX} ${height - 14}`;
    const areaPath = `${linePath} L ${width - padX} ${height} L ${padX} ${height} Z`;
    return {
      linePath,
      areaPath,
      endX: width - padX,
      endY: height - 14,
      hasData: false,
    };
  }

  const maxVal = Math.max(...values);
  const minVal = Math.min(...values);
  const span = maxVal - minVal;
  const plotW = width - padX * 2;
  const plotH = height - padTop - padBottom;

  const points = values.map((val, idx) => {
    const x =
      values.length === 1
        ? width / 2
        : padX + (idx / (values.length - 1)) * plotW;
    const norm = maxVal === 0 ? 0 : span === 0 ? 0.5 : (val - minVal) / span;
    const y = padTop + plotH - norm * plotH;
    return { x, y };
  });

  if (points.length === 1) {
    const p = points[0];
    const linePath = `M ${padX} ${p.y} L ${width - padX} ${p.y}`;
    const areaPath = `${linePath} L ${width - padX} ${height} L ${padX} ${height} Z`;
    return { linePath, areaPath, endX: width - padX, endY: p.y, hasData: maxVal > 0 };
  }

  let linePath = `M ${points[0].x.toFixed(1)} ${points[0].y.toFixed(1)}`;
  for (let i = 0; i < points.length - 1; i++) {
    const curr = points[i];
    const next = points[i + 1];
    const midX = ((curr.x + next.x) / 2).toFixed(1);
    linePath += ` C ${midX} ${curr.y.toFixed(1)}, ${midX} ${next.y.toFixed(1)}, ${next.x.toFixed(1)} ${next.y.toFixed(1)}`;
  }

  const last = points[points.length - 1];
  const areaPath = `${linePath} L ${last.x.toFixed(1)} ${height} L ${points[0].x.toFixed(1)} ${height} Z`;

  return {
    linePath,
    areaPath,
    endX: last.x,
    endY: last.y,
    hasData: maxVal > 0,
  };
}

const CHART_VIEWS = [
  { id: 'trend', label: 'Flow' },
  { id: 'categories', label: 'Categories' },
  { id: 'averages', label: 'Averages' },
];

const Dashboard = () => {
  const currentDate = new Date();
  const [selectedMonth, setSelectedMonth] = useState(currentDate.getMonth());
  const [selectedYear, setSelectedYear] = useState(currentDate.getFullYear());
  const [timeframe, setTimeframe] = useState('monthly'); // 'monthly' | 'annual'
  const [isPrivacyMode, setIsPrivacyMode] = useState(true);
  const [excludedCategoryIds, setExcludedCategoryIds] = useState(new Set());
  const [selectedWalletId, setSelectedWalletId] = useState('all');
  const [activeChartView, setActiveChartView] = useState('trend'); // 'trend' | 'categories' | 'averages'
  const [isCatFilterOpen, setIsCatFilterOpen] = useState(false);

  const [quickAddType, setQuickAddType] = useState(null); // 'expense' | 'income' | null
  const [quickAddWalletId, setQuickAddWalletId] = useState(null);

  const catPopoverRef = useRef(null);
  const shouldReduceMotion = useReducedMotion();

  const { fetchDashboardData, data: dashboardData, loading, error } = useDashboard();
  const {
    expenses,
    fetchExpenses,
    addExpense,
    addMultipleExpenses,
  } = useExpenses(selectedMonth, selectedYear);
  const { income, fetchIncome, addIncome } = useIncome(selectedMonth, selectedYear);
  const { recurring } = useRecurring();
  const { wallets } = useWallets();
  const { categories } = useCategories();
  const { paymentMethods } = usePaymentMethods();
  const { showToast } = useFeedback();

  useEffect(() => {
    fetchDashboardData(selectedMonth, selectedYear, timeframe);
  }, [selectedMonth, selectedYear, timeframe, fetchDashboardData]);

  useEffect(() => {
    if (!isCatFilterOpen) return;
    const handleClickOutside = (e) => {
      if (catPopoverRef.current && !catPopoverRef.current.contains(e.target)) {
        setIsCatFilterOpen(false);
      }
    };
    const handleEsc = (e) => {
      if (e.key === 'Escape') setIsCatFilterOpen(false);
    };
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEsc);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEsc);
    };
  }, [isCatFilterOpen]);

  const walletsData = dashboardData?.walletsData || [];

  const activeWalletEntry = useMemo(
    () => walletsData.find((w) => w.wallet.id === selectedWalletId) || null,
    [walletsData, selectedWalletId]
  );

  const effectiveWalletId = activeWalletEntry ? activeWalletEntry.wallet.id : 'all';

  const visibleWalletIds = useMemo(
    () => new Set(walletsData.map((w) => w.wallet.id)),
    [walletsData]
  );

  const aggregated = useMemo(() => {
    const allTotalIncome = walletsData.reduce(
      (sum, w) => sum + (Number(w.totalIncome) || 0),
      0
    );
    const allTotalExpenses = walletsData.reduce(
      (sum, w) => sum + (Number(w.totalExpenses) || 0),
      0
    );

    if (activeWalletEntry) {
      return {
        walletName: activeWalletEntry.wallet.name,
        totalIncome: Number(activeWalletEntry.totalIncome) || 0,
        totalExpenses: Number(activeWalletEntry.totalExpenses) || 0,
        allTotalIncome,
        allTotalExpenses,
        rawCategoryExpenses: activeWalletEntry.expenses || [],
        trendData: activeWalletEntry.trendData || [],
        averages: activeWalletEntry.averages || [],
      };
    }

    const catMap = {};
    walletsData.forEach((w) => {
      (w.expenses || []).forEach((exp) => {
        const key = exp.category_id || exp.categoryName || exp.name;
        if (!catMap[key]) {
          catMap[key] = { ...exp, totalAmount: 0, value: 0 };
        }
        catMap[key].totalAmount += Number(exp.totalAmount ?? exp.value) || 0;
        catMap[key].value += Number(exp.value ?? exp.totalAmount) || 0;
      });
    });

    const combinedExpenses = Object.values(catMap).sort((a, b) => b.value - a.value);

    const templateTrend = walletsData[0]?.trendData || [];
    const combinedTrend = templateTrend.map((point, idx) => ({
      label: point.label,
      total: walletsData.reduce(
        (sum, w) => sum + (Number(w.trendData?.[idx]?.total) || 0),
        0
      ),
    }));

    const avgMap = {};
    walletsData.forEach((w) => {
      (w.averages || []).forEach((avg) => {
        const key = avg.category_id || avg.category_name;
        if (!avgMap[key]) {
          avgMap[key] = { ...avg, amount: 0 };
        }
        avgMap[key].amount += Number(avg.amount) || 0;
      });
    });
    const combinedAverages = Object.values(avgMap)
      .sort((a, b) => b.amount - a.amount)
      .slice(0, 5);

    return {
      walletName: 'All Wallets',
      totalIncome: allTotalIncome,
      totalExpenses: allTotalExpenses,
      allTotalIncome,
      allTotalExpenses,
      rawCategoryExpenses: combinedExpenses,
      trendData: combinedTrend,
      averages: combinedAverages,
    };
  }, [walletsData, activeWalletEntry]);

  const filteredCategoryExpenses = useMemo(
    () =>
      aggregated.rawCategoryExpenses.filter(
        (item) => !excludedCategoryIds.has(item.category_id)
      ),
    [aggregated.rawCategoryExpenses, excludedCategoryIds]
  );

  // Recent combined transactions for selected month & wallet
  const recentTransactions = useMemo(() => {
    const matchWallet = (wId) =>
      effectiveWalletId === 'all'
        ? visibleWalletIds.size === 0 || visibleWalletIds.has(wId)
        : wId === effectiveWalletId;

    const expRows = (expenses || [])
      .filter((e) => matchWallet(e.wallet_id))
      .map((e) => ({
        id: `exp-${e.id}`,
        type: 'expense',
        amount: Number(e.amount) || 0,
        description: e.description || e.categories?.name || 'Expense',
        dateStr: e.expense_date,
        createdAt: e.created_at || '',
        categoryIcon: e.categories?.icon,
        walletName: e.wallets?.name || '',
      }));

    const incRows = (income || [])
      .filter((i) => matchWallet(i.wallet_id))
      .map((i) => ({
        id: `inc-${i.id}`,
        type: 'income',
        amount: Number(i.amount) || 0,
        description: i.description || 'Income',
        dateStr: i.income_date,
        createdAt: i.created_at || '',
        categoryIcon: null,
        walletName: i.wallets?.name || '',
      }));

    return [...expRows, ...incRows]
      .sort((a, b) => {
        const diff =
          parseLocalDate(b.dateStr).getTime() - parseLocalDate(a.dateStr).getTime();
        if (diff !== 0) return diff;
        return String(b.createdAt).localeCompare(String(a.createdAt));
      })
      .slice(0, 5);
  }, [expenses, income, effectiveWalletId, visibleWalletIds]);

  // Recurring summary widget
  const recurringSummary = useMemo(() => {
    const todayStart = startOfLocalDay(new Date());
    const activeItems = (recurring || []).filter((r) => {
      if (!r.is_active) return false;
      if (effectiveWalletId === 'all') {
        return visibleWalletIds.size === 0 || visibleWalletIds.has(r.wallet_id);
      }
      return r.wallet_id === effectiveWalletId;
    });

    const withNext = activeItems
      .map((item) => ({
        ...item,
        nextDate: computeNextRecurringDate(item, todayStart),
      }))
      .sort((a, b) => a.nextDate.getTime() - b.nextDate.getTime());

    return {
      activeCount: activeItems.length,
      nextItem: withNext[0] || null,
      todayStart,
    };
  }, [recurring, effectiveWalletId, visibleWalletIds]);

  // Spending Pace sparkline & delta
  const paceSummary = useMemo(() => {
    const geom = buildSparklineGeometry(aggregated.trendData);
    const series = aggregated.trendData || [];
    const latest = series.length > 0 ? Number(series[series.length - 1]?.total) || 0 : 0;
    const prev = series.length > 1 ? Number(series[series.length - 2]?.total) || 0 : 0;
    let deltaText = 'Steady';
    let deltaTone = 'neutral';

    if (prev > 0) {
      const pct = Math.round(((latest - prev) / prev) * 100);
      if (pct > 0) {
        deltaText = `+${pct}%`;
        deltaTone = 'up';
      } else if (pct < 0) {
        deltaText = `${pct}%`;
        deltaTone = 'down';
      }
    } else if (latest > 0) {
      deltaText = 'Active';
      deltaTone = 'up';
    }

    return { ...geom, latest, deltaText, deltaTone };
  }, [aggregated.trendData]);

  // KPI Pill Badges
  const incomeBadgeText = useMemo(() => {
    if (effectiveWalletId === 'all') {
      if (aggregated.totalIncome <= 0) return 'All-time';
      if (aggregated.totalIncome >= aggregated.totalExpenses) {
        const netRate = Math.round(
          ((aggregated.totalIncome - aggregated.totalExpenses) / aggregated.totalIncome) * 100
        );
        return `+${netRate}% net`;
      }
      return 'Cumulative';
    }
    const share =
      aggregated.allTotalIncome > 0
        ? Math.round((aggregated.totalIncome / aggregated.allTotalIncome) * 100)
        : 0;
    return `${share}% share`;
  }, [effectiveWalletId, aggregated]);

  const expenseBadgeText = useMemo(() => {
    if (aggregated.totalIncome > 0) {
      const ratio = Math.round(
        (aggregated.totalExpenses / aggregated.totalIncome) * 100
      );
      return `${ratio}% of income`;
    }
    return 'Outflow';
  }, [aggregated]);

  const defaultQuickDate = useMemo(() => {
    const now = new Date();
    if (selectedMonth === now.getMonth() && selectedYear === now.getFullYear()) {
      return format(now, 'yyyy-MM-dd');
    }
    return format(new Date(selectedYear, selectedMonth, 1), 'yyyy-MM-dd');
  }, [selectedMonth, selectedYear]);

  const handleQuickAdd = (type, walletId) => {
    setQuickAddType(type);
    setQuickAddWalletId(walletId && walletId !== 'all' ? walletId : null);
  };

  const handleSaveQuickExpense = async (formData) => {
    try {
      let res;
      if (formData.is_installment && formData.installment_months > 1) {
        const { is_installment, installment_months, ...expenseData } = formData;
        const startDate = parseLocalDate(expenseData.expense_date);
        const batch = [];
        for (let i = 0; i < installment_months; i++) {
          batch.push({
            ...expenseData,
            description: `${expenseData.description || 'Expense'} (Installment ${i + 1}/${installment_months})`,
            expense_date: format(addMonths(startDate, i), 'yyyy-MM-dd'),
          });
        }
        res = await addMultipleExpenses(batch);
        if (res?.error) {
          showToast(res.error, 'error');
          return;
        }
        showToast(
          `Added ${installment_months}-month installment plan`,
          'success'
        );
      } else {
        const { is_installment, installment_months, ...expenseData } = formData;
        res = await addExpense(expenseData);
        if (res?.error) {
          showToast(res.error, 'error');
          return;
        }
        showToast('Expense added', 'success');
      }

      setQuickAddType(null);
      setQuickAddWalletId(null);
      await Promise.all([
        fetchDashboardData(selectedMonth, selectedYear, timeframe),
        fetchExpenses({ month: selectedMonth + 1, year: selectedYear }),
      ]);
    } catch (err) {
      showToast(err.message || 'Failed to save expense', 'error');
    }
  };

  const handleSaveQuickIncome = async (formData) => {
    try {
      const res = await addIncome(formData);
      if (res?.error) {
        showToast(res.error, 'error');
        return;
      }
      showToast('Income added', 'success');
      setQuickAddType(null);
      setQuickAddWalletId(null);
      await Promise.all([
        fetchDashboardData(selectedMonth, selectedYear, timeframe),
        fetchIncome({ month: selectedMonth + 1, year: selectedYear }),
      ]);
    } catch (err) {
      showToast(err.message || 'Failed to save income', 'error');
    }
  };

  const formatDueBadge = (nextDate) => {
    if (!nextDate) return '';
    if (isSameDay(nextDate, recurringSummary.todayStart)) return 'Today';
    if (isSameDay(nextDate, addDays(recurringSummary.todayStart, 1))) return 'Tomorrow';
    return formatDate(nextDate, 'MMM d');
  };

  // Error State
  if (error && !dashboardData) {
    return (
      <div className="dash-state-card" role="alert">
        <div className="dash-error-icon" aria-hidden="true">
          <AlertCircle size={26} />
        </div>
        <h2>Unable to load dashboard</h2>
        <p>{error}</p>
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => fetchDashboardData(selectedMonth, selectedYear, timeframe)}
        >
          <RefreshCw size={15} />
          <span>Retry</span>
        </button>
      </div>
    );
  }

  // Initial Skeleton State
  if (loading && !dashboardData) {
    return (
      <div className="dash-page" aria-busy="true" aria-label="Loading dashboard">
        <div className="dash-shell-grid">
          <div className="dash-left-col">
            <div className="dash-header-row">
              <div className="skeleton" style={{ width: 190, height: 34 }} />
              <div className="skeleton" style={{ width: 320, height: 38, borderRadius: 999 }} />
            </div>
            <div className="skeleton" style={{ width: '100%', height: 42, borderRadius: 999 }} />
            <div className="skeleton" style={{ width: '100%', height: 332, borderRadius: 22 }} />
            <div className="dash-bottom-split">
              <div className="skeleton" style={{ width: '100%', height: 236, borderRadius: 22 }} />
              <div className="dash-kpi-stack">
                <div className="skeleton" style={{ width: '100%', height: 111, borderRadius: 20 }} />
                <div className="skeleton" style={{ width: '100%', height: 111, borderRadius: 20 }} />
              </div>
            </div>
          </div>
          <div className="dash-right-col">
            <div className="skeleton" style={{ width: '100%', height: 270, borderRadius: 22 }} />
            <div className="skeleton" style={{ width: '100%', height: 290, borderRadius: 22 }} />
            <div className="dash-mini-grid">
              <div className="skeleton" style={{ width: '100%', height: 132, borderRadius: 18 }} />
              <div className="skeleton" style={{ width: '100%', height: 132, borderRadius: 18 }} />
            </div>
          </div>
        </div>
      </div>
    );
  }

  const maxAverageAmount = Math.max(
    ...(aggregated.averages || []).map((a) => Number(a.amount) || 0),
    1
  );

  const chartCardTitle =
    activeChartView === 'trend'
      ? 'Cashflow Flow'
      : activeChartView === 'categories'
        ? 'Category Spend'
        : 'Category Averages';

  const chartPeriodCaption =
    activeChartView === 'trend'
      ? timeframe === 'monthly'
        ? '6-month trend'
        : `${selectedYear} monthly`
      : activeChartView === 'averages'
        ? timeframe === 'monthly'
          ? 'Prior 3-mo avg'
          : `${selectedYear} 12-mo avg`
        : timeframe === 'monthly'
          ? format(new Date(selectedYear, selectedMonth, 1), 'MMMM yyyy')
          : `${selectedYear} total`;

  return (
    <div className="dash-page">
      <div className="dash-shell-grid">
        {/* LEFT COLUMN: My Dashboard */}
        <div className="dash-left-col">
          {/* Top Header Row */}
          <header className="dash-header-row">
            <div className="dash-title-group">
              <h1 className="dash-page-title">My Dashboard</h1>
              {loading && (
                <span className="dash-sync-dot" title="Syncing latest data" aria-label="Updating" />
              )}
            </div>

            <div className="dash-header-controls">
              <button
                type="button"
                className={`dash-privacy-btn ${isPrivacyMode ? 'is-masked' : ''}`}
                onClick={() => setIsPrivacyMode((prev) => !prev)}
                aria-pressed={isPrivacyMode}
                aria-label={isPrivacyMode ? 'Show balances' : 'Hide balances'}
                title={isPrivacyMode ? 'Show balances' : 'Hide balances'}
              >
                {isPrivacyMode ? <EyeOff size={15} /> : <Eye size={15} />}
                <span>{isPrivacyMode ? 'Private' : 'Visible'}</span>
              </button>

              <MonthSelector
                month={selectedMonth}
                year={selectedYear}
                minMonth={7}
                minYear={2026}
                hideMonth={timeframe === 'annual'}
                onChange={(m, y) => {
                  setSelectedMonth(m);
                  setSelectedYear(y);
                }}
              />

              <div className="dash-timeframe-switch" role="group" aria-label="Timeframe">
                {['monthly', 'annual'].map((mode) => {
                  const isActive = timeframe === mode;
                  return (
                    <button
                      key={mode}
                      type="button"
                      className={`dash-timeframe-btn ${isActive ? 'active' : ''}`}
                      aria-pressed={isActive}
                      onClick={() => setTimeframe(mode)}
                    >
                      {isActive && (
                        <motion.span
                          layoutId="dash-timeframe-pill"
                          className="dash-timeframe-pill"
                          transition={
                            shouldReduceMotion
                              ? { duration: 0.01 }
                              : { type: 'spring', stiffness: 420, damping: 32 }
                          }
                        />
                      )}
                      <span className="dash-timeframe-label">
                        {mode === 'monthly' ? 'Monthly' : 'Annual'}
                      </span>
                    </button>
                  );
                })}
              </div>

              <div className="dash-cat-filter-anchor" ref={catPopoverRef}>
                <button
                  type="button"
                  className={`dash-cat-trigger ${isCatFilterOpen || excludedCategoryIds.size > 0 ? 'has-active' : ''}`}
                  onClick={() => setIsCatFilterOpen((prev) => !prev)}
                  aria-expanded={isCatFilterOpen}
                  aria-haspopup="dialog"
                  aria-label="Filter categories"
                >
                  <SlidersHorizontal size={14} />
                  <span>Categories</span>
                  {excludedCategoryIds.size > 0 && (
                    <span className="dash-cat-badge">{excludedCategoryIds.size}</span>
                  )}
                </button>

                <AnimatePresence>
                  {isCatFilterOpen && (
                    <motion.div
                      className="dash-cat-popover"
                      role="dialog"
                      aria-label="Category filters"
                      initial={{ opacity: 0, y: -6, scale: 0.97 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, y: -6, scale: 0.97 }}
                      transition={{ duration: 0.16, ease: [0.16, 1, 0.3, 1] }}
                    >
                      <CategoryFilter
                        categories={categories}
                        excludedIds={excludedCategoryIds}
                        onChange={setExcludedCategoryIds}
                      />
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>
            </div>
          </header>

          {/* Wallet Pill Selector Bar */}
          <nav className="dash-wallet-pills" aria-label="Filter dashboard by wallet">
            <button
              type="button"
              className={`dash-wallet-pill ${effectiveWalletId === 'all' ? 'active' : ''}`}
              aria-pressed={effectiveWalletId === 'all'}
              onClick={() => setSelectedWalletId('all')}
            >
              {effectiveWalletId === 'all' && (
                <motion.div
                  layoutId="dash-wallet-pill"
                  className="dash-wallet-pill-bg"
                  transition={
                    shouldReduceMotion
                      ? { duration: 0.01 }
                      : { type: 'spring', stiffness: 400, damping: 30 }
                  }
                />
              )}
              <span className="dash-wallet-pill-content">
                <Layers size={14} aria-hidden="true" />
                <span>All Wallets</span>
              </span>
            </button>

            {walletsData.map((wItem) => {
              const w = wItem.wallet;
              const isActive = effectiveWalletId === w.id;
              return (
                <button
                  key={w.id}
                  type="button"
                  className={`dash-wallet-pill ${isActive ? 'active' : ''}`}
                  aria-pressed={isActive}
                  onClick={() => setSelectedWalletId(w.id)}
                >
                  {isActive && (
                    <motion.div
                      layoutId="dash-wallet-pill"
                      className="dash-wallet-pill-bg"
                      transition={
                        shouldReduceMotion
                          ? { duration: 0.01 }
                          : { type: 'spring', stiffness: 400, damping: 30 }
                      }
                    />
                  )}
                  <span className="dash-wallet-pill-content">
                    <span
                      className="dash-wallet-dot"
                      style={{ backgroundColor: isActive ? '#051A13' : w.color || '#28C991' }}
                    />
                    <span>{w.name}</span>
                  </span>
                </button>
              );
            })}
          </nav>

          {/* Primary Chart Card (NO double-nested glass cards) */}
          <section className="dash-chart-card" aria-label="Cashflow and spending overview">
            <div className="dash-chart-header">
              <div className="dash-chart-title-block">
                <h2 className="dash-chart-title">{chartCardTitle}</h2>
                <span className="dash-chart-caption">{chartPeriodCaption}</span>
              </div>

              <div className="dash-chart-header-right">
                <div className="dash-chart-tabs" role="tablist" aria-label="Chart view">
                  {CHART_VIEWS.map((tab) => {
                    const isActive = activeChartView === tab.id;
                    return (
                      <button
                        key={tab.id}
                        type="button"
                        role="tab"
                        aria-selected={isActive}
                        className={`dash-chart-tab ${isActive ? 'active' : ''}`}
                        onClick={() => setActiveChartView(tab.id)}
                      >
                        {isActive && (
                          <motion.span
                            layoutId="dash-chart-view"
                            className="dash-chart-tab-bg"
                            transition={
                              shouldReduceMotion
                                ? { duration: 0.01 }
                                : { type: 'spring', stiffness: 420, damping: 32 }
                            }
                          />
                        )}
                        <span className="dash-chart-tab-label">{tab.label}</span>
                      </button>
                    );
                  })}
                </div>

                <Link to="/expenses" className="dash-section-link">
                  <span>View All</span>
                  <ChevronRight size={14} />
                </Link>
              </div>
            </div>

            <div className="dash-chart-body">
              {activeChartView === 'trend' && (
                <TrendChart
                  data={aggregated.trendData}
                  isPrivacyMode={isPrivacyMode}
                />
              )}

              {activeChartView === 'categories' && (
                <SpendingChart
                  data={filteredCategoryExpenses}
                  isPrivacyMode={isPrivacyMode}
                  hasActiveFilter={excludedCategoryIds.size > 0}
                  onResetFilter={() => setExcludedCategoryIds(new Set())}
                />
              )}

              {activeChartView === 'averages' && (
                <div className="dash-averages-wrap">
                  {aggregated.averages.length === 0 ? (
                    <div className="dash-averages-empty">
                      No historical spending data for{' '}
                      {timeframe === 'monthly' ? 'prior 3 months' : `${selectedYear}`}.
                    </div>
                  ) : (
                    <table className="dash-averages-table">
                      <thead>
                        <tr>
                          <th>Category</th>
                          <th className="dash-avg-track-col">Share</th>
                          <th className="dash-avg-val-col">Monthly Avg</th>
                        </tr>
                      </thead>
                      <tbody>
                        {aggregated.averages.map((avg, idx) => {
                          const pct = Math.max(
                            6,
                            Math.round(((Number(avg.amount) || 0) / maxAverageAmount) * 100)
                          );
                          return (
                            <tr key={avg.category_id || avg.category_name}>
                              <td className="dash-avg-name">{avg.category_name}</td>
                              <td className="dash-avg-track-col">
                                <div className="dash-avg-track" aria-hidden="true">
                                  <motion.div
                                    className="dash-avg-fill"
                                    initial={
                                      shouldReduceMotion
                                        ? { width: `${pct}%` }
                                        : { width: '0%' }
                                    }
                                    animate={{ width: `${pct}%` }}
                                    transition={
                                      shouldReduceMotion
                                        ? { duration: 0.01 }
                                        : {
                                            duration: 0.45,
                                            delay: idx * 0.04,
                                            ease: [0.16, 1, 0.3, 1],
                                          }
                                    }
                                  />
                                </div>
                              </td>
                              <td className="dash-avg-val-col">
                                {formatCurrency(avg.amount, { masked: isPrivacyMode })}
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  )}
                </div>
              )}
            </div>
          </section>

          {/* Bottom Split Row: Category Donut (1.15fr) + Stacked KPI Cards (0.85fr) */}
          <div className="dash-bottom-split">
            <CategoryPieChart
              data={filteredCategoryExpenses}
              title="Category Breakdown"
              isPrivacyMode={isPrivacyMode}
              hasActiveFilter={excludedCategoryIds.size > 0}
            />

            <div className="dash-kpi-stack">
              {/* Income KPI Card */}
              <div className="dash-kpi-card">
                <div className="dash-kpi-top">
                  <div className="dash-kpi-label-group">
                    <span className="dash-kpi-icon income" aria-hidden="true">
                      <ArrowDownLeft size={15} />
                    </span>
                    <span className="dash-kpi-title">Income</span>
                  </div>
                  <span className="dash-kpi-badge emerald">{incomeBadgeText}</span>
                </div>
                <div className="dash-kpi-value">
                  <AnimatedNumber
                    value={aggregated.totalIncome}
                    masked={isPrivacyMode}
                  />
                </div>
                <p className="dash-kpi-subtitle">
                  Cumulative inflow · {aggregated.walletName}
                </p>
              </div>

              {/* Expense KPI Card */}
              <div className="dash-kpi-card">
                <div className="dash-kpi-top">
                  <div className="dash-kpi-label-group">
                    <span className="dash-kpi-icon expense" aria-hidden="true">
                      <ArrowUpRight size={15} />
                    </span>
                    <span className="dash-kpi-title">Expense</span>
                  </div>
                  <span className="dash-kpi-badge violet">{expenseBadgeText}</span>
                </div>
                <div className="dash-kpi-value">
                  <AnimatedNumber
                    value={aggregated.totalExpenses}
                    masked={isPrivacyMode}
                  />
                </div>
                <p className="dash-kpi-subtitle">
                  Cumulative outflow · {aggregated.walletName}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* RIGHT COLUMN: Wallet Card Stack + Overlapping Glass Transactions + Mini Widgets */}
        <aside className="dash-right-col" aria-label="Wallets and activity">
          <WalletCardStack
            walletsData={walletsData}
            selectedWalletId={effectiveWalletId}
            onSelectWallet={setSelectedWalletId}
            isPrivacyMode={isPrivacyMode}
            onQuickAdd={handleQuickAdd}
          />

          {/* Overlapping Frosted Glass Transactions Panel */}
          <section
            className="dash-transactions-panel"
            aria-label="Recent transactions"
          >
            <div className="dash-tx-header">
              <h2 className="dash-tx-title">Transactions</h2>
              <Link to="/expenses" className="dash-section-link">
                <span>View All</span>
                <ChevronRight size={14} />
              </Link>
            </div>

            {recentTransactions.length === 0 ? (
              <div className="dash-tx-empty">
                <p>No transactions recorded for this period.</p>
                <button
                  type="button"
                  className="dash-tx-add-btn"
                  onClick={() => handleQuickAdd('expense', effectiveWalletId)}
                >
                  <Plus size={14} />
                  <span>Add Expense</span>
                </button>
              </div>
            ) : (
              <ul className="dash-tx-list">
                {recentTransactions.map((tx) => {
                  const isIncome = tx.type === 'income';
                  const hasUserEmoji =
                    !isIncome && tx.categoryIcon && tx.categoryIcon !== '❓';

                  return (
                    <li key={tx.id} className="dash-tx-row">
                      <div
                        className={`dash-tx-avatar ${isIncome ? 'is-income' : 'is-expense'}`}
                        aria-hidden="true"
                      >
                        {isIncome ? (
                          <ArrowDownLeft size={16} />
                        ) : hasUserEmoji ? (
                          <span className="dash-tx-emoji">{tx.categoryIcon}</span>
                        ) : (
                          <Receipt size={15} />
                        )}
                      </div>

                      <div className="dash-tx-meta">
                        <span className="dash-tx-desc" title={tx.description}>
                          {tx.description}
                        </span>
                        <span className="dash-tx-sub">
                          {formatDate(tx.dateStr, 'MMM d')}
                          {effectiveWalletId === 'all' && tx.walletName
                            ? ` · ${tx.walletName}`
                            : ''}
                        </span>
                      </div>

                      <span
                        className={`dash-tx-amount ${isIncome ? 'is-income' : 'is-expense'}`}
                      >
                        {isPrivacyMode
                          ? '••••••'
                          : `${isIncome ? '+' : '-'}${formatCurrency(tx.amount)}`}
                      </span>
                    </li>
                  );
                })}
              </ul>
            )}
          </section>

          {/* Bottom Right 2-Column Mini Widgets */}
          <div className="dash-mini-grid">
            {/* Recurring Widget */}
            <Link to="/recurring" className="dash-mini-card dash-mini-link">
              <div className="dash-mini-top">
                <span className="dash-mini-title">
                  <Repeat size={14} className="dash-mini-icon emerald" aria-hidden="true" />
                  <span>Recurring</span>
                </span>
                <span className="dash-mini-count">{recurringSummary.activeCount} active</span>
              </div>

              {recurringSummary.nextItem ? (
                <div className="dash-mini-body">
                  <div className="dash-rec-next-desc" title={recurringSummary.nextItem.description}>
                    {recurringSummary.nextItem.description || 'Scheduled item'}
                  </div>
                  <div className="dash-rec-next-meta">
                    <span className="dash-rec-due">
                      <CalendarClock size={12} aria-hidden="true" />
                      <span>{formatDueBadge(recurringSummary.nextItem.nextDate)}</span>
                    </span>
                    <span
                      className={`dash-rec-amount ${
                        recurringSummary.nextItem.type === 'income' ? 'is-income' : ''
                      }`}
                    >
                      {formatCurrency(recurringSummary.nextItem.amount, {
                        masked: isPrivacyMode,
                      })}
                    </span>
                  </div>
                </div>
              ) : (
                <div className="dash-mini-empty">No upcoming schedules</div>
              )}
            </Link>

            {/* Spending Pace Animated SVG Bezier Sparkline Widget */}
            <div className="dash-mini-card">
              <div className="dash-mini-top">
                <span className="dash-mini-title">
                  <TrendingUp size={14} className="dash-mini-icon violet" aria-hidden="true" />
                  <span>Spending Pace</span>
                </span>
                <span className={`dash-pace-badge ${paceSummary.deltaTone}`}>
                  {paceSummary.deltaText}
                </span>
              </div>

              <div className="dash-pace-chart-wrap" aria-hidden="true">
                <svg
                  className="dash-pace-svg"
                  viewBox="0 0 220 66"
                  preserveAspectRatio="none"
                >
                  <defs>
                    <linearGradient id="paceAreaGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="rgba(40, 201, 145, 0.34)" />
                      <stop offset="100%" stopColor="rgba(40, 201, 145, 0)" />
                    </linearGradient>
                    <linearGradient id="paceLineGrad" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0%" stopColor="#8E7CF8" />
                      <stop offset="100%" stopColor="#35D49A" />
                    </linearGradient>
                  </defs>
                  <path d={paceSummary.areaPath} fill="url(#paceAreaGrad)" />
                  <motion.path
                    key={`${effectiveWalletId}-${timeframe}-${selectedMonth}-${selectedYear}`}
                    d={paceSummary.linePath}
                    fill="none"
                    stroke="url(#paceLineGrad)"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    initial={
                      shouldReduceMotion
                        ? { pathLength: 1, opacity: 1 }
                        : { pathLength: 0, opacity: 0.35 }
                    }
                    animate={{ pathLength: 1, opacity: 1 }}
                    transition={
                      shouldReduceMotion
                        ? { duration: 0.01 }
                        : { duration: 0.85, ease: [0.16, 1, 0.3, 1] }
                    }
                  />
                  <circle
                    cx={paceSummary.endX}
                    cy={paceSummary.endY}
                    r="4"
                    fill="#35D49A"
                    stroke="#09110F"
                    strokeWidth="1.5"
                  />
                </svg>
              </div>

              <div className="dash-pace-foot">
                <span className="dash-pace-caption">Latest period</span>
                <span className="dash-pace-val">
                  <AnimatedNumber
                    value={paceSummary.latest}
                    masked={isPrivacyMode}
                    compact={paceSummary.latest >= 100000}
                  />
                </span>
              </div>
            </div>
          </div>
        </aside>
      </div>

      {/* Quick-Add Expense / Income Modals */}
      <ExpenseForm
        isOpen={quickAddType === 'expense'}
        onClose={() => {
          setQuickAddType(null);
          setQuickAddWalletId(null);
        }}
        onSave={handleSaveQuickExpense}
        wallets={wallets}
        categories={categories}
        paymentMethods={paymentMethods}
        defaultWalletId={quickAddWalletId}
        defaultDate={defaultQuickDate}
      />

      <IncomeForm
        isOpen={quickAddType === 'income'}
        onClose={() => {
          setQuickAddType(null);
          setQuickAddWalletId(null);
        }}
        onSave={handleSaveQuickIncome}
        wallets={wallets}
        defaultWalletId={quickAddWalletId}
        defaultDate={defaultQuickDate}
      />
    </div>
  );
};

export default Dashboard;
