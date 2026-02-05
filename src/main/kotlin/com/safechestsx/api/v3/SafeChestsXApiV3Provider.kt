package com.safechestsx.api.v3

import org.bukkit.Bukkit

object SafeChestsXApiV3Provider {
    @JvmStatic
    fun get(): SafeChestsXApiV3? {
        val registration = Bukkit.getServicesManager().getRegistration(SafeChestsXApiV3::class.java)
        return registration?.provider
    }
}
