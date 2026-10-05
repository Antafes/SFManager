package antafes.sfmanager.gui.event;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import scripts.laniax.framework.event_dispatcher.Event;

import javax.swing.*;

@Getter
@RequiredArgsConstructor
public class RegisterEscapeCloseOperationEvent extends Event
{
    @NonNull
    private JDialog dialog;
}
