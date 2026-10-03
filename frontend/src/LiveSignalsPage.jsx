import { memo, useMemo, useState } from 'react';
import { useSignals } from './useSignals';

const SignalRow = memo(function SignalRow({ signal }) {
  return <tr>
    <th scope="row">{signal.name}</th><td>{signal.category}</td>
    <td className="signal-value">{Number(signal.value).toFixed(1)} <small>{signal.unit}</small></td>
    <td><span className={`status-badge status-badge--${signal.status.toLowerCase()}`}>{signal.status}</span></td>
    <td><time dateTime={signal.updatedAt}>{formatTime(signal.updatedAt)}</time></td><td>v{signal.version}</td>
  </tr>;
});

export function LiveSignalsPage() {
  const { signals, connection, feed, loading, error, retry } = useSignals();
  const [query, setQuery] = useState(''); const [status, setStatus] = useState('ALL'); const [category, setCategory] = useState('ALL');
  const list = useMemo(() => [...signals.values()], [signals]);
  const categories = useMemo(() => [...new Set(list.map((signal) => signal.category))].sort(), [list]);
  const filtered = useMemo(() => list.filter((signal) => (status === 'ALL' || signal.status === status) && (category === 'ALL' || signal.category === category) && signal.name.toLowerCase().includes(query.toLowerCase())), [list, query, status, category]);
  const counts = useMemo(() => list.reduce((total, signal) => ({ ...total, [signal.status]: total[signal.status] + 1 }), { HEALTHY: 0, WARNING: 0, CRITICAL: 0 }), [list]);

  return <main className="signals-page">
    <header className="signals-hero">
      <div><p className="eyebrow">PostgreSQL → Redis → WebSocket</p><h1>Live server signals</h1><p>One thousand persisted measurements. Each event updates one row.</p></div>
      <div className="live-state" role="status" aria-live="polite"><span className={`connection connection--${connection}`}><span className="connection__dot" />{connection === 'connected' ? 'Socket connected' : connection === 'connecting' ? 'Connecting…' : 'Reconnecting…'}</span><span className={`feed-state feed-state--${feed.toLowerCase()}`}>{feed === 'LIVE' ? 'Redis feed live' : 'Feed degraded'}</span></div>
    </header>

    {error && <div className="error-notice" role="alert"><span>{error}</span><button type="button" onClick={retry}>Retry</button></div>}
    <section className="signal-summary" aria-label="Signal summary">
      <div><span>Total</span><strong>{list.length}</strong></div><div><span>Healthy</span><strong>{counts.HEALTHY}</strong></div><div><span>Warning</span><strong>{counts.WARNING}</strong></div><div><span>Critical</span><strong>{counts.CRITICAL}</strong></div>
    </section>
    <section className="signal-workspace" aria-labelledby="signals-table-title">
      <div className="signal-toolbar"><div><h2 id="signals-table-title">Telemetry registry</h2><p>{filtered.length} signals shown</p></div><label>Search<input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Signal name" /></label><label>Status<select value={status} onChange={(event) => setStatus(event.target.value)}><option>ALL</option><option>HEALTHY</option><option>WARNING</option><option>CRITICAL</option></select></label><label>Category<select value={category} onChange={(event) => setCategory(event.target.value)}><option>ALL</option>{categories.map((item) => <option key={item}>{item}</option>)}</select></label></div>
      {loading ? <div className="signal-message" role="status">Loading the PostgreSQL snapshot…</div> : filtered.length === 0 ? <div className="signal-message">No signals match these filters.</div> : <div className="signal-table-wrap"><table className="signal-table"><thead><tr><th>Signal</th><th>Category</th><th>Value</th><th>Status</th><th>Last update</th><th>Version</th></tr></thead><tbody>{filtered.map((signal) => <SignalRow key={signal.id} signal={signal} />)}</tbody></table></div>}
    </section>
  </main>;
}

function formatTime(value) { return value ? new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit' }).format(new Date(value)) : 'Never'; }
