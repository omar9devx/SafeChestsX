package com.safechestsx.api.v3

import com.safechestsx.Claim
import com.safechestsx.ClaimsManager
import com.safechestsx.VirtualChestManager
import org.bukkit.Location
import java.util.UUID

class SafeChestsXApiV3Impl(
    private val claimsManager: ClaimsManager,
    private val chestManager: VirtualChestManager
) : SafeChestsXApiV3 {
    override fun getClaimAt(location: Location?): ClaimSnapshot? {
        if (location == null) return null
        val claim = claimsManager.getClaimByChestKey(claimsManager.getChestKey(location)) ?: return null
        return claim.toSnapshot()
    }

    override fun isClaimed(location: Location?): Boolean {
        return location != null && claimsManager.isClaimed(location)
    }

    override fun listClaims(owner: UUID): List<ClaimSnapshot> {
        return claimsManager.getClaimsOwnedBy(owner).map { it.toSnapshot() }
    }

    override fun getVirtualChest(owner: UUID): VirtualChestSnapshot {
        return VirtualChestSnapshot(
            owner = owner,
            limit = chestManager.getChestLimit(owner),
            usedCount = chestManager.getUsedChestCount(owner)
        )
    }

    override fun addVirtualChestSlots(owner: UUID, amount: Int): Boolean {
        return chestManager.addChestSlots(owner, amount)
    }

    private fun Claim.toSnapshot(): ClaimSnapshot {
        return ClaimSnapshot(
            name = name,
            owner = owner,
            trusted = trusted.toSet(),
            chestCount = chestKeys.size
        )
    }
}
