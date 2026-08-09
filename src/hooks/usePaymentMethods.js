import { useState, useEffect } from 'react';
import { supabase } from '../lib/supabase';
import { useAuth } from '../context/AuthContext';

export const usePaymentMethods = () => {
  const { user } = useAuth();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchPaymentMethods = async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const { data: pms, error: err } = await supabase
        .from('payment_methods')
        .select('*')
        .eq('user_id', user.id)
        .order('sort_order', { ascending: true });
        
      if (err) throw err;
      setData(pms || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPaymentMethods();
  }, [user]);

  const addPaymentMethod = async (pm) => {
    setLoading(true);
    try {
      const { data: newPm, error: err } = await supabase
        .from('payment_methods')
        .insert([{ ...pm, user_id: user.id }])
        .select()
        .single();
        
      if (err) throw err;
      setData([...data, newPm]);
      return { data: newPm, error: null };
    } catch (err) {
      setError(err.message);
      return { data: null, error: err.message };
    } finally {
      setLoading(false);
    }
  };

  const updatePaymentMethod = async (id, updates) => {
    setLoading(true);
    try {
      const { data: updated, error: err } = await supabase
        .from('payment_methods')
        .update(updates)
        .eq('id', id)
        .eq('user_id', user.id)
        .select()
        .single();
        
      if (err) throw err;
      setData(data.map(p => p.id === id ? updated : p));
      return { data: updated, error: null };
    } catch (err) {
      setError(err.message);
      return { data: null, error: err.message };
    } finally {
      setLoading(false);
    }
  };

  const deletePaymentMethod = async (id) => {
    setLoading(true);
    try {
      const { error: err } = await supabase
        .from('payment_methods')
        .delete()
        .eq('id', id)
        .eq('user_id', user.id);
        
      if (err) throw err;
      setData(data.filter(p => p.id !== id));
      return { error: null };
    } catch (err) {
      setError(err.message);
      return { error: err.message };
    } finally {
      setLoading(false);
    }
  };

  const updatePaymentMethodOrder = async (orderedIds) => {
    // Optimistic local update
    const newOrder = [];
    orderedIds.forEach(id => {
      const pm = data.find(p => p.id === id);
      if (pm) newOrder.push(pm);
    });
    setData(newOrder);
    
    setLoading(true);
    try {
      // Use Promise.all with individual updates to avoid upsert missing column errors
      const updatePromises = orderedIds.map((id, index) => 
        supabase
          .from('payment_methods')
          .update({ sort_order: index + 1 })
          .eq('id', id)
          .eq('user_id', user.id)
      );
      
      await Promise.all(updatePromises);
      
      // refetch to ensure correct ordering locally
      await fetchPaymentMethods();
      return { error: null };
    } catch (err) {
      setError(err.message);
      return { error: err.message };
    } finally {
      setLoading(false);
    }
  };

  return { data, paymentMethods: data, loading, error, fetchPaymentMethods, addPaymentMethod, updatePaymentMethod, deletePaymentMethod, updatePaymentMethodOrder };
};
