/*
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.unethicalite.plugins.brewdrinker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;


@ConfigGroup("brewdrinker")
public interface BrewDrinkerConfig extends Config
{
    @ConfigItem(
            position = 1,
            keyName = "drinkRestore",
            name = "Drink Restore pots",
            description = "The restore pots will be drank"
    )
    default boolean drinkRestore()
    {
        return false;
    }

    @Range(
            min = 1,
            max = 98
    )
    @ConfigItem(
            position = 2,
            hidden = true,
            unhide = "drinkRestore",
            keyName = "restoreSetpoint",
            name = "Prayer Setpoint",
            description = "When prayer <= this value then sip a restore"
    )
    default int prayerSetpoint()
    {
        return 15;
    }
    @ConfigItem(
            position = 3,
            keyName = "drinkSara",
            name = "Drink Saradomin Brews",
            description = "The sara brews will be drank"
    )
    default boolean drinkSara()
    {
        return false;
    }
    @Range(
            min = 1,
            max = 98
    )
    @ConfigItem(
            position = 4,
            hidden = true,
            unhide = "drinkSara",
            keyName = "healSetpoint",
            name = "HP Setpoint",
            description = "When hitpoints <= this value then drink 3x sara brew"
    )
    default int healSetpoint()
    {
        return 75;
    }
}
