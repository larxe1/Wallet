import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts';
import { ChevronRight } from 'lucide-react';
import AnimatedNumber from './AnimatedNumber';
import { formatCurrency } from '../utils/format';
import './CategoryPieChart.css';

const SLICE_COLORS = [
  '#8E7CF8',
  '#C8F865',
  '#28C991',
  '#38BDF8',
  '#F0B44A',
  '#F06478',
  '#A78BFA',
];

function CustomPieTooltip({ active, payload, isPrivacyMode }) {
  if (!active || !payload || !payload.length) return null;
  const entry = payload[0].payload;
  const total = entry.totalSum || 0;
  const pct = total > 0 ? ((entry.value / total) * 100).toFixed(1) : '0.0';

  return (
    <div className="pie-tooltip">
      <div className="pie-tooltip-header">
        <span
          className="pie-tooltip-dot"
          style={{ backgroundColor: entry.color }}
        />
        <span className="pie-tooltip-name">{entry.name}</span>
      </div>
      <div className="pie-tooltip-metrics">
        <span className="pie-tooltip-value">
          {formatCurrency(entry.value, { masked: isPrivacyMode })}
        </span>
        <span className="pie-tooltip-percent">{pct}%</span>
      </div>
    </div>
  );
}

export default function CategoryPieChart({
  data = [],
  title = 'Category Breakdown',
  isPrivacyMode = false,
  hasActiveFilter = false,
}) {
  const [hoveredIdx, setHoveredIdx] = useState(null);

  const normalized = (data || [])
    .map((item) => ({
      id: item.category_id || item.name || item.categoryName,
      name: item.name || item.categoryName || 'Category',
      icon: item.icon || item.categoryIcon,
      value: Number(item.value ?? item.totalAmount) || 0,
    }))
    .filter((item) => item.value > 0);

  const totalAmount = normalized.reduce((sum, item) => sum + item.value, 0);

  const chartData = normalized.map((item, idx) => ({
    ...item,
    color: SLICE_COLORS[idx % SLICE_COLORS.length],
    totalSum: totalAmount,
    sharePct: totalAmount > 0 ? Math.round((item.value / totalAmount) * 100) : 0,
  }));

  const focusedItem = hoveredIdx !== null ? chartData[hoveredIdx] : null;
  const centerLabel = focusedItem ? focusedItem.name : 'Total Spend';
  const centerValue = focusedItem ? focusedItem.value : totalAmount;

  return (
    <section className="pie-card" aria-label={title}>
      <div className="pie-card-header">
        <h3 className="pie-card-title">{title}</h3>
        <Link to="/expenses" className="dash-section-link">
          <span>View All</span>
          <ChevronRight size={14} />
        </Link>
      </div>

      {chartData.length === 0 ? (
        <div className="pie-empty-state">
          <div className="pie-empty-ring" aria-hidden="true">
            <span className="pie-empty-total">
              {formatCurrency(0, { masked: isPrivacyMode })}
            </span>
          </div>
          <p className="pie-empty-copy">
            {hasActiveFilter
              ? 'All categories with spend are excluded.'
              : 'No categorized expenses for this period yet.'}
          </p>
        </div>
      ) : (
        <div className="pie-card-body">
          <div className="pie-donut-wrap">
            <div className="pie-center-kpi">
              <span className="pie-center-label" title={centerLabel}>
                {centerLabel}
              </span>
              <div className="pie-center-value">
                <AnimatedNumber
                  value={centerValue}
                  masked={isPrivacyMode}
                  compact={centerValue >= 100000}
                />
              </div>
            </div>

            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={chartData}
                  cx="50%"
                  cy="50%"
                  innerRadius={56}
                  outerRadius={80}
                  cornerRadius={chartData.length > 1 ? 10 : 0}
                  paddingAngle={chartData.length > 1 ? 5 : 0}
                  dataKey="value"
                  stroke="none"
                  animationDuration={650}
                  onMouseEnter={(_, idx) => setHoveredIdx(idx)}
                  onMouseLeave={() => setHoveredIdx(null)}
                >
                  {chartData.map((entry, idx) => (
                    <Cell
                      key={entry.id || idx}
                      fill={entry.color}
                      opacity={
                        hoveredIdx === null || hoveredIdx === idx ? 1 : 0.35
                      }
                      style={{ transition: 'opacity 160ms ease', cursor: 'pointer' }}
                    />
                  ))}
                </Pie>
                <Tooltip
                  content={<CustomPieTooltip isPrivacyMode={isPrivacyMode} />}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>

          <ul className="pie-legend-list" aria-label="Category breakdown legend">
            {chartData.map((entry, idx) => (
              <li
                key={entry.id || idx}
                className={`pie-legend-row ${hoveredIdx === idx ? 'is-focused' : ''}`}
                onMouseEnter={() => setHoveredIdx(idx)}
                onMouseLeave={() => setHoveredIdx(null)}
              >
                <div className="pie-legend-left">
                  <span
                    className="pie-legend-dot"
                    style={{ backgroundColor: entry.color }}
                  />
                  <span className="pie-legend-name">{entry.name}</span>
                </div>
                <div className="pie-legend-right">
                  <span className="pie-legend-amount">
                    {formatCurrency(entry.value, { masked: isPrivacyMode })}
                  </span>
                  <span className="pie-legend-pct">{entry.sharePct}%</span>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
