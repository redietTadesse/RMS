// src/utils/ethiopianCalendar.js
// Converts between Gregorian and Ethiopian (Amete Mihret) dates.
// All dates stored and sent to the backend stay Gregorian 'YYYY-MM-DD'.
// This file only handles conversion and display.

const ETH_EPOCH = 1723856;

export const ETH_MONTHS = [
  'መስከረም', 'ጥቅምት', 'ኅዳር', 'ታኅሣሥ', 'ጥር', 'የካቲት', 'መጋቢት',
  'ሚያዝያ', 'ግንቦት', 'ሰኔ', 'ሐምሌ', 'ነሐሴ', 'ጳጉሜ',
];

const GREG_MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

const div = (a, b) => Math.floor(a / b);
const mod = (a, b) => a - b * Math.floor(a / b);
const pad = (n) => String(n).padStart(2, '0');

function gregorianToJdn(y, m, d) {
  const a = div(14 - m, 12);
  const yy = y + 4800 - a;
  const mm = m + 12 * a - 3;
  return d + div(153 * mm + 2, 5) + 365 * yy + div(yy, 4) - div(yy, 100) + div(yy, 400) - 32045;
}

function jdnToGregorian(jdn) {
  const a = jdn + 32044;
  const b = div(4 * a + 3, 146097);
  const c = a - div(146097 * b, 4);
  const d = div(4 * c + 3, 1461);
  const e = c - div(1461 * d, 4);
  const m = div(5 * e + 2, 153);
  return {
    day: e - div(153 * m + 2, 5) + 1,
    month: m + 3 - 12 * div(m, 10),
    year: 100 * b + d - 4800 + div(m, 10),
  };
}

function ethiopianToJdn(y, m, d) {
  return ETH_EPOCH + 365 + 365 * (y - 1) + div(y, 4) + 30 * m + d - 31;
}

function jdnToEthiopian(jdn) {
  const r = mod(jdn - ETH_EPOCH, 1461);
  const n = mod(r, 365) + 365 * div(r, 1460);
  return {
    year: 4 * div(jdn - ETH_EPOCH, 1461) + div(r, 365) - div(r, 1460),
    month: div(n, 30) + 1,
    day: mod(n, 30) + 1,
  };
}

/** Parses 'YYYY-MM-DD' (or a longer ISO string) without timezone shifts. */
function parseIso(value) {
  if (!value) return null;
  const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!match) return null;
  return { year: +match[1], month: +match[2], day: +match[3] };
}

/** Pagume has 6 days in years where year % 4 === 3, otherwise 5. */
export function daysInEthiopianMonth(year, month) {
  if (month < 13) return 30;
  return mod(year, 4) === 3 ? 6 : 5;
}

/** 'YYYY-MM-DD' (Gregorian) -> { year, month, day } (Ethiopian) */
export function toEthiopian(isoDate) {
  const g = parseIso(isoDate);
  if (!g) return null;
  return jdnToEthiopian(gregorianToJdn(g.year, g.month, g.day));
}

/** Ethiopian year, month, day -> 'YYYY-MM-DD' (Gregorian) */
export function toGregorianIso(year, month, day) {
  const g = jdnToGregorian(ethiopianToJdn(year, month, day));
  return `${g.year}-${pad(g.month)}-${pad(g.day)}`;
}

/** Today's date in Addis Ababa as Gregorian 'YYYY-MM-DD'. */
export function todayIso() {
  return new Date().toLocaleDateString('en-CA', { timeZone: 'Africa/Addis_Ababa' });
}

/** 'መስከረም 18, 2019' */
export function formatEthiopian(isoDate) {
  const e = toEthiopian(isoDate);
  if (!e) return '';
  return `${ETH_MONTHS[e.month - 1]} ${e.day}, ${e.year}`;
}

/** 'Sep 28, 2026' */
export function formatGregorian(isoDate) {
  const g = parseIso(isoDate);
  if (!g) return '';
  return `${GREG_MONTHS[g.month - 1]} ${g.day}, ${g.year}`;
}

/** 'መስከረም 18, 2019 (Sep 28, 2026)'. Returns fallback for empty values. */
export function formatDualDate(isoDate, fallback = '—') {
  if (!parseIso(isoDate)) return fallback;
  return `${formatEthiopian(isoDate)} (${formatGregorian(isoDate)})`;
}