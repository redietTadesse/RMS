import { useState } from 'react'
import { mockInvoices, mockTenants, mockLeases } from '../data/mockData'
import { FaEye, FaCheck, FaPrint, FaMoneyBill, FaPlus } from 'react-icons/fa'
import DashboardLayout from '../components/Layout/DashboardLayout'
import { CheckCircle2, Clock, XCircle, DollarSign } from 'lucide-react'

function Payments() {
  const [invoices, setInvoices] = useState(mockInvoices)
  const [showModal, setShowModal] = useState(false)
  const [selectedInvoice, setSelectedInvoice] = useState(null)

  // Get tenant name for invoice
  const getTenantName = (tenantName) => {
    const tenant = mockTenants.find(t => t.name === tenantName)
    return tenant ? tenant.name : tenantName
  }

  // Mark invoice as paid (manual payment)
  const handleMarkAsPaid = (id) => {
    if (window.confirm('Mark this invoice as paid?')) {
      setInvoices(invoices.map(invoice => 
        invoice.id === id 
          ? { ...invoice, status: 'Paid' }
          : invoice
      ))
    }
  }

  // Generate new invoice
  const handleGenerateInvoice = () => {
    const usedTenants = invoices.map(i => i.tenant)
    const availableLease = mockLeases.find(l => !usedTenants.includes(l.tenant))
    
    if (availableLease) {
      const newInvoice = {
        id: invoices.length + 1,
        tenant: availableLease.tenant,
        amount: availableLease.monthlyRent,
        dueDate: new Date().toISOString().split('T')[0],
        status: 'Pending'
      }
      setInvoices([...invoices, newInvoice])
      alert('Invoice generated successfully!')
    } else {
      alert('No active leases available for invoice generation.')
    }
  }

  // View invoice details (modal)
  const handleViewInvoice = (invoice) => {
    setSelectedInvoice(invoice)
    setShowModal(true)
  }

  // Get status badge color
  const getStatusBadge = (status) => {
    const styles = {
      Paid: 'bg-emerald-100 text-emerald-700',
      Pending: 'bg-amber-100 text-amber-700',
      Overdue: 'bg-rose-100 text-rose-700'
    }
    return styles[status] || 'bg-slate-100 text-slate-700'
  }

  // Calculate totals
  const totalPending = invoices
    .filter(i => i.status === 'Pending' || i.status === 'Overdue')
    .reduce((sum, i) => sum + i.amount, 0)

  const totalPaid = invoices
    .filter(i => i.status === 'Paid')
    .reduce((sum, i) => sum + i.amount, 0)

  return (
    <DashboardLayout>
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">Payment Management</h2>
          <p className="text-slate-500 text-sm mt-1">Track all invoices and payments</p>
        </div>
        <button 
          onClick={handleGenerateInvoice}
          className="flex items-center gap-2 px-4 py-2.5 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-medium rounded-lg shadow-lg shadow-indigo-500/30"
        >
          <FaPlus /> Generate Invoice
        </button>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div className="bg-white rounded-xl border border-slate-100 p-5 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm text-slate-500 font-medium">Total Invoices</p>
            <p className="text-2xl font-bold text-slate-800 mt-1">{invoices.length}</p>
          </div>
          <div className="w-11 h-11 bg-indigo-100 rounded-xl flex items-center justify-center">
            <DollarSign className="w-5 h-5 text-indigo-600" />
          </div>
        </div>
        <div className="bg-white rounded-xl border border-slate-100 p-5 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm text-slate-500 font-medium">Collected</p>
            <p className="text-2xl font-bold text-emerald-600 mt-1">ETB {totalPaid.toLocaleString()}</p>
          </div>
          <div className="w-11 h-11 bg-emerald-100 rounded-xl flex items-center justify-center">
            <CheckCircle2 className="w-5 h-5 text-emerald-600" />
          </div>
        </div>
        <div className="bg-white rounded-xl border border-slate-100 p-5 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm text-slate-500 font-medium">Pending</p>
            <p className="text-2xl font-bold text-amber-600 mt-1">ETB {totalPending.toLocaleString()}</p>
          </div>
          <div className="w-11 h-11 bg-amber-100 rounded-xl flex items-center justify-center">
            <Clock className="w-5 h-5 text-amber-600" />
          </div>
        </div>
        <div className="bg-white rounded-xl border border-slate-100 p-5 shadow-sm flex items-center justify-between">
          <div>
            <p className="text-sm text-slate-500 font-medium">Overdue</p>
            <p className="text-2xl font-bold text-rose-600 mt-1">{invoices.filter(i => i.status === 'Overdue').length}</p>
          </div>
          <div className="w-11 h-11 bg-rose-100 rounded-xl flex items-center justify-center">
            <XCircle className="w-5 h-5 text-rose-600" />
          </div>
        </div>
      </div>

      {/* Invoice Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-100">
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Invoice #</th>
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Tenant</th>
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Amount</th>
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Due Date</th>
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Status</th>
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {invoices.length === 0 ? (
                <tr>
                  <td colSpan="6" className="px-6 py-8 text-center text-slate-500">
                    No invoices yet. Click "Generate Invoice" to create one.
                  </td>
                </tr>
              ) : (
                invoices.map(invoice => (
                  <tr key={invoice.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-4 font-medium text-slate-800">
                      INV-{String(invoice.id).padStart(4, '0')}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-full flex items-center justify-center text-white font-semibold text-sm">
                          {invoice.tenant.charAt(0)}
                        </div>
                        <span className="text-slate-700">{getTenantName(invoice.tenant)}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 font-semibold text-slate-800">
                      ETB {invoice.amount.toLocaleString()}
                    </td>
                    <td className="px-6 py-4 text-slate-600">{invoice.dueDate}</td>
                    <td className="px-6 py-4">
                      <span className={`px-3 py-1 text-xs font-semibold rounded-full ${getStatusBadge(invoice.status)}`}>
                        {invoice.status}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => handleViewInvoice(invoice)}
                          className="p-2 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors"
                          title="View"
                        >
                          <FaEye />
                        </button>
                        {invoice.status !== 'Paid' && (
                          <button
                            onClick={() => handleMarkAsPaid(invoice.id)}
                            className="p-2 text-slate-500 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition-colors"
                            title="Mark as Paid"
                          >
                            <FaCheck />
                          </button>
                        )}
                        <button
                          className="p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors"
                          title="Print"
                        >
                          <FaPrint />
                        </button>
                        <button
                          className="p-2 text-slate-500 hover:text-purple-600 hover:bg-purple-50 rounded-lg transition-colors"
                          title="Record Payment"
                        >
                          <FaMoneyBill />
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
        <div className="px-6 py-3 bg-slate-50 border-t border-slate-100 flex flex-col sm:flex-row justify-between items-center gap-2 text-sm text-slate-500">
          <span>Total: {invoices.length} invoice{invoices.length !== 1 ? 's' : ''}</span>
          <div className="flex gap-4">
            <span>Paid: <span className="font-medium text-emerald-600">{invoices.filter(i => i.status === 'Paid').length}</span></span>
            <span>Pending: <span className="font-medium text-amber-600">{invoices.filter(i => i.status === 'Pending').length}</span></span>
            <span>Overdue: <span className="font-medium text-rose-600">{invoices.filter(i => i.status === 'Overdue').length}</span></span>
          </div>
        </div>
      </div>

      {/* Invoice Details Modal */}
      {showModal && selectedInvoice && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-fade-in">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl animate-slide-up">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-bold text-slate-800">Invoice Details</h3>
              <button
                onClick={() => setShowModal(false)}
                className="text-slate-400 hover:text-slate-600 text-2xl leading-none p-1 rounded-full hover:bg-slate-100"
              >
                &times;
              </button>
            </div>
            
            <div className="space-y-4">
              <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                <span className="text-slate-500 text-sm">Invoice #</span>
                <span className="font-medium text-slate-800">INV-{String(selectedInvoice.id).padStart(4, '0')}</span>
              </div>
              <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                <span className="text-slate-500 text-sm">Tenant</span>
                <span className="font-medium text-slate-800">{getTenantName(selectedInvoice.tenant)}</span>
              </div>
              <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                <span className="text-slate-500 text-sm">Amount</span>
                <span className="font-bold text-lg text-slate-900">ETB {selectedInvoice.amount.toLocaleString()}</span>
              </div>
              <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                <span className="text-slate-500 text-sm">Due Date</span>
                <span className="font-medium text-slate-800">{selectedInvoice.dueDate}</span>
              </div>
              <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                <span className="text-slate-500 text-sm">Status</span>
                <span className={`px-3 py-1 text-xs font-semibold rounded-full ${getStatusBadge(selectedInvoice.status)}`}>
                  {selectedInvoice.status}
                </span>
              </div>
              <div className="flex justify-between items-center pb-3">
                <span className="text-slate-500 text-sm">Payment Method</span>
                <span className="font-medium text-slate-800">
                  {selectedInvoice.status === 'Paid' ? 'Chapa / Bank Transfer' : 'Pending'}
                </span>
              </div>
            </div>

            <div className="flex gap-3 mt-8">
              {selectedInvoice.status !== 'Paid' && (
                <button
                  onClick={() => {
                    handleMarkAsPaid(selectedInvoice.id)
                    setShowModal(false)
                  }}
                  className="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700 text-white font-medium rounded-lg shadow-lg shadow-emerald-500/30"
                >
                  <FaCheck /> Mark as Paid
                </button>
              )}
              <button
                onClick={() => setShowModal(false)}
                className="flex-1 px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium rounded-lg transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  )
}

export default Payments