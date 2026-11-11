import React, { useState, useEffect, useRef, useMemo } from 'react';
import { motion, useReducedMotion } from 'motion/react';
import { BarChart3 } from 'lucide-react';
import { formatCurrency } from '../utils/format';
import './TrendChart.css';

function getNiceScale(maxValue) {
  if (!maxValue || maxValue <= 0) {
    return { yMax: 4000, ticks: [0, 1000, 2000, 3000, 4000] };
  }
  const roughStep = maxValue / 4;
  const magnitude = Math.pow(10, Math.floor(Math.log10(roughStep)));
  const normalized = roughStep / magnitude;
  let niceNormalized = 1;
  if (normalized <= 1) niceNormalized = 1;
  else if (normalized <= 2) niceNormalized = 2;
  else if (normalized <= 2.5) niceNormalized = 2.5;
  else if (normalized <= 5) niceNormalized = 5;
  else niceNormalized = 10;

  const step = niceNormalized * magnitude;
  const yMax = step * 4;
  return {
    yMax,
    ticks: [0, step, step * 2, step * 3, step * 4],
  };
}

function formatAxisTick(value) {
  if (value === 0) return '₱0';
  if (value >= 1000000) {
    const m = value / 1000000;
    return `₱${Number.isInteger(m) ? m : m.toFixed(1)}M`;
  }
  if (value >= 1000) {
    const k = value / 1000;
    return `₱${Number.isInteger(k) ? k : k.toFixed(1)}k`;
  }
  return `₱${Math.round(value)}`;
}

function buildRoundedBarPath(x, y, w, h, topRadius = 10, bottomRadius = 4) {
  const rt = Math.min(topRadius, w / 2, h / 2);
  const rb = Math.min(bottomRadius, w / 2, h / 2);
  return [
    `M ${x + rt} ${y}`,
    `L ${x + w - rt} ${y}`,
    `Q ${x + w} ${y} ${x + w} ${y + rt}`,
    `L ${x + w} ${y + h - rb}`,
    `Q ${x + w} ${y + h} ${x + w - rb} ${y + h}`,
    `L ${x + rb} ${y + h}`,
    `Q ${x} ${y + h} ${x} ${y + h - rb}`,
    `L ${x} ${y + rt}`,
    `Q ${x} ${y} ${x + rt} ${y}`,
    'Z',
  ].join(' ');
}

