// src/services/verificationApi.js
// Standalone verification client. Kept separate from api.js on purpose so it
// never collides with the shared api.js during merges. Sends the admin JWT the
// same way api.js does, so the /api/verification/** endpoints can be ADMIN-only.

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const getToken = () => localStorage.getItem('token');

async function post(path, body) {
  let res;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {}),
      },
      body: JSON.stringify(body),
    });
  } catch {
    const e = new Error('network');
    e.kind = 'network';
    throw e;
  }

  if (res.status === 401 || res.status === 403) {
    const e = new Error('auth');
    e.kind = 'auth';
    e.status = res.status;
    throw e;
  }

  // Backend returns 200 with a result body even for FAILED — parse regardless.
  let data = null;
  const text = await res.text();
  if (text) {
    try { data = JSON.parse(text); } catch { data = text; }
  }

  if (!res.ok && !data) {
    const e = new Error('http');
    e.kind = 'http';
    e.status = res.status;
    throw e;
  }
  return data;
}

export const verificationApi = {
  verifyTin: (tin) => post('/verification/tin', { tin }),
  initiateIdentity: (faydaNumber) => post('/verification/identity/initiate', { faydaNumber }),
  completeIdentity: (transactionId, otp) => post('/verification/identity/complete', { transactionId, otp }),
};
