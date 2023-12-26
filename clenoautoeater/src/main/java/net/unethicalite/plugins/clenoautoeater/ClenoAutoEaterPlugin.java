package net.unethicalite.plugins.clenoautoeater;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.unethicalite.api.items.Inventory;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
        name = "Cleno Auto Eater",
        description = "Automatically eats food when health is below a certain threshold",
        tags = {
                "auto",
                "eater",
                "food",
                "health",
                "survival"
        }
)
@Singleton
public class ClenoAutoEaterPlugin extends Plugin
{

    @Inject
    private Client client;

    @Inject
    private ClenoAutoEaterConfig config;

    private Set<Integer> foodIds = new HashSet<>();
    private Logger log;

    @Provides
    ClenoAutoEaterConfig getConfig(final ConfigManager configManager)
    {
        return configManager.getConfig(ClenoAutoEaterConfig.class);
    }


    @Override
    protected void startUp()
    {
        try
            {
            updateFoodIds();
            }
        catch (Exception e)
            {
            log.info("Error starting ClenoAutoEater plugin");
            }
    }

    @Override
    protected void shutDown()
    {
        log.info(getName() + " Stopped");
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (event.getGroup().equals("clenoautoeater"))
            {
            updateFoodIds();
            }
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if (client.getLocalPlayer() == null)
            {
            return;
            }

        int currentHealth = client.getBoostedSkillLevel(Skill.HITPOINTS);
        if (currentHealth <= config.eatAt())
            {
            eatFood();
            }
        int prayerPoints = client.getBoostedSkillLevel(Skill.PRAYER);
        int threshold = config.drinkPray();
        if (prayerPoints <= threshold)
            {
            usePotion();
            }
    }


    private void updateFoodIds()
    {
        foodIds = Arrays.stream(config.foodIds().split(","))
                          .map(String::trim)
                          .filter(s -> !s.isEmpty())
                          .map(Integer::parseInt)
                          .collect(Collectors.toSet());
    }

    private void eatFood()
    {
        for (int foodId : foodIds)
            {
            Item foodItem = Inventory.getFirst(foodId);
            if (foodItem != null)
                {
                foodItem.interact("Eat");
                break; // Only eat one piece of food per tick
                }
            }
    }


    private boolean isPrayerPotion(int itemId)
    {
        final Set<Integer> POTION_IDS = Set.of(3024, 3026, 3028, 3030, 2434, 139, 141, 143);
        return POTION_IDS.contains(itemId);
    }

    private void usePotion()
    {
        final Set<Integer> POTION_IDS = Set.of(3024, 3026, 3028, 3030, 2434, 139, 141, 143);

        for (int potionId : POTION_IDS)
            {
            Item potionItem = Inventory.getFirst(potionId);
            if (potionItem != null)
                {
                potionItem.interact("Drink");
                break; // Drink only one potion per tick
                }
            }
    }
}