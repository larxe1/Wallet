import React, { useState, useEffect, useRef } from 'react';
import { format } from 'date-fns';
import { motion, AnimatePresence } from 'motion/react';
import { ChevronLeft, ChevronRight, ChevronDown, Calendar } from 'lucide-react';
import './MonthSelector.css';

const MonthSelector = ({
  month,
  year,
  onChange,
  minMonth = 0,
  minYear = 2000,
  hideMonth = false,
}) => {
  const [showMonthDropdown, setShowMonthDropdown] = useState(false);
  const [showYearDropdown, setShowYearDropdown] = useState(false);
  const containerRef = useRef(null);

  // month is 0-indexed (0=Jan, 11=Dec); construct day=1 to prevent day-31 overflow
  const currentDate = new Date(year, month, 1);

  const isAtMin = hideMonth
    ? year <= minYear
    : year < minYear || (year === minYear && month <= minMonth);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setShowMonthDropdown(false);
        setShowYearDropdown(false);
      }
    };

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setShowMonthDropdown(false);
        setShowYearDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  useEffect(() => {
    if (hideMonth && showMonthDropdown) {
      setShowMonthDropdown(false);
    }
  }, [hideMonth, showMonthDropdown]);

  const handlePrev = () => {
    if (isAtMin) return;
    if (hideMonth) {
      const prevYear = year - 1;
      const clampedMonth = prevYear === minYear && month < minMonth ? minMonth : month;
      onChange(clampedMonth, prevYear);
      return;
    }

    if (month === 0) {
      onChange(11, year - 1);
    } else {
      onChange(month - 1, year);
    }
  };

  const handleNext = () => {
    if (hideMonth) {
      onChange(month, year + 1);
      return;
    }

    if (month === 11) {
      onChange(0, year + 1);
    } else {
      onChange(month + 1, year);
    }
  };

  const months = Array.from({ length: 12 }, (_, i) => {
    const date = new Date(2026, i, 1);
    return {
      short: format(date, 'MMM'),
      full: format(date, 'MMMM'),
    };
  });

  const currentYear = new Date().getFullYear();
  const startYear = Math.min(minYear, currentYear - 1, year);
  const years = Array.from({ length: 9 }, (_, i) => startYear + i);

  return (
    <div className="month-selector" ref={containerRef}>
      <button
        type="button"
        className="nav-btn nav-arrow"
        onClick={handlePrev}
        disabled={isAtMin}
        aria-label={hideMonth ? 'Previous year' : 'Previous month'}
        title={hideMonth ? 'Previous year' : 'Previous month'}
      >
        <ChevronLeft size={16} strokeWidth={2.2} aria-hidden="true" />
      </button>

      <div className="selector-dropdowns current-date-display">
        {!hideMonth && (
          <div className="dropdown-container month-dropdown-wrap">
            <button
              type="button"
              className={`dropdown-toggle ${showMonthDropdown ? 'open' : ''}`}
              aria-haspopup="true"
              aria-expanded={showMonthDropdown}
              aria-label={`Select month, currently ${format(currentDate, 'MMMM')}`}
              onClick={() => {
                setShowMonthDropdown((prev) => !prev);
                setShowYearDropdown(false);
              }}
            >
              <Calendar size={14} strokeWidth={2} className="selector-cal-icon" aria-hidden="true" />
              <span>{format(currentDate, 'MMMM')}</span>
              <ChevronDown
                size={14}
                strokeWidth={2.2}
                className={`selector-chevron ${showMonthDropdown ? 'rotated' : ''}`}
                aria-hidden="true"
              />
            </button>

            <AnimatePresence>
              {showMonthDropdown && (
                <motion.div
                  className="dropdown-menu month-menu"
                  role="listbox"
                  aria-label="Choose month"
                  initial={{ opacity: 0, y: -6, scale: 0.96 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: -6, scale: 0.96 }}
                  transition={{ duration: 0.16, ease: [0.16, 1, 0.3, 1] }}
                >
                  {months.map((m, i) => {
                    const isDisabled = year === minYear && i < minMonth;
                    const isActive = i === month;
                    return (
                      <button
                        key={m.full}
                        type="button"
                        role="option"
                        aria-selected={isActive}
                        aria-label={m.full}
                        className={`dropdown-item ${isActive ? 'active' : ''}`}
                        disabled={isDisabled}
                        onClick={() => {
                          if (isDisabled) return;
                          onChange(i, year);
                          setShowMonthDropdown(false);
                        }}
                      >
                        {m.short}
                      </button>
                    );
                  })}
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        )}

        <div className="dropdown-container year-dropdown-wrap">
          <button
            type="button"
            className={`dropdown-toggle year-toggle ${showYearDropdown ? 'open' : ''}`}
            aria-haspopup="true"
            aria-expanded={showYearDropdown}
            aria-label={`Select year, currently ${year}`}
            onClick={() => {
              setShowYearDropdown((prev) => !prev);
              setShowMonthDropdown(false);
            }}
          >
            {hideMonth && (
              <Calendar size={14} strokeWidth={2} className="selector-cal-icon" aria-hidden="true" />
            )}
            <span className="tabular-nums">{year}</span>
            <ChevronDown
              size={14}
              strokeWidth={2.2}
              className={`selector-chevron ${showYearDropdown ? 'rotated' : ''}`}
              aria-hidden="true"
            />
          </button>

          <AnimatePresence>
            {showYearDropdown && (
              <motion.div
                className="dropdown-menu year-menu"
                role="listbox"
                aria-label="Choose year"
                initial={{ opacity: 0, y: -6, scale: 0.96 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -6, scale: 0.96 }}
                transition={{ duration: 0.16, ease: [0.16, 1, 0.3, 1] }}
              >
                {years.map((y) => {
                  const isDisabled = y < minYear;
                  const isActive = y === year;
                  return (
                    <button
                      key={y}
                      type="button"
                      role="option"
                      aria-selected={isActive}
                      className={`dropdown-item tabular-nums ${isActive ? 'active' : ''}`}
                      disabled={isDisabled}
                      onClick={() => {
                        if (isDisabled) return;
                        if (y === minYear && month < minMonth) {
                          onChange(minMonth, y);
                        } else {
                          onChange(month, y);
                        }
                        setShowYearDropdown(false);
                      }}
                    >
                      {y}
                    </button>
                  );
                })}
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      <button
        type="button"
        className="nav-btn nav-arrow"
        onClick={handleNext}
        aria-label={hideMonth ? 'Next year' : 'Next month'}
        title={hideMonth ? 'Next year' : 'Next month'}
      >
        <ChevronRight size={16} strokeWidth={2.2} aria-hidden="true" />
      </button>
    </div>
  );
};

export default MonthSelector;
