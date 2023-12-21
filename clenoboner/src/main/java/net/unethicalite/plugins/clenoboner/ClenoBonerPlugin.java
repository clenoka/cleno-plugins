package net.unethicalite.plugins.clenoboner;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.entities.TileObjects;
import org.pf4j.Extension;

import javax.inject.Inject;
@Extension
@PluginDescriptor(
        name = "Bones to Altar",
        description = "Automates using bones on an altar",
        tags = {"bones", "altar", "automation"},
        enabledByDefault = false
)
public class ClenoBonerPlugin extends Plugin
{

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    private static final int CHEST_ID = 40739; // Replace with the actual ID of the chest
    private int tickCounter = 0;
    private static final int WAIT_TICKS = 6;

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if (tickCounter < WAIT_TICKS)
        {
            tickCounter++;
            return;
        }

        openChest();
        tickCounter = 0;
    }

    private void openChest()
    {
        GameObject chest = findNearestGameObject(CHEST_ID);
        if (chest != null)
        {
            clientThread.invokeLater(() -> chest.interact("Picklock"));
        }
    }

    private GameObject findNearestGameObject(int chestId)
    {
        // Assuming TileObjects.getNearest() is a valid method in your client
        return (GameObject) TileObjects.getNearest(chestId);
    }
}