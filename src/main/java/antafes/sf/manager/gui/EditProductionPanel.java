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

import antafes.sf.manager.entity.*;
import antafes.sf.manager.entity.recipe.Item;
import antafes.sf.manager.Configuration;
import antafes.sf.manager.gui.focus.DynamicFocusTraversalPolicy;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.service.dto.MaterialSourceGroup;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.List;

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
    private JLabel materialSourcesLabel;
    private JPanel materialSourcesPanel;
    private final Map<String, JComboBox<MaterialSourceGroup>> materialSourceFields = new LinkedHashMap<>();
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
    private JLabel requiredFieldsHintLabel;
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
        this.buildingLabel.setText(this.requiredFieldLabel("building"));
        this.pressurizerLabel.setText(this.requiredFieldLabel("pressurizer"));
        this.recipeLabel.setText(this.requiredFieldLabel(
            this.production.getSelectedItem() == null ? "recipe" : "item"
        ));
        this.groupLabel.setText(this.requiredFieldLabel("group"));
        this.materialSourcesLabel.setText(this.requiredFieldLabel("materialSources"));
        this.positionLabel.setText(this.getLanguage().translate("position"));
        this.purityLabel.setText(this.requiredFieldLabel("purity"));
        this.clockSpeedLabel.setText(this.requiredFieldLabel("clockSpeed"));
        this.usedSomersloopsLabel.setText(this.requiredFieldLabel("usedSomersloops"));
        this.saveButton.setText(this.getLanguage().translate("save"));
        this.cancelButton.setText(this.getLanguage().translate("cancel"));
        this.requiredFieldsHintLabel.setText(this.getLanguage().translate("requiredFieldsHint"));
    }

    @Override
    protected void createFocusTraversalPolicy()
    {
        List<Component> order = new java.util.ArrayList<>();
        if (this.groupField.isEnabled()) {
            order.add(this.groupField);
        }
        order.add(this.positionField);
        if (this.clockSpeedSlider.isEnabled()) {
            order.add(this.clockSpeedSlider);
            order.add(this.clockSpeedField);
        }
        order.addAll(this.materialSourceFields.values());
        if (this.usedSomersloopsField.isEnabled()) {
            order.add(this.usedSomersloopsField);
        }
        order.add(this.saveButton);
        order.add(this.cancelButton);
        this.setFocusTraversalPolicy(new DynamicFocusTraversalPolicy(order));
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

    private void addFullWidthRow(int row, Component component)
    {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 4;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(4, 4, 3, 4);
        this.add(component, constraints);
    }

    private List<ProductionMaterialSource> getMaterialSources()
    {
        return this.materialSourceFields.entrySet().stream()
            .map(entry -> {
                MaterialSourceGroup sourceGroup = (MaterialSourceGroup) entry.getValue().getSelectedItem();
                return new ProductionMaterialSource()
                    .setItemKey(entry.getKey())
                    .setGroup(sourceGroup == null ? "" : sourceGroup.group());
            })
            .toList();
    }

    private String requiredFieldLabel(String key)
    {
        return this.getLanguage().translate(key) + " *";
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
        this.materialSourcesLabel = new JLabel();
        this.materialSourcesPanel = new JPanel(new GridBagLayout());
        this.materialSourcesLabel.setVisible(false);
        this.materialSourcesPanel.setVisible(false);
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
        this.requiredFieldsHintLabel = new JLabel();

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
        this.addFullWidthRow(4, this.materialSourcesLabel);
        this.addFullWidthRow(5, this.materialSourcesPanel);
        this.addFieldRow(6, this.positionLabel, this.positionField);
        this.addFieldRow(7, this.purityLabel, this.purityField);
        this.addFieldRow(8, this.clockSpeedLabel, clockSpeedInput);
        this.addFieldRow(9, this.usedSomersloopsLabel, this.usedSomersloopsField);

        this.setRowVisible(this.pressurizerLabel, this.pressurizerField,
            "well_extractor".equals(building.getKey()));
        this.setRowVisible(this.recipeLabel, this.recipeField,
            this.production.getSelectedRecipe() != null || this.production.getSelectedItem() != null);

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridy = 10;
        buttonConstraints.gridx = 2;
        buttonConstraints.insets = new Insets(8, 4, 4, 4);
        buttonConstraints.anchor = GridBagConstraints.LINE_END;
        this.add(this.saveButton, buttonConstraints);
        buttonConstraints.gridx = 3;
        this.add(this.cancelButton, buttonConstraints);
        this.addFullWidthRow(11, this.requiredFieldsHintLabel);

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
        this.updateMaterialSources();
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
            if (this.groupField.getText().isBlank()) {
                throw new IllegalArgumentException("A production group is required.");
            }
            int usedSomersloops = this.parseNonNegativeInteger(this.usedSomersloopsField.getText());
            this.productionService.editProduction(
                this.productionId,
                clockSpeed,
                this.groupField.getText(),
                this.positionField.getText(),
                usedSomersloops,
                this.getMaterialSources()
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

    private void updateMaterialSources()
    {
        this.materialSourceFields.clear();
        this.materialSourcesPanel.removeAll();
        if (this.production.getSelectedRecipe() == null
            || this.production.getSelectedRecipe().getIngredients() == null
            || this.production.getSelectedRecipe().getIngredients().isEmpty()) {
            return;
        }

        List<ProductionMaterialSource> existingMaterialSources = this.production.getMaterialSources();
        if (existingMaterialSources == null) {
            existingMaterialSources = List.of();
        }
        Map<String, String> selectedGroups = existingMaterialSources.stream()
            .collect(java.util.stream.Collectors.toMap(
                ProductionMaterialSource::getItemKey,
                ProductionMaterialSource::getGroup,
                (first, second) -> first,
                LinkedHashMap::new
            ));
        int row = 0;
        for (Item ingredient : this.production.getSelectedRecipe().getIngredients()) {
            antafes.sf.manager.entity.Item item = ingredient.getResource();
            if (item == null || item.getKey() == null) {
                throw new IllegalStateException("A recipe ingredient has no material.");
            }
            if (item.isHandCollected()) {
                continue;
            }
            JLabel itemLabel = new JLabel(
                item.getName().getLocalizedName(this.selectedLanguage) + " *"
            );
            JComboBox<MaterialSourceGroup> sourceGroupSelect = new JComboBox<>(
                this.productionService.getMaterialSourceGroups(item).toArray(MaterialSourceGroup[]::new)
            );
            sourceGroupSelect.setRenderer(this.materialSourceGroupRenderer());
            String selectedGroup = selectedGroups.get(item.getKey());
            if (selectedGroup != null && this.productionService.getMaterialSourceGroups(item)
                .stream().anyMatch(sourceGroup -> sourceGroup.group().equals(selectedGroup))) {
                for (int index = 0; index < sourceGroupSelect.getItemCount(); index++) {
                    if (selectedGroup.equals(sourceGroupSelect.getItemAt(index).group())) {
                        sourceGroupSelect.setSelectedIndex(index);
                        break;
                    }
                }
            } else {
                sourceGroupSelect.setSelectedIndex(-1);
            }
            this.materialSourceFields.put(item.getKey(), sourceGroupSelect);
            this.addMaterialSourceRow(row++, itemLabel, sourceGroupSelect);
        }
        this.materialSourcesLabel.setVisible(row > 0);
        this.materialSourcesPanel.setVisible(row > 0);
    }

    private void addMaterialSourceRow(
        int row,
        JLabel label,
        JComboBox<MaterialSourceGroup> sourceGroupSelect
    )
    {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.LINE_START;
        labelConstraints.insets = new Insets(3, 4, 3, 8);
        this.materialSourcesPanel.add(label, labelConstraints);

        GridBagConstraints selectConstraints = new GridBagConstraints();
        selectConstraints.gridx = 1;
        selectConstraints.gridy = row;
        selectConstraints.weightx = 1;
        selectConstraints.fill = GridBagConstraints.HORIZONTAL;
        selectConstraints.insets = new Insets(3, 4, 3, 4);
        this.materialSourcesPanel.add(sourceGroupSelect, selectConstraints);
    }

    private DefaultListCellRenderer materialSourceGroupRenderer()
    {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            ) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof MaterialSourceGroup sourceGroup) {
                    this.setText(sourceGroup.group() + " ("
                        + EditProductionPanel.this.clockSpeedFormat.format(sourceGroup.production()) + " / min)");
                }
                return this;
            }
        };
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
