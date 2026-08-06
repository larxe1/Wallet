import React, { useState } from 'react';
import { useExpenses } from '../hooks/useExpenses';
import { useWallets } from '../hooks/useWallets';
import { useCategories } from '../hooks/useCategories';
import { usePaymentMethods } from '../hooks/usePaymentMethods';
import ExpenseForm from '../components/ExpenseForm';
import MonthSelector from '../components/MonthSelector';
import WalletFilter from '../components/WalletFilter';
import { format, parseISO, addMonths } from 'date-fns';
import './Expenses.css';

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' }).format(amount);
};

const Expenses = () => {
  const currentDate = new Date();
  const [selectedMonth, setSelectedMonth] = useState(currentDate.getMonth());
  const [selectedYear, setSelectedYear] = useState(currentDate.getFullYear());
  const [selectedWalletId, setSelectedWalletId] = useState(null);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingExpense, setEditingExpense] = useState(null);
  
  const { expenses, deleteExpense, addExpense, addMultipleExpenses, updateExpense } = useExpenses(selectedMonth, selectedYear);
  const { wallets } = useWallets();
  const { categories } = useCategories();
  const { paymentMethods } = usePaymentMethods();
  
  const handleEdit = (expense) => {
    setEditingExpense(expense);
    setIsFormOpen(true);
  };
  
  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this expense?')) {
      await deleteExpense(id);
    }
  };
  
  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingExpense(null);
  };
  
  const filteredExpenses = expenses.filter(e => !selectedWalletId || e.wallet_id === selectedWalletId);
  const totalAmount = filteredExpenses.reduce((sum, e) => sum + Number(e.amount), 0);
  
  const getCategoryName = (id) => {
    const cat = categories.find(c => c.id === id);
    return cat ? `${cat.icon} ${cat.name}` : 'Unknown';
  };
  
  const getWalletName = (id) => {
    const wallet = wallets.find(w => w.id === id);
    return wallet ? wallet.name : 'Unknown';
  };
  
  return (
    <div className="expenses-container">
      <header className="page-header">
        <h1>Expenses</h1>
        <div className="header-actions">
          <MonthSelector 
            month={selectedMonth} 
            year={selectedYear} 
            minMonth={7}
            minYear={2026}
            onChange={(m, y) => { setSelectedMonth(m); setSelectedYear(y); }} 
          />
          <button className="add-btn" onClick={() => setIsFormOpen(true)}>
            + Add Expense
          </button>
        </div>
      </header>
      
      <div className="filters-section">
        <WalletFilter wallets={wallets} selected={selectedWalletId} onChange={setSelectedWalletId} />
      </div>
      
      <div className="data-table-container">
        {filteredExpenses.length === 0 ? (
          <div className="empty-state">No expenses found for this period.</div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Description</th>
                <th>Category</th>
                <th>Wallet</th>
                <th className="amount-col">Amount</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredExpenses.map(expense => (
                <tr key={expense.id}>
                  <td>{format(new Date(expense.expense_date), 'MMM dd, yyyy')}</td>
                  <td>{expense.description}</td>
                  <td>{getCategoryName(expense.category_id)}</td>
                  <td>{getWalletName(expense.wallet_id)}</td>
                  <td className="amount-col expense-amount">{formatCurrency(expense.amount)}</td>
                  <td className="actions-col">
                    <button className="action-btn edit" onClick={() => handleEdit(expense)}>✎</button>
                    <button className="action-btn delete" onClick={() => handleDelete(expense.id)}>🗑</button>
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td colSpan="4" className="total-label">Total</td>
                <td className="amount-col expense-amount total-amount">{formatCurrency(totalAmount)}</td>
                <td></td>
              </tr>
            </tfoot>
          </table>
        )}
      </div>
      
      {isFormOpen && (
        <ExpenseForm 
          isOpen={isFormOpen}
          expense={editingExpense} 
          onClose={handleCloseForm} 
          onSave={async (data) => {
            if (editingExpense) {
              const { is_installment, installment_months, ...updateData } = data;
              await updateExpense(editingExpense.id, updateData);
            } else {
              if (data.is_installment && data.installment_months > 1) {
                const { is_installment, installment_months, ...expenseData } = data;
                const startDate = parseISO(expenseData.expense_date);
                
                const expensesToCreate = [];
                for (let i = 0; i < installment_months; i++) {
                  const installmentDate = format(addMonths(startDate, i), 'yyyy-MM-dd');
                  expensesToCreate.push({
                    ...expenseData,
                    description: `${expenseData.description} (Installment ${i + 1}/${installment_months})`,
                    expense_date: installmentDate
                  });
                }
                await addMultipleExpenses(expensesToCreate);
              } else {
                const { is_installment, installment_months, ...expenseData } = data;
                await addExpense(expenseData);
              }
            }
            handleCloseForm();
          }}
          wallets={wallets}
          categories={categories}
          paymentMethods={paymentMethods}
        />
      )}
    </div>
  );
};

export default Expenses;
