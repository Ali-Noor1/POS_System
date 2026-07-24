export default function StatusPill({ status }) {
  const normalizedStatus = status || 'UNKNOWN'
  return (
    <span className={`status-pill ${normalizedStatus.toLowerCase()}`}>
      {normalizedStatus}
    </span>
  )
}

