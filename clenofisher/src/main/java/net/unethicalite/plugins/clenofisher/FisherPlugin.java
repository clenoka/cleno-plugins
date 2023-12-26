//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.unethicalite.plugins.clenofisher;

import com.google.inject.Provides;
import java.io.PrintStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.World;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.unethicalite.api.commons.Time;
import net.unethicalite.api.entities.NPCs;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.game.Game;
import net.unethicalite.api.game.Skills;
import net.unethicalite.api.game.Worlds;
import net.unethicalite.api.input.Keyboard;
import net.unethicalite.api.items.Inventory;
import net.unethicalite.api.movement.Movement;
import net.unethicalite.api.movement.pathfinder.Walker;
import net.unethicalite.api.plugins.LoopedPlugin;
import net.unethicalite.api.widgets.Dialog;
import net.unethicalite.client.Static;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Extension
@PluginDescriptor(
        name = "Keegan Fisher",
        description = "3t Fish",
        enabledByDefault = false
)
public class FisherPlugin extends LoopedPlugin
{
    private static final Logger log = LoggerFactory.getLogger(FisherPlugin.class);
    public Client client;
    public long startTime = 0L;
    public int startXP = 0;
    public SecureRandom r;
    public boolean finishedFishing;
    public NPC fishingSpot;
    String status;
    @Inject
    private KeeganFisherConfig config;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private FisherOverlay fisherOverlay;
    private int tickCounter;
    private ArrayList<Integer> worldsList = new ArrayList();
    private int hoppedFrom = 0;
    private int nextWorld;
    private int lastXP;
    private WorldPoint lastLocation;
    private int ticksUntilCast;
    private int ticksFishing;
    private int ticksUntilHerb;
    private boolean updateWorldList;

    public FisherPlugin()
    {
    }

    protected void startUp()
    {
        this.r = new SecureRandom();
        this.startTime = 0L;
        this.startXP = 0;
        this.fishingSpot = null;
        this.finishedFishing = true;
        this.worldsList.clear();
        this.lastLocation = null;
        this.tickCounter = 0;
        this.client = Static.getClient();
        this.overlayManager.add(this.fisherOverlay);
        if (this.worldsList.size() < 1)
            {
            this.InitWorldList();
            }

    }

    private void login(int world)
    {
        if (this.client.getWorldList() == null)
            {
            this.client.loadWorlds();
            }

        if (this.client.getWorld() != world)
            {
            this.client.changeWorld(Worlds.getFirst(world));
            Time.sleep(300L);
            if (this.startTime == 0L)
                {
                Keyboard.pressed(27);
                Keyboard.released(27);
                }
            }

        if (this.client.getWorld() == world)
            {
            this.client.setLoginIndex(2);
            this.client.setUsername(this.config.username());
            this.client.setPassword(this.config.password());
            Keyboard.sendEnter();
            Keyboard.sendEnter();
            Keyboard.sendEnter();
            Time.sleepUntil(() ->
            {
            return Game.isLoggedIn();
            }, 5000 + this.r.nextInt(5000));
            }

    }

    public void InitWorldList()
    {
        this.worldsList.clear();
        if (this.startTime == 0L)
            {
            Worlds.loadWorlds();
            Time.sleepUntil(() ->
            {
            return false;
            }, 300);
            Keyboard.pressed(27);
            Keyboard.released(27);
            }

        List<World> worlds = Worlds.getAll((x) ->
        {
        return x.isNormal() && x.isMembers();
        });

        int index;
        for (index = 0; index < worlds.size(); ++index)
            {
            int worldNumber = ((World) worlds.get(index)).getId();
            if (((World) worlds.get(index)).getLocation() == this.config.Region().getCode())
                {
                PrintStream var10000 = System.out;
                int var10001 = ((World) worlds.get(index)).getId();
                var10000.println("Adding world " + var10001 + " from " + this.config.Region());
                this.worldsList.add(worldNumber);
                }
            }

        if (this.worldsList.size() > 0)
            {
            index = this.r.nextInt(this.worldsList.size());
            this.nextWorld = (Integer) this.worldsList.get(index);
            this.worldsList.remove(index);
            }

    }

