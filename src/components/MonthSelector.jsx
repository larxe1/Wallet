import React, { useState } from 'react';
import { format, setMonth, setYear } from 'date-fns';
import './MonthSelector.css';

const MonthSelector = ({ month, year, onChange, minMonth = 0, minYear = 2000 }) => {
  const [showMonthDropdown, setShowMonthDropdown] = useState(false);
  const [showYearDropdown, setShowYearDropdown] = useState(false);

  // month is 0-indexed (0=Jan, 11=Dec)
  const currentDate = setYear(setMonth(new Date(), month), year);

  const isAtMin = year === minYear && month === minMonth;

  const handlePrevMonth = () => {
    if (isAtMin) return;
    if (month === 0) {
      onChange(11, year - 1);
    } else {
      onChange(month - 1, year);
    }
  };

  const handleNextMonth = () => {
    if (month === 11) {
      onChange(0, year + 1);
    } else {
      onChange(month + 1, year);
    }
  };

  const months = Array.from({ length: 12 }, (_, i) => format(setMonth(new Date(), i), 'MMMM'));
  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: 10 }, (_, i) => currentYear - 5 + i);

  return (
    <div className="month-selector">
      <button className="nav-arrow" onClick={handlePrevMonth} disabled={isAtMin}>&lt;</button>
      
      <div className="selector-dropdowns">
        <div className="dropdown-container">
          <button 
            className="dropdown-toggle" 
            onClick={() => setShowMonthDropdown(!showMonthDropdown)}
          >
            {format(currentDate, 'MMMM')}
          </button>
          {showMonthDropdown && (
            <div className="dropdown-menu">
              {months.map((m, i) => {
                const isDisabled = year === minYear && i < minMonth;
                return (
                  <button 
                    key={m} 
                    className={`dropdown-item ${i === month ? 'active' : ''}`}
                    disabled={isDisabled}
                    onClick={() => {
                      if (isDisabled) return;
                      onChange(i, year);
                      setShowMonthDropdown(false);
                    }}
                  >
                    {m}
                  </button>
                );
              })}
            </div>
          )}
        </div>

        <div className="dropdown-container">
          <button 
            className="dropdown-toggle" 
            onClick={() => setShowYearDropdown(!showYearDropdown)}
          >
            {format(currentDate, 'yyyy')}
          </button>
          {showYearDropdown && (
            <div className="dropdown-menu">
              {years.map((y) => {
                const isDisabled = y < minYear;
                return (
                  <button 
                    key={y} 
                    className={`dropdown-item ${y === year ? 'active' : ''}`}
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
            </div>
          )}
        </div>
      </div>

      <button className="nav-arrow" onClick={handleNextMonth}>&gt;</button>
    </div>
  );
};

export default MonthSelector;
