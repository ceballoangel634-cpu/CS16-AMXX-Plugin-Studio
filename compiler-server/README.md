# Servidor Compilador Real AMX Mod X (`amxxpc`)

Este directorio contiene todo lo necesario para compilar archivos `.sma` reales a `.amxx` utilizando los compiladores oficiales de **AMX Mod X 1.9.0 y 1.10.0 (`amxxpc`)** con todos sus archivos `.inc` oficiales (incluyendo `amxmodx`, `amxmisc`, `cstrike`, `csx`, `fakemeta`, `hamsandwich`, `fun`, `engine`, `nvault`, etc.).

## Archivos incluidos

- `package.json` — Configuración Node.js ESM (`npm start`, `npm test`).
- `server.mjs` — Servidor HTTP aislado (`GET /`, `GET /health`, `POST /compile`) con límites de tamaño (512 KB), timeout (15 s) y limpieza automática de temporales.
- `Dockerfile` y `docker-compose.yml` — Imagen Docker con `libc6:i386` y usuario sin privilegios (`amxxbuilder`).
- `render.yaml` — Blueprint para despliegue automático en Render.
- `amxx/1.9.0/` y `amxx/1.10.0/` — Binarios oficiales `amxxpc`, `amxxpc32.so` y carpeta `include/` oficial (`base` + `cstrike`).

---

## Cómo desplegarlo gratis o económico SIN computadora (solo desde tu teléfono Android)

1. **Sube tu proyecto a GitHub desde AI Studio**
   - En el menú de configuración/exportación de Google AI Studio, pulsa **Push to GitHub** (o exporta el ZIP y súbelo a un repositorio nuevo en `github.com` desde Chrome en tu móvil).

2. **Elige un proveedor compatible con Docker x86_64 desde el navegador de tu teléfono**
   > Importante: `amxxpc` es un binario oficial de 32 bits (x86). El servidor cloud debe usar Linux **x86_64** con soporte Docker/i386 (por ejemplo **Render**, **Railway** o **Koyeb**).

   **Pasos en Render.com (plan gratuito):**
   1. Abre `https://dashboard.render.com` en Chrome en tu teléfono e inicia sesión con tu cuenta de GitHub.
   2. Toca **New +** → **Web Service**.
   3. Selecciona tu repositorio de GitHub (`CS 1.6 Plugin Studio`).
   4. Configura exactamente estos campos:
      - **Name:** `cs16-amxx-compiler`
      - **Root Directory:** `compiler-server`
      - **Language / Runtime:** `Docker`
      - **Dockerfile Path:** `./Dockerfile`
      - **Instance Type:** `Free`
   5. Pulsa **Create Web Service** y espera 2–4 minutos a que termine el build.
   6. Render te dará una URL HTTPS pública, por ejemplo:
      `https://cs16-amxx-compiler.onrender.com`

3. **Verifica desde el navegador del teléfono**
   - Abre `https://TU-APP.onrender.com/health` en Chrome.
   - Debe mostrar `"ok": true` y `"availableCompilers": { "1.9.0": true, "1.10.0": true }`.

4. **Conéctalo a tu app Android**
   - En **CS 1.6 Plugin Studio**, entra en la pestaña **Compila**.
   - Pega tu URL (sin `/compile` al final, ejemplo: `https://cs16-amxx-compiler.onrender.com`).
   - Pulsa **Verificar** → verás **Servidor en línea**.
   - Pulsa **COMPILAR SMA → AMXX** para generar y descargar archivos `.amxx` reales.
