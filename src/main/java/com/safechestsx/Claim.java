package com.safechestsx;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Claim {
    private final String name;
    private final UUID owner;
    private final Set<UUID> trusted = new HashSet<>();
    private final Set<String> chestKeys = new HashSet<>();

    public Claim(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    public Set<UUID> getTrusted() {
        return trusted;
    }

    public Set<String> getChestKeys() {
        return chestKeys;
    }
}
