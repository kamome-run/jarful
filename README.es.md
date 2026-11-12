# Jarful — un gestor de tareas con «bucle de juego» para cerebros con TDAH

[![CI](https://github.com/kamome-run/jarful/actions/workflows/ci.yml/badge.svg)](https://github.com/kamome-run/jarful/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[日本語](README.md) · [English](README.en.md) · [Français](README.fr.md) · [العربية](README.ar.md) · [Русский](README.ru.md) · **Español** · [Deutsch](README.de.md) · [Tiếng Việt](README.vi.md) · [Polski](README.pl.md) · [Українська](README.uk.md) · [Bahasa Indonesia](README.id.md) · [繁體中文（台灣）](README.zh-TW.md)

«Puedo jugar durante horas, pero aplazo el trabajo y las tareas de casa.» Jarful convierte el método
**notas adhesivas × frasco transparente × impresora térmica** descrito por el emprendedor con TDAH Laurie Hérault
([artículo original](https://www.laurieherault.com/articles/a-thermal-receipt-printer-cured-my-procrastination))
en una app para Android (incluidos los Chromebook y los portátiles ChromeOS/Android de Google) y Windows 11.

> **Jarful** = «un frasco lleno». Arruga cada ticket terminado y mira cómo se llena el frasco.

---

## Índice

1. [Cómo funciona](#1-cómo-funciona)
2. [Qué necesitas](#2-qué-necesitas)
3. [Plataformas compatibles](#3-plataformas-compatibles)
4. [Instalación](#4-instalación)
5. [Primer inicio y flujo diario](#5-primer-inicio-y-flujo-diario)
6. [Configuración de la impresora (detallada)](#6-configuración-de-la-impresora-detallada)
7. [Sincronización Android ⇄ Windows (detallada)](#7-sincronización-android--windows-detallada)
8. [Atajos de teclado](#8-atajos-de-teclado)
9. [Gestos táctiles](#9-gestos-táctiles)
10. [Datos y copias de seguridad](#10-datos-y-copias-de-seguridad)
11. [Idiomas](#11-idiomas)
12. [Solución de problemas](#12-solución-de-problemas)
13. [Compilar desde el código fuente](#13-compilar-desde-el-código-fuente)
14. [Licencia y aviso](#14-licencia-y-aviso)

---

## 1. Cómo funciona

| El método | En Jarful |
|-----------|-----------|
| Dividir las tareas en **microtareas de 2–5 minutos** para que el bucle se repita a menudo | Tareas jerárquicas en **columnas contiguas**; `Tab` añade una subtarea al instante |
| Una nota = una tarea; al terminar, **se arruga y se echa a un frasco transparente** | Completar un «ticket de hoy» reproduce una **animación de arrugado + sonido de papel + vibración** y deja caer una bolita en el frasco |
| Empezar el día con hábitos fáciles; **preparar el día siguiente la noche anterior** | Las **rutinas** por día de la semana generan los tickets de mañana tras la hora de preparación (21:00 por defecto) |
| Al notar que procrastinas, escribir **las próximas 3–5 tareas** y empezar | `Ctrl+K` **Reenfocar**: una tarea por línea → tickets inmediatos → la primera se pone en marcha |
| Lo que no se puede dividir se **divide por tiempo** («solo 10 minutos») | Los tickets con bloque de tiempo hacen cuenta atrás; al final: «completar / +5 min / dividir» |
| El atraso acumulado (miles de correos) pasa a ser «**todo lo nuevo + N antiguos, cada día**» | **Rutinas con cuota** (contador +1, se completa al llegar al objetivo) |
| Una **impresora térmica** elimina la fricción | Imprime por Bluetooth Classic / Bluetooth LE / COM / TCP con **ESC/POS, TSPL o CPCL**, un ticket por recibo |

## Capturas de pantalla

Windows 11 usa **Fluent Design System** (WinUI 3); Android usa **Material 3**. Los colores de marca son comunes.

| Windows 11 (Fluent) | Windows 11 oscuro | Windows 11, chino tradicional |
|---|---|---|
| ![windows](docs/screenshots/windows-fluent.png) | ![windows dark](docs/screenshots/windows-fluent-dark.png) | ![windows zh-TW](docs/screenshots/windows-fluent-zh-TW.png) |

| Android (Material 3) | Android oscuro | Android, árabe (RTL) |
|---|---|---|
| ![phone](docs/screenshots/android-material-phone.png) | ![phone dark](docs/screenshots/android-material-phone-dark.png) | ![phone ar](docs/screenshots/android-material-phone-ar.png) |

| Ticket impreso (japonés) | Árabe | Chino tradicional | Vietnamita |
|---|---|---|---|
| ![ticket](docs/screenshots/ticket-raster.png) | ![ticket ar](docs/screenshots/ticket-raster-ar.png) | ![ticket zh](docs/screenshots/ticket-raster-zh-TW.png) | ![ticket vi](docs/screenshots/ticket-raster-vi.png) |

## 2. Qué necesitas

**Imprescindible**
- Un dispositivo Android (teléfono / tableta / Chromebook / portátil ChromeOS o Android de Google) o un PC con Windows 11.

**Recomendado (para reproducir el método completo)**
- **Impresora térmica**: cualquier modelo ESC/POS de 58 mm u 80 mm (Bluetooth Classic, Bluetooth LE o red por cable / Wi-Fi). Para etiquetas, una impresora compatible con TSPL o CPCL.
- **Rollos de papel térmico** del ancho correspondiente. Manipulas mucho los tickets, así que conviene papel **sin bisfenol (sin BPA/BPS)**.
- **Una pizarra blanca e imanes**: los tickets impresos se **fijan a la pizarra con imanes** para tener el trabajo del día delante. Despegar un ticket terminado y arrugarlo es la recompensa. Ten a mano 20–30 imanes pequeños (10–15 mm).
- **Un frasco transparente** para los tickets arrugados. El frasco de la app funciona por sí solo, pero uno real refuerza el efecto.

## 3. Plataformas compatibles

| Plataforma | Descarga | Notas |
|---|---|---|
| Android 8.0 o superior | `androidApp-debug.apk` / `androidApp-release-unsigned.apk` | Teléfonos y tabletas; Material 3, color dinámico en Android 12+ |
| Chromebook (ChromeOS) / portátiles ChromeOS o Android de Google | igual | Se instala sin pantalla táctil; teclado, ratón y táctil compatibles |
| Windows 11 (x64) | `Jarful-*.msi` / `Jarful-*.exe` | Interfaz Fluent Design; impresoras Bluetooth mediante puerto COM virtual |

Las descargas están en la página [Releases](https://github.com/kamome-run/jarful/releases).

## 4. Instalación

### 4.1 Android (teléfono / tableta)

1. Abre [Releases](https://github.com/kamome-run/jarful/releases) en el navegador del dispositivo y descarga el último `androidApp-debug.apk`.
2. Toca el APK desde la notificación o la app Archivos.
3. Si Android avisa de una app desconocida, toca **Ajustes → Permitir desde esta fuente** y vuelve atrás (permiso de una sola vez para el navegador / Archivos).
4. Toca **Instalar** y luego **Abrir**.
5. En el primer inicio, elige **Añadir ejemplos** para obtener una rutina matutina que podrás editar después.

> `release-unsigned.apk` es para desarrolladores que firman y distribuyen la app por su cuenta. Normalmente usa `debug.apk`.

### 4.2 Chromebook / portátiles ChromeOS o Android de Google

ChromeOS permite instalar APK ajenos a Google Play de dos formas.

**Opción A: entorno de desarrollo Linux + adb (recomendada)**
1. **Configuración → Avanzado → Desarrolladores → Entorno de desarrollo Linux → Activar** (la primera vez tarda unos minutos).
2. En la misma pantalla activa **Desarrollar apps Android → Depuración ADB** y reinicia.
3. En la terminal Linux instala adb y conéctate al dispositivo:
   ```bash
   sudo apt update && sudo apt install -y adb
   adb connect 100.115.92.2:5555      # acepta el aviso en pantalla
   adb install ~/Downloads/androidApp-debug.apk
   ```
4. **Jarful** aparece en el iniciador. La ventana se puede redimensionar; a partir de 840 dp de ancho pasa al diseño de tres paneles.

**Opción B: distribución por Play Store gestionada** (equipos de centros educativos / empresas): un administrador puede publicar el APK como app privada.

Empareja primero la impresora Bluetooth en **Configuración de ChromeOS → Bluetooth** y luego elígela en la app (funcionan Classic y LE).

### 4.3 Windows 11

1. Descarga `Jarful-<versión>.msi` desde [Releases](https://github.com/kamome-run/jarful/releases).
2. Haz doble clic en el instalador. Si aparece la pantalla azul de **SmartScreen**, pulsa **Más información → Ejecutar de todas formas** (el instalador no está firmado; el código fuente es público en este repositorio).
3. Confirma la carpeta y pulsa **Install**. Se instala por usuario; no hacen falta permisos de administrador.
4. Inicia **Jarful** desde el menú Inicio.
5. Los datos están en `%APPDATA%\Jarful\jarful-data.json` (se muestra como «Ubicación» en Ajustes).

Para desinstalar: **Configuración → Aplicaciones → Aplicaciones instaladas → Jarful**. El archivo de datos se conserva; bórralo a mano si quieres.

## 5. Primer inicio y flujo diario

1. **Configura las rutinas** (pestaña Rutinas): enumera de arriba abajo hábitos matutinos fáciles (preparar café, abrir la ventana…). Activa los días por rutina; para hábitos con recuento como «procesar 10 correos», introduce el número como **cuota**.
2. **Preparado la noche anterior**: abrir la app después de la hora «Preparar mañana a las» (21:00 por defecto) genera los tickets de rutina de mañana. Por la mañana se crean los de hoy si faltan.
3. **Divide las tareas** (pestaña Columnas): crea una tarea grande («Limpiar la casa») en la columna izquierda, selecciónala y pulsa `Tab` (o «Añadir subtarea») para añadir «Cocina», «Baño»… en la columna siguiente, y sigue dividiendo hasta piezas de **2–5 minutos** («Fregar los platos»). Las tareas abiertas más de 3 días muestran un aviso para dividirlas más.
4. **Crea los tickets de hoy**: selecciona una tarea y pulsa `T`; para toda una columna, `Mayús+T` (o el menú de la columna). Aparecen como tarjetas tipo recibo en la pestaña Hoy.
5. **Imprime y cuelga** (opcional): `Ctrl+P` imprime todos los tickets de hoy; córtalos y **fíjalos a la pizarra con imanes**.
6. **Hazlo → completa**: **Iniciar** muestra el tiempo transcurrido (cuenta atrás si hay bloque de tiempo). Pulsa **Hecho** (o desliza la tarjeta a la derecha): se arruga y cae en el frasco con sonido y vibración. Despega el ticket de papel, arrúgalo y échalo al frasco real.
7. **Cuando te pilles procrastinando**: `Ctrl+K` (⚡ Reenfocar), escribe las próximas 3–5 tareas, una por línea, y pulsa **Empezar**. Se convierten en tickets al instante y la primera arranca.
8. **Estadísticas**: ciclos por día (90 días), racha y cumplimiento de rutinas.

## 6. Configuración de la impresora (detallada)

Pestaña Ajustes → **Impresora térmica**.

### 6.1 Elige el tipo de conexión

| Conexión | Sistemas | Impresoras habituales |
|---|---|---|
| **Bluetooth Classic (SPP)** | Android / Chromebook | Impresoras de modo dual, Bluetooth 2.1–5.x (suelen pedir PIN al emparejar) |
| **Bluetooth LE (GATT)** | Android / Chromebook | Impresoras de bolsillo solo LE, Bluetooth 4.0–5.x (vendidas «solo para app») |
| **Serial / COM** | Windows 11 | Impresoras Bluetooth Classic mediante puerto COM virtual; adaptadores USB-serie |
| **TCP/IP** | Android / Windows | Impresoras de recibos por cable / Wi-Fi (puerto 9100) |

¿No sabes cuál tienes? Si los ajustes Bluetooth del sistema pueden **emparejar** la impresora (PIN o confirmación), es Classic; si el emparejamiento falla y el manual dice «conéctate desde la app», probablemente es LE. Prueba ambas y quédate con la que supere la **impresión de prueba**.

### 6.2 Elige el protocolo de impresión

| Protocolo | Uso |
|---|---|
| **ESC/POS ráster (por defecto)** | La mayoría de impresoras de recibos de 58/80 mm. El ticket se envía como imagen, así que **todos los idiomas se imprimen bien sin depender de las fuentes internas** |
| ESC/POS bit image | Modelos antiguos sin ráster `GS v 0` |
| ESC/POS texto | Imprime con la fuente interna. Ajusta el juego de caracteres (UTF-8 / Shift_JIS / Big5 / GB18030 / Windows-125x / CP8xx…) al de la impresora |
| TSPL | Impresoras de etiquetas (alto y separación en mm) |
| CPCL | Impresoras de etiquetas CPCL |

El ancho de papel es **58 mm (384 puntos)** u **80 mm (576 puntos)**. En impresoras de bolsillo sin cuchilla, deja **Cut desactivado**; 3–5 líneas de avance es un buen valor.

### 6.3 Android / Chromebook — Bluetooth Classic

1. Enciende la impresora; mantén su botón Bluetooth si necesita modo de emparejamiento.
2. **Ajustes del dispositivo → Bluetooth → Vincular nuevo dispositivo**, elige la impresora e introduce el PIN del manual (`0000` o `1234` son habituales).
3. Jarful → Ajustes → Impresora térmica → **Bluetooth Classic (SPP)**.
4. Elige la impresora en la lista de dispositivos vinculados (🔄 actualiza). En Android 12+ concede el permiso **Dispositivos cercanos**.
5. Pulsa **Impresión de prueba**: debería salir «Jarful / Test print OK» en unos segundos.
6. Si se corta a mitad, aumenta las líneas de avance o imprime los tickets de uno en uno (⋮ → Imprimir).

### 6.4 Android / Chromebook — Bluetooth LE

1. Activa el **Bluetooth** (la mayoría de impresoras LE no necesitan emparejamiento). En Android 11 o anterior activa también la **ubicación** (necesaria para escanear BLE).
2. Jarful → Ajustes → Impresora térmica → **Bluetooth LE (GATT)**.
3. Pulsa 🔄 para escanear unos 4 segundos; las impresoras con nombre aparecen en la lista. Selecciona la tuya.
4. **Impresión de prueba**. La primera conexión puede tardar 5–10 segundos.
5. Si no imprime: reinicia la impresora, cierra del todo la app del fabricante (una impresora LE admite una sola conexión) o apaga y enciende el Bluetooth.

### 6.5 Windows 11 — Bluetooth (puerto COM virtual)

1. **Configuración → Bluetooth y dispositivos → Agregar dispositivo → Bluetooth**, empareja la impresora (PIN del manual).
2. **Configuración → Bluetooth y dispositivos → Dispositivos**, baja hasta el final y abre **Más opciones de dispositivos e impresoras**.
3. Clic derecho en la impresora → **Propiedades → Servicios**, marca **Puerto serie (SPP)** y pulsa **Aceptar**.
4. En la misma ventana abre **Más opciones de Bluetooth → Puertos COM** y anota el `COMx` **Saliente** de la impresora. Si no existe: **Agregar → Saliente → elegir la impresora → SPP**.
5. Jarful → Ajustes → Impresora térmica → **Serial / COM** → elige `COMx` → **Impresión de prueba**.
6. «PORT_OPEN_FAILED» significa que otro programa ocupa el puerto (utilidad del fabricante) o que la impresora está apagada: ciérralo y reinicia la impresora.

> La versión de Windows no puede comunicarse con impresoras solo LE. Imprime desde un dispositivo Android o usa una impresora con TCP.

### 6.6 Impresoras de red (TCP/IP)

1. Conecta la impresora a la red e imprime su **página de autotest** (normalmente manteniendo el botón de avance al encender) para ver su IP.
2. Jarful → Ajustes → **TCP/IP** → introduce la IP; puerto `9100` (por defecto).
3. **Impresión de prueba**. Reservar la IP en el router (reserva DHCP) evita que cambie.

### 6.7 Rutina de imprimir y colgar

- Por la mañana: pestaña Hoy → **Imprimir todo el día** (`Ctrl+P`) → corta → **fija a la pizarra con imanes**, de arriba abajo.
- Durante el día: tras cada ticket, despégalo, arrúgalo y échalo **al frasco transparente**. Pulsar Hecho en la app también añade una bolita al frasco virtual.
- Por la noche: los tickets de mañana se preparan solos; por la mañana solo hay que imprimir.

## 7. Sincronización Android ⇄ Windows (detallada)

Sin nube ni cuentas. **Los dispositivos de la misma Wi-Fi se sincronizan directamente** (con PIN, puerto 47831 por defecto).

### 7.1 Anfitrión (se recomienda el PC con Windows)

1. Ajustes → **Sincronización entre dispositivos** → activa **Hacer de este dispositivo el anfitrión**.
2. Anota las **direcciones de este dispositivo** (p. ej. `192.168.1.20`) y el **PIN de 6 dígitos**.
3. Si el Firewall de Windows pregunta si Jarful puede acceder a la red, permítelo en **redes privadas**.
4. Mientras la app esté abierta acepta sincronizaciones de otros dispositivos («● a la escucha»).

### 7.2 Cliente (Android y otros)

1. Ajustes → **Sincronización entre dispositivos** → en **Conectar a** introduce la **IP del anfitrión** y el **PIN**.
2. Pulsa **Sincronizar ahora**. «Sincronizado» lo confirma; el botón 🔄 de la barra superior hace lo mismo.
3. Deja activada la **sincronización automática** para sincronizar al abrir y cada 5 minutos.

### 7.3 Funcionamiento y precauciones

- Se sincronizan **tareas, tickets y rutinas**. Los ajustes propios del dispositivo (impresora, etc.) no.
- Si ambos dispositivos cambian el mismo elemento, **gana el cambio más reciente**. Los borrados también se propagan (una edición posterior lo recupera).
- El tráfico es HTTP sin cifrar dentro de la LAN. Úsalo en redes de confianza y desactiva el anfitrión en Wi-Fi públicas.
- Funciona con tres o más dispositivos si todos se conectan al mismo anfitrión.

## 8. Atajos de teclado

| Tecla | Acción |
|-----|--------|
| `N` / `Intro` | Nueva tarea en esta columna |
| `Tab` / `Mayús+Intro` | Añadir subtarea (dividir) |
| `↑ ↓` | Moverse en la columna |
| `← →` | Cambiar de columna |
| `Espacio` | Hecho / pendiente |
| `T` / `Mayús+T` | Ticket de hoy / toda la columna → hoy |
| `P` / `Mayús+P` / `Ctrl+P` | Imprimir tarea / columna / todo el día |
| `Ctrl+K` | Reenfocar |
| `F2` | Renombrar |
| `Supr` | Eliminar (`Ctrl+Z` deshace) |
| `Alt+↑ ↓` | Reordenar |
| `Ctrl+Z` | Deshacer |
| `Ctrl+1–5` | Cambiar de pestaña |
| `Esc` | Cancelar |

## 9. Gestos táctiles

| Gesto | Acción |
|---|---|
| **Deslizar un ticket a la derecha** | Completar (más del 40 % del ancho) |
| **Tocar** una tarea | Seleccionar (en teléfonos: abrir sus subtareas) |
| **Mantener pulsada** una tarea | Menú (subtarea / hoy / imprimir / renombrar / mover / eliminar) |
| **Doble toque** | Renombrar |
| **←** arriba a la izquierda | Volver a la columna superior |

Funciona en las pantallas táctiles de Chromebook y portátiles de Google y en tabletas, junto con ratón y teclado.

## 10. Datos y copias de seguridad

- Ubicación: Android `filesDir/jarful-data.json` (privado de la app); Windows `%APPDATA%\Jarful\jarful-data.json`.
- **Copia de seguridad**: Ajustes → Datos → **Exportar JSON (copiar)** copia todo al portapapeles; pégalo en una nota.
- **Restaurar**: pega en **Importar JSON**. Los datos existentes se sustituyen (`Ctrl+Z` deshace una vez).
- Nada se envía fuera del dispositivo automáticamente; el único interlocutor es el anfitrión de sincronización que configures.

## 11. Idiomas

日本語 / English / Français / العربية / Русский / Español / Deutsch / Tiếng Việt / Polski / Українська / Bahasa Indonesia / 繁體中文（台灣）.
La app sigue el idioma del sistema por defecto; cámbialo en Ajustes → Idioma. El árabe pone toda la interfaz de derecha a izquierda.
La impresión ráster funciona en todos los idiomas (se elige una fuente adecuada por línea; el árabe se liga y alinea a la derecha).

## 12. Solución de problemas

| Síntoma | Solución |
|---|---|
| El APK no se instala | Permite apps desconocidas; comprueba que sea Android 8.0+ |
| El dispositivo Bluetooth no aparece | Empareja primero en el sistema (Classic). En LE pulsa 🔄 de nuevo y activa la ubicación (Android 11 o anterior) |
| La impresión de prueba agota el tiempo | Revisa la alimentación, la distancia y otras apps conectadas; cierra la app del fabricante en LE |
| Texto ilegible (modo texto) | Ajusta el juego de caracteres a la fuente de la impresora o cambia a **ESC/POS ráster** |
| Impresión tenue | La cara brillante del papel térmico debe mirar al cabezal |
| No hay puerto COM en Windows | Añade un puerto **Saliente** como en 6.5; vuelve a emparejar |
| Sincronización: «no se puede contactar con el anfitrión» | ¿Misma Wi-Fi? ¿App anfitriona abierta? ¿Firewall permitido? |
| Sincronización: «PIN incorrecto» | Vuelve a introducir el PIN que muestra la pantalla de ajustes del anfitrión |
| Faltan tickets de rutina | Revisa los días y el interruptor «Activada»; usa «Regenerar hoy» |

## 13. Compilar desde el código fuente

```bash
# pruebas unitarias (dominio, codificación de impresión, sincronización, capturas)
./gradlew :shared:desktopTest :desktopApp:test
# APK Android (debug)
./gradlew :androidApp:assembleDebug
# instalador de Windows (ejecutar en Windows)
./gradlew :desktopApp:packageMsi
# ejecutar la app de escritorio
./gradlew :desktopApp:run
```

Requisitos: JDK 17 y el SDK de Android (API 35). Consulta [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md);
la especificación está en [docs/SPEC.md](docs/SPEC.md) y las notas de los artículos en [docs/SOURCES.md](docs/SOURCES.md) (en japonés).

## 14. Licencia y aviso

Licencia MIT. Jarful es una **implementación independiente y no oficial** inspirada en un artículo público.
No está afiliado ni respaldado por Laurie Hérault, su app Colonnes ni la redacción de Nazology, y no contiene
sus textos, imágenes ni software. No existe afiliación ni garantía con ningún producto de impresora.
