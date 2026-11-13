package com.example.domain

enum class DiagnosticSeverity { ERROR, WARNING, INFO }

data class PawnDiagnostic(
    val line: Int,
    val column: Int,
    val severity: DiagnosticSeverity,
    val code: String,
    val message: String
)

object PawnStaticAnalyzer {

    fun analyze(source: String): List<PawnDiagnostic> {
        val diagnostics = mutableListOf<PawnDiagnostic>()
        val lines = source.lines()

        var hasAmxModXInclude = false
        var hasPluginInit = false
        var hasRegisterPlugin = false
        val usesPragmaSemicolon = source.contains(Regex("""#pragma\s+semicolon\s+1"""))

        data class BraceToken(val ch: Char, val line: Int, val col: Int)
        val stack = ArrayDeque<BraceToken>()
        var inBlockComment = false
        var blockCommentStartLine = 1

        lines.forEachIndexed { idx, rawLine ->
            val lineNum = idx + 1
            val trimmed = rawLine.trim()

            if (trimmed.contains("#include <amxmodx>") || trimmed.contains("#include \"amxmodx\"")) {
                hasAmxModXInclude = true
            }
            if (trimmed.contains("plugin_init(")) {
                hasPluginInit = true
            }
            if (trimmed.contains("register_plugin(")) {
                hasRegisterPlugin = true
            }

            // Detect unbalanced quotes or comments character by character
            var i = 0
            var inString = false
            var stringChar = '"'
            var stringStartCol = 1
            while (i < rawLine.length) {
                val c = rawLine[i]
                val next = if (i + 1 < rawLine.length) rawLine[i + 1] else '\u0000'

                if (inBlockComment) {
                    if (c == '*' && next == '/') {
                        inBlockComment = false
                        i += 2
                        continue
                    }
                    i++
                    continue
                }

                if (!inString && c == '/' && next == '/') {
                    break // rest of line is comment
                }
                if (!inString && c == '/' && next == '*') {
                    inBlockComment = true
                    blockCommentStartLine = lineNum
                    i += 2
                    continue
                }

                if (inString) {
                    if ((c == '\\' || c == '^') && i + 1 < rawLine.length) {
                        i += 2
                        continue
                    }
                    if (c == stringChar) {
                        inString = false
                    }
                    i++
                    continue
                }

                if (c == '"' || c == '\'') {
                    inString = true
                    stringChar = c
                    stringStartCol = i + 1
                    i++
                    continue
                }

                when (c) {
                    '{', '(', '[' -> stack.addLast(BraceToken(c, lineNum, i + 1))
                    '}', ')', ']' -> {
                        val expectedOpen = when (c) {
                            '}' -> '{'
                            ')' -> '('
                            else -> '['
                        }
                        val top = stack.lastOrNull()
                        if (top == null) {
                            diagnostics.add(
                                PawnDiagnostic(
                                    line = lineNum,
                                    column = i + 1,
                                    severity = DiagnosticSeverity.ERROR,
                                    code = "E001",
                                    message = "Símbolo de cierre inesperado '$c' sin apertura correspondiente."
                                )
                            )
                        } else if (top.ch != expectedOpen) {
                            diagnostics.add(
                                PawnDiagnostic(
                                    line = lineNum,
                                    column = i + 1,
                                    severity = DiagnosticSeverity.ERROR,
                                    code = "E002",
                                    message = "Se esperaba cierre para '${top.ch}' (línea ${top.line}), pero se encontró '$c'."
                                )
                            )
                            stack.removeLast()
                        } else {
                            stack.removeLast()
                        }
                    }
                }
                i++
            }

            if (inString) {
                diagnostics.add(
                    PawnDiagnostic(
                        line = lineNum,
                        column = stringStartCol,
                        severity = DiagnosticSeverity.ERROR,
                        code = "E037",
                        message = "Cadena de texto sin cerrar antes del final de la línea."
                    )
                )
            }

            // #pragma semicolon 1 validation on simple statements
            if (usesPragmaSemicolon && !inBlockComment && trimmed.isNotEmpty() &&
                !trimmed.startsWith("//") && !trimmed.startsWith("#") &&
                !trimmed.startsWith("public ") && !trimmed.startsWith("stock ") &&
                !trimmed.startsWith("forward ") && !trimmed.startsWith("native ") &&
                !trimmed.startsWith("enum ") && !trimmed.startsWith("if") &&
                !trimmed.startsWith("else") && !trimmed.startsWith("for") &&
                !trimmed.startsWith("while") && !trimmed.startsWith("switch") &&
                !trimmed.startsWith("case ") && !trimmed.startsWith("default:") &&
                !trimmed.endsWith("{") && !trimmed.endsWith("}") &&
                !trimmed.endsWith(";") && !trimmed.endsWith(",") &&
                !trimmed.endsWith("\\") && !trimmed.endsWith(":") &&
                !trimmed.endsWith("*/")
            ) {
                diagnostics.add(
                    PawnDiagnostic(
                        line = lineNum,
                        column = rawLine.length,
                        severity = DiagnosticSeverity.WARNING,
                        code = "W001",
                        message = "#pragma semicolon 1 activo: falta ';' al final de la sentencia."
                    )
                )
            }

            // Common AMXX pitfall: cs_set_user_money / get_user_health without cstrike / fun include
            if (trimmed.contains("cs_") && !source.contains("<cstrike>")) {
                diagnostics.add(
                    PawnDiagnostic(
                        line = lineNum,
                        column = 1,
                        severity = DiagnosticSeverity.WARNING,
                        code = "W017",
                        message = "Se detectó función 'cs_*' pero falta `#include <cstrike>`."
                    )
                )
            }
            if ((trimmed.contains("RegisterHam") || trimmed.contains("ExecuteHam")) && !source.contains("<hamsandwich>")) {
                diagnostics.add(
                    PawnDiagnostic(
                        line = lineNum,
                        column = 1,
                        severity = DiagnosticSeverity.WARNING,
                        code = "W018",
                        message = "Se detectó HamSandwich pero falta `#include <hamsandwich>`."
                    )
                )
            }
        }

        if (inBlockComment) {
            diagnostics.add(
                PawnDiagnostic(
                    line = blockCommentStartLine,
                    column = 1,
                    severity = DiagnosticSeverity.ERROR,
                    code = "E003",
                    message = "Comentario de bloque '/*' abierto y nunca cerrado con '*/'."
                )
            )
        }

        while (stack.isNotEmpty()) {
            val unclosed = stack.removeLast()
            diagnostics.add(
                PawnDiagnostic(
                    line = unclosed.line,
                    column = unclosed.col,
                    severity = DiagnosticSeverity.ERROR,
                    code = "E004",
                    message = "Bloque '${unclosed.ch}' abierto en línea ${unclosed.line} sin cierre."
                )
            )
        }

        if (!hasAmxModXInclude && source.isNotBlank()) {
            diagnostics.add(
                PawnDiagnostic(
                    line = 1,
                    column = 1,
                    severity = DiagnosticSeverity.WARNING,
                    code = "W100",
                    message = "No se encontró `#include <amxmodx>`. Requerido para plugins AMX Mod X."
                )
            )
        }
        if (!hasPluginInit && source.isNotBlank()) {
            diagnostics.add(
                PawnDiagnostic(
                    line = 1,
                    column = 1,
                    severity = DiagnosticSeverity.INFO,
                    code = "I101",
                    message = "No se encontró `public plugin_init()`. Añádelo si este archivo es un plugin principal (no un .inc)."
                )
            )
        } else if (hasPluginInit && !hasRegisterPlugin) {
            diagnostics.add(
                PawnDiagnostic(
                    line = 1,
                    column = 1,
                    severity = DiagnosticSeverity.WARNING,
                    code = "W102",
                    message = "Se recomienda llamar `register_plugin(PLUGIN, VERSION, AUTHOR)` dentro de `plugin_init()`."
                )
            )
        }

        return diagnostics.sortedBy { it.line }
    }
}
