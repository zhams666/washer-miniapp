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

function assertWxmlStructure(relativePath) {
  const source = readFileSync(resolve(root, relativePath), 'utf8').replaceAll('wx:', 'data-wx-');
  const stack = [];
  const tokens = source.match(/<\/?[A-Za-z][^<>]*>/g) || [];

  for (const token of tokens) {
    const closing = /^<\//.test(token);
    const match = token.match(/^<\/?([A-Za-z][\w-]*)/);
    if (!match) {
      continue;
    }
    const tag = match[1];
    if (closing) {
      assert.equal(stack.pop(), tag, `${relativePath} has mismatched closing tag: ${tag}`);
    } else if (!/\/\s*>$/.test(token)) {
      stack.push(tag);
    }
  }

  assert.deepEqual(stack, [], `${relativePath} has unclosed WXML tags`);
}

test('首页和底部导航 WXML 结构完整', () => {
  assertWxmlStructure('pages/home/index.wxml');
  assertWxmlStructure('custom-tab-bar/index.wxml');
});

test('首页引用的本地背景资源存在', () => {
  const source = readFileSync(resolve(root, 'pages/home/index.wxml'), 'utf8');
  const assetPaths = [...source.matchAll(/src="(\/assets\/[^"']+)"/g)].map((match) => match[1]);
  assert.ok(assetPaths.length > 0, 'home page should declare a local asset');
  for (const assetPath of assetPaths) {
    assert.ok(existsSync(resolve(root, assetPath.slice(1))), `missing home asset: ${assetPath}`);
  }
});
