package com.example.domain

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

enum class CompilerExecutionState(val label: String) {
    IDLE("Esperando compilación"),
    CONNECTING("Conectando al servidor"),
    COMPILING("Compilando con amxxpc"),
    SUCCESS("Compilación exitosa"),
    COMPILE_ERROR("Error de compilación"),
    SERVER_UNAVAILABLE("Servidor no disponible")
}

enum class ServerHealthStatus(val label: String) {
    NOT_CONFIGURED("Sin servidor configurado"),
    CHECKING("Comprobando servidor..."),
    ONLINE("Servidor en línea"),
    OFFLINE("Servidor no disponible")
}

data class ServerHealthResult(
    val status: ServerHealthStatus,
    val message: String,
    val supportedVersions: List<String> = emptyList()
)

data class CustomIncludePayload(
    val name: String,
    val content: String
)

data class PlatformArchitectureInfo(
    val osName: String = "Android",
    val androidVersion: String = Build.VERSION.RELEASE ?: "Desconocida",
    val primaryAbi: String = Build.SUPPORTED_ABIS.firstOrNull() ?: "Desconocida",
    val supportedAbis: List<String> = Build.SUPPORTED_ABIS.toList(),
    val isDesktopBinaryCompatible: Boolean = Build.SUPPORTED_ABIS.any {
        it.equals("x86", ignoreCase = true) || it.equals("x86_64", ignoreCase = true)
    },
    val hasLocalCompilerBinary: Boolean = false,
    val localCompilerPath: String? = null
)

