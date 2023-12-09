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

}
