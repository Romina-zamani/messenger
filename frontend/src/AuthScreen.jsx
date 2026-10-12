import { useState } from 'react';
import { authPost, currentAccount } from './authApi';

export function AuthScreen({ onLogin }) {
  const [registering, setRegistering] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    const values = { username: username.trim().toLowerCase(), password };
    try {
      if (registering) {
        await authPost('register', values);
        setRegistering(false);
      }
      await authPost('login', values);
      const account = await currentAccount();
      if (!account) throw new Error('Please sign in again.');
      onLogin(account);
    } catch (failure) {
      setError(failure instanceof TypeError ? 'Could not reach the server. Please try again.' : failure.message);
    } finally {
      setPassword('');
      setBusy(false);
    }
  }

  return (
    <main className="join-layout">
      <section className="join-copy" aria-labelledby="join-title">
        <p className="eyebrow">Java Messenger</p>
        <h1 id="join-title">One account. A shared conversation.</h1>
        <p className="lead">Sign in to chat. Your messages are saved, so you can return to the conversation later.</p>
        <p>This is a shared room. Everyone who signs in can read its recent messages.</p>
      </section>
      <section className="join-panel" aria-labelledby="auth-title">
        <div className="mark" aria-hidden="true">JM</div>
        <h2 id="auth-title">{registering ? 'Create your account' : 'Sign in'}</h2>
        <form onSubmit={submit} aria-busy={busy}>
          <label htmlFor="username">Username</label>
          <input id="username" name="username" value={username} onChange={event => setUsername(event.target.value)}
            autoComplete="username" autoCapitalize="none" spellCheck={false} required maxLength={30}
            disabled={busy} aria-describedby="username-help" />
          <p className="field-help" id="username-help">3–30 letters, numbers, or underscores. Uppercase becomes lowercase.</p>
          <label htmlFor="password">Password</label>
          <input id="password" name="password" type="password" value={password} onChange={event => setPassword(event.target.value)}
            autoComplete={registering ? 'new-password' : 'current-password'} required disabled={busy}
            aria-describedby={registering ? 'password-help' : undefined} />
          {registering && <p className="field-help" id="password-help">Use at least 12 characters. Maximum 72 bytes; some characters take more than one byte. Account recovery is not available yet.</p>}
          {error && <p className="error-notice" role="alert">{error}</p>}
          <button className="primary-button" disabled={busy || !username.trim() || !password} type="submit">
            {busy ? 'Please wait…' : registering ? 'Register' : 'Sign in'}
          </button>
          <button className="text-button" disabled={busy} type="button" onClick={() => {
            setRegistering(!registering); setError(''); setPassword('');
          }}>{registering ? 'Already registered? Sign in' : 'Create an account'}</button>
        </form>
      </section>
    </main>
  );
}
