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

package antafes.sf.manager.gui.util;

import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@UtilityClass
public final class PersistentToolTip
{
    /**
     * Keeps tooltips of the component visible until the mouse moves or leaves the component.
     */
    public void apply(JComponent component)
    {
        component.addMouseListener(new MouseAdapter() {
            private int previousDismissDelay;

            @Override
            public void mouseEntered(MouseEvent event)
            {
                ToolTipManager manager = ToolTipManager.sharedInstance();
                this.previousDismissDelay = manager.getDismissDelay();
                manager.setDismissDelay(Integer.MAX_VALUE);
            }

            @Override
            public void mouseExited(MouseEvent event)
            {
                ToolTipManager.sharedInstance().setDismissDelay(this.previousDismissDelay);
            }
        });
    }
}
