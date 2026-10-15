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

import antafes.sf.manager.Configuration;
import antafes.sf.manager.SFManager;
import antafes.sf.manager.gui.event.RegisterEscapeCloseOperationEvent;
import antafes.utilities.language.LanguageInterface;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NewProductionDialog extends JDialog
{
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private Configuration configuration;
    private LanguageInterface language;

    public NewProductionDialog(Frame owner, boolean modal)
    {
        super(owner, modal);
    }

    @PostConstruct
    private void initializeDialog()
    {
        this.setResizable(false);
        this.setSize(480, 420);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        this.language = this.configuration.getLanguageObject();
        this.initComponents();
        this.init();
        this.setFieldTexts();
        int width = this.getWidth();
        this.pack();
        this.setSize(width, this.getHeight());
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
        this.applicationContext.publishEvent(new RegisterEscapeCloseOperationEvent(this));

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
