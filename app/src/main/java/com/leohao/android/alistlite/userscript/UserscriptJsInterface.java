package com.leohao.android.alistlite.userscript;

import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * 提供给 WebView 内脚本调用的 JS 桥（window.AlistLiteUserscript）。
 *
 * @author AListLite 自编译分支
 */
public class UserscriptJsInterface {
    private static final String TAG = "UserscriptBridge";

    /**
     * 取回当前页面需要注入的脚本列表（JSON 数组）
     *
     * @param url  当前页面地址
     * @param mode "start" 或 "end"
     */
    @JavascriptInterface
    public String getScripts(String url, String mode) {
        try {
            return UserscriptManager.getInstance().buildScriptsJson(url, mode);
        } catch (Throwable t) {
            Log.e(TAG, "getScripts: " + t.getLocalizedMessage());
            return "[]";
        }
    }

    /**
     * 读取脚本的持久化数据（GM_getValue）
     */
    @JavascriptInterface
    public String getValue(String scriptId, String key, String defaultValue) {
        try {
            return UserscriptManager.getInstance().getValue(scriptId, key, defaultValue);
        } catch (Throwable t) {
            Log.e(TAG, "getValue: " + t.getLocalizedMessage());
            return defaultValue;
        }
    }

    /**
     * 写入脚本的持久化数据（GM_setValue）
     */
    @JavascriptInterface
    public void setValue(String scriptId, String key, String value) {
        try {
            UserscriptManager.getInstance().setValue(scriptId, key, value);
        } catch (Throwable t) {
            Log.e(TAG, "setValue: " + t.getLocalizedMessage());
        }
    }

    /**
     * 删除脚本的持久化数据（GM_deleteValue）
     */
    @JavascriptInterface
    public void deleteValue(String scriptId, String key) {
        try {
            UserscriptManager.getInstance().deleteValue(scriptId, key);
        } catch (Throwable t) {
            Log.e(TAG, "deleteValue: " + t.getLocalizedMessage());
        }
    }

    /**
     * 脚本日志（GM_log），只写入 logcat
     */
    @JavascriptInterface
    public void log(String message) {
        Log.i(TAG, message == null ? "" : message);
    }
}
