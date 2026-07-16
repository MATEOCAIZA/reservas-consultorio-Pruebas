const { test, expect } = require('@playwright/test');

test.describe('Cliente - crear reserva', () => {
  test('un cliente puede completar el formulario y crear una reserva', async ({ page }) => {
    await page.goto('/reservar');

    await expect(page.getByRole('heading', { name: 'Nueva Reserva' })).toBeVisible();

    await page.getByPlaceholder('Tu nombre completo').fill('Ana Torres');
    await page.getByPlaceholder('Ej: +57 300 123 4567').fill('+57 300 123 4567');
    await page.getByPlaceholder('tu@email.com').fill(`ana.torres.${Date.now()}@example.com`);

    const servicioSelect = page.locator('select').first();
    await servicioSelect.selectOption({ index: 1 });

    const manana = new Date();
    manana.setDate(manana.getDate() + 1);
    const fechaISO = manana.toISOString().split('T')[0];
    await page.locator('input[type="date"]').fill(fechaISO);

    const horaSelect = page.locator('select').nth(1);
    await horaSelect.selectOption('10:00');

    await page.getByRole('button', { name: 'Confirmar Reserva' }).click();

    await expect(page.getByText('Reserva creada exitosamente')).toBeVisible();
  });
});
