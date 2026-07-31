import { useState } from 'react'
import { mockSpaces } from '../data/mockData'
import DashboardLayout from '../components/Layout/DashboardLayout'
import { Plus, Search, Building2, Edit2, Trash2, Filter } from 'lucide-react'

function Spaces() {
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('All')

  const filtered = mockSpaces.filter(s => {
    const matchesSearch = s.unitNumber.toLowerCase().includes(search.toLowerCase()) || 
                          s.category.toLowerCase().includes(search.toLowerCase())
    const matchesFilter = filter === 'All' || s.status === filter
    return matchesSearch && matchesFilter
  })

  return (
    <DashboardLayout>
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">Space Management</h2>
          <p className="text-slate-500 text-sm mt-1">Manage all your commercial spaces and units</p>
        </div>
        <button className="flex items-center gap-2 px-4 py-2.5 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-medium rounded-lg shadow-lg shadow-indigo-500/30">
          <Plus className="w-4 h-4" /> Add New Space
        </button>
      </div>

      {/* Stat strip */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
        {[
          { label: 'Total Units', value: mockSpaces.length, color: 'text-slate-800' },
          { label: 'Vacant', value: mockSpaces.filter(s => s.status === 'Vacant').length, color: 'text-emerald-600' },
          { label: 'Occupied', value: mockSpaces.filter(s => s.status === 'Occupied').length, color: 'text-amber-600' },
          { label: 'Total Area (m²)', value: mockSpaces.reduce((sum, s) => sum + s.size, 0), color: 'text-indigo-600' },
        ].map((stat, i) => (
          <div key={i} className="bg-white rounded-xl border border-slate-100 p-4 shadow-sm">
            <p className="text-xs text-slate-500">{stat.label}</p>
            <p className={`text-2xl font-bold mt-1 ${stat.color}`}>{stat.value}</p>
          </div>
        ))}
      </div>

      {/* Toolbar */}
      <div className="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
        <div className="p-4 border-b border-slate-100 flex flex-col sm:flex-row gap-3">
          <div className="flex items-center gap-2 bg-slate-100 px-3 py-2 rounded-lg flex-1">
            <Search className="w-4 h-4 text-slate-400" />
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by unit or category..."
              className="bg-transparent outline-none text-sm w-full"
            />
          </div>
          <div className="flex items-center gap-2">
            <Filter className="w-4 h-4 text-slate-400" />
            {['All', 'Vacant', 'Occupied'].map(f => (
              <button
                key={f}
                onClick={() => setFilter(f)}
                className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors
                  ${filter === f ? 'bg-indigo-500 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}
              >
                {f}
              </button>
            ))}
          </div>
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-100">
                {['Unit', 'Floor', 'Category', 'Size (m²)', 'Status', 'Actions'].map(h => (
                  <th key={h} className="text-left px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filtered.map(space => (
                <tr key={space.id} className="hover:bg-slate-50 transition-colors">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 bg-gradient-to-br from-indigo-100 to-purple-100 rounded-lg flex items-center justify-center">
                        <Building2 className="w-4 h-4 text-indigo-600" />
                      </div>
                      <span className="font-semibold text-slate-800">{space.unitNumber}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-slate-600">{space.floor}</td>
                  <td className="px-6 py-4 text-slate-600">{space.category}</td>
                  <td className="px-6 py-4 text-slate-600">{space.size}</td>
                  <td className="px-6 py-4">
                    <span className={`px-3 py-1 text-xs font-semibold rounded-full
                      ${space.status === 'Vacant' 
                        ? 'bg-emerald-100 text-emerald-700' 
                        : 'bg-amber-100 text-amber-700'}`}>
                      {space.status}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2">
                      <button className="p-2 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors">
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button className="p-2 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="px-6 py-3 bg-slate-50 border-t border-slate-100 text-sm text-slate-500">
          Showing {filtered.length} of {mockSpaces.length} spaces
        </div>
      </div>
    </DashboardLayout>
  )
}

export default Spaces