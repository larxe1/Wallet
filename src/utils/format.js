import { format } from 'date-fns';

export function formatCurrency(amount, { compact = false, masked = false } = {}) {
  if (masked) {
    return '••••••';
  }

  return new Intl.NumberFormat('en-PH', {
    style: 'currency',
    currency: 'PHP',
    ...(compact ? { notation: 'compact', maximumFractionDigits: 1 } : {}),
  }).format(Number(amount) || 0);
}

export function parseLocalDate(dateStr) {
  if (!dateStr) {
    return new Date();
  }

  if (dateStr instanceof Date) {
    return Number.isNaN(dateStr.getTime()) ? new Date() : dateStr;
  }

  const trimmed = String(dateStr).trim();
  const ymdExact = /^(\d{4})-(\d{2})-(\d{2})$/.exec(trimmed);
  if (ymdExact) {
    const year = Number(ymdExact[1]);
    const month = Number(ymdExact[2]);
    const day = Number(ymdExact[3]);
    return new Date(year, month - 1, day);
  }

  const parsed = new Date(trimmed);
  if (!Number.isNaN(parsed.getTime())) {
    return parsed;
  }

  const ymdPrefix = /^(\d{4})-(\d{2})-(\d{2})/.exec(trimmed);
  if (ymdPrefix) {
    const year = Number(ymdPrefix[1]);
    const month = Number(ymdPrefix[2]);
    const day = Number(ymdPrefix[3]);
    return new Date(year, month - 1, day);
  }

  return new Date();
}

export function formatDate(dateInput, pattern = 'MMM dd, yyyy') {
  if (!dateInput) {
    return '';
  }

  const dateObj = parseLocalDate(dateInput);
  return format(dateObj, pattern);
}
