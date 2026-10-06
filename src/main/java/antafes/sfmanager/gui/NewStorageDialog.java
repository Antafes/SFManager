package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.SFManager;
import antafes.sfmanager.gui.event.RegisterEscapeCloseOperationEvent;
import antafes.utilities.language.LanguageInterface;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.swing.JDialog;
import java.awt.Frame;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NewStorageDialog extends JDialog
{
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private Configuration configuration;
    private LanguageInterface language;

    public NewStorageDialog(Frame owner, boolean modal)
    {
        super(owner, modal);
    }

    @PostConstruct
    private void initializeDialog()
    {
        this.language = this.configuration.getLanguageObject();
        this.setResizable(false);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        SFManager.getDispatcher().dispatch(new RegisterEscapeCloseOperationEvent(this));

        NewStoragePanel panel = this.applicationContext.getBean(
            NewStoragePanel.class,
            this,
            this.configuration
        );
        this.add(panel);
        this.setTitle(this.language.translate("newStorage"));
        this.pack();
    }
}
