# Sobre-IO · app Android e iOS

Cliente de Sobre-IO para Android (móvil, tablet y plegable) y para iOS, hecho con Kotlin Multiplatform y Compose
Multiplatform: la interfaz, los ViewModels, los repositorios y los modelos se escriben una sola vez en el módulo
`shared`. Todas las reglas del dinero están en la base de datos de Supabase (repositorio `Sobre-IO-DB`); la app muestra
los datos y llama a su API.

Estructura: `shared/` (todo el código compartido y los recursos), `app/` (la aplicación Android, solo el cascarón) e
`iosApp/` (el proyecto de Xcode, solo el cascarón).

Normas para agentes de IA (en inglés): [AGENTS.md](AGENTS.md). El código, los comentarios y los commits van en
inglés. La app está en inglés y en español: cada texto que ve el usuario es un recurso con la misma clave en
`shared/src/commonMain/composeResources/values/strings.xml` (inglés, el idioma por defecto) y en `values-es-rES/strings.xml`
(español). En Android 13 o superior se puede elegir el idioma de la app en los ajustes del sistema (la lista de idiomas
está a mano en `app/src/main/res/xml/locales_config.xml`); en iOS se usa el idioma del sistema.

## Estado

Base técnica y autenticación listas: registro con confirmación del correo mediante un código de 6 dígitos, inicio de
sesión y recuperación de la contraseña con otro código. Una vez dentro: lista de grupos (crear uno con nombre y moneda,
o unirse con un código de invitación), pantalla de perfil con cierre de sesión y estructura del grupo con las pestañas
Grupo, Cuentas, Sobres y Movimientos (la de Grupo, todavía vacía). Al abrir la app se entra directamente en el último grupo usado.
El inicio de sesión con Google está programado, pero no se puede probar hasta tener
las credenciales de Google.
Además: Koin, cliente de Supabase (Auth y PostgREST), navegación con Navigation 3, tema de
la marca, icono adaptativo, errores propios de la app (`AppError`) traducidos a mensajes en inglés y español y formato
de importes según el idioma.

Paquete e identificador de la aplicación: `dev.lacruzjs.sobreio`.

## Requisitos

- Android Studio (versión reciente, con el Android Gradle Plugin 9).
- Un emulador o un móvil con Android 7.0 (API 24) o superior. Para probar tablet y plegable se usan los emuladores
  de Android Studio.
- Para iOS: un Mac con **Xcode** instalado (la app y el simulador no se pueden compilar sin él) y, solo si se cambia
  `iosApp/project.yml`, XcodeGen (`brew install xcodegen`).
- Para trabajar contra la base de datos local: la pila de Supabase en marcha (ver `Sobre-IO-DB/README.md`).

## Abrir y ejecutar

1. Abrir esta carpeta (`Sobre-IO-ANDROID`) en Android Studio y esperar a que sincronice Gradle.
2. Elegir un emulador o el móvil y pulsar *Run*.

Desde la terminal:

    ./gradlew :app:assembleDebug           # genera la APK de depuración en app/build/outputs/apk/debug/
    ./gradlew :app:installDebug            # la instala en el dispositivo conectado
    ./gradlew :shared:testAndroidHostTest  # pruebas unitarias
    ./gradlew :app:lintDebug               # lint de Android

En GitHub, cada pull request compila la app Android, pasa las pruebas y el lint de forma automática (iOS no se
compila en la integración continua: necesita un runner de macOS con Xcode); `main` no acepta cambios
si esa comprobación falla. Las versiones entregadas se publican como *releases* con la APK adjunta (ver «Versiones»).

Para instalarla en el móvil de pruebas sin Android Studio basta con copiar la APK y abrirla (hay que permitir
instalar aplicaciones de origen desconocido).

## Versiones

Cada versión entregada es un tag `vMAYOR.MENOR.PARCHE` en `main` con su GitHub Release y la APK adjunta. Lo que trae
cada una está en [CHANGELOG.md](CHANGELOG.md), en español y escrito para quien usa la app.

