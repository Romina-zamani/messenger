export async function currentAccount() {
  const response = await fetch('/api/auth/me', { credentials: 'same-origin' });
  if (response.status === 401) return null;
  if (!response.ok) throw new Error('Could not check your session. Please try again.');
  return response.json();
}

export async function authPost(action, values = {}) {
  const tokenResponse = await fetch('/api/auth/csrf', { credentials: 'same-origin' });
  if (!tokenResponse.ok) throw new Error('Could not connect to the login service. Please try again.');
  const csrf = await tokenResponse.json();
  const registration = action === 'register';
  const response = await fetch(`/api/auth/${action}`, {
    method: 'POST',
    credentials: 'same-origin',
    headers: {
      'Content-Type': registration ? 'application/json' : 'application/x-www-form-urlencoded',
      [csrf.headerName]: csrf.token
    },
    body: registration ? JSON.stringify(values) : new URLSearchParams(values).toString()
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    const fallback = response.status === 401 ? 'Invalid username or password.'
      : response.status === 403 ? 'Your session changed. Please try again.'
        : 'The request failed. Please try again.';
    throw new Error(body.detail || fallback);
  }
}
