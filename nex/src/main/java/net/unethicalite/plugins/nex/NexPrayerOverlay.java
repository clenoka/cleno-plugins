package net.unethicalite.plugins.nex;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.*;
import net.unethicalite.client.Static;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;

@Singleton
@Slf4j
class NexPrayerOverlay extends Overlay
{
    private final Client client;
    private final NexPlugin plugin;
    private final NexConfig config;

    @Inject
    private NexPrayerOverlay(final Client client, final NexPlugin plugin, final NexConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGHEST);
    }

    @Inject
    private ClientThread clientThread;

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.isInFight() || plugin.getNex() == null || client.getLocalPlayer() == null)
            {
            return null;
            }

        if (!config.prayerHelper().showWidgetHelper())
            {
            return null;
            }

        var prayer = NexPhase.phasePrayer(plugin.getCurrentPhase(), client.getLocalPlayer(), plugin.getNex(), plugin.isTrappedInIce());
        // Cleno Prayer
        if (prayer != null && !client.isPrayerActive(prayer))
            {
            Static.getClient().invokeMenuAction("Prayer", "", 1, 57, -1, 10551359);
            Widget prayerWidget = client.getWidget(prayer.getWidgetInfo());
            if (prayerWidget != null && !prayerWidget.isHidden())
                {
                clientThread.invokeLater(() -> prayerWidget.interact("Activate"));

                Static.getClient().invokeMenuAction("Inventory", "", 1, 57, -1, 10551359);
                }
            }
        // Cleno Prayer end

        final Widget meleePrayerWidget = client.getWidget(WidgetInfo.PRAYER_PROTECT_FROM_MELEE);
        final Widget rangePrayerWidget = client.getWidget(WidgetInfo.PRAYER_PROTECT_FROM_MISSILES);
        final Widget magicPrayerWidget = client.getWidget(WidgetInfo.PRAYER_PROTECT_FROM_MAGIC);


        var prayerWidgetHidden = meleePrayerWidget == null
                                         || rangePrayerWidget == null
                                         || magicPrayerWidget == null
                                         || meleePrayerWidget.isHidden()
                                         || rangePrayerWidget.isHidden()
                                         || magicPrayerWidget.isHidden();


        if (!prayerWidgetHidden || config.alwaysShowPrayerHelper())
            {
            if (client.isPrayerActive(prayer) && !config.indicatePrayerIsCorrect())
                {
                return null;
                }
            Color color = client.isPrayerActive(prayer) ? Color.GREEN : Color.RED;
            renderPrayerOverlay(graphics, client, prayer, color);
            }

        return null;
    }

    private void ensurePrayerTabOpen()
    {
        Widget tabPrayer = client.getWidget(WidgetInfo.RESIZABLE_VIEWPORT_PRAYER_TAB);

        if (tabPrayer == null || tabPrayer.isHidden())
            {
            Widget widgetTab = client.getWidget(WidgetInfo.RESIZABLE_VIEWPORT_PRAYER_TAB);
            if (widgetTab != null && !widgetTab.isHidden())
                {
                widgetTab.interact("Prayer");
                }
            }
    }

    private void renderPrayerOverlay(Graphics2D graphics, Client client, Prayer prayer, Color color)
    {
        Widget prayerWidget = client.getWidget(prayer.getWidgetInfo());

        if (prayerWidget == null)
            {
            return;
            }

        final Rectangle prayerRectangle = new Rectangle((int) prayerWidget.getBounds().getWidth(), (int) prayerWidget.getBounds().getHeight());
        prayerRectangle.translate((int) prayerWidget.getBounds().getX(), (int) prayerWidget.getBounds().getY());

        OverlayUtil.renderPolygon(graphics, prayerRectangle, color);
    }
}