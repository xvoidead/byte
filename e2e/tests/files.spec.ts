import { expect, test } from '@playwright/test';
import { watchProblems } from './helpers';

test('песочница: шаблон с конфигом, файлы после запуска, новый файл', async ({ page, baseURL }) => {
  const problems = watchProblems(page, baseURL);
  page.on('dialog', (dialog) => dialog.accept());
  await page.goto('/playground');
  await expect(page.locator('.monaco-editor .view-lines')).toBeVisible();

  await page.selectOption('.template-picker select', { label: 'Конфиг на YAML' });
  await expect(page.locator('.file-tab-name', { hasText: 'config.yml' })).toBeVisible();
  await page.getByRole('button', { name: 'Запустить' }).click();
  await expect(page.locator('.term-output')).toContainText('Мест на сервере: 20');
  // Файл, который создала программа, появляется во вкладках.
  await expect(page.locator('.term-files')).toContainText('создан last-start.txt');
  await page.locator('.file-tab-name', { hasText: 'last-start.txt' }).click();
  await expect(page.locator('.monaco-editor .view-lines')).toContainText('мест: 20');

  // Новый класс из заготовки.
  await page.locator('.file-tab-add').click();
  await page.keyboard.type('Util.java');
  await page.keyboard.press('Enter');
  await expect(page.locator('.file-tab.active')).toContainText('Util.java');
  await expect(page.locator('.monaco-editor .view-lines')).toContainText('public class Util');

  // Недопустимое имя не принимается.
  await page.locator('.file-tab-add').click();
  await page.keyboard.type('../secret.txt');
  await page.keyboard.press('Enter');
  await expect(page.locator('.file-tab-problem')).toBeVisible();
  await page.keyboard.press('Escape');
  expect(problems).toEqual([]);
});

test('песочница: JSON через Gson, ошибка в другом файле ведёт на его вкладку', async ({ page }) => {
  page.on('dialog', (dialog) => dialog.accept());
  await page.goto('/playground');
  await page.selectOption('.template-picker select', { label: 'JSON с Gson' });
  await expect(page.locator('.file-tab-name', { hasText: 'players.json' })).toBeVisible();
  await page.getByRole('button', { name: 'Запустить' }).click();
  await expect(page.locator('.term-output')).toContainText('Ада: 120');
  await expect(page.locator('.term-files')).toContainText('изменён players.json');

  await page.evaluate(() => {
    const project = JSON.parse(localStorage.getItem('byte:project:playground') ?? '{}');
    project.files = project.files.map((f: { name: string; content: string }) =>
      f.name === 'Player.java' ? { ...f, content: 'public record Player(String name, int score) {\n    void x() { int y = "текст"; }\n}\n' } : f,
    );
    project.active = 'Main.java';
    localStorage.setItem('byte:project:playground', JSON.stringify(project));
  });
  await page.reload();
  await expect(page.locator('.statusbar-problems.errors')).toContainText('Player.java');
  await expect(page.locator('.file-tab', { has: page.locator('.file-tab-errors') })).toContainText('Player.java');
  await page.locator('.statusbar-problems').click();
  await expect(page.locator('.file-tab.active')).toContainText('Player.java');
});
