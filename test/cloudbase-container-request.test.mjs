import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import test from 'node:test';
import ts from 'typescript';

const root = resolve(import.meta.dirname, '..');

const loadTypeScriptModule = (relativePath) => {
  const source = readFileSync(resolve(root, relativePath), 'utf8');
  const output = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020,
    },
  }).outputText;
  const module = { exports: {} };
  new Function('exports', 'module', output)(module.exports, module);
  return module.exports;
};

test('GET container path encodes controller request parameters', () => {
  const { buildQueryPath } = loadTypeScriptModule('utils/request-path.ts');
  assert.equal(
    buildQueryPath('/costomer/getOpenId', { code: 'abc 123', wxAppId: 'wx123', empty: undefined }),
    '/costomer/getOpenId?code=abc%20123&wxAppId=wx123'
  );
});

test('backend API modules do not bypass the CloudBase request transport', () => {
  for (const file of ['apis/costomer.ts', 'apis/admin.ts', 'apis/store.ts', 'apis/device.ts', 'apis/franchise.ts']) {
    const source = readFileSync(resolve(root, file), 'utf8');
    assert.doesNotMatch(source, /wx\.request\(/, `${file} must use apiRequest`);
  }
});

test('CloudBase container requests explicitly target the configured environment', () => {
  const source = readFileSync(resolve(root, 'utils/container-request.ts'), 'utf8');
  assert.match(source, /config:\s*\{\s*env:\s*CLOUDBASE_ENV_ID/s);
  assert.match(source, /'X-WX-SERVICE':\s*CLOUDBASE_SERVICE_NAME/);
});

test('phone login preserves a CloudBase HTTP status for diagnostics', () => {
  const source = readFileSync(resolve(root, 'pages/mine/index.ts'), 'utf8');
  assert.match(source, /数据库 HTTP \$\{status\}/);
});
