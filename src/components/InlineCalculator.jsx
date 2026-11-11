import React, { useState, useEffect, useRef, useCallback } from 'react';
import { motion } from 'motion/react';
import { Delete, Check, X } from 'lucide-react';
import './InlineCalculator.css';

const SAFE_EXPR_RE = /^[0-9+\-*/().\s]+$/;

function evaluateExpression(raw) {
  const expr = String(raw || '').trim();
  if (!expr || !SAFE_EXPR_RE.test(expr)) return null;
  try {
    // eslint-disable-next-line no-new-func
    const result = Function(`'use strict'; return (${expr})`)();
    if (typeof result !== 'number' || !Number.isFinite(result)) return null;
    return Math.round(result * 100) / 100;
  } catch {
    return null;
  }
}

const InlineCalculator = ({ onResult, onClose }) => {
  const [expression, setExpression] = useState('');
  const [display, setDisplay] = useState('0');
  const [evaluated, setEvaluated] = useState(false);
  const calcRef = useRef(null);

  useEffect(() => {
    calcRef.current?.focus({ preventScroll: true });
  }, []);

  // Close on outside click
  useEffect(() => {
    const handle = (e) => {
      if (calcRef.current && !calcRef.current.contains(e.target)) {
        onClose();
      }
    };
    document.addEventListener('mousedown', handle);
    return () => document.removeEventListener('mousedown', handle);
  }, [onClose]);

  const press = useCallback(
    (val) => {
      const ops = ['+', '-', '*', '/'];
      if (evaluated && !ops.includes(val)) {
        const nextExpr = val === '.' ? '0.' : val;
        setExpression(nextExpr);
        setDisplay(nextExpr);
        setEvaluated(false);
        return;
      }
      setEvaluated(false);
      const newExpr = expression + val;
      setExpression(newExpr);
      const parts = newExpr.split(/[+\-*/]/);
      setDisplay(parts[parts.length - 1] || '0');
    },
    [expression, evaluated]
  );

  const handleEquals = useCallback(() => {
    if (!expression) return;
    const rounded = evaluateExpression(expression);
    if (rounded === null) {
      setDisplay('Error');
      setExpression('');
      setEvaluated(false);
      return;
    }
    setDisplay(String(rounded));
    setExpression(String(rounded));
    setEvaluated(true);
  }, [expression]);

  const handleUse = useCallback(() => {
    const source = expression || (display !== '0' && display !== 'Error' ? display : '');
    const rounded = evaluateExpression(source);
    if (rounded !== null && rounded > 0) {
      setDisplay(String(rounded));
      setExpression(String(rounded));
      setEvaluated(true);
      onResult(String(rounded));
      onClose();
    } else if (source) {
      setDisplay('Error');
      setExpression('');
      setEvaluated(false);
    }
  }, [expression, display, onResult, onClose]);

  const handleBack = useCallback(() => {
    const newExpr = expression.slice(0, -1);
    setExpression(newExpr);
    const parts = newExpr.split(/[+\-*/]/);
    setDisplay(parts[parts.length - 1] || '0');
    setEvaluated(false);
  }, [expression]);

  const handleClear = useCallback(() => {
    setExpression('');
    setDisplay('0');
    setEvaluated(false);
  }, []);

  const handlePercent = useCallback(() => {
    if (!expression) return;
    const val = evaluateExpression(expression);
    if (val === null) return;
    const rounded = Math.round((val / 100) * 100) / 100;
    setExpression(String(rounded));
    setDisplay(String(rounded));
    setEvaluated(true);
  }, [expression]);

  // Keyboard support
  useEffect(() => {
    const handle = (e) => {
      const key = e.key;
      if (key === 'Escape') {
        e.preventDefault();
        e.stopPropagation();
        onClose();
        return;
      }

      if ('0123456789'.includes(key)) {
        e.preventDefault();
        press(key);
      } else if ('+-*/'.includes(key)) {
        e.preventDefault();
        press(key);
      } else if (key === '.' || key === ',') {
        e.preventDefault();
        press('.');
      } else if (key === '%') {
        e.preventDefault();
        handlePercent();
      } else if (key === 'Enter' || key === '=') {
        e.preventDefault();
        e.stopPropagation();
        handleEquals();
      } else if (key === 'Backspace') {
        e.preventDefault();
        handleBack();
      }
    };
    document.addEventListener('keydown', handle, true);
    return () => document.removeEventListener('keydown', handle, true);
  }, [press, handleEquals, handleBack, handlePercent, onClose]);

  const buttons = [
    ['C', 'BACK', '%', '/'],
    ['7', '8', '9', '*'],
    ['4', '5', '6', '-'],
    ['1', '2', '3', '+'],
    ['0', '.', '=', 'USE'],
  ];

  const handleBtn = (val) => {
    if (val === 'C') handleClear();
    else if (val === 'BACK') handleBack();
    else if (val === '=') handleEquals();
    else if (val === 'USE') handleUse();
    else if (val === '%') handlePercent();
    else press(val);
  };

  const getButtonClass = (val) => {
    if (val === 'USE') return 'calc-btn use-btn';
    if (val === '=') return 'calc-btn equals-btn';
    if (['+', '-', '*', '/'].includes(val)) return 'calc-btn op-btn';
    if (val === 'C' || val === 'BACK' || val === '%') return 'calc-btn func-btn';
    return 'calc-btn';
  };

  const renderLabel = (val) => {
    if (val === 'BACK') return <Delete size={16} strokeWidth={2.2} aria-hidden="true" />;
    if (val === 'USE') return <Check size={17} strokeWidth={2.6} aria-hidden="true" />;
    if (val === '*') return '×';
    if (val === '/') return '÷';
    return val;
  };

  const getAriaLabel = (val) => {
    if (val === 'C') return 'Clear';
    if (val === 'BACK') return 'Backspace';
    if (val === 'USE') return 'Apply result';
    if (val === '=') return 'Equals';
    if (val === '*') return 'Multiply';
    if (val === '/') return 'Divide';
    if (val === '+') return 'Add';
    if (val === '-') return 'Subtract';
    if (val === '%') return 'Percent';
    return val;
  };

  return (
    <motion.div
      ref={calcRef}
      tabIndex={-1}
      role="dialog"
      aria-label="Inline calculator"
      className="inline-calculator"
      initial={{ opacity: 0, y: -8, scale: 0.96 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: -6, scale: 0.96 }}
      transition={{ type: 'spring', stiffness: 420, damping: 30 }}
    >
      <div className="calc-topbar">
        <span className="calc-title">Calculator</span>
        <button
          type="button"
          className="calc-close-btn"
          onClick={onClose}
          aria-label="Close calculator"
        >
          <X size={14} strokeWidth={2.4} aria-hidden="true" />
        </button>
      </div>

      <div className="calc-display">
        <div className="calc-expression">{expression || '0'}</div>
        <div className="calc-result">{display}</div>
      </div>

      <div className="calc-grid">
        {buttons.map((row, ri) =>
          row.map((btn) => (
            <button
              key={`${ri}-${btn}`}
              type="button"
              className={getButtonClass(btn)}
              aria-label={getAriaLabel(btn)}
              onClick={() => handleBtn(btn)}
            >
              {renderLabel(btn)}
            </button>
          ))
        )}
      </div>
    </motion.div>
  );
};

export default InlineCalculator;
