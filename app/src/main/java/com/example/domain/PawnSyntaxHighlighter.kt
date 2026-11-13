package com.example.domain

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.data.local.StudioFileType
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNative
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxOperator
import com.example.ui.theme.SyntaxPreprocessor
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxTag

object PawnSyntaxHighlighter {

    private val pawnKeywords = setOf(
        "public", "stock", "native", "forward", "new", "const", "static", "enum",
        "if", "else", "for", "while", "do", "switch", "case", "default",
        "return", "break", "continue", "sizeof", "charsmax", "true", "false",
        "operator", "state", "goto"
    )

    private val pawnPreprocessors = setOf(
        "#include", "#define", "#undef", "#pragma", "#if", "#else", "#elseif", "#endif", "#ifdef", "#error"
    )

    private val pawnTags = setOf(
        "Float:", "bool:", "Trie:", "Array:", "Handle:", "Ham:", "CsTeams:", "CsArmorType:", "any:"
    )

    private val amxxNatives = setOf(
        "register_plugin", "register_clcmd", "register_concmd", "register_srvcmd",
        "register_event", "register_logevent", "register_message", "register_cvar",
        "bind_pcvar_num", "bind_pcvar_float", "bind_pcvar_string", "get_pcvar_num", "set_pcvar_num",
        "RegisterHam", "ExecuteHam", "ExecuteHamB", "register_forward", "unregister_forward",
        "set_task", "remove_task", "task_exists",
        "client_print", "client_print_color", "server_print", "console_print", "log_amx",
        "set_hudmessage", "show_hudmessage", "CreateHudSyncObj", "ShowSyncHudMsg",
        "menu_create", "menu_additem", "menu_display", "menu_destroy", "menu_item_getinfo",
        "is_user_connected", "is_user_alive", "is_user_bot", "is_user_admin", "get_user_flags",
        "get_user_name", "get_user_authid", "get_user_ip", "get_user_userid",
        "get_user_health", "set_user_health", "get_user_armor", "set_user_armor",
        "give_item", "strip_user_weapons", "set_user_rendering", "set_user_maxspeed", "set_user_gravity",
        "cs_get_user_money", "cs_set_user_money", "cs_get_user_team", "cs_set_user_team",
        "cs_user_spawn", "get_players", "get_maxplayers",
        "emit_sound", "precache_sound", "precache_model", "engfunc", "pev", "set_pev",
        "message_begin", "write_byte", "write_short", "write_coord", "write_string", "message_end",
        "nvault_open", "nvault_get", "nvault_set", "nvault_close",
        "formatex", "format", "copy", "equal", "equali", "containi", "str_to_num", "num_to_str",
        "read_argv", "read_args", "read_argc", "remove_quotes", "trim", "parse",
        "plugin_init", "plugin_precache", "plugin_cfg", "plugin_end", "client_putinserver", "client_disconnected"
    )

    private val cfgKeywords = setOf(
        "bind", "unbind", "unbindall", "alias", "exec", "echo", "setinfo", "wait",
        "say", "say_team", "amx_cvar", "amx_map", "amx_kick", "amx_ban", "amx_slay",
        "mp_roundtime", "mp_freezetime", "mp_buytime", "mp_startmoney", "mp_friendlyfire",
        "sv_gravity", "sv_alltalk", "sv_restart", "sv_maxrate", "cl_cmdrate", "cl_updaterate", "rate"
    )

    fun highlight(code: String, fileType: StudioFileType): AnnotatedString {
        return when (fileType) {
            StudioFileType.CFG, StudioFileType.INI -> highlightCfgOrIni(code)
            else -> highlightPawn(code)
        }
    }