    public void stop()
    {
        this.overlayManager.remove(this.fisherOverlay);
        super.stop();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged e)
    {
        if (e.getGroup().equals("keeganfisher") && e.getKey().equals("region"))
            {
            this.updateWorldList = true;
            }

    }

    protected int loop()
    {
        if (this.updateWorldList)
            {
            this.InitWorldList();
            this.updateWorldList = false;
            return 50;
            }
        else if (!Game.isLoggedIn())
            {
            this.login(this.nextWorld);
            return 50;
            }
        else if (Game.isLoggedIn() && this.startTime == 0L && Skills.getExperience(Skill.FISHING) > 0)
            {
            this.startXP = Skills.getExperience(Skill.FISHING);
            this.startTime = System.currentTimeMillis();
            return 50;
            }
        else if (this.MissingEquipmentDialog())
            {
            Game.logout();
            this.stop();
            return 50;
            }
        else if (this.config.hopWorlds() && this.OtherPlayersAround())
            {
            this.HopWorlds();
            return 50;
            }
        else if (Inventory.isFull())
            {
            Game.logout();
            this.stop();
            return 50;
            }
        else if (!this.finishedFishing && Players.getLocal().getAnimation() != -1)
            {
            return 50;
            }
        else
            {
            this.CatchFish();
            return 50;
            }
    }

    private boolean MissingEquipmentDialog()
    {
        if (Dialog.isOpen())
            {
            if (Dialog.getText().contains("You don't have"))
                {
                return true;
                }

            if (Dialog.getText().contains("You need"))
                {
                return true;
                }
            }

        return false;
    }

    public boolean OtherPlayersAround()
    {
        List<Player> otherPlayers = Players.getAll();
        Iterator var2 = otherPlayers.iterator();

        Player player;
        do
            {
            if (!var2.hasNext())
                {
                return false;
                }

            player = (Player) var2.next();
            } while (player.getName().equals("null") || player.equals(Players.getLocal()) || player.distanceTo(Players.getLocal()) >= 10);

        return true;
    }

    public void CatchFish()
    {
        this.fishingSpot = NPCs.getNearest((x) ->
        {
        return !x.isMoving() && (x.hasAction(new String[]{"Use-rod"}) || x.hasAction(new String[]{"Lure"}));
        });
        if (this.fishingSpot != null)
            {
            this.finishedFishing = false;
            this.TarFish(this.fishingSpot);
            }

    }

    public void HopWorlds()
    {
        this.status = "Hopping worlds";
        if (this.worldsList.size() < 1)
            {
            this.InitWorldList();
            }

        int index = this.r.nextInt(this.worldsList.size());
        this.nextWorld = (Integer) this.worldsList.get(index);
        this.worldsList.remove(index);
        if (Game.isLoggedIn())
            {
            this.logout();
            }

    }

    private void SleepTicks(int ticksToSleep)
    {
        int currentTick = this.tickCounter;
        Time.sleepUntil(() ->
        {
        return this.tickCounter == currentTick + ticksToSleep;
        }, ticksToSleep * 650 + this.r.nextInt(173));
    }

    private void FishingSleep()
    {
        this.finishedFishing = false;
        Time.sleepUntil(() ->
        {
        return Players.getLocal().isMoving();
        }, 1000);
        Time.sleepUntil(() ->
        {
        return !Players.getLocal().isMoving();
        }, 5000);
        Time.sleepUntil(() ->
        {
        return this.finishedFishing;
        }, 1200 + this.r.nextInt(173));
    }

