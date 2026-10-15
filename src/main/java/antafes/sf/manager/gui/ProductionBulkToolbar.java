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

package antafes.sf.manager.gui;

import antafes.sf.manager.gui.util.BulkOperationRunner;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.util.ProductionLabelFormatter;
import antafes.utilities.language.LanguageInterface;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Buttons above a production table to edit or delete all selected productions at once.
 */
public class ProductionBulkToolbar extends JPanel
{
    private final JTable table;
    private final int idColumn;
    private final ProductionService productionService;
    private final LanguageInterface language;
    private final String selectedLanguage;
    private final Function<UUID, Boolean> singleEditor;
    private final Function<List<UUID>, Boolean> bulkEditor;
    private final Runnable onChanged;
    private final JButton editButton = new JButton();
    private final JButton deleteButton = new JButton();
    @Setter
    private Predicate<List<String>> deleteConfirmation = this::confirmDeletion;

    public ProductionBulkToolbar(
        JTable table,
        int idColumn,
        ProductionService productionService,
        LanguageInterface language,
        String selectedLanguage,
        Function<UUID, Boolean> singleEditor,
        Function<List<UUID>, Boolean> bulkEditor,
        Runnable onChanged
    )
    {
        super(new FlowLayout(FlowLayout.LEFT, 4, 4));
        this.table = table;
        this.idColumn = idColumn;
        this.productionService = productionService;
        this.language = language;
        this.selectedLanguage = selectedLanguage;
        this.singleEditor = singleEditor;
        this.bulkEditor = bulkEditor;
        this.onChanged = onChanged;

        this.table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.editButton.setText(language.translate("editSelected"));
        this.deleteButton.setText(language.translate("deleteSelected"));
        this.editButton.addActionListener(event -> this.editSelected());
        this.deleteButton.addActionListener(event -> this.deleteSelected());
        this.add(this.editButton);
        this.add(this.deleteButton);
        this.table.getSelectionModel().addListSelectionListener(event -> this.updateButtons());
        this.updateButtons();
    }

    public List<UUID> getSelectedProductionIds()
    {
        Set<UUID> ids = new LinkedHashSet<>();
        for (int viewRow : this.table.getSelectedRows()) {
            int modelRow = this.table.convertRowIndexToModel(viewRow);
            if (this.table.getModel().getValueAt(modelRow, this.idColumn) instanceof UUID id) {
                ids.add(id);
            }
        }

        return List.copyOf(ids);
    }

    public void editSelected()
    {
        List<UUID> ids = this.getSelectedProductionIds();
        if (ids.isEmpty()) {
            return;
        }
        boolean changed = ids.size() == 1 ? this.singleEditor.apply(ids.get(0)) : this.bulkEditor.apply(ids);
        if (changed) {
            this.onChanged.run();
        }
    }

    public void deleteSelected()
    {
        List<UUID> ids = this.getSelectedProductionIds();
        if (ids.isEmpty()) {
            return;
        }
        List<String> labels;
        try {
            labels = this.productionService.getProductionsAffectedByDeletion(ids).stream()
                .map(production -> ProductionLabelFormatter.format(production, this.selectedLanguage))
                .toList();
        } catch (RuntimeException exception) {
            this.showDeletionError(exception);
            return;
        }
        if (!this.deleteConfirmation.test(labels)) {
            return;
        }

        this.setButtonsEnabled(false);
        BulkOperationRunner.run(
            this,
            this.language.translate("deletingProductions"),
            () -> this.productionService.deleteProductions(ids),
            deleted -> this.onChanged.run(),
            failure -> {
                this.updateButtons();
                this.showDeletionError(failure);
            }
        );
    }

    private void updateButtons()
    {
        this.setButtonsEnabled(this.table.getSelectedRowCount() > 0);
    }

    private void setButtonsEnabled(boolean enabled)
    {
        this.editButton.setEnabled(enabled);
        this.deleteButton.setEnabled(enabled);
    }

    private boolean confirmDeletion(List<String> labels)
    {
        JList<String> list = new JList<>(labels.toArray(String[]::new));
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(360, Math.min(240, 24 + labels.size() * 18)));
        JPanel message = new JPanel(new BorderLayout(0, 8));
        message.add(
            new JLabel(String.format(this.language.translate("confirmDeleteProductions"), labels.size())),
            BorderLayout.NORTH
        );
        message.add(scrollPane, BorderLayout.CENTER);

        return JOptionPane.showConfirmDialog(
            this,
            message,
            this.language.translate("deleteProductions"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        ) == JOptionPane.YES_OPTION;
    }

    private void showDeletionError(Exception exception)
    {
        JOptionPane.showMessageDialog(
            this,
            Objects.toString(exception.getMessage(), this.language.translate("productionsDeletionFailed")),
            this.language.translate("deleteProductions"),
            JOptionPane.ERROR_MESSAGE
        );
    }
}
