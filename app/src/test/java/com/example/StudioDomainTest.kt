package com.example

import com.example.domain.AmxxDocumentation
import com.example.domain.AmxxMetamodAnalyzer
import com.example.domain.AmxxTemplates
import com.example.domain.DiagnosticSeverity
import com.example.domain.PawnStaticAnalyzer
import com.example.domain.PawnSyntaxHighlighter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioDomainTest {

    @Test
    fun templatesContainAll13RequestedModules() {
        assertEquals(13, AmxxTemplates.allTemplates.size)
        val ids = AmxxTemplates.allTemplates.map { it.id }.toSet()
        assertTrue("admin_menu" in ids)
        assertTrue("vip_system" in ids)
        assertTrue("weapon_shop" in ids)
        assertTrue("xp_levels" in ids)
        assertTrue("bot_controller" in ids)
        assertTrue("admin_commands" in ids)
        assertTrue("money_manager" in ids)
        assertTrue("teleport_system" in ids)
        assertTrue("fx_sounds" in ids)
        assertTrue("anti_cheat_guard" in ids)
        assertTrue("auto_respawn" in ids)
        assertTrue("hud_announcer" in ids)
        assertTrue("player_admin" in ids)
    }

    @Test
    fun pawnStaticAnalyzerDetectsUnclosedBraceAndMissingSemicolon() {
        val brokenCode = """
            #include <amxmodx>
            #pragma semicolon 1

            public plugin_init()
            {
                register_plugin("Test", "1.0", "Dev")
        """.trimIndent()

        val diags = PawnStaticAnalyzer.analyze(brokenCode)
        assertTrue(diags.any { it.severity == DiagnosticSeverity.ERROR && it.code == "E004" })
        assertTrue(diags.any { it.severity == DiagnosticSeverity.WARNING && it.code == "W001" && it.line == 6 })
    }

    @Test
    fun amxxMetamodAnalyzerDifferentiatesNativeAndAmxxPlugins() {
        val amxxIni = """
            admin.amxx
            dproto_ef.so
        """.trimIndent()
        val metamodIni = """
            win32 addons\amxmodx\dlls\amxmodx_mm.dll
        """.trimIndent()
        val modulesIni = "fun\n"

        val issues = AmxxMetamodAnalyzer.validateAll(
            amxxPluginsIni = amxxIni,
            modulesIni = modulesIni,
            metamodPluginsIni = metamodIni,
            targetServerOs = "Linux (.so)",
            smaSources = listOf("#include <cstrike>")
        )

        assertTrue(issues.any { it.severity == DiagnosticSeverity.ERROR && it.fileName.contains("amxmodx") })
        assertTrue(issues.any { it.severity == DiagnosticSeverity.WARNING && it.fileName.contains("metamod") })
        assertTrue(issues.any { it.fileName.contains("modules.ini") && it.message.contains("cstrike") })
    }

    @Test
    fun syntaxHighlighterAndDocsAreNonEmpty() {
        val highlighted = PawnSyntaxHighlighter.highlightPawn("#include <amxmodx>\npublic plugin_init() {}")
        assertTrue(highlighted.spanStyles.isNotEmpty())
        assertTrue(AmxxDocumentation.entries.size >= 15)
    }
}
