package antafes.sfmanager.gui.event;

import scripts.laniax.framework.event_dispatcher.EventListener;

import java.util.function.Consumer;

public class NewFactoryListener extends EventListener<NewFactoryEvent>
{
    public NewFactoryListener()
    {
    }

    public NewFactoryListener(Consumer<NewFactoryEvent> consumer)
    {
        super(consumer);
    }

    public NewFactoryListener(Consumer<NewFactoryEvent> consumer, int priority)
    {
        super(consumer, priority);
    }
}
