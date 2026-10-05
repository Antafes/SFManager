package antafes.sfmanager.gui.event;

import scripts.laniax.framework.event_dispatcher.EventListener;

import java.util.function.Consumer;

public class RegisterEscapeCloseOperationListener extends EventListener<RegisterEscapeCloseOperationEvent>
{
    public RegisterEscapeCloseOperationListener()
    {
    }

    public RegisterEscapeCloseOperationListener(Consumer<RegisterEscapeCloseOperationEvent> consumer)
    {
        super(consumer);
    }

    public RegisterEscapeCloseOperationListener(
        Consumer<RegisterEscapeCloseOperationEvent> consumer, int priority
    )
    {
        super(consumer, priority);
    }
}
