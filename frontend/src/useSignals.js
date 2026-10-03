import { useCallback, useEffect, useRef, useState } from 'react';
import { applySignalUpdate } from './signalState';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const WS_URL = import.meta.env.VITE_SIGNALS_WS_URL || 'ws://localhost:8080/ws/signals';
const MAX_RECONNECT_DELAY = 8000;

export function useSignals() {
  const socketRef = useRef(null); const timerRef = useRef(null); const abortRef = useRef(null);
  const attemptsRef = useRef(0); const generationRef = useRef(0); const bufferRef = useRef([]);
  const loadedRef = useRef(false); const feedRef = useRef('DEGRADED');
  const [signals, setSignals] = useState(new Map()); const [connection, setConnection] = useState('connecting');
  const [feed, setFeed] = useState('DEGRADED'); const [loading, setLoading] = useState(true); const [error, setError] = useState('');

  const connect = useCallback(() => {
    const generation = ++generationRef.current;
    clearTimeout(timerRef.current); abortRef.current?.abort(); socketRef.current?.close();
    bufferRef.current = []; loadedRef.current = false;
    setConnection('connecting'); setFeed('DEGRADED'); feedRef.current = 'DEGRADED'; setLoading(true); setError('');

    const scheduleRetry = () => {
      if (generation !== generationRef.current) return;
      setConnection('disconnected'); setFeed('DEGRADED'); feedRef.current = 'DEGRADED';
      const delay = Math.min(1000 * (2 ** attemptsRef.current++), MAX_RECONNECT_DELAY);
      timerRef.current = setTimeout(connect, delay);
    };

    const syncSnapshot = async () => {
      const controller = new AbortController(); abortRef.current?.abort(); abortRef.current = controller;
      const timeout = setTimeout(() => controller.abort(), 5000);
      try {
        const response = await fetch(`${API_URL}/api/signals`, { signal: controller.signal });
        if (!response.ok) throw new Error(`Snapshot request failed (${response.status})`);
        const snapshot = await response.json();
        if (generation !== generationRef.current) return;
        let next = new Map(snapshot.map((signal) => [signal.id, signal]));
        for (const update of bufferRef.current) next = applySignalUpdate(next, update);
        bufferRef.current = []; loadedRef.current = true; setSignals(next); setLoading(false); setError('');
      } catch (snapshotError) {
        if (generation !== generationRef.current || snapshotError.name === 'AbortError' && controller.signal.reason === 'superseded') return;
        setLoading(false); setError(snapshotError.name === 'AbortError' ? 'The signal snapshot timed out.' : snapshotError.message || 'Could not load signals.');
        socketRef.current?.close(); scheduleRetry();
      } finally { clearTimeout(timeout); }
    };

    const socket = new WebSocket(WS_URL); socketRef.current = socket;
    socket.onmessage = ({ data }) => {
      if (generation !== generationRef.current) return;
      try {
        const event = JSON.parse(data);
        if (event.type === 'SIGNAL_UPDATED') { if (loadedRef.current) setSignals((current) => applySignalUpdate(current, event)); else bufferRef.current.push(event); }
        else if (event.type === 'READY') { attemptsRef.current = 0; setConnection('connected'); syncSnapshot(); }
        else if (event.type === 'FEED_STATUS') {
          const recovered = feedRef.current === 'DEGRADED' && event.status === 'LIVE' && loadedRef.current;
          feedRef.current = event.status; setFeed(event.status); if (recovered) syncSnapshot();
        } else if (event.type === 'ERROR') setError(event.detail || 'The signal server reported an error.');
      } catch { setError('The signal server sent unreadable data.'); }
    };
    socket.onerror = () => { if (generation === generationRef.current) setError('The live signal connection is unavailable.'); };
    socket.onclose = () => { if (generation === generationRef.current) scheduleRetry(); };
  }, []);

  useEffect(() => {
    connect();
    return () => { generationRef.current += 1; clearTimeout(timerRef.current); abortRef.current?.abort('superseded'); socketRef.current?.close(); };
  }, [connect]);

  return { signals, connection, feed, loading, error, retry: connect };
}
