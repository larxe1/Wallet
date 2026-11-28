import React, { createContext, useContext, useEffect, useState } from 'react';
import { supabase } from '../lib/supabase.js';

export const OWNER_EMAIL = 'ec.mataac@gmail.com';

const AuthContext = createContext({});

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const applySession = async (session) => {
      const rawEmail = session?.user?.email;
      if (rawEmail && rawEmail.trim().toLowerCase() !== OWNER_EMAIL) {
        setUser(null);
        setError(`This app is locked to ${OWNER_EMAIL}.`);
        try {
          await supabase.auth.signOut();
        } catch {
          // Ignore signOut errors while rejecting a non-owner session.
        }
        return;
      }
      setUser(session?.user ?? null);
    };

    const getSession = async () => {
      try {
        const { data: { session }, error: sessionError } = await supabase.auth.getSession();
        if (sessionError) throw sessionError;
        await applySession(session);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    getSession();

    const { data: { subscription } } = supabase.auth.onAuthStateChange(
      (_event, session) => {
        applySession(session);
      }
    );

    return () => {
      subscription?.unsubscribe();
    };
  }, []);

  const signIn = async (email, password) => {
    try {
      setError(null);
      const normalizedEmail = (email || OWNER_EMAIL).trim().toLowerCase();
      if (normalizedEmail !== OWNER_EMAIL) {
        throw new Error(`This app is locked to ${OWNER_EMAIL}.`);
      }
      const { error: signInError } = await supabase.auth.signInWithPassword({
        email: OWNER_EMAIL,
        password
      });
      if (signInError) throw signInError;
    } catch (err) {
      setError(err.message);
      throw err;
    }
  };

  const signUp = async () => {
    const err = new Error('Account creation is disabled in single-user mode.');
    setError(err.message);
    throw err;
  };

  const signOut = async () => {
    try {
      setError(null);
      const { error: signOutError } = await supabase.auth.signOut();
      if (signOutError) throw signOutError;
    } catch (err) {
      setError(err.message);
      throw err;
    }
  };

  const clearError = () => {
    setError(null);
  };

  const value = {
    user,
    loading,
    error,
    signIn,
    signUp,
    signOut,
    clearError
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  return useContext(AuthContext);
};
