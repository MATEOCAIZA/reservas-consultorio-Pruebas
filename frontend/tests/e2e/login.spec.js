const { test, expect } = require('@playwright/test');

test('debe abrir la página de inicio y mostrar el formulario', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByRole('heading', { name: 'Sistema de Reservas' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Iniciar Sesión' })).toBeVisible();
  await expect(page.getByPlaceholder('admin@reservas.com')).toBeVisible();
});

test('debe permitir iniciar sesión con credenciales válidas', async ({ page }) => {
  await page.goto('/');

  await page.getByPlaceholder('admin@reservas.com').fill('admin@reservas.com');
  await page.getByPlaceholder('Tu contraseña').fill('password');
  await page.getByRole('button', { name: 'Iniciar Sesión' }).click();

  await expect(page).toHaveURL(/\/dashboard/);
});
