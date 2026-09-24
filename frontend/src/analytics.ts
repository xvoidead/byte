// Анонимная статистика прохождения курса: какие уроки открывают, где застревают, какие ошибки делают.
// Без cookies и IP: только случайный идентификатор браузера. При «Не отслеживать» и Global Privacy Control
// ничего не отправляется. Код учеников никогда не передаётся.

export type EventType =
  | 'page_view'
  | 'lesson_open'
  | 'step_view'
  | 'quiz_answer'
  | 'run'
  | 'compile_error'
  | 'check'
  | 'test_failed'
  | 'requirement_failed'
  | 'hint_open'
  | 'solution_open'
  | 'lesson_complete';

interface AnalyticsEvent {
  type: EventType;
  lesson?: string;
  item?: string;
  value?: number;
}

const VISITOR_KEY = 'byte:visitor';
const FLUSH_MS = 5000;
const MAX_BATCH = 50;

let queue: AnalyticsEvent[] = [];
let timer: ReturnType<typeof setTimeout> | null = null;
let visitor: string | null | undefined;

function optedOut(): boolean {
  const nav = navigator as Navigator & { globalPrivacyControl?: boolean; msDoNotTrack?: string };
  return nav.globalPrivacyControl === true || nav.doNotTrack === '1' || nav.msDoNotTrack === '1';
}

function visitorId(): string | null {
  if (visitor !== undefined) return visitor;
  visitor = null;
  try {
    if (optedOut() || typeof crypto.randomUUID !== 'function') return null;
    visitor = localStorage.getItem(VISITOR_KEY);
    if (!visitor) {
      visitor = crypto.randomUUID();
      localStorage.setItem(VISITOR_KEY, visitor);
    }
  } catch {
    visitor = null;
  }
  return visitor;
}

function flush(): void {
  if (timer) {
    clearTimeout(timer);
    timer = null;
  }
  const id = visitorId();
  while (id && queue.length > 0) {
    const events = queue.splice(0, MAX_BATCH);
    const body = JSON.stringify({ visitor: id, events });
    try {
      const sent =
        typeof navigator.sendBeacon === 'function' &&
        navigator.sendBeacon('/api/events', new Blob([body], { type: 'application/json' }));
      if (!sent) {
        void fetch('/api/events', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body,
          keepalive: true,
        }).catch(() => undefined);
      }
    } catch {
      // статистика не важнее работы сайта
    }
  }
  queue = [];
}

export function track(type: EventType, lesson?: string, item?: string | null, value?: number): void {
  if (!visitorId()) return;
  queue.push({ type, lesson, item: item ?? undefined, value });
  if (queue.length >= MAX_BATCH) flush();
  else if (!timer) timer = setTimeout(flush, FLUSH_MS);
}

if (typeof document !== 'undefined') {
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') flush();
  });
  window.addEventListener('pagehide', flush);
}
