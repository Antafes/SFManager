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

import antafes.sf.manager.entity.Factory;
import antafes.sf.manager.service.FactoryService;
import antafes.utilities.language.LanguageInterface;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

final class FactoryDeletion
{
    private FactoryDeletion()
    {
    }

    /**
     * Asks for confirmation and deletes the factory.
     *
     * @return true if the factory has been deleted
     */
    static boolean confirmAndDelete(
        Component parent,
        LanguageInterface language,
        FactoryService factoryService,
        Factory factory
    )
    {
        if (factory == null) {
            return false;
        }

        int answer = JOptionPane.showConfirmDialog(
            parent,
            String.format(language.translate("confirmDeleteFactory"), factory.getName()),
            language.translate("deleteFactory"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (answer != JOptionPane.YES_OPTION) {
            return false;
        }

        try {
            factoryService.deleteFactory(factory);
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                parent,
                language.translate("factoryDeletionFailed"),
                language.translate("deleteFactory"),
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }
}
