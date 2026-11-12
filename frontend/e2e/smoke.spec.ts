import { expect, test } from '@playwright/test'

test.describe('Zenius One Enterprise OS E2E Auth Gate & Responsive Suite', () => {
  test('enforces login before exposing any application modules', async ({ page }) => {
    for (const route of ['/', '/apps', '/crm', '/finance', '/hr', '/projects', '/analytics', '/ai', '/admin']) {
      await page.goto(route)
      await expect(page).toHaveURL(/\/login/)
      await expect(page.getByRole('button', { name: /^Sign In$/i })).toBeVisible()
    }
  })
})
