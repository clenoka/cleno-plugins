//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.unethicalite.plugins.clenofisher;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.unethicalite.api.game.Game;

@ConfigGroup("keeganfisher")
public interface KeeganFisherConfig extends Config
{
    @ConfigItem(
            keyName = "username",
            name = "Username",
            description = "Username",
            position = 0
    )
    default String username()
    {
        return Game.getGameAccount().getUsername();
    }

    @ConfigItem(
            keyName = "password",
            name = "Password",
            description = "Password",
            secret = true,
            position = 1
    )
    default String password()
    {
        return Game.getGameAccount().getPassword();
    }

    @ConfigItem(
            keyName = "hopWorlds",
            name = "Hop Worlds",
            description = "Hop worlds if another player is near by",
            position = 2
    )
    default boolean hopWorlds()
    {
        return false;
    }

    @ConfigItem(
            keyName = "region",
            name = "Region",
            description = "Region to log into",
            position = 3
    )
    default Region Region()
    {
        return Region.USA;
    }

    @ConfigItem(
            keyName = "fishingLocation",
            name = "Fishing Location",
            description = "Location to fish at",
            position = 4
    )
    default FishingLocation FishingLocation()
    {
        return FishingLocation.OTTOS_GROTTO;
    }

    @ConfigItem(
            keyName = "bank",
            name = "Bank",
            hidden = true,
            unhide = "fishingLocation",
            unhideValue = "FISHING_GUILD",
            description = "Whether to bank or not",
            position = 5
    )
    default boolean bank()
    {
        return false;
    }

    @ConfigItem(
            keyName = "debug",
            name = "Debug",
            description = "Prints debugging information",
            position = 6
    )
    default boolean debug()
    {
        return false;
    }
}
