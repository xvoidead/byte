import { expect, test } from '@playwright/test';
import { setDraft, watchProblems } from './helpers';

const CHEAT = `public class Main {
    public static void main(String[] args) {
        System.out.println("Площадь: 21");
        System.out.println("Периметр: 20");
        System.out.println("Диагональ больше 7: true");
    }
}
`;

const HONEST = `public class Main {
    public static void main(String[] args) {
        int width = 7;
        int height = 3;
        System.out.println("Площадь: " + width * height);
        System.out.println("Периметр: " + 2 * (width + height));
        System.out.println("Диагональ больше 7: " + (width * width + height * height > 7 * 7));
    }
}
`;

test('урок: шаги, вопросы, подсказки, защита от подгона и разбор', async ({ page, baseURL }) => {
  const problems = watchProblems(page, baseURL);
  await page.goto('/lessons/variables');
  await expect(page.locator('.stepper')).toBeVisible();
  const bars = await page.locator('.stepper-bar').count();
  expect(bars).toBeGreaterThan(2);

  // Кнопка «Далее» и адрес шага
  await page.getByRole('button', { name: /Далее/ }).click();
  await expect(page).toHaveURL(/step=2/);

  // Вопрос: неверный ответ, затем верный
  await page.goto('/lessons/variables?step=4');
  const quiz = page.locator('.quiz').first();
  await expect(quiz).toBeVisible();
  const options = quiz.locator('.quiz-option');
  const count = await options.count();
  for (let i = 0; i < count; i++) {
    await options.nth(i).click();
    if (await quiz.locator('.quiz-feedback.correct').isVisible()) break;
  }
  await expect(quiz.locator('.quiz-feedback.correct')).toBeVisible();

  // Стрелки на клавиатуре листают шаги до задания
  await page.locator('h1').click();
  for (let i = 0; i < bars; i++) await page.keyboard.press('ArrowRight');
  await expect(page.locator('section.task')).toBeVisible();
  await expect(page).toHaveURL(new RegExp(`step=${bars}`));

  // Подсказки открываются по одной
  await page.getByRole('button', { name: 'Нужна подсказка' }).click();
  await expect(page.locator('.hint')).toHaveCount(1);

  // Подогнанный вывод проходит тесты, но не требования к решению
  await setDraft(page, 'lesson:variables', CHEAT);
  await page.getByRole('button', { name: 'Проверить' }).click();
  await expect(page.locator('.requirements')).toContainText('из переменных');
  await expect(page.locator('.tests-panel .run-status')).not.toHaveClass(/ok/);

  // Честное решение засчитывается, открывается эталон
  await setDraft(page, 'lesson:variables', HONEST);
  await page.getByRole('button', { name: 'Проверить' }).click();
  await expect(page.locator('.tests-panel .run-status.ok')).toBeVisible();
  await expect(page.locator('.badge-done')).toBeVisible();
  await page.getByRole('button', { name: 'Сравнить с эталонным решением' }).click();
  await expect(page.locator('.solution-code')).toContainText('width');

  // Прогресс виден на главной
  await page.goto('/');
  await expect(page.locator('.lesson-row', { hasText: 'Переменные' }).locator('.lesson-row-status')).toHaveClass(/done/);
  expect(problems).toEqual([]);
});

test('ошибка компиляции при проверке показывает понятное сообщение', async ({ page }) => {
  await page.goto('/lessons/hello-world?step=99');
  await setDraft(page, 'lesson:hello-world', 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("Привет")\n    }\n}\n');
  await page.getByRole('button', { name: 'Проверить' }).click();
  await expect(page.locator('.tests-panel')).toContainText(/точк[аи] с запятой|';'/);
});
