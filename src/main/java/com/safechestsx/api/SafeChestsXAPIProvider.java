package com.safechestsx.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;

public final class SafeChestsXAPIProvider {
    private SafeChestsXAPIProvider() {
    }

    public static Optional<SafeChestsXAPI> get() {
        RegisteredServiceProvider<SafeChestsXAPI> registration = Bukkit.getServicesManager()
                .getRegistration(SafeChestsXAPI.class);
        return registration == null ? Optional.empty() : Optional.ofNullable(registration.getProvider());
    }
}
