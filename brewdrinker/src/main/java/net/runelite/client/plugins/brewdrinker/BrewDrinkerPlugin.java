package net.runelite.client.plugins.brewdrinker;

/* Keep the change ya filthy animal
 * Chris
 */


import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.game.Skills;
import net.unethicalite.api.items.Inventory;
import org.pf4j.Extension;


@Extension
@PluginDescriptor(
        name = "Cleno Brew Drinker",
        enabledByDefault = false,
        description = "Drinks Brews & Restores",
        tags = {
                "bosses",
                "combat",
                "nex",
                "gwd",
                "pvm"
        }
)

@Slf4j
@Singleton
public class BrewDrinkerPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private BrewDrinkerConfig config;

    @Provides
    BrewDrinkerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(BrewDrinkerConfig.class);
    }

    public static int drinkTickTimeout = 0;

    @Override
    protected void startUp()
    {

    }

    @Override
    protected void shutDown()
    {

    }


    @Subscribe
    private void onGameTick(final GameTick event)
    {
        if (config.drinkSara() && (Skills.getBoostedLevel(Skill.HITPOINTS) <= config.healSetpoint() || drinkingSara) && drinkTickTimeout < 0)
            {
            int doses = getAllSaraDoses();
            log.info("sara doses: " + doses);
            if (!drinkingSara)
                {
                saraDosesToStopAt = doses - 3;
                if (saraDosesToStopAt < 0)
                    saraDosesToStopAt = 0;
                log.info("setup sara drink process, stop at sara doses: " + saraDosesToStopAt);
                drinkingSara = true;
                }
            Item saraBrew = Inventory.getFirst(ItemID.SARADOMIN_BREW1, ItemID.SARADOMIN_BREW2, ItemID.SARADOMIN_BREW3, ItemID.SARADOMIN_BREW4);
            if (saraBrew != null && calculateSaraBoost() != Skills.getBoostedLevel(Skill.HITPOINTS) && getAllSaraDoses() > saraDosesToStopAt)
                {
                shortSleep();
                saraBrew.interact("Drink");
                drinkTickTimeout = 3;
                }
            else
                {
                log.info("stop sara drink");
                drinkingSara = false;
                }
            }
        if (((config.drinkRestore() && Skills.getBoostedLevel(Skill.PRAYER) < 33) || (config.drinkSara() && Skills.getBoostedLevel(Skill.RANGED) < Skills.getLevel(Skill.RANGED))) && drinkTickTimeout < 0)
            {
            Item superRestore = Inventory.getFirst(ItemID.SUPER_RESTORE1, ItemID.SUPER_RESTORE2, ItemID.SUPER_RESTORE3, ItemID.SUPER_RESTORE4);
            if (superRestore != null)
                {
                shortSleep();
                superRestore.interact("Drink");
                drinkTickTimeout = 3;
                }
            }

        if (drinkTickTimeout >= -5)
            drinkTickTimeout--;
    }

    public static boolean drinkingSara = false;
    public static int saraDosesToStopAt = 0;

    public static int calculateSaraBoost()
    {
        return Skills.getLevel(Skill.HITPOINTS) + (int) (Math.floor(Skills.getLevel(Skill.HITPOINTS) * 0.15)) + 2;
    }

    public static int getAllSaraDoses()
    {
        return Inventory.getCount(ItemID.SARADOMIN_BREW1) +
                       Inventory.getCount(ItemID.SARADOMIN_BREW2) * 2 +
                       Inventory.getCount(ItemID.SARADOMIN_BREW3) * 3 +
                       Inventory.getCount(ItemID.SARADOMIN_BREW4) * 4;
    }
    public static void shortSleep()
    {
        Time.sleep(50, 200);
    }
}