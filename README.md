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
sesión, recuperación de la contraseña con otro código y pantalla provisional de sesión iniciada con cierre de sesión
(la sustituirá la lista de grupos). El inicio de sesión con Google está programado, pero no se puede probar hasta tener
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
si esa comprobación falla. Las versiones entregadas se publican como *releases* con la APK adjunta.

Para instalarla en el móvil de pruebas sin Android Studio basta con copiar la APK y abrirla (hay que permitir
instalar aplicaciones de origen desconocido).

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

Para la APK de publicación hace falta otro cliente Android con la huella de su clave. El emulador necesita una cuenta de
Google iniciada.

En iOS se usa el SDK GoogleSignIn (Swift Package Manager). Hace falta un cliente OAuth de tipo **iOS** con el bundle id
`dev.lacruzjs.sobreio`; su identificador y el esquema de URL invertido están en `iosApp/project.yml` (`GIDClientID` y
`CFBundleURLTypes`). El botón también depende de `GOOGLE_WEB_CLIENT_ID` en `local.properties`.
