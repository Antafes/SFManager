package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Name;
import antafes.sfmanager.gui.editor.ProductionDeleteButtonEditor;
import antafes.sfmanager.gui.editor.ProductionEditButtonEditor;
import antafes.sfmanager.service.ProductionService;
import antafes.sfmanager.service.dto.GeneratorProductionEntry;
import antafes.sfmanager.service.dto.GeneratorProductionIngredient;
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
        this.installEditButton();
        this.installDeleteButton();
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
