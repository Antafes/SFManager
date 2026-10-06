package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.SFManager;
import antafes.sfmanager.gui.event.RegisterEscapeCloseOperationEvent;
import antafes.utilities.language.LanguageInterface;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NewFactoryDialog extends JDialog
{
    private final LanguageInterface language;

    public NewFactoryDialog(Frame owner, boolean modal, ApplicationContext applicationContext, Configuration configuration)
    {
        super(owner, modal);
        this.language = configuration.getLanguageObject();
        this.setResizable(false);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        SFManager.getDispatcher().dispatch(new RegisterEscapeCloseOperationEvent(this));

        NewFactoryPanel panel = applicationContext.getBean(NewFactoryPanel.class, this, configuration);
        this.add(panel);
        this.setTitle(this.language.translate("newFactory"));
        this.pack();
        this.setLocationRelativeTo(owner);
    }
}
