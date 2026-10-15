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
import antafes.sf.manager.entity.*;
import antafes.sf.manager.gui.editor.MinerUpgrade;
import antafes.sf.manager.gui.editor.ProductionDeleteButtonEditor;
import antafes.sf.manager.gui.editor.ProductionEditButtonEditor;
import antafes.sf.manager.gui.editor.ProductionUpgradeButtonEditor;
import antafes.sf.manager.gui.renderer.TableStyle;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.service.dto.ResourceProductionEntry;
import antafes.sf.manager.util.PowerCalculator;
import antafes.sf.manager.util.ProductionCalculator;
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
import java.util.*;
import java.util.List;

@org.springframework.stereotype.Component
@Scope("prototype")
public class ResourceProductionPanel extends JPanel
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

    public ResourceProductionPanel(Item item)
    {
        this.item = item;
    }

    @PostConstruct
    private void initializePanel()
    {
        this.selectedLanguage = configuration.getLanguage().getName();
        this.language = configuration.getLanguageObject();
        this.numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag(
            this.selectedLanguage.equals("German") ? "de" : "en"
        ));
        this.tableModel = new DefaultTableModel(this.columnNames(), 0) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return (column == 8 && this.getValueAt(row, column) != null)
                    || column == 9 || column == 10;
            }
        };
        this.table = new JTable(this.tableModel);
        TableStyle.apply(this.table);
        this.installUpgradeButton();
        this.installEditButton();
        this.installDeleteButton();
        this.setColumnWidths();

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
        List<ResourceProductionEntry> entries = this.productionService.getResourceProductions(this.item);
        List<Object[]> rows = this.createRows(entries);
        rows.forEach(this.tableModel::addRow);
    }

    private void installDeleteButton()
    {
        ProductionDeleteButtonEditor deleteButton = new ProductionDeleteButtonEditor(
            this.productionService,
            this.language,
            this,
            this::refresh
        );
        this.table.getColumnModel().getColumn(10).setCellRenderer(deleteButton);
        this.table.getColumnModel().getColumn(10).setCellEditor(deleteButton);
    }

    private void installEditButton()
    {
        ProductionEditButtonEditor editButton = new ProductionEditButtonEditor(
            this.language,
            this::openEditDialog,
            this::refresh
        );
        this.table.getColumnModel().getColumn(9).setCellRenderer(editButton);
        this.table.getColumnModel().getColumn(9).setCellEditor(editButton);
    }

    private void installUpgradeButton()
    {
        ProductionUpgradeButtonEditor upgradeButton = new ProductionUpgradeButtonEditor(
            this.productionService,
            this.language,
            this.selectedLanguage,
            this,
            this::refresh
        );
        this.table.getColumnModel().getColumn(8).setCellRenderer(upgradeButton);
        this.table.getColumnModel().getColumn(8).setCellEditor(upgradeButton);
    }

    private String[] columnNames()
    {
        return new String[] {
            this.language.translate("building"),
            this.language.translate("position"),
            this.language.translate("group"),
            this.language.translate("purity"),
            this.language.translate("clockSpeed"),
            this.language.translate("production"),
            this.language.translate("usage"),
            this.language.translate("consumption"),
            "",
            "",
            ""
        };
    }

    private void setColumnWidths()
    {
        for (int columnIndex = 1; columnIndex <= 5; columnIndex++) {
            this.table.getColumnModel().getColumn(columnIndex).setMinWidth(70);
            this.table.getColumnModel().getColumn(columnIndex).setPreferredWidth(85);
            this.table.getColumnModel().getColumn(columnIndex).setMaxWidth(100);
        }
        for (int columnIndex = 8; columnIndex <= 10; columnIndex++) {
            this.table.getColumnModel().getColumn(columnIndex).setPreferredWidth(40);
            this.table.getColumnModel().getColumn(columnIndex).setMaxWidth(50);
        }
    }

    private List<Object[]> createRows(List<ResourceProductionEntry> entries)
    {
        List<Object[]> rows = new ArrayList<>();
        Map<UUID, List<ResourceProductionEntry>> wellGroups = new HashMap<>();
        for (ResourceProductionEntry entry : entries) {
            if (entry.pressurizer() != null) {
                wellGroups.computeIfAbsent(entry.pressurizer().getId(), ignored -> new ArrayList<>()).add(entry);
            }
        }
        Set<UUID> displayedPressurizers = new HashSet<>();
        for (ResourceProductionEntry entry : entries) {
            Production production = entry.production();
            if (entry.pressurizer() == null) {
                rows.add(this.createStandardRow(production, entry.baseAmount(), entry.usage()));
                continue;
            }

            Production pressurizer = entry.pressurizer();
            if (displayedPressurizers.add(pressurizer.getId())) {
                rows.add(this.createPressurizerRow(pressurizer));
                for (ResourceProductionEntry groupedEntry : wellGroups.get(pressurizer.getId())) {
                    rows.add(this.createExtractorRow(
                        groupedEntry.production(),
                        groupedEntry.baseAmount(),
                        groupedEntry.usage(),
                        pressurizer
                    ));
                }
            }
        }
        return rows;
    }

    private Object[] createStandardRow(Production production, int baseAmount, double usage)
    {
        Building building = production.getSelectedBuilding();
        Double clockSpeed = production.getClockSpeed();
        Double output = ProductionCalculator.calculateResourceProduction(
            (double) baseAmount,
            production.getPurity(),
            clockSpeed,
            production.getUsedSomersloops(),
            building.getSomersloopSlots()
        );
        Double consumption = this.calculatePowerConsumption(production);
        return new Object[] {
            this.localizedName(building.getName()),
            production.getPosition(),
            production.getGroup(),
            this.purityName(production.getPurity()),
            this.percentage(clockSpeed),
            this.perMinute(output),
            this.perMinute(usage),
            this.megawatts(consumption),
            this.minerUpgrade(production),
            production.getId(),
            production.getId()
        };
    }

    private Object[] createPressurizerRow(Production pressurizer)
    {
        Building building = pressurizer.getSelectedBuilding();
        return new Object[] {
            this.localizedName(building.getName()),
            pressurizer.getPosition(),
            pressurizer.getGroup(),
            "",
            this.percentage(pressurizer.getClockSpeed()),
            "",
            "",
            this.megawatts(this.calculatePowerConsumption(pressurizer)),
            this.minerUpgrade(pressurizer),
            pressurizer.getId(),
            pressurizer.getId()
        };
    }

    private Object[] createExtractorRow(Production production, int baseAmount, double usage, Production pressurizer)
    {
        Building building = production.getSelectedBuilding();
        Double output = ProductionCalculator.calculateResourceProduction(
            (double) baseAmount,
            production.getPurity(),
            pressurizer.getClockSpeed(),
            pressurizer.getUsedSomersloops(),
            pressurizer.getSelectedBuilding().getSomersloopSlots()
        );
        return new Object[] {
            this.localizedName(building.getName()),
            "",
            production.getGroup(),
            this.purityName(production.getPurity()),
            "",
            this.perMinute(output),
            this.perMinute(usage),
            "",
            this.minerUpgrade(production),
            production.getId(),
            production.getId()
        };
    }

    private MinerUpgrade minerUpgrade(Production production)
    {
        Building building = production.getSelectedBuilding();
        return building != null && Set.of("miner_1", "miner_2", "miner_3").contains(building.getKey())
            ? new MinerUpgrade(production.getId(), building.getKey())
            : null;
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

    private Double calculatePowerConsumption(Production production)
    {
        Building building = production.getSelectedBuilding();
        return PowerCalculator.calculateConsumption(
            (double) Math.abs(building.getPower()),
            production.getClockSpeed(),
            production.getUsedSomersloops(),
            building.getSomersloopSlots()
        );
    }

    private String localizedName(Name name)
    {
        return name == null ? "" : name.getLocalizedName(this.selectedLanguage);
    }

    private String purityName(Purity purity)
    {
        return purity == null ? "" : this.language.translate(purity.getKey());
    }

    private String percentage(Double value)
    {
        return value == null ? "" : this.format(value) + "%";
    }

    private String megawatts(Double value)
    {
        return this.format(value) + " MW";
    }

    private String perMinute(Double value)
    {
        return this.format(value) + " / min";
    }

    private String format(Double value)
    {
        return value == null ? "" : this.numberFormat.format(value);
    }
}
