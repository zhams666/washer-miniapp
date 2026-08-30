import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';
import test from 'node:test';

const root = resolve(import.meta.dirname, '..');
const app = JSON.parse(readFileSync(resolve(root, 'app.json'), 'utf8'));

test('所有主包和分包页面文件均存在', () => {
  const pages = [...(app.pages || []), ...(app.subPackages || []).flatMap((pkg) =>
    (pkg.pages || []).map((page) => `${pkg.root}/${page}`))];
  assert.ok(pages.length > 0, 'app.json must declare pages');
  for (const page of pages) {
    assert.ok(existsSync(resolve(root, `${page}.ts`)), `missing page script: ${page}.ts`);
    assert.ok(existsSync(resolve(root, `${page}.wxml`)), `missing page template: ${page}.wxml`);
  }
});

test('自定义底部导航声明了五个可切换页面', () => {
  const source = readFileSync(resolve(root, 'custom-tab-bar/index.ts'), 'utf8');
  for (const page of ['pages/home/index', 'pages/service/index', 'pages/ranking/index', 'pages/order/index', 'pages/mine/index']) {
    assert.match(source, new RegExp(page.replaceAll('/', '\\/')));
  }
});
