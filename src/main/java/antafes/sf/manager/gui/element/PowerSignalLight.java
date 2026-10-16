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

package antafes.sf.manager.gui.element;

import antafes.sf.manager.gui.util.PersistentToolTip;
import antafes.sf.manager.service.dto.PowerStatus;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

public class PowerSignalLight extends JComponent
{
    private static final int LAMP_SIZE = 22;
    private static final int PADDING = 6;
    private static final int GAP = 6;
    private static final int HOUSING_WIDTH = LAMP_SIZE + 2 * PADDING;
    private static final int HOUSING_HEIGHT = 3 * LAMP_SIZE + 2 * GAP + 2 * PADDING;
    private static final Color HOUSING_COLOR = new Color(40, 40, 40);

    @Getter
    private PowerStatus status = PowerStatus.RED;

    public PowerSignalLight()
    {
        this.setPreferredSize(new Dimension(HOUSING_WIDTH, HOUSING_HEIGHT));
        this.setMinimumSize(this.getPreferredSize());
        PersistentToolTip.apply(this);
    }

    public void setStatus(PowerStatus status)
    {
        this.status = status;
        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x = (this.getWidth() - HOUSING_WIDTH) / 2;
        int y = (this.getHeight() - HOUSING_HEIGHT) / 2;
        g2.setColor(HOUSING_COLOR);
        g2.fillRoundRect(x, y, HOUSING_WIDTH, HOUSING_HEIGHT, 16, 16);

        PowerStatus[] lamps = {PowerStatus.RED, PowerStatus.YELLOW, PowerStatus.GREEN};
        for (int index = 0; index < lamps.length; index++) {
            Color color = getColor(lamps[index]);
            g2.setColor(lamps[index] == this.status ? color : color.darker().darker().darker());
            g2.fillOval(x + PADDING, y + PADDING + index * (LAMP_SIZE + GAP), LAMP_SIZE, LAMP_SIZE);
        }

        g2.dispose();
    }

    private static Color getColor(PowerStatus status)
    {
        return switch (status) {
            case RED -> new Color(230, 30, 30);
            case YELLOW -> new Color(250, 210, 0);
            case GREEN -> new Color(40, 200, 70);
        };
    }
}
