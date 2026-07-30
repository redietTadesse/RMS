import { mockSpaces, mockTenants, mockLeases, mockInvoices } from '../data/mockData'
import DashboardLayout from '../components/Layout/DashboardLayout'
import StatsCard from '../components/Dashboard/StatsCard'
import { Building2, DoorOpen, DoorClosed, FileText, TrendingUp, DollarSign, AlertCircle } from 'lucide-react'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts'

function Dashboard() {
  const totalSpaces = mockSpaces.length
  const vacantSpaces = mockSpaces.filter(s => s.status === 'Vacant').length
  const occupiedSpaces = mockSpaces.filter(s => s.status === 'Occupied').length
  const activeLeases = mockLeases.filter(l => l.status === 'Active').length
  const pendingPayments = mockInvoices.filter(i => i.status === 'Pending').length
  const totalRevenue = mockInvoices.filter(i => i.status === 'Paid').reduce((sum, i) => sum + i.amount, 0)

  const occupancyData = [
    { name: 'Occupied', value: occupiedSpaces, color: '#6366f1' },
    { name: 'Vacant', value: vacantSpaces, color: '#e0e7ff' },
  ]

  const revenueData = mockInvoices.map(inv => ({
    name: inv.tenant.split(' ')[0],
    amount: inv.amount,
  }))

  return (
    <DashboardLayout>
      {/* Greeting */}
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-slate-800">Welcome back, Admin 👋</h2>
        <p className="text-slate-500 text-sm mt-1">Here's what's happening with your properties today.</p>
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <StatsCard title="Total Spaces" value={totalSpaces} icon={Building2} gradient="from-blue-500 to-indigo-600" subtext={`${vacantSpaces} vacant`} />
        <StatsCard title="Vacant Spaces" value={vacantSpaces} icon={DoorOpen} gradient="from-emerald-500 to-teal-600" subtext="Ready to lease" />
        <StatsCard title="Occupied" value={occupiedSpaces} icon={DoorClosed} gradient="from-amber-500 to-orange-600" subtext={`${Math.round(occupiedSpaces/totalSpaces*100)}% occupancy`} />
        <StatsCard title="Active Leases" value={activeLeases} icon={FileText} gradient="from-purple-500 to-fuchsia-600" subtext="Currently running" />
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        {/* Revenue chart */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-slate-100 shadow-sm p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="font-bold text-slate-800">Revenue Overview</h3>
              <p className="text-sm text-slate-500">Monthly invoice amounts by tenant</p>
            </div>
            <div className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-50 text-emerald-700 rounded-lg text-sm font-medium">
              <TrendingUp className="w-4 h-4" />
              +12.5%
            </div>
          </div>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={revenueData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />
              <XAxis dataKey="name" tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 12, fill: '#64748b' }} axisLine={false} tickLine={false} tickFormatter={(v) => `${v/1000}k`} />
              <Tooltip 
                contentStyle={{ 
                  borderRadius: '12px', 
                  border: '1px solid #e2e8f0', 
                  boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
                  fontSize: '13px'
                }}
                formatter={(v) => [`ETB ${v.toLocaleString()}`, 'Amount']}
              />
              <Bar dataKey="amount" fill="#6366f1" radius={[6, 6, 0, 0]} barSize={48} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Occupancy pie */}
        <div className="bg-white rounded-2xl border border-slate-100 shadow-sm p-6">
          <h3 className="font-bold text-slate-800 mb-1">Space Occupancy</h3>
          <p className="text-sm text-slate-500 mb-4">Current distribution</p>
          <ResponsiveContainer width="100%" height={220}>
            <PieChart>
              <Pie 
                data={occupancyData} 
                cx="50%" cy="50%" 
                innerRadius={55} 
                outerRadius={85} 
                paddingAngle={3}
                dataKey="value"
              >
                {occupancyData.map((entry, i) => (
                  <Cell key={i} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip contentStyle={{ borderRadius: '12px', fontSize: '13px' }} />
            </PieChart>
          </ResponsiveContainer>
          <div className="space-y-2 mt-2">
            <div className="flex items-center justify-between text-sm">
              <span className="flex items-center gap-2 text-slate-600">
                <span className="w-3 h-3 bg-indigo-500 rounded-full"></span>Occupied
              </span>
              <span className="font-semibold text-slate-800">{occupiedSpaces}</span>
            </div>
            <div className="flex items-center justify-between text-sm">
              <span className="flex items-center gap-2 text-slate-600">
                <span className="w-3 h-3 bg-indigo-100 rounded-full"></span>Vacant
              </span>
              <span className="font-semibold text-slate-800">{vacantSpaces}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom row: financial summary + recent activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="bg-gradient-to-br from-slate-900 to-slate-800 rounded-2xl p-6 text-white shadow-lg">
          <div className="flex items-center justify-between mb-4">
            <div className="w-10 h-10 bg-white/10 rounded-lg flex items-center justify-center">
              <DollarSign className="w-5 h-5" />
            </div>
            <span className="text-xs bg-emerald-500/20 text-emerald-300 px-2 py-1 rounded-full font-medium">Collected</span>
          </div>
          <p className="text-sm text-slate-400">Total Revenue</p>
          <p className="text-3xl font-bold mt-1">ETB {totalRevenue.toLocaleString()}</p>
          <div className="mt-4 pt-4 border-t border-white/10 flex items-center justify-between text-sm">
            <span className="text-slate-400">Pending payments</span>
            <span className="font-semibold text-amber-300">{pendingPayments} invoices</span>
          </div>
        </div>

        <div className="lg:col-span-2 bg-white rounded-2xl border border-slate-100 shadow-sm p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="font-bold text-slate-800">Recent Payments</h3>
            <button className="text-sm text-indigo-600 hover:text-indigo-700 font-medium">View all</button>
          </div>
          <div className="space-y-3">
            {mockInvoices.slice(0, 4).map(invoice => (
              <div key={invoice.id} className="flex items-center justify-between p-3 rounded-xl hover:bg-slate-50 transition-colors">
                <div className="flex items-center gap-3">
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center text-white font-semibold text-sm
                    ${invoice.status === 'Paid' ? 'bg-gradient-to-br from-emerald-400 to-teal-500' : 'bg-gradient-to-br from-amber-400 to-orange-500'}`}>
                    {invoice.tenant.charAt(0)}
                  </div>
                  <div>
                    <p className="font-medium text-slate-800 text-sm">{invoice.tenant}</p>
                    <p className="text-xs text-slate-500">Due {invoice.dueDate}</p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="font-semibold text-slate-800 text-sm">ETB {invoice.amount.toLocaleString()}</p>
                  <span className={`text-xs font-medium ${invoice.status === 'Paid' ? 'text-emerald-600' : 'text-amber-600'}`}>
                    {invoice.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}

export default Dashboard