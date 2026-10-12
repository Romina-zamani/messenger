import '@testing-library/jest-dom/vitest';

class MockWebSocket {
  static instances = [];
  static OPEN = 1;

  constructor(url) {
    this.url = url;
    this.readyState = 0;
    this.sent = [];
    MockWebSocket.instances.push(this);
  }

  send(payload) {
    this.sent.push(payload);
  }

  close() {
    this.readyState = 3;
    this.onclose?.({ code: 1000 });
  }

  open() {
    this.readyState = MockWebSocket.OPEN;
    this.onopen?.();
  }

  receive(event) {
    this.onmessage?.({ data: JSON.stringify(event) });
  }
}

globalThis.WebSocket = MockWebSocket;

beforeEach(() => {
  MockWebSocket.instances = [];
  let signedIn = false;
  globalThis.fetch = vi.fn(async (url) => {
    if (url === '/api/auth/csrf') return { ok: true, status: 200, json: async () => ({ headerName: 'X-CSRF-TOKEN', token: 'test-token' }) };
    if (url === '/api/auth/login') { signedIn = true; return { ok: true, status: 204 }; }
    if (url === '/api/auth/logout') { signedIn = false; return { ok: true, status: 204 }; }
    if (url === '/api/auth/register') return { ok: true, status: 201, json: async () => ({ id: 1, username: 'Mahdi' }) };
    if (url === '/api/auth/me') return { ok: signedIn, status: signedIn ? 200 : 401, json: async () => ({ id: 1, username: 'Mahdi' }) };
    throw new Error(`Unexpected fetch: ${url}`);
  });
});
