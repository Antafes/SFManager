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
import antafes.sf.manager.entity.Building;
import antafes.sf.manager.entity.Name;
import antafes.sf.manager.gui.dialog.BulkEditProductionDialog;
import antafes.sf.manager.gui.dialog.EditProductionDialog;
import antafes.sf.manager.gui.order.LocalizedBuildingOrder;
import antafes.sf.manager.gui.renderer.TableStyle;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.service.dto.ConsumerGroupEntry;
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
import java.util.UUID;

@org.springframework.stereotype.Component
@Scope("prototype")
public class ConsumerProductionPanel extends JPanel
{
    private static final int PRODUCTION_IDS_COLUMN = 4;

    @Autowired
    private ProductionService productionService;
    @Autowired
    private Configuration configuration;
    @Autowired
    private ApplicationContext applicationContext;

    private String selectedLanguage;
    private LanguageInterface language;
    private NumberFormat numberFormat;
    private DefaultTableModel tableModel;
    @Getter
    private JTable table;
    @Getter
    private ProductionBulkToolbar toolbar;
    @Getter
    private JComboBox<Building> buildingFilter;
    private boolean updatingFilter;

    @PostConstruct
    private void initializePanel()
    {
        this.selectedLanguage = this.configuration.getLanguage().getName();
        this.language = this.configuration.getLanguageObject();
        this.numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag(
            "German".equals(this.selectedLanguage) ? "de" : "en"
        ));
        this.tableModel = new DefaultTableModel(new Object[] {
            this.language.translate("group"),
            this.language.translate("building"),
            this.language.translate("amount"),
            this.language.translate("consumption"),
            ""
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        };
        this.table = new JTable(this.tableModel);
        TableStyle.apply(this.table);
        this.table.removeColumn(this.table.getColumnModel().getColumn(PRODUCTION_IDS_COLUMN));
        this.setLayout(new BorderLayout());
        this.toolbar = new ProductionBulkToolbar(
            this.table,
            PRODUCTION_IDS_COLUMN,
            this.productionService,
            this.language,
            this.selectedLanguage,
            this::openEditDialog,
            this::openBulkEditDialog,
            this::refresh
        );
        this.buildingFilter = new JComboBox<>();
        this.buildingFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            )
            {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(value instanceof Building building
                    ? ConsumerProductionPanel.this.displayName(building)
                    : ConsumerProductionPanel.this.language.translate("allBuildings"));
                return this;
            }
        });
        this.buildingFilter.addActionListener(event -> {
            if (!this.updatingFilter) {
                this.refreshRows();
            }
        });
        JPanel header = new JPanel(new BorderLayout());
        header.add(this.toolbar, BorderLayout.WEST);
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        filterPanel.add(new JLabel(this.language.translate("building") + ":"));
        filterPanel.add(this.buildingFilter);
        header.add(filterPanel, BorderLayout.EAST);
        this.add(header, BorderLayout.NORTH);
        this.add(new JScrollPane(this.table), BorderLayout.CENTER);
        this.refresh();
    }

    public void refresh()
    {
        Building selected = (Building) this.buildingFilter.getSelectedItem();
        this.updatingFilter = true;
        this.buildingFilter.removeAllItems();
        this.buildingFilter.addItem(null);
        List<Building> buildings = this.productionService.getConsumerBuildingsWithProductions().stream()
            .sorted(LocalizedBuildingOrder.comparator(this.selectedLanguage))
            .toList();
        buildings.forEach(this.buildingFilter::addItem);
        if (selected != null) {
            buildings.stream()
                .filter(building -> building.getKey().equals(selected.getKey()))
                .findFirst()
                .ifPresent(this.buildingFilter::setSelectedItem);
        }
        this.updatingFilter = false;
        this.refreshRows();
    }

    private void refreshRows()
    {
        this.tableModel.setRowCount(0);
        List<ConsumerGroupEntry> entries = this.productionService.getConsumerGroupEntries(
            (Building) this.buildingFilter.getSelectedItem()
        );
        for (ConsumerGroupEntry entry : entries) {
            this.tableModel.addRow(new Object[] {
                entry.group(),
                this.displayName(entry.building()),
                entry.amount(),
                this.numberFormat.format(entry.powerConsumption()) + " MW",
                entry.productionIds()
            });
        }
    }

    private boolean openEditDialog(UUID productionId)
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

    private boolean openBulkEditDialog(List<UUID> productionIds)
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

    private String displayName(Building building)
    {
        Name name = building.getName();
        return name == null ? building.getKey() : name.getLocalizedName(this.selectedLanguage);
    }
}
