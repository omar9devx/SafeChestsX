package com.safechestsx.command

import com.safechestsx.SafeChestsXPlugin
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.util.Locale

class BypassCommand(
    private val plugin: SafeChestsXPlugin,
    private val bypass: Boolean
) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission("safechestsx.bypass")) {
            plugin.messages.send(sender, "no-permission")
            return true
        }
        if (args.size < 2) {
            val usage = if (bypass) "/bypasscc <player> <claim>" else "/unbypasscc <player> <claim>"
            plugin.messages.send(sender, "bypass-usage", mapOf("usage" to usage))
            return true
        }
        val target = resolvePlayer(args[0])
        if (target == null) {
            plugin.messages.send(sender, "player-not-found")
            return true
        }
        val claimName = args[1]
        val claim = plugin.claimsManager.getClaim(claimName)
        if (claim == null) {
            plugin.messages.send(sender, "claim-missing")
            return true
        }
        val list = claim.bypassed
        if (bypass) {
            list.add(target.uniqueId)
            plugin.messages.send(sender, "bypass-added", mapOf(
                "player" to (target.name ?: target.uniqueId.toString()),
                "claim" to claim.name
            ))
        } else {
            list.remove(target.uniqueId)
            plugin.messages.send(sender, "bypass-removed", mapOf(
                "player" to (target.name ?: target.uniqueId.toString()),
                "claim" to claim.name
            ))
        }
        plugin.claimsManager.saveAsync()
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String> {
        if (!sender.hasPermission("safechestsx.bypass")) return emptyList()
        if (args.size == 1) {
            return filter(offlineNames(), args[0])
        }
        if (args.size == 2) {
            return filter(plugin.claimsManager.claims.map { it.name }, args[1])
        }
        return emptyList()
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

    private fun offlineNames(): List<String> =
        Bukkit.getOfflinePlayers().mapNotNull { it.name }

    private fun filter(options: List<String>, input: String?): List<String> {
        if (input.isNullOrBlank()) return options
        val lower = input.lowercase(Locale.ROOT)
        return options.filter { it.lowercase(Locale.ROOT).startsWith(lower) }
    }
}
