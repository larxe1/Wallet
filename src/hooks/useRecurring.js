import { useState, useEffect } from 'react';
import { supabase } from '../lib/supabase';
import { useAuth } from '../context/AuthContext';

export const useRecurring = () => {
  const { user } = useAuth();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchRecurring = async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const { data: recurring, error: err } = await supabase
        .from('recurring_transactions')
        .select(`
          *,
          wallets(name, color),
          categories(name, icon),
          payment_methods(label, type)
        `)
        .eq('user_id', user.id)
        .order('day_of_month', { ascending: true });
        
      if (err) throw err;
      setData(recurring || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecurring();
  }, [user]);

  const addRecurring = async (recurring) => {
    setLoading(true);
    try {
      const { data: newRec, error: err } = await supabase
        .from('recurring_transactions')
        .insert([{ ...recurring, user_id: user.id }])
        .select(`
          *,
          wallets(name, color),
          categories(name, icon),
          payment_methods(label, type)
        `)
        .single();
        
      if (err) throw err;
      setData([...data, newRec]);
      return { data: newRec, error: null };
    } catch (err) {
      setError(err.message);
      return { data: null, error: err.message };
    } finally {
      setLoading(false);
    }
  };

  const updateRecurring = async (id, updates) => {
    setLoading(true);
    try {
      const { data: updated, error: err } = await supabase
        .from('recurring_transactions')
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
      setData(data.map(r => r.id === id ? updated : r));
      return { data: updated, error: null };
    } catch (err) {
      setError(err.message);
      return { data: null, error: err.message };
    } finally {
      setLoading(false);
    }
  };

  const deleteRecurring = async (id) => {
    setLoading(true);
    try {
      const { error: err } = await supabase
        .from('recurring_transactions')
        .delete()
        .eq('id', id)
        .eq('user_id', user.id);
        
      if (err) throw err;
      setData(data.filter(r => r.id !== id));
      return { error: null };
    } catch (err) {
      setError(err.message);
      return { error: err.message };
    } finally {
      setLoading(false);
    }
  };

  const toggleActive = async (id, isActive) => {
    return updateRecurring(id, { is_active: isActive });
  };

  return { data, recurring: data, loading, error, fetchRecurring, addRecurring, updateRecurring, deleteRecurring, toggleActive, toggleRecurringStatus: toggleActive };
};
