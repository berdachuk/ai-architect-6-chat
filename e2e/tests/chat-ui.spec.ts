import { expect, test } from '@playwright/test';

test.describe('Chat UI smoke', () => {
  test('home page loads composer and MCP panel', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('navigation').getByText('AI Chat')).toBeVisible();
    await expect(page.locator('#composer')).toBeVisible();
    await expect(page.getByText('MCP Tools')).toBeVisible();
    await expect(page.getByRole('button', { name: '+ New Chat' })).toBeVisible();
  });

  test('creates a new chat from navbar', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: '+ New Chat' }).click();
    await expect(page).toHaveURL(/\/chat\/[a-f0-9]+/);
    await expect(page.locator('#composer')).toBeVisible();
  });

  test('mcp panel finishes loading', async ({ page }) => {
    await page.goto('/');
    const list = page.locator('#mcpConnectionList');
    await expect(list).not.toHaveText('Loading…');
    await expect(list).toBeVisible();
  });

  test('agent panel is collapsed before first message', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('#agentPanel')).toHaveClass(/d-none/);
  });

  test('sidebar lists chats after navigation', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('#chatList .chat-item').first()).toBeVisible();
  });
});
