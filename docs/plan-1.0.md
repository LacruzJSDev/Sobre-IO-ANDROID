# Plan hasta la release 1.0

Último plan antes de la 1.0, con fecha 2026-11-18. Todos los requisitos (RF1–RF12) ya funcionan. Lo que queda es **pulir**:
la carga de datos, el tiempo real, las tablets, aprovechar la base de datos, añadir vistas ampliadas e informes, cuidar el
código y preparar la release.

Cada bloque es un PR propio (una rama y un worktree por PR, según `AGENTS.md`). Los que tocan la base de datos llevan
antes su PR en `Sobre-IO-DB`. Tamaños orientativos: S < 2 h, M 2–5 h, L > 5 h.

---

## Diagnóstico (lo que hay hoy)

- **La carga empieza de cero en cada pantalla.** Cada pantalla tiene su ViewModel asociado a la entrada de navegación, y los
  repositorios no guardan nada: piden los datos a Supabase y los devuelven sin quedarse con ellos. Al entrar en un detalle se
  crea un ViewModel nuevo que vuelve a pedirlo todo. Hay 51 `CircularProgressIndicator` repartidos por las pantallas, y en
  Grupo, detalle de cuenta, detalle de sobre, compromisos, transferencias y formularios la pantalla se queda en blanco con el
  spinner. Las pantallas recargan cada vez que se muestran (`LaunchedEffect(Unit) { Refresh }`). Algunas ya conservan los datos
  después de la primera carga (`showSpinner = !hasLoaded`), pero solo dentro del mismo ViewModel.
- **No hay tiempo real.** Ni `realtime-kt` en la app ni publicación de Realtime en la base de datos. Lo que cambia otro miembro
  solo aparece al volver a entrar en la pantalla.
- **Las dos columnas se desplazan a la vez.** Solo Grupo, el resumen de cuenta y el detalle de sobre usan dos columnas, y
  las dos van dentro de un único `verticalScroll`. Las demás pantallas anchas son una sola columna centrada.
- **Funciones de la base de datos sin usar en la app.** `export_period`, `account_balance_at`, `envelope_balance_at`,
  `envelope_outlook(fecha)`, `group_outlook(fecha)`, la vista `account_outlook`, `match_pending` y la tabla `audit_log`
  (D-41). Las demás que no aparecen en la app son internas y deben seguir así: `run_daily`, `purge_group`,
  `anonymize_profile`, `materialize_period`, `system_envelope_id`, `external_envelope_id`, `commitment_due_dates`,
  `commitment_funding_amount` y `delete_my_account` (esta va por la Edge Function).
- **Hueco funcional:** las dos mitades de una aportación a otro grupo no se pueden anular desde Movimientos.
- **Ficheros grandes:** `BankImportScreen` (619 líneas), `GroupSummaryPane` (574), `EnvelopeDetailScreen` (563),
  `MovementsPane` (557), `GroupSettingsScreen` (522), `CommitmentFormScreen` (511) y `AccountSummaryTab` (494).

---

## Fase 0 · Arreglo rápido (S, en paralelo con todo lo demás)

**0.1 Anular aportaciones desde Movimientos.** En las dos mitades de una aportación, deslizar a la izquierda muestra
Eliminar. La fila (y su pareja, si se ve) se oculta al momento, sale un snackbar con Deshacer y, cuando termina sin Deshacer,
se llama a `void_group_transfer`. Solo pueden owner y admin de los dos grupos, o quien la creó; la base de datos decide y el
FZ vuelve a mostrar la fila. Hay que actualizar AGENTS.md, donde ahora pone "not offered yet".

---

## Fase 1 · Cómo se cargan y se actualizan los datos (L, es la base de las fases 2 a 4)

**1.1 Una capa de stores por grupo** (`data/store`).
- Un `GroupDataStore` por grupo abierto (Koin, uno por `groupId`, que vive mientras el grupo está en la pila). Guarda en
  memoria el último resultado de cada lectura: grupo, cuentas, sobres, resumen, progreso, miembros, etiquetas,
  `group_today`/periodo, la primera página de movimientos y los detalles abiertos. Nada en disco: el modo offline queda fuera
  del alcance.
- Cada lectura se expone como `StateFlow<Loadable<T>>`, con `data: T?`, `isRefreshing`, `error` y `fetchedAt`.
- **Muestra lo guardado y refresca detrás:** el ViewModel observa el flujo. Si hay datos se pintan enseguida y se refresca en
  segundo plano. Solo se llama a la red si los datos están marcados como desfasados o tienen más de N segundos (por ejemplo
  30 s), no cada vez que se muestra la pantalla.
