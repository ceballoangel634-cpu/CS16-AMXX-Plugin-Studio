package com.example.domain

data class ConfigIssue(
    val fileName: String,
    val line: Int,
    val severity: DiagnosticSeverity,
    val message: String
)

data class ParsedPluginEntry(
    val rawLine: String,
    val lineNumber: Int,
    val pluginName: String,
    val isEnabled: Boolean,
    val flags: String,
    val isMetamodNative: Boolean,
    val targetOsLabel: String
)

object AmxxMetamodAnalyzer {

    private val knownModules = setOf(
        "fun", "engine", "fakemeta", "geoip", "sockets", "regex",
        "nvault", "cstrike", "csx", "hamsandwich", "mysql", "sqlite"
    )

    fun parseAmxxPluginsIni(content: String): List<ParsedPluginEntry> {
        return content.lines().mapIndexedNotNull { idx, raw ->
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return@mapIndexedNotNull null
            val disabled = trimmed.startsWith(";") || trimmed.startsWith("//")
            val body = trimmed.trimStart(';', '/', ' ')
            val firstToken = body.split(Regex("\\s+")).firstOrNull().orEmpty()
            if (firstToken.isEmpty() || (!firstToken.endsWith(".amxx", true) && disabled)) {
                return@mapIndexedNotNull null
            }
            val flags = body.removePrefix(firstToken).trim()
            ParsedPluginEntry(
                rawLine = raw,
                lineNumber = idx + 1,
                pluginName = firstToken,
                isEnabled = !disabled,
                flags = flags,
                isMetamodNative = false,
                targetOsLabel = "AMX Mod X (.amxx)"
            )
        }
    }

    fun parseMetamodPluginsIni(content: String): List<ParsedPluginEntry> {
        return content.lines().mapIndexedNotNull { idx, raw ->
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return@mapIndexedNotNull null
            val disabled = trimmed.startsWith(";") || trimmed.startsWith("//")
            val body = trimmed.trimStart(';', '/', ' ')
            val parts = body.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (parts.isEmpty()) return@mapIndexedNotNull null
            val osToken = parts[0].lowercase()
            if (osToken != "win32" && osToken != "linux") return@mapIndexedNotNull null
            val path = parts.getOrNull(1).orEmpty()
            ParsedPluginEntry(
                rawLine = raw,
                lineNumber = idx + 1,
                pluginName = path,
                isEnabled = !disabled,
                flags = osToken,
                isMetamodNative = true,
                targetOsLabel = if (osToken == "win32") "Metamod Win32 (.dll)" else "Metamod Linux (.so)"
            )
        }
    }

