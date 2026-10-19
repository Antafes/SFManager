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

import antafes.sf.manager.util.NaturalOrder;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Comparator;

/**
 * Row sorting for tables whose cells hold formatted text such as "1,000 / min" or "112.5 MW". Values that start
 * with a number are ordered by that number, everything else by natural order.
 */
public final class TableSort
{
    private TableSort()
    {
    }

    public static void apply(JTable table, NumberFormat numberFormat)
    {
        TableModel model = table.getModel();
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        Comparator<Object> comparator = (left, right) -> compare(left, right, numberFormat);
        for (int column = 0; column < model.getColumnCount(); column++) {
            sorter.setComparator(column, comparator);
        }
        table.setRowSorter(sorter);
    }

    private static int compare(Object left, Object right, NumberFormat numberFormat)
    {
        if (left == null || right == null) {
            return left == null ? (right == null ? 0 : 1) : -1;
        }
        if (left instanceof String leftText && right instanceof String rightText) {
            return compareText(leftText, rightText, numberFormat);
        }
        if (left instanceof Comparable<?> && left.getClass() == right.getClass()) {
            return compareComparable(left, right);
        }

        return compareText(left.toString(), right.toString(), numberFormat);
    }

    private static int compareText(String left, String right, NumberFormat numberFormat)
    {
        Number leftNumber = leadingNumber(left, numberFormat);
        Number rightNumber = leadingNumber(right, numberFormat);
        if (leftNumber != null && rightNumber != null) {
            int order = Double.compare(leftNumber.doubleValue(), rightNumber.doubleValue());
            if (order != 0) {
                return order;
            }
        }

        return NaturalOrder.compare(left, right);
    }

    @SuppressWarnings("unchecked")
    private static int compareComparable(Object left, Object right)
    {
        return ((Comparable<Object>) left).compareTo(right);
    }

    private static Number leadingNumber(String text, NumberFormat numberFormat)
    {
        ParsePosition position = new ParsePosition(0);
        Number number = numberFormat.parse(text.trim(), position);

        return position.getIndex() == 0 ? null : number;
    }
}
