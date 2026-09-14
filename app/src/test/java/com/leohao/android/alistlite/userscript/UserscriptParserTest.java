package com.leohao.android.alistlite.userscript;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 油猴脚本元数据解析测试
 */
public class UserscriptParserTest {

    private static final String SCRIPT = "// ==UserScript==\n"
            + "// @name         美化脚本\n"
            + "// @namespace    alistlite.local\n"
            + "// @version      1.2.3\n"
            + "// @description  测试用脚本\n"
            + "// @author       Kano\n"
            + "// @match        http://127.0.0.1:5244/*\n"
            + "// @match        https://127.0.0.1:5245/*\n"
            + "// @include      /https?:\\/\\/localhost:.*/\n"
            + "// @exclude      http://127.0.0.1:5244/admin\n"
            + "// @run-at       document-start\n"
            + "// @grant        GM_addStyle\n"
            + "// ==/UserScript==\n"
            + "\n"
            + "GM_addStyle('body{color:red}');\n";

    @Test
    public void parseAllSupportedMetadata() {
        Userscript script = UserscriptParser.parse("beautify.user.js", SCRIPT);

        assertEquals("beautify.user.js", script.getId());
        assertEquals("美化脚本", script.getName());
        assertEquals("1.2.3", script.getVersion());
        assertEquals("测试用脚本", script.getDescription());
        assertEquals("Kano", script.getAuthor());
        assertEquals(2, script.getMatches().size());
        assertEquals("http://127.0.0.1:5244/*", script.getMatches().get(0));
        assertEquals(1, script.getIncludes().size());
        assertEquals(1, script.getExcludes().size());
        assertEquals(1, script.getGrants().size());
        assertTrue(script.isRunAtDocumentStart());
        assertTrue(script.getCode().contains("GM_addStyle"));
    }

    @Test
    public void parseWithoutMetadataBlockUsesFileName() {
        Userscript script = UserscriptParser.parse("plain.js", "console.log(1);");

        assertEquals("plain.js", script.getName());
        assertEquals("", script.getVersion());
        assertTrue(script.getMatches().isEmpty());
        assertTrue(script.getIncludes().isEmpty());
        assertFalse(script.isRunAtDocumentStart());
        assertEquals(Userscript.RUN_AT_DOCUMENT_END, script.getRunAt());
        assertEquals("默认：OpenList 页面", script.getMatchSummary());
    }

    @Test
    public void parseIgnoresEmptyValuesAndStripsBom() {
        String content = "\uFEFF// ==UserScript==\n"
                + "// @name\n"
                + "// @version   \n"
                + "// @description 带 BOM 的脚本\n"
                + "// ==/UserScript==\n";

        Userscript script = UserscriptParser.parse("bom.user.js", content);

        assertEquals("bom.user.js", script.getName());
        assertEquals("", script.getVersion());
        assertEquals("带 BOM 的脚本", script.getDescription());
    }

    @Test
    public void normalizeRunAtVariants() {
        assertEquals(Userscript.RUN_AT_DOCUMENT_START, UserscriptParser.normalizeRunAt("Document-Start"));
        assertEquals(Userscript.RUN_AT_DOCUMENT_IDLE, UserscriptParser.normalizeRunAt("document-idle"));
        assertEquals(Userscript.RUN_AT_DOCUMENT_END, UserscriptParser.normalizeRunAt("document-end"));
        assertEquals(Userscript.RUN_AT_DOCUMENT_END, UserscriptParser.normalizeRunAt("随便写的"));
    }

    @Test
    public void parseFromFile() throws Exception {
        File file = File.createTempFile("alistlite-userscript", ".user.js");
        try {
            Files.write(file.toPath(), SCRIPT.getBytes(StandardCharsets.UTF_8));
            Userscript script = UserscriptParser.parse(file);
            assertEquals(file.getName(), script.getId());
            assertEquals("美化脚本", script.getName());
            assertEquals(file, script.getFile());
        } finally {
            assertTrue(file.delete() || !file.exists());
        }
    }
}
