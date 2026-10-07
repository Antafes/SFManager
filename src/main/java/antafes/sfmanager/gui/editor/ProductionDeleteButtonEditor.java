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

import antafes.sfmanager.service.ProductionService;
import antafes.utilities.language.LanguageInterface;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public class ProductionDeleteButtonEditor extends AbstractCellEditor
    implements TableCellEditor, TableCellRenderer
{
    private final ProductionService productionService;
    private final LanguageInterface language;
    private final Component parent;
    private final Runnable onDeleted;
    private final BooleanSupplier confirmation;
    private final JButton rendererButton = new JButton();
    private final JButton editorButton = new JButton();
    private UUID productionId;

    public ProductionDeleteButtonEditor(
        ProductionService productionService,
        LanguageInterface language,
        Component parent,
        Runnable onDeleted
    )
    {
        this(productionService, language, parent, onDeleted, () ->
            JOptionPane.showConfirmDialog(
                parent,
                language.translate("confirmDeleteProduction"),
                language.translate("deleteProduction"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            ) == JOptionPane.YES_OPTION
        );
    }

    public ProductionDeleteButtonEditor(
        ProductionService productionService,
        LanguageInterface language,
        Component parent,
        Runnable onDeleted,
        BooleanSupplier confirmation
    )
    {
        this.productionService = productionService;
        this.language = language;
        this.parent = parent;
        this.onDeleted = onDeleted;
        this.confirmation = confirmation;
        String deleteLabel = this.language.translate("delete");
        this.configureButton(this.rendererButton, deleteLabel);
        this.configureButton(this.editorButton, deleteLabel);
        this.editorButton.addActionListener(event -> this.deleteProduction());
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

    private void configureButton(JButton button, String deleteLabel)
    {
        button.setIcon(new TrashCanIcon());
        button.setDisabledIcon(new TrashCanIcon());
        button.setToolTipText(deleteLabel);
        button.getAccessibleContext().setAccessibleName(deleteLabel);
    }

    private void deleteProduction()
    {
        boolean deleted = false;
        try {
            if (this.productionId != null && this.confirmation.getAsBoolean()) {
                this.productionService.deleteProduction(this.productionId);
                deleted = true;
            }
        } catch (IOException | RuntimeException exception) {
            JOptionPane.showMessageDialog(
                this.parent,
                Objects.toString(
                    exception.getMessage(),
                    this.language.translate("productionDeletionFailed")
                ),
                this.language.translate("deleteProduction"),
                JOptionPane.ERROR_MESSAGE
            );
        } finally {
            this.fireEditingStopped();
        }
        if (deleted) {
            this.onDeleted.run();
        }
    }
}