**Número de versión.** Sigue [SemVer](https://semver.org/lang/es/): la MENOR sube cuando hay funciones nuevas
(`feat`) y el PARCHE cuando solo hay arreglos (`fix`); la MAYOR, solo con un cambio que obligue a reinstalar o a migrar
datos a mano. Se escribe en un único sitio, `sobreio.version` en `gradle.properties`, y de ahí salen:

| Dónde | Valor | Ejemplo para 1.2.3 |
| --- | --- | --- |
| Android `versionName` | la versión | `1.2.3` |
| Android `versionCode` | mayor·10000 + menor·100 + parche | `10203` |
| iOS `CFBundleShortVersionString` (`iosApp/project.yml` e `Info.plist`) | la versión | `1.2.3` |
| iOS `CFBundleVersion` | el mismo `versionCode` | `10203` |
| Tag de git y Release | `v` + la versión | `v1.2.3` |

Los valores de iOS se escriben a mano en `iosApp/project.yml` (y en el `Info.plist` que genera `xcodegen`); la tarea
`./gradlew :app:checkIosVersion`, que corre en la CI, falla si no coinciden con `gradle.properties`.

**Cómo se anotan los cambios.** Cada PR `feat` o `fix` que se nota al usar la app añade una línea en la sección
«Sin publicar» de `CHANGELOG.md`, en *Novedades*, *Mejoras* o *Arreglos*. Un fallo de algo que todavía no se ha publicado
no es un arreglo y no se anota. La CI (`.github/workflows/changelog.yml`) no deja pasar un PR `feat` o `fix` que no toque
`CHANGELOG.md`, salvo que lleve la etiqueta `no-changelog`. Los PR `refactor`, `test`, `docs`, `build` y `chore` no
añaden nada.

**Cómo se publica una versión:**

1. Un PR `build(app): release vX.Y.Z` que cambia `sobreio.version`, los dos valores de iOS en `iosApp/project.yml` e
   `iosApp/iosApp/Info.plist` (con `xcodegen` si está instalado), y renombra «Sin publicar» a `## [X.Y.Z] · AAAA-MM-DD`
   dejando una «Sin publicar» vacía encima. Si la versión necesita una versión de la base de datos, se indica en la línea
   **Base de datos**.
2. Cuando se fusiona, se pone el tag en ese commit: `git tag vX.Y.Z <commit> && git push origin vX.Y.Z`.
3. Se genera la APK firmada (`./gradlew :app:assembleRelease`, ver «Firmar la APK de publicación»), que queda en
   `app/build/outputs/apk/release/app-release.apk`, y se renombra a `sobre-io-X.Y.Z.apk`.
4. Se crea la Release con las notas del CHANGELOG:

       gh release create vX.Y.Z sobre-io-X.Y.Z.apk --title "Sobre-IO X.Y.Z" --notes "$(tools/release-notes.sh X.Y.Z)"

5. Si el proyecto alojado de Supabase se actualizó para esta entrega, `Sobre-IO-DB` lleva el mismo tag.
6. iOS no se adjunta: se instala desde Xcode con la configuración *Release*.

Un tag publicado no se mueve ni se borra nunca; si algo sale mal, se publica la versión siguiente.

## Firmar la APK de publicación

Android solo instala APKs firmadas. La de depuración usa la clave de depuración de Android Studio; la de publicación usa
una clave propia, que se crea una vez y no necesita ninguna cuenta (ni Google Play ni de desarrollador):

    mkdir -p ~/keystores
    keytool -genkeypair -v -keystore ~/keystores/sobre-io-release.jks -alias sobre-io -keyalg RSA -keysize 4096 -validity 10000

El fichero `.jks` y sus contraseñas **nunca** entran en el repositorio. Hay que guardar una copia de seguridad fuera del
Mac: con otra clave, una versión nueva no se instala encima de la anterior (habría que desinstalar y se perderían la
sesión y las preferencias del móvil). Los datos de firma van en `local.properties`:

    RELEASE_STORE_FILE=/Users/<usuario>/keystores/sobre-io-release.jks
    RELEASE_STORE_PASSWORD=<contraseña del almacén>
    RELEASE_KEY_ALIAS=sobre-io
    RELEASE_KEY_PASSWORD=<contraseña de la clave, la misma si no se eligió otra>

Con los cuatro, `./gradlew :app:assembleRelease` genera `app/build/outputs/apk/release/app-release.apk` firmada. Si falta
alguno, la compilación sigue funcionando pero sale `app-release-unsigned.apk`, que Android no instala. Para comprobar la
firma: `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk` (está en
`~/Library/Android/sdk/build-tools/<versión>/`).

La versión de publicación pasa por R8 solo en los paquetes `androidx`, `kotlin` y `kotlinx` (`optimization.packageScope`
en `app/build.gradle.kts`); el código de la app, Supabase, Ktor y Koin quedan como están, por eso no hacen falta reglas
de R8 propias.

Una APK de publicación no se instala encima de una de depuración (están firmadas con claves distintas): hay que
desinstalar la otra primero.

## Abrir y ejecutar en iOS

1. Con Xcode instalado, abrir `iosApp/iosApp.xcodeproj`.
2. En *Signing & Capabilities* del target `iosApp`, elegir tu propio equipo (*Personal Team*, con un Apple ID gratuito).
   El repositorio no guarda ningún equipo de firma.
3. Elegir un simulador y pulsar *Run*. La primera vez Xcode ejecuta Gradle para compilar el framework `Shared`
   (tarda unos minutos).

Desde la terminal, sin firmar y con un simulador instalado (`xcrun simctl list devices available`):

    xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator \
      -destination 'id=<UDID del simulador>' build

**En tu iPhone con un Apple ID gratuito**: activar el *Modo de desarrollador* (Ajustes → Privacidad y seguridad),
conectar el móvil, elegirlo en Xcode y pulsar *Run*; la primera vez hay que confiar en el perfil en Ajustes → General →
VPN y gestión de dispositivos. Los perfiles gratuitos **caducan a los 7 días**: hay que volver a ejecutar la app desde
Xcode para renovarla.

**Supabase desde iOS.** El simulador comparte la red del Mac, así que llega a la pila local en
`http://127.0.0.1:54321`: es la dirección que usa por defecto (la de `SUPABASE_URL` cambiando `10.0.2.2` por
`127.0.0.1`) y se puede fijar con `SUPABASE_URL_IOS` en `local.properties`. La app permite HTTP sin cifrar solo en red
local (`NSAllowsLocalNetworking` en el `Info.plist`). Un iPhone físico no ve el `127.0.0.1` del Mac: hay que poner en
`SUPABASE_URL_IOS` la IP del Mac en la red local (por ejemplo `http://192.168.1.20:54321`; la pila de Supabase tiene que
aceptar conexiones de la red) o usar el proyecto alojado con HTTPS.

## Conectar con Supabase

La app lee la conexión de `local.properties`, que no se sube a git. Después de cambiarlo hay que volver a compilar:

    SUPABASE_URL=http://10.0.2.2:54321
    SUPABASE_URL_IOS=http://127.0.0.1:54321    # opcional
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
- Si la app dice «No se puede conectar con el servidor» y Supabase está en marcha, en algunos emuladores las apps no
  llegan al Mac por `10.0.2.2` (el shell del emulador sí). La alternativa es pasar el puerto por adb, que funciona
  también con un móvil conectado por USB. Hay que repetir el `adb reverse` cada vez que arranca el emulador:

      adb reverse tcp:54321 tcp:54321

  y en `local.properties`, `SUPABASE_URL=http://127.0.0.1:54321` (después, volver a compilar).

## Probar el registro en local

Con la pila local de Supabase en marcha, todos los correos (códigos de confirmación y de recuperación) llegan a
Mailpit, sin salir del Mac: interfaz en <http://127.0.0.1:54324>.

1. Crear una cuenta con un correo inventado (por ejemplo `prueba@sobre-io.test`) y una contraseña de al menos 6 caracteres.
2. Abrir Mailpit, copiar el código de 6 dígitos del correo y escribirlo en la app: entra directamente.
3. Para la recuperación: «¿Has olvidado la contraseña?», pedir el código, copiarlo de Mailpit y elegir la contraseña nueva.

## Inicio de sesión con Google

El botón «Continuar con Google» solo aparece si `local.properties` tiene el identificador de cliente **web** de Google
(el mismo que se configura en el proveedor Google de Supabase). Después de añadirlo hay que volver a compilar:

    GOOGLE_WEB_CLIENT_ID=<id de cliente web>.apps.googleusercontent.com

Además, en el mismo proyecto de Google Cloud tiene que existir un cliente OAuth de tipo **Android** con el paquete
`dev.lacruzjs.sobreio` y la huella SHA-1 de la clave que firma la APK. La de depuración se obtiene con:

    keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

Para la APK de publicación hace falta otro cliente Android con la huella de su clave (`keytool -list -v -keystore
~/keystores/sobre-io-release.jks -alias sobre-io`, pide la contraseña). El emulador necesita una cuenta de
Google iniciada.

En iOS se usa el SDK GoogleSignIn (Swift Package Manager). Hace falta un cliente OAuth de tipo **iOS** con el bundle id
`dev.lacruzjs.sobreio`; su identificador y el esquema de URL invertido están en `iosApp/project.yml` (`GIDClientID` y
`CFBundleURLTypes`). El botón también depende de `GOOGLE_WEB_CLIENT_ID` en `local.properties`.
