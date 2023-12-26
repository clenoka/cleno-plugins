package io.reisub.unethicalite.zulrah.overlays;

import com.google.common.base.Strings;
import io.reisub.unethicalite.zulrah.ChaosZulrah;
import io.reisub.unethicalite.zulrah.ZulrahConfig;
import io.reisub.unethicalite.zulrah.rotationutils.ZulrahNpc;
import io.reisub.unethicalite.zulrah.util.OverlayUtils;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Objects;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.config.FontType;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class SceneOverlay extends Overlay
{

    private final Client client;
    private final ChaosZulrah plugin;
    private final ZulrahConfig config;
    private final SkillIconManager skillIconManager;

    @Inject
    private SceneOverlay(Client client, ChaosZulrah plugin, ZulrahConfig config,
            SkillIconManager skillIconManager)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.skillIconManager = skillIconManager;
        setPriority(OverlayPriority.HIGH);
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Font prevFont = graphics.getFont();
        graphics.setFont(config.fontType().getFont());
        if (plugin.getZulrahNpc() != null && !plugin.getZulrahNpc().isDead())
            {
            renderPrayerConservation(graphics);
            renderZulrahTicks(graphics);
            renderZulrahTile(graphics);
            renderProjectiles(graphics);
            renderToxicClouds(graphics);
            }

        graphics.setFont(prevFont);
        return null;
    }


    private String getZulrahNextString()
    {
        return plugin.getCurrentRotation() != null ? "Next" : "P. Next";
    }

    private void drawZulrahTile(Graphics2D graphics, ZulrahNpc zulrahNpc, String addonText,
            int offset)
    {
        if (zulrahNpc != null)
            {
            LocalPoint localPoint = zulrahNpc.getZulrahLocation().toLocalPoint();
            Polygon tileAreaPoly = Perspective.getCanvasTileAreaPoly(client, localPoint, 5);
            OverlayUtils.renderPolygon(graphics, tileAreaPoly, zulrahNpc.getType().getColor(),
                    config.outlineWidth(), config.fillAlpha());
            Point basePoint = Perspective.localToCanvas(client, localPoint, client.getPlane(), 0);
            if (basePoint != null)
                {
                int bx = basePoint.getX();
                int by = basePoint.getY();
                String text = getZulrahPhaseString(zulrahNpc, addonText);
                Rectangle2D textBounds = graphics.getFontMetrics().getStringBounds(text, graphics);
                Point textLocation = new Point(bx - (int) textBounds.getWidth() / 2, by - offset);
                Color color = zulrahNpc.getType().getColor();
                ZulrahConfig config = this.config;
                Objects.requireNonNull(config);
                OverlayUtils.renderTextLocation(graphics, textLocation, text, color, config::textOutline);
                if (this.config.phaseHats())
                    {
                    BufferedImage icon = skillIconManager.getSkillImage(zulrahNpc.getType().getSkill(),
                            this.config
                                    .fontType() != FontType.BOLD);
                    int imgPx = bx - icon.getWidth() / 2;
                    int imgPy = by - icon.getHeight() / 2 - offset;
                    Point imgPoint = new Point(imgPx, imgPy);
                    int imgX = imgPoint.getX() - graphics.getFontMetrics().stringWidth(text) / 2 - 15;
                    int imgY = imgPoint.getY() - icon.getHeight() / 2 + 1;
                    graphics.drawImage(icon, imgX, imgY, (ImageObserver) null);
                    }
                }

            }
    }

    private String getZulrahPhaseString(ZulrahNpc npc, String addonText)
    {
        boolean strip = !config.phaseTags() || Strings.isNullOrEmpty(addonText);
        if (npc.isJad())
            {
            return strip ? "Jad" : "[" + addonText + "] Jad";
            }
        else
            {
            return strip ? npc.getType().getName() : "[" + addonText + "] " + npc.getType().getName();
            }
    }


    private void drawTile(Graphics2D graphics, LocalPoint localPoint, String text, Color color,
            int offset)
    {
        if (localPoint != null && !Strings.isNullOrEmpty(text))
            {
            Point textLocation = Perspective.getCanvasTextLocation(client, graphics, localPoint, text, 0);
            Point txtLoc = new Point(textLocation.getX(), textLocation.getY() - offset);
            Color color2 = new Color(color.getRed(), color.getGreen(), color.getBlue());
            ZulrahConfig config = this.config;
            Objects.requireNonNull(config);
            OverlayUtils.renderTextLocation(graphics, txtLoc, text, color2, config::textOutline);
            Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
            OverlayUtils.renderPolygon(graphics, tilePoly, color, this.config.outlineWidth(),
                    this.config.fillAlpha());
            }
    }

    private void renderPrayerConservation(Graphics2D graphics)
    {
        if (config.prayerConservation())
            {
            Player player = client.getLocalPlayer();
            if (player != null && (plugin.getZulrahNpc().getInteracting() == null
                                           || plugin.getZulrahNpc().getInteracting() != client.getLocalPlayer())
                        && player.getOverheadIcon() != null)
                {
                String conserveStr = "Turn off overheads to conserve prayer!";
                Point textLocation = player.getCanvasTextLocation(graphics,
                        "Turn off overheads to conserve prayer!", player.getLogicalHeight() - 22);
                Color color = Color.RED;
                ZulrahConfig config = this.config;
                Objects.requireNonNull(config);
                OverlayUtils.renderTextLocation(graphics, textLocation,
                        "Turn off overheads to conserve prayer!", color, config::textOutline);
                }

            }
    }

    private void renderZulrahTicks(Graphics2D graphics)
    {
        if (config.phaseTickCounter() || config.attackTickCounter())
            {
            StringBuilder sb = new StringBuilder();
            sb = sb.append(
                            config.phaseTickCounter() && plugin.getPhaseTicks() >= 0
                                    ? plugin.getPhaseTicks() : "")
                         .append(
                                 config.phaseTickCounter() && config.attackTickCounter() && plugin.getPhaseTicks() >= 0
                                         && plugin.getAttackTicks() >= 0 ? " : " : "").append(
                            config.attackTickCounter() && plugin.getAttackTicks() >= 0 ? plugin.getAttackTicks()
                                    : "");
            if (!Strings.isNullOrEmpty(sb.toString()))
                {
                Point textLocation = plugin.getZulrahNpc().getCanvasTextLocation(graphics, sb.toString(),
                        plugin.getZulrahNpc().getLogicalHeight() - 130);
                String string = sb.toString();
                Color tickCounterColors = config.tickCounterColors();
                ZulrahConfig config = this.config;
                Objects.requireNonNull(config);
                OverlayUtils.renderTextLocation(graphics, textLocation, string, tickCounterColors,
                        config::textOutline);
                }
            }
    }

    private void renderZulrahTile(Graphics2D graphics)
    {
        if (config.displayZulrahTile())
            {
            Polygon tileAreaPoly = Perspective.getCanvasTileAreaPoly(client,
                    plugin.getZulrahNpc().getLocalLocation(), 5);
            OverlayUtils.renderPolygon(graphics, tileAreaPoly, config.zulrahTileColor(),
                    config.outlineWidth(), config.fillAlpha());
            }
    }

    private void renderProjectiles(Graphics2D graphics)
    {
        if (config.displayProjectiles() && plugin.getProjectilesMap().size() > 0)
            {
            plugin.getProjectilesMap().forEach((localPoint, ticks) ->
            {
            Point textLocation = Perspective.getCanvasTextLocation(client, graphics, localPoint, "#",
                    0);
            String string = Integer.toString(ticks);
            Color color = ticks > 0 ? Color.WHITE : Color.RED;
            ZulrahConfig config = this.config;
            Objects.requireNonNull(config);
            OverlayUtils.renderTextLocation(graphics, textLocation, string, color, config::textOutline);
            Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
            OverlayUtils.renderPolygon(graphics, tilePoly, this.config.projectilesColor(),
                    this.config.outlineWidth(), this.config
                                                        .fillAlpha());
            });
            }
    }

    private void renderToxicClouds(Graphics2D graphics)
    {
        if (!config.displayToxicClouds() || plugin.getToxicCloudsMap().size() <= 0)
            {
            return;
            }
        plugin.getToxicCloudsMap().forEach((obj, ticks) ->
        {
        LocalPoint localPoint = obj.getLocalLocation();
        Polygon tileAreaPoly = Perspective.getCanvasTileAreaPoly(client, localPoint, 3);
        OverlayUtils.renderPolygon(graphics, tileAreaPoly, config.toxicCloudsColor(),
                config.outlineWidth(), config.fillAlpha());
        String ticksString = Integer.toString(ticks);
        Point textLocation = Perspective.getCanvasTextLocation(client, graphics, localPoint,
                ticksString, 0);
        OverlayUtils.renderTextLocation(graphics, textLocation, ticksString,
                ticks > 0 ? Color.RED : Color.GREEN, config::textOutline);
        });
    }
}
