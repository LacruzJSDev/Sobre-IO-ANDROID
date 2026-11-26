# Novedades de Sobre-IO

Qué cambia en cada versión de la app, escrito para quien la usa. Las versiones siguen
[SemVer](https://semver.org/lang/es/) (`MAYOR.MENOR.PARCHE`): la MENOR sube cuando hay funciones nuevas y el PARCHE
cuando solo hay arreglos. El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

Cómo se mantiene (las reglas completas están en la sección «Versiones» del `README.md`):

- Cada PR `feat` o `fix` que se nota al usar la app añade una línea en «Sin publicar», en el apartado que toca:
  **Novedades** (algo que antes no se podía hacer), **Mejoras** (algo que ya existía y ahora funciona o se ve mejor) o
  **Arreglos** (un fallo de una versión ya publicada). Una frase por cambio, desde el punto de vista del usuario, con el
  número del PR al final.
- Un fallo de algo que todavía no se ha publicado no es un arreglo: no se anota.
- Si la versión necesita una versión concreta de la base de datos, se dice en una línea **Base de datos**.
- Al publicar, «Sin publicar» pasa a ser `## [X.Y.Z] · AAAA-MM-DD` y ese texto es el de la GitHub Release
  (`tools/release-notes.sh X.Y.Z`).

## Sin publicar

## [1.0.0] · 2026-11-26

Primera versión completa: todo lo que pedía el plan 1.0.

### Novedades

- **Cuenta:** alta con correo y contraseña, confirmada con un código de 6 dígitos; recuperación de la contraseña con
  otro código; inicio de sesión con Google en Android e iOS; borrar la cuenta desde Perfil.
- **Grupos:** crear un grupo con su moneda, zona horaria, color e icono; invitar con un código y unirse con él;
  archivar, eliminar o salir; papeles de owner, admin y miembro. Al abrir la app se entra en el último grupo usado.
- **Pestaña Grupo:** cuánto dinero queda realmente libre este periodo, a dónde fue el dinero, entradas y salidas,
  próximos vencimientos y avisos; «Mirar hasta…» una fecha y la revisión del periodo con el plan de movimientos entre
  sobres.
- **Cuentas:** cuentas bancarias, de ahorro, de inversión, de efectivo y tarjetas de crédito con saldo inicial;
  saldo previsto a fin de periodo en cada fila; detalle con gráfica, saldo de un día concreto, últimos movimientos,
  vencimientos y comparación con el banco.
- **Sobres:** sobres mensuales, de recibos, de ahorro y libres, con su barra de progreso; repartir el dinero «Sin
  asignar», mover dinero entre sobres y rellenarlos cada periodo hasta lo que necesitan.
- **Compromisos:** gastos, ingresos y transferencias que se repiten, con su reserva en un sobre de recibos;
  confirmar o saltar cada vencimiento; aportaciones periódicas a otro grupo.
- **Movimientos:** gastos, ingresos, ajustes y transferencias entre cuentas; búsqueda con filtros por cuenta, sobre,
  tipo, fechas, etiquetas y autor; totales e informe por periodo y por sobre de cualquier filtro; historial de cambios
  de un movimiento; al apuntar uno que parece un vencimiento pendiente, la app lo propone.
- **Etiquetas:** crear, renombrar, unir y ver los movimientos de una etiqueta.
- **Importar del banco:** leer el CSV de una cuenta, revisar qué es cada fila (ya apuntada, vencimiento, nueva) y
  aplicarlo todo de una vez, con la opción de deshacer la importación.
- **Exportar** un periodo a CSV y compartirlo.
- **Tiempo real:** lo que apunta otro miembro del grupo aparece en unos segundos sin recargar.
- **Gráficas** que se abren a pantalla completa con rango, valores y tabla.
- **Deshacer** en cada borrado, sin diálogos de confirmación.
- **Pantallas grandes:** tablet y plegable con lista y detalle a la vez y dos columnas con su propio scroll.
- **Idiomas y accesibilidad:** español e inglés, modo oscuro, colores para daltonismo y una etiqueta en cada icono al
  mantenerlo pulsado; un «ⓘ» explica las cifras clave.
- **Versión de la app** al pie de Perfil, para saber qué versión está instalada al avisar de un fallo. (#51)

**Base de datos:** `Sobre-IO-DB` con todas las migraciones hasta D-56.
