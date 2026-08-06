import React, { useState, useEffect } from 'react';
import { useDashboard } from '../hooks/useDashboard';
import { useCategories } from '../hooks/useCategories';
import BalanceCard from '../components/BalanceCard';
import SpendingChart from '../components/SpendingChart';
import TrendChart from '../components/TrendChart';
import CategoryPieChart from '../components/CategoryPieChart';
import MonthSelector from '../components/MonthSelector';
import CategoryFilter from '../components/CategoryFilter';
import './Dashboard.css';

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' }).format(amount);
};

const Dashboard = () => {
  const currentDate = new Date();
  const [selectedMonth, setSelectedMonth] = useState(currentDate.getMonth());
  const [selectedYear, setSelectedYear] = useState(currentDate.getFullYear());
  const [excludedCategoryIds, setExcludedCategoryIds] = useState(new Set());
  
  const { fetchDashboardData, data: dashboardData, loading, error } = useDashboard();
  const { categories } = useCategories();
  
  useEffect(() => {
    fetchDashboardData(selectedMonth, selectedYear);
  }, [selectedMonth, selectedYear, fetchDashboardData]);
  
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
      
      <div className="dashboard-content">
        <div className="dashboard-main dashboard-separated">
          {dashboardData.walletsData.map(walletData => {
            // Apply category exclusions to expenses for this specific wallet
            const filteredExpenses = walletData.expenses.filter(e => 
              !excludedCategoryIds.has(e.category_id)
            );
            
            return (
              <div key={walletData.wallet.id} className="wallet-section">
                <div className="wallet-section-header" style={{ borderBottom: `3px solid ${walletData.wallet.color}` }}>
                  <h2>{walletData.wallet.name} Overview</h2>
                </div>
                
                <div className="dashboard-balances single-balance">
                  <BalanceCard 
                    walletName={walletData.wallet.name} 
                    totalIncome={walletData.totalIncome}
                    totalExpenses={walletData.totalExpenses}
                    walletColor={walletData.wallet.color}
                  />
                </div>
                
                <section className="chart-section full-width">
                  <h3>Spending Overview</h3>
                  <SpendingChart data={filteredExpenses} />
                </section>
                
                <div className="chart-row">
                  <section className="chart-section half-width">
                    <h3>6-Month Trend</h3>
                    <TrendChart data={walletData.trendData} />
                  </section>
                  
                  <section className="chart-section half-width">
                    <h3>Expenses by Category</h3>
                    <CategoryPieChart data={filteredExpenses} categories={categories} />
                  </section>
                </div>
                
                <section className="averages-section">
                  <h3>3-Month Averages</h3>
                  <div className="table-responsive">
                    <table className="averages-table">
                      <thead>
                        <tr>
                          <th>Category</th>
                          <th>Average</th>
                        </tr>
                      </thead>
                      <tbody>
                        {walletData.averages.length === 0 ? (
                          <tr><td colSpan="2" className="empty-row">No data for last 3 months</td></tr>
                        ) : (
                          walletData.averages.map(avg => (
                            <tr key={avg.category_id}>
                              <td>{avg.category_name}</td>
                              <td>{formatCurrency(avg.amount)}</td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </section>
              </div>
            );
          })}
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
