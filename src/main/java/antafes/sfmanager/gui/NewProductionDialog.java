package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.SFManager;
import antafes.sfmanager.gui.event.RegisterEscapeCloseOperationEvent;
import antafes.utilities.language.LanguageInterface;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NewProductionDialog extends JDialog
{
    private final ApplicationContext applicationContext;
    private final LanguageInterface language;
    private final Configuration configuration;

    public NewProductionDialog(
        Frame owner,
        boolean modal,
        ApplicationContext applicationContext,
        Configuration configuration
    )
    {
        super(owner, modal);
        this.applicationContext = applicationContext;
        this.configuration = configuration;

        this.setResizable(false);
        this.setSize(400, 300);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        this.language = configuration.getLanguageObject();
        this.initComponents();
        this.init();
        this.setFieldTexts();
    }

    /**
     * Init everything.
     */
    private void init() {
        // Set look and feel.
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(SFManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.setFieldTexts();
    }

    private void initComponents()
    {
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        SFManager.getDispatcher().dispatch(new RegisterEscapeCloseOperationEvent(this));

        NewProductionPanel panel = this.applicationContext.getBean(
            NewProductionPanel.class, this, this.configuration
        );
        GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.ipady = 2;
        constraints.insets.set(2, 2, 2, 2);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;

        this.add(panel, constraints);
    }

    private void setFieldTexts()
    {
        this.setTitle(this.language.translate("newProduction"));
    }
}
