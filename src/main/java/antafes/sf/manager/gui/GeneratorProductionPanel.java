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

import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import antafes.sf.manager.Configuration;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import antafes.sf.manager.entity.Building;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import antafes.sf.manager.entity.Name;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import antafes.sf.manager.gui.editor.ProductionDeleteButtonEditor;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import antafes.sf.manager.gui.editor.ProductionEditButtonEditor;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import antafes.sf.manager.gui.renderer.TableStyle;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import antafes.sf.manager.service.dto.GeneratorProductionEntry;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import antafes.sf.manager.service.dto.GeneratorProductionIngredient;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import antafes.utilities.language.LanguageInterface;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import jakarta.annotation.PostConstruct;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import lombok.Getter;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import org.springframework.beans.factory.annotation.Autowired;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import org.springframework.context.ApplicationContext;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import org.springframework.context.annotation.Scope;

import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import javax.swing.*;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import javax.swing.table.DefaultTableModel;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import java.awt.*;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import java.text.NumberFormat;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import java.util.List;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import java.util.Locale;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import java.util.stream.Collectors;

@org.springframework.stereotype.Component
@Scope("prototype")
public class GeneratorProductionPanel extends JPanel
{
    @Autowired
    private ProductionService productionService;
    @Autowired
    private Configuration configuration;
    @Autowired
    private ApplicationContext applicationContext;

    @Getter
    private final Building building;
    private String selectedLanguage;
    private LanguageInterface language;
    private NumberFormat numberFormat;
    private DefaultTableModel tableModel;
    @Getter
    private JTable table;
    @Getter
    private ProductionBulkToolbar toolbar;

    public GeneratorProductionPanel(Building building)
    {
        this.building = building;
    }

    @PostConstruct
    private void initializePanel()
    {
        this.selectedLanguage = this.configuration.getLanguage().getName();
        this.language = this.configuration.getLanguageObject();
        this.numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag(
            "German".equals(this.selectedLanguage) ? "de" : "en"
        ));
        this.tableModel = new DefaultTableModel(new Object[] {
            this.language.translate("position"),
            this.language.translate("group"),
            this.language.translate("clockSpeed"),
            this.language.translate("production"),
            this.language.translate("usedMaterials"),
            "",
            ""
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return column == 5 || column == 6;
            }
        };
        this.table = new JTable(this.tableModel);
        TableStyle.apply(this.table);
        this.installEditButton();
        this.installDeleteButton();
        this.setLayout(new BorderLayout());
        this.toolbar = new ProductionBulkToolbar(
            this.table,
            5,
            this.productionService,
            this.language,
            this.selectedLanguage,
            this::openEditDialog,
            this::openBulkEditDialog,
            this::refresh
        );
        this.add(this.toolbar, BorderLayout.NORTH);
        this.add(new JScrollPane(this.table), BorderLayout.CENTER);
        this.refresh();
    }

    public void refresh()
    {
        this.tableModel.setRowCount(0);
        List<GeneratorProductionEntry> entries = this.productionService.getGeneratorProductions(this.building);
        for (GeneratorProductionEntry entry : entries) {
            this.tableModel.addRow(new Object[] {
                entry.production().getPosition(),
                entry.production().getGroup(),
                this.percentage(entry.production().getClockSpeed()),
                this.megawatts(entry.powerProduction()),
                this.usedMaterials(entry.usedMaterials()),
                entry.production().getId(),
                entry.production().getId()
            });
        }
    }

    private void installDeleteButton()
    {
        var actionColumn = this.table.getColumnModel().getColumn(6);
        actionColumn.setPreferredWidth(40);
        actionColumn.setMaxWidth(50);
        ProductionDeleteButtonEditor deleteButton = new ProductionDeleteButtonEditor(
            this.productionService,
            this.language,
            this,
            this::refresh
        );
        actionColumn.setCellRenderer(deleteButton);
        actionColumn.setCellEditor(deleteButton);
    }

    private void installEditButton()
    {
        var actionColumn = this.table.getColumnModel().getColumn(5);
        actionColumn.setPreferredWidth(40);
        actionColumn.setMaxWidth(50);
        ProductionEditButtonEditor editButton = new ProductionEditButtonEditor(
            this.language,
            this::openEditDialog,
            this::refresh
        );
        actionColumn.setCellRenderer(editButton);
        actionColumn.setCellEditor(editButton);
    }

    private boolean openEditDialog(java.util.UUID productionId)
    {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (!(window instanceof Frame frame)) {
            throw new IllegalStateException("The production panel has no frame owner.");
        }
        EditProductionDialog dialog = this.applicationContext.getBean(
            EditProductionDialog.class,
            frame,
            true,
            productionId
        );
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return dialog.isSaved();
    }

    private boolean openBulkEditDialog(List<java.util.UUID> productionIds)
    {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (!(window instanceof Frame frame)) {
            throw new IllegalStateException("The production panel has no frame owner.");
        }
        BulkEditProductionDialog dialog = this.applicationContext.getBean(
            BulkEditProductionDialog.class,
            frame,
            true,
            productionIds
        );
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return dialog.isSaved();
    }

    private String usedMaterials(List<GeneratorProductionIngredient> ingredients)
    {
        return ingredients.stream()
            .map(ingredient -> this.localizedName(ingredient.item().getName())
                + " (" + this.format(ingredient.amount()) + ")"
                + this.materialSource(ingredient.sourceGroup()))
            .collect(Collectors.joining(", "));
    }

    private String materialSource(String sourceGroup)
    {
        return sourceGroup == null || sourceGroup.isBlank() ? "" : " [" + sourceGroup + "]";
    }

    private String localizedName(Name name)
    {
        return name == null ? "" : name.getLocalizedName(this.selectedLanguage);
    }

    private String percentage(Double value)
    {
        return value == null ? "" : this.format(value) + "%";
    }

    private String megawatts(double value)
    {
        return this.format(value) + " MW";
    }

    private String format(double value)
    {
        return this.numberFormat.format(value);
    }
}
