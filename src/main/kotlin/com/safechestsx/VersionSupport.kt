package com.safechestsx

import org.bukkit.Bukkit

object VersionSupport {
    private val supportedRange = "1.21.4-1.21.11"

    fun verifyOrDisable(plugin: SafeChestsXPlugin): Boolean {
        val current = Bukkit.getMinecraftVersion()
        if (!isSupported(current)) {
            plugin.logger.severe("Unsupported server version: $current. Supported range: $supportedRange")
            plugin.server.pluginManager.disablePlugin(plugin)
            return false
        }
        return true
    }

    fun isSupported(version: String): Boolean {
        val parts = version.split('.')
        if (parts.size < 3) {
            return false
        }
        val major = parts[0].toIntOrNull() ?: return false
        val minor = parts[1].toIntOrNull() ?: return false
        val patch = parts[2].toIntOrNull() ?: return false
        if (major != 1 || minor != 21) {
            return false
        }
        return patch in 4..11
    }
}
