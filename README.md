# Admin G · App Android nativa

App Android nativa (Kotlin + Jetpack Compose) del panel **Admin G** de
*The Extra Mile / Kalaoz*. Habla con la misma API PHP que el panel web
(`api/data.php`, `api/usuarios.php`, `api/gcal.php`, `api/upload.php`), así que
lo que cambias en la app se ve en el sitio y al revés.

---

## Cómo sacar el APK

1. Sube esta carpeta a tu repositorio de GitHub (rama `main` o `master`).
2. Ve a la pestaña **Actions** → **Build Android APK**. Arranca solo con el push;
   si no, dale a **Run workflow**.
3. Cuando termine (unos 5–8 minutos), entra en la ejecución y baja el artefacto
   **`adminG-debug-apk`**.
4. Descomprime el `.zip`, pasa el `.apk` al celular e instálalo (hay que
   permitir "instalar apps de origen desconocido").

También se genera `adminG-release-apk`. Va firmado con la llave de depuración,
así que se instala igual; para subirlo a Google Play habría que firmarlo con una
llave propia.

## Antes de compilar: revisa tu dominio

En [`app/build.gradle.kts`](app/build.gradle.kts):

```kotlin
buildConfigField("String", "API_BASE_URL",  "\"https://theextramille.online/api/\"")
buildConfigField("String", "SITE_BASE_URL", "\"https://theextramille.online/\"")
```

`SITE_BASE_URL` es la raíz del sitio: se usa para mostrar las imágenes, que en
el servidor se guardan con rutas relativas (`uploads/tours-webp/…`).

---

## Qué hay dentro

Las 13 secciones del panel web, con los mismos permisos por rol.

| Sección | Qué hace |
|---|---|
| **Resumen** | Contadores de reservas, dinero vendido/recibido/saldo y las 5 últimas reservas |
| **Reservas** | Lista con buscador y filtros, detalle, confirmar/cancelar/restaurar, WhatsApp, llamar, factura |
| **Calendario** | Rejilla del mes con las reservas de cada día y cambio de fecha (mueve también el evento de Google Calendar) |
| **Planes** | Crear, editar, ocultar y borrar planes, con subida de imagen |
| **Config. de planes** | Descuentos (% o monto, por plan o todos), muelles con foto y enlace de mapa, y vendedores con su enlace propio |
| **Beneficios** | Catálogo con imágenes y mapa, beneficios ya reservados por clientes, y los mensajes del correo del tiquete |
| **Blog** | Entradas con portada, publicado/borrador, destacados en barra y pie, y SEO por entrada |
| **SEO** | Metadatos con vista previa de Google, imagen para redes, sitemap, buscador inteligente y regenerar el sitio |
| **Extracto** | Ventas con filtros por fecha, estado, pago y plan; totales y exportación a CSV para compartir |
| **Actividad** | Quién hizo qué en el panel, con búsqueda y filtro por tipo |
| **Ajustes** | 13 sub-secciones: marca, portada, textos, colores, empresa, WhatsApp, operador, Google Calendar, Bold, reCAPTCHA, favicon, política del tiquete y los bancos de FAQ/info/horarios/etiquetas |
| **Usuarios** | Usuarios del panel y sus roles (solo Super Admin) |
| **Google Calendar** | Cuentas vinculadas, cambiar de cuenta y desvincular |

### Roles

Son los mismos tres del panel (`admin-js/core.js`), portados en
[`util/Permissions.kt`](app/src/main/java/com/theextramile/admin/util/Permissions.kt):

| Rol | Qué ve |
|---|---|
| **Super Admin** (`super`) | Todo |
| **Editor** (`viewer`) | Resumen, Reservas, Calendario + Planes, Extracto y Beneficios |
| **Gestor de Reservas** (`reservations`) | Resumen, Reservas y Calendario |

Ocultar una sección aquí es comodidad, no seguridad: el backend vuelve a
comprobar el permiso en cada endpoint con `requirePanelAdmin()`.

---

## Cosas que conviene saber si tocas el código

Son detalles del backend que ya causaron problemas y están resueltos en el
código; si los ignoras, se rompen cosas de forma silenciosa.