- **Invalidación en un solo sitio:** cada escritura (movimiento, asignación, cuenta, sobre, compromiso, importación…) avisa a
  un `DataChanges` con lo que cambió, y una tabla dice qué lecturas dependen de ello. Por ejemplo, un gasto invalida cuentas,
  sobres, resumen, progreso, movimientos y el detalle de esa cuenta y ese sobre. Las pantallas dejan de recargar a ciegas.
- Los repositorios siguen siendo la fuente remota, sin estado. Los stores se apoyan en ellos y los ViewModels dependen de
  interfaces de store (con fakes en los tests).

**1.2 Un mismo contrato de carga en la interfaz.**
- **Primera carga sin datos:** un esqueleto con formas grises suaves que imitan la tarjeta hero y las filas, nunca un
  spinner centrado.
- **Refresco con datos:** una barra fina (`LinearProgressIndicator` de 2 dp, indeterminada) justo bajo el header
  (`ScreenHeader`, el shell del grupo y `FormScaffold` cuando carga). La pantalla sigue siendo interactiva.
- **Error al refrescar:** se mantienen los datos y sale un snackbar con "Reintentar". El error a pantalla completa queda solo
  para cuando no hay nada que enseñar.
- Pull to refresh en listas y detalles (`PullToRefreshBox` de Material 3) para forzarlo a mano.
- Un componente `LoadableContent(loadable, skeleton, content)` en `ui/components` y quitar los 51 spinners. Se mantienen solo
  dentro de los botones (`isLoading`) y en "cargar más".

**1.3 Los formularios abren con lo que ya hay.** Editar una cuenta, un sobre o un movimiento abre con los datos del store
(sin spinner) y lee la versión fresca detrás.

Para dar la fase por terminada: ir y volver entre pestañas y detalles no enseña nunca una pantalla en blanco, la barra
aparece como mucho un instante y no se repiten peticiones idénticas (comprobarlo con el log de red).

---

## Fase 2 · Tiempo real entre miembros (M en la base de datos + M en la app, después de la 1.1)

**2.1 Base de datos (PR en `Sobre-IO-DB`, decisión D-55).** Recomiendo **Broadcast desde la base de datos** en lugar de
`postgres_changes`:
- Un trigger `after insert or update` en las tablas del grupo (`transactions`, `account_transfers`, `envelope_transfers`,
  `accounts`, `envelopes`, `commitments`, `commitment_versions`, `commitment_occurrences`, `tags`, `transaction_tags`,
  `account_group_members`, `invitations`, `groups`, `bank_imports`) que llama a `realtime.send` con el tema `group:<id>` y un
  cuerpo mínimo `{table, op}`, sin datos.
- Canales privados: una política sobre `realtime.messages` que deja escuchar `group:<id>` solo a sus miembros
  (`is_member`). Así se cumple RNF1.
- Por qué no `postgres_changes`: evalúa RLS fila a fila para cada suscriptor, necesita muchas tablas en la publicación y manda
  las filas enteras. Broadcast es un mensaje pequeño por cambio y cabe de sobra en el plan gratuito (RNF6).
- Las escrituras en lote (importación, materializar) mandan un único aviso por sentencia (trigger `for each statement`, o se
  agrupan con `pg_notify` por transacción).

**2.2 App.**
- Añadir `realtime-kt`, de la misma versión de `supabase-kt`, al catálogo y a "Stack".
- `GroupRealtime`: se suscribe al abrir un grupo y se da de baja al salir o al pasar la app a segundo plano.
- Junta los avisos durante unos 500 ms y los pasa a `DataChanges`, que invalida igual que una escritura propia. Lo que esté
  en pantalla se refresca con la barra fina.
- Los propios cambios también llegan por el canal, pero no pasa nada: la invalidación es idempotente.
- Al reconectar o volver al primer plano se invalida todo el grupo, por si se perdió algún mensaje.

Para dar la fase por terminada: con dos emuladores en el mismo grupo, un gasto en uno aparece en el otro en menos de 2 s sin
tocar nada.

---

## Fase 3 · Tablet y plegable: dos columnas bien hechas (M, después de la 1.2)

