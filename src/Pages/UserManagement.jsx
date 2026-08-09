import { useEffect, useState } from 'react';
import { Users, Shield, UserPlus, Trash2, Edit2 } from 'lucide-react';
import { adminApi } from '../services/api';

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
    <div className="p-6 lg:p-10">
      <div className="mb-8 flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
        <div>
          <div className="flex items-center gap-3 text-indigo-600">
            <Users className="w-6 h-6" />
            <h1 className="text-2xl font-semibold text-slate-900">User Management</h1>
          </div>
          <p className="mt-2 text-sm text-slate-500">Create, manage and disable users for your rental system.</p>
        </div>
        <button
          onClick={() => window.location.assign('/register')}
          className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-3 text-sm font-semibold text-white shadow-lg shadow-indigo-500/20 hover:bg-indigo-700 transition-colors"
        >
          <UserPlus className="w-4 h-4" />
          Add user
        </button>
      </div>

      {error && (
        <div className="mb-6 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {error}
        </div>
      )}

      <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
        <table className="w-full text-left text-sm text-slate-600">
          <thead className="bg-slate-50 text-slate-500">
            <tr>
              <th className="px-6 py-4">Username</th>
              <th className="px-6 py-4">Name</th>
              <th className="px-6 py-4">Role</th>
              <th className="px-6 py-4">Status</th>
              <th className="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan="5" className="px-6 py-8 text-center text-slate-500">
                  Loading users...
                </td>
              </tr>
            ) : users.length === 0 ? (
              <tr>
                <td colSpan="5" className="px-6 py-8 text-center text-slate-500">
                  No users created yet.
                </td>
              </tr>
            ) : (
              users.map((user) => (
                <tr key={user.id} className="border-t border-slate-100">
                  <td className="px-6 py-4 font-medium text-slate-900">{user.username}</td>
                  <td className="px-6 py-4">{user.name}</td>
                  <td className="px-6 py-4 uppercase tracking-wider text-slate-600">{user.role}</td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ${user.active ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}`}>
                      {user.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <button
                      onClick={() => handleToggleActive(user.id, user.active)}
                      className="mr-2 inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100"
                    >
                      <Edit2 className="w-4 h-4" />
                      {user.active ? 'Disable' : 'Enable'}
                    </button>
                    <button
                      onClick={() => handleDelete(user.id)}
                      className="inline-flex items-center gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-xs font-semibold text-rose-700 hover:bg-rose-100"
                    >
                      <Trash2 className="w-4 h-4" />
                      Delete
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
