import { useState, useEffect } from 'react';
import { supabase } from '../lib/supabase';
import { useAuth } from '../context/AuthContext';
import { format } from 'date-fns';

export const useIncome = (month, year) => {
  const { user } = useAuth();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchIncome = async (filters = {}) => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      let query = supabase
        .from('income')
        .select(`
          *,
          wallets(name, color)
        `)
        .eq('user_id', user.id);

      if (filters.walletId) {
        query = query.eq('wallet_id', filters.walletId);
      }
      if (filters.startDate) {
        query = query.gte('income_date', filters.startDate);
      }
      if (filters.endDate) {
        query = query.lte('income_date', filters.endDate);
      }
      if (filters.year && filters.month) {
        const start = new Date(filters.year, filters.month - 1, 1);
        const end = new Date(filters.year, filters.month, 0);
        query = query.gte('income_date', format(start, 'yyyy-MM-dd')).lte('income_date', format(end, 'yyyy-MM-dd'));
      }

      query = query.order('income_date', { ascending: false });

      const { data: income, error: err } = await query;
        
      if (err) throw err;
      setData(income || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (month !== undefined && year !== undefined) {
      fetchIncome({ month: month + 1, year }); // JS month is 0-indexed, Supabase uses 1-12 in this filter logic
    } else {
      fetchIncome();
    }
  }, [user, month, year]);

  const addIncome = async (income) => {
    setLoading(true);
    try {
      const payload = {
        ...income,
        user_id: user.id,
        income_date: income.income_date || format(new Date(), 'yyyy-MM-dd'),
      };
      const { data: newInc, error: err } = await supabase
        .from('income')
        .insert([payload])
        .select(`
          *,
          wallets(name, color)
        `)
        .single();
        
      if (err) throw err;
      setData([newInc, ...data]);
      return { data: newInc, error: null };
    } catch (err) {
      setError(err.message);
      return { data: null, error: err.message };
    } finally {
      setLoading(false);
    }
  };

  const updateIncome = async (id, updates) => {
    setLoading(true);
    try {
      const { data: updated, error: err } = await supabase
        .from('income')
        .update(updates)
        .eq('id', id)
        .eq('user_id', user.id)
        .select(`
          *,
          wallets(name, color)
        `)
        .single();
        
      if (err) throw err;
      setData(data.map(i => i.id === id ? updated : i));
      return { data: updated, error: null };
    } catch (err) {
      setError(err.message);
      return { data: null, error: err.message };
    } finally {
      setLoading(false);
    }
  };

  const deleteIncome = async (id) => {
    setLoading(true);
    try {
      const { error: err } = await supabase
        .from('income')
        .delete()
        .eq('id', id)
        .eq('user_id', user.id);
        
      if (err) throw err;
      setData(data.filter(i => i.id !== id));
      return { error: null };
    } catch (err) {
      setError(err.message);
      return { error: err.message };
    } finally {
      setLoading(false);
    }
  };

  return { data, income: data, loading, error, fetchIncome, addIncome, updateIncome, deleteIncome };
};
