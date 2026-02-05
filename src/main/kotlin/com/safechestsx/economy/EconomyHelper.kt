package com.safechestsx.economy

import net.milkbowl.vault.economy.Economy
import org.bukkit.entity.Player

object EconomyHelper {
    fun withdraw(economy: Economy, player: Player, amount: Double): Boolean {
        val response = economy.withdrawPlayer(player, amount)
        return response != null && response.transactionSuccess()
    }

    fun deposit(economy: Economy, player: Player, amount: Double): Boolean {
        val response = economy.depositPlayer(player, amount)
        return response != null && response.transactionSuccess()
    }
}
