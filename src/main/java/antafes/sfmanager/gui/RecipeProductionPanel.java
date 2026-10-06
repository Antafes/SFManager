package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Item;
import antafes.sfmanager.entity.Name;
import antafes.sfmanager.gui.editor.ProductionDeleteButtonEditor;
import antafes.sfmanager.service.ProductionService;
import antafes.sfmanager.service.dto.RecipeProductionEntry;
import antafes.sfmanager.service.dto.RecipeProductionMaterial;
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
public class RecipeProductionPanel extends JPanel
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
            this.language.translate("usedMaterials"),
            this.language.translate("recipe"),
            this.language.translate("clockSpeed"),
            this.language.translate("somersloops"),
            this.language.translate("producedMaterials"),
            this.language.translate("usage"),
            this.language.translate("consumption"),
            ""
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return column == 7;
            }
        };
        this.table = new JTable(this.tableModel);
        this.installDeleteButton();
        this.setLayout(new BorderLayout());
        this.add(new JScrollPane(this.table), BorderLayout.CENTER);
        this.refresh();
    }

    public void refresh()
    {
        this.tableModel.setRowCount(0);
        List<RecipeProductionEntry> entries = this.productionService.getRecipeProductions(this.item);
        for (RecipeProductionEntry entry : entries) {
            this.tableModel.addRow(new Object[] {
                this.materials(entry.usedMaterials()),
                this.localizedName(entry.recipe().getName()),
                this.percentage(entry.production().getClockSpeed()),
                entry.production().getUsedSomersloops(),
                this.materials(entry.producedMaterials()),
                "",
                this.consumption(entry.powerConsumption()),
                entry.production().getId()
            });
        }
    }

    private void installDeleteButton()
    {
        var actionColumn = this.table.getColumnModel().getColumn(7);
        actionColumn.setPreferredWidth(80);
        actionColumn.setMaxWidth(100);
        ProductionDeleteButtonEditor deleteButton = new ProductionDeleteButtonEditor(
            this.productionService,
            this.language,
            this,
            this::refresh
        );
        actionColumn.setCellRenderer(deleteButton);
        actionColumn.setCellEditor(deleteButton);
    }

    private String materials(List<RecipeProductionMaterial> materials)
    {
        return materials.stream()
            .map(material -> this.localizedName(material.item().getName())
                + " (" + this.numberFormat.format(material.amount()) + ")")
            .collect(Collectors.joining(", "));
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
}
