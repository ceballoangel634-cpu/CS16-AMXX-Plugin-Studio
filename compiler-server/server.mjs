import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import { execFile } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const LOCAL_AMXX_DIR = path.join(__dirname, 'amxx');
const PORT = Number(process.env.PORT || 8080);
const AMXX_BASE_DIR = process.env.AMXX_BASE_DIR || (fs.existsSync('/opt/amxx') ? '/opt/amxx' : LOCAL_AMXX_DIR);
const MAX_SOURCE_BYTES = Number(process.env.MAX_SOURCE_BYTES || 512 * 1024);
const COMPILE_TIMEOUT_MS = Number(process.env.COMPILE_TIMEOUT_MS || 15000);
const COMPILER_API_TOKEN = (process.env.COMPILER_API_TOKEN || '').trim();

const SAFE_SMA_REGEX = /^[a-zA-Z0-9_.-]+\.sma$/;
const SAFE_INC_REGEX = /^[a-zA-Z0-9_.-]+\.inc$/;

export function normalizeVersionKey(rawVersion = '') {
  const v = String(rawVersion).toLowerCase();
  if (v.includes('1.10')) return '1.10.0';
  if (v.includes('1.8')) return '1.8.2';
  return '1.9.0';
}

export function resolveCompilerPaths(versionKey, baseDir = AMXX_BASE_DIR) {
  const candidates = [
    path.join(baseDir, versionKey, 'addons', 'amxmodx', 'scripting'),
    path.join(baseDir, versionKey, 'scripting'),
    path.join(baseDir, 'scripting')
  ];

  for (const scriptingDir of candidates) {
    const exeName = process.platform === 'win32' ? 'amxxpc.exe' : 'amxxpc';
    const binary = path.join(scriptingDir, exeName);
    if (fs.existsSync(binary)) {
      return {
        scriptingDir,
        binary,
        includeDir: path.join(scriptingDir, 'include')
      };
    }
  }
  return null;
}

export function parseCompilerDiagnostics(output = '') {
  let errorCount = 0;
  let warningCount = 0;

  for (const line of output.split(/\r?\n/)) {
    if (/\(\d+\)\s*:\s*(fatal\s+error|error)\s+\d+/i.test(line)) {
      errorCount++;
    } else if (/\(\d+\)\s*:\s*warning\s+\d+/i.test(line)) {
      warningCount++;
    }
  }

  const summaryErr = output.match(/(\d+)\s+Errors?\./i);
  if (summaryErr) {
    errorCount = Math.max(errorCount, Number(summaryErr[1]));
  }
  return { errorCount, warningCount };
}

function runCompilerProcess(binary, args, cwd, timeoutMs) {
  return new Promise((resolve) => {
    const startedAt = Date.now();
    execFile(
      binary,
      args,
      { cwd, timeout: timeoutMs, maxBuffer: 1024 * 1024 },
      (error, stdout, stderr) => {
        const durationMs = Date.now() - startedAt;
        const combinedOutput = [stdout, stderr].filter(Boolean).join('\n').trim();
        let exitCode = 0;
        if (error) {
          exitCode = typeof error.code === 'number' ? error.code : 1;
        }
        resolve({
          exitCode,
          timedOut: Boolean(error && error.killed),
          durationMs,
          output: combinedOutput
        });
      }
    );
  });
}

function sendJson(res, statusCode, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization'
  });
  res.end(body);
}

function readJsonBody(req, maxBytes) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let total = 0;
    req.on('data', (chunk) => {
      total += chunk.length;
      if (total > maxBytes) {
        reject(Object.assign(new Error('Payload demasiado grande'), { statusCode: 413 }));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => {
      try {
        const raw = Buffer.concat(chunks).toString('utf8');
        resolve(raw ? JSON.parse(raw) : {});
      } catch {
        reject(Object.assign(new Error('JSON inválido'), { statusCode: 400 }));
      }
    });
    req.on('error', (err) => reject(err));
  });
}

