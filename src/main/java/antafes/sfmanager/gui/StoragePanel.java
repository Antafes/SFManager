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

package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Name;
import antafes.sfmanager.gui.editor.StorageDeleteButtonEditor;
import antafes.sfmanager.gui.renderer.TableStyle;
import antafes.sfmanager.service.FactoryService;
import antafes.sfmanager.service.StorageService;
import antafes.sfmanager.service.dto.StorageItem;
import antafes.sfmanager.service.dto.StorageProductionChoice;
import antafes.utilities.language.LanguageInterface;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@org.springframework.stereotype.Component
@Scope("prototype")
public class StoragePanel extends JPanel
{
    @Autowired
    private StorageService storageService;
    @Autowired
    private FactoryService factoryService;
    @Autowired
    private Configuration configuration;

    private String selectedLanguage;
    private LanguageInterface language;
    private NumberFormat numberFormat;
    private DefaultTableModel tableModel;
    @Getter
    private JTable table;

    public StoragePanel()
    {
    }

    @PostConstruct
    private void initializePanel()
    {
        this.selectedLanguage = this.configuration.getLanguage().getName();
        this.language = this.configuration.getLanguageObject();
        this.numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag(
            "German".equals(this.selectedLanguage) ? "de" : "en"
        ));
        this.createTable();
        this.setLayout(new BorderLayout());
        this.add(new JScrollPane(this.table), BorderLayout.CENTER);
        this.refresh();
    }

    public void refresh()
    {
        this.tableModel.setRowCount(0);
        if (this.factoryService.getCurrentFactory() == null) {
            return;
        }
        this.storageService.getStoredItems().stream()
            .sorted(Comparator.<StorageItem, String>comparing(
                storageItem -> this.localizedName(storageItem.item().getName()),
                Collator.getInstance(Locale.forLanguageTag(
                    "German".equals(this.selectedLanguage) ? "de" : "en"
                ))
            ).thenComparing(storageItem -> storageItem.item().getKey()))
            .forEach(storageItem -> this.tableModel.addRow(new Object[] {
                this.localizedName(storageItem.item().getName()),
                this.perMinute(storageItem.rate()),
                this.sourceDescriptions(storageItem.sources()),
                storageItem
            }));
    }

    private void createTable()
    {
        this.tableModel = new DefaultTableModel(new Object[] {
            this.language.translate("item"),
            this.language.translate("storedRate"),
            this.language.translate("sourceProductions"),
            ""
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return column == 3;
            }
        };
        this.table = new JTable(this.tableModel);
        TableStyle.apply(this.table);
        this.table.setAutoCreateRowSorter(true);
        var actionColumn = this.table.getColumnModel().getColumn(3);
        actionColumn.setPreferredWidth(40);
        actionColumn.setMaxWidth(50);
        StorageDeleteButtonEditor deleteButton = new StorageDeleteButtonEditor(
            this.storageService,
            this.language,
            this,
            this::refresh
        );
        actionColumn.setCellRenderer(deleteButton);
        actionColumn.setCellEditor(deleteButton);
    }

    private String sourceDescriptions(List<StorageProductionChoice> sources)
    {
        return sources.stream()
            .map(this::productionDescription)
            .sorted()
            .reduce((left, right) -> left + ", " + right)
            .orElse("");
    }

    private String productionDescription(StorageProductionChoice choice)
    {
        List<String> context = new java.util.ArrayList<>();
        context.add(this.localizedName(choice.production().getSelectedBuilding().getName()));
        if (choice.production().getSelectedRecipe() != null) {
            context.add(this.localizedName(choice.production().getSelectedRecipe().getName()));
        }
        if (choice.production().getGroup() != null && !choice.production().getGroup().isBlank()) {
            context.add(choice.production().getGroup());
        }
        if (choice.production().getPosition() != null && !choice.production().getPosition().isBlank()) {
            context.add(choice.production().getPosition());
        }
        return String.join(" — ", context);
    }

    private String localizedName(Name name)
    {
        return name == null ? "" : name.getLocalizedName(this.selectedLanguage);
    }

    private String perMinute(double amount)
    {
        return this.numberFormat.format(amount) + " / min";
    }
}
