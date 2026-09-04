import { useEffect, useState } from 'react';
import { Clock, Zap, RotateCcw, Loader2 } from 'lucide-react';
import { testApi } from '../../services/api';
import { useAuth } from '../../hooks/useAuth';

export default function TimeTravelControls() {
  const { user } = useAuth();
  const [date, setDate] = useState('');
  const [days, setDays] = useState(1);
  const [months, setMonths] = useState(1);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [currentDate, setCurrentDate] = useState('');

  useEffect(() => {
    (async () => {
      try {
        const r = await testApi.getCurrentTime();
        setCurrentDate(r.currentDate || r.now || '');
      } catch (e) { console.error('load time', e); }
    })();
  }, []);

  if (user?.role !== 'ADMIN') return null;

  const setMsg = (m) => { setMessage(m); };

  const handleSetDate = async () => {
    if (!date) return;
    setLoading(true);
    try {
      const r = await testApi.setTimeDate(date);
      setCurrentDate(r.currentDate || r.now || date);
      setMsg(`✅ Time set to: ${r.currentDate || r.now || date}`);
    } catch (e) { setMsg(`❌ ${e.message}`); }
    setLoading(false);
  };

  const handleAdvanceDays = async () => {
    setLoading(true);
    try {
      const r = await testApi.advanceDays(days);
      setCurrentDate(r.currentDate || r.now || '');
      setMsg(`✅ Advanced ${days} day${days > 1 ? 's' : ''}`);
    } catch (e) { setMsg(`❌ ${e.message}`); }
    setLoading(false);
  };

  const handleAdvanceMonths = async () => {
    setLoading(true);
    try {
      const r = await testApi.advanceMonths(months);
      setCurrentDate(r.currentDate || r.now || '');
      setMsg(`✅ Advanced ${months} month${months > 1 ? 's' : ''}`);
    } catch (e) { setMsg(`❌ ${e.message}`); }
    setLoading(false);
  };

  const handleRunJobs = async () => {
    setLoading(true);
    try {
      const r = await testApi.runScheduledJobs();
      setCurrentDate(r.currentDate || r.now || '');
      setMsg('✅ Scheduled jobs executed');
    } catch (e) { setMsg(`❌ ${e.message}`); }
    setLoading(false);
  };

  const handleReset = async () => {
    setLoading(true);
    try {
      const r = await testApi.resetTime();
      setCurrentDate(r.currentDate || r.now || '');
      setMsg('✅ Reset to real time');
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
        <span className="text-xs font-mono bg-slate-100 px-2 py-0.5 rounded">
          {currentDate || '—'}
        </span>
      </div>

      {message && (
        <div className={`text-xs p-2 rounded-lg mb-3 ${message.includes('✅') ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'}`}>
          {message}
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

      <div className="mt-3 text-[10px] text-slate-400 border-t border-slate-100 pt-2">
        ⚠️ Test mode — changes affect all users
      </div>
    </div>
  );
}
