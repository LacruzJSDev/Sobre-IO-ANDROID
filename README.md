# Sobre-IO · app Android

Cliente Android de Sobre-IO para móvil, tablet y plegable, hecho con Kotlin y Jetpack Compose. Todas las reglas del
dinero están en la base de datos de Supabase (repositorio `Sobre-IO-DB`); la app muestra los datos y llama a su API.

Normas para agentes de IA (en inglés): [AGENTS.md](AGENTS.md). El código, los comentarios y los commits van en
inglés. La app está en inglés y en español: cada texto que ve el usuario es un recurso con la misma clave en
`res/values/strings.xml` (inglés, el idioma por defecto) y en `res/values-es/strings.xml` (español). En Android 13 o
superior se puede elegir el idioma de la app en los ajustes del sistema.

## Estado

Base técnica lista, sin funciones todavía: Koin, cliente de Supabase (Auth y PostgREST), navegación con
Navigation 3, tema de la marca, icono adaptativo, errores propios de la app (`AppError`) traducidos a mensajes en
inglés y español (incluidos los códigos `FZ0xx`) y formato de importes según el idioma. Al abrirla se ve una pantalla de bienvenida provisional, que sustituirá el inicio de sesión.

Paquete e identificador de la aplicación: `dev.lacruzjs.sobreio`.

## Requisitos

- Android Studio (versión reciente, con el Android Gradle Plugin 9).
- Un emulador o un móvil con Android 7.0 (API 24) o superior. Para probar tablet y plegable se usan los emuladores
  de Android Studio.
- Para trabajar contra la base de datos local: la pila de Supabase en marcha (ver `Sobre-IO-DB/README.md`).

## Abrir y ejecutar

1. Abrir esta carpeta (`Sobre-IO-ANDROID`) en Android Studio y esperar a que sincronice Gradle.
2. Elegir un emulador o el móvil y pulsar *Run*.

Desde la terminal:

    ./gradlew assembleDebug    # genera la APK de depuración en app/build/outputs/apk/debug/
    ./gradlew installDebug     # la instala en el dispositivo conectado
    ./gradlew test             # pruebas unitarias

Para instalarla en el móvil de pruebas sin Android Studio basta con copiar la APK y abrirla (hay que permitir
instalar aplicaciones de origen desconocido).

## Conectar con Supabase

La app lee la conexión de `local.properties`, que no se sube a git. Después de cambiarlo hay que volver a compilar:

    SUPABASE_URL=http://10.0.2.2:54321
    SUPABASE_PUBLISHABLE_KEY=<clave publicable>

- **En local**, la URL es `http://10.0.2.2:54321` desde el emulador (es el Mac visto desde el emulador) y la clave
  publicable la muestra `supabase status` en `Sobre-IO-DB`.
- **En el proyecto real**, la URL es `https://<ref>.supabase.co` y la clave publicable está en el panel de Supabase,
  en *Project Settings → API Keys*.
- La clave secreta (*secret* o `service_role`) no se pone nunca en la app.
- Si falta `SUPABASE_URL`, se usa la del emulador. Si falta la clave, la app arranca, pero falla en cuanto intenta
  hablar con Supabase.
- Las versiones de depuración pueden usar HTTP sin cifrar solo con `10.0.2.2`, `localhost` y `127.0.0.1`; la versión
  de publicación solo usa HTTPS.
