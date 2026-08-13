// src/services/api.js
const API_BASE =
  import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

const getToken = () => localStorage.getItem('token');

const getHeaders = (options = {}) => {
  const headers =
    options.body instanceof FormData
      ? {}
      : { 'Content-Type': 'application/json' };

  const token = getToken();

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return headers;
};

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
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!res.ok) {
    let message = `Request failed (${res.status})`;

    if (data && typeof data === 'object') {
      message =
        data.error || Object.values(data).join(', ') || message;
    }

    throw new Error(message);
  }

  return data;
}

// ===== AUTH =====
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

// ===== SPACES =====
export const spacesApi = {
  getAll: (status) =>
    request(`/spaces${status ? `?status=${status}` : ''}`),

  getById: (id) =>
    request(`/spaces/${id}`),

  create: (space) =>
    request('/spaces', {
      method: 'POST',
      body: JSON.stringify(space),
    }),

  update: (id, space) =>
    request(`/spaces/${id}`, {
      method: 'PUT',
      body: JSON.stringify(space),
    }),

  remove: (id) =>
    request(`/spaces/${id}`, {
      method: 'DELETE',
    }),
};

// ===== TENANTS =====
export const tenantsApi = {
  getAll: (search) =>
    request(
      `/tenants${search ? `?search=${encodeURIComponent(search)}` : ''}`
    ),

  getById: (id) =>
    request(`/tenants/${id}`),

  create: (tenant) =>
    request('/tenants', {
      method: 'POST',
      body: JSON.stringify(tenant),
    }),

  update: (id, tenant) =>
    request(`/tenants/${id}`, {
      method: 'PUT',
      body: JSON.stringify(tenant),
    }),

  remove: (id) =>
    request(`/tenants/${id}`, {
      method: 'DELETE',
    }),
};

// ===== DOCUMENTS =====
export const documentsApi = {
  getForTenant: (tenantId) =>
    request(`/documents/tenant/${tenantId}`),

  getMyDocuments: () =>
    request('/documents/my-documents'),

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

  delete: (documentId) =>
    request(`/documents/${documentId}`, {
      method: 'DELETE',
    }),
};

// ===== DASHBOARD =====
export const dashboardApi = {
  getStats: () =>
    request('/dashboard/stats'),
};

// ===== LEASES =====
export const leasesApi = {
  getAll: () =>
    request('/leases'),

  getById: (id) =>
    request(`/leases/${id}`),

  create: (lease) =>
    request('/leases', {
      method: 'POST',
      body: JSON.stringify(lease),
    }),

  update: (id, lease) =>
    request(`/leases/${id}`, {
      method: 'PUT',
      body: JSON.stringify(lease),
    }),

  terminate: (id) =>
    request(`/leases/${id}/terminate`, {
      method: 'PUT',
    }),

  markExpired: () =>
    request('/leases/test/mark-expired', {
      method: 'POST',
    }),

  forceExpire: (id) =>
    request(`/leases/test/${id}/force-expire`, {
      method: 'POST',
    }),

  remove: (id) =>
    request(`/leases/${id}`, {
      method: 'DELETE',
    }),

  getActive: () =>
    request('/leases/active'),

  getByTenant: (tenantId) =>
    request(`/leases/tenant/${tenantId}`),
};

// ===== INVOICES =====
export const invoicesApi = {
  getAll: () =>
    request('/invoices'),

  getById: (id) =>
    request(`/invoices/${id}`),

  getByStatus: (status) =>
    request(`/invoices/status/${status}`),

  getByTenant: (tenantId) =>
    request(`/invoices/tenant/${tenantId}`),

  generate: (leaseId) =>
    request(`/invoices/generate/${leaseId}`, {
      method: 'POST',
    }),

  markOverdue: () =>
    request('/invoices/test/mark-overdue', {
      method: 'POST',
    }),

  forceOverdue: (id) =>
    request(`/invoices/test/${id}/force-overdue`, {
      method: 'POST',
    }),

  downloadPdf: (id) => {
    const token = getToken();

    return fetch(`${API_BASE}/invoices/${id}/pdf`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }).then((res) => {
      if (!res.ok) {
        throw new Error('Failed to download invoice');
      }

      return res.blob();
    });
  },
};

// ===== PAYMENTS =====
export const paymentsApi = {
  record: (invoiceId, paymentData, receiptFile) => {
    const formData = new FormData();

    formData.append(
      'payment',
      new Blob([JSON.stringify(paymentData)], {
        type: 'application/json',
      })
    );

    if (receiptFile) {
      formData.append('receipt', receiptFile);
    }

    return request(`/payments/${invoiceId}`, {
      method: 'POST',
      body: formData,
    });
  },

  previewPenalty: (invoiceId, paymentDate) =>
    request(
      `/payments/preview?invoiceId=${invoiceId}&paymentDate=${paymentDate}`
    ),

  approve: (invoiceId) =>
    request(`/payments/${invoiceId}/approve`, { method: 'PUT' }),
};

// ===== TENANT (SELF) =====
export const tenantApi = {
  getMyLease: () =>
    request('/tenant/my-lease'),

  getMyInvoices: () =>
    request('/tenant/my-invoices'),

  getStats: () =>
    request('/tenant/stats'),

  submitReceipt: (invoiceId, paymentDate, receiptFile) => {
    const form = new FormData();
    form.append('receipt', receiptFile);
    return request(
      `/tenant/invoices/${invoiceId}/receipt?paymentDate=${paymentDate}&paymentMethod=receipt`,
      {
        method: 'POST',
        body: form,
      }
    );
  },
};

// ===== NOTIFICATIONS =====
export const notificationsApi = {
  getMine: () =>
    request('/notifications'),

  getUnreadCount: () =>
    request('/notifications/unread-count'),

  markRead: (id) =>
    request(`/notifications/${id}/read`, {
      method: 'PUT',
    }),

  markAllRead: () =>
    request('/notifications/read-all', {
      method: 'PUT',
    }),
};

// ===== ADMIN USERS =====
export const adminApi = {
  getAllUsers: () =>
    request('/users'),

  getUserById: (id) =>
    request(`/users/${id}`),

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

  deleteUser: (id) =>
    request(`/users/${id}`, {
      method: 'DELETE',
    }),

  toggleActive: (id) =>
    request(`/users/${id}/toggle-active`, {
      method: 'PATCH',
    }),

  changeRole: (id, role) =>
    request(
      `/users/${id}/role?role=${encodeURIComponent(role)}`,
      {
        method: 'PATCH',
      }
    ),
};

// ===== USER PROFILE =====
export const userApi = {
  getProfile: () =>
    request('/users/me'),

  changePassword: (payload) =>
    request('/users/me/password', {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
};

// ===== CHAPA =====
export const chapaApi = {
  initialize: (invoiceId, customerData) =>
    request('/payments/chapa/initialize', {
      method: 'POST',
      body: JSON.stringify({
        invoiceId,
        firstName: customerData.firstName,
        lastName: customerData.lastName,
        email: customerData.email,
        phoneNumber: customerData.phoneNumber,
      }),
    }),

  verify: (txRef) =>
    request(`/payments/chapa/verify?tx_ref=${txRef}`),
};

// ===== REPORTS =====
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