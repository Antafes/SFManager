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
import antafes.sf.manager.entity.Category;
import antafes.sf.manager.entity.Name;
import antafes.sf.manager.gui.element.PowerSignalLight;
import antafes.sf.manager.gui.order.LocalizedBuildingOrder;
import antafes.sf.manager.gui.order.LocalizedItemOrder;
import antafes.sf.manager.gui.renderer.MaterialStatusRenderer;
import antafes.sf.manager.gui.renderer.TableSort;
import antafes.sf.manager.gui.renderer.TableStyle;
import antafes.sf.manager.gui.util.PersistentToolTip;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.service.dto.*;
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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@org.springframework.stereotype.Component
@Scope("prototype")
public class FactoryOverviewPanel extends JPanel
{
    @Autowired
    private ProductionService productionService;
    @Autowired
    private Configuration configuration;

    private String selectedLanguage;
    private LanguageInterface language;
    private NumberFormat numberFormat;
    private DefaultTableModel materialTableModel;
    private DefaultTableModel powerSourceTableModel;
    private DefaultTableModel consumerTableModel;
    @Getter
    private JTable materialTable;
    @Getter
    private JTable powerSourceTable;
    @Getter
    private JTable consumerTable;
    @Getter
    private JLabel totalPowerProductionLabel;
    private JLabel totalPowerConsumptionLabel;
    @Getter
    private JLabel availablePowerLabel;
    @Getter
    private PowerSignalLight powerSignalLight;

    public FactoryOverviewPanel()
    {
    }

