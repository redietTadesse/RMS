// src/pages/Dashboards.jsx - COMPLETE FILE
import { useEffect, useState } from 'react';
import {
  PieChart, Pie, Cell,
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, Legend, ResponsiveContainer,
} from 'recharts';
import { dashboardApi, invoicesApi, leasesApi } from '../services/api';
import DashboardLayout from '../Components/Layout/DashboardLayout';
import { Loader2, AlertCircle } from 'lucide-react';

const PIE_COLORS = { Vacant: '#22c55e', Occupied: '#3b82f6' };

function StatCard({ label, value, accent = 'text-slate-800' }) {
  return (
    <div className="bg-white rounded-xl border border-slate-100 p-4 shadow-sm hover:shadow-md transition-all hover:-translate-y-0.5">
      <div>
        <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
        <p className={`mt-1 text-xl font-bold ${accent}`}>{value}</p>
      </div>
    </div>
  );
}

const fmtMoney = (n) => Number(n ?? 0).toLocaleString(undefined, { maximumFractionDigits: 2 });

const fmtDate = (d) => (d ? new Date(d).toLocaleDateString() : '—');

function buildRevenueByMonth(invoices) {
  const months = [];
  const now = new Date();
  for (let i = 5; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    months.push({
      key: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`,
      label: d.toLocaleDateString(undefined, { month: 'short' }),
      revenue: 0,
    });
  }
  for (const inv of invoices) {
    if (inv.status !== 'PAID' || !inv.issueDate) continue;
    const key = inv.issueDate.slice(0, 7);
    const bucket = months.find((m) => m.key === key);
    if (bucket) bucket.revenue += Number(inv.totalAmount ?? 0);
  }
  return months;
}

export default function Dashboards() {
  const [stats, setStats] = useState(null);
  const [invoices, setInvoices] = useState([]);
  const [leases, setLeases] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const [s, inv, ls] = await Promise.all([
          dashboardApi.getStats(),
          invoicesApi.getAll(),
          leasesApi.getAll(),
        ]);
        setStats(s);
        setInvoices(inv);
        setLeases(ls);
      } catch (e) {
        setError(e.message);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  if (loading) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center h-96">
          <Loader2 className="w-8 h-8 animate-spin text-indigo-600" />
          <span className="ml-3 text-slate-500">Loading dashboard…</span>
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

  const occupancyData = [
    { name: 'Vacant', value: stats.vacantSpaces },
    { name: 'Occupied', value: stats.occupiedSpaces },
  ];
  const hasSpaces = stats.totalSpaces > 0;
  const revenueData = buildRevenueByMonth(invoices);
  const recentPayments = invoices.filter((i) => i.status === 'PAID').slice(0, 5);
  const recentLeases = leases.slice(0, 5);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Dashboard</h1>
          <p className="text-sm text-slate-500">Building overview at a glance</p>
        </div>

        <div className="grid grid-cols-2 gap-4 md:grid-cols-4 xl:grid-cols-7">
          <StatCard label="Total Spaces" value={stats.totalSpaces} />
          <StatCard label="Vacant" value={stats.vacantSpaces} accent="text-emerald-600" />
          <StatCard label="Occupied" value={stats.occupiedSpaces} accent="text-blue-600" />
          <StatCard label="Active Leases" value={stats.activeLeases} accent="text-indigo-600" />
          <StatCard label="Pending" value={stats.pendingPayments} accent="text-amber-600" />
          <StatCard label="Overdue" value={stats.overduePayments} accent="text-rose-600" />
          <StatCard label="Penalties" value={fmtMoney(stats.totalPenalties)} accent="text-rose-600" />
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          <div className="bg-white rounded-xl border border-slate-100 p-5 shadow-sm">
            <h2 className="mb-3 text-sm font-semibold text-slate-700">Occupancy</h2>
            {hasSpaces ? (
              <ResponsiveContainer width="100%" height={260}>
                <PieChart>
                  <Pie data={occupancyData} dataKey="value" nameKey="name" innerRadius={60} outerRadius={90} paddingAngle={2}>
                    {occupancyData.map((entry) => (
                      <Cell key={entry.name} fill={PIE_COLORS[entry.name]} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <p className="py-16 text-center text-sm text-slate-400">Add spaces to see occupancy.</p>
            )}
          </div>

          <div className="bg-white rounded-xl border border-slate-100 p-5 shadow-sm">
            <h2 className="mb-3 text-sm font-semibold text-slate-700">Revenue — last 6 months</h2>
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={revenueData}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="label" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip formatter={(v) => `ETB ${fmtMoney(v)}`} />
                <Bar dataKey="revenue" fill="#6366f1" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          <div className="bg-white rounded-xl border border-slate-100 p-5 shadow-sm">
            <h2 className="mb-3 text-sm font-semibold text-slate-700">Recent Payments</h2>
            {recentPayments.length === 0 ? (
              <p className="text-sm text-slate-400">No payments recorded yet.</p>
            ) : (
              <ul className="divide-y divide-slate-100">
                {recentPayments.map((inv) => (
                  <li key={inv.id} className="flex items-center justify-between py-2.5 text-sm">
                    <div>
                      <p className="font-medium text-slate-800">{inv.invoiceNumber}</p>
                      <p className="text-xs text-slate-500">Issued {fmtDate(inv.issueDate)}</p>
                    </div>
                    <span className="font-semibold text-emerald-600">ETB {fmtMoney(inv.totalAmount)}</span>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="bg-white rounded-xl border border-slate-100 p-5 shadow-sm">
            <h2 className="mb-3 text-sm font-semibold text-slate-700">Recent Leases</h2>
            {recentLeases.length === 0 ? (
              <p className="text-sm text-slate-400">No leases yet.</p>
            ) : (
              <ul className="divide-y divide-slate-100">
                {recentLeases.map((l) => (
                  <li key={l.id} className="flex items-center justify-between py-2.5 text-sm">
                    <div>
                      <p className="font-medium text-slate-800">
                        {l.tenant?.name || 'Tenant'} — {l.space?.unitNumber || 'Unit'}
                      </p>
                      <p className="text-xs text-slate-500">{fmtDate(l.startDate)} → {fmtDate(l.endDate)}</p>
                    </div>
                    <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${l.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}`}>
                      {l.status}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}