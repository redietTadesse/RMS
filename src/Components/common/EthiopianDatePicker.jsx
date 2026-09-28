// src/Components/Common/EthiopianDatePicker.jsx
// Drop-in replacement for <input type="date">.
// The user picks an Ethiopian date; onChange receives the Gregorian 'YYYY-MM-DD'
// in the same { target: { name, value } } shape as a normal input, so existing
// handleChange functions keep working unchanged.
import { useEffect, useRef, useState } from 'react';
import {
  ETH_MONTHS,
  daysInEthiopianMonth,
  toEthiopian,
  toGregorianIso,
  todayIso,
  formatGregorian,
} from '../../utils/ethiopianCalendar';

const BLANK = { year: '', month: '', day: '' };

export default function EthiopianDatePicker({
  name,
  value,
  onChange,
  className = '',
  disabled = false,
  yearsBack = 30,
  yearsForward = 15,
}) {
  const [parts, setParts] = useState(() => toEthiopian(value) || BLANK);
  // Remembers what we last sent up, so our own partial selections
  // don't get wiped when the parent passes the value back down.
  const lastEmitted = useRef(value);

  useEffect(() => {
    if (value === lastEmitted.current) return;
    lastEmitted.current = value;
    setParts(toEthiopian(value) || BLANK);
  }, [value]);

  const emit = (iso) => {
    lastEmitted.current = iso;
    onChange?.({ target: { name, value: iso, type: 'date' } });
  };

  const update = (field, raw) => {
    const next = { ...parts, [field]: raw === '' ? '' : Number(raw) };

    // Pagume has only 5 or 6 days, so clamp the day if needed
    if (next.year && next.month && next.day) {
      const max = daysInEthiopianMonth(next.year, next.month);
      if (next.day > max) next.day = max;
    }

    setParts(next);
    emit(next.year && next.month && next.day ? toGregorianIso(next.year, next.month, next.day) : '');
  };

  const setToday = () => {
    const iso = todayIso();
    setParts(toEthiopian(iso));
    emit(iso);
  };

  const currentEthYear = toEthiopian(todayIso()).year;
  const years = [];
  for (let y = currentEthYear + yearsForward; y >= currentEthYear - yearsBack; y--) years.push(y);

  const dayCount = parts.month
    ? daysInEthiopianMonth(parts.year || currentEthYear, parts.month)
    : 30;
  const days = Array.from({ length: dayCount }, (_, i) => i + 1);

  return (
    <div>
      <div className="grid grid-cols-3 gap-2">
        <select
          value={parts.day}
          onChange={(e) => update('day', e.target.value)}
          disabled={disabled}
          className={className}
          aria-label="Day"
        >
          <option value="">ቀን / Day</option>
          {days.map((d) => (
            <option key={d} value={d}>{d}</option>
          ))}
        </select>

        <select
          value={parts.month}
          onChange={(e) => update('month', e.target.value)}
          disabled={disabled}
          className={className}
          aria-label="Month"
        >
          <option value="">ወር / Month</option>
          {ETH_MONTHS.map((m, i) => (
            <option key={m} value={i + 1}>{m}</option>
          ))}
        </select>

        <select
          value={parts.year}
          onChange={(e) => update('year', e.target.value)}
          disabled={disabled}
          className={className}
          aria-label="Year"
        >
          <option value="">ዓመት / Year</option>
          {years.map((y) => (
            <option key={y} value={y}>{y}</option>
          ))}
        </select>
      </div>

      <div className="flex items-center justify-between mt-1 text-xs">
        <span className="text-slate-500">
          {value ? `Gregorian: ${formatGregorian(value)}` : 'Select day, month and year'}
        </span>
        {!disabled && (
          <button
            type="button"
            onClick={setToday}
            className="text-indigo-600 hover:text-indigo-700 font-medium"
          >
            ዛሬ / Today
          </button>
        )}
      </div>
    </div>
  );
}