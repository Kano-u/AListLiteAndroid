package com.leohao.android.alistlite.userscript;

import android.util.Log;
import android.webkit.WebView;

import com.leohao.android.alistlite.util.Constants;

/**
 * 把油猴脚本注入到 WebView 页面。
 *
 * <p>实现方式：注入一段 bootstrap 脚本，由它通过 JS 桥取回匹配的脚本源码，
 * 再逐个在页面全局作用域执行，避免把大段脚本源码拼进 evaluateJavascript 字符串。</p>
 *
 * @author AListLite 自编译分支
 */
public final class UserscriptInjector {
    private static final String TAG = "UserscriptInjector";
    /**
     * @run-at document-start 阶段
     */
    public static final String MODE_START = "start";
    /**
     * document-end / document-idle 阶段
     */
    public static final String MODE_END = "end";

    private UserscriptInjector() {
    }

    /**
     * 注入脚本
     *
     * @param mode {@link #MODE_START} 或 {@link #MODE_END}
     */
    public static void inject(WebView webView, String url, String mode) {
        if (webView == null || url == null) {
            return;
        }
        // 只处理真实网页，跳过 app 内置的 about / release-log 页面
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return;
        }
        try {
            if (MODE_END.equals(mode)) {
                // 页面加载完成时，先补一次 document-start 注入：
                // bootstrap 自带防重复标记，已注入过则是空操作；
                // 若 onPageStarted 阶段没赶上（WebView 常见情况），这里做兜底。
                webView.evaluateJavascript(
                        createBootstrapJs(MODE_START) + "\n" + createBootstrapJs(MODE_END), null);
            } else {
                webView.evaluateJavascript(createBootstrapJs(mode), null);
            }
        } catch (Throwable t) {
            Log.e(TAG, "inject: " + t.getLocalizedMessage());
        }
    }

    /**
     * 生成 bootstrap 脚本
     */
    static String createBootstrapJs(String mode) {
        StringBuilder js = new StringBuilder();
        js.append("(function () {\n");
        js.append("  var mode = '").append(mode).append("';\n");
        js.append("  var guard = '__alistlite_userscript_' + mode;\n");
        js.append("  if (window[guard]) { return; }\n");
        js.append("  window[guard] = true;\n");
        js.append("  var bridge = window.").append(Constants.JS_INTERFACE_USER_SCRIPT).append(";\n");
        js.append("  if (!bridge || typeof bridge.getScripts !== 'function') { return; }\n");
        js.append("  var scripts = [];\n");
        js.append("  try {\n");
        js.append("    scripts = JSON.parse(bridge.getScripts(location.href, mode)) || [];\n");
        js.append("  } catch (e) {\n");
        js.append("    try { bridge.log('获取脚本列表失败: ' + e); } catch (ignore) {}\n");
        js.append("    return;\n");
        js.append("  }\n");
        js.append("  for (var i = 0; i < scripts.length; i++) {\n");
        js.append("    (function (script) {\n");
        js.append("      function report(message) {\n");
        js.append("        try { bridge.log(script.name + ': ' + message); } catch (ignore) {}\n");
        js.append("      }\n");
        js.append("      function safe(fn) {\n");
        js.append("        return function () {\n");
        js.append("          try { return fn.apply(null, arguments); } catch (e) { report('GM 接口出错: ' + e); }\n");
        js.append("        };\n");
        js.append("      }\n");
        js.append("      var api = {\n");
        js.append("        info: { script: { name: script.name, version: script.version, id: script.id } },\n");
        js.append("        addStyle: safe(function (css) {\n");
        js.append("          var el = document.createElement('style');\n");
        js.append("          el.setAttribute('type', 'text/css');\n");
        js.append("          el.textContent = css;\n");
        js.append("          (document.head || document.documentElement).appendChild(el);\n");
        js.append("          return el;\n");
        js.append("        }),\n");
        js.append("        setValue: safe(function (key, value) {\n");
        js.append("          bridge.setValue(script.id, String(key), JSON.stringify(value === undefined ? null : value));\n");
        js.append("        }),\n");
        js.append("        getValue: safe(function (key, defaultValue) {\n");
        js.append("          var raw = bridge.getValue(script.id, String(key), '');\n");
        js.append("          if (raw === null || raw === undefined || raw === '') { return defaultValue; }\n");
        js.append("          try { return JSON.parse(raw); } catch (e) { return raw; }\n");
        js.append("        }),\n");
        js.append("        deleteValue: safe(function (key) { bridge.deleteValue(script.id, String(key)); }),\n");
        js.append("        log: safe(function () {\n");
        js.append("          bridge.log(script.name + ': ' + Array.prototype.slice.call(arguments).join(' '));\n");
        js.append("        })\n");
        js.append("      };\n");
        js.append("      window.GM = api;\n");
        js.append("      window.GM_addStyle = api.addStyle;\n");
        js.append("      window.GM_setValue = api.setValue;\n");
        js.append("      window.GM_getValue = api.getValue;\n");
        js.append("      window.GM_deleteValue = api.deleteValue;\n");
        js.append("      window.GM_log = api.log;\n");
        js.append("      if (!window.unsafeWindow) { window.unsafeWindow = window; }\n");
        js.append("      try {\n");
        js.append("        (0, eval)(script.code);\n");
        js.append("      } catch (e) {\n");
        js.append("        report('执行出错: ' + e);\n");
        js.append("      }\n");
        js.append("    })(scripts[i]);\n");
        js.append("  }\n");
        js.append("})();");
        return js.toString();
    }
}
