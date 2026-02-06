package com.safechestsx.extensions

import com.safechestsx.SafeChestsXPlugin
import java.util.concurrent.ConcurrentHashMap

class ExtensionManager(private val plugin: SafeChestsXPlugin) {
    private val extensions = ConcurrentHashMap<String, SafeChestsXExtension>()

    fun register(extension: SafeChestsXExtension): Boolean {
        val key = extension.id.lowercase()
        if (extensions.containsKey(key)) {
            return false
        }
        extensions[key] = extension
        extension.onEnable(plugin)
        plugin.logger.info("SafeChestsX extension enabled: ${extension.displayName} (${extension.id})")
        return true
    }

    fun unregister(id: String): Boolean {
        val key = id.lowercase()
        val extension = extensions.remove(key) ?: return false
        extension.onDisable(plugin)
        plugin.logger.info("SafeChestsX extension disabled: ${extension.displayName} (${extension.id})")
        return true
    }

    fun list(): List<SafeChestsXExtension> = extensions.values.sortedBy { it.displayName }

    fun disableAll() {
        extensions.values.forEach { it.onDisable(plugin) }
        extensions.clear()
    }
}
