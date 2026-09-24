// src/pages/TenantDashboard.jsx
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Building2, FileText, CreditCard, AlertCircle, Upload,
  CheckCircle, Clock, Loader2, Calendar, Home,
  DollarSign, Info, Download,
  Receipt,
} from 'lucide-react';
import DashboardLayout from '../Components/Layout/DashboardLayout';
import { tenantApi, chapaApi, documentsApi, invoicesApi } from '../services/api';
import { useAuth } from '../hooks/useAuth';
function StatCard({ label, value, icon: Icon, color = 'indigo', subtext }) {
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
        </div>
        <div className={`w-11 h-11 rounded-xl flex items-center justify-center ${colors[color]}`}>
          <Icon className="w-5 h-5" />
        </div>
      </div>
    </div>
  );
}

function StatusBadge({ status }) {
  const styles = {
    ACTIVE: 'bg-emerald-100 text-emerald-700',
    EXPIRED: 'bg-amber-100 text-amber-700',
    TERMINATED: 'bg-rose-100 text-rose-700',
    PENDING: 'bg-amber-100 text-amber-700',
    SUBMITTED: 'bg-blue-100 text-blue-700',
    PAID: 'bg-emerald-100 text-emerald-700',
    OVERDUE: 'bg-rose-100 text-rose-700',
  };
  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${styles[status] || 'bg-slate-100 text-slate-700'}`}>
      {status}
    </span>
  );
}

function InvoiceCard({ invoice, onPay, onUpload, onDownloadPdf, paying, uploading }) {
  const isPayable = invoice.status === 'PENDING' || invoice.status === 'OVERDUE';
  const isSubmitted = invoice.status === 'SUBMITTED';
  const isPaid = invoice.status === 'PAID';
  const hasPenalty = invoice.penaltyAmount > 0;
  const hasWithholding = Number(invoice.withholdingTax) > 0 && invoice.withholdingApplicable;

  return (
    <div className={`border rounded-lg p-4 transition ${
      invoice.isOverdue ? 'border-rose-200 bg-rose-50/30' :
      isPaid ? 'border-emerald-200 bg-emerald-50/30' :
      'border-slate-100 hover:bg-slate-50/60'
    }`}>
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div className="flex-1">
          <div className="flex items-center gap-3 flex-wrap">
            <p className="font-semibold text-slate-800">{invoice.invoiceNumber}</p>
            <StatusBadge status={invoice.status} />
            {invoice.isOverdue && (
              <span className="text-xs text-rose-600 font-medium bg-rose-100 px-2 py-0.5 rounded-full">
                {invoice.daysOverdue} days overdue
              </span>
            )}
            {isPaid && invoice.paymentDate && (
              <span className="text-xs text-emerald-600 font-medium bg-emerald-100 px-2 py-0.5 rounded-full">
                Paid on {new Date(invoice.paymentDate).toLocaleDateString()}
              </span>
            )}
            {hasWithholding && (
              <span className="text-xs text-purple-600 font-medium bg-purple-100 px-2 py-0.5 rounded-full">
                <Receipt className="w-3 h-3 inline mr-1" />
                Withholding: ETB {Number(invoice.withholdingTax).toLocaleString()}
              </span>
            )}
          </div>

          <div className="mt-2 grid grid-cols-2 md:grid-cols-4 gap-2 text-sm">
            <div>
              <p className="text-slate-500">Amount</p>
              <p className="font-medium text-slate-800">ETB {Number(invoice.totalAmount).toLocaleString()}</p>
            </div>
            <div>
              <p className="text-slate-500">Due Date</p>
              <p className={`font-medium ${invoice.isOverdue ? 'text-rose-600' : 'text-slate-800'}`}>
                {invoice.dueDate}
              </p>
            </div>
            <div>
              <p className="text-slate-500">Lease Start</p>
              <p className="text-slate-800">{invoice.leaseStartDate}</p>
            </div>
            <div>
              <p className="text-slate-500">Last Payment</p>
              <p className="text-slate-800">{invoice.lastPaymentDate || 'Never'}</p>
            </div>
          </div>

          {/* Tax Breakdown */}
          {(hasWithholding || Number(invoice.untaxedRent) > 0) && (
            <div className="mt-2 text-xs bg-slate-50 border border-slate-200 rounded-lg px-3 py-2">
              <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                {Number(invoice.untaxedRent) > 0 && (
                  <div>
                    <span className="text-slate-500">Untaxed Rent:</span>
                    <span className="font-medium text-slate-700 ml-1">
                      ETB {Number(invoice.untaxedRent).toLocaleString()}
                    </span>
                  </div>
                )}
                {Number(invoice.vatAmount) > 0 && (
                  <div>
                    <span className="text-slate-500">VAT (15%):</span>
                    <span className="font-medium text-slate-700 ml-1">
                      ETB {Number(invoice.vatAmount).toLocaleString()}
                    </span>
                  </div>
                )}
                {hasWithholding && (
                  <div>
                    <span className="text-purple-500">Withholding (3%):</span>
                    <span className="font-medium text-purple-700 ml-1">
                      ETB {Number(invoice.withholdingTax).toLocaleString()}
                    </span>
                  </div>
                )}
                {invoice.penaltyAmount > 0 && (
                  <div>
                    <span className="text-rose-500">Penalty (20%):</span>
                    <span className="font-medium text-rose-700 ml-1">
                      ETB {Number(invoice.penaltyAmount).toLocaleString()}
                    </span>
                  </div>
                )}
              </div>
            </div>
          )}

          {hasPenalty && (
            <div className="mt-2 text-sm bg-rose-50 border border-rose-200 rounded-lg px-3 py-1.5 inline-block">
              <span className="text-rose-700">⚠️ Penalty: ETB {Number(invoice.penaltyAmount).toLocaleString()}</span>
              <span className="text-rose-500 text-xs ml-2">
                ({Math.round((invoice.penaltyAmount / invoice.rentAmount) * 100)}% late fee)
              </span>
            </div>
          )}

          {isPaid && invoice.paymentMethod && (
            <div className="mt-2 text-sm text-emerald-700">
              ✅ Paid via {invoice.paymentMethod} on {new Date(invoice.paymentDate).toLocaleDateString()}
            </div>
          )}

          {isSubmitted && (
            <div className="mt-2 text-sm text-blue-600 flex items-center gap-2">
              <Clock className="w-4 h-4" />
              Awaiting admin approval - you'll be notified once confirmed
            </div>
          )}

          {invoice.status === 'PENDING' && !invoice.isOverdue && (
            <div className="mt-3 text-xs text-slate-500 bg-slate-50 rounded-lg px-3 py-1.5">
              💡 This invoice is due on {invoice.dueDate}. You can pay online or upload a receipt.
            </div>
          )}

          {invoice.isOverdue && (
            <div className="mt-3 text-xs text-rose-700 bg-rose-100 rounded-lg px-3 py-1.5">
              ⚠️ This invoice is {invoice.daysOverdue} days overdue. Late fees have been applied. Please pay immediately.
            </div>
          )}
        </div>

        <div className="flex items-center gap-2 flex-shrink-0">
          {isSubmitted ? (
            <span className="text-sm text-blue-600 font-medium whitespace-nowrap">Awaiting approval</span>
          ) : isPaid ? (
            <span className="text-sm text-emerald-600 font-medium whitespace-nowrap">Paid ✓</span>
          ) : isPayable ? (
            <>
              <button
                onClick={() => onPay(invoice)}
                disabled={paying === invoice.id}
                className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white text-sm font-medium rounded-lg transition inline-flex items-center gap-2"
              >
                {paying === invoice.id ? (
                  <><Loader2 className="w-4 h-4 animate-spin" /> Processing</>
                ) : (
                  <><CreditCard className="w-4 h-4" /> Pay</>
                )}
              </button>
              <label className="px-4 py-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-sm font-medium rounded-lg transition inline-flex items-center gap-2 cursor-pointer">
                {uploading === invoice.id ? (
                  <><Loader2 className="w-4 h-4 animate-spin" /> Uploading</>
                ) : (
                  <><Upload className="w-4 h-4" /> Receipt</>
                )}
                <input
                  type="file"
                  accept="image/*,.pdf"
                  className="hidden"
                  disabled={uploading === invoice.id}
                  onChange={(e) => {
                    const file = e.target.files[0];
                    if (file) onUpload(invoice, file);
                  }}
                />
              </label>
            </>
          ) : null}
          <button
            onClick={() => onDownloadPdf(invoice)}
            className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-sm font-medium rounded-lg transition inline-flex items-center gap-1.5"
            title="Download PDF"
          >
            <Download className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}

function PaymentHistory({ invoices }) {
  const paidInvoices = invoices.filter(i => i.status === 'PAID');

  if (paidInvoices.length === 0) {
    return (
      <div className="bg-white rounded-xl border border-slate-100 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 flex items-center gap-2">
          <Clock className="w-5 h-5 text-indigo-600" />
          <h2 className="text-lg font-semibold text-slate-800">Payment History</h2>
        </div>
        <div className="p-6 text-center">
          <p className="text-sm text-slate-500">No payments recorded yet.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl border border-slate-100 shadow-sm overflow-hidden">
      <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Clock className="w-5 h-5 text-indigo-600" />
          <h2 className="text-lg font-semibold text-slate-800">Payment History</h2>
        </div>
        <span className="text-sm text-slate-500">{paidInvoices.length} payments</span>
      </div>
      <div className="p-4 space-y-2">
        {paidInvoices
          .sort((a, b) => new Date(b.paymentDate) - new Date(a.paymentDate))
          .map(inv => (
            <div key={inv.id} className="flex items-center justify-between border-b border-slate-100 pb-2 last:border-0">
              <div>
                <p className="text-sm font-medium text-slate-800">{inv.invoiceNumber}</p>
                <p className="text-xs text-slate-500">
                  {inv.paymentDate ? new Date(inv.paymentDate).toLocaleDateString() : 'N/A'}
                </p>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold text-emerald-600">
                  ETB {Number(inv.totalAmount).toLocaleString()}
                </p>
                <p className="text-xs text-slate-400">{inv.paymentMethod || 'receipt'}</p>
              </div>
            </div>
          ))}
      </div>
    </div>
  );
}

function PaymentSummary({ stats }) {
  return (
    <div className="bg-white rounded-xl border border-slate-100 shadow-sm overflow-hidden">
      <div className="px-6 py-4 border-b border-slate-100 flex items-center gap-2">
        <DollarSign className="w-5 h-5 text-emerald-600" />
        <h2 className="text-lg font-semibold text-slate-800">Payment Summary</h2>
      </div>
      <div className="p-4 grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-slate-50 rounded-lg p-3 text-center">
          <p className="text-2xl font-bold text-slate-800">{stats?.paidInvoices || 0}</p>
          <p className="text-xs text-slate-500">Paid</p>
        </div>
        <div className="bg-slate-50 rounded-lg p-3 text-center">
          <p className={`text-2xl font-bold ${(stats?.pendingInvoices || 0) > 0 ? 'text-amber-600' : 'text-slate-800'}`}>
            {stats?.pendingInvoices || 0}
          </p>
          <p className="text-xs text-slate-500">Pending</p>
        </div>
        <div className="bg-slate-50 rounded-lg p-3 text-center">
          <p className={`text-2xl font-bold ${(stats?.submittedInvoices || 0) > 0 ? 'text-blue-600' : 'text-slate-800'}`}>
            {stats?.submittedInvoices || 0}
          </p>
          <p className="text-xs text-slate-500">Awaiting Approval</p>
        </div>
        <div className="bg-slate-50 rounded-lg p-3 text-center">
          <p className={`text-2xl font-bold ${(stats?.overdueInvoices || 0) > 0 ? 'text-rose-600' : 'text-slate-800'}`}>
            {stats?.overdueInvoices || 0}
          </p>
          <p className="text-xs text-slate-500">Overdue</p>
        </div>
      </div>
    </div>
  );
}

export default function TenantDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lease, setLease] = useState(null);
  const [invoices, setInvoices] = useState([]);
  const [stats, setStats] = useState(null);
  const [payingId, setPayingId] = useState(null);
  const [uploadingId, setUploadingId] = useState(null);
  const [payError, setPayError] = useState('');
  const [documents, setDocuments] = useState([]);
  const [loadingDocs] = useState(false);

  useEffect(() => {
    if (user && user.role !== 'TENANT') {
      navigate('/dashboard');
      return;
    }

    const loadData = async () => {
      setLoading(true);
      setError('');
      try {
        const [leaseData, invoicesData, statsData] = await Promise.all([
          tenantApi.getMyLease(),
          tenantApi.getMyInvoices(),
          tenantApi.getStats(),
        ]);
        setLease(leaseData);
        setInvoices(invoicesData || []);
        setStats(statsData);
      } catch (err) {
        setError(err.message || 'Failed to load tenant data');
        console.error('Tenant dashboard error:', err);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [user, navigate]);

  const refresh = async () => {
    try {
      const [invoicesData, statsData] = await Promise.all([
        tenantApi.getMyInvoices(),
        tenantApi.getStats(),
      ]);
      setInvoices(invoicesData || []);
      setStats(statsData);
    } catch (err) {
      console.error('Refresh failed:', err);
    }
  };

  const handlePay = async (invoice) => {
    setPayError('');
    setPayingId(invoice.id);
    try {
      const phone = prompt('Enter your phone number (e.g., 0912345678):');
      if (!phone) {
        setPayingId(null);
        return;
      }
      const res = await chapaApi.initialize(invoice.id, phone);
      const url = res?.checkoutUrl || res?.checkout_url || res?.data?.checkout_url;
      if (url) {
        window.location.href = url;
      } else {
        setPayError('Payment link not received. Contact admin.');
        setPayingId(null);
      }
    } catch (err) {
      setPayError(err.message || 'Could not start payment.');
      setPayingId(null);
    }
  };

  const handleReceiptUpload = async (invoice, file) => {
    if (!file) return;
    setPayError('');
    setUploadingId(invoice.id);
    try {
      const today = new Date().toISOString().split('T')[0];
      await tenantApi.submitReceipt(invoice.id, today, file);
      await refresh();
      alert('✅ Receipt uploaded successfully! Waiting for admin approval.');
    } catch (err) {
      // Handle validation errors from backend
      if (err.message && err.message.includes('less than expected')) {
        setPayError('Payment amount validation failed: ' + err.message);
      } else if (err.message && err.message.includes('exceeds expected')) {
        setPayError('Payment amount validation failed: ' + err.message);
      } else {
        setPayError(err.message || 'Could not upload receipt.');
      }
    } finally {
      setUploadingId(null);
    }
  };

  useEffect(() => {
    if (!user || user.role !== 'TENANT') return;
    let cancelled = false;
    documentsApi.getMyDocuments()
      .then((data) => {
        if (!cancelled) setDocuments(data || []);
      })
      .catch((err) => {
        console.error('Failed to load documents:', err);
      });
    return () => {
      cancelled = true;
    };
  }, [user]);

  const handleDownloadPdf = async (invoice) => {
    try {
      await invoicesApi.downloadPdf(invoice.id, invoice.invoiceNumber);
    } catch (err) {
      alert('Failed to download PDF: ' + err.message);
    }
  };

  const handleDownload = async (doc) => {
    try {
      await documentsApi.download(doc.id, doc.fileName);
    } catch (error) {
      console.error('Download failed:', error);
      alert('Failed to download document. Please try again.');
    }
  };

  if (loading) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center h-96">
          <Loader2 className="w-8 h-8 animate-spin text-indigo-600" />
          <span className="ml-3 text-slate-500">Loading your dashboard...</span>
        </div>
      </DashboardLayout>
    );
  }

  if (error) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center h-96">
          <div className="bg-red-50 border border-red-200 rounded-lg px-6 py-4 text-sm text-red-700 flex items-center gap-3">
            <AlertCircle className="w-5 h-5" />
            {error}
          </div>
        </div>
      </DashboardLayout>
    );
  }

  const hasLease = lease && lease.status === 'ACTIVE';
  const totalInvoices = stats?.totalInvoices || 0;
  const pendingInvoices = stats?.pendingInvoices || 0;
  const submittedInvoices = stats?.submittedInvoices || 0;
  const paidInvoices = stats?.paidInvoices || 0;
  const overdueInvoices = stats?.overdueInvoices || 0;
  const leaseExpired = lease?.endDate && new Date(lease.endDate) < new Date();
  const nextDueDate = stats?.nextDueDate;
  const daysUntilDue = stats?.daysUntilDue;

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">
            Welcome back, {user?.name || 'Tenant'} 👋
          </h1>
          <p className="text-sm text-slate-500">Here's your rental summary</p>
        </div>

        {stats?.nextDueDate && (
          <div className={`rounded-xl p-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 ${
            daysUntilDue === 0 ? 'bg-rose-50 border border-rose-200' :
            daysUntilDue <= 3 ? 'bg-amber-50 border border-amber-200' :
            'bg-indigo-50 border border-indigo-200'
          }`}>
            <div className="flex items-center gap-3">
              <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${
                daysUntilDue === 0 ? 'bg-rose-100' :
                daysUntilDue <= 3 ? 'bg-amber-100' :
                'bg-indigo-100'
              }`}>
                <Calendar className={`w-5 h-5 ${
                  daysUntilDue === 0 ? 'text-rose-600' :
                  daysUntilDue <= 3 ? 'text-amber-600' :
                  'text-indigo-600'
                }`} />
              </div>
              <div>
                <p className={`text-sm font-medium ${
                  daysUntilDue === 0 ? 'text-rose-800' :
                  daysUntilDue <= 3 ? 'text-amber-800' :
                  'text-indigo-800'
                }`}>
                  {daysUntilDue === 0 ? '🚨 Rent Due Today!' :
                   daysUntilDue <= 3 ? `⚠️ Rent Due in ${daysUntilDue} Days` :
                   'Next Rent Due'}
                </p>
                <p className={`text-xs ${
                  daysUntilDue === 0 ? 'text-rose-600' :
                  daysUntilDue <= 3 ? 'text-amber-600' :
                  'text-indigo-600'
                }`}>
                  {daysUntilDue === 0 ? 'Please make your payment today' :
                   daysUntilDue <= 3 ? `Due on ${nextDueDate}` :
                   `Due on ${nextDueDate} (${daysUntilDue} days from now)`}
                </p>
              </div>
            </div>
            <Link
              to="/my-payments"
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition text-center"
            >
              View Payments
            </Link>
          </div>
        )}

        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          <StatCard label="Total Invoices" value={totalInvoices} icon={FileText} color="indigo" />
          <StatCard label="Pending" value={pendingInvoices} icon={Clock} color="amber" />
          <StatCard label="Awaiting Approval" value={submittedInvoices} icon={Info} color="blue" />
          <StatCard label="Paid" value={paidInvoices} icon={CheckCircle} color="emerald" />
          <StatCard label="Overdue" value={overdueInvoices} icon={AlertCircle} color="rose" />
          <StatCard 
            label="Next Rent Due" 
            value={daysUntilDue !== undefined && daysUntilDue >= 0 ? `${daysUntilDue}d` : 'N/A'} 
            icon={Calendar} 
            color="purple"
            subtext={nextDueDate ? `Due on ${nextDueDate}` : ''}
          />
        </div>

        {payError && (
          <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-700 flex items-center gap-2">
            <AlertCircle className="w-4 h-4" />
            {payError}
          </div>
        )}

        {overdueInvoices > 0 && (
          <div className="bg-rose-50 border border-rose-200 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-rose-100 rounded-full flex items-center justify-center shrink-0">
                <AlertCircle className="w-5 h-5 text-rose-600" />
              </div>
              <div>
                <p className="text-sm font-medium text-rose-800">Overdue Invoices</p>
                <p className="text-xs text-rose-600">
                  You have {overdueInvoices} overdue invoice(s). Please pay immediately to avoid additional penalties.
                </p>
              </div>
            </div>
          </div>
        )}

        {lease && leaseExpired && (
          <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-amber-100 rounded-full flex items-center justify-center shrink-0">
                <Calendar className="w-5 h-5 text-amber-600" />
              </div>
              <div>
                <p className="text-sm font-medium text-amber-800">Lease Expired</p>
                <p className="text-xs text-amber-600">
                  Your lease expired on {lease.endDate}. Please contact admin to renew.
                </p>
              </div>
            </div>
            <Link to="/profile" className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium rounded-lg transition text-center">
              Contact Admin
            </Link>
          </div>
        )}

        <div className="bg-white rounded-xl border border-slate-100 p-6 shadow-sm">
          <div className="flex items-center gap-2 mb-4">
            <Home className="w-5 h-5 text-indigo-600" />
            <h2 className="text-lg font-semibold text-slate-800">My Lease</h2>
          </div>

          {!hasLease ? (
            <div className="text-center py-8">
              <div className="w-16 h-16 mx-auto bg-slate-100 rounded-full flex items-center justify-center mb-4">
                <Building2 className="w-8 h-8 text-slate-400" />
              </div>
              <p className="text-slate-500">You don't have an active lease yet.</p>
              <p className="text-sm text-slate-400">Contact your administrator for assistance.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
              <div className="bg-slate-50 rounded-lg p-4">
                <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Space</p>
                <p className="text-lg font-bold text-slate-800 mt-1">
                  {lease.space?.unitNumber || 'N/A'}
                </p>
                <p className="text-sm text-slate-500">{lease.space?.category || ''}</p>
              </div>
              <div className="bg-slate-50 rounded-lg p-4">
                <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Monthly Rent</p>
                <p className="text-lg font-bold text-slate-800 mt-1">
                  ETB {Number(lease.monthlyRent).toLocaleString()}
                </p>
                <p className="text-sm text-slate-500">
                  Due on the {lease.dueDay || 1}st of each month
                </p>
              </div>
              <div className="bg-slate-50 rounded-lg p-4">
                <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Lease Period</p>
                <p className="text-sm font-medium text-slate-800 mt-1">
                  {lease.startDate} → {lease.endDate}
                </p>
                <p className="text-xs text-slate-400 mt-1">
                  Started: {new Date(lease.startDate).toLocaleDateString()}
                </p>
                {daysUntilDue !== undefined && daysUntilDue >= 0 && (
                  <p className={`text-xs mt-1 ${
                    daysUntilDue === 0 ? 'text-rose-600 font-medium' :
                    daysUntilDue <= 3 ? 'text-amber-600' :
                    'text-slate-500'
                  }`}>
                    {daysUntilDue === 0 ? '🚨 Rent due today!' :
                     daysUntilDue <= 3 ? `⚠️ Rent due in ${daysUntilDue} days` :
                     `Next rent due: ${daysUntilDue} days`}
                  </p>
                )}
              </div>
              <div className="bg-slate-50 rounded-lg p-4">
                <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Deposit</p>
                <p className="text-lg font-bold text-slate-800 mt-1">
                  ETB {Number(lease.deposit || 0).toLocaleString()}
                </p>
                <p className="text-sm text-slate-500">{lease.initialPaymentMonths || 3} months initial payment</p>
              </div>
              <div className="bg-slate-50 rounded-lg p-4">
                <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Status</p>
                <div className="mt-1">
                  <StatusBadge status={lease.status} />
                </div>
                {daysUntilDue !== undefined && daysUntilDue >= 0 && (
                  <p className="text-xs text-slate-500 mt-2">
                    {daysUntilDue === 0 ? 'Rent due today!' : `${daysUntilDue} days until next rent due`}
                  </p>
                )}
              </div>
            </div>
          )}
        </div>

        <PaymentSummary stats={stats} />

        <div className="bg-white rounded-xl border border-slate-100 shadow-sm overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <FileText className="w-5 h-5 text-indigo-600" />
              <h2 className="text-lg font-semibold text-slate-800">My Invoices</h2>
            </div>
            <span className="text-sm text-slate-500">{invoices.length} total</span>
          </div>

          {invoices.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-slate-500">No invoices yet.</p>
            </div>
          ) : (
            <div className="p-4 space-y-3">
              {invoices.map((inv) => (
                <InvoiceCard
                  key={inv.id}
                  invoice={inv}
                  onPay={handlePay}
                  onUpload={handleReceiptUpload}
                  onDownloadPdf={handleDownloadPdf}
                  paying={payingId}
                  uploading={uploadingId}
                />
              ))}
            </div>
          )}
        </div>

        <PaymentHistory invoices={invoices} />

        <div className="bg-white rounded-xl border border-slate-100 shadow-sm overflow-hidden">
          <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <FileText className="w-5 h-5 text-indigo-600" />
              <h2 className="text-lg font-semibold text-slate-800">My Documents</h2>
            </div>
            <span className="text-sm text-slate-500">{documents.length} files</span>
          </div>

          {loadingDocs ? (
            <div className="text-center py-8">
              <Loader2 className="w-6 h-6 animate-spin text-indigo-600 mx-auto" />
            </div>
          ) : documents.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-slate-500">No documents uploaded yet.</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {documents.map((doc) => (
                <div key={doc.id} className="px-6 py-4 flex items-center justify-between hover:bg-slate-50/60 transition">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-slate-800">{doc.type}</span>
                      <span className="text-slate-300">·</span>
                      <span className="text-sm text-slate-500">{doc.fileName}</span>
                    </div>
                    {doc.description && <p className="text-sm text-slate-500">{doc.description}</p>}
                    <p className="text-xs text-slate-400 mt-1">
                      Uploaded: {new Date(doc.uploadedAt).toLocaleDateString()}
                    </p>
                  </div>
                  <button
                    onClick={() => handleDownload(doc)}
                    className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-medium rounded-lg transition"
                  >
                    Download
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
