/*
 * This file is part of SFManager.
 *
 * SFManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SFManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SFManager. If not, see <http://www.gnu.org/licenses/>.
 *
 * @package SFManager
 * @author Marian Pollzien <map@wafriv.de>
 * @copyright (c) 2026, Marian Pollzien
 * @license https://www.gnu.org/licenses/lgpl.html LGPLv3
 */

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
