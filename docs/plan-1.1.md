# Plan de la versión 1.1: lo legal, la web y descargar mis datos

Plan con fecha 2026-11-25, para después de la 1.0 (la entrega del curso, publicada el 2026-11-26 como `v1.0.0`). La 1.1 no añade funciones de
presupuesto. Prepara Sobre-IO para que lo use gente real:

- **lo legal**: política de privacidad, términos de uso, aviso legal, la aceptación al darse de alta y "Descargar mis datos";
- **la web**: una página de presentación y, en el mismo sitio, la app en el navegador.

Al terminarla, todo lo que exige la ley para una app gratuita con datos personales en España queda cubierto.

Se trabaja igual que en la 1.0: un PR por bloque, con su rama y su worktree. Si el bloque toca la base de datos, primero va
su PR en `Sobre-IO-DB` con su decisión (D-57 en adelante). Los textos que ve el usuario van en los dos idiomas. Tamaños
orientativos: S < 2 h, M 2–5 h, L > 5 h.

> Los documentos legales de este plan son plantillas razonables, no asesoría legal. Conviene que alguien con formación
> jurídica les eche un vistazo antes de publicarlos, aunque sea una revisión rápida.

---

## Decisiones tomadas (2026-11-25)

| Tema | Decisión | Por qué |
| --- | --- | --- |
| Cifrado de textos | **No.** | Los importes y las fechas tienen que quedar en claro, porque la base de datos hace todos los cálculos, y eso ya delata mucho (el mismo importe cada día 1 es un alquiler). No se podría vender como privacidad real, y gestionar las claves en grupos compartidos es caro. La promesa es otra: **los datos no se venden, no se ceden y no hay publicidad ni analítica de terceros**. |
| Web | **Sí, en la 1.1.** | Llega a cualquier dispositivo con el mismo código y da un sitio donde explicar el producto. Reabre la decisión cerrada "No desktop, no web". |
| Página de presentación | **Astro**, sitio estático, sin JavaScript en las páginas. | Los documentos legales en Markdown y en dos idiomas, con componentes, y nada que rastree a nadie. |
| Web app | **La misma app de Compose compilada a web** (`wasmJs`), servida en `/app/`. | Ninguna pantalla se escribe dos veces. Su build son ficheros estáticos, así que convive con la página de presentación en el mismo sitio. |
| Hosting | **Cloudflare Workers con ficheros estáticos**, gratuito (Worker `sobre-io-web`, `wrangler.jsonc`, sin código de servidor). | Al conectar el repositorio, Cloudflare creó un Worker en vez de un proyecto de Pages: es su forma actual de servir sitios estáticos. Sirve desde la raíz, `_headers` da la caché y las cabeceras de seguridad, y `dist/404.html` cubre las rutas que no existen. GitHub Pages obligaría a una subruta. |
| Dominio | **Pendiente: el autor lo está buscando.** Hasta entonces, la dirección gratuita del Worker: `sobre-io-web.jclacruzlacruz.workers.dev`. | Un dominio da también el correo de contacto de los documentos (fase 0, punto 5). La app no lleva ninguna URL escrita: lee los enlaces de la base de datos (fase 3), así que cambiar de dominio no obliga a sacar otra versión. |
| Repositorio | **`Sobre-IO-WEB`, nuevo** (`github.com/LacruzJSDev/Sobre-IO-WEB`, privado), para la página de presentación. | Una carpeta y un repositorio por proyecto, como manda el workspace. `Sobre-IO-ANDROID` no sabe nada de la web: solo publica su build web en la release, y la web la recoge al desplegarse. |
| Enlaces en la app | **Sin URLs ni correos escritos en el código.** Las URLs de los documentos salen de `legal_documents`, en la base de datos. | Los textos legales, el dominio y el contacto viven en la web; la app solo los enlaza. |
| Distribución | **APK a mano, Xcode y web.** Sin tiendas. | La decisión cerrada de las tiendas sigue igual. Si un día se publica en Google Play, basta con añadir el formulario "Seguridad de los datos", porque los documentos ya existirán. |

Cambios en la documentación del workspace (se hacen en cuanto se aprueba este plan): en `AGENTS.md`, el mapa con
`Sobre-IO-WEB/` y la decisión cerrada de la web; en `docs/agents/product.md`, quitar "web" de lo que queda fuera del alcance.

---

## Lo que dice la ley y cómo se cubre

