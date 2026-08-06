import { useState, useCallback } from 'react';
import { supabase } from '../lib/supabase';
import { useAuth } from '../context/AuthContext';
import { format, subMonths } from 'date-fns';

export const useDashboard = () => {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchDashboardData = useCallback(async (month, year, timeframe = 'monthly') => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      // 1. Calculate dates based on timeframe
      let startDate, endDate;
      if (timeframe === 'monthly') {
        startDate = new Date(year, month, 1);
        endDate = new Date(year, month + 1, 0);
      } else if (timeframe === 'annual') {
        startDate = new Date(year, 0, 1);
        endDate = new Date(year, 11, 31);
      }

      // 2. Fetch wallets FIRST and initialize our byWallet structure
      const { data: rawWallets, error: walError } = await supabase
        .from('wallets')
        .select('id, name, color')
        .eq('user_id', user.id);
        
      if (walError) throw walError;
      
      const byWallet = {};
      (rawWallets || []).forEach(w => {
         if (w.name.toLowerCase() !== 'alex') {
             byWallet[w.id] = {
                 wallet: w,
                 expenses: [],
                 trendData: [],
                 averages: [],
                 totalIncome: 0,
                 totalExpenses: 0
             };
         }
      });

      // 3. Fetch expenses for the timeframe
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
        .gte('expense_date', format(startDate, 'yyyy-MM-dd'))
        .lte('expense_date', format(endDate, 'yyyy-MM-dd'));
      
      const { data: rawExpenses, error: expError } = await expQuery;

      if (expError) throw expError;

      // Aggregate expenses by category for each wallet
      const expAggByWallet = {};
      (rawExpenses || []).forEach(exp => {
        if (!byWallet[exp.wallet_id]) return; // Skip if wallet is filtered out (like Alex)
        const key = `${exp.wallet_id}-${exp.category_id}`;
        if (!expAggByWallet[key]) {
          expAggByWallet[key] = {
            category_id: exp.category_id,
            categoryName: exp.categories?.name || 'Unknown',
            name: exp.categories?.name || 'Unknown',
            categoryIcon: exp.categories?.icon || '❓',
            icon: exp.categories?.icon || '❓',
            totalAmount: 0,
            value: 0
          };
        }
        expAggByWallet[key].totalAmount += exp.amount;
        expAggByWallet[key].value += exp.amount;
      });

      Object.keys(byWallet).forEach(wId => {
          byWallet[wId].expenses = Object.keys(expAggByWallet)
             .filter(k => k.startsWith(`${wId}-`))
             .map(k => expAggByWallet[k])
             .sort((a, b) => b.value - a.value); // Sort biggest expenses first
      });

      // 4. Calculate total balance for wallets
      const { data: allIncome } = await supabase.from('income').select('amount, wallet_id').eq('user_id', user.id);
      const { data: allExpenses } = await supabase.from('expenses').select('amount, wallet_id').eq('user_id', user.id);
      
      (allIncome || []).forEach(inc => { 
        if (byWallet[inc.wallet_id]) {
            byWallet[inc.wallet_id].totalIncome += inc.amount;
        }
      });
      (allExpenses || []).forEach(exp => { 
        if (byWallet[exp.wallet_id]) {
            byWallet[exp.wallet_id].totalExpenses += exp.amount;
        }
      });

      // 5. Trend Data per wallet
      if (timeframe === 'monthly') {
          for (let i = 5; i >= 0; i--) {
            const d = subMonths(startDate, i);
            const mStart = new Date(d.getFullYear(), d.getMonth(), 1);
            const mEnd = new Date(d.getFullYear(), d.getMonth() + 1, 0);
            
            const { data: mExp } = await supabase
              .from('expenses')
              .select('amount, wallet_id')
              .eq('user_id', user.id)
              .gte('expense_date', format(mStart, 'yyyy-MM-dd'))
              .lte('expense_date', format(mEnd, 'yyyy-MM-dd'));
              
            const mTotalByWallet = {};
            (mExp || []).forEach(e => {
                if (byWallet[e.wallet_id]) {
                    mTotalByWallet[e.wallet_id] = (mTotalByWallet[e.wallet_id] || 0) + e.amount;
                }
            });

            Object.keys(byWallet).forEach(wId => {
                byWallet[wId].trendData.push({
                    label: format(d, 'MMM'),
                    total: mTotalByWallet[wId] || 0
                });
            });
          }
      } else if (timeframe === 'annual') {
          for (let i = 0; i < 12; i++) {
            const mStart = new Date(year, i, 1);
            const mEnd = new Date(year, i + 1, 0);
            
            const { data: mExp } = await supabase
              .from('expenses')
              .select('amount, wallet_id')
              .eq('user_id', user.id)
              .gte('expense_date', format(mStart, 'yyyy-MM-dd'))
              .lte('expense_date', format(mEnd, 'yyyy-MM-dd'));
              
            const mTotalByWallet = {};
            (mExp || []).forEach(e => {
                if (byWallet[e.wallet_id]) {
                    mTotalByWallet[e.wallet_id] = (mTotalByWallet[e.wallet_id] || 0) + e.amount;
                }
            });

            Object.keys(byWallet).forEach(wId => {
                byWallet[wId].trendData.push({
                    label: format(mStart, 'MMM'),
                    total: mTotalByWallet[wId] || 0
                });
            });
          }
      }

      // 6. Averages per wallet
      if (timeframe === 'monthly') {
          const threeMonthsAgo = subMonths(startDate, 3);
          const { data: avgExp } = await supabase
            .from('expenses')
            .select('amount, category_id, wallet_id, categories(name)')
            .eq('user_id', user.id)
            .gte('expense_date', format(threeMonthsAgo, 'yyyy-MM-dd'))
            .lt('expense_date', format(startDate, 'yyyy-MM-dd'));
            
          const avgAggByWallet = {};
          (avgExp || []).forEach(exp => {
            if (!byWallet[exp.wallet_id]) return;
            const key = `${exp.wallet_id}-${exp.category_id}`;
            if (!avgAggByWallet[key]) {
                avgAggByWallet[key] = {
                    category_id: exp.category_id,
                    category_name: exp.categories?.name || 'Unknown',
                    amount: 0
                };
            }
            avgAggByWallet[key].amount += (exp.amount / 3);
          });
          
          Object.keys(byWallet).forEach(wId => {
              byWallet[wId].averages = Object.keys(avgAggByWallet)
                  .filter(k => k.startsWith(`${wId}-`))
                  .map(k => avgAggByWallet[k])
                  .sort((a, b) => b.amount - a.amount)
                  .slice(0, 5); // top 5
          });
      } else if (timeframe === 'annual') {
          const { data: avgExp } = await supabase
            .from('expenses')
            .select('amount, category_id, wallet_id, categories(name)')
            .eq('user_id', user.id)
            .gte('expense_date', format(startDate, 'yyyy-MM-dd'))
            .lte('expense_date', format(endDate, 'yyyy-MM-dd'));
            
          const avgAggByWallet = {};
          (avgExp || []).forEach(exp => {
            if (!byWallet[exp.wallet_id]) return;
            const key = `${exp.wallet_id}-${exp.category_id}`;
            if (!avgAggByWallet[key]) {
                avgAggByWallet[key] = {
                    category_id: exp.category_id,
                    category_name: exp.categories?.name || 'Unknown',
                    amount: 0
                };
            }
            avgAggByWallet[key].amount += (exp.amount / 12); // Average across 12 months
          });
          
          Object.keys(byWallet).forEach(wId => {
              byWallet[wId].averages = Object.keys(avgAggByWallet)
                  .filter(k => k.startsWith(`${wId}-`))
                  .map(k => avgAggByWallet[k])
                  .sort((a, b) => b.amount - a.amount)
                  .slice(0, 5); // top 5
          });
      }

      setData({
        walletsData: Object.values(byWallet).sort((a, b) => a.wallet.name.localeCompare(b.wallet.name))
      });

    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [user]);

  return { fetchDashboardData, data, loading, error };
};
