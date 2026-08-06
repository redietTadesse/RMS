// src/services/api.js
const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

// Helper to get token
const getToken = () => localStorage.getItem('token');

// Helper to get headers with auth
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

// Fetch wrapper
async function request(path, options = {}) {
  const headers = getHeaders(options);
  
  const res = await fetch(`${API_BASE}${path}`, {
    headers,
    ...options,
  });

  if (res.status === 204) return null;

  let data = null;
  const text = await res.text();
  if (text) {
    try { data = JSON.parse(text); } catch { data = text; }
  }

  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    if (data && typeof data === 'object') {
      message = data.error || Object.values(data).join(', ') || message;
    }
    throw new Error(message);
  }
  return data;
}

// ===== AUTH API =====
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
};

// ===== Spaces =====
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

// ===== Tenants =====
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

// ===== Documents =====
export const documentsApi = {
  getByTenant: (tenantId) => request(`/tenants/${tenantId}/documents`),
  upload: (tenantId, type, file) => {
    const form = new FormData();
    form.append('type', type);
    form.append('file', file);
    return request(`/tenants/${tenantId}/documents`, {
      method: 'POST',
      body: form,
    });
  },
  downloadUrl: (tenantId, documentId) =>
    `${API_BASE}/tenants/${tenantId}/documents/${documentId}/download`,
};

// ===== Dashboard =====
export const dashboardApi = {
  getStats: () => request('/dashboard/stats'),
};

// ===== Leases =====
export const leasesApi = {
  getAll: () => request('/leases'),
  getById: (id) => request(`/leases/${id}`),
  create: (lease) =>
    request('/leases', { method: 'POST', body: JSON.stringify(lease) }),
  update: (id, lease) =>
    request(`/leases/${id}`, { method: 'PUT', body: JSON.stringify(lease) }),
  terminate: (id) =>
    request(`/leases/${id}/terminate`, { method: 'PUT' }),
  getActive: () => request('/leases/active'),
  getByTenant: (tenantId) => request(`/leases/tenant/${tenantId}`),
};




// Invoices
export const invoicesApi = {
  getAll: () => request('/invoices'),
  getById: (id) => request(`/invoices/${id}`),
  getByStatus: (status) => request(`/invoices/status/${status}`),
  getByTenant: (tenantId) => request(`/invoices/tenant/${tenantId}`),
  generate: (leaseId) =>
    request(`/invoices/generate/${leaseId}`, { method: 'POST' }),
};

// ===== Payments =====
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
};