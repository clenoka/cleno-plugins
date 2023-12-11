package net.unethicalite.gemcutter.util

import net.runelite.api.Client
import net.runelite.api.ItemID
import net.unethicalite.api.entities.Players
import net.unethicalite.api.game.Game
import net.unethicalite.api.items.Inventory
import net.unethicalite.gemcutter.GemCutterPlugin
import net.unethicalite.gemcutter.States
import javax.inject.Inject

class Functions {
    @Inject
    lateinit var client: Client

    fun GemCutterPlugin.sleepDelay(): Long {
        sleepLength = calculation.randomDelay(
            config.sleepWeightedDistribution(),
            config.sleepMin(),
            config.sleepMax(),
            config.sleepDeviation(),
            config.sleepTarget()
        )
        return sleepLength
    }

    fun GemCutterPlugin.getState(): States {
        if (!Game.isLoggedIn()) return States.UNKNOWN
        if (chinBreakHandler.shouldBreak(this))
            return States.HANDLE_BREAK
        if (!Inventory.contains(config.productType().itemID) || !Inventory.contains(ItemID.CHISEL))
            return States.HANDLE_BANK
        if (!Players.getLocal().isAnimating && Inventory.contains(config.productType().itemID))
            return States.CUT_GEM
        return States.UNKNOWN
    }

}