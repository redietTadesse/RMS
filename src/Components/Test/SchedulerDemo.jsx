import { useState } from 'react';
import { CalendarClock, Eye, RotateCcw } from 'lucide-react';

const jobs = [
  { name: 'Generate monthly invoices', schedule: '0 0 0 1 * ?', timing: '12:00 AM on the 1st' },
  { name: 'Calculate daily penalties', schedule: '0 0 1 * * ?', timing: '1:00 AM daily' },
  { name: 'Expire ended leases', schedule: '0 0 2 * * ?', timing: '2:00 AM daily' },
  { name: 'Purge refresh tokens', schedule: '0 0 3 * * ?', timing: '3:00 AM daily' },
];

const sampleOverdues = [
  { invoice: 'INV-2026-0101', tenant: 'Acme Ltd', amount: 4500, dueDate: '2026-09-10', daysOverdue: 14 },
  { invoice: 'INV-2026-0102', tenant: 'Beta Co', amount: 1200, dueDate: '2026-09-18', daysOverdue: 6 },
];

export default function SchedulerDemo() {
  const [preview, setPreview] = useState(false);

  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <CalendarClock className="h-5 w-5 text-indigo-600" />
            <h2 className="text-lg font-semibold text-slate-900">Scheduler demo</h2>
          </div>
          <p className="mt-1 text-sm text-slate-500">
            Frontend preview only. No jobs run and no data is changed.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setPreview((visible) => !visible)}
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
        >
          {preview ? <RotateCcw className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          {preview ? 'Hide preview' : 'Preview overdue work'}
        </button>
      </div>

      <div className="mt-5 overflow-x-auto">
        <table className="w-full min-w-[620px] text-left text-sm">
          <thead>
            <tr className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-500">
              <th className="px-3 py-2 font-medium">Job</th>
              <th className="px-3 py-2 font-medium">Cron</th>
              <th className="px-3 py-2 font-medium">Schedule</th>
              <th className="px-3 py-2 font-medium">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-50">
            {jobs.map((job) => (
              <tr key={job.name}>
                <td className="px-3 py-3 font-medium text-slate-800">{job.name}</td>
                <td className="px-3 py-3 font-mono text-xs text-slate-500">{job.schedule}</td>
                <td className="px-3 py-3 text-slate-600">{job.timing}</td>
                <td className="px-3 py-3">
                  <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">Configured</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {preview && (
        <div className="mt-5 rounded-2xl border border-amber-200 bg-amber-50/60 p-4">
          <div className="mb-3 flex items-center justify-between gap-3">
            <div>
              <h3 className="text-sm font-semibold text-amber-900">Sample overdue output</h3>
              <p className="text-xs text-amber-700">Demo data only. These rows are not from the database.</p>
            </div>
            <span className="rounded-full bg-amber-100 px-2.5 py-1 text-xs font-semibold text-amber-800">Preview</span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[560px] text-left text-sm">
              <thead>
                <tr className="border-b border-amber-200 text-xs uppercase tracking-wide text-amber-800">
                  <th className="px-3 py-2 font-medium">Invoice</th>
                  <th className="px-3 py-2 font-medium">Tenant</th>
                  <th className="px-3 py-2 font-medium">Amount</th>
                  <th className="px-3 py-2 font-medium">Due date</th>
                  <th className="px-3 py-2 font-medium">Overdue</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-amber-100">
                {sampleOverdues.map((item) => (
                  <tr key={item.invoice} className="text-amber-950">
                    <td className="px-3 py-3 font-medium">{item.invoice}</td>
                    <td className="px-3 py-3">{item.tenant}</td>
                    <td className="px-3 py-3">ETB {item.amount.toLocaleString()}</td>
                    <td className="px-3 py-3">{item.dueDate}</td>
                    <td className="px-3 py-3 font-semibold">{item.daysOverdue} days</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
