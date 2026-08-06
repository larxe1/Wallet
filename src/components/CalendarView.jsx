import React from 'react';
import { 
  startOfMonth, endOfMonth, startOfWeek, endOfWeek, 
  eachDayOfInterval, format, isSameMonth, isSameDay, isToday, parseISO 
} from 'date-fns';
import './CalendarView.css';

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' }).format(amount);
};

const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

export default function CalendarView({ 
  year, 
  month, 
  expenses = [], 
  income = [], 
  onDayClick,
  selectedDate 
}) {
  // Construct a date object for the current month being viewed
  const currentDate = new Date(year, month - 1, 1);
  const monthStart = startOfMonth(currentDate);
  const monthEnd = endOfMonth(currentDate);
  const startDate = startOfWeek(monthStart);
  const endDate = endOfWeek(monthEnd);

  const days = eachDayOfInterval({ start: startDate, end: endDate });
  const selectedDateObj = selectedDate ? parseISO(selectedDate) : null;

  // Pre-process data for O(1) lookup per day
  const dataMap = {};
  
  const processData = (list, type) => {
    list.forEach(item => {
      const dateStr = item[`${type}_date`]; // expense_date or income_date
      if (!dateStr) return;
      
      // Assumes dateStr is 'YYYY-MM-DD'
      const prefix = dateStr.substring(0, 10);
      if (!dataMap[prefix]) {
        dataMap[prefix] = { expenses: 0, income: 0, expenseItems: [], incomeItems: [] };
      }
      dataMap[prefix][type === 'expense' ? 'expenses' : 'income'] += item.amount;
      dataMap[prefix][type === 'expense' ? 'expenseItems' : 'incomeItems'].push(item);
    });
  };

  processData(expenses, 'expense');
  processData(income, 'income');

  // Find max values to scale opacities
  let maxExpense = 0;
  let maxIncome = 0;
  Object.values(dataMap).forEach(dayData => {
    if (dayData.expenses > maxExpense) maxExpense = dayData.expenses;
    if (dayData.income > maxIncome) maxIncome = dayData.income;
  });

  return (
    <div className="calendar-container">
      <div className="calendar-header">
        <h3 className="calendar-title">{format(currentDate, 'MMMM yyyy')}</h3>
      </div>
      
      <div className="calendar-grid">
        {WEEKDAYS.map(day => (
          <div key={day} className="calendar-weekday">
            {day}
          </div>
        ))}
        
        {days.map(day => {
          const dateStr = format(day, 'yyyy-MM-dd');
          const dayData = dataMap[dateStr] || { expenses: 0, income: 0 };
          const isSelected = selectedDateObj && isSameDay(day, selectedDateObj);
          const isCurrentMonth = isSameMonth(day, currentDate);
          const isTodayDate = isToday(day);

          let expenseOpacity = dayData.expenses > 0 ? Math.max(0.3, dayData.expenses / maxExpense) : 0;
          let incomeOpacity = dayData.income > 0 ? Math.max(0.3, dayData.income / maxIncome) : 0;

          let classNames = ['calendar-day'];
          if (!isCurrentMonth) classNames.push('dimmed');
          if (isSelected) classNames.push('selected');
          if (isTodayDate) classNames.push('today');

          return (
            <div 
              key={dateStr} 
              className={classNames.join(' ')}
              onClick={() => onDayClick && onDayClick(dateStr)}
            >
              <span className="day-number">{format(day, 'd')}</span>
              
              <div className="day-indicators">
                {dayData.income > 0 && (
                  <div 
                    className="indicator income-indicator" 
                    style={{ '--indicator-opacity': incomeOpacity }}
                    title={`Income: ${formatCurrency(dayData.income)}`}
                  >
                    <span className="indicator-amount">{dayData.income > 999 ? (dayData.income/1000).toFixed(1)+'k' : dayData.income}</span>
                  </div>
                )}
                {dayData.expenses > 0 && (
                  <div 
                    className="indicator expense-indicator"
                    style={{ '--indicator-opacity': expenseOpacity }}
                    title={`Expense: ${formatCurrency(dayData.expenses)}`}
                  >
                    <span className="indicator-amount">{dayData.expenses > 999 ? (dayData.expenses/1000).toFixed(1)+'k' : dayData.expenses}</span>
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
