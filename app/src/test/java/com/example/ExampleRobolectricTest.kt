package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.CompilerExecutionState
import com.example.domain.CompilerService
import com.example.domain.DiagnosticSeverity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CS 1.6 Plugin Studio", appName)
    }

    @Test
    fun `compiler service parses real amxxpc error and warning lines`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = CompilerService(context)
        val rawOutput = """
            plugin.sma(14) : warning 217: loose indentation
            plugin.sma(28) : error 017: undefined symbol "zp_get_user_zombie"
            plugin.sma(31) : fatal error 100: cannot read from file: "zombieplague"
        """.trimIndent()

        val diags = service.parseAmxxpcOutput(rawOutput)
        assertEquals(3, diags.size)
        assertEquals(DiagnosticSeverity.WARNING, diags[0].severity)
        assertEquals(14, diags[0].line)
        assertEquals("217", diags[0].code)

        assertEquals(DiagnosticSeverity.ERROR, diags[1].severity)
        assertEquals(28, diags[1].line)
        assertEquals("017", diags[1].code)

        assertEquals(DiagnosticSeverity.ERROR, diags[2].severity)
        assertEquals(31, diags[2].line)
        assertEquals("100", diags[2].code)
    }

    @Test
    fun `compiler service never generates fake amxx when server is not configured`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = CompilerService(context)

        val result = service.compileWithRealCompiler(
            fileName = "vip_test.sma",
            sourceCode = "#include <amxmodx>\npublic plugin_init() {}",
            compilerVersion = "AMX Mod X 1.10.0",
            serverUrl = "",
            customIncludes = emptyList(),
            onProgress = { _, _ -> }
        )

        assertFalse(result.succeeded)
        assertEquals(CompilerExecutionState.SERVER_UNAVAILABLE, result.executionState)
        assertNull(result.amxxBytes)
    }

    @Test
    fun `compiler service rejects unsafe filename and static analyzer runs separately`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = CompilerService(context)

        val staticReport = service.runStaticAnalysis(
            "vip_test.sma",
            "#include <amxmodx>\npublic plugin_init() { register_plugin(\"A\", \"1.0\", \"B\"); }"
        )
        assertEquals(0, staticReport.errorCount)

        val badNameResult = service.compileWithRealCompiler(
            fileName = "../evil.txt",
            sourceCode = "public plugin_init() {}",
            compilerVersion = "AMX Mod X 1.9.0",
            serverUrl = "http://127.0.0.1:8080",
            customIncludes = emptyList(),
            onProgress = { _, _ -> }
        )
        assertFalse(badNameResult.succeeded)
        assertEquals(CompilerExecutionState.COMPILE_ERROR, badNameResult.executionState)
        assertTrue(badNameResult.diagnostics.any { it.code == "SEC01" })
        assertNull(badNameResult.amxxBytes)
    }
}
