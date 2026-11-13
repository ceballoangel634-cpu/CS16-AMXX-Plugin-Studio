package com.example.domain

enum class DocCategory(val label: String) {
    NATIVES("Natives"),
    FORWARDS("Forwards"),
    INCLUDES("Includes"),
    EXAMPLES("Ejemplos"),
    TUTORIALS("Tutoriales")
}

data class AmxxDocEntry(
    val id: String,
    val title: String,
    val category: DocCategory,
    val includeFile: String,
    val signature: String,
    val summary: String,
    val exampleSnippet: String
)

object AmxxDocumentation {

    val entries: List<AmxxDocEntry> = listOf(
        // NATIVES
        AmxxDocEntry(
            id = "register_plugin",
            title = "register_plugin",
            category = DocCategory.NATIVES,
            includeFile = "amxmodx.inc",
            signature = "register_plugin(const plugin_name[], const version[], const author[])",
            summary = "Registra el nombre, versión y autor del plugin dentro de AMX Mod X. Debe llamarse dentro de plugin_init().",
            exampleSnippet = """register_plugin("Mi Plugin", "1.0", "Autor");"""
        ),
        AmxxDocEntry(
            id = "register_clcmd",
            title = "register_clcmd",
            category = DocCategory.NATIVES,
            includeFile = "amxmodx.inc",
            signature = "register_clcmd(const client_cmd[], const function[], flags = -1, const info[] = \"\")",
            summary = "Vincula un comando de consola o chat del cliente (por ejemplo 'say /menu') a una función public.",
            exampleSnippet = """register_clcmd("say /menu", "CmdOpenMenu", ADMIN_ALL, "Abre el menú");"""
        ),
        AmxxDocEntry(
            id = "set_hudmessage",
            title = "set_hudmessage / ShowSyncHudMsg",
            category = DocCategory.NATIVES,
            includeFile = "amxmodx.inc",
            signature = "set_hudmessage(red, green, blue, Float:x, Float:y, effects, Float:fxtime, Float:holdtime, Float:fadeintime, Float:fadeouttime, channel)",
            summary = "Configura color RGB, posición normalizada (0.0 a 1.0, -1.0 centrado) y duración del texto HUD en pantalla.",
            exampleSnippet = """set_hudmessage(0, 180, 255, -1.0, 0.25, 0, 0.5, 4.0, 0.1, 0.2, -1);
ShowSyncHudMsg(id, g_HudSync, "Mensaje HUD activo");"""
        ),
        AmxxDocEntry(
            id = "menu_create",
            title = "menu_create / menu_additem / menu_display",
            category = DocCategory.NATIVES,
            includeFile = "newmenus.inc",
            signature = "menu_create(const title[], const handler[], bool:ml = false)",
            summary = "API moderna de menús paginados de AMX Mod X. Recuerda destruir el menú con menu_destroy(menu) en el handler.",
            exampleSnippet = """new menu = menu_create("\yTítulo del Menú", "MenuHandler");
menu_additem(menu, "Opción 1", "1");
menu_display(id, menu, 0);"""
        ),
        AmxxDocEntry(
            id = "cs_set_user_money",
            title = "cs_set_user_money / cs_get_user_money",
            category = DocCategory.NATIVES,
            includeFile = "cstrike.inc",
            signature = "cs_set_user_money(index, money, flash = 1)",
            summary = "Lee o modifica el dinero del jugador en Counter-Strike 1.6 (rango estándar 0..16000).",
            exampleSnippet = """new current = cs_get_user_money(id);
cs_set_user_money(id, min(16000, current + 1000), 1);"""
        ),
        AmxxDocEntry(
            id = "give_item",
            title = "give_item / set_user_health",
            category = DocCategory.NATIVES,
            includeFile = "fun.inc",
            signature = "give_item(index, const item[])",
            summary = "Entrega armas, granadas o ítems ('weapon_ak47', 'item_kevlar') a un jugador vivo.",
            exampleSnippet = """if (is_user_alive(id)) {
    give_item(id, "weapon_ak47");
    set_user_health(id, 120);
}"""
        ),
        AmxxDocEntry(
            id = "RegisterHam",
            title = "RegisterHam / ExecuteHamB",
            category = DocCategory.NATIVES,
            includeFile = "hamsandwich.inc",
            signature = "RegisterHam(Ham:function, const EntityClass[], const Callback[], Post = 0)",
            summary = "Intercepta funciones virtuales de entidades HL1 (Spawn, TakeDamage, Killed) con alto rendimiento.",
            exampleSnippet = """RegisterHam(Ham_Spawn, "player", "FwPlayerSpawn", 1);"""
        ),
        AmxxDocEntry(
            id = "set_task",
            title = "set_task / remove_task",
            category = DocCategory.NATIVES,
            includeFile = "amxmodx.inc",
            signature = "set_task(Float:time, const function[], id = 0, const any:parameter[] = \"\", len = 0, const flags[] = \"\", repeat = 0)",
            summary = "Programa una tarea diferida o repetitiva (flag \"b\" para bucle infinito).",
            exampleSnippet = """set_task(5.0, "TaskAnuncio", id + 100, _, _, "b");"""
        ),
        // FORWARDS
        AmxxDocEntry(
            id = "plugin_init",
            title = "plugin_init()",
            category = DocCategory.FORWARDS,
            includeFile = "amxmodx.inc",
            signature = "public plugin_init()",
            summary = "Se ejecuta al iniciar el mapa. Aquí registras comandos, eventos, cvars y hooks Ham/Fakemeta.",
            exampleSnippet = """public plugin_init() {
    register_plugin("Demo", "1.0", "Autor");
}"""
        ),
        AmxxDocEntry(
            id = "plugin_precache",
            title = "plugin_precache()",
            category = DocCategory.FORWARDS,
            includeFile = "amxmodx.inc",
            signature = "public plugin_precache()",
            summary = "Se ejecuta antes de plugin_init(). Obligatorio para precargar modelos (.mdl), sonidos (.wav) y sprites (.spr).",
            exampleSnippet = """public plugin_precache() {
    precache_sound("misc/sonido.wav");
}"""
        ),
        AmxxDocEntry(
            id = "client_putinserver",
            title = "client_putinserver(id) / client_disconnected(id)",
            category = DocCategory.FORWARDS,
            includeFile = "amxmodx.inc",
            signature = "public client_putinserver(id)",
            summary = "Se dispara cuando el jugador entra al servidor (ideal para cargar nVault) y al salir (para limpiar arrays y tareas).",
            exampleSnippet = """public client_disconnected(id) {
    remove_task(id + 100);
}"""
        ),
        // INCLUDES
        AmxxDocEntry(
            id = "inc_amxmodx",
            title = "<amxmodx> y <amxmisc>",
            category = DocCategory.INCLUDES,
            includeFile = "amxmodx.inc / amxmisc.inc",
            signature = "#include <amxmodx>\n#include <amxmisc>",
            summary = "Núcleo obligatorio de todo plugin AMXX. amxmisc añade cmd_access(), cmd_target() y utilidades de admin.",
            exampleSnippet = """#include <amxmodx>
#include <amxmisc>"""
        ),
        AmxxDocEntry(
            id = "inc_cstrike",
            title = "<cstrike> y <fun>",
            category = DocCategory.INCLUDES,
            includeFile = "cstrike.inc / fun.inc",
            signature = "#include <cstrike>\n#include <fun>",
            summary = "Proporciona control específico de CS 1.6 (dinero, equipos, modelos, bpammo) y utilidades de jugador (vida, armas, gravedad).",
            exampleSnippet = """#include <cstrike>
#include <fun>"""
        ),
        AmxxDocEntry(
            id = "inc_hamsandwich",
            title = "<hamsandwich> y <fakemeta>",
            category = DocCategory.INCLUDES,
            includeFile = "hamsandwich.inc / fakemeta.inc",
            signature = "#include <hamsandwich>\n#include <fakemeta>",
            summary = "Hooks avanzados del motor GoldSrc y manipulación directa de entidades (pev / set_pev / engfunc).",
            exampleSnippet = """#include <hamsandwich>
#include <fakemeta>"""
        ),
        // EXAMPLES
        AmxxDocEntry(
            id = "ex_damage",
            title = "Multiplicador de daño con Ham_TakeDamage",
            category = DocCategory.EXAMPLES,
            includeFile = "hamsandwich.inc",
            signature = "RegisterHam(Ham_TakeDamage, \"player\", \"FwTakeDamage\")",
            summary = "Ejemplo limpio para multiplicar el daño de un jugador VIP por 1.5x.",
            exampleSnippet = """public plugin_init() {
    RegisterHam(Ham_TakeDamage, "player", "FwTakeDamage");
}
public FwTakeDamage(victim, inflictor, attacker, Float:damage, damagebits) {
    if (is_user_connected(attacker) && (get_user_flags(attacker) & ADMIN_LEVEL_H)) {
        SetHamParamFloat(4, damage * 1.5);
        return HAM_HANDLED;
    }
    return HAM_IGNORED;
}"""
        ),
        // TUTORIALS
        AmxxDocEntry(
            id = "tut_estructura",
            title = "Tutorial 1: Estructura básica de un plugin .sma",
            category = DocCategory.TUTORIALS,
            includeFile = "Guía Principiantes",
            signature = "Includes -> Variables globales -> plugin_init() -> Callbacks",
            summary = "1) Declara #include <amxmodx>. 2) Define tus arrays globales tamaño [33] (1..32 para jugadores). 3) Registra comandos/eventos en plugin_init(). 4) Valida siempre is_user_connected(id) o is_user_alive(id).",
            exampleSnippet = """#include <amxmodx>
public plugin_init() {
    register_plugin("Hola CS 1.6", "1.0", "TuNombre");
    register_clcmd("say /hola", "CmdHola");
}
public CmdHola(id) {
    client_print(id, print_chat, "¡Hola desde Pawn!");
    return PLUGIN_HANDLED;
}"""
        ),
        AmxxDocEntry(
            id = "tut_compilar_instalar",
            title = "Tutorial 2: De .sma a .amxx e instalación en cstrike",
            category = DocCategory.TUTORIALS,
            includeFile = "Guía Servidor",
            signature = "scripting/*.sma -> plugins/*.amxx -> configs/plugins.ini",
            summary = "1) Guarda tu fuente en addons/amxmodx/scripting/. 2) Compila con amxxpc a .amxx. 3) Copia el .amxx a addons/amxmodx/plugins/. 4) Añade el nombre exacto 'mi_plugin.amxx' al final de addons/amxmodx/configs/plugins.ini.",
            exampleSnippet = """; En addons/amxmodx/configs/plugins.ini
mi_plugin.amxx"""
        )
    )

