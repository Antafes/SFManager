/*
 * This file is part of SFManager.
 *
 * SFManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SFManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SFManager. If not, see <http://www.gnu.org/licenses/>.
 *
 * @package SFManager
 * @author Marian Pollzien <map@wafriv.de>
 * @copyright (c) 2026, Marian Pollzien
 * @license https://www.gnu.org/licenses/lgpl.html LGPLv3
 */

package antafes.sf.manager.gui.dialog;

import antafes.sf.manager.gui.BasePanel;
import antafes.sf.manager.Configuration;
import antafes.sf.manager.gui.BasePanel;
import antafes.sf.manager.service.FactoryService;
import antafes.sf.manager.gui.BasePanel;
import antafes.utilities.gui.OrderFocusTraversalPolicy;
import antafes.sf.manager.gui.BasePanel;
import jakarta.annotation.PostConstruct;
import antafes.sf.manager.gui.BasePanel;
import org.springframework.beans.factory.annotation.Autowired;
import antafes.sf.manager.gui.BasePanel;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import antafes.sf.manager.gui.BasePanel;
import org.springframework.context.annotation.Scope;

import antafes.sf.manager.gui.BasePanel;
import javax.swing.*;
import antafes.sf.manager.gui.BasePanel;
import java.awt.*;
import antafes.sf.manager.gui.BasePanel;
import java.io.IOException;
import antafes.sf.manager.gui.BasePanel;
import java.util.Objects;
import antafes.sf.manager.gui.BasePanel;
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
    private FactoryService factoryService;

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
            this.factoryService.createFactory(this.nameField.getText());
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