| Norma | Qué pide | Dónde se cubre |
| --- | --- | --- |
| RGPD, art. 13 | Informar de quién trata los datos, cuáles, para qué, con qué base legal, a quién se dan, cuánto tiempo se guardan y qué derechos hay | Política de privacidad (fase 2) |
| RGPD, art. 15 y 20 | Acceso y portabilidad: el usuario recibe sus datos en un formato estructurado y legible por máquina | "Descargar mis datos" (fase 4) |
| RGPD, art. 17 | Supresión | Ya existe: "Eliminar mi cuenta" (D-51). La política explica qué se borra y qué se anonimiza |
| RGPD, art. 28 | Contrato con quien trata los datos por ti | El DPA de Supabase, incluido en sus condiciones de servicio (fase 0) |
| RGPD, art. 7 | Poder demostrar que se aceptó | Registro de la versión aceptada y la fecha (fase 3) |
| LOPDGDD, art. 7 | Edad mínima de 14 años para consentir | Términos de uso |
| LSSI, art. 10 | Identificar a quien ofrece un servicio en la web | Aviso legal (fase 2) |
| LSSI, art. 22.2 | Consentimiento para las cookies que no son técnicas | No hay ninguna: la sesión de la web app es técnica y la página de presentación no lleva analítica ni fuentes de terceros, así que **no hace falta banner** |

---

## Fase 0 · Preparación (S, en cuanto salga la 1.0)

1. Actualizar el workspace con las decisiones de arriba (hecho el 2026-11-25).
2. Crear en Cloudflare la cuenta y el Worker conectado a `Sobre-IO-WEB` (hecho el 2026-11-25).
3. Región del proyecto de Supabase: `eu-central-1` (Fráncfort), comprobada el 2026-11-25. El DPA de Supabase forma parte
   de sus condiciones de servicio y se acepta con ellas: el panel solo permite consultarlo, así que no hay nada más que firmar.
4. Qué conserva Supabase Auth tras la baja (comprobado en local el 2026-11-25, repitiendo los pasos de la Edge Function
   `delete-account`): el borrado suave cambia el correo y el teléfono por valores aleatorios, quita la contraseña, vacía
   los metadatos y los datos de la identidad (Google), y borra sesiones y tokens; quedan el `id`, `created_at` y
   `last_sign_in_at`, y el correo se puede volver a registrar. **Pero `auth.audit_log_entries` conserva el correo en claro**
   (alta, cada inicio de sesión y la propia baja). Se corrige en D-57: una función solo para el servicio,
   `private.forget_auth_audit(user_id)`, borra esas filas (por `actor_id` y por `traits.user_id`) y la Edge Function la
   llama después de `deleteUser`. La política de privacidad ya describe este comportamiento, así que **no puede dejar de
   ser borrador hasta que D-57 esté en producción**. Comprobar también en el panel si el proyecto alojado guarda el registro
   de Auth en la base de datos (si no, el problema solo existe en local) y cuánto conserva Supabase sus logs de Auth.
5. **Dominio y correo de contacto.** Comprar el dominio y crear una dirección suya (por ejemplo `hola@<dominio>`) que
   reenvíe al correo personal del autor. Con el dominio en Cloudflare, Email Routing lo hace gratis y sin buzón nuevo.
   La misma dirección sirve para privacidad y para contacto general, y sustituye `[CORREO DE CONTACTO]` en los tres
   documentos. Mientras tanto el contacto es `19lacruz88@gmail.com`. Al tener el dominio: añadirlo al Worker como dominio propio,
   cambiar `site` en `astro.config.mjs` y actualizar las URLs de `legal_documents` con una migración (la app no cambia).

---

## Fase 1 · Página de presentación (M, repositorio `Sobre-IO-WEB`)

**Contenido (español en `/`, inglés en `/en/`):**
- **Inicio:** qué es Sobre-IO en una frase. Cómo funciona: cada euro tiene **dónde está** (una cuenta) y **para qué es** (un
  sobre). Las cuatro pestañas con capturas, los grupos compartidos en tiempo real, las previsiones del periodo y la
  importación del banco. Un bloque de privacidad: "Tus datos no se venden. Sin anuncios. Sin rastreadores". Botones "Abrir
  en el navegador" (`/app/`) y "Descargar para Android" (el APK de la última GitHub Release de `Sobre-IO-ANDROID`).
- **Legal:** `/privacidad`, `/terminos`, `/aviso-legal` y sus versiones en inglés.
- **Novedades:** `/novedades`, generada a partir del `CHANGELOG.md` de la app en el momento del build (opcional en la 1.1).

**Técnica:**
- Astro con salida estática. Los documentos legales son una *content collection* de Markdown con `version` y `fecha` en el
  frontmatter.
