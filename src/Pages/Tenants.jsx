// src/pages/Tenants.jsx - COMPLETE FILE
import { useEffect, useRef, useState } from 'react';
import { tenantsApi, documentsApi } from '../services/api';
import DashboardLayout from '../components/Layout/DashboardLayout';
import { Loader2, Plus, Search, Edit, Trash2, FileText, X } from 'lucide-react';

const inputClass = 'w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 bg-slate-50 focus:bg-white transition';
const labelClass = 'mb-1 block text-sm font-medium text-slate-700';

const EMPTY_FORM = { name: '', type: 'INDIVIDUAL', contact: '', phone: '', email: '', tinNumber: '', businessLicense: '' };
const DOC_TYPES = [
  { value: 'TIN_CERTIFICATE', label: 'TIN Certificate' },
  { value: 'ID_CARD', label: 'ID Card' },
  { value: 'BUSINESS_LICENSE', label: 'Business License' },
];

function DocBadge({ ok, label }) {
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${ok ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}`}>
      {label} {ok ? '✓' : '—'}
    </span>
  );
}

export default function Tenants() {
  const [tenants, setTenants] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const searchTimer = useRef(null);
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [docsTenant, setDocsTenant] = useState(null);
  const [docs, setDocs] = useState([]);
  const [docsLoading, setDocsLoading] = useState(false);
  const [docsError, setDocsError] = useState('');
  const [uploadingType, setUploadingType] = useState('');

  const loadTenants = async (q = search) => {
    setLoading(true);
    setError('');
    try {
      const data = await tenantsApi.getAll(q || undefined);
      setTenants(data);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadTenants(''); }, []);

  const handleSearchChange = (e) => {
    const q = e.target.value;
    setSearch(q);
    clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => loadTenants(q), 300);
  };

  const openAdd = () => { setEditingId(null); setFormData(EMPTY_FORM); setFormError(''); setShowModal(true); };
  const openEdit = (t) => {
    setEditingId(t.id);
    setFormData({ name: t.name || '', type: t.type || 'INDIVIDUAL', contact: t.contact || '', phone: t.phone || '', email: t.email || '', tinNumber: t.tinNumber || '', businessLicense: t.businessLicense || '' });
    setFormError('');
    setShowModal(true);
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((f) => ({ ...f, [name]: value }));
  };

  const handleSave = async () => {
    if (!formData.name.trim() || !formData.phone.trim()) {
      setFormError('Name and phone are required.');
      return;
    }
    const payload = { ...formData, name: formData.name.trim(), phone: formData.phone.trim(), email: formData.email.trim() || null };
    setSaving(true);
    setFormError('');
    try {
      if (editingId) {
        const current = tenants.find((t) => t.id === editingId);
        await tenantsApi.update(editingId, { ...payload, hasIdCard: current?.hasIdCard ?? false, hasBusinessLicense: current?.hasBusinessLicense ?? false });
      } else {
        await tenantsApi.create(payload);
      }
      setShowModal(false);
      await loadTenants();
    } catch (e) {
      setFormError(e.message);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (t) => {
    if (!window.confirm(`Delete tenant "${t.name}"?`)) return;
    try {
      await tenantsApi.remove(t.id);
      await loadTenants();
    } catch (e) {
      setError(e.message);
    }
  };

  const openDocs = async (t) => {
    setDocsTenant(t);
    setDocs([]);
    setDocsError('');
    setDocsLoading(true);
    try {
      setDocs(await documentsApi.getByTenant(t.id));
    } catch (e) {
      setDocsError(e.message);
    } finally {
      setDocsLoading(false);
    }
  };

  const handleUpload = async (type, file) => {
    if (!file) return;
    setUploadingType(type);
    setDocsError('');
    try {
      await documentsApi.upload(docsTenant.id, type, file);
      setDocs(await documentsApi.getByTenant(docsTenant.id));
      await loadTenants();
    } catch (e) {
      setDocsError(e.message);
    } finally {
      setUploadingType('');
    }
  };

  const docLabel = (type) => DOC_TYPES.find((d) => d.value === type)?.label || type;

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Tenants</h1>
            <p className="text-sm text-slate-500">Businesses and individuals renting spaces</p>
          </div>
          <button onClick={openAdd} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg shadow-sm transition">
            <Plus className="w-4 h-4" /> Add Tenant
          </button>
        </div>

        <div className="relative max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <input value={search} onChange={handleSearchChange} className={`${inputClass} pl-10`} placeholder="Search by name, email, or TIN…" />
        </div>

        {error && <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 border border-red-200">{error}</div>}

        <div className="bg-white rounded-xl border border-slate-100 shadow-sm overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-100">
              <tr>
                {['Name', 'Type', 'Contact', 'Phone', 'Email', 'TIN', 'Documents', 'Actions'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr><td colSpan={8} className="px-4 py-8 text-center text-slate-400"><Loader2 className="w-5 h-5 animate-spin inline mr-2" /> Loading…</td></tr>
              ) : tenants.length === 0 ? (
                <tr><td colSpan={8} className="px-4 py-8 text-center text-slate-400">{search ? `No tenants match "${search}".` : 'No tenants yet.'}</td></tr>
              ) : (
                tenants.map((t) => (
                  <tr key={t.id} className="hover:bg-slate-50/60 transition">
                    <td className="px-4 py-3 font-medium text-slate-800">{t.name}</td>
                    <td className="px-4 py-3 text-slate-600">{t.type === 'COMPANY' ? 'Company' : 'Individual'}</td>
                    <td className="px-4 py-3 text-slate-600">{t.contact || '—'}</td>
                    <td className="px-4 py-3 text-slate-600">{t.phone}</td>
                    <td className="px-4 py-3 text-slate-600">{t.email || '—'}</td>
                    <td className="px-4 py-3 text-slate-600">{t.tinNumber || '—'}</td>
                    <td className="px-4 py-3"><div className="flex flex-wrap gap-1"><DocBadge ok={t.hasIdCard} label="ID" /><DocBadge ok={t.hasBusinessLicense} label="License" /></div></td>
                    <td className="px-4 py-3">
                      <div className="flex gap-1.5">
                        <button onClick={() => openDocs(t)} className="p-1.5 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition"><FileText className="w-4 h-4" /></button>
                        <button onClick={() => openEdit(t)} className="p-1.5 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition"><Edit className="w-4 h-4" /></button>
                        <button onClick={() => handleDelete(t)} className="p-1.5 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"><Trash2 className="w-4 h-4" /></button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add/Edit Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto bg-white rounded-xl shadow-xl p-6">
            <h2 className="mb-4 text-lg font-semibold text-slate-800">{editingId ? 'Edit Tenant' : 'Add Tenant'}</h2>
            {formError && <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 border border-red-200">{formError}</div>}
            <div className="space-y-4">
              <div><label className={labelClass}>Name *</label><input name="name" value={formData.name} onChange={handleChange} className={inputClass} placeholder="Full name or company name" /></div>
              <div className="grid grid-cols-2 gap-4">
                <div><label className={labelClass}>Type *</label><select name="type" value={formData.type} onChange={handleChange} className={inputClass}><option value="INDIVIDUAL">Individual</option><option value="COMPANY">Company</option></select></div>
                <div><label className={labelClass}>Contact Person</label><input name="contact" value={formData.contact} onChange={handleChange} className={inputClass} placeholder="For companies" /></div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div><label className={labelClass}>Phone *</label><input name="phone" value={formData.phone} onChange={handleChange} className={inputClass} placeholder="+2519…" /></div>
                <div><label className={labelClass}>Email</label><input type="email" name="email" value={formData.email} onChange={handleChange} className={inputClass} placeholder="name@example.com" /></div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div><label className={labelClass}>TIN Number</label><input name="tinNumber" value={formData.tinNumber} onChange={handleChange} className={inputClass} placeholder="e.g. 0012345678" /></div>
                <div><label className={labelClass}>Business License No.</label><input name="businessLicense" value={formData.businessLicense} onChange={handleChange} className={inputClass} placeholder="License reference" /></div>
              </div>
              <p className="text-xs text-slate-500">ID and license files are attached from the Documents button after saving.</p>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button onClick={() => setShowModal(false)} className="px-4 py-2 border border-slate-200 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 transition" disabled={saving}>Cancel</button>
              <button onClick={handleSave} className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg shadow-sm transition disabled:opacity-60" disabled={saving}>
                {saving ? 'Saving…' : editingId ? 'Save Changes' : 'Add Tenant'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Documents Modal */}
      {docsTenant && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto bg-white rounded-xl shadow-xl p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-slate-800">Documents</h2>
              <button onClick={() => setDocsTenant(null)} className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition"><X className="w-5 h-5" /></button>
            </div>
            <p className="mb-4 text-sm text-slate-500">{docsTenant.name}</p>
            {docsError && <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 border border-red-200">{docsError}</div>}
            <div className="space-y-3">
              {DOC_TYPES.map(({ value, label }) => (
                <div key={value} className="rounded-lg border border-slate-200 p-3">
                  <label className={labelClass}>{label}</label>
                  <input type="file" accept=".pdf,.jpg,.jpeg,.png" disabled={uploadingType !== ''} onChange={(e) => { handleUpload(value, e.target.files[0]); e.target.value = ''; }} className="block w-full text-sm text-slate-600 file:mr-3 file:rounded-lg file:border-0 file:bg-indigo-50 file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-indigo-700 hover:file:bg-indigo-100" />
                  {uploadingType === value && <p className="mt-1 text-xs text-indigo-600">Uploading…</p>}
                </div>
              ))}
            </div>
            <h3 className="mb-2 mt-5 text-sm font-semibold text-slate-700">Uploaded files</h3>
            {docsLoading ? <p className="text-sm text-slate-400">Loading…</p> : docs.length === 0 ? <p className="text-sm text-slate-400">No documents uploaded yet.</p> : (
              <ul className="divide-y divide-slate-100 rounded-lg border border-slate-200">
                {docs.map((d) => (
                  <li key={d.id} className="flex items-center justify-between px-3 py-2 text-sm">
                    <div><p className="font-medium text-slate-800">{docLabel(d.type)}</p><p className="text-xs text-slate-500">{d.fileName}</p></div>
                    <a href={documentsApi.downloadUrl(docsTenant.id, d.id)} className="text-xs font-medium text-indigo-600 hover:underline">Download</a>
                  </li>
                ))}
              </ul>
            )}
            <div className="mt-6 flex justify-end">
              <button onClick={() => setDocsTenant(null)} className="px-4 py-2 border border-slate-200 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 transition">Close</button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}