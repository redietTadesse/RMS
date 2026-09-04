// src/Pages/Reports.jsx
import { useEffect, useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
} from 'recharts';
import { reportsApi } from '../services/api';
import { exportToCsv } from '../utils/csvExport';
import DashboardLayout from '../components/Layout/DashboardLayout';   // ← fixed path (lowercase 'components')
import { Loader2, Download, AlertCircle } from 'lucide-react';

const PIE_COLORS = { Vacant: '#22c55e', Occupied: '#3b82f6' };
const fmtMoney = (n) => `ETB ${Number(n ?? 0).toLocaleString(undefined, { maximumFractionDigits: 2 })}`;

function SectionCard({ title, action, children }) {
  return (
    <div className="bg-white rounded-xl border border-slate-100 p-5 shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-sm font-semibold text-slate-700">{title}</h2>
        {action}
      </div>
      {children}
    </div>
  );
}

function ExportButton({ onClick }) {
  return (
    <button
      onClick={onClick}
      className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-indigo-600 hover:bg-indigo-50 rounded-lg transition"
    >
      <Download className="w-3.5 h-3.5" /> Export CSV
    </button>
  );
}

export default function Reports() {
  const [revenue, setRevenue] = useState([]);
  const [outstanding, setOutstanding] = useState([]);
  const [occupancy, setOccupancy] = useState(null);
  const [leaseExpiry, setLeaseExpiry] = useState([]);
  const [expiryFilter, setExpiryFilter] = useState('90');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const [rev, out, occ, exp] = await Promise.all([
          reportsApi.getRevenue(6),
          reportsApi.getOutstandingBalance(),
          reportsApi.getOccupancy(),
          reportsApi.getLeaseExpiry(90),
        ]);
        if (!cancelled) {
          setRevenue(rev);
          setOutstanding(out);
          setOccupancy(occ);
          setLeaseExpiry(exp);
        }
      } catch (e) {
        if (!cancelled) setError(e.message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  const filteredExpiry = leaseExpiry.filter((l) => Number(l.daysRemaining) <= Number(expiryFilter));

  if (loading) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center h-96">
          <Loader2 className="w-8 h-8 animate-spin text-indigo-600" />
          <span className="ml-3 text-slate-500">Loading reports…</span>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Reports</h1>
          <p className="text-sm text-slate-500">Revenue, occupancy, balances, and lease expiries</p>
        </div>

        {error && (
          <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 border border-red-200 flex items-center gap-2">
            <AlertCircle className="w-4 h-4" /> {error}
          </div>
        )}

        {/* Revenue Report */}
        <SectionCard
          title="Revenue — last 6 months"
          action={<ExportButton onClick={() => exportToCsv('revenue-report', revenue.map(r => ({ Month: r.label, Revenue: r.revenue })))} />}
        >
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={revenue}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip formatter={(v) => fmtMoney(v)} />
              <Bar dataKey="revenue" fill="#6366f1" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
          <table className="w-full text-sm mt-4">
            <thead>
              <tr className="text-left text-slate-500 border-b border-slate-100">
                <th className="py-2 font-medium">Month</th>
                <th className="py-2 font-medium text-right">Revenue</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {revenue.map((r) => (
                <tr key={r.month}>
                  <td className="py-2 text-slate-700">{r.label}</td>
                  <td className="py-2 text-right font-medium text-slate-800">{fmtMoney(r.revenue)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </SectionCard>

        <div className="grid gap-6 lg:grid-cols-2">
          {/* Occupancy Report */}
          <SectionCard title="Occupancy">
            {occupancy && occupancy.totalSpaces > 0 ? (
              <>
                <ResponsiveContainer width="100%" height={220}>
                  <PieChart>
                    <Pie
                      data={[
                        { name: 'Vacant', value: occupancy.vacantSpaces },
                        { name: 'Occupied', value: occupancy.occupiedSpaces },
                      ]}
                      dataKey="value"
                      nameKey="name"
                      innerRadius={55}
                      outerRadius={85}
                      paddingAngle={2}
                    >
                      {['Vacant', 'Occupied'].map((name) => (
                        <Cell key={name} fill={PIE_COLORS[name]} />
                      ))}
                    </Pie>
                    <Tooltip />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
                <div className="grid grid-cols-2 gap-3 mt-3 text-sm">
                  <div className="bg-emerald-50 rounded-lg p-3">
                    <p className="text-emerald-700 font-semibold">{occupancy.vacantSpaces} vacant</p>
                    <p className="text-emerald-600 text-xs">{occupancy.vacantRate.toFixed(1)}%</p>
                  </div>
                  <div className="bg-blue-50 rounded-lg p-3">
                    <p className="text-blue-700 font-semibold">{occupancy.occupiedSpaces} occupied</p>
                    <p className="text-blue-600 text-xs">{occupancy.occupiedRate.toFixed(1)}%</p>
                  </div>
                </div>
              </>
            ) : (
              <p className="py-16 text-center text-sm text-slate-400">No spaces yet.</p>
            )}
          </SectionCard>

          {/* Outstanding Balance Report */}
          <SectionCard
            title="Outstanding Balances"
            action={<ExportButton onClick={() => exportToCsv('outstanding-balance', outstanding.map(o => ({ Tenant: o.tenantName, Phone: o.phone, UnpaidInvoices: o.unpaidInvoiceCount, TotalOwed: o.totalOwed })))} />}
          >
            {outstanding.length === 0 ? (
              <p className="py-8 text-center text-sm text-slate-400">No outstanding balances 🎉</p>
            ) : (
              <ul className="divide-y divide-slate-100 max-h-64 overflow-y-auto">
                {outstanding.map((o) => (
                  <li key={o.tenantId} className="flex items-center justify-between py-2.5 text-sm">
                    <div>
                      <p className="font-medium text-slate-800">{o.tenantName}</p>
                      <p className="text-xs text-slate-500">{o.unpaidInvoiceCount} unpaid invoice{o.unpaidInvoiceCount !== 1 ? 's' : ''}</p>
                    </div>
                    <span className="font-semibold text-rose-600">{fmtMoney(o.totalOwed)}</span>
                  </li>
                ))}
              </ul>
            )}
          </SectionCard>
        </div>

        {/* Lease Expiry Report */}
        <SectionCard
          title="Lease Expiry"
          action={<ExportButton onClick={() => exportToCsv('lease-expiry', filteredExpiry.map(l => ({ Tenant: l.tenantName, Space: l.spaceUnitNumber, EndDate: l.endDate, DaysRemaining: l.daysRemaining })))} />}
        >
          <div className="flex gap-2 mb-4">
            {['30', '60', '90'].map((d) => (
              <button
                key={d}
                onClick={() => setExpiryFilter(d)}
                className={`px-4 py-1.5 rounded-lg text-sm font-medium transition ${
                  expiryFilter === d ? 'bg-indigo-600 text-white shadow-sm' : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
                }`}
              >
                Within {d} days
              </button>
            ))}
          </div>
          {filteredExpiry.length === 0 ? (
            <p className="py-8 text-center text-sm text-slate-400">No leases expiring in this window.</p>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-slate-500 border-b border-slate-100">
                  <th className="py-2 font-medium">Tenant</th>
                  <th className="py-2 font-medium">Space</th>
                  <th className="py-2 font-medium">End Date</th>
                  <th className="py-2 font-medium text-right">Days Left</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {filteredExpiry.map((l) => (
                  <tr key={l.leaseId}>
                    <td className="py-2 text-slate-700">{l.tenantName}</td>
                    <td className="py-2 text-slate-700">{l.spaceUnitNumber}</td>
                    <td className="py-2 text-slate-600">{l.endDate}</td>
                    <td className={`py-2 text-right font-medium ${l.daysRemaining <= 30 ? 'text-rose-600' : l.daysRemaining <= 60 ? 'text-amber-600' : 'text-slate-600'}`}>
                      {l.daysRemaining}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </SectionCard>
      </div>
    </DashboardLayout>
  );
}