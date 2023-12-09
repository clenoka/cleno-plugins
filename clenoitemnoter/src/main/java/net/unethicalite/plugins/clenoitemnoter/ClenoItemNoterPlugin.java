package net.unethicalite.plugins.clenoitemnoter;

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
        name = "Cleno Item Noter",
        description = "Notes Items",
        enabledByDefault = false,
        tags = {"noter"}
)
@Singleton
public class ClenoItemNoterPlugin extends Plugin
{

    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;

    @Inject
    private ClenoItemNoterConfig config;

    private Logger log = Logger.getLogger(getName());
    private static final int BANKERS_NOTE_ID = 28767;


    @Provides
    ClenoItemNoterConfig getConfig(final ConfigManager configManager)
    {
        return configManager.getConfig(ClenoItemNoterConfig.class);
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

    @Subscribe
    public void onGameTick(GameTick event)
    {
        Item bankersNote = Inventory.getFirst(BANKERS_NOTE_ID);
        if (bankersNote == null)
        {
            return; // Stop if the Banker's Note isn't in the inventory
        }

        // Split the config item IDs by commas and parse them to integers
        String[] itemIdsToNote = config.noteItemIds().split(",");
        for (String itemIdStr : itemIdsToNote)
        {
            int itemId = Integer.parseInt(itemIdStr.trim());

            // Find the item in the inventory with the matching ID
            Item item = Inventory.getFirst(itemId);
            if (item != null)
            {
                item.useOn(bankersNote);
                break; // Note one item per tick to avoid actions being too quick
            }
        }
    }
}