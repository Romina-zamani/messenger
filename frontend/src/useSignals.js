import { useCallback, useEffect, useRef, useState } from 'react';
import { applySignalUpdate } from './signalState';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const WS_URL = import.meta.env.VITE_SIGNALS_WS_URL || 'ws://localhost:8080/ws/signals';
const MAX_RECONNECT_DELAY = 8000;

export function useSignals() {
  const socketRef = useRef(null);
  const timerRef = useRef(null);
  const attemptsRef = useRef(0);
  const stoppedRef = useRef(false);
  const [signals, setSignals] = useState(new Map());
  const [connection, setConnection] = useState('connecting');
  const [feed, setFeed] = useState('DEGRADED');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const connect = useCallback(async () => {
    clearTimeout(timerRef.current);
    setConnection('connecting'); setLoading(true); setError('');
    try {
      const response = await fetch(`${API_URL}/api/signals`);
      if (!response.ok) throw new Error(`Snapshot request failed (${response.status})`);
      const snapshot = await response.json();
      if (stoppedRef.current) return;
      setSignals(new Map(snapshot.map((signal) => [signal.id, signal])));
      setLoading(false);
    } catch (snapshotError) {
      if (!stoppedRef.current) { setLoading(false); setError(snapshotError.message || 'Could not load signals.'); }
      return;
    }

    const socket = new WebSocket(WS_URL);
    socketRef.current = socket;
    socket.onmessage = ({ data }) => {
      try {
        const event = JSON.parse(data);
        if (event.type === 'SIGNAL_UPDATED') setSignals((current) => applySignalUpdate(current, event));
        else if (event.type === 'READY') { attemptsRef.current = 0; setConnection('connected'); }
        else if (event.type === 'FEED_STATUS') setFeed(event.status);
        else if (event.type === 'ERROR') setError(event.detail || 'The signal server reported an error.');
      } catch { setError('The signal server sent unreadable data.'); }
    };
    socket.onerror = () => setError('The live signal connection is unavailable.');
    socket.onclose = () => {
      if (stoppedRef.current || socketRef.current !== socket) return;
      setConnection('disconnected'); setFeed('DEGRADED');
      const delay = Math.min(1000 * (2 ** attemptsRef.current++), MAX_RECONNECT_DELAY);
      timerRef.current = setTimeout(connect, delay);
    };
  }, []);

  useEffect(() => {
    stoppedRef.current = false; connect();
    return () => { stoppedRef.current = true; clearTimeout(timerRef.current); socketRef.current?.close(); };
  }, [connect]);

  return { signals, connection, feed, loading, error, retry: connect };
}
