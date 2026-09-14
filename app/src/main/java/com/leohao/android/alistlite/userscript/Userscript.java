package com.leohao.android.alistlite.userscript;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 油猴脚本（UserScript）模型。
 *
 * <p>本类保持纯 Java 实现（不依赖 Android API），便于直接做单元测试。</p>
 *
 * @author AListLite 自编译分支
 */
public class Userscript {
    /**
     * 在页面脚本执行前尽早注入（尽力而为）
     */
    public static final String RUN_AT_DOCUMENT_START = "document-start";
    /**
     * 页面加载完成后注入（默认）
     */
    public static final String RUN_AT_DOCUMENT_END = "document-end";
    /**
     * 页面加载完成且空闲后注入
     */
    public static final String RUN_AT_DOCUMENT_IDLE = "document-idle";

    /**
     * 脚本唯一标识：脚本文件名（含后缀）
     */
    private String id = "";
    private File file;
    private String name = "";
    private String version = "";
    private String description = "";
    private String author = "";
    private String runAt = RUN_AT_DOCUMENT_END;
    private String code = "";
    private boolean enabled = true;
    private final List<String> matches = new ArrayList<>();
    private final List<String> includes = new ArrayList<>();
    private final List<String> excludes = new ArrayList<>();
    private final List<String> grants = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null ? "" : id;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version == null ? "" : version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author == null ? "" : author;
    }

    public String getRunAt() {
        return runAt;
    }

    public void setRunAt(String runAt) {
        this.runAt = runAt == null || runAt.isEmpty() ? RUN_AT_DOCUMENT_END : runAt;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code == null ? "" : code;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getMatches() {
        return matches;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public List<String> getGrants() {
        return grants;
    }

    /**
     * 是否需要尽早注入（@run-at document-start）
     */
    public boolean isRunAtDocumentStart() {
        return RUN_AT_DOCUMENT_START.equalsIgnoreCase(runAt);
    }

    /**
     * 列表里展示用的匹配摘要
     */
    public String getMatchSummary() {
        List<String> rules = new ArrayList<>();
        rules.addAll(matches);
        rules.addAll(includes);
        if (rules.isEmpty()) {
            return "默认：OpenList 页面";
        }
        StringBuilder summary = new StringBuilder();
        for (String rule : rules) {
            if (summary.length() > 0) {
                summary.append("，");
            }
            summary.append(rule);
        }
        return summary.toString();
    }
}
