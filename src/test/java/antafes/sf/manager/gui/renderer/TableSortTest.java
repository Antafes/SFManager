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

import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableSortTest
{
    @Test
    void testSortsFormattedNumbersNumerically()
    {
        JTable table = this.table(Locale.ENGLISH, "100 / min", "20 / min", "1,000 / min", "3.5 / min");

        assertEquals(List.of("3.5 / min", "20 / min", "100 / min", "1,000 / min"), this.sorted(table, true));
        assertEquals(List.of("1,000 / min", "100 / min", "20 / min", "3.5 / min"), this.sorted(table, false));
    }

    @Test
    void testSortsGermanFormat()
    {
        JTable table = this.table(Locale.GERMAN, "1.000,5 MW", "20 MW", "100,25 MW");

        assertEquals(List.of("20 MW", "100,25 MW", "1.000,5 MW"), this.sorted(table, true));
    }

    @Test
    void testSortsPercentAndNegativeValues()
    {
        JTable table = this.table(Locale.ENGLISH, "150%", "-5 MW", "20%");

        assertEquals(List.of("-5 MW", "20%", "150%"), this.sorted(table, true));
    }

    @Test
    void testFallsBackToNaturalOrderForText()
    {
        JTable table = this.table(Locale.ENGLISH, "Miner Mk.10", "miner Mk.2", "Assembler", "100 / min");

        assertEquals(List.of("100 / min", "Assembler", "miner Mk.2", "Miner Mk.10"), this.sorted(table, true));
    }

    private JTable table(Locale locale, String... values)
    {
        DefaultTableModel model = new DefaultTableModel(new Object[] {"Value"}, 0);
        for (String value : values) {
            model.addRow(new Object[] {value});
        }
        JTable table = new JTable(model);
        TableSort.apply(table, NumberFormat.getNumberInstance(locale));

        return table;
    }

    private List<String> sorted(JTable table, boolean ascending)
    {
        table.getRowSorter().setSortKeys(List.of(
            new RowSorter.SortKey(0, ascending ? SortOrder.ASCENDING : SortOrder.DESCENDING)
        ));
        List<String> values = new ArrayList<>();
        for (int row = 0; row < table.getRowCount(); row++) {
            values.add((String) table.getValueAt(row, 0));
        }

        return values;
    }
}
