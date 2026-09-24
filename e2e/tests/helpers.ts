import { expect, type Page } from '@playwright/test';

/** Собирает ошибки страницы: исключения, ошибки консоли, нарушения CSP и запросы на чужие адреса. */
export function watchProblems(page: Page, baseURL: string | undefined, allowed: RegExp[] = []): string[] {
  const problems: string[] = [];
  const origin = new URL(baseURL ?? 'http://localhost').origin;
  page.on('pageerror', (e) => problems.push(`pageerror: ${e.message}`));
  page.on('console', (m) => {
    if (m.type() === 'error' && !allowed.some((re) => re.test(m.text()))) problems.push(`console: ${m.text()}`);
  });
  page.on('request', (r) => {
    const url = new URL(r.url());
    if (!['data:', 'blob:'].includes(url.protocol) && url.origin !== origin) problems.push(`external: ${r.url()}`);
  });
  return problems;
}

/** Кладёт код в черновик редактора и перезагружает страницу — быстрее и надёжнее набора в Monaco. */
export async function setDraft(page: Page, key: string, code: string) {
  await page.evaluate(([k, c]) => localStorage.setItem(`byte:code:${k}`, c), [key, code]);
  await page.reload();
  await expect(page.locator('.monaco-editor .view-lines')).toBeVisible();
}

export async function expectNoHorizontalScroll(page: Page) {
  expect(await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)).toBeLessThanOrEqual(0);
}
