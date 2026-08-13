// src/pages/TenantDashboard.jsx - UPDATED WITH DOCUMENTS
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Building2, FileText, CreditCard, AlertCircle, Upload,
  CheckCircle, Clock, Loader2, Calendar, Home,
  DollarSign
} from 'lucide-react';
import DashboardLayout from '../components/Layout/DashboardLayout';
import { tenantApi, chapaApi, documentsApi } from '../services/api';
import { useAuth } from '../context/AuthContext';

function StatCard({ label, value, icon: Icon, color = 'indigo', subtext }) {
  const colors = {
    indigo: 'bg-indigo-100 text-indigo-600',
    emerald: 'bg-emerald-100 text-emerald-600',
    amber: 'bg-amber-100 text-amber-600',
    rose: 'bg-rose-100 text-rose-600',
    blue: 'bg-blue-100 text-blue-600',
    purple: 'bg-purple-100 text-purple-600',
  };

  return (
    <div className="bg-white rounded-xl border border-slate-100 p-5 shadow-sm hover:shadow-md transition-all hover:-translate-y-0.5">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-slate-500 font-medium">{label}</p>
          <p className="text-2xl font-bold text-slate-800 mt-2">{value}</p>
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

export default function TenantDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [lease, setLease] = useState(null);
  const [invoices, setInvoices] = useState([]);
  const [stats, setStats] = useState(null);
  const [payingId, setPayingId] = useState(null);
  const [payError, setPayError] = useState('');
  const [uploadingId, setUploadingId] = useState(null);

  // ✅ NEW: Documents state
  const [documents, setDocuments] = useState([]);
  const [loadingDocs, setLoadingDocs] = useState(false);

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
    const [invoicesData, statsData] = await Promise.all([
      tenantApi.getMyInvoices(),
      tenantApi.getStats(),
    ]);
    setInvoices(invoicesData || []);
    setStats(statsData);
  };

  const handlePay = async (invoice) => {
    setPayError('');
    setPayingId(invoice.id);
    try {
      const res = await chapaApi.initialize(invoice.id, {
        firstName: user?.name?.split(' ')[0] || 'Tenant',
        lastName: user?.name?.split(' ').slice(1).join(' ') || 'User',
        email: lease?.tenant?.email || user?.email || 'tenant@example.com',
        phoneNumber: lease?.tenant?.phone || '0900000000',
      });
      const url = res?.checkoutUrl || res?.checkout_url || res?.data?.checkout_url;
      if (url) {
        window.location.href = url;
      } else {
        setPayError('Payment started but no checkout link was returned. Contact admin.');
        setPayingId(null);
      }
    } catch (err) {
      setPayError(err.message || 'Could not start payment. Please try again.');
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
    } catch (err) {
      setPayError(err.message || 'Could not upload receipt. Please try again.');
    } finally {
      setUploadingId(null);
    }
  };

  // ✅ NEW: Load documents for tenant
  const loadDocuments = async () => {
    setLoadingDocs(true);
    try {
      const data = await documentsApi.getMyDocuments();
      setDocuments(data || []);
    } catch (err) {
      console.error('Failed to load documents:', err);
    } finally {
      setLoadingDocs(false);
    }
  };

  useEffect(() => {
    if (user && user.role === 'TENANT') {
      loadDocuments();
    }
  }, [user]);

  // ✅ NEW: Handle document download
  const handleDownload = async (doc) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`http://localhost:8080/api/documents/download/${doc.id}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!response.ok) {
        throw new Error(`Download failed: ${response.status}`);
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = doc.fileName || 'document';
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);

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
  const paidInvoices = stats?.paidInvoices || 0;
  const overdueInvoices = stats?.overdueInvoices || 0;
  const leaseExpired = lease?.endDate && new Date(lease.endDate) < new Date();

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold text-slate-800">
            Welcome back, {user?.name || 'Tenant'} 👋
          </h1>
          <p className="text-sm text-slate-500">Here's your rental summary</p>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard label="Total Invoices" value={totalInvoices} icon={FileText} color="indigo" />
          <StatCard label="Pending" value={pendingInvoices} icon={Clock} color="amber" />
          <StatCard label="Paid" value={paidInvoices} icon={CheckCircle} color="emerald" />
          <StatCard label="Overdue" value={overdueInvoices} icon={AlertCircle} color="rose" />
        </div>

        {overdueInvoices > 0 && (
          <div className="bg-rose-50 border border-rose-200 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-rose-100 rounded-full flex items-center justify-center shrink-0">
                <AlertCircle className="w-5 h-5 text-rose-600" />
              </div>
              <div>
                <p className="text-sm font-medium text-rose-800">Overdue Invoices</p>
                <p className="text-xs text-rose-600">
                  You have {overdueInvoices} overdue invoice(s). Please pay immediately to avoid penalties.
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
            <Link
              to="/profile"
              className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium rounded-lg transition text-center"
            >
              Contact Admin
            </Link>
          </div>
        )}

        {/* Lease Card */}
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
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
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
                <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Deposit</p>
                <p className="text-lg font-bold text-slate-800 mt-1">
                  ETB {Number(lease.deposit || 0).toLocaleString()}
                </p>
                <p className="text-sm text-slate-500">{lease.prepaymentMonths || 3} months prepayment</p>
              </div>
              <div className="bg-slate-50 rounded-lg p-4">
                <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Status</p>
                <div className="mt-1">
                  <StatusBadge status={lease.status} />
                </div>
                <div className="flex items-center gap-2 mt-2 text-sm text-slate-500">
                  <Calendar className="w-3 h-3" />
                  <span>{lease.startDate} → {lease.endDate}</span>
                </div>
              </div>
            </div>
          )}
        </div>

        {payError && (
          <div className="bg-rose-50 border border-rose-200 rounded-lg px-4 py-3 text-sm text-rose-700 flex items-center gap-2">
            <AlertCircle className="w-4 h-4" /> {payError}
          </div>
        )}

        {/* My Invoices */}
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
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-50">
                  <tr className="text-left text-slate-500">
                    <th className="px-6 py-3 font-medium">Invoice #</th>
                    <th className="px-6 py-3 font-medium">Amount</th>
                    <th className="px-6 py-3 font-medium">Due Date</th>
                    <th className="px-6 py-3 font-medium">Status</th>
                    <th className="px-6 py-3 font-medium text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {invoices.slice(0, 10).map((inv) => (
                    <tr key={inv.id} className="hover:bg-slate-50/60 transition">
                      <td className="px-6 py-3 font-medium text-slate-800">{inv.invoiceNumber}</td>
                      <td className="px-6 py-3 font-semibold text-slate-800">
                        ETB {Number(inv.totalAmount).toLocaleString()}
                      </td>
                      <td className="px-6 py-3 text-slate-600">{inv.dueDate}</td>
                      <td className="px-6 py-3">
                        <StatusBadge status={inv.status} />
                      </td>
                      <td className="px-6 py-3 text-right">
                        {inv.status === 'PAID' ? (
                          <span className="text-xs text-emerald-600 font-medium">Paid</span>
                        ) : inv.status === 'SUBMITTED' ? (
                          <span className="text-xs text-blue-600 font-medium">Awaiting approval</span>
                        ) : (
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => handlePay(inv)}
                              disabled={payingId === inv.id}
                              className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white text-xs font-medium rounded-lg transition inline-flex items-center gap-1.5"
                            >
                              {payingId === inv.id ? (
                                <><Loader2 className="w-3 h-3 animate-spin" /> Starting…</>
                              ) : (
                                <><CreditCard className="w-3 h-3" /> Pay online</>
                              )}
                            </button>
                            <label className="px-3 py-1.5 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-medium rounded-lg transition inline-flex items-center gap-1.5 cursor-pointer">
                              {uploadingId === inv.id ? (
                                <><Loader2 className="w-3 h-3 animate-spin" /> Uploading…</>
                              ) : (
                                <><Upload className="w-3 h-3" /> Upload receipt</>
                              )}
                              <input
                                type="file"
                                accept="image/*,.pdf"
                                className="hidden"
                                disabled={uploadingId === inv.id}
                                onChange={(e) => handleReceiptUpload(inv, e.target.files[0])}
                              />
                            </label>
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {invoices.length > 10 && (
                <div className="px-6 py-3 bg-slate-50 border-t border-slate-100 text-sm text-slate-500">
                  Showing 10 of {invoices.length} invoices
                </div>
              )}
            </div>
          )}
        </div>

        {/* ✅ NEW: My Documents */}
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
                    {doc.description && (
                      <p className="text-sm text-slate-500">{doc.description}</p>
                    )}
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