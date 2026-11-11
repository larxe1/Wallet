import React, { useEffect, useState } from 'react';
import { useMotionValue, useSpring, useReducedMotion } from 'motion/react';
import { formatCurrency } from '../utils/format';

export default function AnimatedNumber({
  value = 0,
  masked = false,
  compact = false,
  prefix = '',
  suffix = '',
  formatFn,
  className = '',
}) {
  const numericValue = Number.isFinite(Number(value)) ? Number(value) : 0;
  const shouldReduceMotion = useReducedMotion();

  const motionValue = useMotionValue(numericValue);
  const springValue = useSpring(motionValue, {
    stiffness: 160,
    damping: 26,
    mass: 0.75,
  });

  const [displayValue, setDisplayValue] = useState(numericValue);

  useEffect(() => {
    if (shouldReduceMotion || masked) {
      motionValue.jump(numericValue);
      setDisplayValue(numericValue);
      return;
    }
    motionValue.set(numericValue);
  }, [numericValue, masked, shouldReduceMotion, motionValue]);

  useEffect(() => {
    const unsubscribe = springValue.on('change', (latest) => {
      const cleaned = Math.abs(latest) < 0.005 ? 0 : latest;
      setDisplayValue(cleaned);
    });
    return unsubscribe;
  }, [springValue]);

  if (masked) {
    return (
      <span
        className={`animated-number is-masked ${className}`.trim()}
        style={{ fontVariantNumeric: 'tabular-nums' }}
        aria-label="Hidden amount"
      >
        ••••••
      </span>
    );
  }

  const valToFormat = shouldReduceMotion ? numericValue : displayValue;
  const formatted = formatFn
    ? formatFn(valToFormat)
    : formatCurrency(valToFormat, { compact, masked: false });

  return (
    <span
      className={`animated-number ${className}`.trim()}
      style={{ fontVariantNumeric: 'tabular-nums' }}
    >
      {prefix}
      {formatted}
      {suffix}
    </span>
  );
}
