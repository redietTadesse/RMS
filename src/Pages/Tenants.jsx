import EthiopianDatePicker from "../Components/common/EthiopianDatePicker";
import { formatDualDate } from "../utils/ethiopianCalendar";
import { useEffect, useRef, useState } from 'react';
import { tenantsApi, documentsApi, adminApi, API_BASE } from '../services/api';
import { verificationApi } from '../services/verificationApi';
import DashboardLayout from '../Components/Layout/DashboardLayout';
import { Loader2, Plus, Search, Edit, Trash2, FileText, X } from 'lucide-react';
import DocumentList from '../Components/Documents/DocumentList';
import DocumentUpload from '../Components/Documents/DocumentUpload';
const inputClass = 'w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 bg-slate-50 focus:bg-white transition';
const labelClass = 'mb-1 block text-sm font-medium text-slate-700';

const EMPTY_FORM = {
  name: '', type: 'INDIVIDUAL', contact: '', phone: '', email: '',
  tinNumber: '', businessLicense: '', faydaNumber: '',
  tinStatus: 'UNVERIFIED', identityStatus: 'UNVERIFIED', userId: '',
  isWithholdingTaxApplicable: false,
  withholdingStartDate: '',
  withholdingEvidenceVerified: false,
  withholdingEvidenceStatus: '',
  withholdingVerifiedBy: '',
  withholdingVerifiedAt: '',
  withholdingDocumentId: null,
};
const EMPTY_VERIFY = { tinBusy: false, tinResult: null, faydaBusy: false, faydaTxn: null, faydaOtp: '', faydaPending: null, faydaResult: null };

function DocBadge({ ok, label }) {
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${ok ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}`}>
      {label} {ok ? '✓' : '—'}
    </span>
  );
}

function WithholdingBadge({ tenant }) {
  if (!tenant.isWithholdingTaxApplicable) {
    return <span className="text-xs text-slate-400">—</span>;
  }
  if (!tenant.withholdingEvidenceVerified) {
    return <span className="inline-block rounded-full px-2 py-0.5 text-xs font-medium bg-amber-100 text-amber-700">Pending</span>;
  }
  return <span className="inline-block rounded-full px-2 py-0.5 text-xs font-medium bg-emerald-100 text-emerald-700">Active</span>;
}

function VerifyPill({ result }) {
  if (!result) return null;
  const map = {
    VERIFIED: 'bg-emerald-100 text-emerald-700',
    FAILED: 'bg-rose-100 text-rose-700',
    ERROR: 'bg-amber-100 text-amber-700',
    PENDING: 'bg-blue-100 text-blue-700',
    UNVERIFIED: 'bg-slate-100 text-slate-600',
  };
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${map[result.status] || map.UNVERIFIED}`}>
      {result.status}
    </span>
  );
}

