package net.unethicalite.plugins.clenoitemnoter;

import net.runelite.client.config.*;

@ConfigGroup("clenoitemnoter")
public interface ClenoItemNoterConfig extends Config
{
    @ConfigSection(
            name = "Section 1",
            description = "Section 1 description",
            position = 0,
            keyName = "section1"
    )
    String section1 = "Section 1";

    @ConfigItem(
            name = "Item IDs",
            description = "Item IDs to note, separated by commas",
            position = 1,
            keyName = "noteItemIds",
            section = section1
    )
    default String noteItemIds()
    {
        return "12345"; // Default item ID; replace with a real one.
    }

    @ConfigItem(
            keyName = "noteAtX",
            name = "Note At X",
            description = "How many items should we have before noting?",
            position = 2
    )
    default int noteAtX()
    {
        return 1; // Default to 5 or any other number you choose
    }

}
