import { formatDualDate } from "../utils/ethiopianCalendar";
// src/pages/Payments.jsx
import { useState, useEffect, useMemo } from 'react';
import {
  FaEye, FaPlus, FaCheck,
} from 'react-icons/fa';
import {
  CheckCircle2, Clock, AlertTriangle, DollarSign,
  Settings2, X, Loader2, FileCheck, TrendingUp,
} from 'lucide-react';
import DashboardLayout from '../Components/Layout/DashboardLayout';
import { invoicesApi, paymentsApi, leasesApi, settingsApi } from '../services/api';

const statusBadge = {
  PENDING: 'bg-amber-100 text-amber-700',
  SUBMITTED: 'bg-blue-100 text-blue-700',
  PAID: 'bg-emerald-100 text-emerald-700',
  OVERDUE: 'bg-rose-100 text-rose-700',
};

const statusIcons = {
  PENDING: Clock,
  SUBMITTED: FileCheck,
  PAID: CheckCircle2,
  OVERDUE: AlertTriangle,
};

function StatCard({ label, value, icon: Icon, color = 'indigo', subtext, change }) {
  const colors = {
    indigo: 'bg-indigo-100 text-indigo-600',
    emerald: 'bg-emerald-100 text-emerald-600',
    amber: 'bg-amber-100 text-amber-600',
    rose: 'bg-rose-100 text-rose-600',
    blue: 'bg-blue-100 text-blue-600',
    purple: 'bg-purple-100 text-purple-600',
    slate: 'bg-slate-100 text-slate-600',
  };

  return (
    <div className="bg-white rounded-xl border border-slate-100 p-5 shadow-sm hover:shadow-md transition-all hover:-translate-y-0.5">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-slate-500 font-medium">{label}</p>
          <p className="text-2xl font-bold text-slate-800 mt-2">{value}</p>
          {subtext && <p className="text-xs text-slate-400 mt-1">{subtext}</p>}
          {change !== undefined && (
            <p className={`text-xs mt-1 font-medium ${change >= 0 ? 'text-emerald-600' : 'text-rose-600'}`}>
              {change >= 0 ? '↑' : '↓'} {Math.abs(change)}% from last month
            </p>
          )}
        </div>
        <div className={`w-11 h-11 rounded-xl flex items-center justify-center ${colors[color]}`}>
          <Icon className="w-5 h-5" />
        </div>
      </div>
    </div>
  );
}

function InvoiceStatusFilter({ filters, selected, onChange }) {
  return (
    <div className="flex gap-2 flex-wrap">
      {filters.map(f => (
        <button
          key={f.value}
          onClick={() => onChange(f.value)}
          className={`px-4 py-1.5 rounded-lg text-sm font-medium transition flex items-center gap-2
            ${selected === f.value
              ? 'bg-indigo-600 text-white shadow-sm'
              : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'}`}
        >
          {f.icon && <f.icon className="w-3.5 h-3.5" />}
          {f.label}
          {f.count !== undefined && (
            <span className={`text-xs ${selected === f.value ? 'text-indigo-200' : 'text-slate-400'}`}>
              ({f.count})
            </span>
          )}
        </button>
      ))}
    </div>
  );
}

