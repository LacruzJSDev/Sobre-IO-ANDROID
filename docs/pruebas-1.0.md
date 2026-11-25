# Pruebas manuales de la 1.0

Matriz de pruebas manuales antes de publicar la 1.0.0 (punto 4 de la Fase 7 de [plan-1.0.md](plan-1.0.md)). Las pruebas
automáticas (tests unitarios de la app y pgTAP de la base de datos) no ven la interfaz real, los dispositivos ni la red;
esta matriz sí. El resultado alimenta el informe de pruebas del curso.

## Cómo se usa

1. Se prueba con la **APK de publicación firmada** (`./gradlew :app:assembleRelease`, ver «Firmar la APK de
   publicación» en el README) en Android y con la configuración *Release* de Xcode en iOS: es lo que se entrega, con R8
   activado. La de depuración solo para buscar la causa de un fallo.
2. **Datos:** el proyecto de Supabase alojado tiene datos reales. Las pruebas que crean o borran cosas se hacen en un
   grupo de pruebas («Pruebas 1.0») y, las de alta y borrado de cuenta, con una cuenta de correo de pruebas, nunca con la
   cuenta personal. Al terminar se elimina el grupo de pruebas.
3. Cada celda se marca con **✅** (funciona como dice «Resultado esperado»), **❌ F-n** (falla; el fallo se apunta abajo
   con su número) o **—** (no aplica a esa configuración). Una celda vacía es una prueba pendiente.
4. Cada fallo se arregla en su propio PR `fix` con la etiqueta `no-changelog` (todavía no se ha publicado ninguna
   versión) y se vuelve a probar la celda.
5. La 1.0.0 se publica cuando no queda ninguna celda vacía ni ningún ❌.

## Configuraciones

Cada configuración cambia a la vez el dispositivo, el tema, el idioma y la accesibilidad, para cubrir todas las
variantes sin multiplicar las pruebas.

| Id | Dispositivo | Tema | Idioma | Colores para daltonismo |
| --- | --- | --- | --- | --- |
| **M** | Móvil Android real (APK instalada a mano) | Claro | Español | No |
| **E** | Emulador Pixel 10 Pro (móvil) | Oscuro | Inglés | Sí |
| **T** | Emulador de tablet (Pixel Tablet, crearlo en Android Studio), vertical y horizontal | Claro | Español | No |
| **P** | Emulador Pixel 10 Pro Fold, cerrado y abierto (girar y plegar a mitad de flujo) | Oscuro | Español | No |
| **I** | Simulador de iOS (iPhone) | Claro | Inglés | Sí |

El idioma se cambia en Android 13+ desde los ajustes de la app (Ajustes → Apps → Sobre-IO → Idioma) y en iOS con el
idioma del sistema. El modo oscuro, desde los ajustes rápidos del sistema. Los colores para daltonismo, en Perfil →
Accesibilidad.

## Flujos

