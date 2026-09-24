import { expect, test } from '@playwright/test';
import { expectNoHorizontalScroll, watchProblems } from './helpers';

test('главная показывает весь курс', async ({ page, baseURL }) => {
  const problems = watchProblems(page, baseURL);
  await page.goto('/');
  await expect(page.locator('h1')).toBeVisible();
  await expect(page.locator('.lesson-row')).toHaveCount(41);
  await expect(page.locator('.module')).toHaveCount(9);
  // Трек «Конфиги»: уроки трека и переключение файлов в витрине.
  await expect(page.locator('.track-lessons a')).toHaveCount(5);
  await page.getByRole('tab', { name: 'players.json' }).click();
  await expect(page.locator('.config-window .window-console')).toContainText('Боб: 110');
  await page.locator('.lesson-row').first().click();
  await expect(page).toHaveURL(/\/lessons\/hello-world/);
  await expect(page.locator('.stepper')).toBeVisible();
  expect(problems).toEqual([]);
});

test('сервер отдаёт мета-теги урока без JavaScript', async ({ request }) => {
  const response = await request.get('/lessons/loops');
  expect(response.status()).toBe(200);
  const html = await response.text();
  expect(html).toMatch(/<title>[^<]*Циклы[^<]*<\/title>/);
  expect(html).toContain('og:title');
});

test('неизвестная страница — 404', async ({ page, request }) => {
  expect((await request.get('/no-such-page')).status()).toBe(404);
  await page.goto('/no-such-page');
  await expect(page.getByRole('heading', { name: 'Страница не найдена' })).toBeVisible();
});

test('заголовки безопасности и кеш', async ({ request }) => {
  const page = await request.get('/');
  expect(page.headers()['content-security-policy']).toContain("default-src 'self'");
  expect(page.headers()['x-content-type-options']).toBe('nosniff');
  const robots = await request.get('/robots.txt');
  expect(await robots.text()).toContain('Sitemap:');
  const health = await request.get('/api/health');
  expect(await health.json()).toMatchObject({ status: 'ok', sandbox: 'ok' });
});

test.describe('телефон', () => {
  test.use({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true });

  for (const path of ['/', '/lessons/arrays?step=2', '/playground', '/privacy']) {
    test(`без горизонтальной прокрутки: ${path}`, async ({ page, baseURL }) => {
      const problems = watchProblems(page, baseURL);
      await page.goto(path);
      await page.waitForLoadState('networkidle');
      await expectNoHorizontalScroll(page);
      expect(problems).toEqual([]);
    });
  }
});