function InvoiceRow({ invoice, onView, onApprove, approving, onForceOverdue }) {
  const StatusIcon = statusIcons[invoice.status] || Clock;

  return (
    <tr className="hover:bg-slate-50/60 transition">
      <td className="px-4 py-3 font-medium text-slate-800">{invoice.invoiceNumber}</td>
      <td className="px-4 py-3 text-slate-700">{invoice.tenantName || 'N/A'}</td>
      <td className="px-4 py-3 text-slate-700">{invoice.spaceUnit || 'N/A'}</td>
      <td className="px-4 py-3 font-semibold text-slate-800">
        ETB {Number(invoice.totalAmount).toLocaleString()}
      </td>
      <td className="px-4 py-3 text-slate-600">{formatDualDate(invoice.dueDate)}</td>
      <td className="px-4 py-3">
        <div className="flex items-center gap-2">
          <span className={`px-2.5 py-1 rounded-full text-xs font-medium flex items-center gap-1 ${statusBadge[invoice.status]}`}>
            <StatusIcon className="w-3 h-3" />
            {invoice.status}
          </span>
          {invoice.isOverdue && (
            <span className="text-xs text-rose-600 font-medium">
              {invoice.daysOverdue}d overdue
            </span>
          )}
        </div>
      </td>
      <td className="px-4 py-3">
        {invoice.receiptPath ? (
          <span className="text-xs text-emerald-600 font-medium flex items-center gap-1">
            <FileCheck className="w-3 h-3" /> Uploaded
          </span>
        ) : (
          <span className="text-xs text-slate-400">—</span>
        )}
      </td>
      <td className="px-4 py-3">
        <div className="flex items-center justify-end gap-2">
          <button
            onClick={() => onView(invoice)}
            className="p-1.5 rounded-lg text-slate-500 hover:bg-slate-100 hover:text-indigo-600 transition"
            title="View invoice"
          >
            <FaEye />
          </button>
          {invoice.status === 'PENDING' && (
            <button
              onClick={() => onForceOverdue(invoice)}
              className="p-1.5 text-amber-600 hover:bg-amber-50 rounded-lg transition"
              title="Force Overdue"
            >
              <AlertTriangle className="w-4 h-4" />
            </button>
          )}
          {invoice.status === 'SUBMITTED' && (
            <button
              onClick={() => onApprove(invoice)}
              disabled={approving === invoice.id}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-medium text-xs transition"
            >
              {approving === invoice.id
                ? <Loader2 className="w-3 h-3 animate-spin" />
                : <FaCheck />}
              Approve
            </button>
          )}
        </div>
      </td>
    </tr>
  );
}

