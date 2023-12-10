package net.unethicalite.plugins.clenoskillunnoter;

import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.utils.MessageUtils;
import net.runelite.api.events.GameTick;
import org.pf4j.Extension;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.logging.Logger;

@Extension
@PluginDescriptor(
        name = "Cleno Skill Unnoter",
        description = "Unnotes Items",
        enabledByDefault = false,
        tags = {"unnoter"}
)
@Singleton
public class ClenoSkillUnnoterPlugin extends Plugin
{

    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;

    @Inject
    private ClenoSkillUnnoterConfig config;

    private Logger log = Logger.getLogger(getName());
    private static final int BANKERS_NOTE_ID = 28767;


    @Provides
    ClenoSkillUnnoterConfig getConfig(final ConfigManager configManager)
    {
        return configManager.getConfig(ClenoSkillUnnoterConfig.class);
    }

    @Override
    protected void startUp()
    {
        log.info(getName() + " Started");

        if (client.getGameState() == GameState.LOGGED_IN)
        {
            MessageUtils.addMessage(getName() + " Started");
        }
    }

    @Override
    protected void shutDown()
    {
        log.info(getName() + " Stopped");
    }

    private int tickCounter = 0;
    private static final int ACTION_DELAY_TICKS = 5; // 5 game ticks delay

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (tickCounter > 0)
        {
            tickCounter--;
            return; // Wait until the delay has passed
        }

        int notedItemId = config.notedItemId();
        int unnotedItemId = config.unnotedItemId();
        int minimumRemaining = config.minimumRemaining();

        int countUnnoted = Inventory.getCount(unnotedItemId);

        if (countUnnoted < minimumRemaining)
        {
            Item bankersNote = Inventory.getFirst(BANKERS_NOTE_ID);
            Item notedItem = Inventory.getFirst(notedItemId);

            if (bankersNote != null && notedItem != null)
            {
                notedItem.useOn(bankersNote);
                tickCounter = ACTION_DELAY_TICKS; // Reset the tick counter after performing the action
            }
        }
    }
}