export function createCompilerServer(options = {}) {
  const baseDir = options.baseDir || AMXX_BASE_DIR;
  const maxSourceBytes = options.maxSourceBytes || MAX_SOURCE_BYTES;
  const compileTimeoutMs = options.compileTimeoutMs || COMPILE_TIMEOUT_MS;
  const apiToken = options.apiToken ?? COMPILER_API_TOKEN;

  return http.createServer(async (req, res) => {
    const urlPath = (req.url || '/').split('?')[0];

    if (req.method === 'OPTIONS') {
      res.writeHead(204, {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type, Authorization'
      });
      res.end();
      return;
    }

    if (apiToken) {
      const auth = req.headers.authorization || '';
      if (auth !== `Bearer ${apiToken}`) {
        sendJson(res, 401, { ok: false, error: 'Token de autorización inválido o faltante.' });
        return;
      }
    }

    if (req.method === 'GET' && (urlPath === '/' || urlPath === '/health')) {
      const versions = ['1.9.0', '1.10.0'];
      const availableCompilers = {};
      for (const ver of versions) {
        availableCompilers[ver] = Boolean(resolveCompilerPaths(ver, baseDir));
      }
      sendJson(res, 200, {
        ok: true,
        service: 'cs16-amxxpc-compiler',
        endpoints: ['/health', '/compile'],
        versions,
        availableCompilers,
        maxSourceBytes,
        timeoutMs: compileTimeoutMs
      });
      return;
    }

    if (req.method === 'POST' && urlPath === '/compile') {
      let tempDir = null;
      try {
        const body = await readJsonBody(req, maxSourceBytes + 256 * 1024);
        const rawFilename = String(body.filename || 'plugin.sma').trim();
        const filename = path.basename(rawFilename);
        const source = typeof body.source === 'string' ? body.source : '';
        const compilerVersion = normalizeVersionKey(body.compiler);
        const customIncludes = Array.isArray(body.customIncludes) ? body.customIncludes : [];

        if (!SAFE_SMA_REGEX.test(filename) || filename.includes('..')) {
          sendJson(res, 400, {
            success: false,
            output: `Nombre de archivo inválido: ${rawFilename}. Debe terminar en .sma sin rutas.`
          });
          return;
        }
        if (!source.trim() || Buffer.byteLength(source, 'utf8') > maxSourceBytes) {
          sendJson(res, 400, {
            success: false,
            output: `El código fuente está vacío o excede el límite de ${maxSourceBytes} bytes.`
          });
          return;
        }
        if (customIncludes.length > 25) {
          sendJson(res, 400, { success: false, output: 'Demasiados archivos .inc personalizados (máx. 25).' });
          return;
        }

        const compiler = resolveCompilerPaths(compilerVersion, baseDir);
        if (!compiler) {
          sendJson(res, 503, {
            success: false,
            compilerVersion,
            output: `Compilador oficial amxxpc ${compilerVersion} no encontrado en ${baseDir}.`
          });
          return;
        }

        tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'amxx-build-'));
        const customIncDir = path.join(tempDir, 'include');
        fs.mkdirSync(customIncDir, { recursive: true });

        for (const inc of customIncludes) {
          const incName = path.basename(String(inc?.name || '').trim());
          const incContent = typeof inc?.content === 'string' ? inc.content : '';
          if (!SAFE_INC_REGEX.test(incName) || incName.includes('..')) {
            sendJson(res, 400, { success: false, output: `Nombre de include inválido: ${incName}` });
            return;
          }
          fs.writeFileSync(path.join(customIncDir, incName), incContent, 'utf8');
        }

        const smaPath = path.join(tempDir, filename);
        const outputFilename = filename.replace(/\.sma$/i, '.amxx');
        const amxxPath = path.join(tempDir, outputFilename);
        fs.writeFileSync(smaPath, source, 'utf8');

        const args = [
          smaPath,
          `-o${amxxPath}`,
          `-i${customIncDir}`
        ];
        if (fs.existsSync(compiler.includeDir)) {
          args.push(`-i${compiler.includeDir}`);
        }

        const runResult = await runCompilerProcess(
          compiler.binary,
          args,
          compiler.scriptingDir,
          compileTimeoutMs
        );

        const { errorCount, warningCount } = parseCompilerDiagnostics(runResult.output);
        const hasBinary = runResult.exitCode === 0 && fs.existsSync(amxxPath);
        const amxxBuffer = hasBinary ? fs.readFileSync(amxxPath) : null;
        const success = Boolean(hasBinary && amxxBuffer && amxxBuffer.length > 0 && errorCount === 0);

        sendJson(res, 200, {
          success,
          compilerVersion: `AMX Mod X ${compilerVersion}`,
          durationMs: runResult.durationMs,
          errorCount,
          warningCount,
          timedOut: runResult.timedOut,
          outputFilename,
          output: runResult.timedOut
            ? `Tiempo límite agotado (${compileTimeoutMs}ms) ejecutando amxxpc.\n${runResult.output}`
            : (runResult.output || (success ? 'Done.' : 'Compilación fallida sin salida.')),
          amxxBase64: success ? amxxBuffer.toString('base64') : ''
        });
      } catch (err) {
        const status = err.statusCode || 500;
        sendJson(res, status, {
          success: false,
          output: err.message || 'Error interno en servidor de compilación.'
        });
      } finally {
        if (tempDir) {
          fs.rmSync(tempDir, { recursive: true, force: true });
        }
      }
      return;
    }

    sendJson(res, 404, { ok: false, error: 'Ruta no encontrada. Usa GET /health o POST /compile.' });
  });
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const server = createCompilerServer();
  server.listen(PORT, '0.0.0.0', () => {
    console.log(`[amxx-compiler] Escuchando en http://0.0.0.0:${PORT} (base: ${AMXX_BASE_DIR})`);
  });
}
