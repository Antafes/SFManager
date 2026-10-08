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

package antafes.sf.manager.gui.focus;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DynamicFocusTraversalPolicy extends FocusTraversalPolicy
{
    private final List<Component> order;

    public DynamicFocusTraversalPolicy(List<Component> order)
    {
        this.order = List.copyOf(order);
    }

    @Override
    public Component getComponentAfter(Container container, Component component)
    {
        return this.nextFocusableComponent(component, 1);
    }

    @Override
    public Component getComponentBefore(Container container, Component component)
    {
        return this.nextFocusableComponent(component, -1);
    }

    @Override
    public Component getFirstComponent(Container container)
    {
        return this.findFocusableComponent(-1, 1);
    }

    @Override
    public Component getLastComponent(Container container)
    {
        return this.findFocusableComponent(0, -1);
    }

    @Override
    public Component getDefaultComponent(Container container)
    {
        return this.getFirstComponent(container);
    }

    private Component nextFocusableComponent(Component component, int direction)
    {
        int currentIndex = this.indexOf(component);
        return this.findFocusableComponent(
            currentIndex < 0 ? (direction > 0 ? -1 : 0) : currentIndex,
            direction
        );
    }

    private Component findFocusableComponent(int currentIndex, int direction)
    {
        for (int offset = 1; offset <= this.order.size(); offset++) {
            int index = Math.floorMod(currentIndex + direction * offset, this.order.size());
            Component candidate = this.order.get(index);
            if (candidate.isShowing() && candidate.isEnabled() && candidate.isFocusable()) {
                return candidate;
            }
        }
        return null;
    }

    private int indexOf(Component component)
    {
        for (int index = 0; index < this.order.size(); index++) {
            Component candidate = this.order.get(index);
            if (component == candidate || SwingUtilities.isDescendingFrom(component, candidate)) {
                return index;
            }
        }
        return -1;
    }
}