- Marca de `sobre_io_marca/`: logotipo SVG, el favicon que cambia con el tema, Lexend para los títulos e Inter con cifras
  tabulares para el texto. **Las fuentes van dentro del sitio**: nada de Google Fonts, que envía la IP del visitante a
  Google.
- Tema claro y oscuro con `prefers-color-scheme`, la misma tinta que la app (`#0c0c0c` / `#f4f4f4`).
- `public/_headers`: `Content-Security-Policy` estricta para la página de presentación (la de `/app/` abre `connect-src`
  al dominio de Supabase y permite `wasm-unsafe-eval`), `X-Content-Type-Options`, `Referrer-Policy` y caché larga para
  `/_astro/*` y `/app/*.wasm`.
- Sin `_redirects`: Workers rechaza reescribir a un `index.html` (lo toma por un bucle). En la 1.1 la web app vive en una
  sola URL, `/app/`, así que no hace falta.
- Despliegue: Workers Builds conectado a GitHub (cada push a `main` compila y publica, sin tokens ni secretos; el
  comando de despliegue es `npx wrangler deploy`). El comando de build es `npm run build:cloudflare`: `scripts/fetch-web-app.sh` descarga el zip
  de la web app de la última release pública de `Sobre-IO-ANDROID` en `public/app/` y después compila. Mientras no exista
  la web app, `/app/` muestra "Muy pronto".

Se da por terminada cuando las tres páginas legales y el inicio están publicados en los dos idiomas, Lighthouse da 100 en
accesibilidad y buenas prácticas, y la página no hace ninguna petición a otro dominio.

---

## Fase 2 · Documentos legales (M, en `Sobre-IO-WEB`)

Datos del responsable, que pone el autor: nombre y apellidos, NIF (para el aviso legal), correo de contacto para
privacidad y localidad. Todo se escribe en español, que es la versión que vale, y en inglés como traducción.

**Política de privacidad:**
- Responsable y contacto.
- **Datos que se tratan:** cuenta (correo, nombre visible, foto si entra con Google), datos de los grupos (cuentas, sobres,
  movimientos, compromisos, etiquetas, notas, extractos importados), quién hizo cada cosa (`created_by`, historial de
  cambios, D-26 y D-41) y datos técnicos de la sesión.
- **Finalidad y base legal:** prestar el servicio (ejecución del contrato, art. 6.1.b) y mantenerlo seguro (interés legítimo).
  Nada más: sin perfiles, sin publicidad y sin decisiones automatizadas.
- **Encargados del tratamiento:** Supabase (base de datos, autenticación y correos, en la región de la UE), Cloudflare (sirve
  la web), Google (solo si se entra con Google) y GitHub (sirve el APK). Transferencias fuera de la UE amparadas por el
  Marco de Privacidad UE-EE. UU. o por cláusulas tipo, según cada proveedor.
- **Grupos compartidos:** lo que apuntas en un grupo lo ven sus miembros. Al salir de un grupo, lo que apuntaste se queda en
  él, porque es contabilidad común (D-17).
- **Conservación:** mientras la cuenta existe. Al eliminarla (D-51): los grupos donde estabas solo se borran al momento, en
  los compartidos tu autoría pasa a "Usuario eliminado" y tu perfil se anonimiza. Lo que quede en Supabase Auth se explica
  según lo que se compruebe en la fase 0.
- **Derechos:** acceso y portabilidad (con "Descargar mis datos"), rectificación, supresión (con "Eliminar mi cuenta"),
  oposición y limitación (por correo), y reclamación ante la AEPD.
- **Compromiso:** "No vendemos, alquilamos ni cedemos tus datos. No hay publicidad ni herramientas de analítica de terceros".
- Versión y fecha de entrada en vigor.

**Términos de uso:** qué es el servicio (gratuito y sin garantía de disponibilidad), que **no es asesoramiento financiero**
y que las cifras dependen de lo que apunta cada usuario; edad mínima de 14 años; responsabilidad sobre lo que se apunta en
grupos compartidos y uso aceptable; baja en cualquier momento; cambios de los términos, avisados en la app con una
nueva aceptación; limitación de responsabilidad dentro de lo que permite la ley de consumidores; ley española.

**Aviso legal:** titular, NIF, contacto y propiedad del contenido de la web.

Cada documento lleva `version: AAAA-MM-DD`. La versión vigente de privacidad y términos es la que la app pide aceptar
(fase 3).

---

## Fase 3 · Aceptar los términos (S en BD + M en app)

**3.1 Base de datos (D-57):**
- Tabla `legal_documents(document, version, published_at, url_es, url_en)`, con `document` en (`terms`, `privacy`) y una
  fila por versión publicada, que se añade con una migración al publicar una nueva. Es la única fuente de los enlaces que
  muestra la app.
