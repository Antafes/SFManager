package antafes.sfmanager.gui.event;

import scripts.laniax.framework.event_dispatcher.EventListener;

import java.util.function.Consumer;

public class NewProductionListener extends EventListener<NewProductionEvent>
{
    public NewProductionListener()
    {
    }

    public NewProductionListener(Consumer<NewProductionEvent> consumer)
    {
        super(consumer);
    }

    public NewProductionListener(Consumer<NewProductionEvent> consumer, int priority)
    {
        super(consumer, priority);
    }
}
