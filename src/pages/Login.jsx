import React, { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';
import { AlertCircle, ArrowRight, Eye, EyeOff, Lock, Mail } from 'lucide-react';
import { OWNER_EMAIL, useAuth } from '../context/AuthContext';
import './Login.css';

const Login = () => {
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const { user, error: authError, signIn, clearError } = useAuth();
  const navigate = useNavigate();

  if (user) {
    return <Navigate to="/dashboard" replace />;
  }

  const displayError = error || authError;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    clearError?.();
    setLoading(true);

    try {
      await signIn(OWNER_EMAIL, password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-ambient-mesh" aria-hidden="true" />

      <motion.main
        className="login-card"
        initial={{ opacity: 0, y: 12, scale: 0.985 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.32, ease: [0.16, 1, 0.3, 1] }}
      >
        <div className="login-header">
          <div className="login-brand-mark" aria-hidden="true">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
              <path
                d="M4 16.5L9.5 9L13.5 13.5L19.5 6"
                stroke="#051A13"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <circle cx="19.5" cy="6" r="2.2" fill="#C8F865" />
            </svg>
          </div>
          <h1>Money Tracker</h1>
          <p>Personal wallet for {OWNER_EMAIL} — stays signed in on this device.</p>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          {displayError && (
            <div className="login-error-banner" role="alert">
              <AlertCircle size={17} aria-hidden="true" />
              <span>{displayError}</span>
            </div>
          )}

          <div className="login-field">
            <label htmlFor="email">Email</label>
            <div className="login-input-wrap is-locked">
              <Mail className="login-input-icon" size={16} aria-hidden="true" />
              <input
                id="email"
                type="email"
                autoComplete="username"
                value={OWNER_EMAIL}
                readOnly
                aria-readonly="true"
              />
            </div>
          </div>

          <div className="login-field">
            <label htmlFor="password">Password</label>
            <div className="login-input-wrap">
              <Lock className="login-input-icon" size={16} aria-hidden="true" />
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
              <button
                type="button"
                className="password-toggle-btn"
                onClick={() => setShowPassword((prev) => !prev)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                aria-pressed={showPassword}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          <button type="submit" className="login-submit-btn" disabled={loading}>
            <span>{loading ? 'Signing In…' : 'Sign In'}</span>
            {!loading && <ArrowRight size={17} aria-hidden="true" />}
          </button>
        </form>
      </motion.main>
    </div>
  );
};

export default Login;
