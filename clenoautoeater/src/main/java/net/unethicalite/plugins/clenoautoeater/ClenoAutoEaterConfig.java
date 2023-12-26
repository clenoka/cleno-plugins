package net.unethicalite.plugins.clenoautoeater;

import net.runelite.client.config.*;

@ConfigGroup("clenoautoeater")
public interface ClenoAutoEaterConfig extends Config
{

    @ConfigItem(
            name = "Food IDs",
            description = "Food item IDs, separated by commas.",
            position = 1,
            keyName = "foodIds"
    )
    default String foodIds()
    {
        return "3144,";
    }

    @ConfigItem(
            name = "Eat at HP",
            description = "HP to eat at",
            position = 0,
            keyName = "eatAt"
    )
    @Range(min = 1, max = 100)
    default int eatAt()
    {
        return 70;
    }

    @ConfigItem(
            name = "Drink Prayer at",
            description = "When to drink prayer pots",
            position = 2,
            keyName = "drinkPray"
    )
    @Range(min = 1, max = 100)
    default int drinkPray()
    {
        return 60;
    }

}