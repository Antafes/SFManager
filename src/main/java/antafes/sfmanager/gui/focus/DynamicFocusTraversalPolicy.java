package antafes.sfmanager.gui.focus;

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
