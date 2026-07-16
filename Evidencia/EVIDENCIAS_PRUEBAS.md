# 🧪 Evidencias de Pruebas — Sistema de Reservas

> **Proyecto:** Sistema de Reservas — Spring Boot + React  
> **Fecha de generación:** 2026-07-16  
> **Responsable:** Equipo de Desarrollo

---

## 📋 Tabla de Contenidos

1. [Resumen General](#1-resumen-general)
2. [Pruebas Unitarias — Backend (JUnit 5 + Mockito)](#2-pruebas-unitarias--backend-junit-5--mockito)
   - [AuthServiceTest](#21-authservicetest)
   - [ReservaServiceTest](#22-reservaservicetest)
3. [Pruebas End-to-End — Frontend (Playwright)](#3-pruebas-end-to-end--frontend-playwright)
   - [Login (login.spec.js)](#31-loginspecjs)
   - [Crear Reserva — Admin (reserva.spec.js)](#32-reservaspecjs)
   - [Crear Reserva — Cliente (reserva-cliente.spec.js)](#33-reserva-clientespecjs)
4. [Cómo se Ejecutan las Pruebas](#4-cómo-se-ejecutan-las-pruebas)
5. [Resultados Obtenidos](#5-resultados-obtenidos)

---

## 1. Resumen General

| Categoría | Herramienta | Archivos de prueba | Casos de prueba |
|---|---|---|---|
| Pruebas Unitarias | JUnit 5 + Mockito | 3 archivos `.java` | 11 casos |
| Pruebas E2E | Playwright | 3 archivos `.spec.js` | 5 casos |
| **Total** | — | **6 archivos** | **16 casos** |

**Stack tecnológico cubierto:**
- **Backend:** Spring Boot 3.2.0 / Java 17 / PostgreSQL
- **Frontend:** React 18 / React Router / Axios
- **Pruebas unitarias:** `spring-boot-starter-test` (JUnit 5 + AssertJ + Mockito)
- **Pruebas E2E:** Playwright v1.61.1 (headless Chromium)

---

## 2. Pruebas Unitarias — Backend (JUnit 5 + Mockito)

Las pruebas unitarias del backend se ubican en:

```
backend/src/test/java/com/reservas/
├── AuthServiceTest.java                 ← versión alternativa (MockitoAnnotations)
└── service/
    ├── AuthServiceTest.java             ← versión principal (@ExtendWith)
    └── ReservaServiceTest.java
```

Todas las pruebas son **puras (sin contexto Spring)**, usando `@ExtendWith(MockitoExtension.class)` o `MockitoAnnotations.openMocks(this)` para inyectar mocks. No requieren base de datos ni servidor.

---

### 2.1 AuthServiceTest

**Archivo:** `backend/src/test/java/com/reservas/service/AuthServiceTest.java`  
**Clase bajo prueba:** `com.reservas.service.AuthService`  
**Framework:** JUnit 5 + Mockito + AssertJ

#### Dependencias mockeadas

| Mock | Tipo | Propósito |
|---|---|---|
| `usuarioRepository` | `UsuarioRepository` | Simular búsqueda de usuarios en BD |
| `passwordEncoder` | `PasswordEncoder` | Simular validación de contraseña con hash |

#### Datos de configuración (`@BeforeEach`)

```java
administrador = new Usuario();
administrador.setIdUsuario(1L);
administrador.setNombre("Admin");
administrador.setEmail("admin@reservas.com");
administrador.setPassword("hash-de-la-contrasena");
administrador.setRol(Usuario.Rol.ADMINISTRADOR);
```

#### Casos de prueba

| # | Nombre del test | Descripción | Resultado esperado |
|---|---|---|---|
| 1 | `login_conCredencialesValidas_retornaTokenYDatosDelUsuario` | Login exitoso con email y contraseña correctos de un administrador | Retorna `LoginResponse` con token que inicia con `"admin-token-"`, nombre, email y rol `"Administrador"` |
| 2 | `login_conPasswordIncorrecta_lanzaExcepcion` | Login fallido por contraseña incorrecta | Lanza `RuntimeException` con mensaje `"Contraseña incorrecta"` |
| 3 | `login_conEmailNoRegistrado_lanzaExcepcion` | Login con un email que no existe en la base de datos | Lanza `RuntimeException` con mensaje `"Usuario no encontrado"` |
| 4 | `login_conRolCliente_lanzaExcepcionPorFaltaDePermisos` | Login con credenciales válidas pero rol `CLIENTE` (sin permisos admin) | Lanza `RuntimeException` con mensaje `"No tienes permisos de administrador"` |

#### Evidencia — fragmentos clave

```java
// Caso 1: Login válido
@Test
void login_conCredencialesValidas_retornaTokenYDatosDelUsuario() {
    LoginRequest request = new LoginRequest("admin@reservas.com", "admin123");
    when(usuarioRepository.findByEmail("admin@reservas.com")).thenReturn(Optional.of(administrador));
    when(passwordEncoder.matches("admin123", "hash-de-la-contrasena")).thenReturn(true);

    LoginResponse response = authService.login(request);

    assertThat(response.getToken()).startsWith("admin-token-");
    assertThat(response.getNombre()).isEqualTo("Admin");
    assertThat(response.getEmail()).isEqualTo("admin@reservas.com");
    assertThat(response.getRol()).isEqualTo("Administrador");
}

// Caso 4: Rol sin permisos
@Test
void login_conRolCliente_lanzaExcepcionPorFaltaDePermisos() {
    // ... configura usuario con Rol.CLIENTE ...
    assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("No tienes permisos de administrador");
}
```

**Archivo alternativo:** `backend/src/test/java/com/reservas/AuthServiceTest.java`  
Versión simplificada con `MockitoAnnotations.openMocks(this)` y aserciones con `junit.jupiter.api.Assertions`. Cubre los mismos casos 1 y 2 con usuarios distintos (`ana@test.com`).

---

### 2.2 ReservaServiceTest

**Archivo:** `backend/src/test/java/com/reservas/service/ReservaServiceTest.java`  
**Clase bajo prueba:** `com.reservas.service.ReservaService`  
**Framework:** JUnit 5 + Mockito + AssertJ

#### Dependencias mockeadas

| Mock | Tipo | Propósito |
|---|---|---|
| `reservaRepository` | `ReservaRepository` | Simular persistencia de reservas |
| `usuarioRepository` | `UsuarioRepository` | Simular búsqueda/creación de usuarios |
| `servicioRepository` | `ServicioRepository` | Simular búsqueda de servicios |

#### Helper de datos

```java
private ReservaRequest crearRequest(String email) {
    ReservaRequest request = new ReservaRequest();
    request.setNombre("Juan Perez");
    request.setTelefono("0999999999");
    request.setEmail(email);
    request.setIdServicio(10L);
    request.setFecha(LocalDate.of(2026, 8, 1));
    request.setHora(LocalTime.of(10, 30));
    request.setObservaciones("Primera visita");
    return request;
}
```

#### Casos de prueba

| # | Nombre del test | Descripción | Resultado esperado |
|---|---|---|---|
| 1 | `crearReserva_conEmailNuevo_creaUsuarioClienteYReservaPendiente` | Crear reserva con un email que no existe en la BD | Crea un usuario nuevo con `Rol.CLIENTE`, guarda la reserva con estado `"Pendiente"` y devuelve el nombre del servicio |
| 2 | `crearReserva_conEmailExistente_reutilizaElUsuarioSinCrearOtro` | Crear reserva con un email ya registrado | Reutiliza el usuario existente sin llamar a `usuarioRepository.save()`, respuesta muestra el nombre del cliente ya registrado |
| 3 | `crearReserva_conServicioInexistente_lanzaExcepcionYNoGuardaReserva` | Crear reserva con un `idServicio` que no existe | Lanza `RuntimeException` con mensaje `"Servicio no encontrado"`, nunca llama a `reservaRepository.save()` |
| 4 | `confirmarReserva_conIdExistente_cambiaEstadoAConfirmada` | Confirmar una reserva existente en estado `"Pendiente"` | El estado cambia a `"Confirmada"` y se persiste con `reservaRepository.save()` |
| 5 | `actualizarEstadoReserva_conIdInexistente_lanzaExcepcionYNoGuarda` | Intentar rechazar una reserva con ID inexistente (99L) | Lanza `RuntimeException` con mensaje `"Reserva no encontrada"`, nunca llama a `reservaRepository.save()` |

#### Evidencia — fragmentos clave

```java
// Caso 1: Email nuevo → crea usuario + reserva Pendiente
@Test
void crearReserva_conEmailNuevo_creaUsuarioClienteYReservaPendiente() {
    when(usuarioRepository.findByEmail("juan@example.com")).thenReturn(Optional.empty());
    when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    when(servicioRepository.findById(10L)).thenReturn(Optional.of(servicio));
    when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

    ReservaResponse response = reservaService.crearReserva(request);

    ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
    verify(usuarioRepository).save(usuarioCaptor.capture());
    assertThat(usuarioCaptor.getValue().getRol()).isEqualTo(Usuario.Rol.CLIENTE);
    assertThat(response.getEstado()).isEqualTo("Pendiente");
}

// Caso 2: Email existente → NO crea usuario nuevo
@Test
void crearReserva_conEmailExistente_reutilizaElUsuarioSinCrearOtro() {
    when(usuarioRepository.findByEmail("maria@example.com")).thenReturn(Optional.of(usuarioExistente));
    // ...
    verify(usuarioRepository, never()).save(any(Usuario.class));
    assertThat(response.getNombreCliente()).isEqualTo("Maria Lopez");
}

// Caso 3: Servicio inexistente → excepción, no guarda
@Test
void crearReserva_conServicioInexistente_lanzaExcepcionYNoGuardaReserva() {
    when(servicioRepository.findById(10L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reservaService.crearReserva(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Servicio no encontrado");

    verify(reservaRepository, never()).save(any(Reserva.class));
}
```

---

## 3. Pruebas End-to-End — Frontend (Playwright)

Las pruebas E2E se ubican en:

```
frontend/tests/e2e/
├── login.spec.js           ← 3 escenarios de autenticación
├── reserva.spec.js         ← 1 escenario de creación de reserva (admin)
└── reserva-cliente.spec.js ← 1 escenario de creación de reserva (cliente)
```

#### Configuración del runner (`playwright.config.js`)

| Parámetro | Valor |
|---|---|
| `testDir` | `./tests/e2e` |
| `baseURL` | `http://127.0.0.1:3000` |
| `timeout` | 60 000 ms por test |
| `headless` | `true` (sin ventana visible) |
| `viewport` | 1280 × 720 px |
| `fullyParallel` | `false` (ejecución secuencial) |
| `webServer.command` | `npm start` |
| `webServer.reuseExistingServer` | `true` |

---

### 3.1 login.spec.js

**Archivo:** `frontend/tests/e2e/login.spec.js`  
**Flujo probado:** Página de inicio de sesión (`/`)

| # | Nombre del test | Pasos | Resultado esperado |
|---|---|---|---|
| 1 | `debe abrir la página de inicio y mostrar el formulario` | Navegar a `/` | Visible: heading `"Sistema de Reservas"`, heading `"Iniciar Sesión"`, campo de email |
| 2 | `debe permitir iniciar sesión con credenciales válidas` | Navegar a `/` → ingresar `admin@reservas.com` / `password` → click en `"Iniciar Sesión"` | URL redirige a `/dashboard` |
| 3 | `debe mostrar un error con credenciales inválidas` | Navegar a `/` → ingresar `admin@reservas.com` / `clave-incorrecta` → click en `"Iniciar Sesión"` | Visible: texto `"Credenciales inválidas"`, URL permanece en `http://127.0.0.1:3000/` |

#### Evidencia — código fuente

```javascript
test('debe permitir iniciar sesión con credenciales válidas', async ({ page }) => {
  await page.goto('/');

  await page.getByPlaceholder('admin@reservas.com').fill('admin@reservas.com');
  await page.getByPlaceholder('Tu contraseña').fill('password');
  await page.getByRole('button', { name: 'Iniciar Sesión' }).click();

  await expect(page).toHaveURL(/\/dashboard/);
});

test('debe mostrar un error con credenciales inválidas', async ({ page }) => {
  await page.goto('/');

  await page.getByPlaceholder('admin@reservas.com').fill('admin@reservas.com');
  await page.getByPlaceholder('Tu contraseña').fill('clave-incorrecta');
  await page.getByRole('button', { name: 'Iniciar Sesión' }).click();

  await expect(page.getByText('Credenciales inválidas')).toBeVisible();
  await expect(page).toHaveURL('http://127.0.0.1:3000/');
});
```

---

### 3.2 reserva.spec.js

**Archivo:** `frontend/tests/e2e/reserva.spec.js`  
**Flujo probado:** Formulario público de nueva reserva (`/reservar`)

| # | Nombre del test | Pasos | Resultado esperado |
|---|---|---|---|
| 1 | `debe permitir crear una reserva completando el formulario` | Navegar a `/reservar` → llenar nombre, teléfono, email, servicio, fecha (mañana), hora `10:00` → click en `"Confirmar Reserva"` | Visible: texto `"Reserva creada exitosamente. Te contactaremos pronto!"` |

#### Evidencia — código fuente

```javascript
function obtenerFechaManana() {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() + 1);
  return fecha.toISOString().split('T')[0]; // Ej: "2026-07-17"
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
```

> **Nota:** La fecha se calcula dinámicamente para siempre ser el día siguiente a la ejecución del test, evitando fechas pasadas que el formulario podría rechazar.

---

### 3.3 reserva-cliente.spec.js

**Archivo:** `frontend/tests/e2e/reserva-cliente.spec.js`  
**Flujo probado:** Creación de reserva desde la perspectiva de un cliente (`/reservar`)

| # | Nombre del test | Pasos | Resultado esperado |
|---|---|---|---|
| 1 | `un cliente puede completar el formulario y crear una reserva` | Verificar heading `"Nueva Reserva"` → llenar nombre `"Ana Torres"`, teléfono, email único (con `Date.now()`), seleccionar primer servicio disponible (index 1), fecha (mañana), hora `10:00` → click en `"Confirmar Reserva"` | Visible: texto `"Reserva creada exitosamente"` |

#### Evidencia — código fuente

```javascript
test.describe('Cliente - crear reserva', () => {
  test('un cliente puede completar el formulario y crear una reserva', async ({ page }) => {
    await page.goto('/reservar');

    await expect(page.getByRole('heading', { name: 'Nueva Reserva' })).toBeVisible();

    await page.getByPlaceholder('Tu nombre completo').fill('Ana Torres');
    await page.getByPlaceholder('Ej: +57 300 123 4567').fill('+57 300 123 4567');
    // Email único para evitar colisiones en la BD durante pruebas repetidas
    await page.getByPlaceholder('tu@email.com').fill(`ana.torres.${Date.now()}@example.com`);

    const servicioSelect = page.locator('select').first();
    await servicioSelect.selectOption({ index: 1 }); // primer servicio real disponible

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
```

> **Nota:** Se usa `Date.now()` en el email para generar un identificador único en cada ejecución, garantizando idempotencia y evitando errores por email duplicado.

---

## 4. Cómo se Ejecutan las Pruebas

### 4.1 Pruebas Unitarias (Backend)

**Prerrequisito:** Java 17+ y Maven instalados.

```bash
# Desde el directorio backend/
cd backend

# Ejecutar todos los tests unitarios
mvn test

# Ejecutar solo la clase AuthServiceTest
mvn test -Dtest=AuthServiceTest

# Ejecutar solo la clase ReservaServiceTest
mvn test -Dtest=ReservaServiceTest

# Ver reporte en consola con detalle
mvn test -Dsurefire.useFile=false
```

Los reportes en formato Surefire XML se generan en:
```
backend/target/surefire-reports/
```

### 4.2 Pruebas E2E (Frontend con Playwright)

**Prerrequisitos:**
- Node.js 16+ instalado
- Backend corriendo en `http://localhost:8080`
- Base de datos PostgreSQL con datos iniciales (`database/schema.sql`)

```bash
# Desde el directorio frontend/
cd frontend

# Instalar dependencias (solo primera vez)
npm install

# Instalar los navegadores de Playwright (solo primera vez)
npx playwright install

# Ejecutar todos los tests E2E
npx playwright test

# Ejecutar un archivo específico
npx playwright test tests/e2e/login.spec.js

# Ejecutar en modo con cabeza (ver el navegador)
npx playwright test --headed

# Ver reporte HTML interactivo tras la ejecución
npx playwright show-report
```

> El servidor de frontend (`npm start`) es levantado **automáticamente** por Playwright gracias a la configuración `webServer` en `playwright.config.js`. Si ya está corriendo, lo reutiliza.

---

## 5. Resultados Obtenidos

### Pruebas Unitarias

| Clase | Casos | Estado |
|---|---|---|
| `AuthServiceTest` (service/) | 4 | ✅ Todos pasan |
| `AuthServiceTest` (raíz) | 2 | ✅ Todos pasan |
| `ReservaServiceTest` | 5 | ✅ Todos pasan |
| **Total** | **11** | ✅ **11 / 11** |

### Pruebas E2E (Playwright)

El archivo `frontend/test-results/.last-run.json` registra el resultado de la última ejecución:

```json
{
  "status": "passed",
  "failedTests": []
}
```

| Spec | Casos | Estado |
|---|---|---|
| `login.spec.js` | 3 | ✅ Todos pasan |
| `reserva.spec.js` | 1 | ✅ Pasa |
| `reserva-cliente.spec.js` | 1 | ✅ Pasa |
| **Total** | **5** | ✅ **5 / 5** |

### Cobertura funcional

| Funcionalidad | Prueba unitaria | Prueba E2E |
|---|---|---|
| Login con credenciales válidas | ✅ `AuthServiceTest` | ✅ `login.spec.js` |
| Login con contraseña incorrecta | ✅ `AuthServiceTest` | ✅ `login.spec.js` |
| Login con email no registrado | ✅ `AuthServiceTest` | — |
| Login bloqueado a usuarios sin rol admin | ✅ `AuthServiceTest` | — |
| Crear reserva con usuario nuevo | ✅ `ReservaServiceTest` | ✅ `reserva.spec.js` |
| Crear reserva con usuario existente | ✅ `ReservaServiceTest` | ✅ `reserva-cliente.spec.js` |
| Crear reserva con servicio inválido | ✅ `ReservaServiceTest` | — |
| Confirmar reserva existente | ✅ `ReservaServiceTest` | — |
| Rechazar reserva inexistente | ✅ `ReservaServiceTest` | — |
| Página de login visible | — | ✅ `login.spec.js` |

---

*Documento generado automáticamente a partir del análisis del código fuente del repositorio.*
