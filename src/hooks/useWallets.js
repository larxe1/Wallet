import { useState, useEffect } from 'react';
import { supabase } from '../lib/supabase';
import { useAuth } from '../context/AuthContext';

export const useWallets = () => {
  const { user } = useAuth();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchWallets = async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const { data: wallets, error: err } = await supabase
        .from('wallets')
        .select('*')
        .eq('user_id', user.id)
        .order('sort_order', { ascending: true });
        
      if (err) throw err;
      setData(wallets || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWallets();
  }, [user]);

  const addWallet = async (wallet) => {
    setLoading(true);
    try {
      const { data: newWallet, error: err } = await supabase
        .from('wallets')
        .insert([{ ...wallet, user_id: user.id }])
        .select()
        .single();
        
      if (err) throw err;
      setData(prev => [...prev, newWallet]);
      return { data: newWallet, error: null };
    } catch (err) {
      setError(err.message);
      return { data: null, error: err.message };
    } finally {
      setLoading(false);
    }
  };

  const updateWallet = async (id, updates) => {
    setLoading(true);
    try {
      const { data: updated, error: err } = await supabase
        .from('wallets')
        .update(updates)
        .eq('id', id)
        .eq('user_id', user.id)
        .select()
        .single();
        
      if (err) throw err;
      setData(prev => prev.map(w => w.id === id ? updated : w));
      return { data: updated, error: null };
    } catch (err) {
      setError(err.message);
      return { data: null, error: err.message };
    } finally {
      setLoading(false);
    }
  };

  const deleteWallet = async (id) => {
    setLoading(true);
    try {
      const { error: err } = await supabase
        .from('wallets')
        .delete()
        .eq('id', id)
        .eq('user_id', user.id);
        
      if (err) throw err;
      setData(prev => prev.filter(w => w.id !== id));
      return { error: null };
    } catch (err) {
      setError(err.message);
      return { error: err.message };
    } finally {
      setLoading(false);
    }
  };

  return { data, wallets: data, loading, error, fetchWallets, addWallet, updateWallet, deleteWallet };
};
