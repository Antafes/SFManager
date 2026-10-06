package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Name;
import antafes.sfmanager.gui.order.LocalizedItemOrder;
import antafes.sfmanager.service.ProductionService;
import antafes.sfmanager.service.dto.FactoryOverviewData;
import antafes.sfmanager.service.dto.FactoryOverviewMaterial;
import antafes.sfmanager.service.dto.FactoryOverviewPowerSource;
import antafes.utilities.language.LanguageInterface;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.Collator;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Locale;

@org.springframework.stereotype.Component
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
    @Getter
    private JTable materialTable;
    @Getter
    private JTable powerSourceTable;
    private JLabel totalPowerProductionLabel;
    private JLabel totalPowerConsumptionLabel;
    private JLabel availablePowerLabel;

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
                this.megawatts(material.powerConsumption())
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

        this.totalPowerProductionLabel.setText(
            this.language.translate("totalPowerProduction") + ": " + this.megawatts(overview.totalPowerProduction())
        );
        this.totalPowerConsumptionLabel.setText(
            this.language.translate("totalPowerConsumption") + ": " + this.megawatts(overview.totalPowerConsumption())
        );
        this.availablePowerLabel.setText(
            this.language.translate("availablePower") + ": " + this.megawatts(overview.availablePower())
        );
    }

    private void createTables()
    {
        this.materialTableModel = new DefaultTableModel(new Object[] {
            this.language.translate("material"),
            this.language.translate("production"),
            this.language.translate("usage"),
            this.language.translate("unused"),
            this.language.translate("consumption")
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        };
        this.materialTable = new JTable(this.materialTableModel);
        this.materialTable.setAutoCreateRowSorter(true);

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
    }

    private void createLayout()
    {
        this.setLayout(new GridLayout(1, 2, 8, 0));
        this.add(new JScrollPane(this.materialTable));

        JPanel powerPanel = new JPanel(new BorderLayout(0, 8));
        JPanel summaryPanel = new JPanel(new GridLayout(4, 2, 8, 4));
        summaryPanel.add(new JLabel(this.language.translate("power")));
        summaryPanel.add(new JLabel());
        summaryPanel.add(new JLabel(this.language.translate("totalPowerProduction")));
        this.totalPowerProductionLabel = new JLabel();
        summaryPanel.add(this.totalPowerProductionLabel);
        summaryPanel.add(new JLabel(this.language.translate("totalPowerConsumption")));
        this.totalPowerConsumptionLabel = new JLabel();
        summaryPanel.add(this.totalPowerConsumptionLabel);
        summaryPanel.add(new JLabel(this.language.translate("availablePower")));
        this.availablePowerLabel = new JLabel();
        summaryPanel.add(this.availablePowerLabel);

        JPanel powerSourcesPanel = new JPanel(new BorderLayout());
        powerSourcesPanel.add(new JLabel(this.language.translate("powerSources")), BorderLayout.NORTH);
        powerSourcesPanel.add(new JScrollPane(this.powerSourceTable), BorderLayout.CENTER);
        powerPanel.add(summaryPanel, BorderLayout.NORTH);
        powerPanel.add(powerSourcesPanel, BorderLayout.CENTER);
        this.add(powerPanel);
    }

    private String localizedName(Name name)
    {
        return name == null ? "" : name.getLocalizedName(this.selectedLanguage);
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