**3.1 Un componente `TwoColumnLayout(left, right)`** en `ui/components`. Cuando el panel mide 720 dp o más, cada columna
tiene **su propio scroll** (`LazyColumn` o `verticalScroll` propio); por debajo, una sola columna con el orden
`left + right`. Las columnas se reparten por peso configurable (por ejemplo 5/7) y no por la mitad fija.

**3.2 Una regla de reparto:** la izquierda es **lo que resume** (hero, cifras, gráfica compacta) y la derecha **lo que se
recorre** (listas e historial). Si el contenido de la izquierda es corto, se queda fijo arriba mientras la derecha se
desplaza.

**3.3 Revisión pantalla por pantalla:**

| Pantalla | Izquierda | Derecha |
| --- | --- | --- |
| Grupo | Hero, revisión y avisos | A dónde fue el dinero, entradas y salidas, próximos vencimientos |
| Cuentas | Hero y gráfica | Lista de cuentas |
| Sobres | Hero de "libre para repartir" | Secciones de sobres |
| Movimientos | Resumen del filtro (fase 5.3) y filtros | Lista |
| Detalle de cuenta | Saldo, gráfica y previsión | Últimos movimientos, vencimientos, banco; Compromisos en su pestaña |
| Detalle de sobre | Disponible, este periodo, realmente libre | Gráfica, compromisos, movimientos entre sobres, movimientos |
| Detalle de compromiso | Importe y reserva | Historial y cambios |
| Ajustes del grupo | Detalles y etiquetas | Miembros e invitaciones |
| Importar del banco (revisión) | Resumen y botón Importar | Filas |
| Perfil | Datos y accesibilidad | Borrar cuenta |

**3.4 Verificar** en el emulador de móvil, Pixel Tablet (en vertical y en horizontal) y Pixel Fold (abierto y cerrado), con
capturas antes y después de cada pantalla.

---

## Fase 4 · Sacar partido a la base de datos sin borrar nada (M)

| Función | Dónde aparece |
| --- | --- |
| `export_period` | "Exportar periodo" en Movimientos (menú del header) y en Ajustes del grupo: elegir periodo y generar un CSV en el idioma del usuario, que se comparte con la hoja del sistema (expect/actual: `CreateDocument`/`ACTION_SEND` en Android, `UIActivityViewController` en iOS). |
| `account_balance_at` / `envelope_balance_at` | En la gráfica a pantalla completa (fase 5.1): al elegir un día, "Saldo el <día>", y un selector "¿Cuánto había el…?" en el detalle de cuenta y de sobre. |
| `envelope_outlook(fecha)` / `group_outlook(fecha)` | "Mirar hasta…" en la vista ampliada del hero de Grupo y en el detalle de sobre: las tres cifras (comprometido, libre, descubierto) hasta una fecha elegida, por ejemplo la próxima nómina o más allá del fin de periodo. |
| Vista `account_outlook` | Una segunda línea en cada fila de Cuentas: "A fin de periodo: X" (en rojo si se queda corta), sin tener que entrar al detalle. |
| `match_pending` | Formulario de movimiento: al crear un gasto o ingreso que se parece a un vencimiento pendiente (misma cuenta, importe cercano), propone "¿Es el pago de <compromiso> del <día>?". Si se acepta, se confirma el vencimiento en lugar de crear un duplicado. |
| `audit_log` (D-41) | "Historial" en un movimiento editado (quién lo creó, quién lo cambió y cuándo), útil en grupos compartidos. Requiere comprobar o añadir en la base de datos la política de lectura para los miembros del grupo. |

Las funciones internas que lista el diagnóstico se documentan como internas en AGENTS.md para que nadie intente
"aprovecharlas".

---

## Fase 5 · Detalles más ligeros que se pueden explorar (L, después de la fase 1)

**5.1 Gráficas a pantalla completa con zoom.** Al tocar cualquier gráfica compacta (hero de Grupo, Cuentas, saldo de cuenta,
columnas de sobre, a dónde fue el dinero) se abre con una **transición de elemento compartido** (`SharedTransitionLayout`
y `sharedBounds`, de Compose MP 1.12) a una ruta `ChartDetail` a pantalla completa, sin metadatos de panel. Esa vista tiene
rango (1M/3M/6M/1A/Todo), cruceta con valor y fecha, leyenda, la tabla de valores, "Saldo el día" (fase 4) y "Mirar
hasta…". Atrás deshace la animación.

**5.2 Detalles con menos scroll.** Los detalles de grupo, cuenta y sobre enseñan lo esencial (cifra, una gráfica, avisos,
3 movimientos) y **tarjetas tocables** que abren la vista ampliada: gráfica, historial de periodos, movimientos entre sobres
y compromisos. Se apoya en el store (fase 1) para que abrir y cerrar sea instantáneo.

