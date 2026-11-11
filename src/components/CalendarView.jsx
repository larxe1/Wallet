import React, { useMemo } from 'react';
import { motion } from 'motion/react';
import {
  startOfMonth,
  endOfMonth,
  startOfWeek,
  endOfWeek,
  eachDayOfInterval,
  format,
  isSameMonth,
  isSameDay,
  isToday
} from 'date-fns';
import { Repeat } from 'lucide-react';
import { formatCurrency, parseLocalDate } from '../utils/format';
import './CalendarView.css';

const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

export default function CalendarView({
  year,
  month,
  expenses = [],
  income = [],
  onDayClick,
  selectedDate
}) {
  const currentDate = useMemo(() => new Date(year, month - 1, 1), [year, month]);
  const monthStart = startOfMonth(currentDate);
  const monthEnd = endOfMonth(currentDate);
  const startDate = startOfWeek(monthStart);
  const endDate = endOfWeek(monthEnd);

  const days = useMemo(
    () => eachDayOfInterval({ start: startDate, end: endDate }),
    [startDate, endDate]
  );

  const selectedDateObj = selectedDate ? parseLocalDate(selectedDate) : null;

  const { dataMap, maxExpense, maxIncome } = useMemo(() => {
    const map = {};
    const processData = (list, type) => {
      list.forEach((item) => {
        const dateStr = item[`${type}_date`];
        if (!dateStr) return;

        const prefix = String(dateStr).substring(0, 10);
        if (!map[prefix]) {
          map[prefix] = {
            expenses: 0,
            income: 0,
            hasProjected: false
          };
        }
        const amt = Number(item.amount) || 0;
        map[prefix][type === 'expense' ? 'expenses' : 'income'] += amt;
        if (item.isProjected) {
          map[prefix].hasProjected = true;
        }
      });
    };

    processData(expenses, 'expense');
    processData(income, 'income');

    let maxExp = 0;
    let maxInc = 0;
    Object.values(map).forEach((dayData) => {
      if (dayData.expenses > maxExp) maxExp = dayData.expenses;
      if (dayData.income > maxInc) maxInc = dayData.income;
    });

    return { dataMap: map, maxExpense: maxExp, maxIncome: maxInc };
  }, [expenses, income]);

  return (
    <div className="cal-grid-card">
      <div className="cal-grid-header">
        <h2 className="cal-grid-title">{format(currentDate, 'MMMM yyyy')}</h2>
        <div className="cal-grid-legend" aria-hidden="true">
          <span className="cal-legend-item">
            <span className="cal-legend-dot cal-legend-dot--income" />
            Income
          </span>
          <span className="cal-legend-item">
            <span className="cal-legend-dot cal-legend-dot--expense" />
            Expense
          </span>
          <span className="cal-legend-item">
            <Repeat size={11} />
            Projected
          </span>
        </div>
      </div>

      <div className="cal-grid-weekdays">
        {WEEKDAYS.map((day) => (
          <div key={day} className="cal-grid-weekday">
            {day}
          </div>
        ))}
      </div>

      <div className="cal-grid-cells">
        {days.map((day) => {
          const dateStr = format(day, 'yyyy-MM-dd');
          const dayData = dataMap[dateStr] || {
            expenses: 0,
            income: 0,
            hasProjected: false
          };
          const isSelected = Boolean(selectedDateObj && isSameDay(day, selectedDateObj));
          const isCurrentMonth = isSameMonth(day, currentDate);
          const isTodayDate = isToday(day);

          const expenseIntensity =
            dayData.expenses > 0 && maxExpense > 0
              ? Math.max(0.24, Math.min(0.85, (dayData.expenses / maxExpense) * 0.85))
              : 0;
          const incomeIntensity =
            dayData.income > 0 && maxIncome > 0
              ? Math.max(0.24, Math.min(0.85, (dayData.income / maxIncome) * 0.85))
              : 0;

          const classNames = ['cal-grid-day'];
          if (!isCurrentMonth) classNames.push('is-dimmed');
          if (isSelected) classNames.push('is-selected');
          if (isTodayDate) classNames.push('is-today');

          const ariaParts = [format(day, 'MMMM d, yyyy')];
          if (dayData.income > 0) {
            ariaParts.push(`Income ${formatCurrency(dayData.income)}`);
          }
          if (dayData.expenses > 0) {
            ariaParts.push(`Expenses ${formatCurrency(dayData.expenses)}`);
          }

          return (
            <motion.button
              key={dateStr}
              type="button"
              className={classNames.join(' ')}
              onClick={() => onDayClick && onDayClick(dateStr)}
              whileTap={{ scale: 0.97 }}
              aria-label={ariaParts.join(', ')}
              aria-pressed={isSelected}
            >
              <div className="cal-day-top">
                {dayData.hasProjected ? (
                  <span className="cal-day-projected-badge" title="Includes projected recurring">
                    <Repeat size={10} aria-hidden="true" />
                  </span>
                ) : (
                  <span />
                )}
                <span className="cal-day-number">
                  {format(day, 'd')}
                  {isTodayDate && <span className="cal-day-today-dot" aria-hidden="true" />}
                </span>
              </div>

              <div className="cal-day-indicators">
                {dayData.income > 0 && (
                  <div
                    className="cal-indicator cal-indicator--income"
                    style={{
                      backgroundColor: `rgba(40, 201, 145, ${incomeIntensity * 0.42})`
                    }}
                    title={`Income: ${formatCurrency(dayData.income)}`}
                  >
                    <span className="cal-indicator-dot" aria-hidden="true" />
                    <span className="cal-indicator-amount">
                      +{formatCurrency(dayData.income, { compact: true })}
                    </span>
                  </div>
                )}
                {dayData.expenses > 0 && (
                  <div
                    className="cal-indicator cal-indicator--expense"
                    style={{
                      backgroundColor: `rgba(240, 100, 120, ${expenseIntensity * 0.42})`
                    }}
                    title={`Expenses: ${formatCurrency(dayData.expenses)}`}
                  >
                    <span className="cal-indicator-dot" aria-hidden="true" />
                    <span className="cal-indicator-amount">
                      -{formatCurrency(dayData.expenses, { compact: true })}
                    </span>
                  </div>
                )}
              </div>
            </motion.button>
          );
        })}
      </div>
    </div>
  );
}
