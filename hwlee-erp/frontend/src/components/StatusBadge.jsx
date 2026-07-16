import { statusMeta } from '../domain/status'

const TONE_STYLE = {
  neutral: { bg: 'rgba(100,116,139,0.18)', color: '#cbd5e1', border: '#64748b' },
  active: { bg: 'rgba(34,197,94,0.15)', color: '#86efac', border: '#22c55e' },
  warn: { bg: 'rgba(245,158,11,0.15)', color: '#fcd34d', border: '#f59e0b' },
  done: { bg: 'rgba(56,189,248,0.15)', color: '#7dd3fc', border: '#38bdf8' },
  muted: { bg: 'rgba(71,85,105,0.2)', color: '#94a3b8', border: '#475569' },
  danger: { bg: 'rgba(239,68,68,0.15)', color: '#fca5a5', border: '#ef4444' },
}

// 상태 코드 → 한글 라벨 배지.
// map = QUOTATION_STATUS | SALES_ORDER_STATUS
export default function StatusBadge({ map, status }) {
  const meta = statusMeta(map, status)
  const s = TONE_STYLE[meta.tone] || TONE_STYLE.neutral
  return (
    <span
      className="badge"
      style={{ background: s.bg, color: s.color, borderColor: s.border }}
    >
      {meta.label}
    </span>
  )
}