export default function Tenants() {
  const [tenants, setTenants] = useState([]);
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const searchTimer = useRef(null);
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [verify, setVerify] = useState(EMPTY_VERIFY);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [docsTenant, setDocsTenant] = useState(null);

  // Document upload states
  const [showDocUpload, setShowDocUpload] = useState(false);
  const [tenantDocuments, setTenantDocuments] = useState([]);
  const [loadingDocs, setLoadingDocs] = useState(false);

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

  const loadDocuments = async (tenantId) => {
    setLoadingDocs(true);
    try {
      const data = await documentsApi.getForTenant(tenantId);
      setTenantDocuments(data);
    } catch (err) {
      console.error('Failed to load documents:', err);
    } finally {
      setLoadingDocs(false);
    }
  };

  const openDocs = async (t) => {
    setDocsTenant(t);
    await loadDocuments(t.id);
  };

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await tenantsApi.getAll(undefined);
        if (!cancelled) setTenants(data);
      } catch (e) {
        if (!cancelled) setError(e.message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    (async () => {
      try {
        const data = await adminApi.getAllUsers();
        if (!cancelled) setUsers((data || []).filter((u) => u.role === 'TENANT'));
      } catch (e) {
        console.error('Failed to load users:', e);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  const handleSearchChange = (e) => {
    const q = e.target.value;
    setSearch(q);
    clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => loadTenants(q), 300);
  };

  const openAdd = () => { setEditingId(null); setFormData(EMPTY_FORM); setVerify(EMPTY_VERIFY); setFormError(''); setShowModal(true); };
  const openEdit = (t) => {
    setEditingId(t.id);
    setFormData({
      name: t.name || '',
      type: t.type || 'INDIVIDUAL',
      contact: t.contact || '',
      phone: t.phone || '',
      email: t.email || '',
      tinNumber: t.tinNumber || '',
      businessLicense: t.businessLicense || '',
      faydaNumber: t.faydaNumber || '',
      tinStatus: t.tinStatus || 'UNVERIFIED',
      identityStatus: t.identityStatus || 'UNVERIFIED',
      userId: t.userId ?? '',
      isWithholdingTaxApplicable: !!t.isWithholdingTaxApplicable,
      withholdingStartDate: t.withholdingStartDate || '',
      withholdingEvidenceVerified: !!t.withholdingEvidenceVerified,
      withholdingVerifiedBy: t.withholdingVerifiedBy || '',
      withholdingVerifiedAt: t.withholdingVerifiedAt || '',
      withholdingDocumentId: t.withholdingDocumentId || null,
      withholdingEvidenceStatus: t.withholdingEvidenceVerified ? 'VERIFIED' : (t.withholdingDocumentId ? 'PENDING' : ''),
    });
    setVerify(EMPTY_VERIFY);
    setFormError('');
    setShowModal(true);
  };

  const handleWithholdingEvidenceUpload = async (file) => {
    if (!editingId) {
      setFormError('Please save the tenant first, then upload evidence.');
      return;
    }
    try {
      const fd = new FormData();
      fd.append('file', file);
      if (formData.tinNumber) fd.append('tinNumber', formData.tinNumber);
      if (formData.withholdingStartDate) fd.append('startDate', formData.withholdingStartDate);
      await adminApi.uploadWithholdingEvidence(editingId, fd);
      alert('✅ Evidence uploaded successfully! Awaiting admin verification.');
      setFormData((f) => ({ ...f, withholdingDocumentId: 'uploaded' }));
    } catch (err) {
      setFormError(err.message || 'Failed to upload evidence');
    }
  };

  const toErr = (err) =>
    err && err.kind === 'auth'
      ? { status: 'ERROR', message: `Server rejected the request (${err.status}). Check the ADMIN token and the /api/verification/** rule in SecurityConfig.` }
      : { status: 'ERROR', message: 'Could not reach the verification service. Is the backend running on 8080?' };

  const verifyTin = async () => {
    setVerify((v) => ({ ...v, tinBusy: true, tinResult: null }));
    try {
      const data = await verificationApi.verifyTin(formData.tinNumber);
      setVerify((v) => ({ ...v, tinResult: data }));
      setFormData((f) => ({ ...f, tinStatus: data.status }));
    } catch (err) {
      const r = toErr(err);
      setVerify((v) => ({ ...v, tinResult: r }));
      setFormData((f) => ({ ...f, tinStatus: 'ERROR' }));
    } finally {
      setVerify((v) => ({ ...v, tinBusy: false }));
    }
  };

  const faydaSendCode = async () => {
    setVerify((v) => ({ ...v, faydaBusy: true, faydaResult: null, faydaPending: null }));
    try {
      const data = await verificationApi.initiateIdentity(formData.faydaNumber);
      if (data && data.status === 'PENDING' && data.transactionId) {
        setVerify((v) => ({ ...v, faydaTxn: data.transactionId, faydaPending: data.message }));
      } else {
        setVerify((v) => ({ ...v, faydaResult: data }));
        setFormData((f) => ({ ...f, identityStatus: data.status }));
      }
    } catch (err) {
      const r = toErr(err);
      setVerify((v) => ({ ...v, faydaResult: r }));
      setFormData((f) => ({ ...f, identityStatus: 'ERROR' }));
    } finally {
      setVerify((v) => ({ ...v, faydaBusy: false }));
    }
  };

  const faydaVerifyCode = async () => {
    setVerify((v) => ({ ...v, faydaBusy: true }));
    try {
      const data = await verificationApi.completeIdentity(verify.faydaTxn, verify.faydaOtp);
      setVerify((v) => ({ ...v, faydaResult: data, faydaPending: null }));
      setFormData((f) => ({ ...f, identityStatus: data.status }));
    } catch (err) {
      const r = toErr(err);
      setVerify((v) => ({ ...v, faydaResult: r }));
      setFormData((f) => ({ ...f, identityStatus: 'ERROR' }));
    } finally {
      setVerify((v) => ({ ...v, faydaBusy: false }));
    }
  };

  const faydaReset = () => setVerify((v) => ({ ...v, faydaTxn: null, faydaOtp: '', faydaPending: null }));

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((f) => ({ ...f, [name]: value }));
  };

  const handleSave = async () => {
    if (!formData.name.trim() || !formData.phone.trim()) {
      setFormError('Name and phone are required.');
      return;
    }
    if (!formData.userId) {
      setFormError('Please select a user account for this tenant.');
      return;
    }
    const payload = {
      ...formData,
      name: formData.name.trim(),
      phone: formData.phone.trim(),
      email: formData.email.trim() || null,
      faydaNumber: formData.faydaNumber.trim() || null,
      userId: parseInt(formData.userId, 10),
    };
    setSaving(true);
    setFormError('');
    try {
      if (editingId) {
        const current = tenants.find((t) => t.id === editingId);
        await tenantsApi.update(editingId, {
          ...payload,
          hasIdCard: current?.hasIdCard ?? false,
          hasBusinessLicense: current?.hasBusinessLicense ?? false,
        });
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
                {['Name', 'Type', 'Contact', 'Phone', 'Email', 'TIN', 'Withholding', 'User', 'Documents', 'Actions'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr><td colSpan={10} className="px-4 py-8 text-center text-slate-400"><Loader2 className="w-5 h-5 animate-spin inline mr-2" /> Loading…</td></tr>
              ) : tenants.length === 0 ? (
                <tr><td colSpan={10} className="px-4 py-8 text-center text-slate-400">{search ? `No tenants match "${search}".` : 'No tenants yet.'}</td></tr>
              ) : (
                tenants.map((t) => (
                  <tr key={t.id} className="hover:bg-slate-50/60 transition">
                    <td className="px-4 py-3 font-medium text-slate-800">{t.name}</td>
                    <td className="px-4 py-3 text-slate-600">{t.type === 'COMPANY' ? 'Company' : 'Individual'}</td>
                    <td className="px-4 py-3 text-slate-600">{t.contact || '—'}</td>
                    <td className="px-4 py-3 text-slate-600">{t.phone}</td>
                    <td className="px-4 py-3 text-slate-600">{t.email || '—'}</td>
                    <td className="px-4 py-3 text-slate-600">{t.tinNumber || '—'}</td>
                    <td className="px-4 py-3"><WithholdingBadge tenant={t} /></td>
                    <td className="px-4 py-3 text-slate-600">{users.find((u) => u.id === t.userId)?.username || 'Not linked'}</td>
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
              <div className="space-y-2">
                <label className={labelClass}>TIN Number</label>
                <div className="flex gap-2">
                  <input name="tinNumber" value={formData.tinNumber} onChange={handleChange} className={inputClass} placeholder="e.g. 0012345678" />
                  <button type="button" onClick={verifyTin} disabled={verify.tinBusy || !formData.tinNumber.trim()} className="shrink-0 rounded-lg bg-slate-800 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50">{verify.tinBusy ? 'Checking…' : 'Verify'}</button>
                </div>
                {verify.tinResult && (<div className="flex items-center gap-2"><VerifyPill result={verify.tinResult} /><span className="text-xs text-slate-500">{verify.tinResult.message}</span></div>)}
              </div>
              {/* Withholding Tax Section */}
              <div className="border-t border-slate-100 pt-4 mt-4">
                <h3 className="text-sm font-semibold text-slate-700 mb-3">Withholding Tax</h3>
                <div className="flex items-center gap-3 mb-3">
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={formData.isWithholdingTaxApplicable}
                      onChange={(e) => {
                        setFormData({ ...formData, isWithholdingTaxApplicable: e.target.checked });
                      }}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-slate-200 peer-focus:ring-2 peer-focus:ring-indigo-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-indigo-600"></div>
                    <span className="ml-3 text-sm font-medium text-slate-700">Withholding Tax Applicable</span>
                  </label>
                  {formData.isWithholdingTaxApplicable && formData.withholdingEvidenceVerified && (
                    <span className="text-xs text-emerald-600 font-medium bg-emerald-50 px-2 py-0.5 rounded-full">Verified</span>
                  )}
                </div>
                {formData.isWithholdingTaxApplicable && (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-slate-50 rounded-lg p-4">
                    <div>
                      <label className="text-xs font-medium text-slate-500 block mb-1">TIN Number</label>
                      <input
                        name="tinNumber"
                        value={formData.tinNumber || ''}
                        onChange={handleChange}
                        className={inputClass}
                        placeholder="Enter TIN number"
                      />
                    </div>
                    <div>
                      <label className="text-xs font-medium text-slate-500 block mb-1">Effective Date</label>
                      <EthiopianDatePicker name="withholdingStartDate" value={formData.withholdingStartDate || ""} onChange={handleChange} className={inputClass} />
                      <p className="text-xs text-slate-400 mt-1">Withholding applies from this date forward</p>
                    </div>
                    <div className="md:col-span-2">
                      <label className="text-xs font-medium text-slate-500 block mb-1">Evidence Document</label>
                      <div className="flex items-center gap-3 flex-wrap">
                        <input
                          type="file"
                          accept=".pdf,.jpg,.jpeg,.png"
                          onChange={(e) => {
                            const file = e.target.files[0];
                            if (file) handleWithholdingEvidenceUpload(file);
                          }}
                          className="text-sm text-slate-500 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
                        />
                        {formData.withholdingDocumentId && (
                          <span className="text-xs text-emerald-600">✓ Evidence uploaded</span>
                        )}
                        {formData.withholdingEvidenceVerified ? (
                          <span className="inline-block rounded-full px-2 py-0.5 text-xs font-medium bg-emerald-100 text-emerald-700">Active</span>
                        ) : (
                          <span className="inline-block rounded-full px-2 py-0.5 text-xs font-medium bg-amber-100 text-amber-700">Pending Verification</span>
                        )}
                      </div>
                    </div>
                    {formData.withholdingEvidenceVerified && (
                      <div className="md:col-span-2 text-xs text-slate-500 bg-emerald-50 rounded-lg px-3 py-2">
                        Verified by: {formData.withholdingVerifiedBy || 'Admin'} on {formData.withholdingVerifiedAt ? formatDualDate(formData.withholdingVerifiedAt) : 'N/A'}
                      </div>
                    )}
                    {/* Activate / Deactivate buttons */}
                    {formData.isWithholdingTaxApplicable && formData.withholdingDocumentId && !formData.withholdingEvidenceVerified && (
                      <div className="md:col-span-2 flex gap-3 mt-2">
                        <button
                          type="button"
                          onClick={async () => {
                            if (!editingId) { setFormError('Please save the tenant first.'); return; }
                            if (!formData.withholdingStartDate) { setFormError('Please set an effective date.'); return; }
                            try {
                              await adminApi.activateWithholding(editingId, formData.withholdingStartDate);
                              setFormData((f) => ({ ...f, withholdingEvidenceVerified: true, withholdingEvidenceStatus: 'VERIFIED' }));
                              alert('✅ Withholding tax activated successfully!');
                            } catch (err) {
                              setFormError(err.message || 'Failed to activate withholding');
                            }
                          }}
                          className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-medium rounded-lg transition"
                        >
                          ✓ Activate Withholding
                        </button>
                      </div>
                    )}
                    {formData.isWithholdingTaxApplicable && formData.withholdingEvidenceVerified && (
                      <div className="md:col-span-2 flex gap-3 mt-2">
                        <button
                          type="button"
                          onClick={async () => {
                            if (!editingId) return;
                            if (!window.confirm('Deactivate withholding for this tenant?')) return;
                            try {
                              await adminApi.deactivateWithholding(editingId);
                              setFormData((f) => ({ ...f, withholdingEvidenceVerified: false, isWithholdingTaxApplicable: false, withholdingEvidenceStatus: '' }));
                              alert('✅ Withholding tax deactivated.');
                            } catch (err) {
                              setFormError(err.message || 'Failed to deactivate withholding');
                            }
                          }}
                          className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white text-sm font-medium rounded-lg transition"
                        >
                          ✕ Deactivate
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </div>
              <div><label className={labelClass}>Business License No.</label><input name="businessLicense" value={formData.businessLicense} onChange={handleChange} className={inputClass} placeholder="License reference" /></div>
              <div className="space-y-2">
                <label className={labelClass}>Fayda Number</label>
                {!verify.faydaTxn ? (
                  <div className="flex gap-2">
                    <input name="faydaNumber" value={formData.faydaNumber} onChange={handleChange} className={inputClass} placeholder="12-digit Fayda number" />
                    <button type="button" onClick={faydaSendCode} disabled={verify.faydaBusy || !formData.faydaNumber.trim()} className="shrink-0 rounded-lg bg-slate-800 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50">{verify.faydaBusy ? 'Sending…' : 'Send code'}</button>
                  </div>
                ) : (
                  <div className="space-y-2">
                    {verify.faydaPending && <p className="text-xs text-blue-600">{verify.faydaPending}</p>}
                    <div className="flex gap-2">
                      <input value={verify.faydaOtp} onChange={(e) => setVerify((v) => ({ ...v, faydaOtp: e.target.value }))} className={inputClass} placeholder="Enter code (demo: 123456)" />
                      <button type="button" onClick={faydaVerifyCode} disabled={verify.faydaBusy || !verify.faydaOtp.trim()} className="shrink-0 rounded-lg bg-slate-800 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-50">{verify.faydaBusy ? 'Verifying…' : 'Verify'}</button>
                    </div>
                    <button type="button" onClick={faydaReset} className="text-xs text-slate-500 underline hover:text-slate-700">Start over</button>
                  </div>
                )}
                {verify.faydaResult && (<div className="flex items-center gap-2"><VerifyPill result={verify.faydaResult} /><span className="text-xs text-slate-500">{verify.faydaResult.message}</span></div>)}
              </div>
              <div>
                <label className={labelClass}>User Account *</label>
                <select
                  name="userId"
                  value={formData.userId}
                  onChange={handleChange}
                  className={inputClass}
                  required
                >
                  <option value="">Select User</option>
                  {users.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.username} ({u.role})
                    </option>
                  ))}
                </select>
                <p className="text-xs text-slate-400 mt-1">
                  Link this tenant to a user account with TENANT role
                </p>
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

      {/* Documents Modal with Upload */}
      {docsTenant && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto bg-white rounded-xl shadow-xl p-6">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-lg font-semibold text-slate-800">Documents</h2>
                <p className="text-sm text-slate-500">{docsTenant.name}</p>
              </div>
              <button onClick={() => setDocsTenant(null)} className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Upload Button */}
            <button
              onClick={() => setShowDocUpload(true)}
              className="mb-4 flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg shadow-sm transition"
            >
              <Plus className="w-4 h-4" />
              Upload Document
            </button>

            {/* Document List */}
            {loadingDocs ? (
              <div className="flex items-center justify-center py-8">
                <Loader2 className="w-6 h-6 animate-spin text-indigo-600" />
              </div>
            ) : tenantDocuments.length === 0 ? (
              <div className="text-center py-8">
                <FileText className="w-12 h-12 mx-auto text-slate-300 mb-3" />
                <p className="text-slate-500">No documents uploaded yet.</p>
              </div>
            ) : (
              <div className="divide-y divide-slate-100">
                {tenantDocuments.map((doc) => (
                  <div key={doc.id} className="flex items-center justify-between px-4 py-3 hover:bg-slate-50/60 transition">
                    <div>
                      <p className="font-medium text-slate-800">{doc.fileName}</p>
                      <p className="text-sm text-slate-500">{doc.type}</p>
                      {doc.description && <p className="text-sm text-slate-500">{doc.description}</p>}
                      <p className="text-xs text-slate-400">Uploaded: {formatDualDate(doc.uploadedAt)}</p>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={async () => {
                          try {
                            const token = localStorage.getItem('token');
                            const response = await fetch(`${API_BASE}/documents/download/${doc.id}`, {
                              headers: {
                                'Authorization': `Bearer ${token}`
                              }
                            });
                            
                            if (!response.ok) {
                              throw new Error(`Download failed: ${response.status}`);
                            }
                            
                            const blob = await response.blob();
                            const url = window.URL.createObjectURL(blob);
                            const a = document.createElement('a');
                            a.href = url;
                            a.download = doc.fileName || 'document';
                            document.body.appendChild(a);
                            a.click();
                            a.remove();
                            window.URL.revokeObjectURL(url);
                            
                          } catch (error) {
                            console.error('Download failed:', error);
                            alert('Failed to download document. Please try again.');
                          }
                        }}
                        className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition"
                      >
                        Download
                      </button>
                      <button
                        onClick={() => {
                          if (window.confirm('Delete this document?')) {
                            documentsApi.delete(doc.id).then(() => {
                              loadDocuments(docsTenant.id);
                            });
                          }
                        }}
                        className="p-1.5 text-rose-600 hover:bg-rose-50 rounded-lg transition"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div className="mt-6 flex justify-end">
              <button onClick={() => setDocsTenant(null)} className="px-4 py-2 border border-slate-200 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 transition">
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Document Upload Modal */}
      {showDocUpload && docsTenant && (
        <DocumentUpload
          tenantId={docsTenant.id}
          onUpload={() => loadDocuments(docsTenant.id)}
          onClose={() => setShowDocUpload(false)}
        />
      )}
    </DashboardLayout>
  );
}