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
  const [timeframe, setTimeframe] = useState('monthly'); // 'monthly', 'annual'
  const [isPrivacyMode, setIsPrivacyMode] = useState(true);
  const [excludedCategoryIds, setExcludedCategoryIds] = useState(new Set());
  
  // Track which tab is active for each wallet
  // e.g., { 'wallet-uuid': 'overview' }
  const [activeTabs, setActiveTabs] = useState({});
  
  const { fetchDashboardData, data: dashboardData, loading, error } = useDashboard();
  const { categories } = useCategories();
  
  useEffect(() => {
    fetchDashboardData(selectedMonth, selectedYear, timeframe);
  }, [selectedMonth, selectedYear, timeframe, fetchDashboardData]);
  
  const handleCategoryToggle = (id) => {
    const newExcluded = new Set(excludedCategoryIds);
    if (newExcluded.has(id)) {
      newExcluded.delete(id);
    } else {
      newExcluded.add(id);
    }
    setExcludedCategoryIds(newExcluded);
  };
  
  const setTabForWallet = (walletId, tab) => {
    setActiveTabs(prev => ({ ...prev, [walletId]: tab }));
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
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <h1>Dashboard</h1>
          <button 
            onClick={() => setIsPrivacyMode(!isPrivacyMode)}
            className="privacy-toggle-btn"
            title={isPrivacyMode ? "Show Balances" : "Hide Balances"}
            style={{
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              fontSize: '1.2rem',
              color: 'var(--text-secondary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '4px',
              borderRadius: '50%',
              transition: 'background 0.2s, color 0.2s'
            }}
          >
            {isPrivacyMode ? '👁️‍🗨️' : '👁️'}
          </button>
        </div>
        
        <div className="header-controls">
          <div className={`header-month-selector ${timeframe === 'annual' ? 'hide-month-dropdown' : ''}`}>
            <MonthSelector 
              month={selectedMonth} 
              year={selectedYear} 
              minMonth={7}
              minYear={2026}
              onChange={(m, y) => { setSelectedMonth(m); setSelectedYear(y); }} 
            />
          </div>

          <div className="timeframe-toggle">
            {['monthly', 'annual'].map(t => (
              <button 
                key={t}
                className={timeframe === t ? 'active' : ''}
                onClick={() => setTimeframe(t)}
              >
                {t}
              </button>
            ))}
          </div>
        </div>
      </header>
      
      <div className="dashboard-content">
        <div className="dashboard-main dashboard-separated">
          {dashboardData.walletsData.map(walletData => {
            const walletId = walletData.wallet.id;
            const activeTab = activeTabs[walletId] || 'overview'; // default to overview
            
            // Apply category exclusions to expenses for this specific wallet
            const filteredExpenses = walletData.expenses.filter(e => 
              !excludedCategoryIds.has(e.category_id)
            );
            
            return (
              <div key={walletId} className="wallet-section">
                <div className="wallet-section-header" style={{ borderBottom: `3px solid ${walletData.wallet.color}` }}>
                  <h2>{walletData.wallet.name} Overview</h2>
                </div>
                
                <div className="dashboard-balances single-balance">
                  <BalanceCard 
                    walletName={walletData.wallet.name} 
                    totalIncome={walletData.totalIncome}
                    totalExpenses={walletData.totalExpenses}
                    walletColor={walletData.wallet.color}
                    isPrivacyMode={isPrivacyMode}
                  />
                </div>
                
                <div className="wallet-tabs-container">
                  <div className="wallet-tabs-header">
                    <button 
                      className={`wallet-tab-btn ${activeTab === 'overview' ? 'active' : ''}`}
                      onClick={() => setTabForWallet(walletId, 'overview')}
                    >
                      Spending Overview
                    </button>
                    <button 
                      className={`wallet-tab-btn ${activeTab === 'trend' ? 'active' : ''}`}
                      onClick={() => setTabForWallet(walletId, 'trend')}
                    >
                      {timeframe === 'monthly' ? '6-Month Trend' : '12-Month Trend'}
                    </button>
                    <button 
                      className={`wallet-tab-btn ${activeTab === 'category' ? 'active' : ''}`}
                      onClick={() => setTabForWallet(walletId, 'category')}
                    >
                      By Category
                    </button>
                    <button 
                      className={`wallet-tab-btn ${activeTab === 'averages' ? 'active' : ''}`}
                      onClick={() => setTabForWallet(walletId, 'averages')}
                    >
                      {timeframe === 'monthly' ? '3M Averages' : '12M Averages'}
                    </button>
                  </div>
                  
                  <div className="wallet-tab-content chart-section">
                    {activeTab === 'overview' && (
                      <div className="tab-pane fade-in">
                        <SpendingChart data={filteredExpenses} />
                      </div>
                    )}
                    
                    {activeTab === 'trend' && (
                      <div className="tab-pane fade-in">
                        <TrendChart data={walletData.trendData} />
                      </div>
                    )}
                    
                    {activeTab === 'category' && (
                      <div className="tab-pane fade-in">
                        <CategoryPieChart data={filteredExpenses} categories={categories} />
                      </div>
                    )}
                    
                    {activeTab === 'averages' && (
                      <div className="tab-pane fade-in averages-section-tab">
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
                      </div>
                    )}
                  </div>
                </div>
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
