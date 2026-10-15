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

import antafes.sf.manager.Configuration;
import antafes.sf.manager.entity.Item;
import antafes.sf.manager.entity.Name;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import antafes.sf.manager.gui.editor.ProductionDeleteButtonEditor;
import antafes.sf.manager.gui.editor.ProductionEditButtonEditor;
import antafes.sf.manager.gui.renderer.TableStyle;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.service.dto.RecipeProductionEntry;
import antafes.sf.manager.service.dto.RecipeProductionMaterial;
import antafes.utilities.language.LanguageInterface;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@org.springframework.stereotype.Component
@Scope("prototype")
public class RecipeProductionPanel extends JPanel
{
    @Autowired
    private ProductionService productionService;
    @Autowired
    private Configuration configuration;
    @Autowired
    private ApplicationContext applicationContext;

    @Getter
    private final Item item;
    private String selectedLanguage;
    private LanguageInterface language;
    private NumberFormat numberFormat;
    private DefaultTableModel tableModel;
    @Getter
    private JTable table;
    @Getter
    private ProductionBulkToolbar toolbar;

    public RecipeProductionPanel(Item item)
    {
        this.item = item;
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
            this.language.translate("building"),
            this.language.translate("recipe"),
            this.language.translate("group"),
            this.language.translate("usedMaterials"),
            this.language.translate("clockSpeed"),
            this.language.translate("somersloops"),
            this.language.translate("producedMaterials"),
            this.language.translate("usage"),
            this.language.translate("consumption"),
            "",
            ""
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return column == 9 || column == 10;
            }
        };
        this.table = new JTable(this.tableModel);
        TableStyle.apply(this.table);
        this.installEditButton();
        this.installDeleteButton();
        this.setLayout(new BorderLayout());
        this.toolbar = new ProductionBulkToolbar(
            this.table,
            9,
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
        List<RecipeProductionEntry> entries = this.productionService.getRecipeProductions(this.item);
        for (RecipeProductionEntry entry : entries) {
            this.tableModel.addRow(new Object[] {
                this.localizedName(entry.production().getSelectedBuilding().getName()),
                this.localizedName(entry.recipe().getName()),
                entry.production().getGroup(),
                this.materials(entry.usedMaterials()),
                this.percentage(entry.production().getClockSpeed()),
                entry.production().getUsedSomersloops(),
                this.materials(entry.producedMaterials()),
                this.perMinute(entry.usage()),
                this.consumption(entry.powerConsumption()),
                entry.production().getId(),
                entry.production().getId()
            });
        }
    }

    private void installDeleteButton()
    {
        var actionColumn = this.table.getColumnModel().getColumn(10);
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
        var actionColumn = this.table.getColumnModel().getColumn(9);
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

    private String materials(List<RecipeProductionMaterial> materials)
    {
        return materials.stream()
            .map(material -> this.localizedName(material.item().getName())
                + " (" + this.numberFormat.format(material.amount()) + ")"
                + this.materialSource(material.sourceGroup()))
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
        return value == null ? "" : this.numberFormat.format(value) + "%";
    }

    private String consumption(double value)
    {
        return value == 0 ? "" : this.numberFormat.format(value) + " MW";
    }

    private String perMinute(double value)
    {
        return this.numberFormat.format(value) + " / min";
    }
}
