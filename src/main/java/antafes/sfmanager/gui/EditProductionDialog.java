package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.SFManager;
import antafes.sfmanager.gui.event.RegisterEscapeCloseOperationEvent;
import antafes.utilities.language.LanguageInterface;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EditProductionDialog extends JDialog
{
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private Configuration configuration;
    private final UUID productionId;
    private LanguageInterface language;
    @Getter
    private boolean saved;

    public EditProductionDialog(Frame owner, boolean modal, UUID productionId)
    {
        super(owner, modal);
        this.productionId = productionId;
    }

    public void setSaved()
    {
        this.saved = true;
    }

    @PostConstruct
    private void initializeDialog()
    {
        this.setResizable(false);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.language = this.configuration.getLanguageObject();
        this.initializeLookAndFeel();
        this.initComponents();
        this.setTitle(this.language.translate("editProduction"));
        this.pack();
    }

    private void initializeLookAndFeel()
    {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | UnsupportedLookAndFeelException exception) {
            Logger.getLogger(SFManager.class.getName()).log(Level.SEVERE, null, exception);
        }
    }

    private void initComponents()
    {
        SFManager.getDispatcher().dispatch(new RegisterEscapeCloseOperationEvent(this));
        this.add(this.applicationContext.getBean(
            EditProductionPanel.class,
            this,
            this.configuration,
            this.productionId
        ));
    }
}
