import tokenManager from './TokenManager';

export const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

// Auth lifecycle events — dispatched on `window` so non-React modules (api.js)
// can signal React state (AuthContext) without creating an import cycle.
export const AUTH_EVENTS = {
  SESSION_EXPIRED: 'rms:session-expired',
  TOKEN_REFRESHED: 'rms:token-refreshed',
};

// Honor both the modern tokenManager keys and the legacy CRA-era 'token' key.
const getToken = () =>
  tokenManager.getAccessToken() || localStorage.getItem('token');

const getHeaders = (options = {}) => {
  const headers = options.body instanceof FormData
    ? {}
    : { 'Content-Type': 'application/json' };

  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return headers;
};

const readBody = async (res) => {
  if (res.status === 204) return null;
  const text = await res.text();
  if (!text) return null;
  try { return JSON.parse(text); } catch { return text; }
};

const errorFromResponse = (res, data) => {
  let message = `Request failed (${res.status})`;
  if (data && typeof data === 'object') {
    if (data.error) message = data.error;
    else if (data.message) message = data.message;
    else if (data.errors) {
      // Spring validation errors: { field: ["msg", ...], ... }
      const fieldErrors = Object.values(data.errors).flat().join(', ');
      if (fieldErrors) message = fieldErrors;
    } else {
      const joined = Object.values(data).flat().join(', ');
      if (joined) message = joined;
    }
  } else if (typeof data === 'string' && data) {
    message = data;
  }
  return new Error(message);
};

// Only one refresh may be in flight at a time. Both the 401 handler below and
// AuthContext's scheduled refresh go through this, so they can never race each
// other and invalidate a freshly issued token.
let refreshPromise = null;
let sessionExpiredNotified = false;

const notify = (name, detail) => {
  try {
    window.dispatchEvent(new CustomEvent(name, detail ? { detail } : undefined));
  } catch {
    /* non-browser environment (tests / SSR) */
  }
};

