import { useState, useEffect } from 'react';
import { supabase } from '../lib/supabase';
import { useAuth } from '../context/AuthContext';
import { format } from 'date-fns';

export const useExpenses = (month, year) => {
  const { user } = useAuth();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchExpenses = async (filters = {}) => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      let query = supabase
        .from('expenses')
        .select(`
          *,
          wallets(name, color),
          categories(name, icon),
          payment_methods(label, type)
        `)
        .eq('user_id', user.id);

      if (filters.walletId) {
        query = query.eq('wallet_id', filters.walletId);
      }
      if (filters.categoryId) {
        query = query.eq('category_id', filters.categoryId);
      }
      if (filters.paymentMethodId) {
        query = query.eq('payment_method_id', filters.paymentMethodId);
      }
      if (filters.startDate) {
        query = query.gte('expense_date', filters.startDate);
      }
      if (filters.endDate) {
        query = query.lte('expense_date', filters.endDate);
      }
      if (filters.year && filters.month) {
        const start = new Date(filters.year, filters.month - 1, 1);
        const end = new Date(filters.year, filters.month, 0);
        query = query.gte('expense_date', format(start, 'yyyy-MM-dd')).lte('expense_date', format(end, 'yyyy-MM-dd'));
      }

      query = query.order('expense_date', { ascending: false });

      const { data: expenses, error: err } = await query;
        
      if (err) throw err;
      setData(expenses || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (month !== undefined && year !== undefined) {
      fetchExpenses({ month: month + 1, year }); // JS month is 0-indexed, Supabase uses 1-12 in this filter logic
    } else {
      fetchExpenses();
    }
  }, [user, month, year]);

  const addExpense = async (expense) => {
    setLoading(true);
    try {
      const payload = {
        ...expense,
        user_id: user.id,
        expense_date: expense.expense_date || format(new Date(), 'yyyy-MM-dd'),
      };
      const { data: newExp, error: err } = await supabase
        .from('expenses')
        .insert([payload])
        .select(`
          *,
          wallets(name, color),
          categories(name, icon),
          payment_methods(label, type)
        `)
        .single();
        
      if (err) throw err;
      setData([newExp, ...data]);
      return { data: newExp, error: null };
    } catch (err) {
      setError(err.message);
      return { data: null, error: err.message };
    } finally {
      setLoading(false);
    }
  };

  const addMultipleExpenses = async (expensesArray) => {
    setLoading(true);
    try {
      const payloads = expensesArray.map(exp => ({
        ...exp,
        user_id: user.id,
        expense_date: exp.expense_date || format(new Date(), 'yyyy-MM-dd'),
      }));
      
      const { data: newExps, error: err } = await supabase
        .from('expenses')
        .insert(payloads)
        .select(`
          *,
          wallets(name, color),
          categories(name, icon),
          payment_methods(label, type)
        `);
        
      if (err) throw err;
      setData([...(newExps || []), ...data]);
      return { data: newExps, error: null };
    } catch (err) {
      setError(err.message);
      return { data: null, error: err.message };
    } finally {
      setLoading(false);
    }
  };

  const updateExpense = async (id, updates) => {
    setLoading(true);
    try {
      const { data: updated, error: err } = await supabase
        .from('expenses')
        .update(updates)
        .eq('id', id)
        .eq('user_id', user.id)
        .select(`
          *,
          wallets(name, color),
          categories(name, icon),
          payment_methods(label, type)
        `)
        .single();
        
      if (err) throw err;
      setData(data.map(e => e.id === id ? updated : e));
      return { data: updated, error: null };
    } catch (err) {
      setError(err.message);
      return { data: null, error: err.message };
    } finally {
      setLoading(false);
    }
  };

  const deleteExpense = async (id) => {
    setLoading(true);
    try {
      const { error: err } = await supabase
        .from('expenses')
        .delete()
        .eq('id', id)
        .eq('user_id', user.id);
        
      if (err) throw err;
      setData(data.filter(e => e.id !== id));
      return { error: null };
    } catch (err) {
      setError(err.message);
      return { error: err.message };
    } finally {
      setLoading(false);
    }
  };

  return { data, expenses: data, loading, error, fetchExpenses, addExpense, addMultipleExpenses, updateExpense, deleteExpense };
};