function ViewInvoiceModal({ invoice, onClose, onApprove, approving }) {
  if (!invoice) return null;

  return (
    <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl p-6 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-5">
          <div>
            <div className="flex items-center gap-3">
              <h3 className="text-lg font-bold text-slate-800">{invoice.invoiceNumber}</h3>
              <span className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-medium ${statusBadge[invoice.status]}`}>
                {invoice.status}
              </span>
            </div>
            <p className="text-sm text-slate-500 mt-1">
              {invoice.tenantName} · {invoice.spaceUnit}
            </p>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg text-slate-400 hover:bg-slate-100">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="grid grid-cols-2 gap-4 mb-4">
          <div className="bg-slate-50 rounded-lg p-4">
            <p className="text-xs text-slate-500">Rent Amount</p>
            <p className="text-lg font-bold text-slate-800">ETB {Number(invoice.rentAmount).toLocaleString()}</p>
          </div>
          <div className="bg-slate-50 rounded-lg p-4">
            <p className="text-xs text-slate-500">Penalty</p>
            <p className={`text-lg font-bold ${invoice.penaltyAmount > 0 ? 'text-rose-600' : 'text-slate-800'}`}>
              {invoice.penaltyAmount > 0 ? `ETB ${Number(invoice.penaltyAmount).toLocaleString()}` : 'None'}
            </p>
          </div>
        </div>

        <div className="bg-indigo-50 rounded-lg p-4 mb-4">
          <p className="text-xs text-slate-500">Total Amount</p>
          <p className="text-2xl font-bold text-slate-800">ETB {Number(invoice.totalAmount).toLocaleString()}</p>
        </div>

        <div className="grid grid-cols-2 gap-3 text-sm">
          <div className="bg-slate-50 rounded-lg p-3">
            <p className="text-xs text-slate-500">Issue Date</p>
            <p className="font-medium text-slate-800">{formatDualDate(invoice.issueDate)}</p>
          </div>
          <div className="bg-slate-50 rounded-lg p-3">
            <p className="text-xs text-slate-500">Due Date</p>
            <p className={`font-medium ${invoice.isOverdue ? 'text-rose-600' : 'text-slate-800'}`}>
              {formatDualDate(invoice.dueDate)}
              {invoice.isOverdue && ` (${invoice.daysOverdue} days overdue)`}
            </p>
          </div>
          <div className="bg-slate-50 rounded-lg p-3">
            <p className="text-xs text-slate-500">Lease Start</p>
            <p className="font-medium text-slate-800">{invoice.leaseStartDate || 'N/A'}</p>
          </div>
          <div className="bg-slate-50 rounded-lg p-3">
            <p className="text-xs text-slate-500">Last Payment</p>
            <p className="font-medium text-slate-800">{invoice.lastPaymentDate || 'Never'}</p>
          </div>
        </div>

        {invoice.paymentMethod && (
          <div className="grid grid-cols-2 gap-3 mt-3">
            <div className="bg-slate-50 rounded-lg p-3">
              <p className="text-xs text-slate-500">Payment Method</p>
              <p className="font-medium text-slate-800">{invoice.paymentMethod}</p>
            </div>
            <div className="bg-slate-50 rounded-lg p-3">
              <p className="text-xs text-slate-500">Payment Date</p>
              <p className="font-medium text-slate-800">{invoice.paymentDate || 'N/A'}</p>
            </div>
          </div>
        )}

        <div className="mt-4 bg-slate-50 rounded-lg p-3">
          <p className="text-xs text-slate-500">Receipt</p>
          <p className="font-medium text-slate-800">
            {invoice.receiptPath ? '✅ Uploaded' : 'Not uploaded'}
          </p>
        </div>

        {invoice.status === 'SUBMITTED' && (
          <button
            onClick={() => onApprove(invoice)}
            disabled={approving === invoice.id}
            className="w-full mt-5 py-3 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-medium rounded-lg shadow-sm transition flex items-center justify-center gap-2"
          >
            {approving === invoice.id
              ? <Loader2 className="w-5 h-5 animate-spin" />
              : <><FaCheck /> Approve & Mark Paid</>}
          </button>
        )}
      </div>
    </div>
  );
}

function SettingsModal({ settings, onSave, onClose, saving }) {
  const [localSettings, setLocalSettings] = useState(settings);

  return (
    <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
        <div className="flex items-center justify-between mb-5">
          <h3 className="text-lg font-bold text-slate-800">Penalty Settings</h3>
          <button onClick={onClose} className="p-1.5 rounded-lg text-slate-400 hover:bg-slate-100">
            <X className="w-5 h-5" />
          </button>
        </div>
        <form onSubmit={(e) => { e.preventDefault(); onSave(localSettings); }}>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">
                Late penalty rate (%)
              </label>
              <input
                type="number"
                min="0"
                max="100"
                value={localSettings.penaltyRate}
                onChange={(e) => setLocalSettings({ ...localSettings, penaltyRate: Number(e.target.value) || 0 })}
                className="w-full px-4 py-2.5 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition text-sm"
                required
              />
              <p className="text-xs text-slate-400 mt-1.5">
                Applied to rent when payment lands after the {localSettings.paymentWindowEnd}th of the month.
              </p>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">
                Payment window end day
              </label>
              <input
                type="number"
                min="1"
                max="31"
                value={localSettings.paymentWindowEnd}
                onChange={(e) => setLocalSettings({ ...localSettings, paymentWindowEnd: Number(e.target.value) || 10 })}
                className="w-full px-4 py-2.5 border border-slate-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition text-sm"
                required
              />
              <p className="text-xs text-slate-400 mt-1.5">
                Payments made after this day of the month will incur penalties.
              </p>
            </div>
            <div className="flex gap-3 pt-2">
              <button
                type="submit"
                disabled={saving}
                className="flex-1 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg shadow-sm transition disabled:opacity-50"
              >
                {saving ? <Loader2 className="w-4 h-4 animate-spin mx-auto" /> : 'Save Settings'}
              </button>
              <button
                type="button"
                onClick={onClose}
                className="px-6 py-2.5 border border-slate-200 hover:bg-slate-50 text-slate-700 font-medium rounded-lg transition"
              >
                Cancel
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function Payments() {
  const [invoices, setInvoices] = useState([]);
  const [leases, setLeases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');
  const [settings, setSettings] = useState({ penaltyRate: 20, paymentWindowEnd: 10 });
  const [showSettings, setShowSettings] = useState(false);
  const [savingSettings, setSavingSettings] = useState(false);
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
      setInvoices(invoicesData || []);
      setLeases(leasesData || []);
    } catch (err) {
      setError(err.message || 'Failed to load data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [invoicesData, leasesData] = await Promise.all([
          invoicesApi.getAll(),
          leasesApi.getAll(),
        ]);
        if (!cancelled) {
          setInvoices(invoicesData || []);
          setLeases(leasesData || []);
        }
      } catch (err) {
        if (!cancelled) setError(err.message || 'Failed to load data');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    (async () => {
      try {
        const data = await settingsApi.getSettings();
        if (data && !cancelled) {
          setSettings({
            penaltyRate: data.penaltyRate || 20,
            paymentWindowEnd: data.paymentWindowEnd || 10,
          });
        }
      } catch (err) {
        console.error('Failed to load settings:', err);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  const totals = useMemo(() => ({
    total: invoices.length,
    pending: invoices.filter(i => i.status === 'PENDING').reduce((s, i) => s + (i.totalAmount || 0), 0),
    submitted: invoices.filter(i => i.status === 'SUBMITTED').length,
    collected: invoices.filter(i => i.status === 'PAID').reduce((s, i) => s + (i.totalAmount || 0), 0),
    overdue: invoices.filter(i => i.status === 'OVERDUE').length,
    overdueAmount: invoices.filter(i => i.status === 'OVERDUE').reduce((s, i) => s + (i.totalAmount || 0), 0),
    totalPenalties: invoices.filter(i => i.penaltyAmount > 0).reduce((s, i) => s + (i.penaltyAmount || 0), 0),
  }), [invoices]);

  const filterCounts = useMemo(() => {
    const counts = { All: invoices.length };
    ['PENDING', 'SUBMITTED', 'PAID', 'OVERDUE'].forEach(s => {
      counts[s] = invoices.filter(i => i.status === s).length;
    });
    return counts;
  }, [invoices]);

  const filters = [
    { value: 'All', label: 'All', icon: null },
    { value: 'PENDING', label: 'Pending', icon: Clock },
    { value: 'SUBMITTED', label: 'Awaiting Approval', icon: FileCheck },
    { value: 'PAID', label: 'Paid', icon: CheckCircle2 },
    { value: 'OVERDUE', label: 'Overdue', icon: AlertTriangle },
  ].map(f => ({ ...f, count: filterCounts[f.value] }));

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

  const handleForceOverdue = async (invoice) => {
    if (!window.confirm(`Force invoice ${invoice.invoiceNumber} to be overdue?`)) return;
    try {
      await invoicesApi.forceOverdue(invoice.id);
      await loadData();
      alert('✅ Invoice marked as overdue');
    } catch (err) {
      setError(err.message || 'Failed to force overdue');
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
      alert('✅ Invoice generated successfully!');
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
      alert('✅ Statuses updated successfully!');
    } catch (err) {
      setError(err.message || 'Failed to update statuses');
    } finally {
      setUpdatingStatus(false);
    }
  };

  const handleSaveSettings = async (newSettings) => {
    setSavingSettings(true);
    setError('');
    try {
      await settingsApi.updateSettings({
        penaltyRate: parseInt(newSettings.penaltyRate),
        paymentWindowEnd: parseInt(newSettings.paymentWindowEnd),
      });
      setSettings(newSettings);
      setShowSettings(false);
      alert('✅ Settings saved successfully!');
    } catch (err) {
      setError(err.message || 'Failed to save settings');
    } finally {
      setSavingSettings(false);
    }
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
        {/* Header */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h2 className="text-2xl font-bold text-slate-800">Invoices & Payments</h2>
            <p className="text-slate-500 text-sm mt-1">
              Manage tenant payments and approvals
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

        {/* Overview Alert - Overdue Summary */}
        {totals.overdue > 0 && (
          <div className="bg-rose-50 border border-rose-200 rounded-xl p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-rose-100 rounded-full flex items-center justify-center shrink-0">
                <AlertTriangle className="w-5 h-5 text-rose-600" />
              </div>
              <div>
                <p className="text-sm font-medium text-rose-800">
                  {totals.overdue} Overdue Invoice{totals.overdue !== 1 ? 's' : ''}
                </p>
                <p className="text-xs text-rose-600">
                  Total overdue amount: ETB {totals.overdueAmount.toLocaleString()}
                  {totals.totalPenalties > 0 && ` · Penalties: ETB ${totals.totalPenalties.toLocaleString()}`}
                </p>
              </div>
            </div>
            <button
              onClick={() => setStatusFilter('OVERDUE')}
              className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white text-sm font-medium rounded-lg transition"
            >
              View Overdue
            </button>
          </div>
        )}

        {/* Submitted Alert - Awaiting Approval */}
        {totals.submitted > 0 && (
          <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center shrink-0">
                <FileCheck className="w-5 h-5 text-blue-600" />
              </div>
              <div>
                <p className="text-sm font-medium text-blue-800">
                  {totals.submitted} Payment{totals.submitted !== 1 ? 's' : ''} Awaiting Approval
                </p>
                <p className="text-xs text-blue-600">
                  Tenants have submitted receipts. Review and approve them.
                </p>
              </div>
            </div>
            <button
              onClick={() => setStatusFilter('SUBMITTED')}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg transition"
            >
              Review Now
            </button>
          </div>
        )}

        {error && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700 flex items-center justify-between">
            <span>{error}</span>
            <button onClick={() => setError('')} className="text-red-500 hover:text-red-700">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Stats Cards */}
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
          <StatCard label="Total" value={totals.total} icon={DollarSign} color="indigo" />
          <StatCard label="Pending" value={`ETB ${totals.pending.toLocaleString()}`} icon={Clock} color="amber" />
          <StatCard label="Awaiting Approval" value={totals.submitted} icon={FileCheck} color="blue" />
          <StatCard label="Collected" value={`ETB ${totals.collected.toLocaleString()}`} icon={CheckCircle2} color="emerald" />
          <StatCard label="Overdue" value={totals.overdue} icon={AlertTriangle} color="rose" />
          <StatCard label="Penalties" value={`ETB ${totals.totalPenalties.toLocaleString()}`} icon={TrendingUp} color="rose" />
        </div>

        {/* Filters */}
        <InvoiceStatusFilter
          filters={filters}
          selected={statusFilter}
          onChange={setStatusFilter}
        />

        {/* Table */}
        <div className="bg-white rounded-xl border border-slate-100 shadow-sm overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-100">
              <tr className="text-left text-slate-500">
                <th className="px-4 py-3 font-medium">Invoice #</th>
                <th className="px-4 py-3 font-medium">Tenant</th>
                <th className="px-4 py-3 font-medium">Space</th>
                <th className="px-4 py-3 font-medium">Total</th>
                <th className="px-4 py-3 font-medium">Due Date</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Receipt</th>
                <th className="px-4 py-3 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredInvoices.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-4 py-8 text-center text-slate-400">
                    No invoices found with the selected filter.
                  </td>
                </tr>
              ) : (
                filteredInvoices.map((inv) => (
                  <InvoiceRow
                    key={inv.id}
                    invoice={inv}
                    onView={setViewingInvoice}
                    onApprove={handleApprove}
                    approving={approvingId}
                    onForceOverdue={handleForceOverdue}
                  />
                ))
              )}
            </tbody>
          </table>
          {!loading && filteredInvoices.length > 0 && (
            <div className="px-4 py-2 bg-slate-50 border-t border-slate-100 text-xs text-slate-500 flex justify-between">
              <span>Showing {filteredInvoices.length} of {invoices.length} invoices</span>
              <span>
                {invoices.filter(i => i.status === 'SUBMITTED').length} awaiting approval ·
                {invoices.filter(i => i.status === 'OVERDUE').length} overdue
              </span>
            </div>
          )}
        </div>
      </div>

      {/* View Invoice Modal */}
      {viewingInvoice && (
        <ViewInvoiceModal
          invoice={viewingInvoice}
          onClose={() => setViewingInvoice(null)}
          onApprove={handleApprove}
          approving={approvingId}
        />
      )}

      {/* Settings Modal */}
      {showSettings && (
        <SettingsModal
          settings={settings}
          onSave={handleSaveSettings}
          onClose={() => setShowSettings(false)}
          saving={savingSettings}
        />
      )}
    </DashboardLayout>
  );
}