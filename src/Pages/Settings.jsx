import DashboardLayout from '../components/Layout/DashboardLayout';

export default function Settings() {
  return (
    <DashboardLayout>
      <div className="rounded-3xl border border-slate-200 bg-white p-8 shadow-sm">
        <div className="flex flex-col gap-4">
          <h1 className="text-2xl font-semibold text-slate-900">Settings</h1>
          <p className="text-sm text-slate-500">Manage account preferences and security settings here.</p>

          <div className="grid gap-4 lg:grid-cols-2">
            <div className="rounded-3xl border border-slate-100 bg-slate-50 p-6">
              <h2 className="text-lg font-semibold text-slate-900">Account</h2>
              <p className="mt-2 text-sm text-slate-500">Review your account details and get support information.</p>
            </div>
            <div className="rounded-3xl border border-slate-100 bg-slate-50 p-6">
              <h2 className="text-lg font-semibold text-slate-900">Support</h2>
              <p className="mt-2 text-sm text-slate-500">If you need help, contact your system administrator.</p>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
