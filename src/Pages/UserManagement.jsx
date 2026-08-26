// src/Pages/UserManagement.jsx
import { useEffect, useState } from 'react';
import { Users, Shield, UserPlus, Trash2, Edit2 } from 'lucide-react';
import { adminApi } from '../services/api';
import DashboardLayout from '../components/Layout/DashboardLayout';

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function loadUsers() {
      setLoading(true);
      setError(null);
      try {
        const data = await adminApi.getAllUsers();
        setUsers(data);
      } catch (err) {
        setError(err.message || 'Unable to load users');
      } finally {
        setLoading(false);
      }
    }

    loadUsers();
  }, []);

  const handleToggleActive = async (userId, currentState) => {
    try {
      await adminApi.updateUser(userId, { active: !currentState });
      setUsers((prev) => prev.map((user) => user.id === userId ? { ...user, active: !currentState } : user));
    } catch (err) {
      setError(err.message || 'Unable to update user');
    }
  };

  const handleDelete = async (userId) => {
    try {
      await adminApi.deleteUser(userId);
      setUsers((prev) => prev.filter((user) => user.id !== userId));
    } catch (err) {
      setError(err.message || 'Unable to delete user');
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <div className="flex items-center gap-3 text-indigo-600">
              <Users className="w-6 h-6" />
              <h1 className="text-2xl font-bold text-slate-800">User Management</h1>
            </div>
            <p className="text-sm text-slate-500">Create, manage and disable users for your rental system.</p>
          </div>
          <button
            onClick={() => window.location.assign('/register')}
            className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg shadow-sm transition"
          >
            <UserPlus className="w-4 h-4" />
            Add user
          </button>
        </div>

        {error && (
          <div className="rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 border border-red-200">
            {error}
          </div>
        )}

        <div className="bg-white rounded-xl border border-slate-100 shadow-sm overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-100">
              <tr className="text-left text-slate-500">
                <th className="px-4 py-3 font-medium">Username</th>
                <th className="px-4 py-3 font-medium">Name</th>
                <th className="px-4 py-3 font-medium">Role</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 text-right font-medium">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan="5" className="px-4 py-8 text-center text-slate-400">
                    Loading users...
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan="5" className="px-4 py-8 text-center text-slate-400">
                    No users created yet.
                  </td>
                </tr>
              ) : (
                users.map((user) => (
                  <tr key={user.id} className="hover:bg-slate-50/60 transition">
                    <td className="px-4 py-3 font-medium text-slate-800">{user.username}</td>
                    <td className="px-4 py-3 text-slate-700">{user.name}</td>
                    <td className="px-4 py-3 uppercase tracking-wider text-slate-600">{user.role}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${user.active ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}`}>
                        {user.active ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => handleToggleActive(user.id, user.active)}
                        className="mr-2 inline-flex items-center gap-1.5 px-3 py-1.5 border border-slate-200 bg-slate-50 hover:bg-slate-100 text-slate-700 text-xs font-medium rounded-lg transition"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                        {user.active ? 'Disable' : 'Enable'}
                      </button>
                      <button
                        onClick={() => handleDelete(user.id)}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 border border-rose-200 bg-rose-50 hover:bg-rose-100 text-rose-700 text-xs font-medium rounded-lg transition"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        Delete
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          {!loading && users.length > 0 && (
            <div className="px-4 py-2.5 bg-slate-50 border-t border-slate-100 text-xs text-slate-500">
              Total: {users.length} user{users.length !== 1 ? 's' : ''}
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}