    @PostConstruct
    private void initializePanel()
    {
        this.selectedLanguage = this.configuration.getLanguage().getName();
        this.language = this.configuration.getLanguageObject();
        this.numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag(
            this.selectedLanguage.equals("German") ? "de" : "en"
        ));
        this.createTables();
        this.createLayout();
        this.refresh();
    }

    public void refresh()
    {
        FactoryOverviewData overview = this.productionService.getFactoryOverview();
        this.materialTableModel.setRowCount(0);
        for (FactoryOverviewMaterial material : overview.materials().stream()
            .sorted(Comparator.comparing(
                FactoryOverviewMaterial::item,
                LocalizedItemOrder.comparator(this.selectedLanguage)
            ))
            .toList()) {
            this.materialTableModel.addRow(new Object[] {
                this.localizedName(material.item().getName()),
                this.perMinute(material.production()),
                this.perMinute(material.usage()),
                this.perMinute(material.production() - material.usage()),
                this.megawatts(material.powerConsumption()),
                MaterialStatus.of(material.production(), material.usage())
            });
        }

        this.powerSourceTableModel.setRowCount(0);
        Collator collator = Collator.getInstance(Locale.forLanguageTag(
            "German".equalsIgnoreCase(this.selectedLanguage) ? "de" : "en"
        ));
        collator.setStrength(Collator.SECONDARY);
        for (FactoryOverviewPowerSource powerSource : overview.powerSources().stream()
            .sorted((left, right) -> {
                int displayNameOrder = collator.compare(
                    this.localizedName(left.building().getName()),
                    this.localizedName(right.building().getName())
                );
                return displayNameOrder != 0
                    ? displayNameOrder
                    : left.building().getKey().compareTo(right.building().getKey());
            })
            .toList()) {
            this.powerSourceTableModel.addRow(new Object[] {
                this.localizedName(powerSource.building().getName()),
                this.megawatts(powerSource.production())
            });
        }

        this.consumerTableModel.setRowCount(0);
        Map<String, Building> consumerBuildings = new LinkedHashMap<>();
        Map<String, Double> consumerPower = new LinkedHashMap<>();
        for (ConsumerGroupEntry entry : this.productionService.getConsumerGroupEntries(null)) {
            consumerBuildings.putIfAbsent(entry.building().getKey(), entry.building());
            consumerPower.merge(entry.building().getKey(), entry.powerConsumption(), Double::sum);
        }
        consumerBuildings.values().stream()
            .sorted(LocalizedBuildingOrder.comparator(this.selectedLanguage))
            .forEach(building -> this.consumerTableModel.addRow(new Object[] {
                this.localizedName(building.getName()),
                this.megawatts(consumerPower.get(building.getKey()))
            }));

        this.totalPowerProductionLabel.setText(this.megawatts(overview.totalPowerProduction()));
        this.totalPowerConsumptionLabel.setText(this.megawatts(overview.totalPowerConsumption()));
        this.availablePowerLabel.setText(this.megawatts(overview.availablePower()));
        this.powerSignalLight.setStatus(PowerStatus.of(overview.totalPowerProduction(), overview.availablePower()));
        this.powerSignalLight.setToolTipText(this.powerTooltip(this.powerSignalLight.getStatus()));
    }

    private void createTables()
    {
        this.materialTableModel = new DefaultTableModel(new Object[] {
            this.language.translate("material"),
            this.language.translate("production"),
            this.language.translate("usage"),
            this.language.translate("unused"),
            this.language.translate("consumption"),
            this.language.translate("status")
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        };
        this.materialTable = new JTable(this.materialTableModel);
        TableStyle.apply(this.materialTable);
        TableSort.apply(this.materialTable, this.numberFormat);
        this.materialTable.getColumnModel().getColumn(5).setCellRenderer(
            new MaterialStatusRenderer(this.language)
        );
        this.materialTable.getColumnModel().getColumn(5).setMaxWidth(70);
        PersistentToolTip.apply(this.materialTable);

        this.powerSourceTableModel = new DefaultTableModel(new Object[] {
            this.language.translate("powerSource"),
            this.language.translate("production")
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        };
        this.powerSourceTable = new JTable(this.powerSourceTableModel);
        TableStyle.apply(this.powerSourceTable);

        this.consumerTableModel = new DefaultTableModel(new Object[] {
            this.language.translate("building"),
            this.language.translate("consumption")
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        };
        this.consumerTable = new JTable(this.consumerTableModel);
        TableStyle.apply(this.consumerTable);
    }

    private void createLayout()
    {
        this.setLayout(new GridLayout(1, 2, 8, 0));
        this.add(new JScrollPane(this.materialTable));

        JPanel powerPanel = new JPanel(new BorderLayout(0, 8));
        JPanel summaryPanel = new JPanel(new BorderLayout(8, 4));
        summaryPanel.add(new JLabel(this.language.translate("power")), BorderLayout.NORTH);
        JPanel valuesPanel = new JPanel(new GridBagLayout());
        this.totalPowerProductionLabel = new JLabel();
        this.addPowerRow(valuesPanel, 0, "totalPowerProduction", this.totalPowerProductionLabel);
        this.totalPowerConsumptionLabel = new JLabel();
        this.addPowerRow(valuesPanel, 1, "totalPowerConsumption", this.totalPowerConsumptionLabel);
        this.availablePowerLabel = new JLabel();
        this.addPowerRow(valuesPanel, 2, "availablePower", this.availablePowerLabel);
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        contentPanel.add(valuesPanel);
        this.powerSignalLight = new PowerSignalLight();
        JPanel lightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints lightConstraints = new GridBagConstraints();
        lightConstraints.weightx = 1;
        lightConstraints.weighty = 1;
        lightConstraints.anchor = GridBagConstraints.WEST;
        lightPanel.add(this.powerSignalLight, lightConstraints);
        contentPanel.add(lightPanel);
        summaryPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel powerSourcesPanel = new JPanel(new BorderLayout());
        powerSourcesPanel.add(new JLabel(this.language.translate("powerSources")), BorderLayout.NORTH);
        JScrollPane powerSourceScrollPane = new JScrollPane(this.powerSourceTable);
        long powerBuildingCount = this.productionService.getBuildings().stream()
            .filter(building -> building.getCategory() == Category.POWER)
            .count();
        int tableHeight = this.powerSourceTable.getTableHeader().getPreferredSize().height
            + (int) powerBuildingCount * this.powerSourceTable.getRowHeight() + 2;
        powerSourceScrollPane.setPreferredSize(new Dimension(0, tableHeight));
        powerSourcesPanel.add(powerSourceScrollPane, BorderLayout.CENTER);

        JPanel consumersPanel = new JPanel(new BorderLayout());
        consumersPanel.add(new JLabel(this.language.translate("consumerGroups")), BorderLayout.NORTH);
        consumersPanel.add(new JScrollPane(this.consumerTable), BorderLayout.CENTER);

        JPanel tablesPanel = new JPanel(new BorderLayout(0, 8));
        tablesPanel.add(powerSourcesPanel, BorderLayout.NORTH);
        tablesPanel.add(consumersPanel, BorderLayout.CENTER);
        powerPanel.add(summaryPanel, BorderLayout.NORTH);
        powerPanel.add(tablesPanel, BorderLayout.CENTER);
        this.add(powerPanel);
    }

    private void addPowerRow(JPanel panel, int row, String captionKey, JLabel valueLabel)
    {
        GridBagConstraints captionConstraints = new GridBagConstraints();
        captionConstraints.gridx = 0;
        captionConstraints.gridy = row;
        captionConstraints.weighty = 1;
        captionConstraints.anchor = GridBagConstraints.WEST;
        captionConstraints.insets = new Insets(0, 0, 0, 8);
        panel.add(new JLabel(this.language.translate(captionKey) + ":"), captionConstraints);

        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = row;
        valueConstraints.weightx = 1;
        valueConstraints.weighty = 1;
        valueConstraints.anchor = GridBagConstraints.WEST;
        panel.add(valueLabel, valueConstraints);
    }

    private String localizedName(Name name)
    {
        return name == null ? "" : name.getLocalizedName(this.selectedLanguage);
    }

    private String powerTooltip(PowerStatus status)
    {
        return "<html><b>" + this.language.translate(status.getTranslationKey()) + "</b><hr>"
            + this.language.translate("powerStatusRed") + "<br>"
            + this.language.translate("powerStatusYellow") + "<br>"
            + this.language.translate("powerStatusGreen")
            + "</html>";
    }

    private String perMinute(double amount)
    {
        return this.numberFormat.format(amount) + " / min";
    }

    private String megawatts(double amount)
    {
        return this.numberFormat.format(amount) + " MW";
    }
}
