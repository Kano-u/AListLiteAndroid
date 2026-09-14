package com.leohao.android.alistlite.userscript;

import java.util.regex.Pattern;

/**
 * 油猴脚本匹配规则判定。
 *
 * <p>支持：</p>
 * <ul>
 *     <li>@match：{@code scheme://host/path} 形式的通配，scheme、host、path 均支持通配符</li>
 *     <li>@include / @exclude：glob（如 {@code http://127.0.0.1:5244/*}）或 {@code /正则/}</li>
 * </ul>
 *
 * <p>本类保持纯 Java 实现（不依赖 Android API），便于直接做单元测试。</p>
 */
public final class UserscriptMatcher {
    private UserscriptMatcher() {
    }

    /**
     * 判断脚本是否需要注入到指定 URL
     *
     * @param url           当前页面地址
     * @param defaultTarget 没有任何 @match/@include 时使用的默认目标前缀（OpenList 前端地址），可为 null
     */
    public static boolean applies(Userscript script, String url, String defaultTarget) {
        if (script == null || url == null || url.isEmpty()) {
            return false;
        }
        // @exclude 优先级最高
        for (String exclude : script.getExcludes()) {
            if (matchesRule(exclude, url)) {
                return false;
            }
        }
        for (String match : script.getMatches()) {
            if (matchesMatchPattern(match, url)) {
                return true;
            }
        }
        for (String include : script.getIncludes()) {
            if (matchesRule(include, url)) {
                return true;
            }
        }
        if (script.getMatches().isEmpty() && script.getIncludes().isEmpty()) {
            return matchesDefaultTarget(url, defaultTarget);
        }
        return false;
    }

    /**
     * 没有匹配规则时，只注入到 OpenList 前端页面（如 http://127.0.0.1:5244/...）
     */
    static boolean matchesDefaultTarget(String url, String defaultTarget) {
        if (defaultTarget == null || defaultTarget.isEmpty()) {
            return false;
        }
        String base = defaultTarget;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            return false;
        }
        return url.equals(base) || url.startsWith(base + "/");
    }

    /**
     * @match 规则判定
     */
    public static boolean matchesMatchPattern(String pattern, String url) {
        if (pattern == null || pattern.isEmpty() || url == null) {
            return false;
        }
        try {
            return toMatchPattern(pattern).matcher(url).matches();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @include / @exclude 规则判定：/正则/ 或 glob
     */
    static boolean matchesRule(String rule, String url) {
        if (rule == null || rule.isEmpty() || url == null) {
            return false;
        }
        if (rule.length() > 2 && rule.startsWith("/") && rule.endsWith("/")) {
            try {
                return Pattern.compile(rule.substring(1, rule.length() - 1)).matcher(url).find();
            } catch (Exception e) {
                return false;
            }
        }
        try {
            return toGlobPattern(rule).matcher(url).matches();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@code scheme://host/path} 通配 -> 正则
     */
    static Pattern toMatchPattern(String pattern) {
        String scheme = "*";
        String rest = pattern;
        int schemeIndex = pattern.indexOf("://");
        if (schemeIndex > 0) {
            scheme = pattern.substring(0, schemeIndex);
            rest = pattern.substring(schemeIndex + 3);
        }
        String host = rest;
        String path = "/*";
        int slashIndex = rest.indexOf('/');
        if (slashIndex >= 0) {
            host = rest.substring(0, slashIndex);
            path = rest.substring(slashIndex);
        }
        StringBuilder regex = new StringBuilder("^");
        if ("*".equals(scheme)) {
            regex.append("https?");
        } else {
            regex.append(escape(scheme));
        }
        regex.append("://");
        if ("*".equals(host)) {
            regex.append("[^/]+");
        } else if (host.startsWith("*.")) {
            // *.example.com 同时匹配 example.com 及其子域
            regex.append("(?:[^/]+\\.)?").append(escape(host.substring(2)));
        } else {
            regex.append(escape(host).replace("\\*", "[^/]*"));
        }
        regex.append(escape(path).replace("\\*", ".*"));
        regex.append("$");
        return Pattern.compile(regex.toString());
    }

    /**
     * glob -> 正则（整串匹配）
     */
    static Pattern toGlobPattern(String glob) {
        return Pattern.compile("^" + escape(glob).replace("\\*", ".*") + "$");
    }

    /**
     * 转义正则元字符（* 会被转义成 \*，由调用方替换为通配）
     */
    private static String escape(String input) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ("\\^$.|?*+()[]{}".indexOf(c) >= 0) {
                builder.append('\\');
            }
            builder.append(c);
        }
        return builder.toString();
    }
}