export async function refreshAccessTokenOnce(refreshToken) {
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    const rt = refreshToken || tokenManager.getRefreshToken();
    if (!rt) {
      const err = new Error('No refresh token available');
      err.kind = 'auth';
      throw err;
    }

    let res;
    try {
      res = await fetch(`${API_BASE}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: rt }),
      });
    } catch {
      const err = new Error('Network error while refreshing your session');
      err.kind = 'network';
      throw err;
    }

    const data = await readBody(res);
    if (!res.ok) {
      const err = errorFromResponse(res, data);
      err.kind = 'auth';
      throw err;
    }

    tokenManager.setTokens(data.token, data.refreshToken || rt);
    if (data.id && data.username) {
      const user = {
        id: data.id,
        username: data.username,
        name: data.name,
        email: data.email,
        role: data.role,
      };
      tokenManager.setUser(user);
      sessionExpiredNotified = false;
      notify(AUTH_EVENTS.TOKEN_REFRESHED, user);
    }
    return data;
  })();

  try {
    return await refreshPromise;
  } finally {
    refreshPromise = null;
  }
}

function handleSessionExpired() {
  tokenManager.clearAll();
  if (!sessionExpiredNotified) {
    sessionExpiredNotified = true;
    notify(AUTH_EVENTS.SESSION_EXPIRED);
  }
  const err = new Error('Your session has expired. Please sign in again.');
  err.kind = 'auth';
  throw err;
}

async function request(path, options = {}) {
  let retried = false;

  const attempt = async () => {
    const headers = getHeaders(options);
    let res;
    try {
      res = await fetch(`${API_BASE}${path}`, { headers, ...options });
    } catch {
      const err = new Error('Network error — check your connection and try again.');
      err.kind = 'network';
      throw err;
    }

    const data = await readBody(res);

    if (res.ok) {
      sessionExpiredNotified = false;
      return data;
    }

    const err = errorFromResponse(res, data);

    // Expired / invalid access token: try a transparent refresh and retry once.
    const isAuthRoute = path.startsWith('/auth/');
    if (res.status === 401 && !isAuthRoute && !retried && tokenManager.getRefreshToken()) {
      try {
        await refreshAccessTokenOnce();
        retried = true;
        return await attempt();
      } catch {
        handleSessionExpired();
      }
    }

    if (res.status === 401) {
      err.kind = 'auth';
    }
    throw err;
  };

  return attempt();
}

async function downloadBlob(path) {
  let retried = false;

  const attempt = async () => {
    let res;
    try {
      res = await fetch(`${API_BASE}${path}`, {
        headers: getHeaders(),
      });
    } catch {
      const err = new Error('Network error — check your connection and try again.');
      err.kind = 'network';
      throw err;
    }

    if (res.ok) {
      sessionExpiredNotified = false;
      return res.blob();
    }

    const data = await readBody(res);
    const err = errorFromResponse(res, data);
    if (res.status === 401 && !retried && tokenManager.getRefreshToken()) {
      try {
        await refreshAccessTokenOnce();
        retried = true;
        return attempt();
      } catch {
        handleSessionExpired();
      }
    }
    if (res.status === 401) err.kind = 'auth';
    throw err;
  };

  return attempt();
}

export async function downloadFile(path, filename) {
  const blob = await downloadBlob(path);
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

export const authApi = {
  login: (username, password) =>
    request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),
  register: (userData) =>
    request('/auth/register', {
      method: 'POST',
      body: JSON.stringify(userData),
    }),
  refreshToken: (refreshToken) => refreshAccessTokenOnce(refreshToken),
  // Send the refresh token so the server can invalidate the session even when
  // the access token is already expired (the logout endpoint is public).
  logout: (refreshToken) =>
    request('/auth/logout', {
      method: 'POST',
      ...(refreshToken ? { body: JSON.stringify({ refreshToken }) } : {}),
    }),
};

export const spacesApi = {
  getAll: (status) =>
    request(`/spaces${status ? `?status=${status}` : ''}`),
  getById: (id) => request(`/spaces/${id}`),
  create: (space) =>
    request('/spaces', { method: 'POST', body: JSON.stringify(space) }),
  update: (id, space) =>
    request(`/spaces/${id}`, { method: 'PUT', body: JSON.stringify(space) }),
  remove: (id) => request(`/spaces/${id}`, { method: 'DELETE' }),
};

export const tenantsApi = {
  getAll: (search) =>
    request(`/tenants${search ? `?search=${encodeURIComponent(search)}` : ''}`),
  getById: (id) => request(`/tenants/${id}`),
  create: (tenant) =>
    request('/tenants', { method: 'POST', body: JSON.stringify(tenant) }),
  update: (id, tenant) =>
    request(`/tenants/${id}`, { method: 'PUT', body: JSON.stringify(tenant) }),
  remove: (id) => request(`/tenants/${id}`, { method: 'DELETE' }),
};

export const documentsApi = {
  getForTenant: (tenantId) => request(`/documents/tenant/${tenantId}`),
  getMyDocuments: () => request('/documents/my-documents'),
  uploadForTenant: (tenantId, type, description, file) => {
    const formData = new FormData();
    formData.append('type', type);
    formData.append('description', description || '');
    formData.append('file', file);
    return request(`/documents/upload/${tenantId}`, {
      method: 'POST',
      body: formData,
    });
  },
  downloadUrl: (documentId) =>
    `${API_BASE}/documents/download/${documentId}`,
  download: (documentId, filename) =>
    downloadFile(`/documents/download/${documentId}`, filename || 'document'),
  delete: (documentId) =>
    request(`/documents/${documentId}`, { method: 'DELETE' }),
};

export const dashboardApi = {
  getStats: () => request('/dashboard/stats'),
};

export const leasesApi = {
  getAll: () => request('/leases'),
  getById: (id) => request(`/leases/${id}`),
  create: (lease) =>
    request('/leases', { method: 'POST', body: JSON.stringify(lease) }),
  update: (id, lease) =>
    request(`/leases/${id}`, { method: 'PUT', body: JSON.stringify(lease) }),
  terminate: (id) =>
    request(`/leases/${id}/terminate`, { method: 'PUT' }),
  markExpired: () =>
    request('/leases/test/mark-expired', { method: 'POST' }),
  remove: (id) =>
    request(`/leases/${id}`, { method: 'DELETE' }),
  getActive: () => request('/leases/active'),
  getByTenant: (tenantId) => request(`/leases/tenant/${tenantId}`),
};

export const invoicesApi = {
  getAll: () => request('/invoices'),
  getById: (id) => request(`/invoices/${id}`),
  getByStatus: (status) => request(`/invoices/status/${status}`),
  getByTenant: (tenantId) => request(`/invoices/tenant/${tenantId}`),
  generate: async (leaseId) => {
    try {
      const response = await request(`/invoices/generate/${leaseId}`, { method: 'POST' });
      return response;
    } catch (err) {
      if (err.message?.includes('400')) {
        return null;
      }
      throw err;
    }
  },
  markOverdue: () =>
    request('/invoices/test/mark-overdue', { method: 'POST' }),
  forceOverdue: (id) =>
    request(`/invoices/test/${id}/force-overdue`, { method: 'POST' }),
  downloadPdf: (id, invoiceNumber) =>
    downloadFile(`/invoices/${id}/pdf`, `invoice-${invoiceNumber || id}.pdf`),
};

export const paymentsApi = {
  record: (invoiceId, paymentData, receiptFile) => {
    const formData = new FormData();
    formData.append('payment', new Blob([JSON.stringify(paymentData)], {
      type: 'application/json'
    }));
    if (receiptFile) {
      formData.append('receipt', receiptFile);
    }
    return request(`/payments/${invoiceId}`, {
      method: 'POST',
      body: formData,
    });
  },
  previewPenalty: (invoiceId, paymentDate) =>
    request(`/payments/preview?invoiceId=${invoiceId}&paymentDate=${paymentDate}`),
  approve: (invoiceId) =>
    request(`/payments/${invoiceId}/approve`, { method: 'PUT' }),
};

export const chapaApi = {
  initialize: (invoiceId, phoneNumber) =>
    request('/payments/chapa/initialize', {
      method: 'POST',
      body: JSON.stringify({ invoiceId, phoneNumber }),
    }),
  verify: (txRef) =>
    request(`/payments/chapa/verify?tx_ref=${txRef}`),
  confirm: (txRef) =>
    request(`/payments/chapa/confirm?tx_ref=${txRef}`, { method: 'POST' }),
};

export const tenantApi = {
  getMyLease: () => request('/tenant/my-lease'),
  getMyInvoices: () => request('/tenant/my-invoices'),
  getStats: () => request('/tenant/stats'),
  submitReceipt: (invoiceId, paymentDate, receiptFile) => {
    const formData = new FormData();
    formData.append('payment', new Blob(
      [JSON.stringify({ paymentDate, paymentMethod: 'RECEIPT' })],
      { type: 'application/json' }
    ));
    formData.append('receipt', receiptFile);
    return request(`/payments/${invoiceId}`, {
      method: 'POST',
      body: formData,
    });
  },
};

export const adminApi = {
  getAllUsers: () => request('/users'),
  getUserById: (id) => request(`/users/${id}`),
  createUser: (userData) =>
    request('/users', {
      method: 'POST',
      body: JSON.stringify(userData),
    }),
  updateUser: (id, userData) =>
    request(`/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(userData),
    }),
  deleteUser: (id) => request(`/users/${id}`, { method: 'DELETE' }),
  toggleActive: (id) =>
    request(`/users/${id}/toggle-active`, { method: 'PATCH' }),
  changeRole: (id, role) =>
    request(`/users/${id}/role?role=${encodeURIComponent(role)}`, { method: 'PATCH' }),

  // Withholding management
  uploadWithholdingEvidence: (tenantId, formData) =>
    request(`/admin/withholding/${tenantId}/evidence`, {
      method: 'POST',
      body: formData,
    }),
  activateWithholding: (tenantId, startDate) =>
    request(`/admin/withholding/${tenantId}/activate?startDate=${encodeURIComponent(startDate)}`, {
      method: 'PUT',
    }),
  deactivateWithholding: (tenantId) =>
    request(`/admin/withholding/${tenantId}/deactivate`, {
      method: 'PUT',
    }),
  previewWithholding: (tenantId, rentAmount, invoiceDate) =>
    request(`/admin/withholding/${tenantId}/preview?rentAmount=${rentAmount}&invoiceDate=${invoiceDate}`),
};

