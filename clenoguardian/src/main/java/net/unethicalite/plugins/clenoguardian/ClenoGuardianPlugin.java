package net.unethicalite.plugins.clenoguardian;

import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.events.ProjectileSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.utils.MessageUtils;
import org.pf4j.Extension;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.logging.Logger;

@Extension
@PluginDescriptor(
        name = "Cleno Guardian",
        description = "Resummons guardian on attack",
        enabledByDefault = false,
        tags = {"guardian"}
)
@Singleton
public class ClenoGuardianPlugin extends Plugin
{

    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;

    @Inject
    private ClenoGuardianConfig config;

    private Logger log = Logger.getLogger(getName());
    private static final int GUARDIAN_PROJECTILE_ID = 2616;


    @Provides
    ClenoGuardianConfig getConfig(final ConfigManager configManager)
    {
        return configManager.getConfig(ClenoGuardianConfig.class);
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
    public void onProjectileSpawned(ProjectileSpawned event)
    {
        Projectile projectile = event.getProjectile();

        if (projectile.getId() == GUARDIAN_PROJECTILE_ID)
        {
            // When the guardian's projectile is detected, perform the summon and attack
            clientThread.invokeLater(() -> {
                selectSummonOption();
                attackClosestInteractingNpc();
            });
        }
    }

    private int lastSummonTick = -1;
    private void selectSummonOption()

    {
        int currentTick = client.getTickCount();
        Item horn = Inventory.getFirst("Guardian horn");
        if (horn != null && (lastSummonTick == -1 || currentTick > lastSummonTick))
        {
            horn.interact("Summon");
            lastSummonTick = currentTick;
        }
    }
    private void attackClosestInteractingNpc()
    {
        NPC npcToAttack = NPCs.getNearest(npc -> npc.isInteracting());
        if (npcToAttack != null && npcToAttack.getHealthRatio() > 0)
        {
            npcToAttack.interact("Attack");
        }
    }
}