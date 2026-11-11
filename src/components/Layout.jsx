import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'motion/react';
import {
  ArrowRight,
  CalendarDays,
  LayoutGrid,
  LogOut,
  Receipt,
  Repeat,
  Search,
  SlidersHorizontal,
  TrendingUp,
  X,
} from 'lucide-react';
import Sidebar from './Sidebar';
import ChangelogModal from './ChangelogModal';
import { useAutoPost } from '../hooks/useAutoPost';
import { useAuth } from '../context/AuthContext';
import { useExpenses } from '../hooks/useExpenses';
import { useIncome } from '../hooks/useIncome';
import { formatCurrency, formatDate } from '../utils/format';
import './Layout.css';

const QUICK_PAGES = [
  { path: '/dashboard', label: 'Dashboard', hint: 'Overview & wallets', icon: LayoutGrid },
  { path: '/expenses', label: 'Expenses', hint: 'Spending ledger', icon: Receipt },
  { path: '/income', label: 'Income', hint: 'Earnings ledger', icon: TrendingUp },
  { path: '/calendar', label: 'Calendar', hint: 'Daily schedule & projections', icon: CalendarDays },
  { path: '/recurring', label: 'Recurring', hint: 'Automated rules', icon: Repeat },
  { path: '/settings', label: 'Settings', hint: 'Wallets, cards & categories', icon: SlidersHorizontal },
];

function deriveGreetingName(email) {
  if (!email) return { name: 'Friend', initials: 'MT' };
  const localPart = email.split('@')[0] || 'Friend';
  const tokens = localPart.split(/[._\-+]+/).filter(Boolean);
  const rawFirst = tokens[0] || localPart;
  const formattedName = rawFirst.charAt(0).toUpperCase() + rawFirst.slice(1);
  const initials =
    tokens.length >= 2
      ? `${tokens[0].charAt(0)}${tokens[1].charAt(0)}`.toUpperCase()
      : rawFirst.slice(0, 2).toUpperCase();
  return { name: formattedName, initials };
}

