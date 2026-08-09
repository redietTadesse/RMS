import { useEffect, useState } from 'react';
import { User, Lock, CheckCircle } from 'lucide-react';
import DashboardLayout from '../components/Layout/DashboardLayout';
import { useAuth } from '../context/AuthContext';
import { userApi } from '../services/api';

export default function Profile() {
  const { user } = useAuth();
  const [profile, setProfile] = useState(user);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [formData, setFormData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  useEffect(() => {
    const loadProfile = async () => {
      setLoading(true);
      setError('');
      try {
        const data = await userApi.getProfile();
        setProfile(data);
      } catch (err) {
        setError(err.message || 'Unable to load profile');
      } finally {
        setLoading(false);
      }
    };
    loadProfile();
  }, []);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (formData.newPassword !== formData.confirmPassword) {
      setError('New password and confirmation do not match.');
      return;
    }
    if (formData.newPassword.length < 6) {
      setError('New password must be at least 6 characters.');
      return;
    }

    try {
      await userApi.changePassword(formData);
      setSuccess('Your password has been updated successfully.');
      setFormData({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setError(err.message || 'Unable to update password.');
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-3xl bg-indigo-500/10 text-indigo-600 flex items-center justify-center text-2xl">
              <User className="w-8 h-8" />
            </div>
            <div>
              <h1 className="text-2xl font-semibold text-slate-900">My Profile</h1>
              <p className="text-sm text-slate-500">Review your account details and update your password.</p>
            </div>
          </div>

          <div className="mt-6 grid gap-4 md:grid-cols-3">
            <div className="rounded-3xl border border-slate-100 bg-slate-50 p-5">
              <p className="text-xs uppercase tracking-wide text-slate-500">Username</p>
              <p className="mt-2 text-base font-medium text-slate-900">{profile?.username || user?.username}</p>
            </div>
            <div className="rounded-3xl border border-slate-100 bg-slate-50 p-5">
              <p className="text-xs uppercase tracking-wide text-slate-500">Name</p>
              <p className="mt-2 text-base font-medium text-slate-900">{profile?.name || user?.name}</p>
            </div>
            <div className="rounded-3xl border border-slate-100 bg-slate-50 p-5">
              <p className="text-xs uppercase tracking-wide text-slate-500">Role</p>
              <p className="mt-2 text-base font-medium text-slate-900">{profile?.role || user?.role}</p>
            </div>
          </div>
        </div>

        <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center justify-between gap-4 mb-6">
            <div>
              <h2 className="text-xl font-semibold text-slate-900">Change Password</h2>
              <p className="text-sm text-slate-500">Update your password to keep your account secure.</p>
            </div>
            <div className="inline-flex items-center gap-2 text-sm font-semibold text-emerald-600">
              <CheckCircle className="w-4 h-4" /> Secure account
            </div>
          </div>

          {success && (
            <div className="mb-4 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-700">
              {success}
            </div>
          )}
          {error && (
            <div className="mb-4 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">Current Password</label>
              <input
                type="password"
                name="currentPassword"
                value={formData.currentPassword}
                onChange={handleChange}
                required
                className="w-full rounded-3xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-900 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">New Password</label>
              <input
                type="password"
                name="newPassword"
                value={formData.newPassword}
                onChange={handleChange}
                required
                className="w-full rounded-3xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-900 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">Confirm New Password</label>
              <input
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                required
                className="w-full rounded-3xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-900 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200"
              />
            </div>
            <button
              type="submit"
              className="inline-flex items-center justify-center rounded-3xl bg-indigo-600 px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-indigo-500/20 hover:bg-indigo-700 transition-colors"
            >
              Update Password
            </button>
          </form>
        </div>
      </div>
    </DashboardLayout>
  );
}
