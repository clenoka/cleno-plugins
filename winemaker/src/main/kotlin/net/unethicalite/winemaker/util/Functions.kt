package net.unethicalite.winemaker.util

import net.runelite.api.Client
import net.unethicalite.api.commons.Time
import net.unethicalite.api.items.Inventory
import net.unethicalite.client.Static
import net.unethicalite.winemaker.WineMakerPlugin
import net.unethicalite.winemaker.States
import java.util.function.BooleanSupplier
import javax.inject.Inject

class Functions {
    @Inject
    lateinit var client: Client

    fun WineMakerPlugin.sleepDelay(): Long {
        sleepLength = calculation.randomDelay(
            config.sleepWeightedDistribution(),
            config.sleepMin(),
            config.sleepMax(),
            config.sleepDeviation(),
            config.sleepTarget()
        )
        return sleepLength
    }

    fun WineMakerPlugin.getState(): States {
        return States.UNKNOWN
    }

    fun WineMakerPlugin.sleepUntil(supplier: BooleanSupplier, resetSupplier: BooleanSupplier, pollingRate: Int, timeOut: Int): Boolean {
        if (Static.getClient().isClientThread) {
            return false
        }
        var start = System.currentTimeMillis()
        while (!supplier.asBoolean) {
            if (System.currentTimeMillis() > start + timeOut) {
                return false
            }
            if (resetSupplier.asBoolean) {
                start = System.currentTimeMillis()
            }
            if (!Time.sleep(pollingRate.toLong())) {
                return false
            }
        }
        return true
    }
}