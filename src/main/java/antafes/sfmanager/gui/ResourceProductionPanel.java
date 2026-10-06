package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Item;
import antafes.sfmanager.entity.Name;
import antafes.sfmanager.entity.Production;
import antafes.sfmanager.entity.Purity;
import antafes.sfmanager.service.ProductionService;
import antafes.sfmanager.service.ResourceProductionEntry;
import antafes.sfmanager.util.PowerCalculator;
import antafes.sfmanager.util.ProductionCalculator;
import antafes.utilities.language.LanguageInterface;
import lombok.Getter;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@org.springframework.stereotype.Component
@Scope("prototype")
public class ResourceProductionPanel extends JPanel
{
    @Autowired
    private ProductionService productionService;
    @Autowired
    private Configuration configuration;

    @Getter
    private final Item item;
    private String selectedLanguage;
    private LanguageInterface language;
    private NumberFormat numberFormat;
    private DefaultTableModel tableModel;
    @Getter
    private JTable table;

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
                return false;
            }
        };
        this.table = new JTable(this.tableModel);
        this.setColumnWidths();

        this.setLayout(new BorderLayout());
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
        this.table.getColumnModel().getColumn(8).setPreferredWidth(80);
        this.table.getColumnModel().getColumn(8).setMaxWidth(100);
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
                rows.add(this.createStandardRow(production, entry.baseAmount()));
                continue;
            }

            Production pressurizer = entry.pressurizer();
            if (displayedPressurizers.add(pressurizer.getId())) {
                rows.add(this.createPressurizerRow(pressurizer));
                for (ResourceProductionEntry groupedEntry : wellGroups.get(pressurizer.getId())) {
                    rows.add(this.createExtractorRow(
                        groupedEntry.production(),
                        groupedEntry.baseAmount(),
                        pressurizer
                    ));
                }
            }
        }
        return rows;
    }

    private Object[] createStandardRow(Production production, int baseAmount)
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
            "",
            this.megawatts(consumption),
            ""
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
            ""
        };
    }

    private Object[] createExtractorRow(Production production, int baseAmount, Production pressurizer)
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
            "",
            "",
            ""
        };
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
