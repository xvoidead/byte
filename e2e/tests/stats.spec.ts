import { expect, test } from '@playwright/test';

const TOKEN = process.env.E2E_ADMIN_TOKEN ?? 'e2e-token';

test('события урока попадают в статистику, дашборд открывается по токену', async ({ browser, baseURL }) => {
  // Ученик открывает урок и отвечает на вопрос; при закрытии вкладки события уходят через sendBeacon.
  const student = await browser.newContext({ baseURL });
  const lesson = await student.newPage();
  await lesson.goto('/lessons/loops');
  await expect(lesson.locator('.stepper')).toBeVisible();
  const beacon = lesson.waitForResponse((r) => r.url().endsWith('/api/events'));
  // Так браузер ведёт себя, когда вкладку закрывают или сворачивают.
  await lesson.evaluate(() => window.dispatchEvent(new Event('pagehide')));
  expect((await beacon).status()).toBe(204);
  await student.close();

  const owner = await browser.newContext({ baseURL });
  const page = await owner.newPage();
  await page.goto('/stats');
  await page.getByPlaceholder('Токен').fill('wrong');
  await page.getByRole('button', { name: 'Открыть' }).click();
  await expect(page.locator('.error-text')).toContainText('Неверный токен');

  await page.getByPlaceholder('Токен').fill(TOKEN);
  await page.getByRole('button', { name: 'Открыть' }).click();
  await expect(page.locator('.stats-tile')).toHaveCount(5);
  const loops = page.locator('.stats-table tr', { hasText: 'Циклы: for и while' });
  await expect(loops.locator('td').nth(1)).not.toHaveText('0');

  // Подсказка графика появляется при наведении
  await page.locator('.stats-chart-wide .stats-bar').first().hover();
  await expect(page.locator('.stats-tooltip')).toContainText('открыли');
  await owner.close();
});

test('при Global Privacy Control статистика не отправляется', async ({ browser, baseURL }) => {
  const context = await browser.newContext({ baseURL });
  await context.addInitScript(() => Object.defineProperty(navigator, 'globalPrivacyControl', { get: () => true }));
  const page = await context.newPage();
  let sent = 0;
  page.on('request', (r) => {
    if (r.url().endsWith('/api/events')) sent++;
  });
  await page.goto('/lessons/hello-world');
  await expect(page.locator('.stepper')).toBeVisible();
  await page.getByRole('button', { name: /Далее/ }).click();
  await page.evaluate(() => window.dispatchEvent(new Event('pagehide')));
  await page.waitForTimeout(500);
  await context.close();
  expect(sent).toBe(0);
});
