import { formatMoney } from '../../lib/format'

export interface MoneyProps {
  amount: number
  currency?: string
}

// Montant formaté, devise ISO après le nombre (§7.3).
export function Money(p: MoneyProps) {
  return (
    <span className="gm-num" style={{ display: 'inline-block', textAlign: 'right' }}>
      {formatMoney(p.amount)} {p.currency || 'XAF'}
    </span>
  )
}
