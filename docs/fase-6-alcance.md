# Fase 6 · Alcance del orden del código

Auditoría del 2026-11-19 sobre `main` (después del PR #36). El objetivo de la fase es que el código se entienda y se
mantenga mejor **sin cambiar lo que hace la app**: cada parte es una refactorización pura, va en su propio PR, mantiene
todos los tests en verde y no cambia ningún texto ni comportamiento salvo que se diga.

## Lo que se ha medido

| Medida | Valor |
| --- | --- |
| Código de la app (`commonMain`) | 35.918 líneas; `ui/` 27.817, `data/` 5.107, `domain/` 1.717, `navigation/` 914 |
| Tests (`commonTest`) | 11.225 líneas; 29 de 30 ViewModels tienen test (falta `MovementHistoryViewModel`) |
| Ficheros de más de 400 líneas | 12; los mayores, `GroupSummaryPane` (651), `EnvelopeDetailScreen` (645), `BankImportScreen` (622), `MovementsPane` (614) |
| ViewModels con más dependencias | `MovementFormViewModel` (10 repositorios o almacenes, 523 líneas), `EnvelopeDetailViewModel` (7), `AccountDetailViewModel` (6) |
| Reglas de capas | Ninguna pantalla llama a un repositorio; ningún ViewModel importa Compose UI; nada de `java.*`/`android.*` en común; un solo `!!` (`AdaptiveListDetail`) |
| `AGENTS.md` | 799 líneas; «Current state» ocupa unas 600 y tiene datos ya desfasados (por ejemplo, «5 últimos movimientos», que ahora son 3) |

## Problemas encontrados (con su evidencia)

1. **El calendario vive en el repositorio del detalle de cuenta.** `AccountDetailRepository.getToday/getCurrentPeriod` lo usan
   12 ViewModels que no tienen nada que ver con el detalle de cuenta (Cuentas, Sobres, formularios, revisión…). Además
   `MovementRepository.getDateContext` y `ExportRepository.getPeriods` vuelven a leer `group_today`, `current_period` y
   `periods` por su cuenta. Es la mayor confusión de responsabilidades del proyecto.
2. **Componentes compartidos dentro de una pantalla.** `SoftTile` está en `ui/accountdetail` y lo importan 10 ficheros de
   otras pantallas; `validateRequired` y compañía están en `ui/auth` y los usan 8 ficheros fuera de la autenticación.
3. **La misma lógica copiada.**
   - `validateName` está repetida, idéntica salvo el límite, en 4 ViewModels.
   - `SectionTitle` está repetida en 3 pantallas.
   - Traducir el nombre de «Sin asignar» (que la base de datos guarda en español) se resuelve a mano en 15 sitios, cada
     uno a su manera.
4. **Ficheros de pantalla demasiado largos.** Hay 8 pantallas de más de 490 líneas que mezclan varias secciones
   independientes: cuestan de leer y de revisar.
5. **ViewModels que hacen demasiado.**
   - `MovementFormViewModel` junta tipo, cuentas, sobre y cobertura, grupo destino, etiquetas y la sugerencia de
     vencimiento.
   - `BankImportViewModel` y `CommitmentFormViewModel` mezclan la validación y la construcción de borradores con el
     estado de la pantalla.
6. **La ruta del grupo orquesta demasiado.** `GroupRoute` (en `GroupScreen.kt`, 498 líneas) crea 6 ViewModels y cablea a
   mano las cuatro pestañas.
7. **La documentación no escala.** `AGENTS.md` describe cada pantalla en un párrafo largo dentro de un único fichero, y ya
   hay afirmaciones desfasadas.

## Lo que NO entra (y por qué)

- **Cambiar comportamiento, textos o diseño:** esta fase solo ordena.
- **Una capa de «casos de uso» para todo:** solo se extrae lógica pura donde hoy hay duplicados o ViewModels demasiado
  grandes; crear una capa entera sería cambio por cambiar.
- **Los repositorios `Supabase*`:** tienen un tamaño razonable (48–274 líneas) y una responsabilidad clara, salvo el
  calendario (punto 1).
- **`RefreshOnRemoteChange` lee `GroupChangeFeed` desde la UI:** es una interfaz, se prueba con un fake y evita meter la
  misma dependencia en 10 ViewModels. Se documenta como excepción deliberada.

## Partes, en este orden

Cada parte es un PR. Antes de empezar la siguiente, la anterior está fusionada, con la CI en verde y con un repaso en el
emulador.

| Parte | Qué | Riesgo | Cómo se comprueba |
| --- | --- | --- | --- |
| **A** | Piezas de UI compartidas: `SoftTile` y un `SectionTitle` en `ui/components`; validaciones en `ui/validation` (`validateName(name, max)` una vez); `envelopeDisplayName` para «Sin asignar»; quitar el `!!` | Bajo: mueve código y sustituye duplicados idénticos | Tests, build, lint; capturas de 3 pantallas |
| **B** | `CalendarRepository` (hoy, periodo actual, contexto de fechas, periodos) con su fake y su caché; los 12 ViewModels pasan a usarlo; `AccountDetailRepository`, `MovementRepository` y `ExportRepository` dejan de leer el calendario | Medio: cambia firmas de constructores | Los tests de los 12 ViewModels guían; build e iOS |
| **C** | Partir las 8 pantallas largas en ficheros por sección (sin cambiar nada de lo que se dibuja), con un objetivo de menos de 400 líneas por fichero | Bajo: mueve composables | Tests, build; capturas antes y después |
| **D** | Sacar la lógica pura de los ViewModels grandes a funciones probadas: validación y borradores de los formularios de movimiento, compromiso e importación; la selección de etiquetas del formulario de movimiento a una clase pequeña con su test | Medio | Tests nuevos de las funciones extraídas, más los existentes |
| **E** | `GroupRoute`: cada pestaña con su propio composable de ruta (`AccountsTabRoute`…) que crea y cablea su ViewModel | Medio: toca la navegación del shell | Tests de navegación; repaso de las 4 pestañas en el emulador |
| **F** | Tests que faltan: `MovementHistoryViewModel` y lo que las partes anteriores dejen sin cubrir | Bajo | — |
| **G** | Documentación: «Current state» de `AGENTS.md` pasa a `docs/features/<pantalla>.md` (una por pantalla, puestas al día); `AGENTS.md` queda como índice, reglas y arquitectura | Bajo | Revisar que cada afirmación coincide con el código |
