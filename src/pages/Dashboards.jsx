import { useEffect, useState } from 'react'
import {
  PieChart, Pie, Cell,
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, Legend, ResponsiveContainer,
} from 'recharts'
import { dashboardApi, invoicesApi, leasesApi } from '../services/api'

const PIE_COLORS = { Vacant: '#22c55e', Occupied: '#3b82f6' }

function StatCard({ label, value, accent = 'text-gray-900' }) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4">
      <p className="text-xs font-medium uppercase tracking-wide text-gray-500">{label}</p>
      <p className={`mt-1 text-2xl font-bold ${accent}`}>{value}</p>
    </div>
  )
}

const fmtMoney = (n) =>
  Number(n ?? 0).toLocaleString(undefined, { maximumFractionDigits: 2 })

const fmtDate = (d) => (d ? new Date(d).toLocaleDateString() : '—')

/** Build "last 6 months" revenue from PAID invoices (rent + penalty). */
function buildRevenueByMonth(invoices) {
  const months = []
  const now = new Date()
  for (let i = 5; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    months.push({
      key: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`,
      label: d.toLocaleDateString(undefined, { month: 'short' }),
      revenue: 0,
    })
  }
  for (const inv of invoices) {
    if (inv.status !== 'PAID' || !inv.issueDate) continue
    const key = inv.issueDate.slice(0, 7) // "YYYY-MM"
    const bucket = months.find((m) => m.key === key)
    if (bucket) bucket.revenue += Number(inv.totalAmount ?? 0)
  }
  return months
}

export default function Dashboards() {
  const [stats, setStats] = useState(null)
  const [invoices, setInvoices] = useState([])
  const [leases, setLeases] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      setError('')
      try {
        const [s, inv, ls] = await Promise.all([
          dashboardApi.getStats(),
          invoicesApi.getAll(),
          leasesApi.getAll(),
        ])
        setStats(s)
        setInvoices(inv)
        setLeases(ls)
      } catch (e) {
        setError(e.message)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) {
    return <div className="p-6 text-gray-400">Loading dashboard…</div>
  }

  if (error) {
    return (
      <div className="p-6">
        <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">
          {error} — is the backend running on port 8080?
        </div>
      </div>
    )
  }

  const occupancyData = [
    { name: 'Vacant', value: stats.vacantSpaces },
    { name: 'Occupied', value: stats.occupiedSpaces },
  ]
  const hasSpaces = stats.totalSpaces > 0
  const revenueData = buildRevenueByMonth(invoices)

  // Recent activity: last 5 paid invoices (= payments) and last 5 leases.
  // Both endpoints return newest first.
  const recentPayments = invoices.filter((i) => i.status === 'PAID').slice(0, 5)
  const recentLeases = leases.slice(0, 5)

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-sm text-gray-500">Building overview at a glance</p>
      </div>

      {/* Stats cards */}
      <div className="mb-6 grid grid-cols-2 gap-4 md:grid-cols-4 xl:grid-cols-7">
        <StatCard label="Total Spaces" value={stats.totalSpaces} />
        <StatCard label="Vacant" value={stats.vacantSpaces} accent="text-green-600" />
        <StatCard label="Occupied" value={stats.occupiedSpaces} accent="text-blue-600" />
        <StatCard label="Active Leases" value={stats.activeLeases} />
        <StatCard label="Pending Payments" value={stats.pendingPayments} accent="text-amber-600" />
        <StatCard label="Overdue Payments" value={stats.overduePayments} accent="text-red-600" />
        <StatCard label="Total Penalties" value={fmtMoney(stats.totalPenalties)} accent="text-red-600" />
      </div>

      {/* Charts */}
      <div className="mb-6 grid gap-4 lg:grid-cols-2">
        {/* Occupancy pie */}
        <div className="rounded-xl border border-gray-200 bg-white p-4">
          <h2 className="mb-2 text-sm font-semibold text-gray-700">Occupancy</h2>
          {hasSpaces ? (
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie
                  data={occupancyData}
                  dataKey="value"
                  nameKey="name"
                  innerRadius={60}
                  outerRadius={90}
                  paddingAngle={2}
                >
                  {occupancyData.map((entry) => (
                    <Cell key={entry.name} fill={PIE_COLORS[entry.name]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <p className="py-16 text-center text-sm text-gray-400">
              Add spaces to see occupancy.
            </p>
          )}
        </div>

        {/* Revenue bar chart */}
        <div className="rounded-xl border border-gray-200 bg-white p-4">
          <h2 className="mb-2 text-sm font-semibold text-gray-700">
            Revenue — last 6 months (paid invoices)
          </h2>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={revenueData}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip formatter={(v) => fmtMoney(v)} />
              <Bar dataKey="revenue" fill="#3b82f6" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Recent activity */}
      <div className="grid gap-4 lg:grid-cols-2">
        <div className="rounded-xl border border-gray-200 bg-white p-4">
          <h2 className="mb-3 text-sm font-semibold text-gray-700">Recent Payments</h2>
          {recentPayments.length === 0 ? (
            <p className="text-sm text-gray-400">No payments recorded yet.</p>
          ) : (
            <ul className="divide-y divide-gray-100">
              {recentPayments.map((inv) => (
                <li key={inv.id} className="flex items-center justify-between py-2 text-sm">
                  <div>
                    <p className="font-medium text-gray-800">
                      {inv.invoiceNumber}
                      {inv.lease?.tenant?.name ? ` — ${inv.lease.tenant.name}` : ''}
                    </p>
                    <p className="text-xs text-gray-500">Issued {fmtDate(inv.issueDate)}</p>
                  </div>
                  <span className="font-semibold text-green-600">
                    {fmtMoney(inv.totalAmount)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="rounded-xl border border-gray-200 bg-white p-4">
          <h2 className="mb-3 text-sm font-semibold text-gray-700">Recent Leases</h2>
          {recentLeases.length === 0 ? (
            <p className="text-sm text-gray-400">No leases yet.</p>
          ) : (
            <ul className="divide-y divide-gray-100">
              {recentLeases.map((l) => (
                <li key={l.id} className="flex items-center justify-between py-2 text-sm">
                  <div>
                    <p className="font-medium text-gray-800">
                      {l.tenant?.name || 'Tenant'} — {l.space?.unitNumber || 'Unit'}
                    </p>
                    <p className="text-xs text-gray-500">
                      {fmtDate(l.startDate)} → {fmtDate(l.endDate)}
                    </p>
                  </div>
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                      l.status === 'ACTIVE'
                        ? 'bg-green-100 text-green-700'
                        : 'bg-gray-100 text-gray-500'
                    }`}
                  >
                    {l.status}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  )
}
