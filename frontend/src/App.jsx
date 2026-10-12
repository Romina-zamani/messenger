import { useCallback, useEffect, useRef, useState } from 'react';
import { useChatSocket } from './useChatSocket';
import { LiveSignalsPage } from './LiveSignalsPage';
import { AuthScreen } from './AuthScreen';
import { authPost, currentAccount } from './authApi';

const MAX_MESSAGE = 500;

function ConnectionStatus({ state }) {
  const labels = {
    idle: 'Not connected',
    connecting: 'Connecting…',
    connected: 'Connected',
    disconnected: 'Reconnecting…'
  };

  return (
    <div className={`connection connection--${state}`} role="status" aria-live="polite">
      <span className="connection__dot" aria-hidden="true" />
      {labels[state]}
    </div>
  );
}

function LearningRail({ nickname }) {
  return (
    <aside className="learning-rail" aria-labelledby="learning-title">
      <div>
        <p className="eyebrow">Java Messenger</p>
        <h1 id="learning-title">Shared room</h1>
        <p className="rail-intro">You joined as <strong>{nickname}</strong>. The browser keeps one WebSocket open instead of making a new HTTP request for every message.</p>
      </div>
      <div className="data-flow" aria-label="Live message data flow">
        <div><span>01</span><p><strong>Browser</strong> creates JSON</p></div>
        <i aria-hidden="true">↓</i>
        <div><span>02</span><p><strong>Java</strong> validates it</p></div>
        <i aria-hidden="true">↓</i>
        <div><span>03</span><p><strong>PostgreSQL</strong> stores it</p></div>
        <i aria-hidden="true">↓</i>
        <div><span>04</span><p><strong>Broadcast</strong> reaches clients</p></div>
      </div>
      <p className="rail-note"><span aria-hidden="true">●</span> Messages shown here came from the server.</p>
    </aside>
  );
}

function ChatRoom({ account, onLeave, onSessionLost, loggingOut }) {
  const nickname = account.username;
  const { connection, messages, error, send, reconnect } = useChatSocket(nickname, onSessionLost);
  const [content, setContent] = useState('');
  const endRef = useRef(null);

  useEffect(() => {
    if (typeof endRef.current?.scrollIntoView === 'function') {
      endRef.current.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }, [messages]);

  function submit(event) {
    event.preventDefault();
    const cleaned = content.trim();
    if (!cleaned || cleaned.length > MAX_MESSAGE) return;
    if (send(cleaned)) setContent('');
  }

  return (
    <main className="app-shell">
      <LearningRail nickname={nickname} />
      <section className="chat" aria-labelledby="chat-title">
        <header className="chat-header">
          <div>
            <p className="eyebrow">WebSocket endpoint</p>
            <h2 id="chat-title">General chat</h2>
          </div>
          <div className="header-actions">
            <ConnectionStatus state={connection} />
            <button className="text-button" type="button" disabled={loggingOut} onClick={onLeave}>{loggingOut ? 'Logging out…' : 'Log out'}</button>
          </div>
        </header>

        {error && (
          <div className="error-notice" role="alert">
            <span>{error}</span>
            {connection !== 'connected' && <button type="button" onClick={reconnect}>Reconnect now</button>}
          </div>
        )}

        <div className="messages" aria-live="polite" aria-label="Chat messages">
          {messages.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon" aria-hidden="true">↔</div>
              <h3>The connection is ready for a conversation.</h3>
              <p>Send the first message, or open another browser tab to see the Java server broadcast between clients.</p>
            </div>
          ) : messages.map((message) => {
            const mine = message.senderId != null && message.senderId === account.id;
            return (
              <article className={`message ${mine ? 'message--mine' : ''}`} key={message.id ?? `${message.sentAt}-${message.sender}`}>
                <div className="message__meta">
                  <strong>{message.sender}</strong>
                  <time dateTime={message.sentAt}>{formatTime(message.sentAt)}</time>
                </div>
                <p>{message.content}</p>
              </article>
            );
          })}
          <div ref={endRef} />
        </div>

        <form className="composer" onSubmit={submit}>
          <label className="sr-only" htmlFor="message">Message</label>
          <textarea
            id="message"
            name="message"
            value={content}
            onChange={(event) => setContent(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                event.currentTarget.form?.requestSubmit();
              }
            }}
            maxLength={MAX_MESSAGE}
            rows="1"
            placeholder={connection === 'connected' ? 'Write a message…' : 'Waiting for the server…'}
            disabled={connection !== 'connected'}
          />
          <div className="composer__footer">
            <span><kbd>Enter</kbd> send · <kbd>Shift</kbd> + <kbd>Enter</kbd> new line</span>
            <span className="character-count">{content.length}/{MAX_MESSAGE}</span>
            <button className="send-button" type="submit" disabled={connection !== 'connected' || !content.trim()}>Send</button>
          </div>
        </form>
      </section>
    </main>
  );
}

function formatTime(value) {
  if (!value) return '';
  return new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' }).format(new Date(value));
}

export default function App() {
  const [account, setAccount] = useState(undefined);
  const [authError, setAuthError] = useState('');
  const [loggingOut, setLoggingOut] = useState(false);
  const [page, setPage] = useState('chat');
  const sessionLost = useCallback(() => {
    setAccount(null);
    setAuthError('Your session ended. Please sign in again.');
  }, []);
  useEffect(() => {
    let active = true;
    currentAccount().then(value => { if (active) setAccount(value); })
      .catch(() => { if (active) { setAccount(null); setAuthError('Could not check your session. Please sign in again.'); } });
    return () => { active = false; };
  }, []);
  async function logout() {
    setLoggingOut(true);
    setAuthError('');
    try { await authPost('logout'); setAccount(null); }
    catch (error) { setAuthError(error.message); }
    finally { setLoggingOut(false); }
  }
  return <>
    <nav className="site-nav" aria-label="Primary navigation">
      <strong>Java Messenger</strong>
      <div><button type="button" aria-current={page === 'chat' ? 'page' : undefined} onClick={() => setPage('chat')}>Chat</button><button type="button" aria-current={page === 'signals' ? 'page' : undefined} onClick={() => setPage('signals')}>Live Signals</button></div>
    </nav>
    {authError && <p role="alert" className="error-notice">{authError}</p>}
    {page === 'signals' ? <LiveSignalsPage /> : account === undefined ? <p role="status">Checking your session…</p>
      : account ? <ChatRoom key={account.id} account={account} onLeave={logout} onSessionLost={sessionLost} loggingOut={loggingOut} />
        : <AuthScreen onLogin={value => { setAccount(value); setAuthError(''); }} />}
  </>;
}