    fun validateAll(
        amxxPluginsIni: String,
        modulesIni: String,
        metamodPluginsIni: String,
        targetServerOs: String,
        smaSources: List<String>
    ): List<ConfigIssue> {
        val issues = mutableListOf<ConfigIssue>()
        val isLinuxTarget = targetServerOs.contains("Linux", ignoreCase = true)

        // 1. Validate AMX Mod X plugins.ini
        val seenAmxx = mutableSetOf<String>()
        amxxPluginsIni.lines().forEachIndexed { idx, raw ->
            val lineNum = idx + 1
            val trimmed = raw.trim()
            if (trimmed.isEmpty() || trimmed.startsWith(";") || trimmed.startsWith("//")) return@forEachIndexed
            val token = trimmed.split(Regex("\\s+")).firstOrNull().orEmpty()
            if (token.endsWith(".dll", true) || token.endsWith(".so", true)) {
                issues.add(
                    ConfigIssue(
                        "addons/amxmodx/configs/plugins.ini",
                        lineNum,
                        DiagnosticSeverity.ERROR,
                        "'$token' es un módulo nativo de Metamod (.dll/.so) y NO debe colocarse en el plugins.ini de AMX Mod X."
                    )
                )
            } else if (!token.endsWith(".amxx", true)) {
                issues.add(
                    ConfigIssue(
                        "addons/amxmodx/configs/plugins.ini",
                        lineNum,
                        DiagnosticSeverity.WARNING,
                        "El plugin '$token' no termina en '.amxx'. Verifica el nombre del binario."
                    )
                )
            }
            if (!seenAmxx.add(token.lowercase())) {
                issues.add(
                    ConfigIssue(
                        "addons/amxmodx/configs/plugins.ini",
                        lineNum,
                        DiagnosticSeverity.WARNING,
                        "Plugin duplicado '$token' en plugins.ini."
                    )
                )
            }
        }

        // 2. Validate Metamod plugins.ini
        var hasAmxModXMetamodEntry = false
        metamodPluginsIni.lines().forEachIndexed { idx, raw ->
            val lineNum = idx + 1
            val trimmed = raw.trim()
            if (trimmed.isEmpty() || trimmed.startsWith(";") || trimmed.startsWith("//")) return@forEachIndexed
            val parts = trimmed.split(Regex("\\s+"))
            if (parts.size < 2) {
                issues.add(
                    ConfigIssue(
                        "addons/metamod/plugins.ini",
                        lineNum,
                        DiagnosticSeverity.ERROR,
                        "Sintaxis inválida. Formato esperado: 'linux addons/...' o 'win32 addons\\...'."
                    )
                )
                return@forEachIndexed
            }
            val osType = parts[0].lowercase()
            val libPath = parts[1]
            if (osType != "linux" && osType != "win32") {
                issues.add(
                    ConfigIssue(
                        "addons/metamod/plugins.ini",
                        lineNum,
                        DiagnosticSeverity.ERROR,
                        "Plataforma desconocida '$osType'. Metamod solo acepta 'linux' o 'win32'."
                    )
                )
            }
            if (libPath.endsWith(".amxx", true)) {
                issues.add(
                    ConfigIssue(
                        "addons/metamod/plugins.ini",
                        lineNum,
                        DiagnosticSeverity.ERROR,
                        "'$libPath' es un plugin AMX Mod X (.amxx), no una librería nativa Metamod."
                    )
                )
            }
            if (osType == "linux" && !libPath.endsWith(".so", true)) {
                issues.add(
                    ConfigIssue(
                        "addons/metamod/plugins.ini",
                        lineNum,
                        DiagnosticSeverity.ERROR,
                        "Entrada 'linux' debe apuntar a un archivo .so (actual: $libPath)."
                    )
                )
            }
            if (osType == "win32" && !libPath.endsWith(".dll", true)) {
                issues.add(
                    ConfigIssue(
                        "addons/metamod/plugins.ini",
                        lineNum,
                        DiagnosticSeverity.ERROR,
                        "Entrada 'win32' debe apuntar a un archivo .dll (actual: $libPath)."
                    )
                )
            }
            if (isLinuxTarget && osType == "win32") {
                issues.add(
                    ConfigIssue(
                        "addons/metamod/plugins.ini",
                        lineNum,
                        DiagnosticSeverity.WARNING,
                        "Servidor objetivo es Linux, pero esta entrada está configurada para 'win32'."
                    )
                )
            } else if (!isLinuxTarget && osType == "linux") {
                issues.add(
                    ConfigIssue(
                        "addons/metamod/plugins.ini",
                        lineNum,
                        DiagnosticSeverity.WARNING,
                        "Servidor objetivo es Windows, pero esta entrada está configurada para 'linux'."
                    )
                )
            }
            if (libPath.contains("amxmodx_mm", ignoreCase = true)) {
                hasAmxModXMetamodEntry = true
            }
        }

        if (!hasAmxModXMetamodEntry) {
            issues.add(
                ConfigIssue(
                    "addons/metamod/plugins.ini",
                    1,
                    DiagnosticSeverity.WARNING,
                    "No se detectó 'amxmodx_mm' activo en Metamod. AMX Mod X no cargará sin Metamod."
                )
            )
        }

        // 3. Cross-check modules.ini against #include in project .sma files
        val activeModules = modulesIni.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith(";") && !it.startsWith("//") }
            .map { it.lowercase() }
            .toSet()

        val combinedSma = smaSources.joinToString("\n")
        val requiredFromIncludes = listOf("cstrike", "fun", "fakemeta", "hamsandwich", "engine", "nvault", "csx")
        requiredFromIncludes.forEach { mod ->
            if (combinedSma.contains("<$mod>") && mod !in activeModules) {
                issues.add(
                    ConfigIssue(
                        "addons/amxmodx/configs/modules.ini",
                        1,
                        DiagnosticSeverity.INFO,
                        "Tus archivos .sma usan #include <$mod>. Verifica que '$mod' esté habilitado o auto-cargado en modules.ini."
                    )
                )
            }
        }

        modulesIni.lines().forEachIndexed { idx, raw ->
            val trimmed = raw.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith(";") && !trimmed.startsWith("//")) {
                val modName = trimmed.split(Regex("\\s+")).firstOrNull()?.lowercase().orEmpty()
                if (modName.isNotEmpty() && modName !in knownModules && !modName.endsWith("_amxx")) {
                    issues.add(
                        ConfigIssue(
                            "addons/amxmodx/configs/modules.ini",
                            idx + 1,
                            DiagnosticSeverity.INFO,
                            "Módulo personalizado '$modName' detectado; confirma que exista en addons/amxmodx/modules/."
                        )
                    )
                }
            }
        }

        return issues
    }
}
