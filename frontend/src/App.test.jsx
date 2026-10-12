import { StrictMode } from 'react';
import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';

async function joinAs(nickname = 'Mahdi') {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText(/username/i), nickname);
  await user.type(screen.getByLabelText(/^password$/i), 'LearningJava123!');
  await user.click(screen.getByRole('button', { name: /^sign in$/i }));
  await screen.findByRole('button', { name: /log out/i });
  return { user, socket: WebSocket.instances[0] };
}

test('joining creates a WebSocket and READY marks the app connected', async () => {
  render(<App />);

  const { socket } = await joinAs();
  expect(socket.url).toBe(`${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws/chat`);
  expect(screen.getByText(/connecting/i)).toBeInTheDocument();

  act(() => {
    socket.open();
    socket.receive({ type: 'READY', detail: 'Connected' });
  });

  expect(screen.getByText('Connected')).toBeInTheDocument();
});

test('a sent message appears only after the server broadcasts MESSAGE', async () => {
  render(<App />);
  const { user, socket } = await joinAs('Mahdi');

  act(() => {
    socket.open();
    socket.receive({ type: 'READY', detail: 'Connected' });
  });

  await user.type(screen.getByLabelText(/^message$/i), 'Hello server');
  await user.click(screen.getByRole('button', { name: /^send$/i }));

  expect(JSON.parse(socket.sent[0])).toEqual({
    content: 'Hello server'
  });
  expect(screen.queryByText('Hello server')).not.toBeInTheDocument();

  act(() => socket.receive({
    type: 'MESSAGE',
    id: 7,
    sender: 'Mahdi',
    content: 'Hello server',
    sentAt: '2026-09-28T10:15:00Z'
  }));

  expect(screen.getByText('Hello server')).toBeInTheDocument();
  expect(within(screen.getByLabelText('Chat messages')).getByText('Mahdi')).toBeInTheDocument();
});

test('server errors are announced to the user', async () => {
  render(<App />);
  const { socket } = await joinAs();

  act(() => {
    socket.open();
    socket.receive({ type: 'ERROR', detail: 'Message is too long.' });
  });

  expect(screen.getByRole('alert')).toHaveTextContent('Message is too long.');
});

test('live signals navigation loads a snapshot and applies one delta', async () => {
  const user = userEvent.setup();
  const authFetch = global.fetch;
  const signalsResponse = {
    ok: true,
    json: async () => [
      { id: 1, name: 'CPU 001', category: 'CPU', unit: '%', value: 42, status: 'HEALTHY', updatedAt: '2026-10-03T12:00:00Z', version: 0 },
      { id: 2, name: 'Memory 002', category: 'MEMORY', unit: '%', value: 64, status: 'HEALTHY', updatedAt: '2026-10-03T12:00:00Z', version: 0 }
    ]
  };
  global.fetch = vi.fn((url, options) => String(url).includes('/api/signals') ? Promise.resolve(signalsResponse) : authFetch(url, options));

  render(<App />);
  await user.click(screen.getByRole('button', { name: /live signals/i }));

  const socket = WebSocket.instances.at(-1);
  act(() => {
    socket.open();
    socket.receive({ type: 'READY' });
    socket.receive({ type: 'SIGNAL_UPDATED', id: 1, value: 91, status: 'CRITICAL', updatedAt: '2026-10-03T12:00:01Z', version: 1 });
  });

  expect(await screen.findByText('CPU 001')).toBeInTheDocument();
  expect(screen.getByText('91.0')).toBeInTheDocument();
  expect(screen.getByText('Memory 002')).toBeInTheDocument();
  expect(global.fetch.mock.calls.filter(([url]) => String(url).includes('/api/signals'))).toHaveLength(1);
});

test('strict mode retires the obsolete signal socket', async () => {
  const user = userEvent.setup();
  const authFetch = global.fetch;
  global.fetch = vi.fn((url, options) => String(url).includes('/api/signals') ? Promise.resolve({ ok: true, json: async () => [] }) : authFetch(url, options));
  render(<StrictMode><App /></StrictMode>);
  await user.click(screen.getByRole('button', { name: /live signals/i }));

  const active = WebSocket.instances.filter((socket) => socket.readyState !== 3);
  expect(active).toHaveLength(1);
  act(() => active[0].receive({ type: 'READY' }));
  expect(await screen.findByText(/no signals match/i)).toBeInTheDocument();
});

test('registration creates an account, signs in, and logout returns to the form', async () => {
  render(<App />);
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: /create an account/i }));
  await user.type(screen.getByLabelText(/username/i), 'Mahdi');
  await user.type(screen.getByLabelText(/^password$/i), 'LearningJava123!');
  await user.click(screen.getByRole('button', { name: /^register$/i }));
  await screen.findByRole('button', { name: /log out/i });
  expect(global.fetch.mock.calls.some(([url]) => url === '/api/auth/register')).toBe(true);
  await user.click(screen.getByRole('button', { name: /log out/i }));
  expect(await screen.findByRole('button', { name: /^sign in$/i })).toBeInTheDocument();
  expect(WebSocket.instances[0].readyState).toBe(3);
});

test('wrong credentials show an error without opening a chat socket', async () => {
  const originalFetch = global.fetch;
  global.fetch = vi.fn((url, options) => url === '/api/auth/login'
    ? Promise.resolve({ ok: false, status: 401, json: async () => ({}) }) : originalFetch(url, options));
  render(<App />);
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText(/username/i), 'Mahdi');
  await user.type(screen.getByLabelText(/^password$/i), 'wrong-password');
  await user.click(screen.getByRole('button', { name: /^sign in$/i }));
  expect(await screen.findByRole('alert')).toHaveTextContent(/invalid username or password/i);
  expect(WebSocket.instances).toHaveLength(0);
});

test('restores the account on reload and returns to login when the session ends', async () => {
  const originalFetch = global.fetch;
  global.fetch = vi.fn((url, options) => url === '/api/auth/me'
    ? Promise.resolve({ ok: true, status: 200, json: async () => ({ id: 5, username: 'returning_user' }) }) : originalFetch(url, options));
  render(<App />);
  await screen.findByRole('button', { name: /log out/i });
  const socket = WebSocket.instances[0];
  await act(async () => { await socket.onclose({ code: 1008 }); });
  expect(await screen.findByRole('button', { name: /^sign in$/i })).toBeInTheDocument();
  expect(screen.getByRole('alert')).toHaveTextContent(/session ended/i);
});

test('registration keeps its form and explains a taken username', async () => {
  const originalFetch = global.fetch;
  global.fetch = vi.fn((url, options) => url === '/api/auth/register'
    ? Promise.resolve({ ok: false, status: 409, json: async () => ({ detail: 'That username is already taken.' }) }) : originalFetch(url, options));
  render(<App />);
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: /create an account/i }));
  await user.type(screen.getByLabelText(/username/i), 'Mahdi');
  await user.type(screen.getByLabelText(/^password$/i), 'LearningJava123!');
  await user.click(screen.getByRole('button', { name: /^register$/i }));
  expect(await screen.findByRole('alert')).toHaveTextContent(/already taken/i);
  expect(WebSocket.instances).toHaveLength(0);
});
