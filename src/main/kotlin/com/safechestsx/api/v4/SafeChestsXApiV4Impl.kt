package com.safechestsx.api.v4

import com.safechestsx.Claim
import com.safechestsx.ClaimsManager
import com.safechestsx.VirtualChestManager
import org.bukkit.Location
import java.util.UUID

class SafeChestsXApiV4Impl(
    private val claimsManager: ClaimsManager,
    private val chestManager: VirtualChestManager
) : SafeChestsXApiV4 {
    override fun getClaimAt(location: Location?): ClaimInfo? {
        if (location == null) return null
        val claim = claimsManager.getClaimByChestKey(claimsManager.getChestKey(location)) ?: return null
        return claim.toInfo()
    }

    override fun getClaimByName(name: String): ClaimInfo? {
        val claim = claimsManager.getClaim(name) ?: return null
        return claim.toInfo()
    }

    override fun isClaimed(location: Location?): Boolean {
        return location != null && claimsManager.isClaimed(location)
    }

    override fun listClaims(owner: UUID?): List<ClaimInfo> {
        val claims = if (owner == null) {
            claimsManager.getClaims()
        } else {
            claimsManager.getClaimsOwnedBy(owner)
        }
        return claims.map { it.toInfo() }
    }

    override fun addClaimChests(claimName: String, locations: Collection<Location>): Int {
        val claim = claimsManager.getClaim(claimName) ?: return 0
        return claimsManager.addChests(claim, locations.toSet())
    }

    override fun removeClaimChests(claimName: String, locations: Collection<Location>): Int {
        val claim = claimsManager.getClaim(claimName) ?: return 0
        return claimsManager.removeChests(claim, locations.toSet())
    }

    override fun setTrust(claimName: String, playerId: UUID, trusted: Boolean): Boolean {
        val claim = claimsManager.getClaim(claimName) ?: return false
        if (trusted) {
            claim.trusted.add(playerId)
        } else {
            claim.trusted.remove(playerId)
        }
        return true
    }

    override fun getVirtualChestInfo(owner: UUID): VirtualChestInfo {
        return VirtualChestInfo(owner, chestManager.getChestLimit(owner))
    }

    override fun addVirtualChestSlots(owner: UUID, amount: Int): Boolean {
        return chestManager.addChestSlots(owner, amount)
    }

    private fun Claim.toInfo(): ClaimInfo {
        return ClaimInfo(
            name = name,
            owner = owner,
            trusted = trusted.toSet(),
            chestCount = chestKeys.size
        )
    }
}
