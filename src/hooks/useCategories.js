import { useState, useEffect } from 'react';
import { supabase } from '../lib/supabase';
import { useAuth } from '../context/AuthContext';

export const useCategories = () => {
  const { user } = useAuth();
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchCategories = async () => {
    if (!user) return;
    setLoading(true);
    setError(null);
    try {
      const { data: categories, error: err } = await supabase
        .from('categories')
        .select('*')
        .eq('user_id', user.id)
        .order('sort_order', { ascending: true });
        
      if (err) throw err;
      setData(categories || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, [user]);

  const addCategory = async (category) => {
    setLoading(true);
    try {
      const { data: newCategory, error: err } = await supabase
        .from('categories')
        .insert([{ ...category, user_id: user.id }])
        .select()
        .single();
        
      if (err) throw err;
      setData(prev => [...prev, newCategory]);
      return { data: newCategory, error: null };
    } catch (err) {
      setError(err.message);
      return { data: null, error: err.message };
    } finally {
      setLoading(false);
    }
  };

  const updateCategory = async (id, updates) => {
    setLoading(true);
    try {
      const { data: updatedCategory, error: err } = await supabase
        .from('categories')
        .update(updates)
        .eq('id', id)
        .eq('user_id', user.id)
        .select()
        .single();
        
      if (err) throw err;
      setData(prev => prev.map(c => c.id === id ? updatedCategory : c));
      return { data: updatedCategory, error: null };
    } catch (err) {
      setError(err.message);
      return { data: null, error: err.message };
    } finally {
      setLoading(false);
    }
  };

  const deleteCategory = async (id) => {
    setLoading(true);
    try {
      const { error: err } = await supabase
        .from('categories')
        .delete()
        .eq('id', id)
        .eq('user_id', user.id);
        
      if (err) throw err;
      setData(prev => prev.filter(c => c.id !== id));
      return { error: null };
    } catch (err) {
      setError(err.message);
      return { error: err.message };
    } finally {
      setLoading(false);
    }
  };

  return { data, categories: data, loading, error, fetchCategories, addCategory, updateCategory, deleteCategory };
};
