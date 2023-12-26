//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.unethicalite.plugins.clenofisher;

import com.google.inject.Singleton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.text.NumberFormat;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.unethicalite.api.entities.Players;
import net.unethicalite.api.game.Skills;
import net.unethicalite.api.widgets.Widgets;

@Singleton
class FisherOverlay extends Overlay
{
    private static final Font FONT = FontManager.getRunescapeFont().deriveFont(1, 16.0F);
    private static final Font SMALL_FONT = FontManager.getRunescapeFont().deriveFont(1, 12.0F);
    private static final Color RED = new Color(221, 44, 0);
    private static final Color GREEN = new Color(0, 200, 83);
    private static final Color YELLOW = new Color(255, 214, 0);
    private final FisherPlugin plugin;
    private final KeeganFisherConfig config;
    @Inject
    private Client client;
    private NumberFormat myFormat;

    @Inject
    private FisherOverlay(Client client, FisherPlugin plugin, KeeganFisherConfig config)
    {
        super(plugin);
        this.setPosition(OverlayPosition.DYNAMIC);
        this.setLayer(OverlayLayer.ABOVE_WIDGETS);
        this.setPriority(OverlayPriority.HIGHEST);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.myFormat = NumberFormat.getInstance();
        this.myFormat.setGroupingUsed(true);
    }

    public Dimension render(Graphics2D graphics)
    {
        int secondsRunning = (int) (System.currentTimeMillis() - this.plugin.startTime) / 1000;
        double hoursAsDouble = (double) secondsRunning / 3600.0;
        String runtimeString = "Runtime: ";
        int xpGained = Skills.getExperience(Skill.FISHING) - this.plugin.startXP;
        String statusString = "Status: " + this.plugin.status;
        int var10000 = Players.getLocal().getAnimation();
        String animationString = "Animation: " + var10000 + "(" + Players.getLocal().getAnimationFrame() + ")";
        String xpString = "XP: " + xpGained + " (" + (int) ((double) xpGained / hoursAsDouble / 1000.0) + "k/hr)";
        List<Widget> chatboxRoot = Widgets.get(162);
        Widget box = (Widget) chatboxRoot.get(34);
        int x = 0;
        int y = 0;
        if (box != null)
            {
            Rectangle slotBounds = box.getBounds();
            x = (int) slotBounds.getMinX() + 20;
            y = (int) slotBounds.getMinY() + 30;
            Point mouse = this.plugin.client.getCanvas().getMousePosition();
            graphics.setColor(new Color(0, 0, 0, 155));
            if (mouse != null && slotBounds.contains(mouse))
                {
                return null;
                }

            graphics.fill(slotBounds);
            }

        int hours = secondsRunning / 3600;
        secondsRunning -= hours * 3600;
        int minutes = secondsRunning / 60;
        secondsRunning -= minutes * 60;
        String secondString = "" + secondsRunning;
        String minuteString = "" + minutes;
        String hourString = "" + hours;
        if (secondsRunning < 10)
            {
            secondString = "0" + secondsRunning;
            if (secondsRunning == 0)
                {
                secondString = "00";
                }
            }

        if (minutes < 10)
            {
            minuteString = "0" + minutes;
            if (minutes == 0)
                {
                minuteString = "00";
                }
            }

        if (hours < 10)
            {
            hourString = "0" + hours;
            if (hours == 0)
                {
                hourString = "00";
                }
            }

        runtimeString = runtimeString + hourString + ":" + minuteString + ":" + secondString;
        graphics.setFont(FONT);
        graphics.setColor(Color.BLACK);
        graphics.drawString(runtimeString, x + 1, y + 1);
        graphics.setColor(YELLOW);
        graphics.drawString(runtimeString, x, y);
        int textX = x + 0;
        int textY = y + 20;
        graphics.setColor(Color.BLACK);
        graphics.drawString(statusString, textX + 1, textY + 1);
        graphics.setColor(YELLOW);
        graphics.drawString(statusString, textX, textY);
        textX += 0;
        textY += 20;
        graphics.setColor(Color.BLACK);
        graphics.drawString(animationString, textX + 1, textY + 1);
        graphics.setColor(YELLOW);
        graphics.drawString(animationString, textX, textY);
        textX = x + 200;
        graphics.setFont(FONT);
        graphics.setColor(Color.BLACK);
        graphics.drawString(xpString, textX + 1, y + 1);
        graphics.setColor(YELLOW);
        graphics.drawString(xpString, textX, y);
        return null;
    }
}
