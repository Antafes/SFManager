package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Purity;
import antafes.sfmanager.entity.Recipe;
import antafes.sfmanager.repository.BuildingRepository;
import antafes.utilities.gui.OrderFocusTraversalPolicy;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import jakarta.annotation.PostConstruct;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

@org.springframework.stereotype.Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NewProductionPanel extends BasePanel
{
    private final String selectedLanguage;
    private JLabel buildingSelectLabel;
    private JComboBox<Building> buildingSelect;
    private JLabel recipeSelectLabel;
    private JComboBox<Recipe> recipeSelect;
    private JLabel amountLabel;
    private JTextField amountField;
    private JLabel groupLabel;
    private JComboBox<String> groupField;
    private JLabel positionLabel;
    private JTextField positionField;
    private JLabel puritySelectLabel;
    private JComboBox<Purity> puritySelect;
    private JLabel clockSpeedLabel;
    private JTextField clockSpeedField;
    private JLabel usedSomersloopsLabel;
    private JTextField usedSomersloopsField;
    private JLabel ingredientsLabel;
    private final List<JLabel> ingredientLabels = new ArrayList<>();
    private JLabel productionLabel;
    private final List<JLabel> productionLabels = new ArrayList<>();
    private JLabel consumptionLabel;
    private JLabel consumptionInfoLabel;
    private JButton okButton;
    private JButton cancelButton;

    @Autowired
    private BuildingRepository buildingRepository;

    public NewProductionPanel(JDialog parent, Configuration configuration)
    {
        super(parent, configuration);
        this.selectedLanguage = configuration.getLanguage().getName();
    }

    @PostConstruct
    private void initializePanel()
    {
        this.init();
    }

    @Override
    protected void init()
    {
        this.initComponents();
        this.setFieldTexts();
        this.createFocusTraversalPolicy();
    }

    @Override
    protected void setFieldTexts()
    {
        this.buildingSelectLabel.setText(this.getLanguage().translate("building"));
        this.recipeSelectLabel.setText(this.getLanguage().translate("recipe"));
        this.amountLabel.setText(this.getLanguage().translate("amount"));
        this.groupLabel.setText(this.getLanguage().translate("group"));
        this.positionLabel.setText(this.getLanguage().translate("position"));
        this.puritySelectLabel.setText(this.getLanguage().translate("purity"));
        this.clockSpeedLabel.setText(this.getLanguage().translate("clockSpeed"));
        this.usedSomersloopsLabel.setText(this.getLanguage().translate("usedSomersloops"));
        this.ingredientsLabel.setText(this.getLanguage().translate("ingredients"));
        this.productionLabel.setText(this.getLanguage().translate("production"));
        this.consumptionLabel.setText(this.getLanguage().translate("consumption"));
        this.okButton.setText(this.getLanguage().translate("ok"));
        this.cancelButton.setText(this.getLanguage().translate("cancel"));
    }

    @Override
    protected void createFocusTraversalPolicy()
    {
        Vector<Component> order = new Vector<>();
        order.add(this.buildingSelect);
        order.add(this.recipeSelect);
        order.add(this.amountField);
        order.add(this.groupField);
        order.add(this.positionField);
        order.add(this.puritySelect);
        order.add(this.clockSpeedField);
        order.add(this.usedSomersloopsField);
        order.add(this.okButton);
        order.add(this.cancelButton);
        this.setFocusTraversalPolicy(new OrderFocusTraversalPolicy(order));
        this.setFocusTraversalPolicyProvider(true);
    }

    private String getBuildingDisplayName(Building building)
    {
        if (building == null || building.getName() == null) {
            return "";
        }
        return building.getName().getLocalizedName(this.selectedLanguage);
    }

    private void initComponents()
    {
        this.buildingSelectLabel = new JLabel();
        this.buildingSelect = new JComboBox<>();
        this.buildingSelect.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            ) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                this.setText(value instanceof Building building
                    ? NewProductionPanel.this.getBuildingDisplayName(building)
                    : "");
                return this;
            }
        });
        this.buildingRepository.findAll().values().stream()
            .sorted(Comparator.comparing(this::getBuildingDisplayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Building::getKey))
            .forEach(this.buildingSelect::addItem);
        this.recipeSelectLabel = new JLabel();
        this.recipeSelect = new JComboBox<>();
        this.amountLabel = new JLabel();
        this.amountField = new JTextField();
        this.groupLabel = new JLabel();
        this.groupField = new JComboBox<>();
        this.positionLabel = new JLabel();
        this.positionField = new JTextField();
        this.puritySelectLabel = new JLabel();
        this.puritySelect = new JComboBox<>();
        this.clockSpeedLabel = new JLabel();
        this.clockSpeedField = new JTextField();
        this.usedSomersloopsLabel = new JLabel();
        this.usedSomersloopsField = new JTextField();
        this.ingredientsLabel = new JLabel();
        this.productionLabel = new JLabel();
        this.consumptionLabel = new JLabel();
        this.consumptionInfoLabel = new JLabel();
        this.okButton = new JButton();
        this.cancelButton = new JButton();

        GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);
        GridBagConstraints constraints = this.setupConstraints();

        constraints.gridwidth = 2;
        this.add(this.buildingSelectLabel, constraints);
        constraints.gridx += 3;
        this.add(this.buildingSelect, constraints);
        constraints.gridx = 0;
        constraints.gridy++;

        this.add(this.recipeSelectLabel, constraints);
        constraints.gridx += 3;
        this.add(this.recipeSelect, constraints);
        constraints.gridx = 0;
        constraints.gridy++;
    }

    private @NonNull GridBagConstraints setupConstraints()
    {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.ipady = 2;
        constraints.insets.set(2, 2, 2, 2);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;

        return constraints;
    }
}
