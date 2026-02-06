package com.safechestsx.api.v4

import org.bukkit.Bukkit

object SafeChestsXApiV4Provider {
    @JvmStatic
    fun get(): SafeChestsXApiV4? {
        val registration = Bukkit.getServicesManager().getRegistration(SafeChestsXApiV4::class.java)
        return registration?.provider
    }
}
