package com.leohao.android.alistlite.userscript;

import android.os.Environment;
import android.util.Log;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import com.leohao.android.alistlite.model.Alist;
import com.leohao.android.alistlite.util.Constants;
import com.leohao.android.alistlite.util.SharedDataHelper;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 油猴脚本管理器：负责脚本目录扫描、增删改、启用状态与 GM 存储。
 *
 * @author AListLite 自编译分支
 */
public class UserscriptManager {
    private static final String TAG = "UserscriptManager";
    /**
     * 新建脚本时的默认模板
     */
    private static final String TEMPLATE_SUFFIX = "\n(function () {\n"
            + "  'use strict';\n"
            + "  // 脚本只在文档加载时注入一次；OpenList 前端是单页应用，\n"
            + "  // 切换目录不会重新注入，需要监听路由变化请自行 hook history.pushState。\n"
            + "  GM_addStyle('/* 你的样式 */');\n"
            + "  console.log('[AListLite 脚本] 已注入: ' + location.href);\n"
            + "})();\n";

    private static UserscriptManager instance;

    private UserscriptManager() {
    }

    public static synchronized UserscriptManager getInstance() {
        if (instance == null) {
            instance = new UserscriptManager();
        }
        return instance;
    }

    /**
     * 脚本目录（公共目录，方便用文件管理器或数据线直接拖入 .user.js）
     */
    public File getScriptDir() {
        File dir = new File(Environment.getExternalStorageDirectory(), Constants.USER_SCRIPT_DIR_NAME);
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "创建脚本目录失败: " + dir.getAbsolutePath());
        }
        return dir;
    }

    /**
     * 扫描目录，返回全部脚本（带启用状态）
     */
    public List<Userscript> loadScripts() {
        List<Userscript> scripts = new ArrayList<>();
        File[] files = getScriptDir().listFiles();
        if (files == null) {
            return scripts;
        }
        for (File file : files) {
            if (!file.isFile() || !file.getName().toLowerCase(Locale.ROOT).endsWith(".js")) {
                continue;
            }
            try {
                Userscript script = UserscriptParser.parse(file);
                script.setEnabled(isEnabled(script.getId()));
                scripts.add(script);
            } catch (Exception e) {
                Log.e(TAG, "解析脚本失败: " + file.getName() + " - " + e.getLocalizedMessage());
            }
        }
        Collections.sort(scripts, new Comparator<Userscript>() {
            @Override
            public int compare(Userscript left, Userscript right) {
                return left.getId().compareToIgnoreCase(right.getId());
            }
        });
        return scripts;
    }

    public boolean isEnabled(String id) {
        return SharedDataHelper.getInstance()
                .getBoolShareData(Constants.SHARED_DATA_KEY_USER_SCRIPT_ENABLED_PREFIX + id, true);
    }

    public void setEnabled(String id, boolean enabled) {
        SharedDataHelper.getInstance()
                .putSharedData(Constants.SHARED_DATA_KEY_USER_SCRIPT_ENABLED_PREFIX + id, enabled);
    }

    /**
     * 保存脚本内容
     */
    public File saveScript(String fileName, String content) throws IOException {
        File target = new File(getScriptDir(), normalizeFileName(fileName));
        FileUtils.write(target, content == null ? "" : content, StandardCharsets.UTF_8);
        return target;
    }

    /**
     * 删除脚本（同时清理它的启用状态）
     */
    public boolean deleteScript(Userscript script) {
        if (script == null || script.getFile() == null) {
            return false;
        }
        boolean deleted = script.getFile().delete();
        if (deleted) {
            setEnabled(script.getId(), true);
        }
        return deleted;
    }

    /**
     * 生成一段可直接使用的脚本模板
     */
    public String buildTemplate(String name) {
        String scriptName = name == null || name.isEmpty() ? "我的脚本" : name;
        return "// ==UserScript==\n"
                + "// @name         " + scriptName + "\n"
                + "// @namespace    alistlite.local\n"
                + "// @version      1.0.0\n"
                + "// @description  在这里写脚本说明\n"
                + "// @match        http://127.0.0.1:5244/*\n"
                + "// @run-at       document-idle\n"
                + "// @grant        GM_addStyle\n"
                + "// ==/UserScript==\n"
                + TEMPLATE_SUFFIX;
    }

    /**
     * 依据脚本内容推断文件名（取 @name，取不到则用时间戳）
     */
    public String buildFileName(String content) {
        String name = UserscriptParser.parse("", content).getName();
        if (name == null || name.isEmpty() || name.endsWith(".js")) {
            name = "script_" + System.currentTimeMillis();
        }
        return normalizeFileName(name);
    }

    /**
     * 保证文件名合法且以 .js 结尾
     */
    public static String normalizeFileName(String fileName) {
        String name = fileName == null ? "" : fileName.trim();
        name = name.replace("\\", "_").replace("/", "_").replace(":", "_");
        if (name.isEmpty()) {
            name = "script_" + System.currentTimeMillis();
        }
        if (!name.toLowerCase(Locale.ROOT).endsWith(".js")) {
            name = name + ".user.js";
        }
        return name;
    }

    /**
     * 取回需要注入到指定页面的脚本（供 JS 桥调用）
     *
     * @param url  当前页面地址
     * @param mode "start"（document-start）或 "end"（其余时机）
     */
    public String buildScriptsJson(String url, String mode) {
        boolean startMode = UserscriptInjector.MODE_START.equals(mode);
        String defaultTarget = resolveDefaultTarget();
        StringBuilder json = new StringBuilder("[");
        for (Userscript script : loadScripts()) {
            if (!script.isEnabled()) {
                continue;
            }
            if (script.isRunAtDocumentStart() != startMode) {
                continue;
            }
            if (!UserscriptMatcher.applies(script, url, defaultTarget)) {
                continue;
            }
            JSONObject item = JSONUtil.createObj();
            item.set("id", script.getId());
            item.set("name", script.getName());
            item.set("version", script.getVersion());
            item.set("code", script.getCode());
            if (json.length() > 1) {
                json.append(',');
            }
            json.append(item.toString());
        }
        json.append(']');
        return json.toString();
    }

    /**
     * 默认注入目标：OpenList 前端地址
     */
    private String resolveDefaultTarget() {
        try {
            Alist alist = Alist.getInstance();
            String cached = alist.getCachedServerAddress();
            if (cached != null && cached.startsWith("http")) {
                return cached;
            }
            return alist.getServerAddress();
        } catch (Exception e) {
            Log.w(TAG, "获取默认注入地址失败: " + e.getLocalizedMessage());
            return null;
        }
    }

    // ==================== GM 存储 ====================

    public String getValue(String scriptId, String key, String defaultValue) {
        String value = SharedDataHelper.getInstance().getStringShareData(valueKey(scriptId, key));
        return value == null ? defaultValue : value;
    }

    public void setValue(String scriptId, String key, String value) {
        SharedDataHelper.getInstance().putSharedData(valueKey(scriptId, key), value == null ? "" : value);
    }

    /**
     * 删除 GM 存储值（实现上以空串表示已删除）
     */
    public void deleteValue(String scriptId, String key) {
        SharedDataHelper.getInstance().putSharedData(valueKey(scriptId, key), "");
    }

    private String valueKey(String scriptId, String key) {
        return Constants.SHARED_DATA_KEY_USER_SCRIPT_VALUE_PREFIX + scriptId + "__" + key;
    }
}
