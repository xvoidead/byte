import type { CheckResult, LessonDetails, LessonSummary, RunResult } from './types';

export class ApiError extends Error {}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(url, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...init?.headers },
    });
  } catch {
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
  lessons: () => request<LessonSummary[]>('/api/lessons'),
  lesson: (slug: string) => request<LessonDetails>(`/api/lessons/${encodeURIComponent(slug)}`),
  run: (code: string, stdin: string) =>
    request<RunResult>('/api/run', { method: 'POST', body: JSON.stringify({ code, stdin }) }),
  check: (slug: string, code: string) =>
    request<CheckResult>(`/api/lessons/${encodeURIComponent(slug)}/check`, {
      method: 'POST',
      body: JSON.stringify({ code }),
    }),
};
