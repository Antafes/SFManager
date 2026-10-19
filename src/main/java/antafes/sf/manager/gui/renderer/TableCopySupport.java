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

package antafes.sf.manager.gui.renderer;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

/**
 * Makes the copy shortcut of a table copy only the displayed text of the lead cell instead of the whole row.
 */
public final class TableCopySupport
{
    private TableCopySupport()
    {
    }

    public static void install(JTable table)
    {
        Object copyKey = TransferHandler.getCopyAction().getValue(Action.NAME);
        table.getActionMap().put(copyKey, new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                try {
                    copyLeadCell(table, table.getToolkit().getSystemClipboard());
                } catch (HeadlessException | IllegalStateException exception) {
                    // The clipboard is unavailable, so there is nothing to copy to.
                }
            }
        });
    }

    /**
     * @return whether a value was put on the clipboard
     */
    public static boolean copyLeadCell(JTable table, Clipboard clipboard)
    {
        int row = leadIndex(table.getSelectionModel().getLeadSelectionIndex(), table.getSelectedRow());
        int column = leadIndex(
            table.getColumnModel().getSelectionModel().getLeadSelectionIndex(),
            table.getSelectedColumn()
        );
        if (row < 0 || column < 0 || row >= table.getRowCount() || column >= table.getColumnCount()) {
            return false;
        }

        // Only plain text values are copied, button columns, status dots and expanders carry no copyable text.
        Object value = table.getValueAt(row, column);
        if (!(value instanceof String || value instanceof Number)) {
            return false;
        }
        String text = value.toString();
        if (text.isEmpty() || isExpanderSymbol(text)) {
            return false;
        }

        StringSelection selection = new StringSelection(text);
        clipboard.setContents(selection, selection);

        return true;
    }

    private static int leadIndex(int lead, int selected)
    {
        return lead >= 0 ? lead : selected;
    }

    private static boolean isExpanderSymbol(String text)
    {
        return antafes.sf.manager.gui.element.ExpandableMaterialsTable.COLLAPSED_SYMBOL.equals(text)
            || antafes.sf.manager.gui.element.ExpandableMaterialsTable.EXPANDED_SYMBOL.equals(text);
    }
}