**5.3 Informes con cualquier filtro, también por etiqueta.**
- Base de datos (PR en `Sobre-IO-DB`): `search_movements_summary`, con los **mismos parámetros** que `search_movements`
  (cuenta, sobre, tipos, fechas, etiquetas, autor, texto). Devuelve entradas, salidas, neto y número de movimientos, además
  del desglose por periodo y por sobre. La app no suma nunca (regla de AGENTS.md), así que esta función es imprescindible.
- App: en Movimientos, con cualquier filtro activo, una tarjeta `StatCard` arriba (entradas, salidas, neto) y un botón
  "Ver informe": una pantalla con columnas por periodo, reparto por sobre y exportación.
- Atajos: una fila de Etiquetas abre su informe ("Vacaciones: 1.240 € en 3 meses") y el detalle de cuenta ofrece "Informe
  por etiqueta".

**5.4 Textos que explican.**
- Revisar que **todos** los iconos de acción tengan nombre accesible con tooltip al mantener pulsado
  (`IconActionButton`/`LabelTooltip`), incluidos los del header, las pestañas, los chips de filtro y los avatares.
- Un icono "ⓘ" junto a las cifras clave (realmente libre, libre para repartir, reservado, comprometido, fuera de los
  sobres) que abre una hoja breve con qué es y de dónde sale. Los textos van en los dos idiomas.
- Estados vacíos con una frase que explique para qué sirve cada sección y qué hacer primero, por ejemplo "Crea un sobre de
  Recibos para reservar el seguro anual".

---

## Fase 6 · Cuidar el código (M, repartido: cada fase deja su parte limpia)

- **Ficheros por debajo de unas 350 líneas:** partir las pantallas grandes en ficheros por sección (`GroupSummaryHero.kt`,
  `GroupSummaryWarnings.kt`…), con la misma regla en todas.
- **Repositorios por dominio, no por pantalla:** sacar `group_today`/`current_period`/`periods` a un `PeriodRepository`
  (ahora están dentro de `AccountDetailRepository` y otros los piden prestados), y revisar que `GroupSummaryRepository`,
  `AccountDetailRepository` y `EnvelopeDetailRepository` no dupliquen lecturas.
- **ViewModels más finos:** la carga, el error y el refresco los da `Loadable` (fase 1), y el ViewModel solo combina flujos y
  atiende eventos.
- **Tests:** stores (caché, invalidación, refresco), `DataChanges` (qué invalida cada escritura), realtime con un fake del
  canal y los informes. Mantener `StringsParityTest`.
- **AGENTS.md:** la sección "Current state" ya es enorme. Moverla a `docs/agents/features/<feature>.md`, una por pantalla, y
  dejar en AGENTS.md el índice y las reglas, entre ellas las nuevas: stores, carga, dos columnas y tiempo real.

---

## Fase 7 · Release 1.0 (M)

Decidido el 2026-11-25: la versión es la **1.0.0** (no la 0.1 del calendario anterior), las novedades se documentan en
`CHANGELOG.md` en español y ese mismo texto es el de la GitHub Release; la app no tiene pantalla de novedades. Cómo se
numeran las versiones y cómo se publica una está en la sección «Versiones» del README.

Cada punto es un PR, en este orden:

1. **Versiones y CHANGELOG** (`docs: versioning and changelog`): la versión en un único sitio (`sobreio.version` en
   `gradle.properties`, de donde salen `versionName` y `versionCode`) con la comprobación de iOS en la CI
   (`checkIosVersion`), `CHANGELOG.md` con la 1.0.0 en «Sin publicar», la plantilla de PR, la comprobación de la CI de que
   un PR `feat` o `fix` toca el CHANGELOG y `tools/release-notes.sh`.
2. **Versión en Perfil** (`feat(ui)`): «Versión 1.0.0» al pie de Perfil, para saber qué versión tenía quien encuentra un
   fallo.
3. **Firma de release** (`build(app)`): `signingConfig` que lee el keystore desde `local.properties` (el keystore queda
   fuera del repositorio; sin él, el build de release sale sin firmar), reglas de R8 revisadas y la APK de release
   probada en un dispositivo real.
