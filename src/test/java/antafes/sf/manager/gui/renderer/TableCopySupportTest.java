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

import antafes.sf.manager.gui.element.ExpandableMaterialsTable;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TableCopySupportTest
{
    @Test
    void testCopiesOnlyTheLeadCell() throws Exception
    {
        JTable table = this.table();
        table.setRowSelectionInterval(0, 1);
        table.getSelectionModel().setLeadSelectionIndex(1);
        table.getColumnModel().getSelectionModel().setSelectionInterval(2, 2);
        Clipboard clipboard = new Clipboard("test");

        assertTrue(TableCopySupport.copyLeadCell(table, clipboard));
        assertEquals("112.5 MW", clipboard.getData(DataFlavor.stringFlavor));
    }

    @Test
    void testCopiesNothingForButtonExpanderAndEmptyCells()
    {
        JTable table = this.table();
        table.setRowSelectionInterval(0, 0);
        Clipboard clipboard = new Clipboard("test");

        table.getColumnModel().getSelectionModel().setSelectionInterval(0, 0);
        assertFalse(TableCopySupport.copyLeadCell(table, clipboard));
        table.getColumnModel().getSelectionModel().setSelectionInterval(3, 3);
        assertFalse(TableCopySupport.copyLeadCell(table, clipboard));
        table.getColumnModel().getSelectionModel().setSelectionInterval(4, 4);
        assertFalse(TableCopySupport.copyLeadCell(table, clipboard));
        assertNull(clipboard.getContents(null));
    }

    @Test
    void testCopiesNothingWithoutSelection()
    {
        assertFalse(TableCopySupport.copyLeadCell(this.table(), new Clipboard("test")));
    }

    @Test
    void testReplacesTheDefaultCopyAction()
    {
        JTable table = this.table();
        Object key = TransferHandler.getCopyAction().getValue(Action.NAME);
        Action before = table.getActionMap().get(key);

        TableStyle.apply(table);

        assertNotSame(before, table.getActionMap().get(key));
    }

    private JTable table()
    {
        DefaultTableModel model = new DefaultTableModel(new Object[] {"", "Position", "Production", "", ""}, 0);
        model.addRow(new Object[] {ExpandableMaterialsTable.COLLAPSED_SYMBOL, "A1", "100 MW", UUID.randomUUID(), ""});
        model.addRow(new Object[] {ExpandableMaterialsTable.COLLAPSED_SYMBOL, "A2", "112.5 MW", UUID.randomUUID(), ""});

        return new JTable(model);
    }
}
