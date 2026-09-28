import { act, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';

async function joinAs(nickname = 'Mahdi') {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText(/nickname/i), nickname);
  await user.click(screen.getByRole('button', { name: /join shared room/i }));
  return { user, socket: WebSocket.instances[0] };
}

test('joining creates a WebSocket and READY marks the app connected', async () => {
  render(<App />);

  const { socket } = await joinAs();
  expect(socket.url).toBe('ws://localhost:8080/ws/chat');
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
    nickname: 'Mahdi',
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
