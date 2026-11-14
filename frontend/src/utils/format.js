export const RISKS = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

export const RISK_RANK = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };

export const STATUSES = [
  ['NEEDS_REVIEW', 'Needs review'],
  ['CONFIRMED', 'Confirmed'],
  ['NOT_APPLICABLE', 'Not applicable'],
  ['REMEDIATED', 'Remediated'],
];

export function statusLabel(status) {
  return STATUSES.find(([value]) => value === status)?.[1] || 'Needs review';
}

export function modeLabel(mode) {
  return mode === 'AI' ? 'AI analysis' : 'Rule-based fallback';
}

export function formatWhen(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString(undefined, { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit' });
}

export function formatBytes(value) {
  if (value == null) return '—';
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(0)} KB`;
  return `${(value / (1024 * 1024)).toFixed(1)} MB`;
}

export function isOpenGap(item) {
  return item.status === 'NEEDS_REVIEW' || item.status === 'CONFIRMED';
}
