import { useState, useMemo } from 'react'
import { mockInvoices, mockLeases, defaultSettings } from '../data/mockData'
import { FaEye, FaPlus, FaMoneyBill } from 'react-icons/fa'
import DashboardLayout from '../components/Layout/DashboardLayout'
import { CheckCircle2, Clock, AlertTriangle, DollarSign, Settings2, Upload, X } from 'lucide-react'

// ────────────────────────────────────────────────────────────────
// Core business logic (SRS 4.5) — kept as pure functions so they
// can move to the Spring Boot backend with the same behaviour.
// ────────────────────────────────────────────────────────────────

/** A payment is late if it lands after the payment window (the 10th). */
export function isLatePayment(paymentDateStr, windowEnd) {
  const day = new Date(paymentDateStr + 'T00:00:00').getDate()
  return day > windowEnd
}

/** Penalty = rent × rate%, tracked separately from rent (SRS 3.3). */
export function calculatePenalty(rentAmount, paymentDateStr, settings) {
  if (!isLatePayment(paymentDateStr, settings.paymentWindowEnd)) return 0
  return Math.round(rentAmount * (settings.penaltyRate / 100))
}

/** Pending invoices past their due date become Overdue (SRS 4.4). */
export function withOverdueStatus(invoice, todayStr) {
  if (invoice.status === 'Pending' && invoice.dueDate < todayStr) {
    const penalty = Math.round(invoice.rentAmount * (defaultSettings.penaltyRate / 100))
    return { ...invoice, status: 'Overdue', penaltyAmount: penalty, totalDue: invoice.rentAmount + penalty }
  }
  return invoice
}

function nextInvoiceNumber(invoices) {
  const year = new Date().getFullYear()
  const seq = String(invoices.length + 1).padStart(4, '0')
  return `INV-${year}-${seq}`
}

