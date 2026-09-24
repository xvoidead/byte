import { defineConfig, devices } from '@playwright/test';

// По умолчанию тесты сами запускают собранный сервер (backend/target/byte.jar) со статикой из frontend/dist.
// Чтобы проверить уже запущенный сайт, задайте E2E_BASE_URL (и E2E_ADMIN_TOKEN для страницы статистики).
const external = process.env.E2E_BASE_URL;
const port = 8090;

export default defineConfig({
  testDir: './tests',
  timeout: 60_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  forbidOnly: !!process.env.CI,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: external ?? `http://localhost:${port}`,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    ...devices['Desktop Chrome'],
    viewport: { width: 1440, height: 900 },
  },
  webServer: external
    ? undefined
    : {
        command: [
          'java -jar ../backend/target/byte.jar',
          `--server.port=${port}`,
          '--spring.web.resources.static-locations=file:../frontend/dist/',
          // Тесты запускают код чаще живого ученика — лимиты поднимаем, чтобы проверять поведение, а не их.
          '--byte.limits.runs-per-minute=600',
          '--byte.limits.run-burst=100',
          '--byte.limits.concurrent-runs-per-ip=8',
        ].join(' '),
        url: `http://localhost:${port}/api/health`,
        env: { BYTE_ADMIN_TOKEN: 'e2e-token' },
        timeout: 120_000,
        reuseExistingServer: !process.env.CI,
        stdout: 'ignore',
        stderr: 'pipe',
      },
});
