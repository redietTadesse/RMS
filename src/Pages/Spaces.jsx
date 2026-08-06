// src/pages/Spaces.jsx - COMPLETE FILE
import { useEffect, useState } from 'react';
import { spacesApi } from '../services/api';
import DashboardLayout from '../components/Layout/DashboardLayout';
import { Loader2, Plus, Edit, Trash2 } from 'lucide-react';

const inputClass = 'w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 bg-slate-50 focus:bg-white transition';
const labelClass = 'mb-1 block text-sm font-medium text-slate-700';

const EMPTY_FORM = { unitNumber: '', floor: '', category: '', size: '', status: 'VACANT' };
const CATEGORIES = ['Shop', 'Office', 'Cafe', 'Restaurant', 'Warehouse', 'Other'];

function StatusBadge({ status }) {
  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${status === 'VACANT' ? 'bg-emerald-100 text-emerald-700' : 'bg-blue-100 text-blue-700'}`}>
      {status === 'VACANT' ? 'Vacant' : 'Occupied'}
    </span>
  );
}

export default function Spaces() {
  const [spaces, setSpaces] = useState([]);
  const [filter, setFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const loadSpaces = async (status = filter) => {
    setLoading(true);
    setError('');
    try {
      const data = await spacesApi.getAll(status || undefined);
      setSpaces(data);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadSpaces(); }, [filter]);

  const openAdd = () => { setEditingId(null); setFormData(EMPTY_FORM); setFormError(''); setShowModal(true); };
  const openEdit = (space) => {
    setEditingId(space.id);
    setFormData({
      unitNumber: space.unitNumber || '',
      floor: space.floor?.toString() ?? '',
      category: space.category || '',
      size: space.size?.toString() ?? '',
      status: space.status || 'VACANT',
    });
    setFormError('');
    setShowModal(true);
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((f) => ({ ...f, [name]: value }));
  };

  const handleSave = async () => {
    if (!formData.unitNumber.trim() || formData.floor === '' || !formData.category || formData.size === '') {
      setFormError('Please fill in all required fields.');
      return;
    }
    const payload = {
      unitNumber: formData.unitNumber.trim(),
      floor: parseInt(formData.floor, 10),
      category: formData.category,
      size: parseFloat(formData.size),
      status: formData.status,
    };
    setSaving(true);
    setFormError('');
    try {
      if (editingId) await spacesApi.update(editingId, payload);
      else await spacesApi.create(payload);
      setShowModal(false);
      await loadSpaces();
    } catch (e) {
      setFormError(e.message);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (space) => {
    if (!window.confirm(`Delete space ${space.unitNumber}?`)) return;
    try {
      await spacesApi.remove(space.id);
      await loadSpaces();
    } catch (e) {
      setError(e.message);
    }
  };

  const filterBtn = (value, label) => (
    <button
      key={value}
      onClick={() => setFilter(value)}
      className={`rounded-lg px-4 py-1.5 text-sm font-medium transition ${
        filter === value ? 'bg-indigo-600 text-white shadow-sm' : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
      }`}
    >
      {label}
    </button>
  );

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Spaces</h1>
            <p className="text-sm text-slate-500">Manage the building's rentable units</p>
          </div>
          <button onClick={openAdd} className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg shadow-sm transition">
            <Plus className="w-4 h-4" /> Add Space
          </button>
        </div>

        <div className="flex gap-2 flex-wrap">
          {filterBtn('', 'All')}
          {filterBtn('VACANT', 'Vacant')}
          {filterBtn('OCCUPIED', 'Occupied')}
        </div>

        {error && (
          <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 border border-red-200">
            {error}
          </div>
        )}

        <div className="bg-white rounded-xl border border-slate-100 shadow-sm overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-100">
              <tr>
                {['Unit', 'Floor', 'Category', 'Size (m²)', 'Status', 'Actions'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr><td colSpan={6} className="px-4 py-8 text-center text-slate-400"><Loader2 className="w-5 h-5 animate-spin inline mr-2" /> Loading…</td></tr>
              ) : spaces.length === 0 ? (
                <tr><td colSpan={6} className="px-4 py-8 text-center text-slate-400">No spaces yet.</td></tr>
              ) : (
                spaces.map((space) => (
                  <tr key={space.id} className="hover:bg-slate-50/60 transition">
                    <td className="px-4 py-3 font-medium text-slate-800">{space.unitNumber}</td>
                    <td className="px-4 py-3 text-slate-600">{space.floor}</td>
                    <td className="px-4 py-3 text-slate-600">{space.category}</td>
                    <td className="px-4 py-3 text-slate-600">{space.size}</td>
                    <td className="px-4 py-3"><StatusBadge status={space.status} /></td>
                    <td className="px-4 py-3">
                      <div className="flex gap-1.5">
                        <button onClick={() => openEdit(space)} className="p-1.5 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition"><Edit className="w-4 h-4" /></button>
                        <button onClick={() => handleDelete(space)} className="p-1.5 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"><Trash2 className="w-4 h-4" /></button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-md bg-white rounded-xl shadow-xl p-6">
            <h2 className="mb-4 text-lg font-semibold text-slate-800">{editingId ? 'Edit Space' : 'Add Space'}</h2>
            {formError && <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 border border-red-200">{formError}</div>}
            <div className="space-y-4">
              <div><label className={labelClass}>Unit Number *</label><input name="unitNumber" value={formData.unitNumber} onChange={handleChange} className={inputClass} placeholder="e.g. G-01" /></div>
              <div className="grid grid-cols-2 gap-4">
                <div><label className={labelClass}>Floor *</label><input type="number" name="floor" value={formData.floor} onChange={handleChange} className={inputClass} placeholder="0" /></div>
                <div><label className={labelClass}>Size (m²) *</label><input type="number" step="0.1" min="0" name="size" value={formData.size} onChange={handleChange} className={inputClass} placeholder="45.5" /></div>
              </div>
              <div><label className={labelClass}>Category *</label><select name="category" value={formData.category} onChange={handleChange} className={inputClass}><option value="">Select…</option>{CATEGORIES.map(c => <option key={c} value={c}>{c}</option>)}</select></div>
              <div><label className={labelClass}>Status *</label><select name="status" value={formData.status} onChange={handleChange} className={inputClass}><option value="VACANT">Vacant</option><option value="OCCUPIED">Occupied</option></select></div>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button onClick={() => setShowModal(false)} className="px-4 py-2 border border-slate-200 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 transition" disabled={saving}>Cancel</button>
              <button onClick={handleSave} className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg shadow-sm transition disabled:opacity-60" disabled={saving}>
                {saving ? 'Saving…' : editingId ? 'Save Changes' : 'Add Space'}
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}