data class StaticAnalysisReport(
    val fileName: String,
    val diagnostics: List<PawnDiagnostic>,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class CompilationResult(
    val fileName: String,
    val compilerVersion: String,
    val executionState: CompilerExecutionState,
    val modeUsed: String,
    val succeeded: Boolean,
    val errorCount: Int,
    val warningCount: Int,
    val durationMs: Long,
    val diagnostics: List<PawnDiagnostic>,
    val consoleOutput: String,
    val amxxBytes: ByteArray? = null,
    val outputFileName: String = fileName.substringBeforeLast('.') + ".amxx"
)

class CompilerService(private val context: Context) {

    companion object {
        const val MAX_SOURCE_BYTES = 512 * 1024
        private val SAFE_SMA_REGEX = Regex("^[a-zA-Z0-9_.-]+\\.sma$")
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    fun inspectArchitecture(): PlatformArchitectureInfo {
        val localBin = File(context.filesDir, "bin/amxxpc")
        val localReady = localBin.exists() && localBin.canExecute()
        return PlatformArchitectureInfo(
            hasLocalCompilerBinary = localReady,
            localCompilerPath = if (localBin.exists()) localBin.absolutePath else null
        )
    }

    fun runStaticAnalysis(fileName: String, sourceCode: String): StaticAnalysisReport {
        val diags = PawnStaticAnalyzer.analyze(sourceCode)
        return StaticAnalysisReport(
            fileName = fileName,
            diagnostics = diags,
            errorCount = diags.count { it.severity == DiagnosticSeverity.ERROR },
            warningCount = diags.count { it.severity == DiagnosticSeverity.WARNING },
            infoCount = diags.count { it.severity == DiagnosticSeverity.INFO }
        )
    }

    suspend fun checkServerHealth(serverUrl: String): ServerHealthResult = withContext(Dispatchers.IO) {
        val base = normalizeBaseUrl(serverUrl)
        if (base.isEmpty()) {
            return@withContext ServerHealthResult(
                status = ServerHealthStatus.NOT_CONFIGURED,
                message = "Configura la URL del servidor compilador amxxpc."
            )
        }
        return@withContext try {
            val request = Request.Builder().url("$base/health").get().build()
            httpClient.newCall(request).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    return@withContext ServerHealthResult(
                        status = ServerHealthStatus.OFFLINE,
                        message = "El servidor respondió HTTP ${res.code}"
                    )
                }
                val json = runCatching { JSONObject(body) }.getOrNull()
                val versions = mutableListOf<String>()
                val avail = json?.optJSONObject("availableCompilers")
                if (avail != null) {
                    avail.keys().forEach { key ->
                        if (avail.optBoolean(key, false)) versions.add(key)
                    }
                }
                ServerHealthResult(
                    status = ServerHealthStatus.ONLINE,
                    message = if (versions.isNotEmpty()) {
                        "Conectado • amxxpc disponible (${versions.joinToString(", ")})"
                    } else {
                        "Conectado al servicio compilador"
                    },
                    supportedVersions = versions
                )
            }
        } catch (e: Exception) {
            ServerHealthResult(
                status = ServerHealthStatus.OFFLINE,
                message = "No accesible: ${e.localizedMessage ?: e.javaClass.simpleName}"
            )
        }
    }

    suspend fun compileWithRealCompiler(
        fileName: String,
        sourceCode: String,
        compilerVersion: String,
        serverUrl: String,
        customIncludes: List<CustomIncludePayload>,
        onProgress: (CompilerExecutionState, Float) -> Unit
    ): CompilationResult = withContext(Dispatchers.IO) {
        val safeName = fileName.substringAfterLast('/').substringAfterLast('\\').trim()
        if (!SAFE_SMA_REGEX.matches(safeName) || safeName.contains("..")) {
            return@withContext CompilationResult(
                fileName = fileName,
                compilerVersion = compilerVersion,
                executionState = CompilerExecutionState.COMPILE_ERROR,
                modeUsed = "Validación de seguridad",
                succeeded = false,
                errorCount = 1,
                warningCount = 0,
                durationMs = 0L,
                diagnostics = listOf(
                    PawnDiagnostic(1, 1, DiagnosticSeverity.ERROR, "SEC01", "Nombre de archivo inválido: '$fileName'. Debe terminar en .sma.")
                ),
                consoleOutput = "Validación rechazada: nombre de archivo inválido '$fileName'."
            )
        }

        if (sourceCode.toByteArray(Charsets.UTF_8).size > MAX_SOURCE_BYTES) {
            return@withContext CompilationResult(
                fileName = safeName,
                compilerVersion = compilerVersion,
                executionState = CompilerExecutionState.COMPILE_ERROR,
                modeUsed = "Validación de tamaño",
                succeeded = false,
                errorCount = 1,
                warningCount = 0,
                durationMs = 0L,
                diagnostics = listOf(
                    PawnDiagnostic(1, 1, DiagnosticSeverity.ERROR, "SEC02", "El código supera el límite de 512 KB.")
                ),
                consoleOutput = "El archivo excede el tamaño máximo permitido (512 KB)."
            )
        }

        val arch = inspectArchitecture()
        val baseUrl = normalizeBaseUrl(serverUrl)

        // 1. Remote amxxpc server compilation (Standard path on Android ARM64 & PC)
        if (baseUrl.isNotEmpty()) {
            return@withContext executeRemoteCompilation(
                baseUrl = baseUrl,
                fileName = safeName,
                sourceCode = sourceCode,
                compilerVersion = compilerVersion,
                customIncludes = customIncludes,
                onProgress = onProgress
            )
        }

        // 2. Local compatible amxxpc binary if present (e.g. desktop / x86 environment)
        if (arch.hasLocalCompilerBinary && arch.localCompilerPath != null) {
            onProgress(CompilerExecutionState.COMPILING, 0.55f)
            return@withContext executeLocalAmxxpc(
                binaryPath = arch.localCompilerPath,
                fileName = safeName,
                sourceCode = sourceCode,
                compilerVersion = compilerVersion,
                customIncludes = customIncludes
            )
        }

        // 3. Honest unavailable state: NEVER simulate compilation or fake an .amxx binary
        onProgress(CompilerExecutionState.SERVER_UNAVAILABLE, 0f)
        CompilationResult(
            fileName = safeName,
            compilerVersion = compilerVersion,
            executionState = CompilerExecutionState.SERVER_UNAVAILABLE,
            modeUsed = "Compilador real no configurado",
            succeeded = false,
            errorCount = 0,
            warningCount = 0,
            durationMs = 0L,
            diagnostics = emptyList(),
            consoleOutput = buildString {
                appendLine("Servidor compilador amxxpc no configurado.")
                appendLine("Dispositivo: Android ${arch.androidVersion} (${arch.primaryAbi}).")
                appendLine("Este IDE no simula compilaciones ni genera archivos .amxx falsos.")
                appendLine("")
                appendLine("Para compilar archivos .amxx reales:")
                appendLine("1) Ejecuta el servidor incluido en /compiler-server (Docker o Node.js + amxxpc).")
                appendLine("2) Introduce la URL del servidor arriba (ej. http://192.168.1.50:8080).")
                appendLine("3) Pulsa 'COMPILAR SMA → AMXX'.")
            },
            amxxBytes = null
        )
    }

    private fun executeRemoteCompilation(
        baseUrl: String,
        fileName: String,
        sourceCode: String,
        compilerVersion: String,
        customIncludes: List<CustomIncludePayload>,
        onProgress: (CompilerExecutionState, Float) -> Unit
    ): CompilationResult {
        val targetUrl = "$baseUrl/compile"
        val startedAt = System.currentTimeMillis()
        onProgress(CompilerExecutionState.CONNECTING, 0.25f)

        return try {
            val incArray = JSONArray().apply {
                customIncludes.take(25).forEach { inc ->
                    put(
                        JSONObject().apply {
                            put("name", inc.name)
                            put("content", inc.content)
                        }
                    )
                }
            }
            val payload = JSONObject().apply {
                put("filename", fileName)
                put("compiler", compilerVersion)
                put("source", sourceCode)
                put("customIncludes", incArray)
            }.toString()

            val request = Request.Builder()
                .url(targetUrl)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            onProgress(CompilerExecutionState.COMPILING, 0.65f)

            httpClient.newCall(request).execute().use { response ->
                val rawBodyBytes = response.body?.bytes() ?: ByteArray(0)
                val bodyText = rawBodyBytes.toString(Charsets.UTF_8)
                val totalDuration = System.currentTimeMillis() - startedAt

                if (!response.isSuccessful) {
                    val jsonErr = runCatching { JSONObject(bodyText) }.getOrNull()
                    val serverOutput = jsonErr?.optString("output")?.ifBlank { null } ?: bodyText
                    val parsed = parseAmxxpcOutput(serverOutput)
                    val errCount = parsed.count { it.severity == DiagnosticSeverity.ERROR }
                    val warnCount = parsed.count { it.severity == DiagnosticSeverity.WARNING }
                    val state = if (response.code >= 500) {
                        CompilerExecutionState.SERVER_UNAVAILABLE
                    } else {
                        CompilerExecutionState.COMPILE_ERROR
                    }
                    return CompilationResult(
                        fileName = fileName,
                        compilerVersion = compilerVersion,
                        executionState = state,
                        modeUsed = "amxxpc remoto ($baseUrl)",
                        succeeded = false,
                        errorCount = errCount,
                        warningCount = warnCount,
                        durationMs = totalDuration,
                        diagnostics = parsed,
                        consoleOutput = "HTTP ${response.code}:\n$serverOutput"
                    )
                }

                val json = JSONObject(bodyText)
                val success = json.optBoolean("success", false)
                val output = json.optString("output", "")
                val serverDuration = json.optLong("durationMs", totalDuration)
                val b64 = json.optString("amxxBase64", "")
                val binary = if (b64.isNotBlank()) {
                    android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                } else null
                val parsed = parseAmxxpcOutput(output)
                val errCount = maxOf(
                    json.optInt("errorCount", 0),
                    parsed.count { it.severity == DiagnosticSeverity.ERROR }
                )
                val warnCount = maxOf(
                    json.optInt("warningCount", 0),
                    parsed.count { it.severity == DiagnosticSeverity.WARNING }
                )
                val finalSuccess = success && binary != null && binary.isNotEmpty() && errCount == 0

                CompilationResult(
                    fileName = fileName,
                    compilerVersion = json.optString("compilerVersion", compilerVersion),
                    executionState = if (finalSuccess) CompilerExecutionState.SUCCESS else CompilerExecutionState.COMPILE_ERROR,
                    modeUsed = "amxxpc oficial remoto",
                    succeeded = finalSuccess,
                    errorCount = errCount,
                    warningCount = warnCount,
                    durationMs = serverDuration,
                    diagnostics = parsed,
                    consoleOutput = output.ifBlank { if (finalSuccess) "Done." else "Compilación fallida." },
                    amxxBytes = if (finalSuccess) binary else null,
                    outputFileName = json.optString("outputFilename", fileName.substringBeforeLast('.') + ".amxx")
                )
            }
        } catch (e: Exception) {
            val totalDuration = System.currentTimeMillis() - startedAt
            CompilationResult(
                fileName = fileName,
                compilerVersion = compilerVersion,
                executionState = CompilerExecutionState.SERVER_UNAVAILABLE,
                modeUsed = "amxxpc remoto ($baseUrl)",
                succeeded = false,
                errorCount = 0,
                warningCount = 0,
                durationMs = totalDuration,
                diagnostics = emptyList(),
                consoleOutput = "No se pudo conectar con el servidor compilador ($targetUrl):\n" +
                    "${e.localizedMessage ?: e.javaClass.simpleName}\n\n" +
                    "Verifica que el servidor Docker/Node.js esté ejecutándose y accesible desde este dispositivo."
            )
        }
    }

    private fun executeLocalAmxxpc(
        binaryPath: String,
        fileName: String,
        sourceCode: String,
        compilerVersion: String,
        customIncludes: List<CustomIncludePayload>
    ): CompilationResult {
        val workDir = File(context.cacheDir, "amxx_compile").apply { mkdirs() }
        val incDir = File(workDir, "include").apply { mkdirs() }
        val inputSma = File(workDir, fileName)
        val outputAmxx = File(workDir, fileName.substringBeforeLast('.') + ".amxx")
        val startedAt = System.currentTimeMillis()
        return try {
            customIncludes.forEach { File(incDir, it.name).writeText(it.content, Charsets.UTF_8) }
            inputSma.writeText(sourceCode, Charsets.UTF_8)
            val process = ProcessBuilder(
                binaryPath,
                inputSma.absolutePath,
                "-o${outputAmxx.absolutePath}",
                "-i${incDir.absolutePath}"
            )
                .directory(workDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            val duration = System.currentTimeMillis() - startedAt
            val parsed = parseAmxxpcOutput(output)
            val errCount = parsed.count { it.severity == DiagnosticSeverity.ERROR }
            val warnCount = parsed.count { it.severity == DiagnosticSeverity.WARNING }
            val bytes = if (exitCode == 0 && outputAmxx.exists() && errCount == 0) outputAmxx.readBytes() else null
            val ok = bytes != null && bytes.isNotEmpty()
            CompilationResult(
                fileName = fileName,
                compilerVersion = compilerVersion,
                executionState = if (ok) CompilerExecutionState.SUCCESS else CompilerExecutionState.COMPILE_ERROR,
                modeUsed = "Binario local amxxpc",
                succeeded = ok,
                errorCount = errCount,
                warningCount = warnCount,
                durationMs = duration,
                diagnostics = parsed,
                consoleOutput = output,
                amxxBytes = bytes
            )
        } catch (e: Exception) {
            CompilationResult(
                fileName = fileName,
                compilerVersion = compilerVersion,
                executionState = CompilerExecutionState.SERVER_UNAVAILABLE,
                modeUsed = "Binario local amxxpc",
                succeeded = false,
                errorCount = 0,
                warningCount = 0,
                durationMs = System.currentTimeMillis() - startedAt,
                diagnostics = emptyList(),
                consoleOutput = "Error al ejecutar binario local $binaryPath: ${e.localizedMessage}"
            )
        } finally {
            workDir.deleteRecursively()
        }
    }

    fun persistCompiledBinary(projectId: Long, fileName: String, bytes: ByteArray) {
        val dir = File(context.filesDir, "compiled_amxx/$projectId").apply { mkdirs() }
        File(dir, fileName).writeBytes(bytes)
    }

    fun loadPersistedBinaries(projectId: Long): Map<String, ByteArray> {
        val dir = File(context.filesDir, "compiled_amxx/$projectId")
        if (!dir.exists()) return emptyMap()
        return dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".amxx", true) }
            ?.associate { it.name to it.readBytes() }
            ?: emptyMap()
    }

    private fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        return if (trimmed.endsWith("/compile")) trimmed.removeSuffix("/compile") else trimmed
    }

    fun parseAmxxpcOutput(output: String): List<PawnDiagnostic> {
        val regex = Regex("""^(.+?)\((\d+)\)\s*:\s*(fatal error|error|warning)\s+(\d+)\s*:\s*(.+)$""", RegexOption.IGNORE_CASE)
        val result = mutableListOf<PawnDiagnostic>()
        output.lines().forEach { line ->
            val match = regex.find(line.trim()) ?: return@forEach
            val lineNum = match.groupValues[2].toIntOrNull() ?: 1
            val typeStr = match.groupValues[3].lowercase()
            val code = match.groupValues[4]
            val msg = match.groupValues[5]
            val severity = when {
                typeStr.contains("error") -> DiagnosticSeverity.ERROR
                typeStr.contains("warning") -> DiagnosticSeverity.WARNING
                else -> DiagnosticSeverity.INFO
            }
            result.add(PawnDiagnostic(lineNum, 1, severity, code, msg))
        }
        return result
    }
}
