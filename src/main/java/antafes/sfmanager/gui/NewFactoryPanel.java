package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.repository.FactoryRepository;
import antafes.utilities.gui.OrderFocusTraversalPolicy;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.Vector;

@org.springframework.stereotype.Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NewFactoryPanel extends BasePanel
{
    private JLabel nameLabel;
    private JTextField nameField;
    private JButton okButton;
    private JButton cancelButton;

    @Autowired
    private FactoryRepository factoryRepository;

    public NewFactoryPanel(JDialog parent, Configuration configuration)
    {
        super(parent, configuration);
    }

    @PostConstruct
    private void initializePanel()
    {
        this.init();
    }

    @Override
    protected void init()
    {
        this.setLayout(new GridBagLayout());
        this.nameLabel = new JLabel();
        this.nameField = new JTextField(24);
        this.okButton = new JButton();
        this.cancelButton = new JButton();

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        labelConstraints.insets = new Insets(8, 8, 8, 4);
        labelConstraints.anchor = GridBagConstraints.LINE_START;
        this.add(this.nameLabel, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = 0;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(8, 4, 8, 8);
        this.add(this.nameField, fieldConstraints);

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridy = 1;
        buttonConstraints.insets = new Insets(4, 4, 8, 8);
        buttonConstraints.anchor = GridBagConstraints.LINE_END;
        buttonConstraints.gridx = 1;
        this.add(this.okButton, buttonConstraints);
        buttonConstraints.gridx = 2;
        this.add(this.cancelButton, buttonConstraints);

        this.okButton.addActionListener(event -> this.createFactory());
        this.cancelButton.addActionListener(event -> this.getParentDialog().dispose());
        this.getParentDialog().getRootPane().setDefaultButton(this.okButton);
        this.setFieldTexts();
        this.createFocusTraversalPolicy();
    }

    @Override
    protected void setFieldTexts()
    {
        this.nameLabel.setText(this.getLanguage().translate("factoryName"));
        this.okButton.setText(this.getLanguage().translate("ok"));
        this.cancelButton.setText(this.getLanguage().translate("cancel"));
    }

    @Override
    protected void createFocusTraversalPolicy()
    {
        Vector<Component> order = new Vector<>();
        order.add(this.nameField);
        order.add(this.okButton);
        order.add(this.cancelButton);
        this.setFocusTraversalPolicy(new OrderFocusTraversalPolicy(order));
        this.setFocusTraversalPolicyProvider(true);
    }

    private void createFactory()
    {
        try {
            this.factoryRepository.createFactory(this.nameField.getText());
            this.getParentDialog().dispose();
        } catch (IOException | RuntimeException exception) {
            String message = Objects.toString(
                exception.getMessage(),
                this.getLanguage().translate("factoryCreationFailed")
            );
            JOptionPane.showMessageDialog(
                this.getParentDialog(),
                message,
                this.getLanguage().translate("newFactory"),
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
