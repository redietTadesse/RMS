import { useEffect, useState } from 'react'
import { spacesApi } from '../services/api'

const inputClass =
  'w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500'
const labelClass = 'mb-1 block text-sm font-medium text-gray-700'

const EMPTY_FORM = {
  unitNumber: '',
  floor: '',
  category: '',
  size: '',
  status: 'VACANT',
}

const CATEGORIES = ['Shop', 'Office', 'Cafe', 'Restaurant', 'Warehouse', 'Other']

function StatusBadge({ status }) {
  const styles =
    status === 'VACANT'
      ? 'bg-green-100 text-green-700'
      : 'bg-blue-100 text-blue-700'
  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${styles}`}>
      {status === 'VACANT' ? 'Vacant' : 'Occupied'}
    </span>
  )
}

export default function Spaces() {
  const [spaces, setSpaces] = useState([])
  const [filter, setFilter] = useState('') // '', 'VACANT', 'OCCUPIED'
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // modal state
  const [showModal, setShowModal] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [formData, setFormData] = useState(EMPTY_FORM)
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  const loadSpaces = async (status = filter) => {
    setLoading(true)
    setError('')
    try {
      const data = await spacesApi.getAll(status || undefined)
      setSpaces(data)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadSpaces()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter])

  const openAdd = () => {
    setEditingId(null)
    setFormData(EMPTY_FORM)
    setFormError('')
    setShowModal(true)
  }

  const openEdit = (space) => {
    setEditingId(space.id)
    setFormData({
      unitNumber: space.unitNumber || '',
      floor: space.floor?.toString() ?? '',
      category: space.category || '',
      size: space.size?.toString() ?? '',
      status: space.status || 'VACANT',
    })
    setFormError('')
    setShowModal(true)
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData((f) => ({ ...f, [name]: value }))
  }

  const handleSave = async () => {
    if (!formData.unitNumber.trim() || formData.floor === '' || !formData.category || formData.size === '') {
      setFormError('Please fill in all required fields.')
      return
    }
    const payload = {
      unitNumber: formData.unitNumber.trim(),
      floor: parseInt(formData.floor, 10),
      category: formData.category,
      size: parseFloat(formData.size),
      status: formData.status,
    }
    setSaving(true)
    setFormError('')
    try {
      if (editingId) {
        await spacesApi.update(editingId, payload)
      } else {
        await spacesApi.create(payload)
      }
      setShowModal(false)
      await loadSpaces()
    } catch (e) {
      setFormError(e.message)
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (space) => {
    if (!window.confirm(`Delete space ${space.unitNumber}? This cannot be undone.`)) return
    try {
      await spacesApi.remove(space.id)
      await loadSpaces()
    } catch (e) {
      setError(e.message)
    }
  }

  const filterBtn = (value, label) => (
    <button
      key={value}
      onClick={() => setFilter(value)}
      className={`rounded-lg px-3 py-1.5 text-sm font-medium transition-colors ${
        filter === value
          ? 'bg-blue-600 text-white'
          : 'bg-white text-gray-600 border border-gray-300 hover:bg-gray-50'
      }`}
    >
      {label}
    </button>
  )

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Spaces</h1>
          <p className="text-sm text-gray-500">Manage the building's rentable units</p>
        </div>
        <button
          onClick={openAdd}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          + Add Space
        </button>
      </div>

      {/* Filter */}
      <div className="mb-4 flex gap-2">
        {filterBtn('', 'All')}
        {filterBtn('VACANT', 'Vacant')}
        {filterBtn('OCCUPIED', 'Occupied')}
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error} — is the backend running on port 8080?
        </div>
      )}

      {/* Table */}
      <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white">
        <table className="min-w-full divide-y divide-gray-200 text-sm">
          <thead className="bg-gray-50">
            <tr>
              {['Unit', 'Floor', 'Category', 'Size (m²)', 'Status', 'Actions'].map((h) => (
                <th
                  key={h}
                  className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-gray-500"
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {loading ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-gray-400">
                  Loading spaces…
                </td>
              </tr>
            ) : spaces.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-gray-400">
                  No spaces{filter ? ` with status ${filter.toLowerCase()}` : ''} yet. Add the first one.
                </td>
              </tr>
            ) : (
              spaces.map((space) => (
                <tr key={space.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">{space.unitNumber}</td>
                  <td className="px-4 py-3 text-gray-600">{space.floor}</td>
                  <td className="px-4 py-3 text-gray-600">{space.category}</td>
                  <td className="px-4 py-3 text-gray-600">{space.size}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={space.status} />
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2">
                      <button
                        onClick={() => openEdit(space)}
                        className="rounded-md px-2 py-1 text-xs font-medium text-blue-600 hover:bg-blue-50"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(space)}
                        className="rounded-md px-2 py-1 text-xs font-medium text-red-600 hover:bg-red-50"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Add / Edit modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl">
            <h2 className="mb-4 text-lg font-semibold text-gray-900">
              {editingId ? 'Edit Space' : 'Add Space'}
            </h2>

            {formError && (
              <div className="mb-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
                {formError}
              </div>
            )}

            <div className="space-y-4">
              <div>
                <label className={labelClass}>Unit Number *</label>
                <input
                  name="unitNumber"
                  value={formData.unitNumber}
                  onChange={handleChange}
                  className={inputClass}
                  placeholder="e.g. G-01"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className={labelClass}>Floor *</label>
                  <input
                    type="number"
                    name="floor"
                    value={formData.floor}
                    onChange={handleChange}
                    className={inputClass}
                    placeholder="0 = ground"
                  />
                </div>
                <div>
                  <label className={labelClass}>Size (m²) *</label>
                  <input
                    type="number"
                    step="0.1"
                    min="0"
                    name="size"
                    value={formData.size}
                    onChange={handleChange}
                    className={inputClass}
                    placeholder="e.g. 45.5"
                  />
                </div>
              </div>
              <div>
                <label className={labelClass}>Category *</label>
                <select
                  name="category"
                  value={formData.category}
                  onChange={handleChange}
                  className={inputClass}
                >
                  <option value="">Select category…</option>
                  {CATEGORIES.map((c) => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className={labelClass}>Status *</label>
                <select
                  name="status"
                  value={formData.status}
                  onChange={handleChange}
                  className={inputClass}
                >
                  <option value="VACANT">Vacant</option>
                  <option value="OCCUPIED">Occupied</option>
                </select>
              </div>
            </div>

            <div className="mt-6 flex justify-end gap-3">
              <button
                onClick={() => setShowModal(false)}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
                disabled={saving}
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-60"
                disabled={saving}
              >
                {saving ? 'Saving…' : editingId ? 'Save Changes' : 'Add Space'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
