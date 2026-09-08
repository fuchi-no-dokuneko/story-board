const CANONICAL_INSTANT = /^((?:\d{4}|\+\d{5,10}|-\d{4,10}))-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d{3}|\d{6}|\d{9}))?Z$/u;

function canonicalYear(text) {
  if (/^\d{4}$/u.test(text)) return Number(text);
  if (/^\+[1-9]\d{4,9}$/u.test(text)) {
    const value = Number(text.slice(1));
    return value > 9_999 && value <= 1_000_000_000 ? value : null;
  }
  if (/^-(?:\d{4}|[1-9]\d{4,9})$/u.test(text) && text !== "-0000") {
    const value = -Number(text.slice(1));
    return value >= -1_000_000_000 ? value : null;
  }
  return null;
}

function daysInMonth(year, month) {
  if (month === 2) {
    const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
    return leap ? 29 : 28;
  }
  return [4, 6, 9, 11].includes(month) ? 30 : 31;
}

export function isCanonicalInstant(value) {
  if (typeof value !== "string") return false;
  const match = CANONICAL_INSTANT.exec(value);
  if (match === null) return false;
  const [, yearText, monthText, dayText, hourText, minuteText, secondText, fraction] = match;
  const year = canonicalYear(yearText);
  const month = Number(monthText);
  const day = Number(dayText);
  const canonicalFraction = fraction === undefined
    || (!/^0+$/u.test(fraction) && (fraction.length === 3 || !fraction.endsWith("000")));
  return year !== null
    && month >= 1 && month <= 12
    && day >= 1 && day <= daysInMonth(year, month)
    && Number(hourText) <= 23
    && Number(minuteText) <= 59
    && Number(secondText) <= 59
    && canonicalFraction;
}

export function requireCanonicalInstant(value, location = "instant") {
  if (!isCanonicalInstant(value)) {
    throw new TypeError(`${location} must be canonical java.time.Instant UTC text`);
  }
  return value;
}
