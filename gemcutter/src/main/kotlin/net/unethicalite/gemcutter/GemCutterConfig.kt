package net.unethicalite.gemcutter

import net.runelite.client.config.*
import net.unethicalite.gemcutter.util.Product

@ConfigGroup("GemCutterConfig")
interface GemCutterConfig : Config {

    companion object {
        @ConfigSection(
            name = "Sleep Delays",
            description = "",
            position = 3,
            keyName = "sleepDelays",
            closedByDefault = true
        )
        const val sleepDelays: String = "Sleep Delays"

        @ConfigSection(
            name = "Gem Types",
            description = "",
            position = 10,
            keyName = "gemTypes",
            closedByDefault = true
        )
        const val gemType: String = "Gem Type"
    }


    @Range(min = 0, max = 160)
    @ConfigItem(keyName = "sleepMin", name = "Sleep Min", description = "", position = 4, section = sleepDelays)
    @JvmDefault

    fun sleepMin(): Int {
        return 60
    }

    @Range(min = 0, max = 160)
    @ConfigItem(keyName = "sleepMax", name = "Sleep Max", description = "", position = 5, section = sleepDelays)
    @JvmDefault

    fun sleepMax(): Int {
        return 350
    }

    @Range(min = 0, max = 160)
    @ConfigItem(keyName = "sleepTarget", name = "Sleep Target", description = "", position = 6, section = sleepDelays)
    @JvmDefault

    fun sleepTarget(): Int {
        return 100
    }

    @Range(min = 0, max = 160)
    @ConfigItem(
        keyName = "sleepDeviation",
        name = "Sleep Deviation",
        description = "",
        position = 7,
        section = sleepDelays
    )
    @JvmDefault
    fun sleepDeviation(): Int {
        return 10
    }

    @ConfigItem(
        keyName = "sleepWeightedDistribution",
        name = "Sleep Weighted Distribution",
        description = "Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution",
        position = 8,
        section = sleepDelays
    )
    @JvmDefault

    fun sleepWeightedDistribution(): Boolean {
        return false
    }

    @ConfigItem(
        keyName = "gem",
        name = "Gem Type",
        description = "Choose Gem to cut",
        position = 11,
        section = gemType
    )
    @JvmDefault
    fun productType(): Product {
        return Product.DIAMOND
    }

    @ConfigItem(
        keyName = "startHelper",
        name = "Start / Stop",
        description = "Press button to start / stop plugin",
        position = 20
    )
    @JvmDefault
    fun startButton(): Button? {
        return Button()
    }

}