export default function TrendChart({ data = [], isPrivacyMode = false }) {
  const containerRef = useRef(null);
  const shouldReduceMotion = useReducedMotion();
  const [size, setSize] = useState({ width: 580, height: 256 });
  const [hoveredIndex, setHoveredIndex] = useState(null);
  const [selectedIndex, setSelectedIndex] = useState(null);

  useEffect(() => {
    setSelectedIndex(null);
    setHoveredIndex(null);
  }, [data]);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    const updateSize = () => {
      const rect = el.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0) {
        setSize({ width: rect.width, height: rect.height });
      }
    };

    updateSize();
    if (typeof ResizeObserver === 'undefined') return;

    const observer = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;
        if (width > 0 && height > 0) {
          setSize({ width, height });
        }
      }
    });
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  const defaultActiveIndex = useMemo(() => {
    if (!data || data.length === 0) return -1;
    for (let i = data.length - 1; i >= 0; i--) {
      if (Number(data[i]?.total) > 0) return i;
    }
    return data.length - 1;
  }, [data]);

  const activeIndex =
    hoveredIndex !== null
      ? hoveredIndex
      : selectedIndex !== null
        ? selectedIndex
        : defaultActiveIndex;

  if (!data || data.length === 0) {
    return (
      <div className="trend-chart-empty">
        <BarChart3 size={28} strokeWidth={1.6} />
        <span>No cashflow trend data for this period</span>
      </div>
    );
  }

  const { width, height } = size;
  const padTop = 46;
  const padBottom = 30;
  const padLeft = 56;
  const padRight = 14;
  const plotW = Math.max(120, width - padLeft - padRight);
  const plotH = Math.max(80, height - padTop - padBottom);

  const rawMax = Math.max(...data.map((d) => Number(d.total) || 0), 0);
  const { yMax, ticks } = getNiceScale(rawMax);

  const slotW = plotW / data.length;
  const maxBarW = data.length <= 6 ? 42 : 26;
  const barW = Math.min(maxBarW, Math.max(12, slotW * 0.56));
  const topRadius = data.length <= 6 ? 10 : 7;

  const bars = data.map((item, idx) => {
    const value = Math.max(0, Number(item.total) || 0);
    const ratio = yMax > 0 ? value / yMax : 0;
    const rawH = ratio * plotH;
    const barH = value > 0 ? Math.max(10, rawH) : 6;
    const cx = padLeft + slotW * idx + slotW / 2;
    const barX = cx - barW / 2;
    const barY = padTop + plotH - barH;
    return {
      ...item,
      value,
      idx,
      cx,
      barX,
      barY,
      barH,
      isActive: idx === activeIndex,
      path: buildRoundedBarPath(barX, barY, barW, barH, topRadius, 4),
    };
  });

  const activeBar = bars[activeIndex] || bars[bars.length - 1];
  const tooltipLeft = activeBar
    ? Math.max(padLeft + 46, Math.min(width - 58, activeBar.cx))
    : width / 2;
  const tooltipBottom = activeBar ? Math.max(padBottom + 14, height - activeBar.barY + 14) : 64;

  const handleKeyDown = (e, idx) => {
    if (e.key === 'ArrowRight') {
      e.preventDefault();
      setSelectedIndex((idx + 1) % bars.length);
    } else if (e.key === 'ArrowLeft') {
      e.preventDefault();
      setSelectedIndex((idx - 1 + bars.length) % bars.length);
    } else if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      setSelectedIndex(idx);
    }
  };

  return (
    <div
      className="trend-chart-stage"
      ref={containerRef}
      onMouseLeave={() => setHoveredIndex(null)}
    >
      <svg
        className="trend-chart-svg"
        width={width}
        height={height}
        viewBox={`0 0 ${width} ${height}`}
        role="img"
        aria-label="Spending cashflow bar chart"
      >
        <defs>
          <pattern
            id="hatched-bar"
            width="8"
            height="8"
            patternTransform="rotate(42 0 0)"
            patternUnits="userSpaceOnUse"
          >
            <rect width="8" height="8" fill="rgba(142, 124, 248, 0.08)" />
            <line
              x1="0"
              y1="0"
              x2="0"
              y2="8"
              stroke="rgba(142, 124, 248, 0.28)"
              strokeWidth="2.6"
            />
          </pattern>

          <linearGradient id="active-violet-bar" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#9686FC" />
            <stop offset="100%" stopColor="#6856E8" />
          </linearGradient>

          <linearGradient id="active-violet-halo" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="rgba(150, 134, 252, 0.16)" />
            <stop offset="100%" stopColor="rgba(104, 86, 232, 0)" />
          </linearGradient>
        </defs>

        {/* Horizontal dashed gridlines + Y-axis labels */}
        {ticks.map((tick) => {
          const y = padTop + plotH - (tick / yMax) * plotH;
          return (
            <g key={tick}>
              <line
                x1={padLeft}
                y1={y}
                x2={width - padRight}
                y2={y}
                stroke={tick === 0 ? 'rgba(255, 255, 255, 0.09)' : 'rgba(255, 255, 255, 0.045)'}
                strokeDasharray={tick === 0 ? undefined : '4 4'}
              />
              <text
                x={padLeft - 10}
                y={y + 4}
                textAnchor="end"
                className="trend-axis-y"
              >
                {isPrivacyMode && tick > 0 ? '••••' : formatAxisTick(tick)}
              </text>
            </g>
          );
        })}

        {/* Vertical guide band behind active bar */}
        {activeBar && (
          <rect
            x={activeBar.barX - 5}
            y={padTop}
            width={barW + 10}
            height={plotH}
            rx={12}
            fill="url(#active-violet-halo)"
            pointerEvents="none"
          />
        )}

        {/* Bars */}
        {bars.map((bar) => (
          <g
            key={`${bar.label}-${bar.idx}`}
            className="trend-bar-group"
            tabIndex={0}
            role="button"
            aria-label={`${bar.label}: ${formatCurrency(bar.value, { masked: isPrivacyMode })}`}
            aria-pressed={bar.isActive}
            onMouseEnter={() => setHoveredIndex(bar.idx)}
            onClick={() => setSelectedIndex(bar.idx)}
            onKeyDown={(e) => handleKeyDown(e, bar.idx)}
          >
            {/* Full-slot invisible hit zone for easy hovering */}
            <rect
              x={padLeft + slotW * bar.idx}
              y={padTop}
              width={slotW}
              height={plotH + padBottom}
              fill="transparent"
            />

            <motion.path
              d={bar.path}
              fill={bar.isActive ? 'url(#active-violet-bar)' : 'url(#hatched-bar)'}
              stroke={
                bar.isActive
                  ? 'rgba(255, 255, 255, 0.24)'
                  : 'rgba(142, 124, 248, 0.16)'
              }
              strokeWidth={1}
              initial={false}
              animate={{
                opacity: bar.isActive ? 1 : bar.value === 0 ? 0.45 : 0.85,
              }}
              transition={
                shouldReduceMotion
                  ? { duration: 0.01 }
                  : { duration: 0.22, ease: [0.16, 1, 0.3, 1] }
              }
            />

            <text
              x={bar.cx}
              y={height - 8}
              textAnchor="middle"
              className={`trend-axis-x ${bar.isActive ? 'is-active' : ''}`}
            >
              {bar.label}
            </text>
          </g>
        ))}

        {/* Glowing white-ring anchor dot on active bar top */}
        {activeBar && (
          <g className="trend-anchor-dot" pointerEvents="none">
            <motion.circle
              cx={activeBar.cx}
              cy={activeBar.barY}
              r={10}
              fill="rgba(150, 134, 252, 0.28)"
              initial={false}
              animate={{ cx: activeBar.cx, cy: activeBar.barY }}
              transition={
                shouldReduceMotion
                  ? { duration: 0.01 }
                  : { type: 'spring', stiffness: 360, damping: 28 }
              }
            />
            <motion.circle
              cx={activeBar.cx}
              cy={activeBar.barY}
              r={5}
              fill="#8E7CF8"
              stroke="#FFFFFF"
              strokeWidth={2.5}
              initial={false}
              animate={{ cx: activeBar.cx, cy: activeBar.barY }}
              transition={
                shouldReduceMotion
                  ? { duration: 0.01 }
                  : { type: 'spring', stiffness: 360, damping: 28 }
              }
            />
          </g>
        )}
      </svg>

      {/* Floating dark frosted tooltip badge */}
      {activeBar && (
        <motion.div
          className="trend-floating-badge"
          initial={false}
          animate={{
            left: tooltipLeft,
            bottom: tooltipBottom,
          }}
          transition={
            shouldReduceMotion
              ? { duration: 0.01 }
              : { type: 'spring', stiffness: 360, damping: 30 }
          }
          aria-hidden="true"
        >
          <span className="trend-badge-period">{activeBar.label}</span>
          <span className="trend-badge-amount">
            {formatCurrency(activeBar.value, { masked: isPrivacyMode })}
          </span>
        </motion.div>
      )}
    </div>
  );
}
