import React, { useState, useEffect } from 'react';
import { useDashboard } from '../hooks/useDashboard';
import { useWallets } from '../hooks/useWallets';
import { useCategories } from '../hooks/useCategories';
import { useExpenses } from '../hooks/useExpenses';
import { useIncome } from '../hooks/useIncome';
import BalanceCard from '../components/BalanceCard';
import SpendingChart from '../components/SpendingChart';
import TrendChart from '../components/TrendChart';
import CategoryPieChart from '../components/CategoryPieChart';
import MonthSelector from '../components/MonthSelector';
import WalletFilter from '../components/WalletFilter';
import CategoryFilter from '../components/CategoryFilter';
import './Dashboard.css';

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' }).format(amount);
};

const Dashboard = () => {
  const currentDate = new Date();
  const [selectedMonth, setSelectedMonth] = useState(currentDate.getMonth());
  const [selectedYear, setSelectedYear] = useState(currentDate.getFullYear());
  const [selectedWalletId, setSelectedWalletId] = useState(null);
  const [excludedCategoryIds, setExcludedCategoryIds] = useState(new Set());
  
  const { fetchDashboardData, data: dashboardData, loading, error } = useDashboard();
  const { wallets } = useWallets();
  const { categories } = useCategories();
  
  useEffect(() => {
    fetchDashboardData(selectedMonth, selectedYear);
  }, [selectedMonth, selectedYear, fetchDashboardData]);
  
  const handleWalletSelect = (id) => {
    setSelectedWalletId(id);
  };
  
  const handleCategoryToggle = (id) => {
    const newExcluded = new Set(excludedCategoryIds);
    if (newExcluded.has(id)) {
      newExcluded.delete(id);
    } else {
      newExcluded.add(id);
    }
    setExcludedCategoryIds(newExcluded);
  };
  
  if (error) {
    return (
      <div className="dashboard-loading" style={{color: 'red'}}>
        Error loading dashboard: {error}
      </div>
    );
  }
  
  if (loading || !dashboardData) {
    return <div className="dashboard-loading">Loading dashboard...</div>;
  }
  
  const filteredExpenses = dashboardData.expenses.filter(e => 
    (!selectedWalletId || e.wallet_id === selectedWalletId) &&
    !excludedCategoryIds.has(e.category_id)
  );
  
  const totalBalance = dashboardData.totalBalance || 0;
  
  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <h1>Dashboard</h1>
        <MonthSelector 
          month={selectedMonth} 
          year={selectedYear} 
          minMonth={7}
          minYear={2026}
          onChange={(m, y) => { setSelectedMonth(m); setSelectedYear(y); }} 
        />
      </header>
      
      <div className="dashboard-filters">
        <WalletFilter 
          wallets={wallets.filter(w => w.name.toLowerCase() !== 'alex')} 
          selected={selectedWalletId} 
          onChange={handleWalletSelect} 
        />
      </div>
      
      <div className="dashboard-balances">
        {(dashboardData.walletBalances || [])
          .filter(wallet => wallet.name.toLowerCase() !== 'alex')
          .map(wallet => (
          <BalanceCard 
            key={wallet.id} 
            walletName={wallet.name} 
            totalIncome={wallet.totalIncome}
            totalExpenses={wallet.totalExpenses}
            walletColor={wallet.color}
          />
        ))}
      </div>
      
      <div className="dashboard-content">
        <div className="dashboard-main">
          <section className="chart-section full-width">
            <h2>Spending Overview</h2>
            <SpendingChart data={filteredExpenses} />
          </section>
          
          <div className="chart-row">
            <section className="chart-section half-width">
              <h2>6-Month Trend</h2>
              <TrendChart data={dashboardData.trendData || []} />
            </section>
            
            <section className="chart-section half-width">
              <h2>Expenses by Category</h2>
              <CategoryPieChart data={filteredExpenses} categories={categories} />
            </section>
          </div>
          
          <section className="averages-section">
            <h2>3-Month Averages</h2>
            <div className="table-responsive">
              <table className="averages-table">
                <thead>
                  <tr>
                    <th>Category</th>
                    <th>Average</th>
                  </tr>
                </thead>
                <tbody>
                  {(dashboardData.averages || []).map(avg => (
                    <tr key={avg.category_id}>
                      <td>{avg.category_name}</td>
                      <td>{formatCurrency(avg.amount)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
        
        <aside className="dashboard-sidebar">
          <CategoryFilter 
            categories={categories} 
            excludedIds={excludedCategoryIds} 
            onChange={setExcludedCategoryIds} 
          />
        </aside>
      </div>
    </div>
  );
};

export default Dashboard;
