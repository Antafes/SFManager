package antafes.sfmanager.gui;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Name;
import antafes.sfmanager.entity.Production;
import antafes.sfmanager.entity.Purity;
import antafes.sfmanager.service.ProductionService;
import antafes.utilities.gui.OrderFocusTraversalPolicy;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.Vector;

@org.springframework.stereotype.Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EditProductionPanel extends BasePanel
{
    @Autowired
    private ProductionService productionService;

    private final UUID productionId;
    private final String selectedLanguage;
    private final NumberFormat clockSpeedFormat;
    private Production production;
    private JLabel buildingLabel;
    private JTextField buildingField;
    private JLabel pressurizerLabel;
    private JTextField pressurizerField;
    private JLabel recipeLabel;
    private JTextField recipeField;
    private JLabel groupLabel;
    private JTextField groupField;
    private JLabel positionLabel;
    private JTextField positionField;
    private JLabel purityLabel;
    private JTextField purityField;
    private JLabel clockSpeedLabel;
    private JSlider clockSpeedSlider;
    private JTextField clockSpeedField;
    private JLabel usedSomersloopsLabel;
    private JTextField usedSomersloopsField;
    private JButton saveButton;
    private JButton cancelButton;
    private boolean syncingClockSpeed;

    public EditProductionPanel(JDialog parent, Configuration configuration, UUID productionId)
    {
        super(parent, configuration);
        this.productionId = productionId;
        this.selectedLanguage = configuration.getLanguage().getName();
        Locale locale = "German".equals(this.selectedLanguage) ? Locale.GERMAN : Locale.ENGLISH;
        this.clockSpeedFormat = NumberFormat.getNumberInstance(locale);
        this.clockSpeedFormat.setGroupingUsed(false);
        this.clockSpeedFormat.setMaximumFractionDigits(10);
        this.clockSpeedFormat.setMinimumFractionDigits(0);
    }

    @PostConstruct
    private void initializePanel()
    {
        this.production = this.productionService.getProductionForEditing(this.productionId);
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
        this.buildingLabel.setText(this.getLanguage().translate("building"));
        this.pressurizerLabel.setText(this.getLanguage().translate("pressurizer"));
        this.groupLabel.setText(this.getLanguage().translate("group"));
        this.positionLabel.setText(this.getLanguage().translate("position"));
        this.purityLabel.setText(this.getLanguage().translate("purity"));
        this.clockSpeedLabel.setText(this.getLanguage().translate("clockSpeed"));
        this.usedSomersloopsLabel.setText(this.getLanguage().translate("usedSomersloops"));
        this.saveButton.setText(this.getLanguage().translate("save"));
        this.cancelButton.setText(this.getLanguage().translate("cancel"));
    }

    @Override
    protected void createFocusTraversalPolicy()
    {
        Vector<Component> order = new Vector<>();
        if (this.groupField.isEnabled()) {
            order.add(this.groupField);
        }
        order.add(this.positionField);
        if (this.clockSpeedSlider.isEnabled()) {
            order.add(this.clockSpeedSlider);
            order.add(this.clockSpeedField);
        }
        if (this.usedSomersloopsField.isEnabled()) {
            order.add(this.usedSomersloopsField);
        }
        order.add(this.saveButton);
        order.add(this.cancelButton);
        this.setFocusTraversalPolicy(new OrderFocusTraversalPolicy(order));
        this.setFocusTraversalPolicyProvider(true);
    }

    private void addFieldRow(int row, JLabel label, Component field)
    {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.insets = new Insets(3, 4, 3, 8);
        labelConstraints.anchor = GridBagConstraints.LINE_START;
        this.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.gridwidth = 3;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(3, 4, 3, 4);
        this.add(field, fieldConstraints);
    }

    private void initComponents()
    {
        Building building = this.production.getSelectedBuilding();
        this.buildingLabel = new JLabel();
        this.buildingField = this.readOnlyField(this.localizedName(building.getName()));
        this.pressurizerLabel = new JLabel();
        this.pressurizerField = this.readOnlyField(this.pressurizerName());
        this.recipeLabel = new JLabel(this.production.getSelectedItem() == null
            ? this.getLanguage().translate("recipe")
            : this.getLanguage().translate("item"));
        this.recipeField = this.readOnlyField(this.productionName());
        this.groupLabel = new JLabel();
        this.groupField = new JTextField(Objects.toString(this.production.getGroup(), ""));
        this.positionLabel = new JLabel();
        this.positionField = new JTextField(Objects.toString(this.production.getPosition(), ""));
        this.purityLabel = new JLabel();
        this.purityField = this.readOnlyField(this.purityName(this.production.getPurity()));
        this.clockSpeedLabel = new JLabel();
        this.clockSpeedSlider = new JSlider(0, 250, (int) Math.round(this.production.getClockSpeed()));
        this.clockSpeedSlider.setMajorTickSpacing(50);
        this.clockSpeedSlider.setMinorTickSpacing(10);
        this.clockSpeedSlider.setPaintTicks(true);
        this.clockSpeedSlider.setPaintLabels(true);
        this.clockSpeedSlider.setEnabled(building.isOverclockable());
        this.clockSpeedField = new JTextField(this.clockSpeedFormat.format(this.production.getClockSpeed()), 8);
        this.clockSpeedField.setEnabled(building.isOverclockable());
        this.usedSomersloopsLabel = new JLabel();
        this.usedSomersloopsField = new JTextField(Integer.toString(this.production.getUsedSomersloops()));
        this.usedSomersloopsField.setEnabled(building.getSomersloopSlots() > 0);
        this.saveButton = new JButton();
        this.cancelButton = new JButton();

        JPanel clockSpeedInput = new JPanel(new BorderLayout());
        clockSpeedInput.add(this.clockSpeedSlider, BorderLayout.CENTER);
        JPanel clockSpeedValuePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        clockSpeedValuePanel.add(this.clockSpeedField);
        clockSpeedInput.add(clockSpeedValuePanel, BorderLayout.SOUTH);

        this.setLayout(new GridBagLayout());
        this.addFieldRow(0, this.buildingLabel, this.buildingField);
        this.addFieldRow(1, this.pressurizerLabel, this.pressurizerField);
        this.addFieldRow(2, this.recipeLabel, this.recipeField);
        this.addFieldRow(3, this.groupLabel, this.groupField);
        this.addFieldRow(4, this.positionLabel, this.positionField);
        this.addFieldRow(5, this.purityLabel, this.purityField);
        this.addFieldRow(6, this.clockSpeedLabel, clockSpeedInput);
        this.addFieldRow(7, this.usedSomersloopsLabel, this.usedSomersloopsField);

        this.setRowVisible(this.pressurizerLabel, this.pressurizerField,
            "well_extractor".equals(building.getKey()));
        this.setRowVisible(this.recipeLabel, this.recipeField,
            this.production.getSelectedRecipe() != null || this.production.getSelectedItem() != null);

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridy = 8;
        buttonConstraints.gridx = 2;
        buttonConstraints.insets = new Insets(8, 4, 4, 4);
        buttonConstraints.anchor = GridBagConstraints.LINE_END;
        this.add(this.saveButton, buttonConstraints);
        buttonConstraints.gridx = 3;
        this.add(this.cancelButton, buttonConstraints);

        this.getParentDialog().getRootPane().setDefaultButton(this.saveButton);
        this.saveButton.addActionListener(event -> this.saveProduction());
        this.cancelButton.addActionListener(event -> this.getParentDialog().dispose());
        this.clockSpeedSlider.addChangeListener(event -> this.syncClockSpeedFieldFromSlider());
        this.clockSpeedField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event)
            {
                EditProductionPanel.this.updateClockSpeedSliderFromField();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event)
            {
                EditProductionPanel.this.updateClockSpeedSliderFromField();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event)
            {
                EditProductionPanel.this.updateClockSpeedSliderFromField();
            }
        });
        this.clockSpeedField.addActionListener(event -> this.commitClockSpeedField());
    }

    private JTextField readOnlyField(String value)
    {
        JTextField field = new JTextField(value);
        field.setEnabled(false);
        return field;
    }

    private void setRowVisible(JLabel label, Component field, boolean visible)
    {
        label.setVisible(visible);
        field.setVisible(visible);
    }

    private void syncClockSpeedFieldFromSlider()
    {
        if (!this.syncingClockSpeed) {
            this.clockSpeedField.setText(this.clockSpeedFormat.format(this.clockSpeedSlider.getValue()));
        }
    }

    private void updateClockSpeedSliderFromField()
    {
        if (this.syncingClockSpeed) {
            return;
        }

        Double clockSpeed = this.parseClockSpeed();
        if (clockSpeed != null && clockSpeed <= 250) {
            this.syncingClockSpeed = true;
            this.clockSpeedSlider.setValue((int) Math.round(clockSpeed));
            this.syncingClockSpeed = false;
        }
    }

    private void commitClockSpeedField()
    {
        Double clockSpeed = this.parseClockSpeed();
        if (clockSpeed == null) {
            this.syncClockSpeedFieldFromSlider();
            return;
        }
        this.syncingClockSpeed = true;
        this.clockSpeedSlider.setValue((int) Math.round(Math.min(clockSpeed, 250)));
        this.syncingClockSpeed = false;
        this.clockSpeedField.setText(this.clockSpeedFormat.format(Math.min(clockSpeed, 250)));
    }

    private void saveProduction()
    {
        try {
            Double clockSpeed = this.parseClockSpeed();
            if (clockSpeed == null || clockSpeed > 250) {
                throw new IllegalArgumentException("Clock speed must be between 0 and 250.");
            }
            int usedSomersloops = this.parseNonNegativeInteger(this.usedSomersloopsField.getText());
            this.productionService.editProduction(
                this.productionId,
                clockSpeed,
                this.groupField.getText(),
                this.positionField.getText(),
                usedSomersloops
            );
            ((EditProductionDialog) this.getParentDialog()).setSaved();
            this.getParentDialog().dispose();
        } catch (IOException | RuntimeException exception) {
            javax.swing.JOptionPane.showMessageDialog(
                this.getParentDialog(),
                Objects.toString(
                    exception.getMessage(),
                    this.getLanguage().translate("productionEditingFailed")
                ),
                this.getLanguage().translate("editProduction"),
                javax.swing.JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private Double parseClockSpeed()
    {
        String value = this.clockSpeedField.getText().trim();
        if (value.isEmpty()) {
            return null;
        }
        ParsePosition position = new ParsePosition(0);
        Number number = this.clockSpeedFormat.parse(value, position);
        if (number == null || position.getIndex() != value.length()) {
            return null;
        }
        double clockSpeed = number.doubleValue();
        return Double.isFinite(clockSpeed) && clockSpeed >= 0 ? clockSpeed : null;
    }

    private int parseNonNegativeInteger(String value)
    {
        int parsedValue = Integer.parseInt(value.trim());
        if (parsedValue < 0) {
            throw new IllegalArgumentException("Somersloop count must be a non-negative integer.");
        }
        return parsedValue;
    }

    private String pressurizerName()
    {
        if (this.production.getPressurizerId() == null) {
            return "";
        }
        return this.productionService.getPressurizers().stream()
            .filter(candidate -> this.production.getPressurizerId().equals(candidate.getId()))
            .findFirst()
            .map(candidate -> {
                String group = candidate.getGroup();
                String id = candidate.getId().toString().substring(0, 8);
                return group == null || group.isBlank()
                    ? this.getLanguage().translate("pressurizer") + " " + id
                    : group + " (" + id + ")";
            })
            .orElse("");
    }

    private String productionName()
    {
        if (this.production.getSelectedItem() != null) {
            return this.localizedName(this.production.getSelectedItem().getName());
        }
        return this.production.getSelectedRecipe() == null
            ? ""
            : this.localizedName(this.production.getSelectedRecipe().getName());
    }

    private String purityName(Purity purity)
    {
        return purity == null ? "" : this.getLanguage().translate(purity.getKey());
    }

    private String localizedName(Name name)
    {
        return name == null ? "" : name.getLocalizedName(this.selectedLanguage);
    }
}
