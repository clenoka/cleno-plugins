package net.unethicalite.winemaker

import com.google.inject.Provides
import net.runelite.api.*
import net.runelite.api.events.ChatMessage
import net.runelite.api.events.ConfigButtonClicked
import net.runelite.client.config.ConfigManager
import net.runelite.client.eventbus.Subscribe
import net.runelite.client.plugins.PluginDescriptor
import net.unethicalite.api.commons.Time
import net.unethicalite.api.entities.NPCs
import net.unethicalite.api.entities.Players
import net.unethicalite.api.entities.TileObjects
import net.unethicalite.api.items.Bank
import net.unethicalite.api.items.Inventory
import net.unethicalite.api.plugins.LoopedPlugin
import net.unethicalite.api.utils.MessageUtils
import net.unethicalite.api.widgets.Production
import net.unethicalite.client.Static
import net.unethicalite.winemaker.util.Calculation
import net.unethicalite.winemaker.util.Functions
import net.unethicalite.winemaker.util.Log
import net.unethicalite.winemaker.util.ReflectBreakHandler
import org.pf4j.Extension
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@Extension
@PluginDescriptor(
    name = "Wine Maker",
    description = "Automatic Wine Maker",
    tags = ["wine"]
)
class WineMakerPlugin : LoopedPlugin() {

    @Inject
    lateinit var config: WineMakerConfig

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
    fun provideConfig(configManager: ConfigManager): WineMakerConfig {
        return configManager.getConfig(WineMakerConfig::class.java)
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
        if (!startPlugin || chinBreakHandler.isBreakActive(this)) return 600
        with(functions) {
            if (config.debugger()) MessageUtils.addMessage("Current state: ${getState()}")
            var local: Player = Players.getLocal()

            if(chinBreakHandler.shouldBreak(this@WineMakerPlugin)){
                MessageUtils.addMessage("Attempting to break")
                chinBreakHandler.startBreak(this@WineMakerPlugin)
            }

            if(Inventory.contains(ItemID.JUG_OF_BAD_WINE) || Inventory.contains(ItemID.JUG_OF_WINE) || Inventory.contains("Zamorak's unfermented wine")){
                if(Bank.isOpen()){
                    Bank.depositInventory()
                }else{
                    var banker: NPC? = NPCs.getNearest { it.hasAction("Bank") }
                    if (banker != null) {
                        banker.interact("Bank")
                        Time.sleepUntil({ Bank.isOpen() }, 1500)
                        return 1
                    }
                }
            }

            if(!Inventory.contains("Zamorak's grapes")){
                if(!Bank.isOpen()){
                    var banker: NPC? = NPCs.getNearest { it.hasAction("Bank") }
                    if (banker != null) {
                        banker.interact("Bank")
                        Time.sleepUntil({ Bank.isOpen() }, 1500)
                    }
                }else{
                    if (Bank.contains(ItemID.ZAMORAKS_GRAPES)) {
                        Bank.withdraw(ItemID.ZAMORAKS_GRAPES, 14, Bank.WithdrawMode.ITEM)
                    }
                    else
                    {
                        startPlugin = false
                        return -1
                    }
                }
            }
            if(!Inventory.contains("Jug of water")){
                if(!Bank.isOpen()){
                    var banker: NPC? = NPCs.getNearest { it.hasAction("Bank") }
                    if (banker != null) {
                        banker.interact("Bank")
                        Time.sleepUntil({ Bank.isOpen() }, 1500)
                    }
                }else{
                    if (Bank.contains(ItemID.JUG_OF_WATER)) {
                        Bank.withdraw(ItemID.JUG_OF_WATER, 14, Bank.WithdrawMode.ITEM)
                    }
                    else
                    {
                        startPlugin = false
                        return -1
                    }
                }
                Bank.close()
                Time.sleepTick()
            }

            if(!Inventory.contains("Zamorak's grapes") || !Inventory.contains("Jug of water")) return 1
            if (!local.isAnimating) {
                if (Production.isOpen()) {
                    Production.chooseOption(1)
                    sleepUntil(
                        { !Inventory.contains { it.id == ItemID.JUG_OF_WATER } || !Inventory.contains { it.id == ItemID.ZAMORAKS_GRAPES } },
                        { Players.getLocal().isAnimating },
                        100, 2000
                    )
                } else {
                    if (Inventory.contains { it.id == ItemID.ZAMORAKS_GRAPES } && Inventory.contains { it.id == ItemID.JUG_OF_WATER }) {
                        Inventory.getFirst { it.id == ItemID.ZAMORAKS_GRAPES }
                            .useOn(Inventory.getFirst { it.id == ItemID.JUG_OF_WATER })
                        Time.sleepUntil({ Production.isOpen() }, 2000)
                    }
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
    private fun onChatMessage(chatMessage: ChatMessage){
        val message: String = chatMessage.message
        if(message.isEmpty() || chatMessage.type != ChatMessageType.GAMEMESSAGE) return

    }
    @Subscribe
    private fun onConfigButtonPressed(configButtonClicked: ConfigButtonClicked) {
        if (!configButtonClicked.group.equals("WineMakerConfig", ignoreCase = true) || Static.getClient().gameState != GameState.LOGGED_IN || Players.getLocal() == null) return
        if (configButtonClicked.key.equals("startHelper", ignoreCase = true)) {
            startPlugin = !startPlugin
            MessageUtils.addMessage("Plugin running: $startPlugin")
            if(startPlugin)
                chinBreakHandler.startPlugin(this)
            else
                chinBreakHandler.stopPlugin(this)
        }
    }

}