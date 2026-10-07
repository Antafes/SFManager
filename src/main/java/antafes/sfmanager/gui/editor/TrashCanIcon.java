package antafes.sfmanager.gui.editor;

import javax.swing.*;
import java.awt.*;

final class TrashCanIcon implements Icon
{
    @Override
    public int getIconWidth()
    {
        return 16;
    }

    @Override
    public int getIconHeight()
    {
        return 16;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y)
    {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        try {
            graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );
            Color color = component.isEnabled()
                ? component.getForeground()
                : UIManager.getColor("Label.disabledForeground");
            graphics2D.setColor(color == null ? component.getForeground() : color);
            graphics2D.setStroke(new BasicStroke(1.5f));
            graphics2D.drawLine(x + 2, y + 4, x + 14, y + 4);
            graphics2D.drawLine(x + 5, y + 2, x + 11, y + 2);
            graphics2D.drawLine(x + 7, y + 2, x + 7, y + 1);
            graphics2D.drawLine(x + 9, y + 2, x + 9, y + 1);
            graphics2D.drawLine(x + 4, y + 6, x + 5, y + 14);
            graphics2D.drawLine(x + 12, y + 6, x + 11, y + 14);
            graphics2D.drawLine(x + 5, y + 14, x + 11, y + 14);
            graphics2D.drawLine(x + 7, y + 7, x + 7, y + 12);
            graphics2D.drawLine(x + 9, y + 7, x + 9, y + 12);
        } finally {
            graphics2D.dispose();
        }
    }
}
