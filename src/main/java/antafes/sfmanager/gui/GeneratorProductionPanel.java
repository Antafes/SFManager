package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Name;
import antafes.sfmanager.service.ProductionService;
import antafes.sfmanager.service.dto.GeneratorProductionEntry;
import antafes.sfmanager.service.dto.GeneratorProductionIngredient;
import antafes.utilities.language.LanguageInterface;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
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
public class GeneratorProductionPanel extends JPanel
{
    @Autowired
    private ProductionService productionService;
    @Autowired
    private Configuration configuration;

    @Getter
    private final Building building;
    private String selectedLanguage;
    private LanguageInterface language;
    private NumberFormat numberFormat;
    private DefaultTableModel tableModel;
    @Getter
    private JTable table;

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
            ""
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        };
        this.table = new JTable(this.tableModel);
        this.table.getColumnModel().getColumn(5).setPreferredWidth(80);
        this.table.getColumnModel().getColumn(5).setMaxWidth(100);
        this.setLayout(new BorderLayout());
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
                this.usedMaterials(entry.usedMaterials())
            });
        }
    }

    private String usedMaterials(List<GeneratorProductionIngredient> ingredients)
    {
        return ingredients.stream()
            .map(ingredient -> this.localizedName(ingredient.item().getName())
                + " (" + this.format(ingredient.amount()) + ")")
            .collect(Collectors.joining(", "));
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
