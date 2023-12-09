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
            name = "Items",
            description = "Item names separated by commas",
            position = 1,
            keyName = "noteItems",
            section = section1
    )
    default String noteItems()
    {
        return "Runite bar";
    }

}
