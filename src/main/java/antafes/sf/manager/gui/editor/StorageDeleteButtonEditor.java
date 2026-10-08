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

package antafes.sf.manager.gui.editor;

import antafes.sf.manager.entity.Storage;
import antafes.sf.manager.service.StorageService;
import antafes.sf.manager.service.dto.StorageItem;
import antafes.utilities.language.LanguageInterface;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public class StorageDeleteButtonEditor extends AbstractCellEditor
    implements TableCellEditor, TableCellRenderer
{
    private final StorageService storageService;
    private final LanguageInterface language;
    private final Component parent;
    private final Runnable onDeleted;
    private final BooleanSupplier confirmation;
    private final JButton rendererButton = new JButton();
    private final JButton editorButton = new JButton();
    private StorageItem storageItem;
    private List<Storage.Entry> entries = List.of();

    public StorageDeleteButtonEditor(
        StorageService storageService,
        LanguageInterface language,
        Component parent,
        Runnable onDeleted
    )
    {
        this(storageService, language, parent, onDeleted, () ->
            JOptionPane.showConfirmDialog(
                parent,
                language.translate("confirmDeleteStorage"),
                language.translate("deleteStorage"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            ) == JOptionPane.YES_OPTION
        );
    }

    StorageDeleteButtonEditor(
        StorageService storageService,
        LanguageInterface language,
        Component parent,
        Runnable onDeleted,
        BooleanSupplier confirmation
    )
    {
        this.storageService = storageService;
        this.language = language;
        this.parent = parent;
        this.onDeleted = onDeleted;
        this.confirmation = confirmation;
        String deleteLabel = this.language.translate("delete");
        this.configureButton(this.rendererButton, deleteLabel);
        this.configureButton(this.editorButton, deleteLabel);
        this.editorButton.addActionListener(event -> this.deleteStorageEntries());
    }

    @Override
    public Object getCellEditorValue()
    {
        return this.storageItem;
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
        this.storageItem = value instanceof StorageItem item ? item : null;
        this.entries = this.toEntries(this.storageItem);
        this.editorButton.setEnabled(!this.entries.isEmpty());
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
        this.rendererButton.setEnabled(!this.toEntries(value).isEmpty());
        return this.rendererButton;
    }

    private List<Storage.Entry> toEntries(Object value)
    {
        if (!(value instanceof StorageItem storageItem)) {
            return List.of();
        }
        return storageItem.sources().stream()
            .map(source -> new Storage.Entry()
                .setProductionId(source.production().getId())
                .setItemKey(source.item().getKey()))
            .toList();
    }

    private void configureButton(JButton button, String deleteLabel)
    {
        button.setIcon(new TrashCanIcon());
        button.setDisabledIcon(new TrashCanIcon());
        button.setToolTipText(deleteLabel);
        button.getAccessibleContext().setAccessibleName(deleteLabel);
    }

    private void deleteStorageEntries()
    {
        boolean deleted = false;
        try {
            if (!this.entries.isEmpty() && this.confirmation.getAsBoolean()) {
                this.storageService.removeStorageEntries(this.entries);
                deleted = true;
            }
        } catch (IOException | RuntimeException exception) {
            JOptionPane.showMessageDialog(
                this.parent,
                Objects.toString(
                    exception.getMessage(),
                    this.language.translate("storageDeletionFailed")
                ),
                this.language.translate("deleteStorage"),
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
