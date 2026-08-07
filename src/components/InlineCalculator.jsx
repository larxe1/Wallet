import React, { useState, useEffect, useRef } from 'react';
import './InlineCalculator.css';

const InlineCalculator = ({ onResult, onClose }) => {
  const [expression, setExpression] = useState('');
  const [display, setDisplay] = useState('0');
  const [evaluated, setEvaluated] = useState(false);
  const calcRef = useRef(null);

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

  // Keyboard support
  useEffect(() => {
    const handle = (e) => {
      const key = e.key;
      if ('0123456789'.includes(key)) press(key);
      else if ('+-*/'.includes(key)) press(key);
      else if (key === '.' || key === ',') press('.');
      else if (key === 'Enter' || key === '=') handleEquals();
      else if (key === 'Backspace') handleBack();
      else if (key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handle);
    return () => document.removeEventListener('keydown', handle);
  }, [expression, display, evaluated]);

  const press = (val) => {
    const ops = ['+', '-', '*', '/'];
    if (evaluated && !ops.includes(val)) {
      setExpression(val);
      setDisplay(val);
      setEvaluated(false);
      return;
    }
    setEvaluated(false);
    const newExpr = expression + val;
    setExpression(newExpr);
    // update display: show just the current operand
    const parts = newExpr.split(/[\+\-\*\/]/);
    setDisplay(parts[parts.length - 1] || '0');
  };

  const handleEquals = () => {
    if (!expression) return;
    try {
      // Safe eval using Function
      // eslint-disable-next-line no-new-func
      const result = Function(`'use strict'; return (${expression})`)();
      const rounded = Math.round(result * 100) / 100;
      setDisplay(String(rounded));
      setExpression(String(rounded));
      setEvaluated(true);
    } catch {
      setDisplay('Error');
      setExpression('');
    }
  };

  const handleUse = () => {
    handleEquals();
    try {
      // eslint-disable-next-line no-new-func
      const result = Function(`'use strict'; return (${expression})`)();
      const rounded = Math.round(result * 100) / 100;
      if (!isNaN(rounded) && rounded > 0) {
        onResult(String(rounded));
        onClose();
      }
    } catch {
      // ignore
    }
  };

  const handleBack = () => {
    const newExpr = expression.slice(0, -1);
    setExpression(newExpr);
    const parts = newExpr.split(/[\+\-\*\/]/);
    setDisplay(parts[parts.length - 1] || '0');
    setEvaluated(false);
  };

  const handleClear = () => {
    setExpression('');
    setDisplay('0');
    setEvaluated(false);
  };

  const buttons = [
    ['C', '⌫', '%', '/'],
    ['7', '8', '9', '*'],
    ['4', '5', '6', '-'],
    ['1', '2', '3', '+'],
    ['0', '.', '=', '✓'],
  ];

  const handleBtn = (val) => {
    if (val === 'C') handleClear();
    else if (val === '⌫') handleBack();
    else if (val === '=') handleEquals();
    else if (val === '✓') handleUse();
    else if (val === '%') {
      try {
        // eslint-disable-next-line no-new-func
        const result = Function(`'use strict'; return (${expression} / 100)`)();
        const rounded = Math.round(result * 100) / 100;
        setExpression(String(rounded));
        setDisplay(String(rounded));
        setEvaluated(true);
      } catch { /* ignore */ }
    }
    else press(val);
  };

  const getButtonClass = (val) => {
    if (val === '✓') return 'calc-btn use-btn';
    if (val === '=') return 'calc-btn equals-btn';
    if (['+', '-', '*', '/'].includes(val)) return 'calc-btn op-btn';
    if (val === 'C' || val === '⌫' || val === '%') return 'calc-btn func-btn';
    return 'calc-btn';
  };

  return (
    <div className="inline-calculator" ref={calcRef}>
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
              onClick={() => handleBtn(btn)}
            >
              {btn}
            </button>
          ))
        )}
      </div>
    </div>
  );
};

export default InlineCalculator;
