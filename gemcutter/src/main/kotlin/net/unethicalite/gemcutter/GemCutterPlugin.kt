package net.unethicalite.gemcutter

import com.google.inject.Provides
import net.runelite.api.*
import net.runelite.api.events.ConfigButtonClicked
import net.runelite.client.config.ConfigManager
import net.runelite.client.eventbus.Subscribe
import net.runelite.client.plugins.PluginDescriptor
import net.unethicalite.api.commons.Time
import net.unethicalite.api.entities.NPCs
import net.unethicalite.api.entities.Players
import net.unethicalite.api.items.Bank
import net.unethicalite.api.items.Inventory
import net.unethicalite.api.plugins.LoopedPlugin
import net.unethicalite.api.utils.MessageUtils
import net.unethicalite.api.widgets.Dialog
import net.unethicalite.api.widgets.Production
import net.unethicalite.client.Static
import net.unethicalite.gemcutter.util.*
import org.pf4j.Extension
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@Extension
@PluginDescriptor(
    name = "Gem cutter",
    description = "Automatic gem cutter",
    tags = ["gem"]
)
class GemCutterPlugin : LoopedPlugin() {

    @Inject
    lateinit var config: GemCutterConfig

    @Inject
    lateinit var functions: Functions

    @Inject
    lateinit var calculation: Calculation

    @Inject
    lateinit var chinBreakHandler: ReflectBreakHandler

    var sleepLength: Long = -1

    private var startTime: Instant = Instant.now()

    private val runtime: Duration get() = Duration.between(startTime, Instant.now())

    var startPlugin: Boolean = false

    companion object : Log()

    @Provides
    fun provideConfig(configManager: ConfigManager): GemCutterConfig {
        return configManager.getConfig(GemCutterConfig::class.java)
    }


    override fun startUp() {
        log.info("${this::class.simpleName} started at $startTime")
        chinBreakHandler.registerPlugin(this)
        reset()
    }

    override fun shutDown() {
        log.info("${this::class.simpleName} stopped at ${Instant.now()} with runtime $runtime")
        chinBreakHandler.unregisterPlugin(this)
        reset()
    }

    override fun loop(): Int {
        if (!startPlugin || chinBreakHandler.isBreakActive(this)) return 100

        with(functions) {
            MessageUtils.addMessage(getState().name)

            when (getState()) {
                States.HANDLE_BREAK -> {
                    MessageUtils.addMessage("Attempting to break")
                    chinBreakHandler.startBreak(this@GemCutterPlugin)
                }

                States.HANDLE_BANK -> {
                    if (Bank.isOpen()) {
                        if (!Bank.contains { it.id == config.productType().itemID }) {
                            startPlugin = false
                            return -1
                        }
                        if (Inventory.contains { it.id != ItemID.CHISEL && it.id != config.productType().itemID }) {
                            Bank.depositAllExcept { it.id == ItemID.CHISEL || it.id == config.productType().itemID }
                            return sleepDelay().toInt()
                        }
                        if (!Inventory.contains(ItemID.CHISEL)) {
                            if (Bank.contains(ItemID.CHISEL)) {
                                Bank.withdraw(ItemID.CHISEL, 1, Bank.WithdrawMode.ITEM)
                            } else {
                                startPlugin = false
                                return -1
                            }
                            return sleepDelay().toInt()
                        }
                        if (!Inventory.contains(config.productType().itemID)) {
                            if (Bank.contains(config.productType().itemID)) {
                                Bank.withdraw(config.productType().itemID, 27, Bank.WithdrawMode.ITEM)
                                Bank.close()
                                Time.sleepUntil({ Inventory.contains(config.productType().itemID) }, 1500)
                            } else {
                                startPlugin = false
                                return -1
                            }
                            return sleepDelay().toInt()
                        }
                    } else {
                        var banker: NPC? = NPCs.getNearest { it.hasAction("Bank") }
                        banker?.interact("Bank")
                        Time.sleepUntil({ Bank.isOpen() }, 2500)
                    }
                }

                States.CUT_GEM -> {
                    if (Production.isOpen()) {
                        Production.chooseOption(1)
                        Time.sleep(5000)
                        Time.sleepUntil(
                            { !Inventory.contains(config.productType().itemID) || (Dialog.isOpen()) },
                            { Players.getLocal().animation != -1 },
                            5000
                        )
                    } else {
                        var gem: Item? = Inventory.getFirst(config.productType().itemID)
                        var chisel: Item? = Inventory.getFirst(ItemID.CHISEL)

                        if (gem != null && chisel != null) {
                            gem.useOn(chisel)
                            Time.sleepUntil({ Production.isOpen() }, 2500)
                        }
                    }
                }

                States.UNKNOWN -> {
                    MessageUtils.addMessage("Reached unknown")
                }

            }
            return sleepDelay().toInt()
        }
    }

    private fun reset() {
        sleepLength = -1
        startPlugin = false
    }

    @Subscribe
    private fun onConfigButtonPressed(configButtonClicked: ConfigButtonClicked) {
        if (!configButtonClicked.group.equals(
                "GemCutterConfig",
                ignoreCase = true
            ) || Static.getClient().gameState != GameState.LOGGED_IN || Players.getLocal() == null
        ) return
        if (configButtonClicked.key.equals("startHelper", ignoreCase = true)) {
            startPlugin = !startPlugin
            MessageUtils.addMessage("Plugin running: $startPlugin")
            if (startPlugin)
                chinBreakHandler.startPlugin(this)
            else
                chinBreakHandler.stopPlugin(this)
        }
    }

}