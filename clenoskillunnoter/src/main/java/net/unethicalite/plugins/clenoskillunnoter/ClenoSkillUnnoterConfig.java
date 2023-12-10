package net.unethicalite.plugins.clenoskillunnoter;

import net.runelite.client.config.*;

@ConfigGroup("clenoskillunnoter")
public interface ClenoSkillUnnoterConfig extends Config
{
    @ConfigSection(
            name = "Section 1",
            description = "Section 1 description",
            position = 0,
            keyName = "section1"
    )
    String section1 = "Section 1";
    @ConfigItem(
            keyName = "notedItemId",
            name = "Noted Item ID",
            description = "The ID of the noted item",
            position = 0
    )
    default int notedItemId()
    {
        return -1; // Default to an invalid item ID
    }

    @ConfigItem(
            keyName = "unnotedItemId",
            name = "Unnoted Item ID",
            description = "The ID of the unnoted item",
            position = 1
    )
    default int unnotedItemId()
    {
        return -1; // Default to an invalid item ID
    }

    @ConfigItem(
            keyName = "minimumRemaining",
            name = "Minimum Remaining",
            description = "The minimum remaining unnoted items before using the noted item on the banker's note",
            position = 2
    )
    default int minimumRemaining()
    {
        return 5; // Default to 5 or any other number you choose
    }

}
