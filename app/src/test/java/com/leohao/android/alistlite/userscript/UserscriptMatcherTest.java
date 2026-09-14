package com.leohao.android.alistlite.userscript;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 油猴脚本匹配规则测试
 */
public class UserscriptMatcherTest {

    private static final String OPENLIST_URL = "http://127.0.0.1:5244/";
    private static final String ALITV_URL = "http://127.0.0.1:4015/";

    private static Userscript scriptWithRules(String... rules) {
        Userscript script = new Userscript();
        script.setId("test.user.js");
        for (String rule : rules) {
            if (rule.startsWith("match:")) {
                script.getMatches().add(rule.substring("match:".length()));
            } else if (rule.startsWith("include:")) {
                script.getIncludes().add(rule.substring("include:".length()));
            } else if (rule.startsWith("exclude:")) {
                script.getExcludes().add(rule.substring("exclude:".length()));
            }
        }
        return script;
    }

    @Test
    public void matchPatternMatchesHostAndPath() {
        assertTrue(UserscriptMatcher.matchesMatchPattern(
                "http://127.0.0.1:5244/*", "http://127.0.0.1:5244/index.html"));
        assertTrue(UserscriptMatcher.matchesMatchPattern(
                "http://127.0.0.1:5244/*", "http://127.0.0.1:5244/#/folder"));
        assertFalse(UserscriptMatcher.matchesMatchPattern(
                "http://127.0.0.1:5244/*", "http://127.0.0.1:5245/index.html"));
        assertFalse(UserscriptMatcher.matchesMatchPattern(
                "http://127.0.0.1:5244/*", ALITV_URL));
    }

    @Test
    public void matchPatternSupportsSchemeAndHostWildcards() {
        assertTrue(UserscriptMatcher.matchesMatchPattern("*://*/*", "http://example.com/a/b"));
        assertTrue(UserscriptMatcher.matchesMatchPattern("*://*/*", "https://127.0.0.1:5244/"));
        assertFalse(UserscriptMatcher.matchesMatchPattern("*://*/*", "file:///android_asset/a.html"));
        assertTrue(UserscriptMatcher.matchesMatchPattern(
                "*://*.example.com/*", "http://a.example.com/x"));
        assertTrue(UserscriptMatcher.matchesMatchPattern(
                "*://*.example.com/*", "https://example.com/x"));
        assertFalse(UserscriptMatcher.matchesMatchPattern(
                "*://*.example.com/*", "https://other.com/x"));
    }

    @Test
    public void includeSupportsGlobAndRegex() {
        assertTrue(UserscriptMatcher.matchesRule(
                "http://127.0.0.1:5244/*", "http://127.0.0.1:5244/assets/app.js"));
        assertTrue(UserscriptMatcher.matchesRule(
                "*localhost*", "http://localhost:5244/"));
        assertTrue(UserscriptMatcher.matchesRule(
                "/^https?:\\/\\/127\\.0\\.0\\.1:5244/", "http://127.0.0.1:5244/"));
        assertFalse(UserscriptMatcher.matchesRule(
                "/^https?:\\/\\/127\\.0\\.0\\.1:5244/", ALITV_URL));
    }

    @Test
    public void excludesTakePrecedence() {
        Userscript script = scriptWithRules(
                "match:*://*/*",
                "exclude:*://127.0.0.1:5244/admin*");

        assertTrue(UserscriptMatcher.applies(script, OPENLIST_URL, null));
        assertFalse(UserscriptMatcher.applies(
                script, "http://127.0.0.1:5244/admin/settings", null));
    }

    @Test
    public void scriptWithoutRulesOnlyRunsOnOpenListPage() {
        Userscript script = scriptWithRules();

        assertTrue(UserscriptMatcher.applies(script, OPENLIST_URL, "http://127.0.0.1:5244"));
        assertFalse(UserscriptMatcher.applies(script, ALITV_URL, "http://127.0.0.1:5244"));
        assertFalse(UserscriptMatcher.applies(script, OPENLIST_URL, null));
    }

    @Test
    public void includeRuleRunsOutsideOpenList() {
        Userscript script = scriptWithRules("include:*://*/*");

        assertTrue(UserscriptMatcher.applies(script, ALITV_URL, "http://127.0.0.1:5244"));
        assertTrue(UserscriptMatcher.applies(script, "https://example.com/", null));
    }

    @Test
    public void invalidRegexRuleDoesNotCrash() {
        Userscript script = scriptWithRules("include:/[unclosed/");

        assertFalse(UserscriptMatcher.applies(script, OPENLIST_URL, "http://127.0.0.1:5244"));
    }
}
