// Decompiled with: CFR 0.151
// Class Version: 11

package io.reisub.unethicalite.zulrah.overlays;

import io.reisub.unethicalite.zulrah.ChaosZulrah;
import io.reisub.unethicalite.zulrah.ZulrahConfig;
import java.awt.*;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.TextComponent;

public class PhaseOverlay
        extends OverlayPanel
{

    private static final int DEFAULT_DIMENSION = 55;
    private static final int GAP = 1;
    private final ChaosZulrah plugin;
    private final ZulrahConfig config;

    @Inject
    private PhaseOverlay(ChaosZulrah plugin, ZulrahConfig config)
    {
        this.plugin = plugin;
        this.config = config;
        this.setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        this.setPriority(OverlayPriority.HIGH);
        this.setResizable(false);
        this.panelComponent.setPreferredSize(new Dimension(56, 56));
        this.panelComponent.setBorder(new Rectangle());
        this.panelComponent.setGap(new Point(1, 1));
        this.panelComponent.setOrientation(ComponentOrientation.HORIZONTAL);
    }

    private String getNextString()
    {
        return plugin.getCurrentRotation() != null ? "Next" : "P. Next";
    }

    private void displayRotationName(Graphics2D graphics)
    {
        Rectangle bounds = panelComponent.getBounds();
        String text =
                plugin.getCurrentRotation() != null ? plugin.getCurrentRotation().getRotationName()
                        : "Unidentified";
        TextComponent textComponent = new TextComponent();
        textComponent.setPosition(
                new Point(bounds.x + (bounds.width - graphics.getFontMetrics().stringWidth(text)) / 2,
                        bounds.y + 1));
        textComponent.setText(text);
        textComponent.setColor(plugin.getCurrentRotation() != null ? Color.GREEN : Color.YELLOW);
        textComponent.setOutline(config.textOutline());
        textComponent.render(graphics);
    }
}
