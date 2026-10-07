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

package antafes.sfmanager.gui.editor;

import antafes.utilities.language.LanguageInterface;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.UUID;
import java.util.function.Function;

public class ProductionEditButtonEditor extends AbstractCellEditor
    implements TableCellEditor, TableCellRenderer
{
    private final Function<UUID, Boolean> editProduction;
    private final Runnable onEdited;
    private final JButton rendererButton = new JButton();
    private final JButton editorButton = new JButton();
    private UUID productionId;

    public ProductionEditButtonEditor(
        LanguageInterface language,
        Function<UUID, Boolean> editProduction,
        Runnable onEdited
    )
    {
        this.editProduction = editProduction;
        this.onEdited = onEdited;
        String editLabel = language.translate("edit");
        this.configureButton(this.rendererButton, editLabel);
        this.configureButton(this.editorButton, editLabel);
        this.editorButton.addActionListener(event -> this.editProduction());
    }

    @Override
    public Object getCellEditorValue()
    {
        return this.productionId;
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable table,
        Object value,
        boolean isSelected,
        int row,
        int column
    )
    {
        this.productionId = value instanceof UUID id ? id : null;
        this.editorButton.setEnabled(this.productionId != null);
        return this.editorButton;
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column
    )
    {
        this.rendererButton.setEnabled(value instanceof UUID);
        return this.rendererButton;
    }

    private void configureButton(JButton button, String editLabel)
    {
        button.setIcon(new PencilIcon());
        button.setDisabledIcon(new PencilIcon());
        button.setToolTipText(editLabel);
        button.getAccessibleContext().setAccessibleName(editLabel);
    }

    private void editProduction()
    {
        boolean edited = false;
        try {
            if (this.productionId != null) {
                edited = this.editProduction.apply(this.productionId);
            }
        } finally {
            this.fireEditingStopped();
        }
        if (edited) {
            this.onEdited.run();
        }
    }

    private static final class PencilIcon implements Icon
    {
        @Override
        public int getIconWidth()
        {
            return 16;
        }

        @Override
        public int getIconHeight()
        {
            return 16;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y)
        {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );
                graphics2D.setColor(component.isEnabled() ? component.getForeground() : Color.GRAY);
                graphics2D.fillPolygon(new Polygon(
                    new int[] {x + 2, x + 6, x + 14, x + 10},
                    new int[] {y + 10, y + 14, y + 6, y + 2},
                    4
                ));
                graphics2D.fillPolygon(new Polygon(
                    new int[] {x + 2, x, x + 6},
                    new int[] {y + 10, y + 16, y + 14},
                    3
                ));
                graphics2D.setColor(component.isEnabled() ? component.getBackground() : Color.LIGHT_GRAY);
                graphics2D.fillPolygon(new Polygon(
                    new int[] {x + 10, x + 14, x + 15, x + 11},
                    new int[] {y + 2, y + 6, y + 5, y + 1},
                    4
                ));
            } finally {
                graphics2D.dispose();
            }
        }
    }
}
