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

/** Прохождение урока: где остановился ученик, ответы на вопросы, открытые подсказки. */
export interface QuizState {
  choice: number;
  correct: boolean;
  attempts: number;
}

export interface LessonState {
  step: number;
  visited: number[];
  quizzes: Record<string, QuizState>;
  hints: number;
  failedChecks: number;
  solutionShown: boolean;
}

const EMPTY_LESSON_STATE: LessonState = {
  step: 0,
  visited: [0],
  quizzes: {},
  hints: 0,
  failedChecks: 0,
  solutionShown: false,
};

export function loadLessonState(slug: string): LessonState {
  try {
    const parsed = JSON.parse(read(`lesson:${slug}`) ?? 'null');
    return parsed && typeof parsed === 'object' ? { ...EMPTY_LESSON_STATE, ...parsed } : EMPTY_LESSON_STATE;
  } catch {
    return EMPTY_LESSON_STATE;
  }
}

export function saveLessonState(slug: string, state: LessonState): void {
  write(`lesson:${slug}`, JSON.stringify(state));
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