    private void TarFish(NPC fishingSpot)
    {
        Item herb = Inventory.getFirst(new int[]{
                249,
                251,
                253,
                255
        });
        Item tar = Inventory.getFirst(new int[]{1939});
        if (herb != null && tar != null)
            {
            this.DropFish();
            if (Players.getLocal().distanceTo(fishingSpot) <= 1)
                {
                this.status = "Tar+Herb";
                this.Print("Using tar on herb (" + this.tickCounter + ")");
                tar.useOn(herb);
                this.ticksUntilHerb = 3;
                Time.sleepUntil(() ->
                {
                return Players.getLocal().getAnimation() == 5249 || Players.getLocal().getAnimation() == 712;
                }, 400 + this.r.nextInt(173));
                }

            this.status = "Fishing";
            if (fishingSpot.hasAction(new String[]{"Lure"}))
                {
                this.Print("Clicking Lure (" + this.tickCounter + ")");
                fishingSpot.interact("Lure");
                }
            else if (fishingSpot.hasAction(new String[]{"Use-rod"}))
                {
                this.Print("Clicking Lure (" + this.tickCounter + ")");
                fishingSpot.interact("Use-rod");
                }
            else
                {
                this.Print("Walking to avoid wasting herb (" + this.tickCounter + ")");
                WorldPoint near = Walker.nearestWalkableTile(Players.getLocal().getWorldLocation(), (x) ->
                {
                return !x.equals(Players.getLocal().getWorldLocation());
                });
                Movement.walk(near);
                this.fishingSpot = null;
                }

            this.FishingSleep();
            }
        else
            {
            Item grimy = Inventory.getFirst(new int[]{
                    199,
                    201,
                    203,
                    255
            });
            if (grimy != null)
                {
                this.Print("Clicking Clean (" + this.tickCounter + ")");
                grimy.interact("Clean");
                Time.sleepUntil(() ->
                {
                return Inventory.getFirst(new int[]{
                        249,
                        251,
                        253,
                        255
                }) != null;
                }, 1000 + this.r.nextInt(173));
                this.SleepTicks(1);
                }
            else
                {
                this.logout();
                this.stop();
                }

            }
    }

    private void logout()
    {
        this.status = "Logging out";
        this.hoppedFrom = this.client.getWorld();
        Widget logoutButton = this.client.getWidget(182, 8);
        Widget logoutDoorButton = this.client.getWidget(69, 23);
        if (logoutButton != null)
            {
            this.Print("Clicking Logout (" + this.tickCounter + ")");
            logoutButton.interact("Logout");
            Time.sleepUntil(() ->
            {
            return !Game.isLoggedIn();
            }, 173 + this.r.nextInt(247));
            }
        else if (logoutDoorButton != null)
            {
            this.Print("Clicking Logout (" + this.tickCounter + ")");
            logoutDoorButton.interact("Logout");
            }

    }

    @Subscribe
    private void onGameTick(GameTick tick)
    {
        --this.ticksUntilHerb;
        if (this.ticksUntilHerb <= 0)
            {
            this.finishedFishing = true;
            }

        ++this.tickCounter;
        --this.ticksUntilCast;
    }

    @Subscribe
    private void onClientTick(ClientTick tick)
    {
    }

    public void Print(String toPrint)
    {
        if (this.config.debug())
            {
            System.out.println(toPrint);
            }

    }

    public void DropFish()
    {
        List<Item> fish = Inventory.getAll(new int[]{
                335,
                331,
                11330,
                11328,
                11332
        });
        if (fish.size() > 1)
            {
            this.status = "Dropping fish";
            Item fish1 = (Item) fish.get(0);
            Item fish2 = (Item) fish.get(1);
            if (fish1 != null)
                {
                this.Print("Dropping fish 1 (" + this.tickCounter + ")");
                fish1.drop();
                if (fish2 != null)
                    {
                    this.Print("Dropping fish 2 (" + this.tickCounter + ")");
                    fish2.drop();
                    }
                }
            }
        else if (fish.size() == 1)
            {
            this.Print("Dropping fish 1 (" + this.tickCounter + ")");
            ((Item) fish.get(0)).drop();
            }

    }

    @Provides
    KeeganFisherConfig provideConfig(ConfigManager configManager)
    {
        return (KeeganFisherConfig) configManager.getConfig(KeeganFisherConfig.class);
    }
}
