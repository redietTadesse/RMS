// src/Components/Accounting/AccountingSummary.jsx
import { useEffect, useState } from 'react';
import {
  DollarSign, Receipt,
  Loader2, AlertCircle, Banknote, CreditCard
} from 'lucide-react';
import { accountingApi } from '../../services/api';
import DashboardLayout from '../Layout/DashboardLayout';
import { format, subMonths, startOfMonth } from 'date-fns';

const fmtMoney = (n) =>
  `ETB ${Number(n ?? 0).toLocaleString(undefined, { maximumFractionDigits: 2 })}`;

const PERIODS = [
  { label: 'This Month', value: 'this-month' },
  { label: 'Last 3 Months', value: 'last-3' },
  { label: 'Last 6 Months', value: 'last-6' },
  { label: 'All Time', value: 'all-time' },
];

function SummaryCard({ icon: Icon, color, label, value, sub }) {
  const bgColors = {
    emerald: 'bg-emerald-100 text-emerald-600',
    purple: 'bg-purple-100 text-purple-600',
    rose: 'bg-rose-100 text-rose-600',
    amber: 'bg-amber-100 text-amber-600',
    blue: 'bg-blue-100 text-blue-600',
    indigo: 'bg-indigo-100 text-indigo-600',
  };

  return (
    <div className="bg-white rounded-xl border border-slate-100 p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${bgColors[color]}`}>
          <Icon className="w-5 h-5" />
        </div>
        <div>
          <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">{label}</p>
          <p className="text-lg font-bold text-slate-800 mt-0.5">{value}</p>
          {sub && <p className="text-xs text-slate-400">{sub}</p>}
        </div>
      </div>
    </div>
    );
}

export default function AccountingSummary() {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [period, setPeriod] = useState('all-time');

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        let start = null, end = null;
        if (period === 'this-month') {
          start = format(startOfMonth(new Date()), 'yyyy-MM-dd');
          end = format(new Date(), 'yyyy-MM-dd');
        } else if (period === 'last-3') {
          start = format(subMonths(new Date(), 2), 'yyyy-MM-dd');
          end = format(new Date(), 'yyyy-MM-dd');
        } else if (period === 'last-6') {
          start = format(subMonths(new Date(), 5), 'yyyy-MM-dd');
          end = format(new Date(), 'yyyy-MM-dd');
        }
        const data = await accountingApi.getSummary(start, end);
        if (!cancelled) setSummary(data);
      } catch (err) {
        if (!cancelled) setError(err.message || 'Failed to load accounting summary');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [period]);

  if (loading) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center h-96">
          <Loader2 className="w-8 h-8 animate-spin text-indigo-600" />
          <span className="ml-3 text-slate-500">Loading accounting summary…</span>
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

  const totalRentInclVat = Number(summary?.totalRent || 0) + Number(summary?.totalVat || 0);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Accounting Summary</h1>
            <p className="text-sm text-slate-500">
              Full financial transparency — rent, VAT, penalties, and withholding
            </p>
          </div>
          <select
            value={period}
            onChange={(e) => setPeriod(e.target.value)}
            className="px-3 py-1.5 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-indigo-300"
          >
            {PERIODS.map((p) => (
              <option key={p.value} value={p.value}>{p.label}</option>
            ))}
          </select>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          <SummaryCard icon={Banknote} color="emerald" label="Total Revenue"
            value={fmtMoney(summary?.totalRevenue)} sub={summary?.paidInvoicesCount + ' paid invoices'} />
          <SummaryCard icon={Receipt} color="purple" label="VAT (15%)"
            value={fmtMoney(summary?.totalVat)} sub="collected" />
          <SummaryCard icon={CreditCard} color="rose" label="Penalties"
            value={fmtMoney(summary?.totalPenalties)} sub={summary?.penaltyInvoicesCount + ' invoices'} />
          <SummaryCard icon={Receipt} color="amber" label="Withholding Tax"
            value={fmtMoney(summary?.totalWithholdingTax)} sub={summary?.withholdingInvoicesCount + ' invoices'} />
          <SummaryCard icon={CreditCard} color="blue" label="Amount to Collect"
            value={fmtMoney(summary?.amountToCollect)} sub="(after withholding)" />
                    <SummaryCard icon={DollarSign} color="indigo" label="Total Rent (excl. VAT)"
            value={fmtMoney(summary?.totalRent)} />
        </div>

        {/* Financial Breakdown Table */}
        {summary && (
          <div className="bg-white rounded-xl border border-slate-100 shadow-sm overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-100">
              <h2 className="text-lg font-semibold text-slate-800">Financial Breakdown</h2>
              <p className="text-xs text-slate-400 mt-1">
                Period: {period === 'all-time' ? 'All time' : period}
              </p>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-50">
                  <tr className="text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                    <th className="px-6 py-3">Category</th>
                    <th className="px-6 py-3 text-right">Amount (ETB)</th>
                    <th className="px-6 py-3 text-right"># Invoices</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  <tr>
                    <td className="px-6 py-3 text-slate-700">Total Rent (excl. VAT)</td>
                    <td className="px-6 py-3 text-right font-medium">{fmtMoney(summary.totalRent)}</td>
                    <td className="px-6 py-3 text-right text-slate-500">—</td>
                  </tr>
                  <tr className="bg-slate-50/50">
                    <td className="px-6 py-3 pl-8 text-slate-600">└ VAT (15%)</td>
                    <td className="px-6 py-3 text-right text-slate-600">{fmtMoney(summary.totalVat)}</td>
                    <td className="px-6 py-3 text-right text-slate-400">{summary.vatInvoicesCount}</td>
                  </tr>
                  <tr>
                    <td className="px-6 py-3 text-slate-700">Total Rent (incl. VAT)</td>
                    <td className="px-6 py-3 text-right font-medium">{fmtMoney(totalRentInclVat)}</td>
                    <td className="px-6 py-3 text-right text-slate-500">—</td>
                  </tr>
                  <tr className="bg-rose-50/50">
                    <td className="px-6 py-3 text-rose-700">└ Late Penalties</td>
                    <td className="px-6 py-3 text-right text-rose-700">{fmtMoney(summary.totalPenalties)}</td>
                    <td className="px-6 py-3 text-right text-rose-500">{summary.penaltyInvoicesCount}</td>
                  </tr>
                  <tr className="bg-purple-50/50">
                    <td className="px-6 py-3 text-purple-700">└ Withholding Tax (3%)</td>
                    <td className="px-6 py-3 text-right text-purple-700">{fmtMoney(summary.totalWithholdingTax)}</td>
                    <td className="px-6 py-3 text-right text-purple-500">{summary.withholdingInvoicesCount}</td>
                  </tr>
                  <tr className="border-t-2 border-slate-200">
                    <td className="px-6 py-4 font-bold text-indigo-800">TOTAL REVENUE (Rent + Penalties)</td>
                    <td className="px-6 py-4 text-right font-bold text-indigo-800">{fmtMoney(summary.totalRevenue)}</td>
                    <td className="px-6 py-4 text-right text-slate-500">{summary.paidInvoicesCount}</td>
                  </tr>
                  <tr className="bg-emerald-50/50">
                    <td className="px-6 py-4 font-bold text-emerald-800">AMOUNT TO COLLECT (after withholding)</td>
                    <td className="px-6 py-4 text-right font-bold text-emerald-800">{fmtMoney(summary.amountToCollect)}</td>
                    <td className="px-6 py-4 text-right text-slate-500">—</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