- Tabla `legal_acceptances(user_id, document, version, accepted_at)`. RLS: cada uno ve e inserta solo las suyas; nadie las
  edita ni las borra. Al anonimizar el perfil (D-18) se conservan: son la prueba del consentimiento y no identifican a
  nadie sin el perfil.
- `legal_status()`: por documento, la versión vigente con sus dos URLs, la que aceptó el usuario y si le falta aceptar algo.
- `accept_legal(p_terms_version, p_privacy_version)`: registra las dos. Da un error FZ nuevo si alguna versión no es la
  vigente, para que nadie acepte un texto antiguo. Entra en `READ_ONLY_RPCS` solo `legal_status`.

**3.2 App:**
- **Alta con correo:** bajo los campos, una línea y dos enlaces (términos y privacidad, de `legal_documents_in_force`)
  para informar antes de pedir los datos (RGPD art. 13). Sin casilla: la aceptación es la puerta de después, la misma
  para todos (decidido al implementarla: un solo mecanismo y ninguna versión que arrastrar por la confirmación del código).
- **Puerta de aceptación** (`LegalGate`, en la raíz, junto a `SessionViewModel`): con sesión y `legal_status` pendiente,
  se muestra antes de `AppNavigation` una pantalla con los dos enlaces, la casilla y "Continuar". Cubre tres casos con un
  solo mecanismo: el alta con Google (no tiene formulario), los usuarios de la 1.0 al actualizar y cualquier versión nueva
  de los documentos. Si se rechaza, ofrece "Cerrar sesión" o "Eliminar mi cuenta".
- `docs/features/legal.md` nuevo, en la lista de "Current state".

---

## Fase 4 · Descargar mis datos (M en BD + S en app)

**4.1 Base de datos (D-58).** `export_my_data()`, con SECURITY INVOKER (la RLS decide qué se ve), devuelve un único
`jsonb`:
- `profile`: los datos del usuario y sus aceptaciones legales;
- `groups[]`: cada grupo donde es o fue miembro, con lo que su RLS le deja leer: miembros (nombre visible y papel), cuentas,
  sobres, periodos con `envelope_periods`, movimientos del libro (`search_movements` sin filtros), compromisos con sus
  versiones y vencimientos, etiquetas, importaciones del banco y el historial de cambios de lo que firmó;
- `exported_at` y `format_version`.

Los importes van en céntimos (enteros) y además en texto decimal, para quien lo abra sin saber del formato. Lleva pgTAP:
un usuario no ve nunca datos de un grupo ajeno y un grupo grande cabe en una sola llamada.

**4.2 App.** En Perfil, "Descargar mis datos" genera `sobre-io-datos-AAAA-MM-DD.json` y lo entrega con el mismo mecanismo
de `export_period`: la hoja de compartir en Android e iOS, una descarga en la web. Un `InlineNotice` explica qué incluye.
Con esto se cumple el art. 20 (JSON es estructurado, de uso común y legible por máquina); un ZIP con CSV queda para más
adelante si alguien lo pide.

---

## Fase 5 · Perfil: sección Legal (S)

En Perfil, una sección con "Política de privacidad", "Términos de uso" (con la versión aceptada y su fecha, de
`legal_status`), "Descargar mis datos" (fase 4) y "Eliminar mi cuenta", que ya existe y se mueve aquí. Los enlaces salen
de `legal_status`; el correo de contacto está en la política de privacidad, así que la app no lo guarda en ningún sitio.

---

## Fase 6 · La app en el navegador (L, `Sobre-IO-ANDROID`)

Comprobado el 2026-11-25: todas las librerías del catálogo publican la variante `wasm-js` (Compose 1.12, material3,
adaptive, iconos, Navigation 3, supabase-kt con realtime, Koin, multiplatform-settings, kotlinx-datetime y el motor JS de
Ktor). El trabajo es el target y las piezas propias de cada plataforma:

1. **Target `wasmJs { browser() }`** en `:shared`, que es también el ejecutable web: su carcasa son `Main.kt`
   (`ComposeViewport`) e `index.html` en `wasmJsMain`. Al hacerlo se vio que un módulo `:webApp` aparte no aportaba nada:
   la carcasa son dos ficheros. Sirve en `/app/` con rutas relativas.
2. **Actuals:** `supabaseUrl` (desde `AppConfig`), motor `ktor-client-js`, ajustes con `localStorage` (multiplatform-settings
   ya lo hace) y el idioma del navegador.
