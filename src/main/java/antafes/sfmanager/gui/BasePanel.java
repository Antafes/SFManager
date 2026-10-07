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

package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.utilities.language.LanguageInterface;
import lombok.AccessLevel;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public abstract class BasePanel extends JPanel
{
    private final Map<Component, Boolean> enteredFields = new HashMap<>();
    private final JDialog parent;
    @Getter(AccessLevel.PROTECTED)
    protected LanguageInterface language;

    public BasePanel(JDialog parent, Configuration configuration)
    {
        this.parent = parent;
        this.language = configuration.getLanguageObject();
    }

    protected JDialog getParentDialog()
    {
        return this.parent;
    }

    protected BasePanel addField(Component field)
    {
        this.enteredFields.put(field, Boolean.FALSE);

        return this;
    }

    protected boolean checkFieldsFilled()
    {
        return !this.enteredFields.containsValue(Boolean.FALSE);
    }

    abstract protected void init();
    abstract protected void setFieldTexts();
    abstract protected void createFocusTraversalPolicy();
}