| # | Flujo | Pasos | Resultado esperado |
| --- | --- | --- | --- |
| 1 | Alta con correo | Crear cuenta con el correo de pruebas → escribir el código de 6 dígitos del correo | Entra sin pedir nada más; el código erróneo dice que no es válido |
| 2 | Contraseña olvidada | «¿Has olvidado la contraseña?» → código → contraseña nueva → entrar con ella | Entra con la contraseña nueva |
| 3 | Google | «Continuar con Google» → elegir cuenta | Entra; si se cancela, vuelve a la pantalla de inicio sin quedarse cargando |
| 4 | Crear grupo | Grupos → «+» → nombre, moneda, zona horaria, color e icono → guardar | El grupo aparece y se abre en la pestaña Grupo; al volver a abrir la app se entra en él |
| 5 | Invitar y unirse | Ajustes del grupo → Invitaciones «+» → copiar el código. En otro dispositivo con otra cuenta: Grupos → unirse con un código | El segundo usuario ve el grupo; el primero lo ve en Miembros |
| 6 | Cuenta y saldo inicial | Cuentas → «+» → banco con saldo inicial; tarjeta de crédito con deuda | El total de Cuentas y el «Sin asignar» suben o bajan lo que corresponde; la tarjeta sale en negativo |
| 7 | Sobres y asignar | Sobres → «+» un sobre mensual con objetivo → deslizar «Asignar» desde Sin asignar | «Libre para repartir» baja lo asignado y el sobre lo muestra con su barra |
| 8 | Gasto rápido | Movimientos → «+» → importe, cuenta, sobre → guardar (4 toques como mucho tras el importe) | El movimiento sale arriba; bajan la cuenta, el sobre y «Realmente libre» |
| 9 | Editar y deshacer | Deslizar un movimiento → Editar importe; deslizar otro → Eliminar → Deshacer; eliminar otro y esperar | La edición cambia las cifras; Deshacer lo devuelve; sin Deshacer desaparece y las cifras se recalculan |
| 10 | Filtros e informe | Movimientos → filtrar por sobre y por etiqueta → «Ver informe» | Totales de entradas, salidas y neto; informe por periodo y por sobre |
| 11 | Compromiso | Detalle de cuenta → Compromisos «+» → gasto mensual en un sobre de Recibos | Aparece en Compromisos y en los próximos vencimientos; el sobre de Recibos muestra lo reservado |
| 12 | Revisar | Grupo → «Revisar» → confirmar un vencimiento y saltar otro | Desaparecen de la revisión; el confirmado crea su movimiento |
| 13 | Mover entre sobres | Sobres → icono de movimientos entre sobres → mover dinero de un sobre a otro | Cambian los dos sobres; «Libre para repartir» no cambia |
| 14 | Importar del banco | Detalle de cuenta → importar un CSV del banco → revisar filas → aplicar → deshacer la importación | Cada fila dice si ya estaba, si es un vencimiento o si es nueva; al aplicar aparecen los movimientos; al deshacer se van |
| 15 | Aportación entre grupos | Movimientos → «+» → transferencia → «A otro grupo» → grupo y cuenta destino | Aparece una mitad en cada grupo; deslizar Eliminar en una mitad anula las dos |
| 16 | Tiempo real | Dos dispositivos en el mismo grupo; apuntar un gasto en uno | En el otro aparece en unos segundos sin tocar nada |
| 17 | Gráficas y «Mirar hasta…» | Tocar la gráfica de Grupo y la de una cuenta → cambiar el rango; «Mirar hasta…» una fecha | La gráfica se amplía con animación; el rango y la fecha cambian las cifras |
| 18 | Exportar | Movimientos → exportar un periodo | Se abre la hoja de compartir con un CSV legible en el idioma de la app |
| 19 | Pantallas anchas | En T y P: recorrer Grupo, Cuentas, Sobres, detalle de cuenta y de sobre | Lista y detalle a la vez; cada columna con su propio scroll; al plegar o girar no se pierde lo abierto |
| 20 | Sin conexión | Modo avión → abrir una pestaña y apuntar un gasto → quitar el modo avión | Los datos ya cargados se ven; el error dice que no hay conexión y nada se queda cargando; al volver, se refresca |
| 21 | Etiquetas en pantalla | Mantener pulsado cada icono del header, los chips y los botones de deslizar | Cada uno muestra su nombre |
| 22 | Versión | Perfil | Pone «Versión 1.0.0» (o la de la APK probada) |
| 23 | Borrar cuenta | Con la cuenta de pruebas: Perfil → Eliminar cuenta → activar el interruptor → eliminar | Vuelve a la pantalla de inicio; esa cuenta ya no puede entrar; el grupo compartido conserva sus movimientos |

## Resultados

Versión probada: ________ · Fecha: ________

| # | M | E | T | P | I |
| --- | --- | --- | --- | --- | --- |
| 1 Alta con correo | | | — | — | |
| 2 Contraseña olvidada | | — | — | — | |
| 3 Google | ❌ F-1 | | — | — | |
| 4 Crear grupo | | | | | |
| 5 Invitar y unirse | | | — | — | |
| 6 Cuenta y saldo inicial | | | | | |
| 7 Sobres y asignar | | | | | |
| 8 Gasto rápido | | | | | |
| 9 Editar y deshacer | | | | | |
| 10 Filtros e informe | | | | | |
| 11 Compromiso | | | | | |
| 12 Revisar | | | | | |
| 13 Mover entre sobres | | | | | |
| 14 Importar del banco | | | | | |
| 15 Aportación entre grupos | | | — | — | |
| 16 Tiempo real | | | — | — | |
| 17 Gráficas y «Mirar hasta…» | | | | | |
| 18 Exportar | | | — | — | |
| 19 Pantallas anchas | — | — | | | — |
| 20 Sin conexión | | | — | — | |
| 21 Etiquetas en pantalla | | | — | — | |
| 22 Versión | | | — | — | |
| 23 Borrar cuenta | | — | — | — | — |

## Fallos encontrados

| Id | Configuración y flujo | Qué pasa | Arreglo |
| --- | --- | --- | --- |
| F-1 | Android, flujo 3 | Si Google rechaza el inicio de sesión (por ejemplo, una APK firmada con una clave que no está dada de alta en Google Cloud: `UNREGISTERED_ON_API_CONSOLE`), la app vuelve a la pantalla de inicio sin decir nada. Debería mostrar un error. Probable causa: el error llega como `GetCredentialCancellationException` y `GoogleSignInRequester` lo trata como una cancelación del usuario, que no muestra nada. Encontrado el 2026-11-25 al probar la APK firmada | Confirmada la causa («[16] Account reauth failed.» frente a «[16] Cancelled by user.»); ahora muestra el error y cerrar el selector sigue sin mostrar nada. Falta volver a probar la celda |

## Comprobado ya (2026-11-25)

Con la APK de publicación firmada en el emulador Pixel 10 Pro (configuración parecida a E, sin daltonismo): arranca,
inicia sesión con Google una vez dado de alta el cliente Android con la huella de la clave, carga Grupos, las cuatro
pestañas del grupo, el detalle de una cuenta y su pestaña de compromisos, muestra «Version 0.1.0» en Perfil y el
tiempo real se vuelve a conectar al volver del segundo plano. Sin errores en el registro de fallos. No cuenta como celda
de la matriz: no se siguieron los flujos completos.
