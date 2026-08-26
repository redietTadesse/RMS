// src/Pages/TenantPayments.jsx
import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { tenantApi, chapaApi } from '../services/api';
import DashboardLayout from '../components/Layout/DashboardLayout';
import { Loader2, CreditCard, Upload, CheckCircle, Clock, AlertCircle, FileText } from 'lucide-react';

function StatusBadge({ status }) {
  const styles = {
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

export default function TenantPayments() {
  const { user } = useAuth();
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [payingId, setPayingId] = useState(null);
  const [uploadingId, setUploadingId] = useState(null);
  const [payError, setPayError] = useState('');

  const loadInvoices = async () => {
    setLoading(true);
    try {
      const data = await tenantApi.getMyInvoices();
      setInvoices(data || []);
    } catch (err) {
      setError(err.message || 'Failed to load invoices');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadInvoices();
  }, []);

  const handleChapaPay = async (invoice) => {
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
      await loadInvoices();
      alert('✅ Receipt uploaded! Admin will review it.');
    } catch (err) {
      setPayError(err.message || 'Could not upload receipt.');
    } finally {
      setUploadingId(null);
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

  const totalPending = invoices
    .filter(i => i.status === 'PENDING' || i.status === 'OVERDUE')
    .reduce((sum, i) => sum + (i.totalAmount || 0), 0);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">My Payments</h1>
          <p className="text-sm text-slate-500">View and pay your invoices</p>
        </div>

        {totalPending > 0 && (
          <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-center gap-3">
            <AlertCircle className="w-5 h-5 text-amber-600 shrink-0" />
            <div>
              <p className="text-sm font-medium text-amber-800">Outstanding Balance</p>
              <p className="text-xs text-amber-600">
                ETB {totalPending.toLocaleString()} due across{' '}
                {invoices.filter(i => i.status === 'PENDING' || i.status === 'OVERDUE').length} invoice(s)
              </p>
            </div>
          </div>
        )}

        {payError && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-700 flex items-center gap-2">
            <AlertCircle className="w-4 h-4" />
            {payError}
          </div>
        )}

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
                    <th className="px-6 py-3 text-right font-medium">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {invoices.map((inv) => (
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
                          <span className="text-xs text-emerald-600 font-medium">Paid ✓</span>
                        ) : inv.status === 'SUBMITTED' ? (
                          <span className="text-xs text-blue-600 font-medium">Awaiting approval</span>
                        ) : (
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => handleChapaPay(inv)}
                              disabled={payingId === inv.id}
                              className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white text-xs font-medium rounded-lg transition inline-flex items-center gap-1.5"
                            >
                              {payingId === inv.id ? (
                                <><Loader2 className="w-3 h-3 animate-spin" /> Starting…</>
                              ) : (
                                <><CreditCard className="w-3 h-3" /> Pay now</>
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
                                onChange={(e) => {
                                  const file = e.target.files[0];
                                  if (file) handleReceiptUpload(inv, file);
                                }}
                              />
                            </label>
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}