- **Guardar reemplaza la colección entera.** `saveTours`, `saveReservations`,
  `saveBenefits`… no guardan un elemento: reescriben el archivo JSON completo.
  Hay que cargar la lista, cambiarla en memoria y mandarla entera.
- **`saveSettings` reemplaza TODO el settings.** Por eso Ajustes tiene un solo
  borrador y las demás pantallas que tocan algo de settings (SEO) usan
  `SettingsRepository.patch { }`, que parte de lo que ya hay guardado.
- **Al editar, siempre `copy()` del objeto cargado, nunca un objeto nuevo.**
  Un plan trae galería, horarios, muelle, precio neto, reseñas y traducción al
  inglés que la app no edita; construirlo desde cero los borraría del sitio.
- **El dinero llega como número o como texto** (`840000` o `"840.000"`). Lo
  resuelve [`FlexibleIntAdapter`](app/src/main/java/com/theextramile/admin/data/api/FlexibleInt.kt):
  lee las dos formas y **escribe siempre entero**. Si se escribiera `840000.0`,
  el backend se queda con los dígitos y lo convertiría en 8.400.000.
- **No leas `total` directamente**, usa `paymentInfo()` de
  [`util/Money.kt`](app/src/main/java/com/theextramile/admin/util/Money.kt):
  reproduce el fallback precio × pasajeros del panel.
- **`upload.php` solo acepta 5 carpetas**: `tours`, `hero`, `brand`, `favicon`
  y `blog`. Las imágenes de muelles y beneficios van como `tours`, igual que en
  la web.
- **Los secretos (Bold, reCAPTCHA) no se leen nunca.** El servidor no los
  devuelve; el campo aparece vacío y solo se manda si escribes algo.

## Estructura

```
app/src/main/java/com/theextramile/admin/
├── MainActivity.kt          Navegación de las 13 secciones + guard de permisos
├── TEMApplication.kt        Repositorios de toda la app
├── data/
│   ├── api/                 TEMApiService (todas las acciones) + ApiClient
│   ├── model/               Reservation, Tour, SiteSettings, Benefit, BlogPost…
│   └── repository/          Uno por área + ApiResult compartido
├── ui/
│   ├── components/          GlassComponents + AdminComponents (piezas comunes)
│   ├── overview/ reservations/ calendar/ tours/ planconfig/ benefits/
│   ├── blog/ seo/ extracto/ activity/ settings/ users/ gcal/ login/
│   └── theme/
└── util/
    ├── Permissions.kt       Roles y secciones
    ├── Money.kt             payParse / paymentInfo
    └── PhoneUtil.kt
```

## Lo que todavía no está al nivel del panel web

- **Reservas** cubre lo del día a día (ver, buscar, filtrar, confirmar,
  cancelar, restaurar, WhatsApp, llamar, factura), pero aún le faltan cosas del
  panel: registrar pago parcial y estados "Pago recibido", elegir muelle y hora
  de llegada, aplicar descuento y vendedor a mano, crear una reserva desde cero
  y las vistas previas de los correos.
- **Planes** edita los campos principales; la galería, los horarios con sus
  bloqueos, las reseñas y la traducción al inglés se siguen editando desde el
  panel web (se conservan intactas al guardar desde la app).
- El **buscador inteligente** de SEO deja activarlo y ponerle textos, pero las
  frases de búsqueda de cada plan son cientos de líneas y se editan en la web.

## Seguridad

Este repositorio **no** lleva nada de la carpeta `data/` del sitio. Ahí viven
`bold_secret.json`, `gcal_credentials.json`, `fcm_service_account.json`, la
llave DKIM y los tokens de sesión: si eso llega a GitHub, cualquiera podría
cobrar con tu pasarela o firmar correos a tu nombre. La app pide esos datos al
servidor con el token del login, que es como debe ser.

## Tecnologías

Kotlin 1.9.22 · Jetpack Compose (BOM 2024.02.01) · Material 3 · Retrofit +
OkHttp + Gson · Coil · Navigation Compose · DataStore · Coroutines + StateFlow ·
Firebase Cloud Messaging.