export const userApi = {
  getProfile: () =>
    request('/users/me'),

  updateProfile: (userData) =>
    request('/users/me', {
      method: 'PUT',
      body: JSON.stringify(userData),
    }),

  changePassword: (payload) =>
    request('/users/me/password', {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
};

export const notificationsApi = {
  getMine: () => request('/notifications'),
  getUnreadCount: () => request('/notifications/unread-count'),
  markRead: (id) =>
    request(`/notifications/${id}/read`, { method: 'PUT' }),
  markAllRead: () =>
    request('/notifications/read-all', { method: 'PUT' }),
};

export const reportsApi = {
  getRevenue: (months = 6) =>
    request(`/reports/revenue?months=${months}`),
  getOutstandingBalance: () =>
    request('/reports/outstanding-balance'),
  getOccupancy: () =>
    request('/reports/occupancy'),
  getLeaseExpiry: (days = 90) =>
    request(`/reports/lease-expiry?days=${days}`),
};

export const settingsApi = {
  getSettings: () => request('/settings'),
  updateSettings: (settings) =>
    request('/settings', {
      method: 'PUT',
      body: JSON.stringify(settings),
    }),
};

export const accountingApi = {
  getSummary: (start, end) => {
    const params = new URLSearchParams();
    if (start) params.append('start', start);
    if (end) params.append('end', end);
    const qs = params.toString();
    return request(`/accounting/summary${qs ? `?${qs}` : ''}`);
  },
  getDashboardWidget: () => request('/accounting/dashboard-widget'),
};

// Test-only time-travel controls (admin only; backend gates /api/test/time/** by ADMIN role)
export const testApi = {
  getCurrentTime: () => request('/test/time/current'),
  setTimeDate: (date) =>
    request('/test/time/set-date?date=' + encodeURIComponent(date), { method: 'POST' }),
  advanceDays: (days) =>
    request('/test/time/advance-days?days=' + days, { method: 'POST' }),
  advanceMonths: (months) =>
    request('/test/time/advance-months?months=' + months, { method: 'POST' }),
  runScheduledJobs: (persist = true) =>
    request('/test/time/run-scheduled-jobs?persist=' + persist, { method: 'POST' }),
  resetTime: () =>
    request('/test/time/reset', { method: 'POST' }),
};
