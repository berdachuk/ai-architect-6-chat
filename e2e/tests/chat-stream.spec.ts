import { expect, test } from '@playwright/test';

test('streams stub assistant reply in e2e profile', async ({ page }) => {
  await page.goto('/');
  await page.locator('#composer').fill('hello e2e');
  await page.getByRole('button', { name: 'Send' }).click();

  const assistant = page.locator('.message-assistant .message-content').last();
  await expect(assistant).toContainText('Hello world', { timeout: 30_000 });
  await expect(page.locator('#composer')).toBeEnabled();
});
