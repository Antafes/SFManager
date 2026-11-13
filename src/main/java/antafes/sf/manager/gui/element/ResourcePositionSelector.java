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

import antafes.sf.manager.entity.ResourceNode;

import javax.swing.*;
import java.awt.Component;
import java.util.List;
import java.util.Objects;

public class ResourcePositionSelector extends JComboBox<Object>
{
    public ResourcePositionSelector()
    {
        this.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            )
            {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                this.setText(value instanceof ResourceNode node
                    ? Math.round(node.getX()) + ", " + Math.round(node.getY())
                    : Objects.toString(value, ""));
                return this;
            }
        });
    }

    public void setPositions(List<ResourceNode> positions, String currentPosition)
    {
        this.removeAllItems();
        this.addItem(null);
        positions.forEach(this::addItem);
        if (currentPosition == null || currentPosition.isBlank()) {
            this.setSelectedItem(null);
            return;
        }
        ResourceNode matchingNode = positions.stream()
            .filter(node -> currentPosition.equals(node.getKey()))
            .findFirst()
            .orElse(null);
        if (matchingNode != null) {
            this.setSelectedItem(matchingNode);
        } else {
            this.addItem(currentPosition);
            this.setSelectedItem(currentPosition);
        }
    }

    public String getPosition()
    {
        Object selectedPosition = this.getSelectedItem();
        if (selectedPosition instanceof ResourceNode node) {
            return node.getKey();
        }
        return selectedPosition instanceof String value && !value.isBlank() ? value : null;
    }
}
