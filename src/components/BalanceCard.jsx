import React, { useState, useRef, useEffect } from 'react';
import { Plus, ArrowUpRight, ArrowDownLeft, Layers, Wallet } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import AnimatedNumber from './AnimatedNumber';
import './BalanceCard.css';

export default function BalanceCard({
  walletName,
  walletColor = '#28C991',
  totalIncome = 0,
  totalExpenses = 0,
  isPrivacyMode = false,
  isFront = true,
  isCombined = false,
  walletCount = 0,
  onQuickAdd,
  onClick,
  depth = 0,
}) {
  const [quickMenuOpen, setQuickMenuOpen] = useState(false);
  const menuRef = useRef(null);

  const balance = (Number(totalIncome) || 0) - (Number(totalExpenses) || 0);
  const isNegative = balance < 0;

  useEffect(() => {
    if (!isFront && quickMenuOpen) {
      setQuickMenuOpen(false);
    }
  }, [isFront, quickMenuOpen]);

  useEffect(() => {
    if (!quickMenuOpen) return;
    const handleOutside = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setQuickMenuOpen(false);
      }
    };
    const handleEsc = (e) => {
      if (e.key === 'Escape') setQuickMenuOpen(false);
    };
    document.addEventListener('mousedown', handleOutside);
    document.addEventListener('keydown', handleEsc);
    return () => {
      document.removeEventListener('mousedown', handleOutside);
      document.removeEventListener('keydown', handleEsc);
    };
  }, [quickMenuOpen]);

  const handleCardClick = (e) => {
    if (!isFront && onClick) {
      e.stopPropagation();
      onClick();
    }
  };

  const handleQuickAddToggle = (e) => {
    e.stopPropagation();
    setQuickMenuOpen((prev) => !prev);
  };

  const handleAction = (type, e) => {
    e.stopPropagation();
    setQuickMenuOpen(false);
    if (onQuickAdd) onQuickAdd(type);
  };

  const Tag = isFront ? 'div' : 'button';

  return (
    <Tag
      type={isFront ? undefined : 'button'}
      className={`balance-card ${isFront ? 'is-front' : `is-stacked depth-${depth}`}`}
      style={{ '--wallet-color': walletColor }}
      onClick={handleCardClick}
      aria-label={!isFront ? `Select ${walletName} wallet` : undefined}
    >
      {/* Clipped decorative mesh & curved SVG light waves */}
      <div className="bc-decor" aria-hidden="true">
        {isFront ? (
          <svg
            className="bc-wave-svg"
            viewBox="0 0 360 220"
            preserveAspectRatio="none"
            fill="none"
          >
            <path
              d="M-20 148 C 60 95, 165 185, 255 110 C 310 64, 345 48, 385 60 L 385 240 L -20 240 Z"
              fill="url(#bcWaveGrad1)"
            />
            <path
              d="M-20 175 C 80 120, 195 205, 310 132 C 340 112, 365 96, 385 104"
              stroke="rgba(255, 255, 255, 0.16)"
              strokeWidth="1.5"
            />
            <path
              d="M30 -10 C 120 45, 210 10, 345 85"
              stroke="rgba(200, 248, 101, 0.18)"
              strokeWidth="1.2"
            />
            <defs>
              <linearGradient id="bcWaveGrad1" x1="0" y1="60" x2="360" y2="220" gradientUnits="userSpaceOnUse">
                <stop offset="0%" stopColor="rgba(255, 255, 255, 0.10)" />
                <stop offset="100%" stopColor="rgba(9, 17, 15, 0.22)" />
              </linearGradient>
            </defs>
          </svg>
        ) : (
          <div className="bc-stacked-tint" />
        )}
      </div>

      {/* Card header */}
      <div className="bc-header">
        <div className="bc-wallet-pill">
          <span
            className="bc-wallet-dot"
            style={{ backgroundColor: isCombined ? '#35D49A' : walletColor }}
          />
          {isCombined ? <Layers size={13} aria-hidden="true" /> : <Wallet size={13} aria-hidden="true" />}
          <span className="bc-title">{walletName}</span>
          {isCombined && walletCount > 0 && (
            <span className="bc-count-tag">{walletCount}</span>
          )}
        </div>

        {isFront && onQuickAdd && (
          <div className="bc-quick-add-wrap" ref={menuRef}>
            <button
              type="button"
              className={`bc-quick-add-btn ${quickMenuOpen ? 'open' : ''}`}
              onClick={handleQuickAddToggle}
              aria-label={`Quick add transaction to ${walletName}`}
              aria-expanded={quickMenuOpen}
              aria-haspopup="menu"
              title="Quick add transaction"
            >
              <Plus size={17} strokeWidth={2.5} />
            </button>

            <AnimatePresence>
              {quickMenuOpen && (
                <motion.div
                  className="bc-quick-menu"
                  role="menu"
                  initial={{ opacity: 0, y: -6, scale: 0.95 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: -6, scale: 0.95 }}
                  transition={{ duration: 0.16, ease: [0.16, 1, 0.3, 1] }}
                >
                  <button
                    type="button"
                    role="menuitem"
                    className="bc-quick-menu-item"
                    onClick={(e) => handleAction('expense', e)}
                  >
                    <span className="bc-menu-icon expense">
                      <ArrowUpRight size={14} />
                    </span>
                    <span>Add Expense</span>
                  </button>
                  <button
                    type="button"
                    role="menuitem"
                    className="bc-quick-menu-item"
                    onClick={(e) => handleAction('income', e)}
                  >
                    <span className="bc-menu-icon income">
                      <ArrowDownLeft size={14} />
                    </span>
                    <span>Add Income</span>
                  </button>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        )}
      </div>

      {/* Main Balance */}
      {isFront && (
        <>
          <div className="bc-balance-section">
            <span className="bc-label">Net Balance</span>
            <div className={`bc-balance ${isNegative && !isPrivacyMode ? 'is-negative' : ''}`}>
              <AnimatedNumber value={balance} masked={isPrivacyMode} />
            </div>
          </div>

          <div className="bc-details">
            <div className="bc-stat">
              <span className="bc-stat-label">Inflow</span>
              <span className="bc-stat-value">
                <AnimatedNumber value={totalIncome} masked={isPrivacyMode} />
              </span>
            </div>
            <div className="bc-stat bc-stat-right">
              <span className="bc-stat-label">Outflow</span>
              <span className="bc-stat-value">
                <AnimatedNumber value={totalExpenses} masked={isPrivacyMode} />
              </span>
            </div>
          </div>
        </>
      )}
    </Tag>
  );
}
