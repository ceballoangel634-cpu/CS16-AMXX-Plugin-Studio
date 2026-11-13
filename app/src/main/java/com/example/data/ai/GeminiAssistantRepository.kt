package com.example.data.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

enum class AiTaskMode(val title: String, val promptPrefix: String, val autoAttachActiveFile: Boolean) {
    CREATE_FROM_IDEA(
        title = "Crear plugin completo",
        promptPrefix = "Crea un plugin completo, 100% compilable en amxxpc (no fragmentos incompletos) para Counter-Strike 1.6 (AMX Mod X 1.9/1.10) en Pawn (.sma) basado en esta descripción o idea:",
        autoAttachActiveFile = false
    ),
    ANALYZE_SMA(
        title = "Analizar archivo SMA",
        promptPrefix = "Analiza en profundidad el siguiente archivo .sma para CS 1.6 AMX Mod X (posibles errores, fugas de memoria, compatibilidad de módulos y mejoras):",
        autoAttachActiveFile = true
    ),
    FIX_ERRORS(
        title = "Corregir errores",
        promptPrefix = "Corrige todos los errores de sintaxis, compilación y lógica del siguiente código Pawn (.sma) RESPETANDO ÍNTEGRAMENTE todas las funciones, comandos y lógica original del autor, y entrega el archivo .sma completo corregido:",
        autoAttachActiveFile = true
    ),
    CONVERT_LEGACY(
        title = "Convertir código antiguo",
        promptPrefix = "Actualiza y convierte este código Pawn antiguo (AMX Mod / AMXX 1.8.x / client_disconnect / viejos natives) a código moderno 100% compatible con AMX Mod X 1.9.0 / 1.10.0 respetando todas sus funciones originales:",
        autoAttachActiveFile = true
    ),
    EXPLAIN_FUNCTIONS(
        title = "Explicar funciones",
        promptPrefix = "Explica paso a paso cada función, forward, native y cvar del siguiente plugin Pawn de CS 1.6:",
        autoAttachActiveFile = true
    ),
    OPTIMIZE_PLUGIN(
        title = "Optimizar código",
        promptPrefix = "Optimiza el rendimiento de este plugin CS 1.6 (usando pcvars, HamSandwich, Trie/Array y validaciones seguras) manteniendo intactas todas sus funciones originales, y entrega el archivo .sma completo:",
        autoAttachActiveFile = true
    ),
    CREATE_AMXX_MENU(
        title = "Crear menú y comandos",
        promptPrefix = "Genera un plugin completo y compilable con menús AMX Mod X (menu_create, menu_additem, handler con MENU_EXIT) y comandos personalizados para:",
        autoAttachActiveFile = false
    ),
    MODIFY_CODE(
        title = "Modificar código pegado",
        promptPrefix = "Modifica el código Pawn proporcionado aplicando exactamente los cambios solicitados, respetando las funciones originales y devolviendo el archivo .sma completo:",
        autoAttachActiveFile = false
    )
}

class GeminiAssistantRepository {

    private val systemPrompt = content {
        text(
            """
            Eres un ingeniero experto en Counter-Strike 1.6, AMX Mod X (1.8.2, 1.9.0, 1.10.0), Metamod y Pawn (.sma / .inc).
            Responde siempre en español técnico, claro y directo.
            Reglas obligatorias para todo código Pawn que generes:
            1. Entrega SIEMPRE archivos .sma completos y listos para compilar en amxxpc (prohibido entregar fragmentos incompletos o con "// ... resto del código").
            2. Usa includes reales de AMX Mod X (<amxmodx>, <amxmisc>, <cstrike>, <fun>, <fakemeta>, <hamsandwich>, <engine>, <nvault>).
            3. Incluye #pragma semicolon 1, register_plugin(), validaciones is_user_connected() / is_user_alive() y libera recursos (menu_destroy, nvault_close, remove_task).
            4. Cuando corrijas, optimices o actualices un plugin existente, RESPETA TODAS las funciones, comandos y comportamiento original del código proporcionado.
            5. Si se solicitan bots compatibles, usa comprobaciones !is_user_bot() o soporte get_players("ch")/"dh" y amx_bot_quota.
            6. Envuelve el código completo en un bloque ```pawn ... ``` para importarlo con un toque al Editor SMA.
            7. No inventes natives inexistentes de AMX Mod X.
            """.trimIndent()
        )
    }

    private val standardModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.35f
                maxOutputTokens = 6144
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.LOW
                }
            },
            systemInstruction = systemPrompt
        )
    }

    private val fastModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                temperature = 0.35f
                maxOutputTokens = 6144
            },
            systemInstruction = systemPrompt
        )
    }

    fun streamPrompt(prompt: String, useFastModel: Boolean = false): Flow<String> = flow {
        val activeModel = if (useFastModel) fastModel else standardModel
        val builder = StringBuilder()
        withTimeout(120_000L) {
            activeModel.generateContentStream(prompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    builder.append(part)
                    emit(builder.toString())
                }
            }
        }
    }
}
