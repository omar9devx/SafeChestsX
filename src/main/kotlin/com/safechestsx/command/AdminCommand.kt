package com.safechestsx.command

import com.safechestsx.Claim
import com.safechestsx.SafeChestsXPlugin
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.Locale

class AdminCommand(private val plugin: SafeChestsXPlugin) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission("safechestsx.admin")) {
            plugin.messages.send(sender, "no-permission")
            return true
        }
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }
        when (args[0].lowercase(Locale.ROOT)) {
            "reload" -> handleReload(sender)
            "stats" -> handleStats(sender)
            "listclaims" -> handleListClaims(sender, args)
            "extensions" -> handleExtensions(sender)
            "setflag" -> handleSetFlag(sender, args)
            "setmode" -> handleSetMode(sender, args)
            "deleteclaim" -> handleDeleteClaim(sender, args)
            "claiminfo" -> handleClaimInfo(sender, args)
            "givewand" -> handleGiveWand(sender, args)
            "clearselection" -> handleClearSelection(sender, args)
            "addvchests" -> handleAddVirtualChests(sender, args)
            "setvchests" -> handleSetVirtualChests(sender, args)
            else -> sendHelp(sender)
        }
        return true
    }

    private fun handleReload(sender: CommandSender) {
        plugin.reloadConfig()
        plugin.reloadMessages()
        plugin.claimsManager.load()
        plugin.virtualChestManager.load()
        plugin.messages.send(sender, "admin-reload")
    }

    private fun handleStats(sender: CommandSender) {
        val claimCount = plugin.claimsManager.claims.size
        val virtualCount = plugin.virtualChestManager.profileCount
        plugin.messages.send(sender, "admin-stats", mapOf(
            "claims", claimCount.toString(),
            "virtual", virtualCount.toString()
        ))
    }

    private fun handleExtensions(sender: CommandSender) {
        val extensions = plugin.extensionManager.list()
        if (extensions.isEmpty()) {
            plugin.messages.send(sender, "admin-extensions-empty")
            return
        }
        plugin.messages.send(sender, "admin-list-header", mapOf(
            "count", extensions.size.toString(),
            "scope", "extensions"
        ))
        extensions.forEach { extension ->
            plugin.messages.send(sender, "admin-extensions-entry", mapOf(
                "name", extension.displayName,
                "id", extension.id
            ))
        }
    }

    private fun handleSetFlag(sender: CommandSender, args: Array<String>) {
        val key = args.getOrNull(1)?.lowercase(Locale.ROOT)
        val value = args.getOrNull(2)?.lowercase(Locale.ROOT)
        if (key == null || value == null) {
            plugin.messages.send(sender, "admin-usage", mapOf("usage", "/scxadmin setflag <explosions|pistons|hoppers|wandcraft> <true|false>"))
            return
        }
        val boolValue = when (value) {
            "true", "yes", "on" -> true
            "false", "no", "off" -> false
            else -> null
        }
        if (boolValue == null) {
            plugin.messages.send(sender, "admin-flag-value")
            return
        }
        val configKey = when (key) {
            "explosions" -> "settings.protect-explosions"
            "pistons" -> "settings.protect-pistons"
            "hoppers" -> "settings.protect-hoppers"
            "wandcraft" -> "settings.allow-wand-craft"
            else -> null
        }
        if (configKey == null) {
            plugin.messages.send(sender, "admin-flag-invalid")
            return
        }
        plugin.config.set(configKey, boolValue)
        plugin.saveConfig()
        plugin.messages.send(sender, "admin-flag-updated", mapOf(
            "flag", key,
            "value", boolValue.toString()
        ))
    }

    private fun handleSetMode(sender: CommandSender, args: Array<String>) {
        val mode = args.getOrNull(1)?.lowercase(Locale.ROOT)
        if (mode == null || (mode != "add" && mode != "replace")) {
            plugin.messages.send(sender, "admin-usage", mapOf("usage", "/scxadmin setmode <add|replace>"))
            return
        }
        plugin.config.set("settings.selection-mode", mode)
        plugin.saveConfig()
        plugin.messages.send(sender, "admin-mode-updated", mapOf("mode", mode))
    }

    private fun handleListClaims(sender: CommandSender, args: Array<String>) {
        val target = resolvePlayer(args.getOrNull(1))
        val claims = if (target == null) {
            plugin.claimsManager.claims.toList()
        } else {
            plugin.claimsManager.getClaimsOwnedBy(target.uniqueId).toList()
        }
        if (claims.isEmpty()) {
            plugin.messages.send(sender, "list-empty")
            return
        }
        plugin.messages.send(sender, "admin-list-header", mapOf(
            "count", claims.size.toString(),
            "scope", target?.name ?: plugin.messages.getRaw("list-scope-all").ifBlank { "all" }
        ))
        claims.sortedBy { it.name.lowercase(Locale.ROOT) }.forEach { claim ->
            plugin.messages.send(sender, "list-entry", mapOf(
                "claim", claim.name,
                "owner", Bukkit.getOfflinePlayer(claim.owner).name ?: claim.owner.toString(),
                "count", claim.chestKeys.size.toString()
            ))
        }
    }

    private fun handleDeleteClaim(sender: CommandSender, args: Array<String>) {
        val name = args.getOrNull(1).orEmpty()
        if (name.isBlank()) {
            plugin.messages.send(sender, "admin-usage", mapOf("usage", "/scxadmin deleteclaim <name>"))
            return
        }
        val claim = plugin.claimsManager.getClaim(name)
        if (claim == null) {
            plugin.messages.send(sender, "claim-missing")
            return
        }
        plugin.claimsManager.deleteClaim(claim)
        plugin.claimsManager.saveAsync()
        plugin.messages.send(sender, "admin-claim-deleted", mapOf("claim", claim.name))
    }

    private fun handleClaimInfo(sender: CommandSender, args: Array<String>) {
        val name = args.getOrNull(1).orEmpty()
        if (name.isBlank()) {
            plugin.messages.send(sender, "admin-usage", mapOf("usage", "/scxadmin claiminfo <name>"))
            return
        }
        val claim = plugin.claimsManager.getClaim(name)
        if (claim == null) {
            plugin.messages.send(sender, "claim-missing")
            return
        }
        sendClaimInfo(sender, claim)
    }

    private fun handleGiveWand(sender: CommandSender, args: Array<String>) {
        val target = resolveOnlinePlayer(args.getOrNull(1))
        if (target == null) {
            plugin.messages.send(sender, "player-not-found")
            return
        }
        plugin.giveWandAndGuide(target)
        plugin.messages.send(sender, "admin-givewand", mapOf("player", target.name))
    }

    private fun handleClearSelection(sender: CommandSender, args: Array<String>) {
        val target = resolveOnlinePlayer(args.getOrNull(1))
        if (target == null) {
            plugin.messages.send(sender, "player-not-found")
            return
        }
        plugin.clearSelection(target)
        plugin.messages.send(sender, "admin-selection-cleared", mapOf("player", target.name))
    }

    private fun handleAddVirtualChests(sender: CommandSender, args: Array<String>) {
        val target = resolvePlayer(args.getOrNull(1))
        val amount = args.getOrNull(2)?.toIntOrNull()
        if (target == null || amount == null) {
            plugin.messages.send(sender, "admin-usage", mapOf("usage", "/scxadmin addvchests <player> <amount>"))
            return
        }
        val success = plugin.virtualChestManager.addChestSlots(target.uniqueId, amount)
        val key = if (success) "admin-vchests-added" else "admin-vchests-failed"
        plugin.messages.send(sender, key, mapOf(
            "player", target.name ?: target.uniqueId.toString(),
            "amount", amount.toString()
        ))
    }

    private fun handleSetVirtualChests(sender: CommandSender, args: Array<String>) {
        val target = resolvePlayer(args.getOrNull(1))
        val amount = args.getOrNull(2)?.toIntOrNull()
        if (target == null || amount == null) {
            plugin.messages.send(sender, "admin-usage", mapOf("usage", "/scxadmin setvchests <player> <amount>"))
            return
        }
        val success = plugin.virtualChestManager.setChestLimit(target.uniqueId, amount)
        val key = if (success) "admin-vchests-set" else "admin-vchests-failed"
        plugin.messages.send(sender, key, mapOf(
            "player", target.name ?: target.uniqueId.toString(),
            "amount", amount.toString()
        ))
    }

    private fun sendHelp(sender: CommandSender) {
        plugin.messages.sendList(sender, "admin-help", emptyMap())
    }

    private fun sendClaimInfo(sender: CommandSender, claim: Claim) {
        plugin.messages.send(sender, "manage-info", mapOf(
            "claim", claim.name,
            "owner", Bukkit.getOfflinePlayer(claim.owner).name ?: claim.owner.toString(),
            "count", claim.chestKeys.size.toString(),
            "trusted", plugin.claimsManager.getTrustedNames(claim)
        ))
    }

    private fun resolvePlayer(name: String?): OfflinePlayer? {
        if (name.isNullOrBlank()) return null
        val cached = Bukkit.getOfflinePlayerIfCached(name)
        if (cached != null && cached.name != null) {
            return cached
        }
        val fallback = Bukkit.getOfflinePlayer(name)
        return if (fallback.name == null) null else fallback
    }

    private fun resolveOnlinePlayer(name: String?): Player? {
        if (name.isNullOrBlank()) return null
        return Bukkit.getPlayerExact(name)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        if (!sender.hasPermission("safechestsx.admin")) return emptyList()
        if (args.size == 1) {
            return filter(
                listOf(
                    "reload",
                    "stats",
                    "listclaims",
                    "extensions",
                    "setflag",
                    "setmode",
                    "deleteclaim",
                    "claiminfo",
                    "givewand",
                    "clearselection",
                    "addvchests",
                    "setvchests"
                ),
                args[0]
            )
        }
        if (args.size == 2 && args[0].equals("listclaims", true)) {
            return filter(onlineNames(), args[1])
        }
        if (args.size == 2 && args[0].equals("setflag", true)) {
            return filter(listOf("explosions", "pistons", "hoppers", "wandcraft"), args[1])
        }
        if (args.size == 2 && args[0].equals("setmode", true)) {
            return filter(listOf("add", "replace"), args[1])
        }
        if (args.size == 2 && args[0].equals("givewand", true)) {
            return filter(onlineNames(), args[1])
        }
        if (args.size == 2 && args[0].equals("clearselection", true)) {
            return filter(onlineNames(), args[1])
        }
        if (args.size == 2 && args[0].equals("deleteclaim", true)) {
            return filter(plugin.claimsManager.claims.map { it.name }, args[1])
        }
        if (args.size == 2 && args[0].equals("claiminfo", true)) {
            return filter(plugin.claimsManager.claims.map { it.name }, args[1])
        }
        if (args.size == 2 && (args[0].equals("addvchests", true) || args[0].equals("setvchests", true))) {
            return filter(offlineNames(), args[1])
        }
        return emptyList()
    }

    private fun onlineNames(): List<String> = Bukkit.getOnlinePlayers().map { it.name }

    private fun offlineNames(): List<String> =
        Bukkit.getOfflinePlayers().mapNotNull { it.name }

    private fun filter(options: List<String>, input: String?): List<String> {
        if (input.isNullOrBlank()) return options
        val lower = input.lowercase(Locale.ROOT)
        return options.filter { it.lowercase(Locale.ROOT).startsWith(lower) }
    }
}
