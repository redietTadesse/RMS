// src/pages/Leases.jsx
import { useState, useEffect } from 'react';
import { leasesApi, tenantsApi, spacesApi } from '../services/api';
import { FaEdit, FaTrash, FaPlus, FaFileInvoice } from 'react-icons/fa';
import DashboardLayout from '../components/Layout/DashboardLayout';
import { Loader2, X, CheckCircle, AlertCircle } from 'lucide-react';

const inputClass =
  'w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 bg-slate-50 focus:bg-white transition';
const labelClass = 'mb-1 block text-sm font-medium text-slate-700';

function StatusBadge({ status }) {
  const styles = {
    ACTIVE: 'bg-emerald-100 text-emerald-700',
    EXPIRED: 'bg-amber-100 text-amber-700',
    TERMINATED: 'bg-rose-100 text-rose-700',
  };
  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${styles[status] || styles.ACTIVE}`}>
      {status || 'ACTIVE'}
    </span>
  );
}

export default function Leases() {
  const [leases, setLeases] = useState([]);
  const [tenants, setTenants] = useState([]);
  const [spaces, setSpaces] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);
  const [formData, setFormData] = useState({
    tenantId: '',
    spaceId: '',
    startDate: '',
    endDate: '',
    monthlyRent: '',
    deposit: '',
    prepaymentMonths: '3',
    dueDay: '1',
  });

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [leasesData, tenantsData, spacesData] = await Promise.all([
        leasesApi.getAll(),
        tenantsApi.getAll(),
        spacesApi.getAll(),
      ]);
      setLeases(leasesData);
      setTenants(tenantsData);
      setSpaces(spacesData);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const resetForm = () => {
    setFormData({
      tenantId: '',
      spaceId: '',
      startDate: '',
      endDate: '',
      monthlyRent: '',
      deposit: '',
      prepaymentMonths: '3',
      dueDay: '1',
    });
    setEditingId(null);
  };

  const handleOpenAdd = () => {
    resetForm();
    setShowForm(true);
  };

  const handleEdit = (lease) => {
    setEditingId(lease.id);
    setFormData({
      tenantId: lease.tenant?.id || '',
      spaceId: lease.space?.id || '',
      startDate: lease.startDate || '',
      endDate: lease.endDate || '',
      monthlyRent: lease.monthlyRent?.toString() || '',
      deposit: lease.deposit?.toString() || '',
      prepaymentMonths: lease.prepaymentMonths?.toString() || '3',
      dueDay: lease.dueDay?.toString() || '1',
    });
    setShowForm(true);
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');

    // Validation
    if (parseInt(formData.prepaymentMonths) < 3) {
      setError('Prepayment must be at least 3 months');
      setSaving(false);
      return;
    }

    if (formData.startDate > formData.endDate) {
      setError('Start date cannot be after end date');
      setSaving(false);
      return;
    }

    const payload = {
      tenantId: parseInt(formData.tenantId),
      spaceId: parseInt(formData.spaceId),
      startDate: formData.startDate,
      endDate: formData.endDate,
      monthlyRent: parseFloat(formData.monthlyRent),
      deposit: parseFloat(formData.deposit) || 0,
      prepaymentMonths: parseInt(formData.prepaymentMonths),
      dueDay: parseInt(formData.dueDay),
    };

    try {
      if (editingId) {
        await leasesApi.update(editingId, payload);
      } else {
        await leasesApi.create(payload);
      }
      setShowForm(false);
      await loadData();
    } catch (e) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  };

  const handleTerminate = async (id) => {
    if (!window.confirm('Terminate this lease? The space will become vacant.')) return;
    try {
      await leasesApi.terminate(id);
      await loadData();
    } catch (e) {
      setError(e.message);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this lease? This cannot be undone.')) return;
    try {
      await leasesApi.remove(id);
      await loadData();
    } catch (e) {
      setError(e.message);
    }
  };

  // Check if selected space is available
  const selectedSpace = spaces.find(s => s.id === parseInt(formData.spaceId));
  const isSpaceOccupied = selectedSpace?.status === 'OCCUPIED';

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Lease Management</h1>
            <p className="text-sm text-slate-500">Manage all lease agreements</p>
          </div>
          <button
            onClick={handleOpenAdd}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg shadow-sm transition"
          >
            <FaPlus /> Create Lease
          </button>
        </div>

        {/* Error */}
        {error && (
          <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 border border-red-200 flex items-center gap-2">
            <AlertCircle className="w-4 h-4" />
            {error}
            <button onClick={() => setError('')} className="ml-auto text-red-500 hover:text-red-700">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Form */}
        {showForm && (
          <div className="bg-white rounded-xl border border-slate-100 p-6 shadow-sm animate-fade-in">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-slate-800">
                {editingId ? 'Edit Lease' : 'Create New Lease'}
              </h3>
              <button
                onClick={() => setShowForm(false)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Tenant - READ-ONLY when editing */}
                <div>
                  <label className={labelClass}>Tenant *</label>
                  {editingId ? (
                    <div className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm bg-slate-100 text-slate-700">
                      {tenants.find(t => t.id === parseInt(formData.tenantId))?.name || 'N/A'}
                    </div>
                  ) : (
                    <select
                      name="tenantId"
                      value={formData.tenantId}
                      onChange={handleChange}
                      className={inputClass}
                      required
                    >
                      <option value="">Select Tenant</option>
                      {tenants.map((t) => (
                        <option key={t.id} value={t.id}>
                          {t.name}
                        </option>
                      ))}
                    </select>
                  )}
                </div>

                {/* Space - READ-ONLY when editing */}
                <div>
                  <label className={labelClass}>Space *</label>
                  {editingId ? (
                    <div className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm bg-slate-100 text-slate-700">
                      {spaces.find(s => s.id === parseInt(formData.spaceId))?.unitNumber || 'N/A'}
                    </div>
                  ) : (
                    <select
                      name="spaceId"
                      value={formData.spaceId}
                      onChange={handleChange}
                      className={inputClass}
                      required
                    >
                      <option value="">Select Space</option>
                      {spaces.map((s) => {
                        const isAvailable = s.status === 'VACANT';
                        return (
                          <option key={s.id} value={s.id} disabled={!isAvailable}>
                            {s.unitNumber} ({s.status})
                          </option>
                        );
                      })}
                    </select>
                  )}
                  {!editingId && formData.spaceId && (
                    <p className={`text-xs mt-1.5 font-medium ${isSpaceOccupied ? 'text-amber-600' : 'text-emerald-600'}`}>
                      {isSpaceOccupied ? '⚠️ This space is currently occupied' : '✅ This space is available'}
                    </p>
                  )}
                </div>

                {/* Start Date */}
                <div>
                  <label className={labelClass}>Start Date *</label>
                  <input
                    type="date"
                    name="startDate"
                    value={formData.startDate}
                    onChange={handleChange}
                    className={inputClass}
                    required
                  />
                </div>

                {/* End Date */}
                <div>
                  <label className={labelClass}>End Date *</label>
                  <input
                    type="date"
                    name="endDate"
                    value={formData.endDate}
                    onChange={handleChange}
                    className={inputClass}
                    required
                  />
                </div>

                {/* Monthly Rent */}
                <div>
                  <label className={labelClass}>Monthly Rent (ETB) *</label>
                  <input
                    type="number"
                    name="monthlyRent"
                    value={formData.monthlyRent}
                    onChange={handleChange}
                    className={inputClass}
                    placeholder="e.g. 15000"
                    required
                  />
                </div>

                {/* Deposit */}
                <div>
                  <label className={labelClass}>Security Deposit (ETB)</label>
                  <input
                    type="number"
                    name="deposit"
                    value={formData.deposit}
                    onChange={handleChange}
                    className={inputClass}
                    placeholder="e.g. 30000"
                  />
                </div>

                {/* Prepayment */}
                <div>
                  <label className={labelClass}>Prepayment (months) *</label>
                  <input
                    type="number"
                    name="prepaymentMonths"
                    min="3"
                    value={formData.prepaymentMonths}
                    onChange={handleChange}
                    className={inputClass}
                    placeholder="Min 3"
                    required
                  />
                  <p className="text-xs text-slate-400 mt-1">Minimum 3 months required</p>
                </div>

                {/* Due Day */}
                <div>
                  <label className={labelClass}>Payment Due Day</label>
                  <select
                    name="dueDay"
                    value={formData.dueDay}
                    onChange={handleChange}
                    className={inputClass}
                  >
                    {[...Array(31)].map((_, i) => (
                      <option key={i + 1} value={i + 1}>{i + 1}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="flex gap-3 mt-6">
                <button
                  type="submit"
                  disabled={saving || (!editingId && isSpaceOccupied)}
                  className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg shadow-sm transition disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {saving ? (
                    <Loader2 className="w-4 h-4 animate-spin inline mr-2" />
                  ) : null}
                  {editingId ? 'Update Lease' : 'Create Lease'}
                </button>
                <button
                  type="button"
                  onClick={() => setShowForm(false)}
                  className="px-6 py-2.5 border border-slate-200 hover:bg-slate-50 text-slate-700 font-medium rounded-lg transition"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Table */}
        <div className="bg-white rounded-xl border border-slate-100 shadow-sm overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-100">
              <tr>
                {['Tenant', 'Space', 'Start', 'End', 'Rent', 'Status', 'Actions'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-slate-400">
                    <Loader2 className="w-5 h-5 animate-spin inline mr-2" /> Loading…
                  </td>
                </tr>
              ) : leases.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-slate-400">
                    No leases created yet. Click "Create Lease" to get started.
                  </td>
                </tr>
              ) : (
                leases.map((lease) => (
                  <tr key={lease.id} className="hover:bg-slate-50/60 transition">
                    <td className="px-4 py-3 font-medium text-slate-800">
                      {lease.tenant?.name || 'N/A'}
                    </td>
                    <td className="px-4 py-3 text-slate-600">{lease.space?.unitNumber || 'N/A'}</td>
                    <td className="px-4 py-3 text-slate-600">{lease.startDate}</td>
                    <td className="px-4 py-3 text-slate-600">{lease.endDate}</td>
                    <td className="px-4 py-3 font-semibold text-slate-800">
                      ETB {Number(lease.monthlyRent).toLocaleString()}
                    </td>
                    <td className="px-4 py-3"><StatusBadge status={lease.status} /></td>
                    <td className="px-4 py-3">
                      <div className="flex gap-1.5">
                        <button
                          onClick={() => handleEdit(lease)}
                          className="p-1.5 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition"
                          title="Edit"
                        >
                          <FaEdit className="w-4 h-4" />
                        </button>
                        {lease.status === 'ACTIVE' && (
                          <button
                            onClick={() => handleTerminate(lease.id)}
                            className="p-1.5 text-slate-500 hover:text-amber-600 hover:bg-amber-50 rounded-lg transition"
                            title="Terminate"
                          >
                            <CheckCircle className="w-4 h-4" />
                          </button>
                        )}
                        <button
                          onClick={() => handleDelete(lease.id)}
                          className="p-1.5 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
                          title="Delete"
                        >
                          <FaTrash className="w-4 h-4" />
                        </button>
                        <button
                          className="p-1.5 text-slate-500 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition"
                          title="Generate Invoice"
                        >
                          <FaFileInvoice className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          {!loading && leases.length > 0 && (
            <div className="px-4 py-2.5 bg-slate-50 border-t border-slate-100 text-xs text-slate-500">
              Total: {leases.length} lease{leases.length !== 1 ? 's' : ''}
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
