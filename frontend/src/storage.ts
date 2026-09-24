// Прогресс и черновики хранятся только в браузере ученика.
// localStorage может быть недоступен (приватный режим, запрет cookies) — тогда просто работаем без сохранения.

const PREFIX = 'byte:';

function read(key: string): string | null {
  try {
    return localStorage.getItem(PREFIX + key);
  } catch {
    return null;
  }
}

function write(key: string, value: string | null): void {
  try {
    if (value === null) localStorage.removeItem(PREFIX + key);
    else localStorage.setItem(PREFIX + key, value);
  } catch {
    // хранилище недоступно или переполнено
  }
}

export function loadCode(key: string): string | null {
  return read(`code:${key}`);
}

export function saveCode(key: string, code: string | null): void {
  write(`code:${key}`, code);
}

const COMPLETED = 'completed';
const listeners = new Set<() => void>();

export function completedLessons(): Set<string> {
  try {
    const parsed = JSON.parse(read(COMPLETED) ?? '[]');
    return new Set(Array.isArray(parsed) ? parsed.filter((s) => typeof s === 'string') : []);
  } catch {
    return new Set();
  }
}

export function markCompleted(slug: string): void {
  const done = completedLessons();
  if (done.has(slug)) return;
  done.add(slug);
  write(COMPLETED, JSON.stringify([...done]));
  listeners.forEach((listener) => listener());
}

export function subscribeProgress(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}
