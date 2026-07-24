export default function TransactionTypeLabel({ type }) {
  const label = type === 'ADJUSTMENT_OUT' ? 'Stock Out' : type === 'ADJUSTMENT_IN' ? 'Stock In' : type
  const tone = type === 'ADJUSTMENT_OUT' ? 'out' : 'in'
  return <span className={`transaction-pill ${tone}`}>{label}</span>
}

