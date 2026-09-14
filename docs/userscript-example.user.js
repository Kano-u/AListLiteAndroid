// ==UserScript==
// @name         AListLite 示例脚本
// @namespace    alistlite.local
// @version      1.0.0
// @description  演示 AListLite 内置脚本管理器支持的元数据与 GM 接口
// @author       Kano
// @match        http://127.0.0.1:5244/*
// @run-at       document-idle
// @grant        GM_addStyle
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_log
// ==/UserScript==

(function () {
  'use strict';

  // 1) 注入样式：右上角显示一个小标签
  GM_addStyle([
    '#alistlite-badge {',
    '  position: fixed; right: 12px; top: 12px; z-index: 99999;',
    '  padding: 4px 8px; border-radius: 8px; font-size: 12px;',
    '  background: rgba(0, 122, 255, .9); color: #fff;',
    '  pointer-events: none; font-family: sans-serif;',
    '}'
  ].join('\n'));

  function render() {
    if (document.getElementById('alistlite-badge')) {
      return;
    }
    if (!document.body) {
      return;
    }
    var badge = document.createElement('div');
    badge.id = 'alistlite-badge';
    // 2) 计数持久化：用 GM_getValue / GM_setValue 记住脚本被注入的次数
    var count = GM_getValue('injectCount', 0) + 1;
    GM_setValue('injectCount', count);
    badge.textContent = '示例脚本已注入 ' + count + ' 次';
    document.body.appendChild(badge);
    GM_log('badge rendered, count = ' + count);
  }

  // OpenList 前端是单页应用，切换目录不会重新加载页面，
  // 需要自己 hook history.pushState / replaceState 才能在路由变化时重新执行。
  function hookRoute() {
    ['pushState', 'replaceState'].forEach(function (name) {
      var original = history[name];
      history[name] = function () {
        var result = original.apply(this, arguments);
        setTimeout(render, 300);
        return result;
      };
    });
    window.addEventListener('popstate', function () { setTimeout(render, 300); });
  }

  render();
  hookRoute();
})();
