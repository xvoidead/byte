import { expect, test } from '@playwright/test';
import { setDraft, watchProblems } from './helpers';

test('интерактивная консоль: программа ждёт ввода и отвечает', async ({ page, baseURL }) => {
  const problems = watchProblems(page, baseURL);
  await page.goto('/playground');
  await setDraft(
    page,
    'playground',
    `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println("Как тебя зовут?");
        String name = in.nextLine();
        System.out.println("Привет, " + name + "!");
    }
}
`,
  );
  await page.getByRole('button', { name: 'Запустить' }).click();
  await expect(page.locator('.term-output')).toContainText('Как тебя зовут?');
  await page.locator('.term-input').focus();
  await page.keyboard.type('Ада');
  await page.keyboard.press('Enter');
  await expect(page.locator('.term-output')).toContainText('Привет, Ада!');
  await expect(page.locator('.term-exit')).toBeVisible();
  expect(problems).toEqual([]);
});

test('бесконечную программу можно остановить', async ({ page }) => {
  await page.goto('/playground');
  await setDraft(
    page,
    'playground',
    `public class Main {
    public static void main(String[] args) throws Exception {
        int i = 0;
        while (true) {
            System.out.println("тик " + i++);
            Thread.sleep(100);
        }
    }
}
`,
  );
  await page.getByRole('button', { name: 'Запустить' }).click();
  await expect(page.locator('.term-output')).toContainText('тик 3');
  await page.getByRole('button', { name: 'Стоп' }).click();
  await expect(page.locator('.term-exit')).toContainText(/останов/i);
});

test('ошибки подсвечиваются во время набора', async ({ page }) => {
  await page.goto('/playground');
  await setDraft(
    page,
    'playground',
    `public class Main {
    public static void main(String[] args) {
        int x = 5
        System.out.println(x);
    }
}
`,
  );
  await expect(page.locator('.statusbar-problems.errors')).toBeVisible();
  await expect(page.locator('.squiggly-error').first()).toBeAttached();
});

test('песочница не даёт программе выйти за пределы', async ({ page }) => {
  await page.goto('/playground');
  await setDraft(
    page,
    'playground',
    `import java.nio.file.*;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println(Files.readString(Path.of("/etc/passwd")));
    }
}
`,
  );
  await page.getByRole('button', { name: 'Запустить' }).click();
  await expect(page.locator('.term-exit')).toBeVisible();
  await expect(page.locator('.term-output')).not.toContainText('root:');
  await expect(page.locator('.term-output')).toContainText(/AccessControlException|доступ/i);
});