4. **Matriz de pruebas manuales** (`docs/pruebas-1.0.md`): móvil, tablet en vertical y en horizontal, plegable abierto y
   cerrado y simulador de iOS; claro, oscuro y daltonismo; español e inglés. Flujos: alta, crear grupo, invitar, gasto,
   asignar, compromiso, revisar, importar CSV, aportación entre grupos y borrar cuenta. Cada fallo, un PR `fix` con la
   etiqueta `no-changelog` (nunca se publicó).
5. **Informe de pruebas** (entregable del curso, en `curso/`): tests unitarios, tests de pgTAP de la base de datos y la
   matriz manual.
6. **Release** (`build(app): release v1.0.0`): versión 1.0.0 (`versionCode` 10000) en Android e iOS, fecha en el
   CHANGELOG, «Estado» del README al día y esta fase marcada como hecha. Después, el tag `v1.0.0`, la APK firmada en la
   GitHub Release, el tag `v1.0.0` en `Sobre-IO-DB` y la compilación Release de iOS desde Xcode.

Lo hace el autor, fuera del repositorio, antes del punto 4: comprobar que el Supabase alojado tiene todas las
migraciones hasta D-56, la Edge Function de borrar cuenta, los correos en español y Realtime; crear el keystore de
release y el cliente OAuth de Android con su huella SHA-1 para que Google funcione en la APK firmada.

---

## Orden y paralelismo

```
Fase 0 ─────────────────────────────────────────── (cuando sea, es pequeña)
Fase 1 (stores + carga) ──┬─> Fase 2 (tiempo real; el PR de BD puede empezar ya)
                          ├─> Fase 3 (dos columnas)
                          └─> Fase 5 (zoom, detalles, informes; el PR de BD de 5.3 puede empezar ya)
Fase 4 (funciones de BD) ── independiente, pero 5.1 usa *_balance_at y *_outlook
Fase 6 ── dentro de cada PR, más un PR final de reorganización y documentación
Fase 7 ── al final
```

Los PRs de base de datos (D-55 tiempo real, `search_movements_summary`, la lectura de `audit_log`) son independientes y pueden
ir en paralelo en `Sobre-IO-DB` desde el principio.

## Decisiones (2026-11-18)

1. **Barra de carga:** tinta de marca (negro en claro, blanco roto en oscuro), dibujada sobre el borde inferior del header
   para que aparecer o desaparecer no mueva el contenido, y oculta en cuanto termina de cargar.
2. **Tiempo real:** Broadcast con trigger (D-54).
3. **Exportación:** solo CSV.
4. **Caducidad de la caché:** 30 s, además de invalidar con cada escritura y, más adelante, con el tiempo real.

## Estado

- Fase 0 (anular aportaciones): hecha (#26).
- Fase 1 (caché, carga en dos pasadas, esqueletos y barra fina): hecha (#27).
- Fase 3 (dos columnas con scroll independiente): Grupo, Cuentas, Sobres, detalles de cuenta, sobre y compromiso, y ajustes del grupo. Movimientos sigue en una columna hasta la 5.3 (resumen del filtro); la revisión de la importación del banco y el perfil se quedan en una columna.
- Fase 4 (funciones de la base de datos): fin de periodo en cada cuenta, saldo en una fecha, "Mirar hasta…" en Grupo y sobre, exportar un periodo a CSV, sugerir el vencimiento pendiente al apuntar un movimiento e historial de cambios de un movimiento.
- Fase 5.1 (gráficas a pantalla completa con animación de zoom y la tabla de valores): hecha.
- Columnas izquierdas de Cuentas y Sobres llenas y € junto al importe (#32).
- Fase 5.4 (textos de ayuda: etiqueta al mantener pulsado en todos los iconos e ⓘ en las cifras clave): hecha.
- Fase 5.3 (informes con cualquier filtro o etiqueta: totales, informe por periodo y por sobre, movimientos de una etiqueta; D-54 en la base de datos): hecha. El tiempo real (Fase 2) usará D-55.
- Fase 5.2 (detalles más ligeros: rango y saldo de un día en la gráfica ampliada, «Libre de verdad» ampliable, 3 filas en las listas): hecha.
- Fase 2 (tiempo real, D-55 en la base de datos): hecha; lo de otro miembro aparece en unos 3 s.
- Fase 6 (orden del código, partes A a G, ver `fase-6-alcance.md`): hecha (#37–#43).
- Fase 7 (release 1.0): en curso; hechos el punto 1 (versiones y CHANGELOG, #50), el 2 (versión en Perfil, #51) y el 3
  (firma de release, la APK de release probada en el emulador).
