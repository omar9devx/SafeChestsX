package com.safechestsx.api

import org.bukkit.Bukkit

@Suppress("DEPRECATION")

object SafeChestsXApi {
    @JvmStatic
    fun get(): SafeChestsXAPI? {
        val registration = Bukkit.getServicesManager().getRegistration(SafeChestsXAPI::class.java)
        return registration?.provider
    }

    @JvmStatic
    fun require(): SafeChestsXAPI {
        return get() ?: error("SafeChestsX API is not available.")
    }
}