3. **Google:** en la web no hay Credential Manager. Se usa el flujo OAuth con redirección de Supabase
   (`signInWith(Google)` con `redirectUrl = <web>/app/`). Hay que añadir esa URL en Supabase Auth y el origen en el cliente
   web de Google. El alta y la recuperación con correo ya usan códigos y no enlaces (D-43), así que no cambian.
4. **Ficheros:** importar del banco con un `<input type="file">` y exportar (periodo y "mis datos") con un `Blob` y un
   enlace de descarga.
5. **Navegación:** la 1.1 vive en una sola URL (`/app/`). Volver atrás es el botón de la app. Sincronizar la pila con el
   historial del navegador queda para después.
6. **Navegadores:** Compose web necesita WasmGC (Chrome y Edge 119+, Firefox 120+, Safari 18.2+). El `index.html` lo
   comprueba y, si falta, muestra un mensaje con el enlace al APK.
7. **CI:** al crear el tag `vX.Y.Z`, un job compila `wasmJsBrowserDistribution` y sube `sobre-io-web-X.Y.Z.zip` a la
   GitHub Release. El despliegue de `Sobre-IO-WEB` la recoge (fase 1).
8. **Verificar** en Chrome, Firefox y Safari, en escritorio ancho (lista y detalle) y estrecho, con dos sesiones a la vez
   para el tiempo real.

Si esta fase se alarga, sale en la 1.2 sin frenar lo legal: las fases 1 a 5 no dependen de ella.

---

## Fase 7 · Release 1.1 (S)

`CHANGELOG.md` ("Novedades: términos y privacidad, descargar mis datos, la app en el navegador"; **Base de datos:**
hasta D-58), `gradle.properties` a 1.1.0, tag `v1.1.0` con el APK y el zip de la web, despliegue de `Sobre-IO-WEB`,
`README` y `AGENTS.md` de cada repositorio al día.

---

## Orden y paralelismo

```
Release 1.0 (entrega del curso)
  └─> Fase 0 (preparación)
        ├─> Fase 1 (página de presentación) ──> Fase 2 (documentos) ──┐
        ├─> D-57 (BD) ──> Fase 3 (aceptación en la app) ──────────────┤
        ├─> D-58 (BD) ──> Fase 4 (descargar mis datos) ───────────────┼─> Fase 5 (Perfil) ──> Fase 7 (release)
        └─> Fase 6 (web app) ── independiente; si se retrasa, va en la 1.2
```

La fase 3 necesita las URLs de la fase 2 publicadas antes de salir, pero se puede programar en paralelo: las URLs llegan
con la migración de `legal_documents`.

## Pendiente del autor

1. Dominio y correo propio (fase 0, punto 5). Mientras tanto, el contacto es `19lacruz88@gmail.com`.

## Estado

- Fase 0: hecha, salvo el dominio y el correo propio (punto 5).
- Fase 1: `Sobre-IO-WEB` en GitHub, publicado por Cloudflare (Worker `sobre-io-web`) con la portada en los dos idiomas.
- Fase 2: borradores en español e inglés con titular, NIF, localidad, correo de contacto y la región de Supabase. Dejan
  de ser borrador cuando D-57 esté en el proyecto alojado y se publiquen sus filas de `legal_documents`.
- Fases 3.1 y 4.1 (D-57 y D-58, `Sobre-IO-DB` #18 y #19): fusionadas el 2026-11-25 y aplicadas al proyecto alojado
  el 2026-11-26, junto con D-56, que faltaba allí aunque la 1.0 la esperaba; Edge Function `delete-account` desplegada.
  El proyecto alojado no guarda el registro de Auth en la base de datos (`Sobre-IO-DB` #20).
- Fases 3.2, 4.2 y 5 (app): fusionadas en `Sobre-IO-ANDROID` #57 el 2026-11-26, probadas en el emulador contra la base local.
- Fase 6 (app en el navegador): hecha salvo Google. Compila y funciona contra la base local (alta, aceptación, grupo,
  perfil, descarga); la CI compila el target web y `web-release.yml` adjunta el zip a cada Release. Pendiente: Google con
  la redirección de Supabase (punto 3) y las variables `SUPABASE_URL` y `SUPABASE_PUBLISHABLE_KEY` del repositorio.
  Encontrado al probarla: en el navegador `kotlinx-datetime` solo conoce UTC y «SYSTEM»; se añadió `@js-joda/timezone` y
  `deviceTimeZoneId()`.
- Fase 7 (release 1.1): `v1.1.0` el 2026-11-26, con la APK y el zip de la web adjuntos; `Sobre-IO-DB` lleva el mismo
  tag. Queda para después Google en la web (fase 6, punto 3).
