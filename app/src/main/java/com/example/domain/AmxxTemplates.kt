package com.example.domain

data class PluginTemplate(
    val id: String,
    val title: String,
    val category: String,
    val fileName: String,
    val description: String,
    val includes: List<String>,
    val code: String
)

object AmxxTemplates {

    val allTemplates: List<PluginTemplate> = listOf(
        PluginTemplate(
            id = "admin_menu",
            title = "Menú de Administración",
            category = "Administración",
            fileName = "admin_menu.sma",
            description = "Menú interactivo para admins (ADMIN_KICK) con opciones de Kick, Slay, Slap y cambio de mapa.",
            includes = listOf("amxmodx", "amxmisc"),
            code = """#include <amxmodx>
#include <amxmisc>

#pragma semicolon 1

#define PLUGIN "Admin Quick Menu"
#define VERSION "1.0"
#define AUTHOR "CS 1.6 Plugin Studio"

new g_TargetPlayer[33];

public plugin_init()
{
    register_plugin(PLUGIN, VERSION, AUTHOR);
    register_clcmd("say /adminmenu", "CmdAdminMenu", ADMIN_KICK, "Abre el menú rápido de administración");
}

public CmdAdminMenu(id, level, cid)
{
    if (!cmd_access(id, level, cid, 1))
        return PLUGIN_HANDLED;

    new menu = menu_create("\y[AMXX]\w Menú de Administración", "HandlerAdminMenu");
    menu_additem(menu, "Matar Jugador (Slay)", "1");
    menu_additem(menu, "Golpear Jugador (Slap 10 HP)", "2");
    menu_additem(menu, "Expulsar Jugador (Kick)", "3");
    menu_additem(menu, "Reiniciar Ronda (sv_restart 1)", "4");

    menu_setprop(menu, MPROP_EXITNAME, "Salir");
    menu_display(id, menu, 0);
    return PLUGIN_HANDLED;
}

public HandlerAdminMenu(id, menu, item)
{
    if (item == MENU_EXIT)
    {
        menu_destroy(menu);
        return PLUGIN_HANDLED;
    }

    new data[6], name[32], access, callback;
    menu_item_getinfo(menu, item, access, data, charsmax(data), name, charsmax(name), callback);
    new action = str_to_num(data);
    menu_destroy(menu);

    if (action == 4)
    {
        server_cmd("sv_restart 1");
        client_print(0, print_chat, "[ADMIN] Ronda reiniciada por un administrador.");
        return PLUGIN_HANDLED;
    }

    OpenPlayerSelectMenu(id, action);
    return PLUGIN_HANDLED;
}

stock OpenPlayerSelectMenu(id, action)
{
    new menu = menu_create("\ySelecciona un jugador:", "HandlerPlayerSelect");
    new players[32], num, tempName[32], info[16];
    get_players(players, num, "ch");

    for (new i = 0; i < num; i++)
    {
        new target = players[i];
        get_user_name(target, tempName, charsmax(tempName));
        formatex(info, charsmax(info), "%d %d", action, get_user_userid(target));
        menu_additem(menu, tempName, info);
    }

    menu_display(id, menu, 0);
}

public HandlerPlayerSelect(id, menu, item)
{
    if (item == MENU_EXIT)
    {
        menu_destroy(menu);
        return PLUGIN_HANDLED;
    }

    new info[16], name[32], access, callback;
    menu_item_getinfo(menu, item, access, info, charsmax(info), name, charsmax(name), callback);
    menu_destroy(menu);

    new sAction[6], sUserId[8];
    parse(info, sAction, charsmax(sAction), sUserId, charsmax(sUserId));
    new action = str_to_num(sAction);
    new target = find_player("k", str_to_num(sUserId));

    if (!is_user_connected(target))
        return PLUGIN_HANDLED;

    g_TargetPlayer[id] = target;
    switch (action)
    {
        case 1: if (is_user_alive(target)) user_kill(target);
        case 2: if (is_user_alive(target)) user_slap(target, 10);
        case 3: server_cmd("kick #%d ^"Expulsado por el administrador^"", get_user_userid(target));
    }
    return PLUGIN_HANDLED;
}"""
        ),
        PluginTemplate(
            id = "vip_system",
            title = "Sistema VIP",
            category = "VIP & Beneficios",
            fileName = "vip_system.sma",
            description = "Beneficios VIP por flag ADMIN_LEVEL_H: chaleco + granadas al aparecer, extra HP por kill y tag en chat.",
            includes = listOf("amxmodx", "cstrike", "fun", "hamsandwich"),
            code = """#include <amxmodx>
#include <cstrike>
#include <fun>
#include <hamsandwich>

#pragma semicolon 1

#define VIP_FLAG ADMIN_LEVEL_H

new g_pCvarKillHp;

public plugin_init()
{
    register_plugin("Sistema VIP Pro", "1.0", "CS 1.6 Plugin Studio");
    RegisterHam(Ham_Spawn, "player", "FwPlayerSpawnPost", 1);
    register_event("DeathMsg", "EventDeathMsg", "a");
    g_pCvarKillHp = register_cvar("vip_kill_hp", "15");
}

public FwPlayerSpawnPost(id)
{
    if (!is_user_alive(id) || !(get_user_flags(id) & VIP_FLAG))
        return;

    cs_set_user_armor(id, 100, CS_ARMOR_VESTHELM);
    give_item(id, "weapon_hegrenade");
    give_item(id, "weapon_flashbang");
    give_item(id, "weapon_smokegrenade");
    client_print(id, print_chat, "[VIP] Equipamiento VIP entregado (Kevlar + Granadas).");
}

public EventDeathMsg()
{
    new killer = read_data(1);
    new victim = read_data(2);

    if (!is_user_alive(killer) || killer == victim)
        return;

    if (get_user_flags(killer) & VIP_FLAG)
    {
        new bonus = get_pcvar_num(g_pCvarKillHp);
        set_user_health(killer, min(150, get_user_health(killer) + bonus));
        cs_set_user_money(killer, min(16000, cs_get_user_money(killer) + 300));
    }
}"""
        ),
        PluginTemplate(
            id = "weapon_shop",
            title = "Tienda de Armas",
            category = "Economía & Tienda",
            fileName = "weapon_shop.sma",
            description = "Menú de tienda /shop para comprar AK-47, M4A1, AWP o Deagle usando dinero de CS 1.6.",
            includes = listOf("amxmodx", "cstrike", "fun"),
            code = """#include <amxmodx>
#include <cstrike>
#include <fun>

#pragma semicolon 1

public plugin_init()
{
    register_plugin("Tienda de Armas", "1.0", "CS 1.6 Plugin Studio");
    register_clcmd("say /shop", "CmdOpenShop");
    register_clcmd("say /tienda", "CmdOpenShop");
}

public CmdOpenShop(id)
{
    if (!is_user_alive(id))
    {
        client_print(id, print_chat, "[TIENDA] Debes estar vivo para comprar.");
        return PLUGIN_HANDLED;
    }

    new menu = menu_create("\y[TIENDA CS 1.6]\w Selecciona tu arma:", "HandlerShop");
    menu_additem(menu, "AK-47 \y[$2500]", "1");
    menu_additem(menu, "M4A1 \y[$3100]", "2");
    menu_additem(menu, "AWP \y[$4750]", "3");
    menu_additem(menu, "Desert Eagle \y[$650]", "4");

    menu_setprop(menu, MPROP_EXITNAME, "Cerrar");
    menu_display(id, menu, 0);
    return PLUGIN_HANDLED;
}

public HandlerShop(id, menu, item)
{
    if (item == MENU_EXIT || !is_user_alive(id))
    {
        menu_destroy(menu);
        return PLUGIN_HANDLED;
    }

    new data[4], name[32], access, callback;
    menu_item_getinfo(menu, item, access, data, charsmax(data), name, charsmax(name), callback);
    menu_destroy(menu);

    new money = cs_get_user_money(id);
    switch (str_to_num(data))
    {
        case 1: TryBuyWeapon(id, money, 2500, "weapon_ak47", CSW_AK47, 90);
        case 2: TryBuyWeapon(id, money, 3100, "weapon_m4a1", CSW_M4A1, 90);
        case 3: TryBuyWeapon(id, money, 4750, "weapon_awp", CSW_AWP, 30);
        case 4: TryBuyWeapon(id, money, 650, "weapon_deagle", CSW_DEAGLE, 35);
    }
    return PLUGIN_HANDLED;
}

stock TryBuyWeapon(id, currentMoney, cost, const weapon[], weaponId, bpAmmo)
{
    if (currentMoney < cost)
    {
        client_print(id, print_chat, "[TIENDA] Dinero insuficiente ($%d requeridos).", cost);
        return;
    }
    cs_set_user_money(id, currentMoney - cost);
    give_item(id, weapon);
    cs_set_user_bpammo(id, weaponId, bpAmmo);
    client_print(id, print_chat, "[TIENDA] Compra completada con éxito.");
}"""
        ),
        PluginTemplate(
            id = "xp_levels",
            title = "Niveles y Experiencia (XP)",
            category = "Progresión RPG",
            fileName = "xp_levels.sma",
            description = "Sistema de niveles con guardado persistente en nVault por SteamID y HUD de progreso.",
            includes = listOf("amxmodx", "nvault"),
            code = """#include <amxmodx>
#include <nvault>

#pragma semicolon 1

#define MAX_LEVELS 10

new g_PlayerXp[33];
new g_PlayerLevel[33];
new g_Vault = INVALID_HANDLE;

new const g_XpTable[MAX_LEVELS] = { 0, 100, 250, 500, 900, 1400, 2000, 2800, 3800, 5000 };

public plugin_init()
{
    register_plugin("Sistema XP & Niveles", "1.0", "CS 1.6 Plugin Studio");
    register_event("DeathMsg", "EventDeathMsg", "a");
    register_clcmd("say /nivel", "CmdShowLevel");
    g_Vault = nvault_open("cs16_xp_levels");
}

public plugin_end()
{
    if (g_Vault != INVALID_HANDLE)
        nvault_close(g_Vault);
}

public client_putinserver(id)
{
    LoadPlayerXp(id);
}

public client_disconnected(id)
{
    SavePlayerXp(id);
}

public EventDeathMsg()
{
    new killer = read_data(1);
    new victim = read_data(2);
    new headshot = read_data(3);

    if (!is_user_connected(killer) || killer == victim)
        return;

    new gain = headshot ? 35 : 20;
    g_PlayerXp[killer] += gain;

    while (g_PlayerLevel[killer] < MAX_LEVELS - 1 && g_PlayerXp[killer] >= g_XpTable[g_PlayerLevel[killer] + 1])
    {
        g_PlayerLevel[killer]++;
        client_print(killer, print_chat, "[XP] ¡Subiste al nivel %d!", g_PlayerLevel[killer] + 1);
    }
}

public CmdShowLevel(id)
{
    client_print(id, print_chat, "[XP] Nivel actual: %d | Experiencia: %d", g_PlayerLevel[id] + 1, g_PlayerXp[id]);
    return PLUGIN_HANDLED;
}

stock LoadPlayerXp(id)
{
    if (g_Vault == INVALID_HANDLE) return;
    new authid[35], data[16];
    get_user_authid(id, authid, charsmax(authid));
    if (nvault_get(g_Vault, authid, data, charsmax(data)))
    {
        g_PlayerXp[id] = str_to_num(data);
        g_PlayerLevel[id] = 0;
        while (g_PlayerLevel[id] < MAX_LEVELS - 1 && g_PlayerXp[id] >= g_XpTable[g_PlayerLevel[id] + 1])
            g_PlayerLevel[id]++;
    }
    else
    {
        g_PlayerXp[id] = 0;
        g_PlayerLevel[id] = 0;
    }
}

stock SavePlayerXp(id)
{
    if (g_Vault == INVALID_HANDLE) return;
    new authid[35], data[16];
    get_user_authid(id, authid, charsmax(authid));
    num_to_str(g_PlayerXp[id], data, charsmax(data));
    nvault_set(g_Vault, authid, data);
}"""
        ),
        PluginTemplate(
            id = "bot_controller",
            title = "Controlador de Bots",
            category = "Servidor & Bots",
            fileName = "bot_controller.sma",
            description = "Administra bots automáticamente según jugadores reales conectados (compatible con PODBot / ZBot).",
            includes = listOf("amxmodx", "amxmisc"),
            code = """#include <amxmodx>
#include <amxmisc>

#pragma semicolon 1

new g_pCvarMinPlayers;

public plugin_init()
{
    register_plugin("Auto Bot Manager", "1.0", "CS 1.6 Plugin Studio");
    g_pCvarMinPlayers = register_cvar("amx_bot_quota", "8");
    register_concmd("amx_fillbots", "CmdFillBots", ADMIN_CFG, "Ajusta la cuota de bots");
    set_task(10.0, "TaskBalanceBots", _, _, _, "b");
}

public CmdFillBots(id, level, cid)
{
    if (!cmd_access(id, level, cid, 1))
        return PLUGIN_HANDLED;

    TaskBalanceBots();
    console_print(id, "[BOTS] Cuota verificada correctamente.");
    return PLUGIN_HANDLED;
}

public TaskBalanceBots()
{
    new humans[32], bots[32], humanCount, botCount;
    get_players(humans, humanCount, "ch");
    get_players(bots, botCount, "dh");

    new quota = get_pcvar_num(g_pCvarMinPlayers);
    new total = humanCount + botCount;

    if (total < quota)
    {
        server_cmd("bot_add");
    }
    else if (total > quota && botCount > 0)
    {
        server_cmd("kick #%d", get_user_userid(bots[0]));
    }
}"""
        ),
        PluginTemplate(
            id = "admin_commands",
            title = "Comandos de Administrador",
            category = "Administración",
            fileName = "admin_commands.sma",
            description = "Comandos personalizados amx_heal, amx_armor y amx_speed con validación cmd_access.",
            includes = listOf("amxmodx", "amxmisc", "fun"),
            code = """#include <amxmodx>
#include <amxmisc>
#include <fun>

#pragma semicolon 1

public plugin_init()
{
    register_plugin("Comandos Admin Extra", "1.0", "CS 1.6 Plugin Studio");
    register_concmd("amx_heal", "CmdHeal", ADMIN_SLAY, "<objetivo> <vida> - Cura a un jugador");
    register_concmd("amx_gravity_player", "CmdGravity", ADMIN_SLAY, "<objetivo> <factor 0.5-1.0>");
}

public CmdHeal(id, level, cid)
{
    if (!cmd_access(id, level, cid, 3))
        return PLUGIN_HANDLED;

    new argTarget[32], argAmount[8];
    read_argv(1, argTarget, charsmax(argTarget));
    read_argv(2, argAmount, charsmax(argAmount));

    new target = cmd_target(id, argTarget, CMDTARGET_ALLOW_SELF | CMDTARGET_ONLY_ALIVE);
    if (!target)
        return PLUGIN_HANDLED;

    new hp = clamp(str_to_num(argAmount), 1, 500);
    set_user_health(target, hp);

    new targetName[32];
    get_user_name(target, targetName, charsmax(targetName));
    console_print(id, "[AMXX] Vida de %s establecida en %d HP.", targetName, hp);
    return PLUGIN_HANDLED;
}

public CmdGravity(id, level, cid)
{
    if (!cmd_access(id, level, cid, 3))
        return PLUGIN_HANDLED;

    new argTarget[32], argGrav[8];
    read_argv(1, argTarget, charsmax(argTarget));
    read_argv(2, argGrav, charsmax(argGrav));

    new target = cmd_target(id, argTarget, CMDTARGET_ALLOW_SELF | CMDTARGET_ONLY_ALIVE);
    if (!target)
        return PLUGIN_HANDLED;

    set_user_gravity(target, floatclamp(str_to_float(argGrav), 0.1, 2.0));
    return PLUGIN_HANDLED;
}"""
        ),
        PluginTemplate(
            id = "money_manager",
            title = "Sistema de Dinero",
            category = "Economía & Tienda",
            fileName = "money_manager.sma",
            description = "Transferencia de dinero entre jugadores con /donar y salario automático por ronda.",
            includes = listOf("amxmodx", "cstrike"),
            code = """#include <amxmodx>
#include <cstrike>

#pragma semicolon 1

public plugin_init()
{
    register_plugin("Sistema de Dinero", "1.0", "CS 1.6 Plugin Studio");
    register_clcmd("say /donar", "CmdDonateUsage");
    register_clcmd("amx_donate", "CmdDonate");
    register_logevent("EventRoundStart", 2, "1=Round_Start");
}

public EventRoundStart()
{
    new players[32], num;
    get_players(players, num, "ch");
    for (new i = 0; i < num; i++)
    {
        new id = players[i];
        cs_set_user_money(id, min(16000, cs_get_user_money(id) + 500));
    }
}

public CmdDonateUsage(id)
{
    client_print(id, print_chat, "[BANCO] Usa en consola: amx_donate <jugador> <cantidad>");
    return PLUGIN_HANDLED;
}

public CmdDonate(id)
{
    new targetArg[32], amountArg[12];
    read_argv(1, targetArg, charsmax(targetArg));
    read_argv(2, amountArg, charsmax(amountArg));

    new amount = str_to_num(amountArg);
    new target = find_player("bl", targetArg);

    if (!is_user_connected(target) || target == id || amount <= 0)
    {
        console_print(id, "[BANCO] Jugador o monto inválido.");
        return PLUGIN_HANDLED;
    }

    new myMoney = cs_get_user_money(id);
    if (myMoney < amount)
    {
        console_print(id, "[BANCO] No tienes suficiente dinero.");
        return PLUGIN_HANDLED;
    }

    cs_set_user_money(id, myMoney - amount);
    cs_set_user_money(target, min(16000, cs_get_user_money(target) + amount));
    client_print(id, print_chat, "[BANCO] Transferencia de $%d realizada.", amount);
    return PLUGIN_HANDLED;
}"""
        ),
        PluginTemplate(
            id = "teleport_system",
            title = "Teletransportación",
            category = "Utilidad & Surf/KZ",
            fileName = "teleport_system.sma",
            description = "Guarda puntos de control (/cp) y teletranspórtate al instante (/tp) con validación anti-atasco.",
            includes = listOf("amxmodx", "fakemeta"),
            code = """#include <amxmodx>
#include <fakemeta>

#pragma semicolon 1

new Float:g_SavedOrigin[33][3];
new bool:g_HasCheckpoint[33];

public plugin_init()
{
    register_plugin("Checkpoint & Teleport", "1.0", "CS 1.6 Plugin Studio");
    register_clcmd("say /cp", "CmdSaveCheckpoint");
    register_clcmd("say /tp", "CmdLoadCheckpoint");
}

public client_disconnected(id)
{
    g_HasCheckpoint[id] = false;
}

public CmdSaveCheckpoint(id)
{
    if (!is_user_alive(id))
        return PLUGIN_HANDLED;

    pev(id, pev_origin, g_SavedOrigin[id]);
    g_HasCheckpoint[id] = true;
    client_print(id, print_chat, "[TP] Punto de control guardado.");
    return PLUGIN_HANDLED;
}

public CmdLoadCheckpoint(id)
{
    if (!is_user_alive(id) || !g_HasCheckpoint[id])
    {
        client_print(id, print_chat, "[TP] Primero guarda una posición con /cp.");
        return PLUGIN_HANDLED;
    }

    set_pev(id, pev_velocity, Float:{0.0, 0.0, 0.0});
    engfunc(EngFunc_SetOrigin, id, g_SavedOrigin[id]);
    client_print(id, print_chat, "[TP] Teletransportado al último checkpoint.");
    return PLUGIN_HANDLED;
}"""
        ),
        PluginTemplate(
            id = "fx_sounds",
            title = "Efectos y Sonidos",
            category = "Audio & Visual FX",
            fileName = "fx_sounds.sma",
            description = "Precache de sonidos, efectos ScreenFade al matar y brillo temporal en jugadores.",
            includes = listOf("amxmodx", "fun"),
            code = """#include <amxmodx>
#include <fun>

#pragma semicolon 1

new const g_KillSound[] = "buttons/bell1.wav";
new g_MsgScreenFade;

public plugin_precache()
{
    precache_sound(g_KillSound);
}

public plugin_init()
{
    register_plugin("FX & Sounds Pack", "1.0", "CS 1.6 Plugin Studio");
    register_event("DeathMsg", "EventDeathMsg", "a");
    g_MsgScreenFade = get_user_msgid("ScreenFade");
}

public EventDeathMsg()
{
    new killer = read_data(1);
    new victim = read_data(2);
    if (!is_user_alive(killer) || killer == victim)
        return;

    emit_sound(killer, CHAN_AUTO, g_KillSound, VOL_NORM, ATTN_NORM, 0, PITCH_NORM);
    set_user_rendering(killer, kRenderFxGlowShell, 0, 170, 255, kRenderNormal, 18);

    message_begin(MSG_ONE_UNRELIABLE, g_MsgScreenFade, _, killer);
    write_short(1 << 10);
    write_short(1 << 10);
    write_short(0x0000);
    write_byte(0);
    write_byte(180);
    write_byte(255);
    write_byte(65);
    message_end();
}"""
        ),
        PluginTemplate(
            id = "anti_cheat_guard",
            title = "Protección contra Trampas",
            category = "Seguridad & Anti-Cheat",
            fileName = "anti_cheat_guard.sma",
            description = "Detecta comandos sospechosos, bloquea spam rápido de nombre y valida velocidad máxima.",
            includes = listOf("amxmodx", "fakemeta"),
            code = """#include <amxmodx>
#include <fakemeta>

#pragma semicolon 1

new Float:g_LastNameChange[33];

public plugin_init()
{
    register_plugin("Basic Anti-Cheat Guard", "1.0", "CS 1.6 Plugin Studio");
    register_forward(FM_ClientUserInfoChanged, "FwClientInfoChanged");
    register_clcmd("fullupdate", "CmdBlockSpam");
}

public CmdBlockSpam(id)
{
    log_amx("[GUARD] Bloqueado fullupdate spam del jugador #%d", get_user_userid(id));
    return PLUGIN_HANDLED;
}

public FwClientInfoChanged(id)
{
    if (!is_user_connected(id))
        return FMRES_IGNORED;

    new Float:now = get_gametime();
    if (now - g_LastNameChange[id] < 5.0)
    {
        client_print(id, print_chat, "[GUARD] Espera 5 segundos antes de cambiar de nombre nuevamente.");
        return FMRES_SUPERCEDE;
    }

    g_LastNameChange[id] = now;
    return FMRES_IGNORED;
}"""
        ),
        PluginTemplate(
            id = "auto_respawn",
            title = "Sistema de Respawn",
            category = "Modos de Juego",
            fileName = "auto_respawn.sma",
            description = "Respawn automático configurable tras morir (estilo Deathmatch) con protección de spawn de 2 segundos.",
            includes = listOf("amxmodx", "hamsandwich", "fun"),
            code = """#include <amxmodx>
#include <hamsandwich>
#include <fun>

#pragma semicolon 1

new g_pCvarRespawnDelay;

public plugin_init()
{
    register_plugin("Auto Respawn Pro", "1.0", "CS 1.6 Plugin Studio");
    RegisterHam(Ham_Killed, "player", "FwPlayerKilledPost", 1);
    g_pCvarRespawnDelay = register_cvar("amx_respawn_delay", "2.5");
}

public FwPlayerKilledPost(victim, attacker, shouldgib)
{
    if (!is_user_connected(victim))
        return;

    new Float:delay = get_pcvar_float(g_pCvarRespawnDelay);
    remove_task(victim + 1000);
    set_task(delay, "TaskRespawnPlayer", victim + 1000);
}

public TaskRespawnPlayer(taskId)
{
    new id = taskId - 1000;
    if (!is_user_connected(id) || is_user_alive(id))
        return;

    new team = get_user_team(id);
    if (team == 1 || team == 2)
    {
        ExecuteHamB(Ham_CS_RoundRespawn, id);
        set_user_godmode(id, 1);
        set_task(2.0, "TaskRemoveSpawnProtection", id + 2000);
    }
}

public TaskRemoveSpawnProtection(taskId)
{
    new id = taskId - 2000;
    if (is_user_alive(id))
        set_user_godmode(id, 0);
}"""
        ),
        PluginTemplate(
            id = "hud_announcer",
            title = "Mensajes HUD",
            category = "Interfaz & HUD",
            fileName = "hud_announcer.sma",
            description = "Anuncios HUD sincronizados con CreateHudSyncObj rotando mensajes informativos en pantalla.",
            includes = listOf("amxmodx"),
            code = """#include <amxmodx>

#pragma semicolon 1

new g_HudSync;
new g_MsgIndex;

new const g_Messages[][] = {
    "Bienvenido al servidor CS 1.6 - Respeta las reglas",
    "Escribe /shop para abrir la tienda de armas",
    "Plugin creado con CS 1.6 Plugin Studio"
};

public plugin_init()
{
    register_plugin("HUD Announcer Sync", "1.0", "CS 1.6 Plugin Studio");
    g_HudSync = CreateHudSyncObj();
    set_task(12.0, "TaskRotateHud", _, _, _, "b");
}

public TaskRotateHud()
{
    set_hudmessage(41, 182, 246, -1.0, 0.12, 0, 0.5, 5.0, 0.2, 0.5, -1);
    ShowSyncHudMsg(0, g_HudSync, "%s", g_Messages[g_MsgIndex]);
    g_MsgIndex = (g_MsgIndex + 1) % sizeof(g_Messages);
}"""
        ),
        PluginTemplate(
            id = "player_admin",
            title = "Administración de Jugadores",
            category = "Administración",
            fileName = "player_admin.sma",
            description = "Transferir jugadores de equipo (CT/T/Spec), silenciar chat (gag) y mostrar información SteamID/IP.",
            includes = listOf("amxmodx", "amxmisc", "cstrike"),
            code = """#include <amxmodx>
#include <amxmisc>
#include <cstrike>

#pragma semicolon 1

new bool:g_Gagged[33];

public plugin_init()
{
    register_plugin("Player Admin Tools", "1.0", "CS 1.6 Plugin Studio");
    register_concmd("amx_gag", "CmdToggleGag", ADMIN_KICK, "<jugador> - Silencia/desilencia el chat");
    register_concmd("amx_transfer", "CmdTransferTeam", ADMIN_KICK, "<jugador> <1=T|2=CT|3=SPEC>");
    register_clcmd("say", "HookSay");
}

public client_disconnected(id)
{
    g_Gagged[id] = false;
}

public HookSay(id)
{
    if (g_Gagged[id])
    {
        client_print(id, print_chat, "[ADMIN] Estás silenciado en el chat.");
        return PLUGIN_HANDLED;
    }
    return PLUGIN_CONTINUE;
}

public CmdToggleGag(id, level, cid)
{
    if (!cmd_access(id, level, cid, 2))
        return PLUGIN_HANDLED;

    new arg[32];
    read_argv(1, arg, charsmax(arg));
    new target = cmd_target(id, arg, CMDTARGET_OBEY_IMMUNITY);
    if (!target)
        return PLUGIN_HANDLED;

    g_Gagged[target] = !g_Gagged[target];
    client_print(target, print_chat, "[ADMIN] Tu estado de silencio ahora es: %s", g_Gagged[target] ? "SILENCIADO" : "ACTIVO");
    return PLUGIN_HANDLED;
}

public CmdTransferTeam(id, level, cid)
{
    if (!cmd_access(id, level, cid, 3))
        return PLUGIN_HANDLED;

    new argTarget[32], argTeam[4];
    read_argv(1, argTarget, charsmax(argTarget));
    read_argv(2, argTeam, charsmax(argTeam));

    new target = cmd_target(id, argTarget, CMDTARGET_ALLOW_SELF);
    if (!target)
        return PLUGIN_HANDLED;

    switch (str_to_num(argTeam))
    {
        case 1: cs_set_user_team(target, CS_TEAM_T);
        case 2: cs_set_user_team(target, CS_TEAM_CT);
        default: cs_set_user_team(target, CS_TEAM_SPECTATOR);
    }
    if (is_user_alive(target))
        user_silentkill(target);
    return PLUGIN_HANDLED;
}"""
        )
    )
}
