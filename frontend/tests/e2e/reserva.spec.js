const { test, expect } = require('@playwright/test');

function obtenerFechaManana() {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() + 1);
  return fecha.toISOString().split('T')[0];
}

test('debe permitir crear una reserva completando el formulario', async ({ page }) => {
  await page.goto('/reservar');

  await page.getByPlaceholder('Tu nombre completo').fill('Carlos Mendoza');
  await page.getByPlaceholder('Ej: +57 300 123 4567').fill('+57 300 123 4567');
  await page.getByPlaceholder('tu@email.com').fill('carlos.mendoza@example.com');
  await page.locator('select[name="idServicio"]').selectOption({ label: /Consulta General/ });
  await page.locator('input[type="date"]').fill(obtenerFechaManana());
  await page.locator('select[name="hora"]').selectOption('10:00');

  await page.getByRole('button', { name: 'Confirmar Reserva' }).click();

  await expect(page.getByText('Reserva creada exitosamente. Te contactaremos pronto!')).toBeVisible();
});
