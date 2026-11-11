import React from 'react';
import { motion, useReducedMotion } from 'motion/react';
import { Tag, BarChart2, RotateCcw } from 'lucide-react';
import { formatCurrency } from '../utils/format';
import './SpendingChart.css';

const BAR_PALETTES = [
  'linear-gradient(90deg, #6856E8 0%, #9686FC 100%)',
  'linear-gradient(90deg, #17A376 0%, #35D49A 100%)',
  'linear-gradient(90deg, #9AC836 0%, #C8F865 100%)',
  'linear-gradient(90deg, #0EA5E9 0%, #38BDF8 100%)',
  'linear-gradient(90deg, #D97706 0%, #F0B44A 100%)',
  'linear-gradient(90deg, #DC2626 0%, #F06478 100%)',
];

const DOT_COLORS = ['#8E7CF8', '#28C991', '#C8F865', '#38BDF8', '#F0B44A', '#F06478'];

export default function SpendingChart({
  data = [],
  isPrivacyMode = false,
  hasActiveFilter = false,
  onResetFilter,
}) {
  const shouldReduceMotion = useReducedMotion();

  if (!data || data.length === 0) {
    return (
      <div className="spending-chart-empty">
        <BarChart2 size={28} strokeWidth={1.6} />
        <p className="spending-empty-text">
          {hasActiveFilter
            ? 'All matching categories are currently filtered out.'
            : 'No category spending recorded for this period.'}
        </p>
        {hasActiveFilter && onResetFilter && (
          <button
            type="button"
            className="spending-reset-btn"
            onClick={onResetFilter}
          >
            <RotateCcw size={13} />
            <span>Show all categories</span>
          </button>
        )}
      </div>
    );
  }

  const totalSpend = data.reduce(
    (sum, item) => sum + (Number(item.totalAmount ?? item.value) || 0),
    0
  );
  const maxSpend = Math.max(
    ...data.map((item) => Number(item.totalAmount ?? item.value) || 0),
    1
  );

  return (
    <div className="spending-bars-list" role="list" aria-label="Spending by category">
      {data.map((item, idx) => {
        const amount = Number(item.totalAmount ?? item.value) || 0;
        const pctOfMax = Math.max(4, (amount / maxSpend) * 100);
        const sharePct = totalSpend > 0 ? Math.round((amount / totalSpend) * 100) : 0;
        const label = item.categoryName || item.name || 'Category';
        const rawIcon = item.categoryIcon || item.icon;
        const hasValidIcon = rawIcon && rawIcon !== '❓';
        const barGradient = BAR_PALETTES[idx % BAR_PALETTES.length];
        const accentColor = DOT_COLORS[idx % DOT_COLORS.length];

        return (
          <div key={item.category_id || label} className="spending-bar-row" role="listitem">
            <div
              className="spending-cat-badge"
              style={{ borderColor: `${accentColor}33`, background: `${accentColor}14` }}
              aria-hidden="true"
            >
              {hasValidIcon ? (
                <span className="spending-cat-emoji">{rawIcon}</span>
              ) : (
                <Tag size={15} style={{ color: accentColor }} />
              )}
            </div>

            <div className="spending-bar-body">
              <div className="spending-bar-meta">
                <div className="spending-bar-label-group">
                  <span className="spending-cat-name">{label}</span>
                  <span className="spending-cat-share">{sharePct}%</span>
                </div>
                <span className="spending-cat-amount">
                  {formatCurrency(amount, { masked: isPrivacyMode })}
                </span>
              </div>

              <div className="spending-bar-track" aria-hidden="true">
                <motion.div
                  className="spending-bar-fill"
                  style={{ background: barGradient }}
                  initial={shouldReduceMotion ? { width: `${pctOfMax}%` } : { width: '0%' }}
                  animate={{ width: `${pctOfMax}%` }}
                  transition={
                    shouldReduceMotion
                      ? { duration: 0.01 }
                      : {
                          duration: 0.55,
                          delay: Math.min(idx * 0.04, 0.24),
                          ease: [0.16, 1, 0.3, 1],
                        }
                  }
                />
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
