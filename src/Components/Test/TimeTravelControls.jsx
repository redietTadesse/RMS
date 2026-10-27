import { useEffect, useState } from 'react';
import { Clock, Zap, RotateCcw, Loader2, ChevronUp, ChevronDown } from 'lucide-react';
import { testApi } from '../../services/api';
import { useAuth } from '../../hooks/useAuth';

const FLASH_KEY = 'rms.timeTravel.flash';

// Success messages used to be wiped instantly by window.location.reload().
// Stash the confirmation (and the last job result) in sessionStorage so it
// survives the reload and the admin can see what the click actually did.
const stashFlash = (message, lastResult = null) => {
  try {
    sessionStorage.setItem(FLASH_KEY, JSON.stringify({ message, lastResult }));
  } catch {
    /* storage unavailable — the message just won't survive the reload */
  }
};

export default function TimeTravelControls() {
  const { user } = useAuth();
  const [date, setDate] = useState('');
  const [days, setDays] = useState(1);
  const [months, setMonths] = useState(1);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [currentDate, setCurrentDate] = useState('');
  const [lastResult, setLastResult] = useState(null);
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    // Only admins can read time-travel state, and this component mounts before
    // login. Without the role guard the request 403s for everyone and the panel
    // never populates after an admin signs in.
    if (user?.role !== 'ADMIN') return;
    (async () => {
      try {
        const r = await testApi.getCurrentTime();
        setCurrentDate(r.currentDate || r.now || '');
      } catch (e) { console.error('load time', e); }
    })();
  }, [user?.role]);

  // Restore a confirmation stashed by the previous page reload (if any).
  useEffect(() => {
    try {
      const raw = sessionStorage.getItem(FLASH_KEY);
      if (!raw) return;
      sessionStorage.removeItem(FLASH_KEY);
      const flash = JSON.parse(raw);
      if (flash?.message) setMessage(flash.message);
      if (flash?.lastResult) setLastResult(flash.lastResult);
    } catch {
      /* malformed flash data — ignore */
    }
  }, []);

  if (user?.role !== 'ADMIN') return null;

  const setMsg = (m) => { setMessage(m); };
  const clearMsg = () => { setMessage(''); };

  const handleSetDate = async () => {
    if (!date) return;
    setLoading(true);
    try {
      const r = await testApi.setTimeDate(date);
      setCurrentDate(r.currentDate || r.now || date);
      const msg = `✅ Time set to: ${r.currentDate || r.now || date}`;
      setMsg(msg);
      stashFlash(msg);
      window.location.reload();
    } catch (e) { setMsg(`❌ ${e.message}`); }
    setLoading(false);
  };

  const handleAdvanceDays = async () => {
    setLoading(true);
    try {
      const r = await testApi.advanceDays(days);
      setCurrentDate(r.currentDate || r.now || '');
      const msg = `✅ Advanced ${days} day${days > 1 ? 's' : ''} to ${r.currentDate || r.now || ''}`.trim();
      setMsg(msg);
      stashFlash(msg);
      window.location.reload();
    } catch (e) { setMsg(`❌ ${e.message}`); }
    setLoading(false);
  };

  const handleAdvanceMonths = async () => {
    setLoading(true);
    try {
      const r = await testApi.advanceMonths(months);
      setCurrentDate(r.currentDate || r.now || '');
      const msg = `✅ Advanced ${months} month${months > 1 ? 's' : ''} to ${r.currentDate || r.now || ''}`.trim();
      setMsg(msg);
      stashFlash(msg);
      window.location.reload();
    } catch (e) { setMsg(`❌ ${e.message}`); }
    setLoading(false);
  };

  const handleRunJobs = async () => {
    setLoading(true);
    clearMsg();
    try {
      const r = await testApi.runScheduledJobs();
      setCurrentDate(r.currentDate || r.now || '');
      
      // Build result summary
      const summary = [];
      if (r.warning) {
        summary.push(`⚠️ ${r.warning}`);
      }
      if (r.invoicesGenerated) {
        summary.push('✅ Invoices generated');
      } else if (r.invoiceError) {
        summary.push(`❌ Invoice error: ${r.invoiceError}`);
      }
      
      if (r.penaltiesCalculated) {
        summary.push('✅ Penalties calculated');
      } else if (r.penaltyError) {
        summary.push(`❌ Penalty error: ${r.penaltyError}`);
      }
      
      if (r.leasesExpired !== undefined && r.leasesExpired >= 0) {
        if (r.leasesExpired > 0) {
          summary.push(`✅ ${r.leasesExpired} lease(s) expired`);
        } else {
          summary.push('✅ No lease expiries');
        }
      } else if (r.leaseError) {
        summary.push(`❌ Lease error: ${r.leaseError}`);
      }
      
      if (summary.length === 0) {
        summary.push('⚠️ No results returned');
      }

      if (r.persisted === false) {
        summary.push('Database unchanged');
      }
      
      const msg = summary.join(' | ');
      setMsg(msg);
      setLastResult(r);
      // Reload so every page refetches with the job results applied, and stash
      // the summary so the confirmation is still visible after the reload.
      stashFlash(msg, r);
      window.location.reload();
    } catch (e) { setMsg(`❌ ${e.message}`); }
    setLoading(false);
  };

  const handleReset = async () => {
    setLoading(true);
    try {
      await testApi.resetTime();
      const r = await testApi.getCurrentTime();
      setCurrentDate(r.currentDate || r.now || '');
      const msg = r.isVirtual
        ? '❌ Reset was not applied'
        : '✅ Real time restored. Reloading data...';
      setMsg(msg);
      const result = { ...r, isVirtual: Boolean(r.isVirtual) };
      setLastResult(result);
      if (!r.isVirtual) {
        stashFlash(msg, result);
        window.location.reload();
      }
    } catch (e) { setMsg(`❌ ${e.message}`); }
    setLoading(false);
  };

  return (
    <div className="fixed bottom-4 right-4 z-50 bg-white rounded-xl shadow-2xl border border-slate-200 p-4 w-80">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-bold text-slate-800 flex items-center gap-2">
          <Clock className="w-4 h-4 text-indigo-600" />
          Time Travel
        </h3>
        <div className="flex items-center gap-2">
          <span className="text-xs font-mono bg-slate-100 px-2 py-0.5 rounded">
            {currentDate || '—'}
          </span>
          <button onClick={() => setCollapsed(!collapsed)}
            className="p-1 hover:bg-slate-100 rounded text-slate-400 hover:text-slate-600">
            {collapsed ? <ChevronDown className="w-4 h-4" /> : <ChevronUp className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {message && (
        <div className={`text-xs p-2 rounded-lg mb-3 ${message.includes('✅') ? 'bg-emerald-50 text-emerald-700' : message.includes('⚠️') ? 'bg-yellow-50 text-yellow-700' : 'bg-red-50 text-red-700'}`}>
          {message}
        </div>
      )}

      {!collapsed && (
        <>
        {lastResult && (
          <div className="mb-2 p-2 bg-slate-50 rounded-lg border border-slate-200">
            <div className="text-xs text-slate-500 mb-1">Last Result:</div>
            <div className="text-xs font-mono text-slate-700">
              {lastResult.isVirtual ? 'Virtual Time: ON' : 'Virtual Time: OFF'}<br />
              {lastResult.invoicesGenerated !== undefined && `Invoices: ${lastResult.invoicesGenerated}`}<br />
              {lastResult.penaltiesCalculated !== undefined && `Penalties: ${lastResult.penaltiesCalculated}`}<br />
              {lastResult.leasesExpired !== undefined && `Lease Expiry Simulations: ${lastResult.leasesExpired}`}<br />
              {lastResult.simulatedOverdueCount !== undefined && `Simulated Overdue: ${lastResult.simulatedOverdueCount}`}<br />
              {lastResult.simulatedPenaltyTotal !== undefined && `Simulated Penalties: ETB ${lastResult.simulatedPenaltyTotal}`}<br />
              {lastResult.persisted === false ? 'Database: unchanged' : 'Database: persisted'}
            </div>
            {lastResult.simulatedOverdueInvoices?.length > 0 && (
              <div className="mt-2 space-y-1 border-t border-slate-200 pt-2 text-xs text-rose-700">
                {lastResult.simulatedOverdueInvoices.map((invoice) => (
                  <div key={invoice.invoiceNumber} className="flex justify-between gap-2">
                    <span>{invoice.invoiceNumber} · {invoice.daysOverdue}d overdue</span>
                    <span>ETB {invoice.penaltyAmount}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        <div className="space-y-3">
        <div className="flex gap-2">
          <input type="date" value={date} onChange={(e) => setDate(e.target.value)}
            className="flex-1 px-2 py-1.5 border border-slate-200 rounded-lg text-sm" />
          <button onClick={handleSetDate} disabled={loading || !date}
            className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-medium rounded-lg disabled:opacity-50">
            {loading ? <Loader2 className="w-3 h-3 animate-spin" /> : 'Set'}
          </button>
        </div>

        <div className="flex gap-2 items-center">
          <input type="number" min="1" max="365" value={days}
            onChange={(e) => setDays(Math.max(1, parseInt(e.target.value) || 1))}
            className="w-16 px-2 py-1.5 border border-slate-200 rounded-lg text-sm text-center" />
          <span className="text-xs text-slate-500">days</span>
          <button onClick={handleAdvanceDays} disabled={loading}
            className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-medium rounded-lg disabled:opacity-50">
            {loading ? <Loader2 className="w-3 h-3 animate-spin" /> : 'Advance'}
          </button>
        </div>

        <div className="flex gap-2 items-center">
          <input type="number" min="1" max="12" value={months}
            onChange={(e) => setMonths(Math.max(1, parseInt(e.target.value) || 1))}
            className="w-16 px-2 py-1.5 border border-slate-200 rounded-lg text-sm text-center" />
          <span className="text-xs text-slate-500">months</span>
          <button onClick={handleAdvanceMonths} disabled={loading}
            className="px-3 py-1.5 bg-purple-600 hover:bg-purple-700 text-white text-xs font-medium rounded-lg disabled:opacity-50">
            {loading ? <Loader2 className="w-3 h-3 animate-spin" /> : 'Advance'}
          </button>
        </div>

        <button onClick={handleRunJobs} disabled={loading}
          className="w-full flex items-center justify-center gap-2 px-3 py-2 bg-amber-600 hover:bg-amber-700 text-white text-sm font-medium rounded-lg disabled:opacity-50">
          {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Zap className="w-4 h-4" />}
          Run Scheduled Jobs
        </button>

        <button onClick={handleReset} disabled={loading}
          className="w-full flex items-center justify-center gap-2 px-3 py-2 bg-slate-600 hover:bg-slate-700 text-white text-sm font-medium rounded-lg disabled:opacity-50">
          {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <RotateCcw className="w-4 h-4" />}
          Reset to Real Time
        </button>
      </div>

      {!collapsed && (
        <div className="mt-3 text-[10px] text-slate-400 border-t border-slate-100 pt-2">
          Job results are saved to the database · Reset returns to real time<br/>
          Note: Invoices are only created for months without existing invoices
        </div>
      )}
      </>
      )}
    </div>
  );
}
