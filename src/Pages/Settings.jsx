import DashboardLayout from '../components/Layout/DashboardLayout';
import { useAuth } from '../hooks/useAuth';

const SUPPORT_EMAIL = import.meta.env.VITE_SUPPORT_EMAIL || 'support@rms.example.com';

export default function Settings() {
  const { user, isAdmin } = useAuth();

  const initials = (user?.name || user?.username || '?')
    .split(' ')
    .map((w) => w[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

  return (
    <DashboardLayout>
      <div className="rounded-3xl border border-slate-200 bg-white p-8 shadow-sm">
        <div className="flex flex-col gap-4">
          <h1 className="text-2xl font-semibold text-slate-900">Settings</h1>
          <p className="text-sm text-slate-500">Manage account preferences and security settings here.</p>

          <div className="grid gap-4 lg:grid-cols-2">
            <div className="rounded-3xl border border-slate-100 bg-slate-50 p-6">
              <h2 className="text-lg font-semibold text-slate-900">Account</h2>
              <p className="mt-2 text-sm text-slate-500">Review your account details.</p>

              <div className="mt-5 flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-indigo-500 text-sm font-semibold text-white">
                  {initials}
                </div>
                <div>
                  <p className="text-sm font-semibold text-slate-900">{user?.name || 'N/A'}</p>
                  <p className="text-xs text-slate-500">@{user?.username || 'N/A'}</p>
                </div>
              </div>

              <dl className="mt-5 space-y-3 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-slate-500">Email</dt>
                  <dd className="font-medium text-slate-900 break-all text-right">{user?.email || 'N/A'}</dd>
                </div>
                <div className="flex justify-between gap-4">
                  <dt className="text-slate-500">Role</dt>
                  <dd className="font-medium text-slate-900">
                    <span className="rounded-full bg-indigo-100 px-2.5 py-0.5 text-xs font-semibold text-indigo-700">
                      {user?.role || 'N/A'}
                    </span>
                  </dd>
                </div>
              </dl>

              <a href="/profile" className="mt-6 inline-flex text-sm font-medium text-indigo-600 hover:text-indigo-700">
                View full profile &rarr;
              </a>
            </div>

            <div className="rounded-3xl border border-slate-100 bg-slate-50 p-6">
              <h2 className="text-lg font-semibold text-slate-900">Support</h2>
              <p className="mt-2 text-sm text-slate-500">
                {isAdmin
                  ? 'Reach the development team for technical issues.'
                  : 'If you need help, contact your system administrator.'}
              </p>

              <div className="mt-5 space-y-3 text-sm">
                <div className="flex justify-between gap-4">
                  <span className="text-slate-500">Email</span>
                  <a href={`mailto:${SUPPORT_EMAIL}`} className="font-medium text-indigo-600 hover:text-indigo-700 break-all text-right">
                    {SUPPORT_EMAIL}
                  </a>
                </div>
              </div>

              <a href={`mailto:${SUPPORT_EMAIL}?subject=RMS%20Support%20Request`} className="mt-6 inline-flex rounded-xl bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700">
                Contact support
              </a>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
