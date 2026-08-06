// src/pages/Payments.jsx
import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  FaEye, FaPlus, FaMoneyBill, FaTrash, FaEdit 
} from 'react-icons/fa';
import { 
  CheckCircle2, Clock, AlertTriangle, DollarSign, 
  Settings2, Upload, X, Loader2 
} from 'lucide-react';
import DashboardLayout from '../components/Layout/DashboardLayout';
import { invoicesApi, paymentsApi, leasesApi } from '../services/api';
import { useAuth } from '../context/AuthContext';

// ── Status Badge Colors ──
const statusBadge = {
  PENDING: 'bg-amber-100 text-amber-700',
  PAID: 'bg-emerald-100 text-emerald-700',
  OVERDUE: 'bg-rose-100 text-rose-700',
};

export default function Payments() {
  const navigate = useNavigate();
  const { token } = useAuth();
  const todayStr = new Date().toISOString().split('T')[0];

  // ── State ──
  const [invoices, setInvoices] = useState([]);
  const [leases, setLeases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');
  const [settings, setSettings] = useState({
    penaltyRate: 20,
    paymentWindowEnd: 10,
  });
  const [showSettings, setShowSettings] = useState(false);

  // ── Payment Modal State ──
  const [payingInvoice, setPayingInvoice] = useState(null);
  const [payForm, setPayForm] = useState({
    paymentDate: todayStr,
    paymentMethod: 'BANK_TRANSFER',
    receipt: null,
    receiptName: '',
  });
  const [penaltyPreview, setPenaltyPreview] = useState(0);
  const [submitting, setSubmitting] = useState(false);

  // ── View Invoice Modal ──
  const [viewingInvoice, setViewingInvoice] = useState(null);

  // ── Generate Invoice ──
  const [generating, setGenerating] = useState(false);

  // ── Load Data ──
  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [invoicesData, leasesData] = await Promise.all([
        invoicesApi.getAll(),
        leasesApi.getAll(),
      ]);
      setInvoices(invoicesData);
      setLeases(leasesData);
    } catch (err) {
      setError(err.message || 'Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // ── Calculate Totals ──
  const totals = useMemo(() => ({
    total: invoices.length,
    pending: invoices.filter(i => i.status === 'PENDING').reduce((s, i) => s + (i.totalAmount || 0), 0),
    collected: invoices.filter(i => i.status === 'PAID').reduce((s, i) => s + (i.totalAmount || 0), 0),
    overdue: invoices.filter(i => i.status === 'OVERDUE').length,
    outstandingPenalties: invoices.filter(i => i.status !== 'PAID').reduce((s, i) => s + (i.penaltyAmount || 0), 0),
    totalPenalties: invoices.filter(i => i.status === 'PAID').reduce((s, i) => s + (i.penaltyAmount || 0), 0),
  }), [invoices]);

  // ── Filtered Invoices ──
  const filteredInvoices = useMemo(() => {
    if (statusFilter === 'All') return invoices;
    return invoices.filter(i => i.status === statusFilter);
  }, [invoices, statusFilter]);

  // ── Open Payment Modal ──
  const openPaymentModal = (invoice) => {
    setPayingInvoice(invoice);
    setPayForm({
      paymentDate: todayStr,
      paymentMethod: 'BANK_TRANSFER',
      receipt: null,
      receiptName: '',
    });
    setPenaltyPreview(0);
  };

  // ── Handle Payment Date Change (for penalty preview) ──
  const handlePaymentDateChange = async (date) => {
    setPayForm({ ...payForm, paymentDate: date });
    if (payingInvoice) {
      try {
        const penalty = await paymentsApi.previewPenalty(payingInvoice.id, date);
        setPenaltyPreview(penalty || 0);
      } catch (err) {
        console.error('Failed to preview penalty:', err);
        setPenaltyPreview(0);
      }
    }
  };

  // ── Submit Payment ──
  const handleRecordPayment = async () => {
    if (!payingInvoice) return;
    
    setSubmitting(true);
    try {
      await paymentsApi.record(
        payingInvoice.id,
        {
          paymentDate: payForm.paymentDate,
          paymentMethod: payForm.paymentMethod,
        },
        payForm.receipt
      );
      setPayingInvoice(null);
      await loadData();
    } catch (err) {
      setError(err.message || 'Failed to record payment');
    } finally {
      setSubmitting(false);
    }
  };

  // ── Generate Invoice ──
  const handleGenerateInvoice = async () => {
    const activeLease = leases.find(l => l.status === 'ACTIVE');
    if (!activeLease) {
      setError('No active leases available to generate invoice');
      return;
    }
    
    setGenerating(true);
    try {
      await invoicesApi.generate(activeLease.id);
      await loadData();
    } catch (err) {
      setError(err.message || 'Failed to generate invoice');
    } finally {
      setGenerating(false);
    }
  };

  // ── Get Tenant Name from Lease ──
  const getTenantName = (invoice) => {
    if (invoice.lease?.tenant?.name) return invoice.lease.tenant.name;
    return invoice.leaseId ? `Lease #${invoice.leaseId}` : 'Unknown';
  };

  // ── Loading State ──
  if (loading) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center h-96">
          <Loader2 className="w-8 h-8 animate-spin text-indigo-600" />
          <span className="ml-3 text-slate-500">Loading invoices...</span>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h2 className="text-2xl font-bold text-slate-800">Invoices & Payments</h2>
            <p className="text-slate-500 text-sm mt-1">
              Manage invoices, record payments, and track penalties
            </p>
          </div>
          <div className="flex items-center gap-3 flex-wrap">
            <button
              onClick={() => setShowSettings(true)}
              className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 font-medium rounded-lg text-sm transition"
            >
              <Settings2 className="w-4 h-4" />
              Penalty: {settings.penaltyRate}%
            </button>
            <button
              onClick={handleGenerateInvoice}
              disabled={generating}
              className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg shadow-sm transition disabled:opacity-50"
            >
              {generating ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <FaPlus />
              )}
              Generate Invoice
            </button>
          </div>
        </div>

        {/* Error Display */}
        {error && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700 flex items-center justify-between">
            <span>{error}</span>
            <button onClick={() => setError('')} className="text-red-500 hover:text-red-700">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Summary Cards */}
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
          {[
            { label: 'Total', value: totals.total, icon: DollarSign, iconBg: 'bg-indigo-100 text-indigo-600' },
            { label: 'Pending', value: `ETB ${totals.pending.toLocaleString()}`, icon: Clock, iconBg: 'bg-amber-100 text-amber-600' },
            { label: 'Collected', value: `ETB ${totals.collected.toLocaleString()}`, icon: CheckCircle2, iconBg: 'bg-emerald-100 text-emerald-600' },
            { label: 'Overdue', value: totals.overdue, icon: AlertTriangle, iconBg: 'bg-rose-100 text-rose-600' },
            { label: 'Penalties', value: `ETB ${totals.outstandingPenalties.toLocaleString()}`, icon: AlertTriangle, iconBg: 'bg-rose-100 text-rose-600' },
          ].map((card) => (
            <div key={card.label} className="bg-white rounded-xl border border-slate-100 p-4 shadow-sm hover:shadow-md transition-all hover:-translate-y-0.5">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{card.label}</p>
                  <p className="mt-1 text-xl font-bold text-slate-800">{card.value}</p>
                </div>
                <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${card.iconBg}`}>
                  <card.icon className="w-4 h-4" />
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Filters */}
        <div className="flex gap-2 flex-wrap">
          {['All', 'PENDING', 'PAID', 'OVERDUE'].map(f => (
            <button
              key={f}
              onClick={() => setStatusFilter(f)}
              className={`px-4 py-1.5 rounded-lg text-sm font-medium transition
                ${statusFilter === f 
                  ? 'bg-indigo-600 text-white shadow-sm' 
                  : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'}`}
            >
              {f === 'All' ? 'All' : f.charAt(0) + f.slice(1).toLowerCase()}
            </button>
          ))}
        </div>

        {/* Invoice Table */}
        <div className="bg-white rounded-xl border border-slate-100 shadow-sm overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-100">
              <tr className="text-left text-slate-500">
                <th className="px-4 py-3 font-medium">Invoice #</th>
                <th className="px-4 py-3 font-medium">Tenant</th>
                <th className="px-4 py-3 font-medium">Rent</th>
                <th className="px-4 py-3 font-medium">Penalty</th>
                <th className="px-4 py-3 font-medium">Total</th>
                <th className="px-4 py-3 font-medium">Due Date</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredInvoices.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-4 py-8 text-center text-slate-400">
                    No invoices found. Generate one to get started.
                  </td>
                </tr>
              ) : (
                filteredInvoices.map((inv) => (
                  <tr key={inv.id} className="hover:bg-slate-50/60 transition">
                    <td className="px-4 py-3 font-medium text-slate-800">{inv.invoiceNumber}</td>
                    <td className="px-4 py-3 text-slate-700">{getTenantName(inv)}</td>
                    <td className="px-4 py-3 text-slate-700">ETB {Number(inv.rentAmount).toLocaleString()}</td>
                    <td className={`px-4 py-3 font-medium ${inv.penaltyAmount > 0 ? 'text-rose-600' : 'text-slate-400'}`}>
                      {inv.penaltyAmount > 0 ? `ETB ${Number(inv.penaltyAmount).toLocaleString()}` : '—'}
                    </td>
                    <td className="px-4 py-3 font-semibold text-slate-800">
                      ETB {Number(inv.totalAmount).toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-slate-600">{inv.dueDate}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${statusBadge[inv.status]}`}>
                        {inv.status}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => setViewingInvoice(inv)}
                          className="p-1.5 rounded-lg text-slate-500 hover:bg-slate-100 hover:text-indigo-600 transition"
                          title="View invoice"
                        >
                          <FaEye />
                        </button>
                        {inv.status !== 'PAID' && (
                          <button
                            onClick={() => openPaymentModal(inv)}
                            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-50 text-emerald-700 hover:bg-emerald-100 font-medium text-xs transition"
                          >
                            <FaMoneyBill /> Pay
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          {!loading && filteredInvoices.length > 0 && (
            <div className="px-4 py-2 bg-slate-50 border-t border-slate-100 text-xs text-slate-500">
              Showing {filteredInvoices.length} of {invoices.length} invoices
            </div>
          )}
        </div>
      </div>

      {/* ── Record Payment Modal ── */}
      {payingInvoice && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold text-slate-800">Record Payment</h3>
              <button onClick={() => setPayingInvoice(null)} className="p-1.5 rounded-lg text-slate-400 hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>
            <p className="text-sm text-slate-500 mb-5">
              {payingInvoice.invoiceNumber} · {getTenantName(payingInvoice)}
            </p>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Payment Date</label>
                <input
                  type="date"
                  value={payForm.paymentDate}
                  onChange={(e) => handlePaymentDateChange(e.target.value)}
                  className="w-full px-4 py-2.5 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition text-sm"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Payment Method</label>
                <select
                  value={payForm.paymentMethod}
                  onChange={(e) => setPayForm({ ...payForm, paymentMethod: e.target.value })}
                  className="w-full px-4 py-2.5 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition text-sm"
                >
                  <option value="BANK_TRANSFER">Bank Transfer</option>
                  <option value="CASH">Cash</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Receipt</label>
                <label className="flex items-center gap-3 px-4 py-3 border-2 border-dashed border-slate-200 rounded-lg cursor-pointer hover:border-indigo-300 hover:bg-indigo-50/40 transition">
                  <Upload className="w-4 h-4 text-slate-400 shrink-0" />
                  <span className="text-sm text-slate-500 truncate">
                    {payForm.receiptName || 'Upload receipt (PNG, JPG, PDF)'}
                  </span>
                  <input
                    type="file"
                    accept="image/*,.pdf"
                    className="hidden"
                    onChange={(e) => {
                      const file = e.target.files[0];
                      if (file) {
                        setPayForm({ ...payForm, receipt: file, receiptName: file.name });
                      }
                    }}
                  />
                </label>
              </div>

              <div className={`rounded-xl p-4 text-sm ${penaltyPreview > 0 ? 'bg-rose-50 border border-rose-100' : 'bg-emerald-50 border border-emerald-100'}`}>
                <div className="flex justify-between text-slate-600">
                  <span>Rent</span>
                  <span>ETB {Number(payingInvoice.rentAmount).toLocaleString()}</span>
                </div>
                <div className={`flex justify-between mt-1 ${penaltyPreview > 0 ? 'text-rose-600 font-medium' : 'text-slate-400'}`}>
                  <span>Late penalty ({settings.penaltyRate}% after the {settings.paymentWindowEnd}th)</span>
                  <span>{penaltyPreview > 0 ? `+ ETB ${Number(penaltyPreview).toLocaleString()}` : 'None'}</span>
                </div>
                <div className="flex justify-between mt-2 pt-2 border-t border-slate-200/70 font-bold text-slate-800">
                  <span>Total</span>
                  <span>ETB {(Number(payingInvoice.rentAmount) + Number(penaltyPreview)).toLocaleString()}</span>
                </div>
              </div>

              <button
                onClick={handleRecordPayment}
                disabled={submitting}
                className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg shadow-sm transition disabled:opacity-50"
              >
                {submitting ? <Loader2 className="w-5 h-5 animate-spin mx-auto" /> : 'Save Payment'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── View Invoice Modal ── */}
      {viewingInvoice && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-5">
              <div>
                <h3 className="text-lg font-bold text-slate-800">{viewingInvoice.invoiceNumber}</h3>
                <span className={`inline-block mt-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${statusBadge[viewingInvoice.status]}`}>
                  {viewingInvoice.status}
                </span>
              </div>
              <button onClick={() => setViewingInvoice(null)} className="p-1.5 rounded-lg text-slate-400 hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-2.5 text-sm">
              {[
                ['Tenant', getTenantName(viewingInvoice)],
                ['Invoice Number', viewingInvoice.invoiceNumber],
                ['Issue Date', viewingInvoice.issueDate],
                ['Due Date', viewingInvoice.dueDate],
                ['Rent', `ETB ${Number(viewingInvoice.rentAmount).toLocaleString()}`],
                ['Penalty', viewingInvoice.penaltyAmount > 0 ? `ETB ${Number(viewingInvoice.penaltyAmount).toLocaleString()}` : '—'],
                ['Total Due', `ETB ${Number(viewingInvoice.totalAmount).toLocaleString()}`],
                ['Status', viewingInvoice.status],
                ['Payment Method', viewingInvoice.paymentMethod || '—'],
                ['Payment Date', viewingInvoice.paymentDate || '—'],
                ['Receipt', viewingInvoice.receiptPath ? '✅ Uploaded' : '—'],
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

      {/* ── Settings Modal ── */}
      {showSettings && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
            <div className="flex items-center justify-between mb-5">
              <h3 className="text-lg font-bold text-slate-800">Penalty Settings</h3>
              <button onClick={() => setShowSettings(false)} className="p-1.5 rounded-lg text-slate-400 hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Late penalty rate (%)</label>
                <input
                  type="number"
                  min="0"
                  max="100"
                  value={settings.penaltyRate}
                  onChange={(e) => setSettings({ ...settings, penaltyRate: Number(e.target.value) || 0 })}
                  className="w-full px-4 py-2.5 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition text-sm"
                />
                <p className="text-xs text-slate-400 mt-1.5">
                  Applied to rent when payment lands after the {settings.paymentWindowEnd}th of the month.
                </p>
              </div>
              <button
                onClick={() => setShowSettings(false)}
                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg shadow-sm transition"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </DashboardLayout>
  );
}