    fun highlightPawn(code: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            val n = code.length
            var i = 0
            while (i < n) {
                val c = code[i]
                // Single-line comment //
                if (c == '/' && i + 1 < n && code[i + 1] == '/') {
                    val start = i
                    while (i < n && code[i] != '\n') i++
                    addStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), start, i)
                    continue
                }
                // Block comment /* ... */
                if (c == '/' && i + 1 < n && code[i + 1] == '*') {
                    val start = i
                    i += 2
                    while (i + 1 < n && !(code[i] == '*' && code[i + 1] == '/')) i++
                    i = if (i + 1 < n) i + 2 else n
                    addStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), start, i)
                    continue
                }
                // Strings "..." or '...'
                if (c == '"' || c == '\'') {
                    val quote = c
                    val start = i
                    i++
                    while (i < n && code[i] != quote && code[i] != '\n') {
                        if ((code[i] == '\\' || code[i] == '^') && i + 1 < n) i += 2 else i++
                    }
                    if (i < n && code[i] == quote) i++
                    addStyle(SpanStyle(color = SyntaxString), start, i)
                    continue
                }
                // Preprocessor #...
                if (c == '#') {
                    val start = i
                    i++
                    while (i < n && (code[i].isLetterOrDigit() || code[i] == '_')) i++
                    val directive = code.substring(start, i)
                    if (directive in pawnPreprocessors) {
                        addStyle(SpanStyle(color = SyntaxPreprocessor, fontWeight = FontWeight.SemiBold), start, i)
                    } else {
                        addStyle(SpanStyle(color = SyntaxPreprocessor), start, i)
                    }
                    continue
                }
                // Angle bracket include <amxmodx>
                if (c == '<') {
                    val lineStart = code.lastIndexOf('\n', i).let { if (it == -1) 0 else it + 1 }
                    if (code.substring(lineStart, i).trimStart().startsWith("#include")) {
                        val start = i
                        val end = code.indexOf('>', i).let { if (it == -1) n else it + 1 }
                        addStyle(SpanStyle(color = SyntaxString), start, end)
                        i = end
                        continue
                    }
                }
                // Numbers (hex or decimal/float)
                if (c.isDigit()) {
                    val start = i
                    if (c == '0' && i + 1 < n && (code[i + 1] == 'x' || code[i + 1] == 'X')) {
                        i += 2
                        while (i < n && (code[i].isDigit() || code[i] in 'a'..'f' || code[i] in 'A'..'F')) i++
                    } else {
                        while (i < n && (code[i].isDigit() || code[i] == '.')) i++
                    }
                    addStyle(SpanStyle(color = SyntaxNumber), start, i)
                    continue
                }
                // Identifiers, keywords, natives, tags
                if (c.isLetter() || c == '_') {
                    val start = i
                    while (i < n && (code[i].isLetterOrDigit() || code[i] == '_')) i++
                    val word = code.substring(start, i)
                    if (i < n && code[i] == ':' && "$word:" in pawnTags) {
                        addStyle(SpanStyle(color = SyntaxTag, fontWeight = FontWeight.Medium), start, i + 1)
                        i++
                    } else if (word in pawnKeywords) {
                        addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.SemiBold), start, i)
                    } else if (word in amxxNatives) {
                        addStyle(SpanStyle(color = SyntaxNative), start, i)
                    } else if (word.all { it.isUpperCase() || it == '_' || it.isDigit() } && word.length > 2) {
                        addStyle(SpanStyle(color = SyntaxTag), start, i)
                    }
                    continue
                }
                if (c in "{}()[]+-*/%=!<>&|^~") {
                    addStyle(SpanStyle(color = SyntaxOperator), i, i + 1)
                }
                i++
            }
        }
    }

    fun highlightCfgOrIni(code: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            val n = code.length
            var i = 0
            while (i < n) {
                val c = code[i]
                val atLineStart = i == 0 || code[i - 1] == '\n'
                if ((c == '/' && i + 1 < n && code[i + 1] == '/') || (atLineStart && (c == ';' || c == '#'))) {
                    val start = i
                    while (i < n && code[i] != '\n') i++
                    addStyle(SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic), start, i)
                    continue
                }
                if (c == '"') {
                    val start = i
                    i++
                    while (i < n && code[i] != '"' && code[i] != '\n') i++
                    if (i < n && code[i] == '"') i++
                    addStyle(SpanStyle(color = SyntaxString), start, i)
                    continue
                }
                if (c.isDigit()) {
                    val start = i
                    while (i < n && (code[i].isDigit() || code[i] == '.')) i++
                    addStyle(SpanStyle(color = SyntaxNumber), start, i)
                    continue
                }
                if (c.isLetter() || c == '_') {
                    val start = i
                    while (i < n && (code[i].isLetterOrDigit() || code[i] == '_' || code[i] == '.')) i++
                    val word = code.substring(start, i)
                    when {
                        word.lowercase() in cfgKeywords ->
                            addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.SemiBold), start, i)
                        word.lowercase().endsWith(".amxx") ->
                            addStyle(SpanStyle(color = SyntaxNative, fontWeight = FontWeight.Medium), start, i)
                        word.lowercase().endsWith(".dll") || word.lowercase().endsWith(".so") ->
                            addStyle(SpanStyle(color = SyntaxPreprocessor, fontWeight = FontWeight.Medium), start, i)
                        word.startsWith("amx_") || word.startsWith("sv_") || word.startsWith("mp_") || word.startsWith("cl_") ->
                            addStyle(SpanStyle(color = SyntaxTag), start, i)
                    }
                    continue
                }
                i++
            }
        }
    }
}

class CodeSyntaxVisualTransformation(
    private val fileType: StudioFileType
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = PawnSyntaxHighlighter.highlight(text.text, fileType)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
