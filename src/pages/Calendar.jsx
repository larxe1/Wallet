import React, { useState } from 'react';
import { format, parseISO, addDays, isBefore, isSameMonth } from 'date-fns';
import { useExpenses } from '../hooks/useExpenses';
import { useIncome } from '../hooks/useIncome';
import { useWallets } from '../hooks/useWallets';
import { useCategories } from '../hooks/useCategories';
import { usePaymentMethods } from '../hooks/usePaymentMethods';
import { useRecurring } from '../hooks/useRecurring';
import CalendarView from '../components/CalendarView';
import ExpenseForm from '../components/ExpenseForm';
import IncomeForm from '../components/IncomeForm';
import MonthSelector from '../components/MonthSelector';
import WalletFilter from '../components/WalletFilter';
import './Calendar.css';

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' }).format(amount);
};

const Calendar = () => {
  const currentDate = new Date();
  const [selectedMonth, setSelectedMonth] = useState(currentDate.getMonth());
  const [selectedYear, setSelectedYear] = useState(currentDate.getFullYear());
  const [selectedWalletId, setSelectedWalletId] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null);
  
  const [showExpenseForm, setShowExpenseForm] = useState(false);
  const [showIncomeForm, setShowIncomeForm] = useState(false);
  
  const { expenses, addExpense, updateExpense, deleteExpense } = useExpenses(selectedMonth, selectedYear);
  const { income, addIncome, updateIncome, deleteIncome } = useIncome(selectedMonth, selectedYear);
  const { recurring } = useRecurring();
  const { wallets } = useWallets();
  const { categories } = useCategories();
  const { paymentMethods } = usePaymentMethods();
  
  const filteredExpenses = expenses.filter(e => !selectedWalletId || e.wallet_id === selectedWalletId);
  const filteredIncome = income.filter(i => !selectedWalletId || i.wallet_id === selectedWalletId);
  
  // Calculate projected recurring transactions for the selected month
  const daysInMonth = new Date(selectedYear, selectedMonth + 1, 0).getDate();
  const monthStart = new Date(selectedYear, selectedMonth, 1);
  const monthEnd = new Date(selectedYear, selectedMonth, daysInMonth);
  
  const projectedExpenses = [];
  const projectedIncome = [];
  
  recurring
    .filter(r => r.is_active && (!selectedWalletId || r.wallet_id === selectedWalletId))
    .forEach(r => {
      const type = r.frequency_type || 'monthly';
      const occurrences = [];
      
      if (type === 'monthly') {
        const day = Math.min(r.day_of_month, daysInMonth);
        occurrences.push(new Date(selectedYear, selectedMonth, day));
      } else if (type === 'interval' && r.start_date && r.interval_days) {
        let current = parseISO(r.start_date);
        
        // Fast forward to at least the start of the current month
        while (isBefore(current, monthStart)) {
          current = addDays(current, r.interval_days);
        }
        
        // Add all occurrences within the selected month
        while (isSameMonth(current, monthStart)) {
          occurrences.push(current);
          current = addDays(current, r.interval_days);
        }
      }
      
      occurrences.forEach((occDate, idx) => {
        const dateStr = format(occDate, 'yyyy-MM-dd');
        const proj = {
          ...r,
          id: `proj-${r.id}-${idx}`,
          isProjected: true
        };
        
        if (r.type === 'income') {
          proj.income_date = dateStr;
          projectedIncome.push(proj);
        } else {
          proj.expense_date = dateStr;
          projectedExpenses.push(proj);
        }
      });
    });
    
  const allExpenses = [...filteredExpenses, ...projectedExpenses];
  const allIncome = [...filteredIncome, ...projectedIncome];
  
  // Use expense_date / income_date!
  const dayExpenses = selectedDate ? allExpenses.filter(e => e.expense_date === format(selectedDate, 'yyyy-MM-dd')) : [];
  const dayIncome = selectedDate ? allIncome.filter(i => i.income_date === format(selectedDate, 'yyyy-MM-dd')) : [];
  
  const totalDayExpense = dayExpenses.reduce((sum, e) => sum + Number(e.amount), 0);
  const totalDayIncome = dayIncome.reduce((sum, i) => sum + Number(i.amount), 0);
  const netDay = totalDayIncome - totalDayExpense;
  
  return (
    <div className="calendar-container">
      <header className="page-header">
        <h1>Calendar</h1>
        <MonthSelector 
          month={selectedMonth} 
          year={selectedYear} 
          minMonth={7}
          minYear={2026}
          onChange={(m, y) => { setSelectedMonth(m); setSelectedYear(y); }} 
        />
      </header>
      
      <div className="filters-section calendar-filters">
        <WalletFilter wallets={wallets} selected={selectedWalletId} onChange={setSelectedWalletId} />
        <div className="calendar-actions">
          <button className="add-btn" onClick={() => setShowExpenseForm(true)}>+ Expense</button>
          <button className="add-btn income-btn" onClick={() => setShowIncomeForm(true)}>+ Income</button>
        </div>
      </div>

      <div className="calendar-layout">
        <div className="calendar-main">
          <CalendarView 
            month={selectedMonth + 1} 
            year={selectedYear} 
            expenses={allExpenses} 
            income={allIncome} 
            onDayClick={(dateStr) => setSelectedDate(new Date(dateStr))}
            selectedDate={selectedDate ? format(selectedDate, 'yyyy-MM-dd') : null}
          />
        </div>
        
        {selectedDate && (
          <aside className="calendar-sidebar slide-in-right">
            <div className="sidebar-header">
              <h2>{format(selectedDate, 'MMM d, yyyy')}</h2>
              <button className="close-btn" onClick={() => setSelectedDate(null)}>✖</button>
            </div>
            
            <div className="day-summary">
              <div className="summary-row">
                <span>Income</span>
                <span className="success">{formatCurrency(totalDayIncome)}</span>
              </div>
              <div className="summary-row">
                <span>Expenses</span>
                <span className="danger">{formatCurrency(totalDayExpense)}</span>
              </div>
              <div className="summary-row total">
                <span>Net</span>
                <span className={netDay >= 0 ? 'success' : 'danger'}>{formatCurrency(netDay)}</span>
              </div>
            </div>
            
            <div className="day-transactions">
              <h3>Transactions</h3>
              {dayIncome.length === 0 && dayExpenses.length === 0 && (
                <p className="no-data">No transactions for this day.</p>
              )}
              
              {dayIncome.map(inc => (
                <div key={inc.id} className="tx-item income-tx">
                  <div className="tx-info">
                    <span className="tx-icon">💰</span>
                    <span className="tx-desc">{inc.description || 'Income'}</span>
                  </div>
                  <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                    <span className="tx-amount">+{formatCurrency(inc.amount)}</span>
                    {!inc.isProjected && (
                      <button 
                        className="tx-delete-btn" 
                        onClick={() => deleteIncome(inc.id)}
                        title="Delete Income"
                      >
                        X
                      </button>
                    )}
                  </div>
                </div>
              ))}
              
              {dayExpenses.map(exp => (
                <div key={exp.id} className={`tx-item expense-tx ${exp.isProjected ? 'projected' : ''}`}>
                  <div className="tx-info">
                    <span className="tx-icon">{exp.isProjected ? '🔄' : (exp.categories?.icon || '💸')}</span>
                    <span className="tx-desc">
                      {exp.description || exp.categories?.name || 'Expense'}
                      {exp.isProjected && <span style={{fontSize: '0.7em', color: '#888', marginLeft: '6px'}}>(Recurring)</span>}
                    </span>
                  </div>
                  <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                    <span className="tx-amount">-{formatCurrency(exp.amount)}</span>
                    {!exp.isProjected && (
                      <button 
                        className="tx-delete-btn" 
                        onClick={() => deleteExpense(exp.id)}
                        title="Delete Expense"
                      >
                        X
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </aside>
        )}
      </div>

      {showExpenseForm && (
        <ExpenseForm 
          isOpen={showExpenseForm}
          onClose={() => setShowExpenseForm(false)}
          onSave={async (data) => {
            await addExpense(data);
            setShowExpenseForm(false);
          }}
          wallets={wallets}
          categories={categories}
          paymentMethods={paymentMethods}
        />
      )}
      
      {showIncomeForm && (
        <IncomeForm 
          isOpen={showIncomeForm}
          onClose={() => setShowIncomeForm(false)}
          onSave={async (data) => {
            await addIncome(data);
            setShowIncomeForm(false);
          }}
          wallets={wallets}
        />
      )}
    </div>
  );
};

export default Calendar;
