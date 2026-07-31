import { useState } from 'react'
import { mockTenants } from '../data/mockData'
import DashboardLayout from '../components/Layout/DashboardLayout'
import { FaEdit, FaTrash, FaPlus } from 'react-icons/fa'

function Tenants() {
  const [tenants, setTenants] = useState(mockTenants)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [formData, setFormData] = useState({
    name: '', type: 'Business', contact: '', phone: '', email: '', businessId: ''
  })

  const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value })

  const handleAdd = () => {
    setTenants([...tenants, { id: tenants.length + 1, ...formData }])
    setShowForm(false)
    setFormData({ name: '', type: 'Business', contact: '', phone: '', email: '', businessId: '' })
  }

  const handleDelete = (id) => {
    if (window.confirm('Delete this tenant?')) setTenants(tenants.filter(t => t.id !== id))
  }

  const handleEdit = (tenant) => {
    setEditingId(tenant.id)
    setFormData({
      name: tenant.name, type: tenant.type, contact: tenant.contact || '',
      phone: tenant.phone || '', email: tenant.email || '', businessId: tenant.businessId || ''
    })
    setShowForm(true)
  }

  const handleUpdate = () => {
    setTenants(tenants.map(t => t.id === editingId ? { ...t, ...formData } : t))
    setShowForm(false); setEditingId(null)
    setFormData({ name: '', type: 'Business', contact: '', phone: '', email: '', businessId: '' })
  }

  const handleCancel = () => {
    setShowForm(false); setEditingId(null)
    setFormData({ name: '', type: 'Business', contact: '', phone: '', email: '', businessId: '' })
  }

  const inputClass = "w-full px-4 py-2.5 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition text-sm"
  const labelClass = "block text-sm font-medium text-slate-700 mb-1.5"

  return (
    <DashboardLayout>
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">Tenant Management</h2>
          <p className="text-slate-500 text-sm mt-1">Manage all tenants and their information</p>
        </div>
        <button 
          onClick={() => setShowForm(true)}
          className="flex items-center gap-2 px-4 py-2.5 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-medium rounded-lg shadow-lg shadow-indigo-500/30"
        >
          <FaPlus /> Add New Tenant
        </button>
      </div>

      {showForm && (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-6 mb-6 animate-slide-up">
          <h3 className="text-lg font-semibold text-slate-800 mb-4">
            {editingId ? 'Edit Tenant' : 'Add New Tenant'}
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className={labelClass}>Tenant Name *</label>
              <input type="text" name="name" value={formData.name} onChange={handleChange} className={inputClass} placeholder="e.g. ABC Trading" />
            </div>
            <div>
              <label className={labelClass}>Tenant Type</label>
              <select name="type" value={formData.type} onChange={handleChange} className={inputClass}>
                <option>Business</option>
                <option>Individual</option>
                <option>Organization</option>
              </select>
            </div>
            <div>
              <label className={labelClass}>Contact Person</label>
              <input type="text" name="contact" value={formData.contact} onChange={handleChange} className={inputClass} placeholder="e.g. John Doe" />
            </div>
            <div>
              <label className={labelClass}>Phone Number</label>
              <input type="text" name="phone" value={formData.phone} onChange={handleChange} className={inputClass} placeholder="e.g. 0912345678" />
            </div>
            <div>
              <label className={labelClass}>Email</label>
              <input type="email" name="email" value={formData.email} onChange={handleChange} className={inputClass} placeholder="e.g. info@abc.com" />
            </div>
            <div>
              <label className={labelClass}>Business ID/TIN</label>
              <input type="text" name="businessId" value={formData.businessId} onChange={handleChange} className={inputClass} placeholder="e.g. 123456789" />
            </div>
          </div>
          <div className="flex gap-3 mt-6">
            <button onClick={editingId ? handleUpdate : handleAdd}
              className="px-6 py-2.5 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-medium rounded-lg shadow-lg shadow-indigo-500/30">
              {editingId ? 'Update Tenant' : 'Save Tenant'}
            </button>
            <button onClick={handleCancel}
              className="px-6 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium rounded-lg">
              Cancel
            </button>
          </div>
        </div>
      )}

      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-100">
                {['Name', 'Type', 'Contact', 'Phone', 'Email', 'Business ID', 'Actions'].map(h => (
                  <th key={h} className="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {tenants.length === 0 ? (
                <tr><td colSpan="7" className="px-6 py-8 text-center text-slate-500">No tenants yet.</td></tr>
              ) : tenants.map(tenant => (
                <tr key={tenant.id} className="hover:bg-slate-50 transition-colors">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-full flex items-center justify-center text-white font-semibold text-sm">
                        {tenant.name.charAt(0)}
                      </div>
                      <span className="font-medium text-slate-800">{tenant.name}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`px-3 py-1 text-xs font-semibold rounded-full
                      ${tenant.type === 'Business' ? 'bg-blue-100 text-blue-700' 
                        : tenant.type === 'Individual' ? 'bg-emerald-100 text-emerald-700' 
                        : 'bg-purple-100 text-purple-700'}`}>
                      {tenant.type}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-slate-600">{tenant.contact || '-'}</td>
                  <td className="px-6 py-4 text-slate-600">{tenant.phone || '-'}</td>
                  <td className="px-6 py-4 text-slate-600">{tenant.email || '-'}</td>
                  <td className="px-6 py-4 text-slate-600">{tenant.businessId || '-'}</td>
                  <td className="px-6 py-4">
                    <button onClick={() => handleEdit(tenant)} className="text-blue-500 hover:text-blue-700 mr-3"><FaEdit /></button>
                    <button onClick={() => handleDelete(tenant.id)} className="text-rose-500 hover:text-rose-700"><FaTrash /></button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="px-6 py-3 bg-slate-50 border-t border-slate-100 text-sm text-slate-500">
          Total: {tenants.length} tenant{tenants.length !== 1 ? 's' : ''}
        </div>
      </div>
    </DashboardLayout>
  )
}

export default Tenants