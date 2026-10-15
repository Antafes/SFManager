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

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NewFactoryDialog extends JDialog
{
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private Configuration configuration;
    private LanguageInterface language;

    public NewFactoryDialog(Frame owner, boolean modal)
    {
        super(owner, modal);
    }

    @PostConstruct
    private void initializeDialog()
    {
        this.language = this.configuration.getLanguageObject();
        this.setResizable(false);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.applicationContext.publishEvent(new RegisterEscapeCloseOperationEvent(this));

        NewFactoryPanel panel = this.applicationContext.getBean(
            NewFactoryPanel.class, this, this.configuration
        );
        this.add(panel);
        this.setTitle(this.language.translate("newFactory"));
        this.pack();
    }
}