const Layout = () => {
  useAutoPost();

  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const now = useMemo(() => new Date(), []);
  const currentMonth = now.getMonth();
  const currentYear = now.getFullYear();

  const { expenses, fetchExpenses } = useExpenses(currentMonth, currentYear);
  const { income, fetchIncome } = useIncome(currentMonth, currentYear);

  const [isChangelogOpen, setIsChangelogOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);

  const searchContainerRef = useRef(null);
  const searchInputRef = useRef(null);
  const hasRefreshedOnFocusRef = useRef(false);

  const { name: greetingName, initials } = useMemo(
    () => deriveGreetingName(user?.email),
    [user?.email]
  );

  const navigateWithTransition = (path) => {
    const prefersReducedMotion =
      typeof window !== 'undefined' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (typeof document !== 'undefined' && document.startViewTransition && !prefersReducedMotion) {
      document.startViewTransition(() => {
        navigate(path);
      });
    } else {
      navigate(path);
    }
  };

  // Global Cmd/Ctrl+K or '/' search shortcut
  useEffect(() => {
    const handleGlobalKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        searchInputRef.current?.focus();
        setIsSearchOpen(true);
      } else if (
        e.key === '/' &&
        !['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement?.tagName) &&
        !document.activeElement?.isContentEditable
      ) {
        e.preventDefault();
        searchInputRef.current?.focus();
        setIsSearchOpen(true);
      }
    };
    window.addEventListener('keydown', handleGlobalKeyDown);
    return () => window.removeEventListener('keydown', handleGlobalKeyDown);
  }, []);

  // Outside click closes search popover
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (searchContainerRef.current && !searchContainerRef.current.contains(e.target)) {
        setIsSearchOpen(false);
        setActiveIndex(-1);
        hasRefreshedOnFocusRef.current = false;
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSearchFocus = () => {
    setIsSearchOpen(true);
    if (!hasRefreshedOnFocusRef.current) {
      hasRefreshedOnFocusRef.current = true;
      fetchExpenses({ month: currentMonth + 1, year: currentYear });
      fetchIncome({ month: currentMonth + 1, year: currentYear });
    }
  };

  const searchResults = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();

    const matchedPages = QUICK_PAGES.filter((p) => {
      if (!q) return true;
      return p.label.toLowerCase().includes(q) || p.hint.toLowerCase().includes(q);
    }).slice(0, q ? 4 : 3);

    const combinedTx = [
      ...(expenses || []).map((e) => ({
        id: `exp-${e.id}`,
        kind: 'expense',
        title: e.description || e.categories?.name || 'Expense',
        subtitle: `${e.categories?.name || 'Uncategorized'} • ${e.wallets?.name || 'Wallet'}`,
        iconText: e.categories?.icon || null,
        date: e.expense_date,
        amount: e.amount,
        targetPath: `/expenses?q=${encodeURIComponent(e.description || e.categories?.name || '')}`,
      })),
      ...(income || []).map((inc) => ({
        id: `inc-${inc.id}`,
        kind: 'income',
        title: inc.description || 'Income',
        subtitle: `Income • ${inc.wallets?.name || 'Wallet'}`,
        iconText: null,
        date: inc.income_date,
        amount: inc.amount,
        targetPath: `/income?q=${encodeURIComponent(inc.description || inc.wallets?.name || '')}`,
      })),
    ].sort((a, b) => String(b.date || '').localeCompare(String(a.date || '')));

    const matchedTransactions = combinedTx
      .filter((tx) => {
        if (!q) return true;
        return (
          tx.title.toLowerCase().includes(q) ||
          tx.subtitle.toLowerCase().includes(q) ||
          String(tx.amount).includes(q)
        );
      })
      .slice(0, 5);

    const flatItems = [
      ...matchedPages.map((p) => ({ type: 'page', key: `page-${p.path}`, path: p.path, data: p })),
      ...matchedTransactions.map((tx) => ({
        type: 'tx',
        key: tx.id,
        path: tx.targetPath,
        data: tx,
      })),
    ];

    return { matchedPages, matchedTransactions, flatItems };
  }, [searchQuery, expenses, income]);

  const handleSelectItem = (path) => {
    setIsSearchOpen(false);
    setActiveIndex(-1);
    searchInputRef.current?.blur();
    navigateWithTransition(path);
  };

  const handleSearchKeyDown = (e) => {
    const total = searchResults.flatItems.length;
    if (e.key === 'Escape') {
      setIsSearchOpen(false);
      setActiveIndex(-1);
      searchInputRef.current?.blur();
    } else if (e.key === 'ArrowDown' && total > 0) {
      e.preventDefault();
      setIsSearchOpen(true);
      setActiveIndex((prev) => (prev + 1) % total);
    } else if (e.key === 'ArrowUp' && total > 0) {
      e.preventDefault();
      setIsSearchOpen(true);
      setActiveIndex((prev) => (prev <= 0 ? total - 1 : prev - 1));
    } else if (e.key === 'Enter' && total > 0) {
      e.preventDefault();
      const chosen =
        activeIndex >= 0 && activeIndex < total
          ? searchResults.flatItems[activeIndex]
          : searchResults.flatItems[0];
      if (chosen) {
        handleSelectItem(chosen.path);
      }
    }
  };

  const pageCount = searchResults.matchedPages.length;

  return (
    <div className="app-layout">
      <Sidebar onOpenChangelog={() => setIsChangelogOpen(true)} />

      <div className="main-content">
        <div className="shell-container">
          <header className="shell-topbar">
            {/* Left: Geometric Mark + Wordmark + Version */}
            <div className="topbar-brand">
              <Link
                to="/dashboard"
                className="topbar-logo-link"
                onClick={(e) => {
                  if (location.pathname !== '/dashboard') {
                    e.preventDefault();
                    navigateWithTransition('/dashboard');
                  }
                }}
              >
                <span className="topbar-logo-mark" aria-hidden="true">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
                    <path
                      d="M4 16.5L9.5 9L13.5 13.5L19.5 6"
                      stroke="#051A13"
                      strokeWidth="2.5"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                    <circle cx="19.5" cy="6" r="2.2" fill="#C8F865" />
                  </svg>
                </span>
                <span className="topbar-wordmark">Money Tracker</span>
              </Link>

              <button
                type="button"
                className="topbar-version-pill tabular-nums"
                onClick={() => setIsChangelogOpen(true)}
                title="View release notes"
                aria-label="Version 1.1.0 release notes"
              >
                v1.1.0
              </button>
            </div>

            {/* Center: Global Quick Search */}
            <div className="topbar-search-wrap" ref={searchContainerRef}>
              <div className={`topbar-search-pill ${isSearchOpen ? 'is-focused' : ''}`}>
                <Search className="topbar-search-icon" size={16} aria-hidden="true" />
                <input
                  ref={searchInputRef}
                  type="search"
                  className="topbar-search-input"
                  placeholder="Search transactions or jump to page..."
                  aria-label="Search transactions or jump to page"
                  aria-expanded={isSearchOpen}
                  aria-controls="topbar-search-listbox"
                  role="combobox"
                  aria-autocomplete="list"
                  value={searchQuery}
                  onFocus={handleSearchFocus}
                  onChange={(e) => {
                    setSearchQuery(e.target.value);
                    setIsSearchOpen(true);
                    setActiveIndex(-1);
                  }}
                  onKeyDown={handleSearchKeyDown}
                />
                {searchQuery ? (
                  <button
                    type="button"
                    className="topbar-search-clear"
                    onClick={() => {
                      setSearchQuery('');
                      setActiveIndex(-1);
                      searchInputRef.current?.focus();
                    }}
                    aria-label="Clear search"
                  >
                    <X size={14} />
                  </button>
                ) : (
                  <kbd className="topbar-search-kbd" aria-hidden="true">
                    Ctrl K
                  </kbd>
                )}
              </div>

              <AnimatePresence>
                {isSearchOpen && (
                  <motion.div
                    id="topbar-search-listbox"
                    role="listbox"
                    initial={{ opacity: 0, y: 6, scale: 0.985 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 4, scale: 0.985 }}
                    transition={{ duration: 0.16, ease: [0.16, 1, 0.3, 1] }}
                    className="topbar-search-dropdown"
                  >
                    {searchResults.matchedPages.length > 0 && (
                      <div className="search-group">
                        <div className="search-group-label">Pages</div>
                        {searchResults.matchedPages.map((page, idx) => {
                          const PageIcon = page.icon;
                          const isHighlighted = activeIndex === idx;
                          return (
                            <button
                              key={page.path}
                              type="button"
                              role="option"
                              aria-selected={isHighlighted}
                              className={`search-result-row ${isHighlighted ? 'is-highlighted' : ''}`}
                              onClick={() => handleSelectItem(page.path)}
                            >
                              <span className="search-result-badge">
                                <PageIcon size={15} />
                              </span>
                              <span className="search-result-main">
                                <span className="search-result-title">{page.label}</span>
                                <span className="search-result-sub">{page.hint}</span>
                              </span>
                              <ArrowRight className="search-result-arrow" size={14} aria-hidden="true" />
                            </button>
                          );
                        })}
                      </div>
                    )}

                    {searchResults.matchedTransactions.length > 0 && (
                      <div className="search-group">
                        <div className="search-group-label">This Month&apos;s Activity</div>
                        {searchResults.matchedTransactions.map((tx, txIdx) => {
                          const flatIdx = pageCount + txIdx;
                          const isHighlighted = activeIndex === flatIdx;
                          return (
                            <button
                              key={tx.id}
                              type="button"
                              role="option"
                              aria-selected={isHighlighted}
                              className={`search-result-row ${isHighlighted ? 'is-highlighted' : ''}`}
                              onClick={() => handleSelectItem(tx.targetPath)}
                            >
                              <span
                                className={`search-result-badge ${
                                  tx.kind === 'income' ? 'is-income' : 'is-expense'
                                }`}
                              >
                                {tx.iconText ? (
                                  <span>{tx.iconText}</span>
                                ) : tx.kind === 'income' ? (
                                  <TrendingUp size={15} />
                                ) : (
                                  <Receipt size={15} />
                                )}
                              </span>
                              <span className="search-result-main">
                                <span className="search-result-title">{tx.title}</span>
                                <span className="search-result-sub">
                                  {tx.subtitle} • {formatDate(tx.date, 'MMM d')}
                                </span>
                              </span>
                              <span
                                className={`search-result-amount amount ${
                                  tx.kind === 'income' ? 'amount-positive' : 'amount-negative'
                                }`}
                              >
                                {tx.kind === 'income' ? '+' : '-'}
                                {formatCurrency(tx.amount)}
                              </span>
                            </button>
                          );
                        })}
                      </div>
                    )}

                    {searchResults.flatItems.length === 0 && (
                      <div className="search-empty-state">
                        No pages or current-month transactions match &ldquo;{searchQuery}&rdquo;
                      </div>
                    )}
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            {/* Right: User Greeting + Avatar + Mobile Sign Out */}
            <div className="topbar-user">
              <div className="topbar-greeting">
                <span className="topbar-greeting-text">Hi, {greetingName}</span>
                <span className="topbar-avatar" aria-hidden="true">
                  {initials}
                </span>
              </div>

              <button
                type="button"
                className="topbar-mobile-logout"
                onClick={signOut}
                aria-label="Sign Out"
                title="Sign Out"
              >
                <LogOut size={17} />
              </button>
            </div>
          </header>

          <main className="shell-main">
            <Outlet />
          </main>
        </div>
      </div>

      <ChangelogModal isOpen={isChangelogOpen} onClose={() => setIsChangelogOpen(false)} />
    </div>
  );
};

export default Layout;
