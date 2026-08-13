// src/pages/Payments.jsx
import { useState, useEffect, useMemo } from 'react';
import {
  FaEye, FaPlus, FaCheck,
} from 'react-icons/fa';
import {
  CheckCircle2, Clock, AlertTriangle, DollarSign,
  Settings2, X, Loader2, FileCheck,
} from 'lucide-react';
import DashboardLayout from '../components/Layout/DashboardLayout';
import { invoicesApi, paymentsApi, leasesApi } from '../services/api';

const statusBadge = {
  PENDING: 'bg-amber-100 text-amber-700',
  SUBMITTED: 'bg-blue-100 text-blue-700',
  PAID: 'bg-emerald-100 text-emerald-700',
  OVERDUE: 'bg-rose-100 text-rose-700',
};

export default function Payments() {
  const todayStr = new Date().toISOString().split('T')[0];

  const [invoices, setInvoices] = useState([]);
  const [leases, setLeases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');
  const [settings, setSettings] = useState({ penaltyRate: 20, paymentWindowEnd: 10 });
  const [showSettings, setShowSettings] = useState(false);

  const [viewingInvoice, setViewingInvoice] = useState(null);
  const [approvingId, setApprovingId] = useState(null);
  const [generating, setGenerating] = useState(false);
  const [updatingStatus, setUpdatingStatus] = useState(false);

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

  useEffect(() => { loadData(); }, []);

  const totals = useMemo(() => ({
    total: invoices.length,
    pending: invoices.filter(i => i.status === 'PENDING').reduce((s, i) => s + (i.totalAmount || 0), 0),
    submitted: invoices.filter(i => i.status === 'SUBMITTED').length,
    collected: invoices.filter(i => i.status === 'PAID').reduce((s, i) => s + (i.totalAmount || 0), 0),
    overdue: invoices.filter(i => i.status === 'OVERDUE').length,
  }), [invoices]);

  const filteredInvoices = useMemo(() => {
    if (statusFilter === 'All') return invoices;
    return invoices.filter(i => i.status === statusFilter);
  }, [invoices, statusFilter]);

  const handleApprove = async (invoice) => {
    setError('');
    setApprovingId(invoice.id);
    try {
      await paymentsApi.approve(invoice.id);
      await loadData();
      setViewingInvoice(null);
    } catch (err) {
      setError(err.message || 'Failed to approve payment');
    } finally {
      setApprovingId(null);
    }
  };

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

  const handleUpdateStatuses = async () => {
    setUpdatingStatus(true);
    setError('');
    try {
      await Promise.all([invoicesApi.markOverdue(), leasesApi.markExpired()]);
      await loadData();
    } catch (err) {
      setError(err.message || 'Failed to update statuses');
    } finally {
      setUpdatingStatus(false);
    }
  };

  const getTenantName = (invoice) => {
    if (invoice.lease?.tenant?.name) return invoice.lease.tenant.name;
    return invoice.leaseId ? `Lease #${invoice.leaseId}` : 'Unknown';
  };

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
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h2 className="text-2xl font-bold text-slate-800">Invoices & Payments</h2>
            <p className="text-slate-500 text-sm mt-1">
              Review tenant-submitted payments and approve them
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
              onClick={handleUpdateStatuses}
              disabled={updatingStatus}
              className="flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 font-medium rounded-lg text-sm transition disabled:opacity-50"
            >
              {updatingStatus ? <Loader2 className="w-4 h-4 animate-spin" /> : <Clock className="w-4 h-4" />}
              Update Status
            </button>
            <button
              onClick={handleGenerateInvoice}
              disabled={generating}
              className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg shadow-sm transition disabled:opacity-50"
            >
              {generating ? <Loader2 className="w-4 h-4 animate-spin" /> : <FaPlus />}
              Generate Invoice
            </button>
          </div>
        </div>

        {error && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700 flex items-center justify-between">
            <span>{error}</span>
            <button onClick={() => setError('')} className="text-red-500 hover:text-red-700">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
          {[
            { label: 'Total', value: totals.total, icon: DollarSign, iconBg: 'bg-indigo-100 text-indigo-600' },
            { label: 'Pending', value: `ETB ${totals.pending.toLocaleString()}`, icon: Clock, iconBg: 'bg-amber-100 text-amber-600' },
            { label: 'Awaiting Approval', value: totals.submitted, icon: FileCheck, iconBg: 'bg-blue-100 text-blue-600' },
            { label: 'Collected', value: `ETB ${totals.collected.toLocaleString()}`, icon: CheckCircle2, iconBg: 'bg-emerald-100 text-emerald-600' },
            { label: 'Overdue', value: totals.overdue, icon: AlertTriangle, iconBg: 'bg-rose-100 text-rose-600' },
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

        <div className="flex gap-2 flex-wrap">
          {['All', 'PENDING', 'SUBMITTED', 'PAID', 'OVERDUE'].map(f => (
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

        <div className="bg-white rounded-xl border border-slate-100 shadow-sm overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-100">
              <tr className="text-left text-slate-500">
                <th className="px-4 py-3 font-medium">Invoice #</th>
                <th className="px-4 py-3 font-medium">Tenant</th>
                <th className="px-4 py-3 font-medium">Total</th>
                <th className="px-4 py-3 font-medium">Due Date</th>
                <th className="px-4 py-3 font-medium">Receipt</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredInvoices.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-slate-400">
                    No invoices found.
                  </td>
                </tr>
              ) : (
                filteredInvoices.map((inv) => (
                  <tr key={inv.id} className="hover:bg-slate-50/60 transition">
                    <td className="px-4 py-3 font-medium text-slate-800">{inv.invoiceNumber}</td>
                    <td className="px-4 py-3 text-slate-700">{getTenantName(inv)}</td>
                    <td className="px-4 py-3 font-semibold text-slate-800">
                      ETB {Number(inv.totalAmount).toLocaleString()}
                    </td>
                    <td className="px-4 py-3 text-slate-600">{inv.dueDate}</td>
                    <td className="px-4 py-3">
                      {inv.receiptPath
                        ? <span className="text-xs text-emerald-600 font-medium">Uploaded ✓</span>
                        : <span className="text-xs text-slate-400">—</span>}
                      {/* TODO: wire to receipt download endpoint when available:
                          <a href={paymentsApi.receiptUrl(inv.id)} target="_blank">View</a> */}
                    </td>
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
                        {inv.status === 'SUBMITTED' && (
                          <button
                            onClick={() => handleApprove(inv)}
                            disabled={approvingId === inv.id}
                            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-medium text-xs transition"
                          >
                            {approvingId === inv.id
                              ? <Loader2 className="w-3 h-3 animate-spin" />
                              : <FaCheck />}
                            Mark Paid
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
                ['Issue Date', viewingInvoice.issueDate],
                ['Due Date', viewingInvoice.dueDate],
                ['Rent', `ETB ${Number(viewingInvoice.rentAmount).toLocaleString()}`],
                ['Penalty', viewingInvoice.penaltyAmount > 0 ? `ETB ${Number(viewingInvoice.penaltyAmount).toLocaleString()}` : '—'],
                ['Total Due', `ETB ${Number(viewingInvoice.totalAmount).toLocaleString()}`],
                ['Payment Method', viewingInvoice.paymentMethod || '—'],
                ['Payment Date', viewingInvoice.paymentDate || '—'],
                ['Receipt', viewingInvoice.receiptPath ? 'Uploaded ✓' : '—'],
              ].map(([k, v]) => (
                <div key={k} className="flex justify-between py-1.5 border-b border-slate-50 last:border-0">
                  <span className="text-slate-500">{k}</span>
                  <span className="font-medium text-slate-800">{v}</span>
                </div>
              ))}
            </div>
            {viewingInvoice.status === 'SUBMITTED' && (
              <button
                onClick={() => handleApprove(viewingInvoice)}
                disabled={approvingId === viewingInvoice.id}
                className="w-full mt-5 py-3 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-medium rounded-lg shadow-sm transition flex items-center justify-center gap-2"
              >
                {approvingId === viewingInvoice.id
                  ? <Loader2 className="w-5 h-5 animate-spin" />
                  : <><FaCheck /> Approve & Mark Paid</>}
              </button>
            )}
          </div>
        </div>
      )}

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
                  type="number" min="0" max="100"
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