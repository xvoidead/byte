import type { CheckResult, Diagnostic, LessonDetails, LessonSummary, RunResult } from './types';

export class ApiError extends Error {}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  if (init?.signal?.aborted) throw new DOMException('aborted', 'AbortError');
  let response: Response;
  try {
    response = await fetch(url, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...init?.headers },
    });
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') throw e;
    throw new ApiError('Нет связи с сервером. Проверьте подключение к интернету.');
  }
  if (!response.ok) {
    let message = `Ошибка сервера (${response.status})`;
    try {
      const body = await response.json();
      if (body && typeof body.error === 'string') message = body.error;
    } catch {
      // тело не JSON — оставляем общее сообщение
    }
    throw new ApiError(message);
  }
  return response.json() as Promise<T>;
}

export const api = {
  site: () => request<{ contactEmail: string | null }>('/api/site'),
  lessons: () => request<LessonSummary[]>('/api/lessons'),
  lesson: (slug: string) => request<LessonDetails>(`/api/lessons/${encodeURIComponent(slug)}`),
  compile: (code: string, signal?: AbortSignal) =>
    request<{ diagnostics: Diagnostic[]; timeMs: number }>('/api/compile', {
      method: 'POST',
      body: JSON.stringify({ code }),
      signal,
    }),
  solution: (slug: string) => request<{ code: string }>(`/api/lessons/${encodeURIComponent(slug)}/solution`),
  run: (code: string, stdin: string) =>
    request<RunResult>('/api/run', { method: 'POST', body: JSON.stringify({ code, stdin }) }),
  check: (slug: string, code: string) =>
    request<CheckResult>(`/api/lessons/${encodeURIComponent(slug)}/check`, {
      method: 'POST',
      body: JSON.stringify({ code }),
    }),
};
