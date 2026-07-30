import { useState } from 'react'
import { mockLeases, mockTenants, mockSpaces } from '../data/mockData'
import { FaEdit, FaTrash, FaPlus, FaFileInvoice } from 'react-icons/fa'
import DashboardLayout from '../components/Layout/DashboardLayout'

function Leases() {
  const [leases, setLeases] = useState(mockLeases)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [formData, setFormData] = useState({
    tenant: '',
    space: '',
    startDate: '',
    endDate: '',
    monthlyRent: '',
    deposit: '',
    prepaymentMonths: '3',
    dueDay: '1',
    status: 'Active'
  })

  const allSpaces = mockSpaces

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    })
  }

  const handleAdd = () => {
    const newLease = {
      id: leases.length + 1,
      ...formData,
      monthlyRent: parseFloat(formData.monthlyRent),
      deposit: parseFloat(formData.deposit) || 0,
      prepaymentMonths: parseInt(formData.prepaymentMonths) || 3
    }
    if (newLease.prepaymentMonths < 3) {
      alert('Prepayment must be at least 3 months (business rule).')
      return
    }
    setLeases([...leases, newLease])
    
    const spaceToUpdate = allSpaces.find(s => s.unitNumber === formData.space)
    if (spaceToUpdate) {
      spaceToUpdate.status = 'Occupied'
    }
    
    setShowForm(false)
    resetForm()
  }

  const handleDelete = (id) => {
    if (window.confirm('Are you sure you want to delete this lease?')) {
      const leaseToDelete = leases.find(l => l.id === id)
      const spaceToUpdate = allSpaces.find(s => s.unitNumber === leaseToDelete?.space)
      if (spaceToUpdate) {
        spaceToUpdate.status = 'Vacant'
      }
      setLeases(leases.filter(lease => lease.id !== id))
    }
  }

  const handleEdit = (lease) => {
    setEditingId(lease.id)
    setFormData({
      tenant: lease.tenant,
      space: lease.space,
      startDate: lease.startDate,
      endDate: lease.endDate,
      monthlyRent: lease.monthlyRent.toString(),
      deposit: lease.deposit?.toString() || '',
      prepaymentMonths: lease.prepaymentMonths?.toString() || '3',
      dueDay: lease.dueDay?.toString() || '1',
      status: lease.status
    })
    setShowForm(true)
  }

  const handleUpdate = () => {
    setLeases(leases.map(lease => 
      lease.id === editingId 
        ? { 
            ...lease, 
            ...formData,
            monthlyRent: parseFloat(formData.monthlyRent),
            deposit: parseFloat(formData.deposit) || 0
          }
        : lease
    ))
    setShowForm(false)
    setEditingId(null)
    resetForm()
  }

  const resetForm = () => {
    setFormData({
      tenant: '',
      space: '',
      startDate: '',
      endDate: '',
      monthlyRent: '',
      deposit: '',
      dueDay: '1',
      status: 'Active'
    })
  }

  const handleCancel = () => {
    setShowForm(false)
    setEditingId(null)
    resetForm()
  }

  // Reusable input classes for consistency
  const inputClass = "w-full px-4 py-2.5 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition text-sm text-slate-700 bg-slate-50 focus:bg-white"
  const labelClass = "block text-sm font-medium text-slate-700 mb-1.5"

  return (
    <DashboardLayout>
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">Lease Management</h2>
          <p className="text-slate-500 text-sm mt-1">Manage all lease agreements</p>
        </div>
        <button 
          onClick={() => setShowForm(true)}
          className="flex items-center gap-2 px-4 py-2.5 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-medium rounded-lg shadow-lg shadow-indigo-500/30"
        >
          <FaPlus /> Create New Lease
        </button>
      </div>

      {/* Add/Edit Form */}
      {showForm && (
        <div className="bg-white rounded-2xl shadow-sm border border-slate-100 p-6 mb-6 animate-slide-up">
          <h3 className="text-lg font-semibold text-slate-800 mb-4">
            {editingId ? 'Edit Lease' : 'Create New Lease'}
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className={labelClass}>Tenant *</label>
              <select
                name="tenant"
                value={formData.tenant}
                onChange={handleChange}
                className={inputClass}
              >
                <option value="">Select Tenant</option>
                {mockTenants.map(tenant => (
                  <option key={tenant.id} value={tenant.name}>
                    {tenant.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className={labelClass}>Space *</label>
              <select
                name="space"
                value={formData.space}
                onChange={handleChange}
                className={inputClass}
              >
                <option value="">Select Space</option>
                {allSpaces.map(space => (
                  <option key={space.id} value={space.unitNumber}>
                    {space.unitNumber} ({space.status})
                  </option>
                ))}
              </select>
              {formData.space && (
                <p className={`text-xs mt-1.5 font-medium ${allSpaces.find(s => s.unitNumber === formData.space)?.status === 'Occupied' ? 'text-amber-600' : 'text-emerald-600'}`}>
                  {allSpaces.find(s => s.unitNumber === formData.space)?.status === 'Occupied' 
                    ? '⚠️ This space is currently occupied' 
                    : '✅ This space is available'}
                </p>
              )}
            </div>
            <div>
              <label className={labelClass}>Start Date *</label>
              <input
                type="date"
                name="startDate"
                value={formData.startDate}
                onChange={handleChange}
                className={inputClass}
              />
            </div>
            <div>
              <label className={labelClass}>End Date *</label>
              <input
                type="date"
                name="endDate"
                value={formData.endDate}
                onChange={handleChange}
                className={inputClass}
              />
            </div>
            <div>
              <label className={labelClass}>Monthly Rent (ETB) *</label>
              <input
                type="number"
                name="monthlyRent"
                value={formData.monthlyRent}
                onChange={handleChange}
                className={inputClass}
                placeholder="e.g. 15000"
              />
            </div>
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
            <div>
              <label className={labelClass}>Prepayment (months, min 3) *</label>
              <input
                type="number"
                name="prepaymentMonths"
                min="3"
                value={formData.prepaymentMonths}
                onChange={handleChange}
                className={inputClass}
                placeholder="e.g. 3"
              />
            </div>
            <div>
              <label className={labelClass}>Payment Due Day</label>
              <select
                name="dueDay"
                value={formData.dueDay}
                onChange={handleChange}
                className={inputClass}
              >
                {[...Array(31)].map((_, i) => (
                  <option key={i + 1} value={i + 1}>
                    {i + 1}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className={labelClass}>Status</label>
              <select
                name="status"
                value={formData.status}
                onChange={handleChange}
                className={inputClass}
              >
                <option value="Active">Active</option>
                <option value="Expired">Expired</option>
                <option value="Terminated">Terminated</option>
              </select>
            </div>
          </div>
          <div className="flex gap-3 mt-6">
            <button
              onClick={editingId ? handleUpdate : handleAdd}
              className="px-6 py-2.5 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-medium rounded-lg shadow-lg shadow-indigo-500/30"
            >
              {editingId ? 'Update Lease' : 'Create Lease'}
            </button>
            <button
              onClick={handleCancel}
              className="px-6 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium rounded-lg transition-colors"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* Lease Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-100">
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Tenant</th>
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Space</th>
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Start Date</th>
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">End Date</th>
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Monthly Rent</th>
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Status</th>
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {leases.length === 0 ? (
                <tr>
                  <td colSpan="7" className="px-6 py-8 text-center text-slate-500">
                    No leases created yet. Click "Create New Lease" to get started.
                  </td>
                </tr>
              ) : (
                leases.map(lease => (
                  <tr key={lease.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-full flex items-center justify-center text-white font-semibold text-sm">
                          {lease.tenant.charAt(0)}
                        </div>
                        <span className="font-medium text-slate-800">{lease.tenant}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-slate-600 font-medium">{lease.space}</td>
                    <td className="px-6 py-4 text-slate-600">{lease.startDate}</td>
                    <td className="px-6 py-4 text-slate-600">{lease.endDate}</td>
                    <td className="px-6 py-4 font-semibold text-slate-800">
                      ETB {lease.monthlyRent.toLocaleString()}
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-3 py-1 text-xs font-semibold rounded-full ${
                        lease.status === 'Active' 
                          ? 'bg-emerald-100 text-emerald-700'
                          : lease.status === 'Expired'
                          ? 'bg-rose-100 text-rose-700'
                          : 'bg-slate-100 text-slate-700'
                      }`}>
                        {lease.status}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => handleEdit(lease)}
                          className="p-2 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors"
                          title="Edit"
                        >
                          <FaEdit />
                        </button>
                        <button
                          onClick={() => handleDelete(lease.id)}
                          className="p-2 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                          title="Delete"
                        >
                          <FaTrash />
                        </button>
                        <button
                          className="p-2 text-slate-500 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition-colors"
                          title="Generate Invoice"
                        >
                          <FaFileInvoice />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        {/* Table Footer */}
        <div className="px-6 py-3 bg-slate-50 border-t border-slate-100 text-sm text-slate-500">
          Total: {leases.length} lease{leases.length !== 1 ? 's' : ''}
        </div>
      </div>
    </DashboardLayout>
  )
}

export default Leases