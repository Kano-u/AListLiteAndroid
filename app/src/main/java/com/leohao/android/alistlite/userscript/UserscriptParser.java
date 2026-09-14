package com.leohao.android.alistlite.userscript;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 油猴脚本元数据解析器。
 *
 * <p>解析标准元数据块：</p>
 * <pre>
 * // ==UserScript==
 * // @name        示例脚本
 * // @match       http://127.0.0.1:5244/*
 * // ==/UserScript==
 * </pre>
 *
 * <p>本类保持纯 Java 实现（不依赖 Android API），便于直接做单元测试。</p>
 */
public final class UserscriptParser {
    /**
     * 元数据块
     */
    private static final Pattern METADATA_BLOCK =
            Pattern.compile("==UserScript==(.*?)==/UserScript==", Pattern.DOTALL);
    /**
     * 元数据行：// @key value
     * 注意：key 与 value 之间只允许空格/制表符，避免空值行"吃掉"下一行
     */
    private static final Pattern METADATA_LINE =
            Pattern.compile("^[ \\t]*//+[ \\t]*@([A-Za-z][A-Za-z0-9_-]*)[ \\t]*(.*?)[ \\t]*$", Pattern.MULTILINE);

    private UserscriptParser() {
    }

    /**
     * 解析脚本文件
     */
    public static Userscript parse(File file) throws IOException {
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        Userscript script = parse(file.getName(), content);
        script.setFile(file);
        return script;
    }

    /**
     * 解析脚本文本
     *
     * @param id      脚本标识（一般为文件名）
     * @param content 脚本内容
     */
    public static Userscript parse(String id, String content) {
        Userscript script = new Userscript();
        script.setId(id);
        script.setName(id);
        if (content == null) {
            content = "";
        }
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        script.setCode(content);

        Matcher blockMatcher = METADATA_BLOCK.matcher(content);
        if (!blockMatcher.find()) {
            return script;
        }
        Matcher lineMatcher = METADATA_LINE.matcher(blockMatcher.group(1));
        while (lineMatcher.find()) {
            String key = lineMatcher.group(1).toLowerCase(Locale.ROOT);
            String value = lineMatcher.group(2).trim();
            if (value.isEmpty()) {
                continue;
            }
            switch (key) {
                case "name":
                    script.setName(value);
                    break;
                case "version":
                    script.setVersion(value);
                    break;
                case "description":
                    script.setDescription(value);
                    break;
                case "author":
                case "copyright":
                    script.setAuthor(value);
                    break;
                case "match":
                    script.getMatches().add(value);
                    break;
                case "include":
                    script.getIncludes().add(value);
                    break;
                case "exclude":
                    script.getExcludes().add(value);
                    break;
                case "grant":
                    script.getGrants().add(value);
                    break;
                case "run-at":
                    script.setRunAt(normalizeRunAt(value));
                    break;
                default:
                    // 其余元数据（@icon、@namespace 等）暂时忽略
                    break;
            }
        }
        return script;
    }

    /**
     * 把 @run-at 的取值归一化
     */
    static String normalizeRunAt(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("start")) {
            return Userscript.RUN_AT_DOCUMENT_START;
        }
        if (normalized.contains("idle")) {
            return Userscript.RUN_AT_DOCUMENT_IDLE;
        }
        return Userscript.RUN_AT_DOCUMENT_END;
    }
}
