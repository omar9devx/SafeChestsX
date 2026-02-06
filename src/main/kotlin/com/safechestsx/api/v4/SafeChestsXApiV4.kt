package com.safechestsx.api.v4

import org.bukkit.Location
import java.util.UUID

data class ClaimInfo(
    val name: String,
    val owner: UUID,
    val trusted: Set<UUID>,
    val chestCount: Int
)

data class VirtualChestInfo(
    val owner: UUID,
    val limit: Int
)

interface SafeChestsXApiV4 {
    fun getClaimAt(location: Location?): ClaimInfo?
    fun getClaimByName(name: String): ClaimInfo?
    fun isClaimed(location: Location?): Boolean
    fun listClaims(owner: UUID? = null): List<ClaimInfo>
    fun addClaimChests(claimName: String, locations: Collection<Location>): Int
    fun removeClaimChests(claimName: String, locations: Collection<Location>): Int
    fun setTrust(claimName: String, playerId: UUID, trusted: Boolean): Boolean
    fun getVirtualChestInfo(owner: UUID): VirtualChestInfo
    fun addVirtualChestSlots(owner: UUID, amount: Int): Boolean
}
