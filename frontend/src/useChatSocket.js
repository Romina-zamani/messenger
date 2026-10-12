import { useCallback, useEffect, useRef, useState } from 'react';
import { currentAccount } from './authApi';

const DEFAULT_URL = `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws/chat`;
const MAX_RECONNECT_DELAY = 8000;

export function useChatSocket(nickname, onSessionLost) {
  const socketRef = useRef(null);
  const reconnectTimerRef = useRef(null);
  const reconnectAttemptRef = useRef(0);
  const manuallyClosedRef = useRef(false);
  const [connection, setConnection] = useState('idle');
  const [messages, setMessages] = useState([]);
  const [error, setError] = useState('');

  const url = import.meta.env.VITE_WS_URL || DEFAULT_URL;

  const connect = useCallback(() => {
    if (!nickname) return;

    clearTimeout(reconnectTimerRef.current);
    manuallyClosedRef.current = false;
    setConnection('connecting');
    setError('');

    const socket = new WebSocket(url);
    socketRef.current = socket;

    socket.onmessage = (message) => {
      if (socketRef.current !== socket || manuallyClosedRef.current) return;
      try {
        const event = JSON.parse(message.data);
        if (event.type === 'MESSAGE') {
          setMessages((current) => {
            if (event.id != null && current.some((item) => item.id === event.id)) return current;
            return [...current, event];
          });
        } else if (event.type === 'READY') {
          reconnectAttemptRef.current = 0;
          setConnection('connected');
        } else if (event.type === 'ERROR') {
          setError(event.detail || 'The server rejected the message.');
        }
      } catch {
        setError('The server sent data this browser could not read.');
      }
    };

    socket.onerror = () => {
      setError('Could not reach the Java server. Check that it is running.');
    };

    socket.onclose = async (event) => {
      if (socketRef.current !== socket || manuallyClosedRef.current) return;
      if (event.code === 1008) { onSessionLost(); return; }
      setConnection('disconnected');
      try {
        const account = await currentAccount();
        if (socketRef.current !== socket || manuallyClosedRef.current) return;
        if (!account || account.username !== nickname) { onSessionLost(); return; }
      } catch { /* Keep retrying when the server is temporarily unreachable. */ }
      if (socketRef.current !== socket || manuallyClosedRef.current) return;
      const delay = Math.min(1000 * (2 ** reconnectAttemptRef.current), MAX_RECONNECT_DELAY);
      reconnectAttemptRef.current += 1;
      reconnectTimerRef.current = setTimeout(connect, delay);
    };
  }, [nickname, url, onSessionLost]);

  useEffect(() => {
    connect();
    return () => {
      manuallyClosedRef.current = true;
      clearTimeout(reconnectTimerRef.current);
      socketRef.current?.close();
    };
  }, [connect]);

  const send = useCallback((content) => {
    const socket = socketRef.current;
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      setError('Wait until the connection is ready before sending.');
      return false;
    }
    socket.send(JSON.stringify({ content }));
    setError('');
    return true;
  }, []);

  return { connection, messages, error, send, reconnect: connect };
}
