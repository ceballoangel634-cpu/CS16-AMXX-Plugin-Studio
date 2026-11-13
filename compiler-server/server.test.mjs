import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { createCompilerServer, parseCompilerDiagnostics } from './server.mjs';

function setupFakeAmxxRoot() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'amxx-test-root-'));
  for (const ver of ['1.9.0', '1.10.0']) {
    const scripting = path.join(root, ver, 'addons', 'amxmodx', 'scripting');
    const inc = path.join(scripting, 'include');
    fs.mkdirSync(inc, { recursive: true });
    fs.writeFileSync(path.join(inc, 'amxmodx.inc'), '// stub include');

    // Ejecutable stub que simula salida autentica de amxxpc y produce bytes binarios .amxx
    const script = `#!/usr/bin/env node
const fs = require('fs');
const args = process.argv.slice(2);
const smaPath = args[0];
const outArg = args.find(a => a.startsWith('-o'));
const outPath = outArg ? outArg.slice(2) : '';
const src = fs.readFileSync(smaPath, 'utf8');
if (src.includes('ERROR_SYNTAX_TEST')) {
  console.log(smaPath + '(4) : error 017: undefined symbol "bad_symbol"');
  console.log('\\n1 Error.\\nCould not locate output file ' + outPath);
  process.exit(1);
}
fs.writeFileSync(outPath, Buffer.from([0x58, 0x58, 0x4D, 0x41, 0x01, 0x02]));
console.log('AMX Mod X Compiler ${ver}');
console.log(smaPath + '(2) : warning 203: symbol is never used: "unused_var"');
console.log('Header size: 256 bytes\\nDone.');
process.exit(0);
`;
    const binPath = path.join(scripting, 'amxxpc');
    fs.writeFileSync(binPath, script, 'utf8');
    fs.chmodSync(binPath, 0o755);
  }
  return root;
}

test('parseCompilerDiagnostics counts real amxxpc errors and warnings', () => {
  const sample = `
plugin.sma(12) : warning 217: loose indentation
plugin.sma(19) : error 017: undefined symbol "foo"
plugin.sma(25) : error 001: expected token: ";", but found "}"
2 Errors.
`;
  const parsed = parseCompilerDiagnostics(sample);
  assert.equal(parsed.errorCount, 2);
  assert.equal(parsed.warningCount, 1);
});

test('compiler server health, compile success, error handling, and security checks', async () => {
  const baseDir = setupFakeAmxxRoot();
  const server = createCompilerServer({ baseDir, maxSourceBytes: 16384, compileTimeoutMs: 5000 });
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  const { port } = server.address();
  const baseUrl = `http://127.0.0.1:${port}`;

  try {
    // 1. Health endpoint
    const healthRes = await fetch(`${baseUrl}/health`);
    assert.equal(healthRes.status, 200);
    const health = await healthRes.json();
    assert.equal(health.ok, true);
    assert.equal(health.availableCompilers['1.9.0'], true);
    assert.equal(health.availableCompilers['1.10.0'], true);

    // 2. Successful compilation with custom include
    const okRes = await fetch(`${baseUrl}/compile`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        filename: 'vip_test.sma',
        compiler: 'AMX Mod X 1.10.0',
        source: '#include <amxmodx>\nnew unused_var;\npublic plugin_init() {}',
        customIncludes: [{ name: 'my_custom.inc', content: 'stock helper() {}' }]
      })
    });
    assert.equal(okRes.status, 200);
    const okJson = await okRes.json();
    assert.equal(okJson.success, true);
    assert.equal(okJson.errorCount, 0);
    assert.equal(okJson.warningCount, 1);
    assert.equal(okJson.outputFilename, 'vip_test.amxx');
    assert.ok(Buffer.from(okJson.amxxBase64, 'base64').length > 0);

    // 3. Compilation failure returns real errors and NO amxx binary
    const errRes = await fetch(`${baseUrl}/compile`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        filename: 'broken.sma',
        compiler: 'AMX Mod X 1.9.0',
        source: 'ERROR_SYNTAX_TEST'
      })
    });
    assert.equal(errRes.status, 200);
    const errJson = await errRes.json();
    assert.equal(errJson.success, false);
    assert.equal(errJson.errorCount, 1);
    assert.equal(errJson.amxxBase64, '');

    // 4. Reject unsafe filename / path traversal
    const badRes = await fetch(`${baseUrl}/compile`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        filename: '../evil.sh',
        compiler: '1.9.0',
        source: 'echo hi'
      })
    });
    assert.equal(badRes.status, 400);
  } finally {
    await new Promise((resolve) => server.close(resolve));
    fs.rmSync(baseDir, { recursive: true, force: true });
  }
});
