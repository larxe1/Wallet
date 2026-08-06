import { useState, useCallback } from 'react';
import { supabase } from '../lib/supabase';
import { useAuth } from '../context/AuthContext';
import { format, subMonths } from 'date-fns';

export const useDashboard = () => {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchDashboardData = useCallback(async (month, year) => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      // 1. Calculate dates for current month
      // month is 0-indexed in JS
      const startOfMonth = new Date(year, month, 1);
      const endOfMonth = new Date(year, month + 1, 0);

      // 2. Fetch wallets FIRST to identify Alex's wallet
      const { data: rawWallets, error: walError } = await supabase
        .from('wallets')
        .select('id, name, color')
        .eq('user_id', user.id);
        
      if (walError) throw walError;
      
      const alexId = (rawWallets || []).find(w => w.name.toLowerCase() === 'alex')?.id;

      // 3. Fetch expenses for the month
      let expQuery = supabase
        .from('expenses')
        .select(`
          amount,
          category_id,
          wallet_id,
          categories(name, icon),
          wallets(name)
        `)
        .eq('user_id', user.id)
        .gte('expense_date', format(startOfMonth, 'yyyy-MM-dd'))
        .lte('expense_date', format(endOfMonth, 'yyyy-MM-dd'));
        
      if (alexId) expQuery = expQuery.neq('wallet_id', alexId);
        
      const { data: rawExpenses, error: expError } = await expQuery;

      if (expError) throw expError;

      // Aggregate expenses by category AND wallet to satisfy the charts
      const expAgg = {};
      (rawExpenses || []).forEach(exp => {
        const key = `${exp.category_id}-${exp.wallet_id}`;
        if (!expAgg[key]) {
          expAgg[key] = {
            category_id: exp.category_id,
            wallet_id: exp.wallet_id,
            categoryName: exp.categories?.name || 'Unknown',
            name: exp.categories?.name || 'Unknown',
            categoryIcon: exp.categories?.icon || '❓',
            icon: exp.categories?.icon || '❓',
            walletName: exp.wallets?.name || 'Unknown',
            totalAmount: 0,
            value: 0
          };
        }
        expAgg[key].totalAmount += exp.amount;
        expAgg[key].value += exp.amount;
      });
      const expenses = Object.values(expAgg);

      // 4. Calculate total balance for valid wallets
      let allIncQuery = supabase.from('income').select('amount, wallet_id').eq('user_id', user.id);
      let allExpQuery = supabase.from('expenses').select('amount, wallet_id').eq('user_id', user.id);
      
      if (alexId) {
        allIncQuery = allIncQuery.neq('wallet_id', alexId);
        allExpQuery = allExpQuery.neq('wallet_id', alexId);
      }
      
      const { data: allIncome } = await allIncQuery;
      const { data: allExpenses } = await allExpQuery;
      
      let totalBalance = 0;
      let totalIncomeAll = 0;
      let totalExpensesAll = 0;
      
      const walletBalances = {};
      (rawWallets || []).forEach(w => {
        walletBalances[w.id] = { id: w.id, name: w.name, color: w.color, totalIncome: 0, totalExpenses: 0 };
      });

      (allIncome || []).forEach(inc => { 
        totalBalance += inc.amount; 
        totalIncomeAll += inc.amount;
        if (walletBalances[inc.wallet_id]) walletBalances[inc.wallet_id].totalIncome += inc.amount;
      });
      (allExpenses || []).forEach(exp => { 
        totalBalance -= exp.amount; 
        totalExpensesAll += exp.amount;
        if (walletBalances[exp.wallet_id]) walletBalances[exp.wallet_id].totalExpenses += exp.amount;
      });

      // 5. Trend Data (last 6 months)
      const trendData = [];
      for (let i = 5; i >= 0; i--) {
        const d = subMonths(startOfMonth, i);
        const mStart = new Date(d.getFullYear(), d.getMonth(), 1);
        const mEnd = new Date(d.getFullYear(), d.getMonth() + 1, 0);
        
        let mExpQuery = supabase
          .from('expenses')
          .select('amount')
          .eq('user_id', user.id)
          .gte('expense_date', format(mStart, 'yyyy-MM-dd'))
          .lte('expense_date', format(mEnd, 'yyyy-MM-dd'));
          
        if (alexId) mExpQuery = mExpQuery.neq('wallet_id', alexId);
          
        const { data: mExp } = await mExpQuery;
          
        const mTotal = (mExp || []).reduce((sum, e) => sum + e.amount, 0);
        trendData.push({
          label: format(d, 'MMM'),
          total: mTotal
        });
      }

      // 6. 3-Month Averages
      const threeMonthsAgo = subMonths(startOfMonth, 3);
      let avgExpQuery = supabase
        .from('expenses')
        .select('amount, category_id, categories(name)')
        .eq('user_id', user.id)
        .gte('expense_date', format(threeMonthsAgo, 'yyyy-MM-dd'))
        .lt('expense_date', format(startOfMonth, 'yyyy-MM-dd'));
        
      if (alexId) avgExpQuery = avgExpQuery.neq('wallet_id', alexId);
        
      const { data: avgExp } = await avgExpQuery;
        
      const avgAgg = {};
      (avgExp || []).forEach(exp => {
        if (!avgAgg[exp.category_id]) {
          avgAgg[exp.category_id] = {
            category_id: exp.category_id,
            category_name: exp.categories?.name || 'Unknown',
            amount: 0
          };
        }
        avgAgg[exp.category_id].amount += (exp.amount / 3);
      });
      const averages = Object.values(avgAgg).sort((a, b) => b.amount - a.amount).slice(0, 5);

      setData({
        expenses,
        totalBalance,
        totalIncomeAll,
        totalExpensesAll,
        walletBalances: Object.values(walletBalances),
        trendData,
        averages
      });

    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [user]);

  return { fetchDashboardData, data, loading, error };
};
