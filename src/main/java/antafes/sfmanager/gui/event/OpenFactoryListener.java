package antafes.sfmanager.gui.event;

import scripts.laniax.framework.event_dispatcher.EventListener;

import java.util.function.Consumer;

public class OpenFactoryListener extends EventListener<OpenFactoryEvent>
{
    public OpenFactoryListener()
    {
    }

    public OpenFactoryListener(Consumer<OpenFactoryEvent> consumer)
    {
        super(consumer);
    }

    public OpenFactoryListener(Consumer<OpenFactoryEvent> consumer, int priority)
    {
        super(consumer, priority);
    }
}
