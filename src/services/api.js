// ============================================================
// API service layer — all backend calls go through this file.
// Backend: Spring Boot at http://localhost:8080 (CORS enabled
// for the Vite dev server). Override with VITE_API_URL in .env
// ============================================================

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

/**
 * Small fetch wrapper: JSON in/out, throws a readable Error on
 * non-2xx responses (using the backend's {error: ...} or field
 * validation messages when available).
 */
async function request(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: options.body instanceof FormData
      ? undefined // let the browser set the multipart boundary
      : { 'Content-Type': 'application/json' },
    ...options,
  })

  if (res.status === 204) return null

  let data = null
  const text = await res.text()
  if (text) {
    try { data = JSON.parse(text) } catch { data = text }
  }

  if (!res.ok) {
    let message = `Request failed (${res.status})`
    if (data && typeof data === 'object') {
      // {error: "..."} or {fieldName: "validation message", ...}
      message = data.error || Object.values(data).join(', ') || message
    }
    throw new Error(message)
  }
  return data
}

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
}

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
}

// ===== Tenant documents =====
export const documentsApi = {
  getByTenant: (tenantId) => request(`/tenants/${tenantId}/documents`),
  /** type: 'TIN_CERTIFICATE' | 'ID_CARD' | 'BUSINESS_LICENSE' */
  upload: (tenantId, type, file) => {
    const form = new FormData()
    form.append('type', type)
    form.append('file', file)
    return request(`/tenants/${tenantId}/documents`, {
      method: 'POST',
      body: form,
    })
  },
  downloadUrl: (tenantId, documentId) =>
    `${API_BASE}/tenants/${tenantId}/documents/${documentId}/download`,
}

// ===== Dashboard =====
export const dashboardApi = {
  getStats: () => request('/dashboard/stats'),
}

// ===== Leases & Invoices (read-only for now) =====
export const leasesApi = {
  getAll: () => request('/leases'),
}

export const invoicesApi = {
  getAll: () => request('/invoices'),
}
