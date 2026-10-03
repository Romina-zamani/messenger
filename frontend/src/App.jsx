import { useEffect, useRef, useState } from 'react';
import { useChatSocket } from './useChatSocket';
import { LiveSignalsPage } from './LiveSignalsPage';

const MAX_NICKNAME = 30;
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

function JoinScreen({ onJoin }) {
  const [nickname, setNickname] = useState('');
  const cleaned = nickname.trim();
  const isValid = cleaned.length > 0 && cleaned.length <= MAX_NICKNAME;

  function submit(event) {
    event.preventDefault();
    if (isValid) onJoin(cleaned);
  }

  return (
    <main className="join-layout">
      <section className="join-copy" aria-labelledby="join-title">
        <p className="eyebrow">Lesson 6 · Browser client</p>
        <h1 id="join-title">See your Java server speak WebSocket.</h1>
        <p className="lead">Open this page in two browser tabs. Every message travels to Java, is saved, and comes back to every connected client.</p>
        <ol className="flow-list" aria-label="Message journey">
          <li><span>1</span><div><strong>Browser sends JSON</strong><small>Your nickname and message use one WebSocket connection.</small></div></li>
          <li><span>2</span><div><strong>Java validates and saves</strong><small>The backend assigns the ID and timestamp.</small></div></li>
          <li><span>3</span><div><strong>Server broadcasts</strong><small>Every connected browser receives the accepted message.</small></div></li>
        </ol>
      </section>

      <section className="join-panel" aria-label="Join the messenger">
        <div className="mark" aria-hidden="true">JM</div>
        <h2>Join the shared room</h2>
        <p>Choose a nickname for this browser session. No account or password is used in this lesson.</p>
        <form onSubmit={submit}>
          <label htmlFor="nickname">Nickname</label>
          <input
            id="nickname"
            name="nickname"
            value={nickname}
            onChange={(event) => setNickname(event.target.value)}
            maxLength={MAX_NICKNAME}
            autoComplete="nickname"
            autoFocus
            placeholder="For example, Mahdi"
          />
          <div className="field-help">
            <span>Shown beside your messages</span>
            <span>{nickname.length}/{MAX_NICKNAME}</span>
          </div>
          <button className="primary-button" type="submit" disabled={!isValid}>Join shared room</button>
        </form>
      </section>
    </main>
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

function ChatRoom({ nickname, onLeave }) {
  const { connection, messages, error, send, reconnect } = useChatSocket(nickname);
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
            <button className="text-button" type="button" onClick={onLeave}>Leave</button>
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
            const mine = message.sender === nickname;
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
  const [nickname, setNickname] = useState('');
  const [page, setPage] = useState('chat');
  return <>
    <nav className="site-nav" aria-label="Primary navigation">
      <strong>Java Messenger</strong>
      <div><button type="button" aria-current={page === 'chat' ? 'page' : undefined} onClick={() => setPage('chat')}>Chat</button><button type="button" aria-current={page === 'signals' ? 'page' : undefined} onClick={() => setPage('signals')}>Live Signals</button></div>
    </nav>
    {page === 'signals' ? <LiveSignalsPage /> : nickname ? <ChatRoom nickname={nickname} onLeave={() => setNickname('')} /> : <JoinScreen onJoin={setNickname} />}
  </>;
}
