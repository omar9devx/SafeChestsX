package com.safechestsx.api.v3

import org.bukkit.Location
import java.util.UUID

data class ClaimSnapshot(
    val name: String,
    val owner: UUID,
    val trusted: Set<UUID>,
    val chestCount: Int
)

data class VirtualChestSnapshot(
    val owner: UUID,
    val limit: Int,
    val usedCount: Int
)

interface SafeChestsXApiV3 {
    fun getClaimAt(location: Location?): ClaimSnapshot?
    fun isClaimed(location: Location?): Boolean
    fun listClaims(owner: UUID): List<ClaimSnapshot>
    fun getVirtualChest(owner: UUID): VirtualChestSnapshot
    fun addVirtualChestSlots(owner: UUID, amount: Int): Boolean
}