    val autocompleteTokens: List<Pair<String, String>> = listOf(
        "register_plugin(" to "register_plugin(\"Nombre\", \"1.0\", \"Autor\");",
        "register_clcmd(" to "register_clcmd(\"say /cmd\", \"CmdHandler\");",
        "register_concmd(" to "register_concmd(\"amx_cmd\", \"CmdAdmin\", ADMIN_KICK, \"Descripción\");",
        "register_cvar(" to "register_cvar(\"cvar_nombre\", \"1\");",
        "RegisterHam(" to "RegisterHam(Ham_Spawn, \"player\", \"FwPlayerSpawn\", 1);",
        "is_user_alive(" to "is_user_alive(id)",
        "is_user_connected(" to "is_user_connected(id)",
        "get_user_flags(" to "get_user_flags(id)",
        "get_user_name(" to "get_user_name(id, name, charsmax(name));",
        "client_print(" to "client_print(id, print_chat, \"Mensaje\");",
        "set_hudmessage(" to "set_hudmessage(0, 180, 255, -1.0, 0.2, 0, 0.5, 4.0, 0.1, 0.2, -1);",
        "menu_create(" to "menu_create(\"\\yTítulo\", \"MenuHandler\");",
        "menu_additem(" to "menu_additem(menu, \"Opción\", \"1\");",
        "menu_display(" to "menu_display(id, menu, 0);",
        "cs_set_user_money(" to "cs_set_user_money(id, cs_get_user_money(id) + 500);",
        "give_item(" to "give_item(id, \"weapon_ak47\");",
        "set_user_health(" to "set_user_health(id, 100);",
        "set_task(" to "set_task(1.0, \"TaskCallback\", id);",
        "#include <amxmodx>" to "#include <amxmodx>",
        "#include <cstrike>" to "#include <cstrike>",
        "#include <hamsandwich>" to "#include <hamsandwich>",
        "#pragma semicolon 1" to "#pragma semicolon 1"
    )
}
