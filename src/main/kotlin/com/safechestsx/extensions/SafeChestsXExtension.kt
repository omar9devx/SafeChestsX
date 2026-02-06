package com.safechestsx.extensions

import com.safechestsx.SafeChestsXPlugin

interface SafeChestsXExtension {
    val id: String
    val displayName: String

    fun onEnable(plugin: SafeChestsXPlugin) {}
    fun onDisable(plugin: SafeChestsXPlugin) {}
}