function Payments() {
  const todayStr = new Date().toISOString().split('T')[0]

  const [invoices, setInvoices] = useState(() => mockInvoices.map(inv => withOverdueStatus(inv, todayStr)))
  const [settings, setSettings] = useState(defaultSettings)
  const [showSettings, setShowSettings] = useState(false)
  const [payingInvoice, setPayingInvoice] = useState(null)   // invoice being paid
  const [viewingInvoice, setViewingInvoice] = useState(null) // invoice detail modal
  const [statusFilter, setStatusFilter] = useState('All')

  // Payment form state
  const [payForm, setPayForm] = useState({ paymentDate: todayStr, method: 'Bank Transfer', receiptName: '' })

  // ── Derived totals ──
  const totals = useMemo(() => ({
    pending: invoices.filter(i => i.status !== 'Paid').reduce((s, i) => s + i.totalDue, 0),
    collected: invoices.filter(i => i.status === 'Paid').reduce((s, i) => s + i.totalDue, 0),
    outstandingPenalties: invoices.filter(i => i.status !== 'Paid').reduce((s, i) => s + i.penaltyAmount, 0),
    collectedPenalties: invoices.filter(i => i.status === 'Paid').reduce((s, i) => s + i.penaltyAmount, 0),
  }), [invoices])

  const filtered = statusFilter === 'All' ? invoices : invoices.filter(i => i.status === statusFilter)

  // ── Actions ──
  const openPaymentModal = (invoice) => {
    setPayForm({ paymentDate: todayStr, method: 'Bank Transfer', receiptName: '' })
    setPayingInvoice(invoice)
  }

  const handleRecordPayment = () => {
    const penalty = calculatePenalty(payingInvoice.rentAmount, payForm.paymentDate, settings)
    setInvoices(invoices.map(inv =>
      inv.id === payingInvoice.id
        ? {
            ...inv,
            status: 'Paid',
            penaltyAmount: penalty,
            totalDue: inv.rentAmount + penalty,
            paymentMethod: payForm.method,
            paymentDate: payForm.paymentDate,
            receiptName: payForm.receiptName || null,
          }
        : inv
    ))
    setPayingInvoice(null)
  }

  const handleGenerateInvoice = () => {
    const lease = mockLeases.find(l => l.status === 'Active')
    if (!lease) { alert('No active leases available.'); return }
    const issue = new Date()
    const due = new Date(issue.getFullYear(), issue.getMonth(), settings.paymentWindowEnd)
    const newInvoice = {
      id: Math.max(0, ...invoices.map(i => i.id)) + 1,
      invoiceNumber: nextInvoiceNumber(invoices),
      leaseId: lease.id,
      tenant: lease.tenant,
      rentAmount: lease.monthlyRent,
      penaltyAmount: 0,
      totalDue: lease.monthlyRent,
      issueDate: todayStr,
      dueDate: due.toISOString().split('T')[0],
      status: 'Pending',
      paymentMethod: null,
      paymentDate: null,
      receiptName: null,
    }
    setInvoices([newInvoice, ...invoices])
  }

  // ── UI helpers ──
  const statusBadge = {
    Paid: 'bg-emerald-100 text-emerald-700',
    Pending: 'bg-amber-100 text-amber-700',
    Overdue: 'bg-rose-100 text-rose-700',
  }
  const inputClass = "w-full px-4 py-2.5 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition text-sm text-slate-700 bg-slate-50 focus:bg-white"
  const labelClass = "block text-sm font-medium text-slate-700 mb-1.5"

  const previewPenalty = payingInvoice ? calculatePenalty(payingInvoice.rentAmount, payForm.paymentDate, settings) : 0

  return (
    <DashboardLayout>
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">Invoices & Payments</h2>
          <p className="text-slate-500 text-sm mt-1">Record payments, track penalties, and manage invoices</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowSettings(true)}
            className="flex items-center gap-2 px-4 py-2.5 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 font-medium rounded-lg text-sm"
          >
            <Settings2 className="w-4 h-4" /> Penalty: {settings.penaltyRate}%
          </button>
          <button
            onClick={handleGenerateInvoice}
            className="flex items-center gap-2 px-4 py-2.5 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-medium rounded-lg shadow-lg shadow-indigo-500/30 text-sm"
          >
            <FaPlus /> Generate Invoice
          </button>
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {[
          { label: 'Total Invoices', value: invoices.length, icon: DollarSign, iconBg: 'bg-indigo-100 text-indigo-600' },
          { label: 'Amount Pending', value: `ETB ${totals.pending.toLocaleString()}`, icon: Clock, iconBg: 'bg-amber-100 text-amber-600' },
          { label: 'Amount Collected', value: `ETB ${totals.collected.toLocaleString()}`, icon: CheckCircle2, iconBg: 'bg-emerald-100 text-emerald-600' },
          { label: 'Outstanding Penalties', value: `ETB ${totals.outstandingPenalties.toLocaleString()}`, icon: AlertTriangle, iconBg: 'bg-rose-100 text-rose-600' },
        ].map((card) => (
          <div key={card.label} className="bg-white rounded-xl border border-slate-100 p-5 shadow-sm flex items-center justify-between">
            <div>
              <p className="text-sm text-slate-500 font-medium">{card.label}</p>
              <p className="text-2xl font-bold text-slate-800 mt-1">{card.value}</p>
            </div>
            <div className={`w-11 h-11 rounded-xl flex items-center justify-center ${card.iconBg}`}>
              <card.icon className="w-5 h-5" />
            </div>
          </div>
        ))}
      </div>

      {/* Filter tabs */}
      <div className="flex gap-2 mb-4">
        {['All', 'Pending', 'Paid', 'Overdue'].map(f => (
          <button
            key={f}
            onClick={() => setStatusFilter(f)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition
              ${statusFilter === f ? 'bg-indigo-600 text-white shadow' : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'}`}
          >
            {f}
          </button>
        ))}
      </div>

      {/* Invoice table */}
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-slate-500 border-b border-slate-100">
              <th className="px-5 py-3.5 font-medium">Invoice #</th>
              <th className="px-5 py-3.5 font-medium">Tenant</th>
              <th className="px-5 py-3.5 font-medium">Rent</th>
              <th className="px-5 py-3.5 font-medium">Penalty</th>
              <th className="px-5 py-3.5 font-medium">Total Due</th>
              <th className="px-5 py-3.5 font-medium">Due Date</th>
              <th className="px-5 py-3.5 font-medium">Status</th>
              <th className="px-5 py-3.5 font-medium text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(inv => (
              <tr key={inv.id} className="border-b border-slate-50 hover:bg-slate-50/60 transition">
                <td className="px-5 py-4 font-medium text-slate-800">{inv.invoiceNumber}</td>
                <td className="px-5 py-4 text-slate-700">{inv.tenant}</td>
                <td className="px-5 py-4 text-slate-700">ETB {inv.rentAmount.toLocaleString()}</td>
                <td className={`px-5 py-4 font-medium ${inv.penaltyAmount > 0 ? 'text-rose-600' : 'text-slate-400'}`}>
                  {inv.penaltyAmount > 0 ? `ETB ${inv.penaltyAmount.toLocaleString()}` : '—'}
                </td>
                <td className="px-5 py-4 font-semibold text-slate-800">ETB {inv.totalDue.toLocaleString()}</td>
                <td className="px-5 py-4 text-slate-600">{inv.dueDate}</td>
                <td className="px-5 py-4">
                  <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${statusBadge[inv.status]}`}>{inv.status}</span>
                </td>
                <td className="px-5 py-4">
                  <div className="flex items-center justify-end gap-2">
                    <button onClick={() => setViewingInvoice(inv)} title="View invoice"
                      className="p-2 rounded-lg text-slate-500 hover:bg-slate-100 hover:text-indigo-600 transition">
                      <FaEye />
                    </button>
                    {inv.status !== 'Paid' && (
                      <button onClick={() => openPaymentModal(inv)}
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-50 text-emerald-700 hover:bg-emerald-100 font-medium text-xs transition">
                        <FaMoneyBill /> Record Payment
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr><td colSpan={8} className="px-5 py-10 text-center text-slate-400">No invoices match this filter. Generate one to get started.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {/* ── Record Payment modal ── */}
      {payingInvoice && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-1">
              <h3 className="text-lg font-bold text-slate-800">Record Payment</h3>
              <button onClick={() => setPayingInvoice(null)} className="p-1.5 rounded-lg text-slate-400 hover:bg-slate-100"><X className="w-5 h-5" /></button>
            </div>
            <p className="text-sm text-slate-500 mb-5">{payingInvoice.invoiceNumber} · {payingInvoice.tenant}</p>

            <div className="space-y-4">
              <div>
                <label className={labelClass}>Payment date</label>
                <input type="date" value={payForm.paymentDate}
                  onChange={e => setPayForm({ ...payForm, paymentDate: e.target.value })}
                  className={inputClass} />
              </div>
              <div>
                <label className={labelClass}>Payment method</label>
                <select value={payForm.method}
                  onChange={e => setPayForm({ ...payForm, method: e.target.value })}
                  className={inputClass}>
                  <option>Bank Transfer</option>
                  <option>Cash</option>
                  <option>Other</option>
                </select>
              </div>
              <div>
                <label className={labelClass}>Receipt / screenshot</label>
                <label className="flex items-center gap-3 px-4 py-3 border-2 border-dashed border-slate-200 rounded-lg cursor-pointer hover:border-indigo-300 hover:bg-indigo-50/40 transition">
                  <Upload className="w-4 h-4 text-slate-400 shrink-0" />
                  <span className="text-sm text-slate-500 truncate">{payForm.receiptName || 'Upload receipt (PNG, JPG, PDF)'}</span>
                  <input type="file" accept="image/*,.pdf" className="hidden"
                    onChange={e => setPayForm({ ...payForm, receiptName: e.target.files[0]?.name || '' })} />
                </label>
              </div>

              {/* Live penalty preview — the core SRS rule, visible before saving */}
              <div className={`rounded-xl p-4 text-sm ${previewPenalty > 0 ? 'bg-rose-50 border border-rose-100' : 'bg-emerald-50 border border-emerald-100'}`}>
                <div className="flex justify-between text-slate-600">
                  <span>Rent</span><span>ETB {payingInvoice.rentAmount.toLocaleString()}</span>
                </div>
                <div className={`flex justify-between mt-1 ${previewPenalty > 0 ? 'text-rose-600 font-medium' : 'text-slate-400'}`}>
                  <span>Late penalty ({settings.penaltyRate}% after the {settings.paymentWindowEnd}th)</span>
                  <span>{previewPenalty > 0 ? `+ ETB ${previewPenalty.toLocaleString()}` : 'None'}</span>
                </div>
                <div className="flex justify-between mt-2 pt-2 border-t border-slate-200/70 font-bold text-slate-800">
                  <span>Total</span><span>ETB {(payingInvoice.rentAmount + previewPenalty).toLocaleString()}</span>
                </div>
              </div>

              <button onClick={handleRecordPayment}
                className="w-full py-3 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-medium rounded-lg shadow-lg shadow-indigo-500/30">
                Save payment
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Penalty settings modal ── */}
      {showSettings && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-sm p-6">
            <div className="flex items-center justify-between mb-5">
              <h3 className="text-lg font-bold text-slate-800">Penalty Settings</h3>
              <button onClick={() => setShowSettings(false)} className="p-1.5 rounded-lg text-slate-400 hover:bg-slate-100"><X className="w-5 h-5" /></button>
            </div>
            <div className="space-y-4">
              <div>
                <label className={labelClass}>Late penalty rate (%)</label>
                <input type="number" min="0" max="100" value={settings.penaltyRate}
                  onChange={e => setSettings({ ...settings, penaltyRate: Number(e.target.value) || 0 })}
                  className={inputClass} />
                <p className="text-xs text-slate-400 mt-1.5">Applied to rent when payment lands after the {settings.paymentWindowEnd}th of the month.</p>
              </div>
              <button onClick={() => setShowSettings(false)}
                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg">
                Done
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Invoice detail modal ── */}
      {viewingInvoice && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-5">
              <div>
                <h3 className="text-lg font-bold text-slate-800">{viewingInvoice.invoiceNumber}</h3>
                <span className={`inline-block mt-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${statusBadge[viewingInvoice.status]}`}>{viewingInvoice.status}</span>
              </div>
              <button onClick={() => setViewingInvoice(null)} className="p-1.5 rounded-lg text-slate-400 hover:bg-slate-100"><X className="w-5 h-5" /></button>
            </div>
            <div className="space-y-2.5 text-sm">
              {[
                ['Tenant', viewingInvoice.tenant],
                ['Lease ID', `#${viewingInvoice.leaseId}`],
                ['Issue date', viewingInvoice.issueDate],
                ['Due date', viewingInvoice.dueDate],
                ['Rent', `ETB ${viewingInvoice.rentAmount.toLocaleString()}`],
                ['Penalty', viewingInvoice.penaltyAmount > 0 ? `ETB ${viewingInvoice.penaltyAmount.toLocaleString()}` : '—'],
                ['Total due', `ETB ${viewingInvoice.totalDue.toLocaleString()}`],
                ['Payment method', viewingInvoice.paymentMethod || '—'],
                ['Payment date', viewingInvoice.paymentDate || '—'],
                ['Receipt', viewingInvoice.receiptName || '—'],
              ].map(([k, v]) => (
                <div key={k} className="flex justify-between py-1.5 border-b border-slate-50 last:border-0">
                  <span className="text-slate-500">{k}</span>
                  <span className="font-medium text-slate-800">{v}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  )
}

export default Payments

