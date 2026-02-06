package com.safechestsx.api.v4

import com.safechestsx.Claim
import com.safechestsx.ClaimsManager
import com.safechestsx.VirtualChestManager
import com.safechestsx.extensions.ExtensionManager
import com.safechestsx.extensions.SafeChestsXExtension
import org.bukkit.Location
import java.util.UUID

class SafeChestsXApiV4Impl(
    private val claimsManager: ClaimsManager,
    private val chestManager: VirtualChestManager,
    private val extensionManager: ExtensionManager
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
        val added = claimsManager.addChests(claim, locations.toSet())
        if (added > 0) {
            claimsManager.saveAsync()
        }
        return added
    }

    override fun removeClaimChests(claimName: String, locations: Collection<Location>): Int {
        val claim = claimsManager.getClaim(claimName) ?: return 0
        val removed = claimsManager.removeChests(claim, locations.toSet())
        if (removed > 0) {
            claimsManager.saveAsync()
        }
        return removed
    }

    override fun createClaim(name: String, owner: UUID, locations: Collection<Location>): Boolean {
        if (name.isBlank() || locations.isEmpty() || claimsManager.claimExists(name)) {
            return false
        }
        claimsManager.createClaim(name, owner, locations.toSet())
        claimsManager.saveAsync()
        return true
    }

    override fun renameClaim(name: String, newName: String): Boolean {
        if (newName.isBlank() || claimsManager.claimExists(newName)) {
            return false
        }
        val claim = claimsManager.getClaim(name) ?: return false
        claimsManager.renameClaim(claim, newName)
        claimsManager.saveAsync()
        return true
    }

    override fun deleteClaim(name: String): Boolean {
        val claim = claimsManager.getClaim(name) ?: return false
        claimsManager.deleteClaim(claim)
        claimsManager.saveAsync()
        return true
    }

    override fun setTrust(claimName: String, playerId: UUID, trusted: Boolean): Boolean {
        val claim = claimsManager.getClaim(claimName) ?: return false
        if (trusted) {
            claim.trusted.add(playerId)
        } else {
            claim.trusted.remove(playerId)
        }
        claimsManager.saveAsync()
        return true
    }

    override fun getVirtualChestInfo(owner: UUID): VirtualChestInfo {
        return VirtualChestInfo(owner, chestManager.getChestLimit(owner))
    }

    override fun addVirtualChestSlots(owner: UUID, amount: Int): Boolean {
        return chestManager.addChestSlots(owner, amount)
    }

    override fun setVirtualChestLimit(owner: UUID, limit: Int): Boolean {
        return chestManager.setChestLimit(owner, limit)
    }

    override fun registerExtension(extension: SafeChestsXExtension): Boolean {
        return extensionManager.register(extension)
    }

    override fun unregisterExtension(id: String): Boolean {
        return extensionManager.unregister(id)
    }

    override fun listExtensions(): List<SafeChestsXExtension> {
        return extensionManager.list()
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
