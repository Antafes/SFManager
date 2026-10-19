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

package antafes.sf.manager.gui.dialog;

import antafes.sf.manager.Configuration;
import antafes.sf.manager.entity.*;
import antafes.sf.manager.entity.recipe.Item;
import antafes.sf.manager.gui.BasePanel;
import antafes.sf.manager.gui.element.GroupedBuildingSelect;
import antafes.sf.manager.gui.focus.DynamicFocusTraversalPolicy;
import antafes.sf.manager.gui.renderer.LocalizedNameRenderer;
import antafes.sf.manager.gui.util.BulkOperationRunner;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.util.PowerCalculator;
import antafes.sf.manager.util.ProductionCalculator;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.List;

@org.springframework.stereotype.Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NewProductionPanel extends BasePanel
{
    private final String selectedLanguage;
    private final NumberFormat numberFormat;
    private final NumberFormat clockSpeedFormat;
    private double clockSpeed = 100;
    private String lastPrefilledGroup;
    private List<String> groupItems = List.of();
    private boolean updatingGroupFilter;
    private Building updatedBuilding;
    private LocalizedNameRenderer<Recipe> recipeRenderer;
    private LocalizedNameRenderer<antafes.sf.manager.entity.Item> itemRenderer;
    private JLabel buildingSelectLabel;
    private JComboBox<Object> buildingSelect;
    private JLabel pressurizerSelectLabel;
    private JComboBox<Production> pressurizerSelect;
    private JLabel recipeSelectLabel;
    private JComboBox<Recipe> recipeSelect;
    private JComboBox<antafes.sf.manager.entity.Item> itemSelect;
    private JPanel productSelectPanel;
    private JLabel amountLabel;
    private JTextField amountField;
    private JLabel groupLabel;
    private JComboBox<String> groupField;
    private JLabel materialSourcesLabel;
    private JPanel materialSourcesPanel;
    private MaterialSourceSelector materialSourceSelector;
    private JLabel positionLabel;
    private JTextField positionField;
    private JLabel puritySelectLabel;
    private JComboBox<Purity> puritySelect;
    private JLabel clockSpeedLabel;
    private JSlider clockSpeedSlider;
    private JTextField clockSpeedValueField;
    private UUID selectedPressurizerId;
    private JLabel usedSomersloopsLabel;
    private JTextField usedSomersloopsField;
    private JLabel ingredientsLabel;
    private JPanel ingredientsPanel;
    private JLabel productionLabel;
    private JPanel productionPanel;
    private JLabel consumptionLabel;
    private JLabel consumptionInfoLabel;
    private JLabel requiredFieldsHintLabel;
    private JButton okButton;
    private JButton cancelButton;
    private boolean syncingClockSpeedFromText;
    private boolean syncingClockSpeedFromSlider;

    @Autowired
    private ProductionService productionService;

    public NewProductionPanel(JDialog parent, Configuration configuration)
    {
        super(parent, configuration);
        this.selectedLanguage = configuration.getLanguage().getName();
        Locale locale = this.selectedLanguage.equals("German") ? Locale.GERMAN : Locale.ENGLISH;
        this.numberFormat = NumberFormat.getNumberInstance(locale);
        this.numberFormat.setMaximumFractionDigits(2);
        this.numberFormat.setMinimumFractionDigits(0);
        this.clockSpeedFormat = NumberFormat.getNumberInstance(locale);
        this.clockSpeedFormat.setGroupingUsed(false);
        this.clockSpeedFormat.setMaximumFractionDigits(10);
        this.clockSpeedFormat.setMinimumFractionDigits(0);
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
        this.buildingSelectLabel.setText(this.requiredFieldLabel("building"));
        this.pressurizerSelectLabel.setText(this.requiredFieldLabel("pressurizer"));
        this.amountLabel.setText(this.requiredFieldLabel("amount"));
        this.groupLabel.setText(this.requiredFieldLabel("group"));
        this.materialSourcesLabel.setText(this.requiredFieldLabel("materialSources"));
        this.positionLabel.setText(this.getLanguage().translate("position"));
        this.puritySelectLabel.setText(this.requiredFieldLabel("purity"));
        this.clockSpeedLabel.setText(this.requiredFieldLabel("clockSpeed"));
        this.usedSomersloopsLabel.setText(this.requiredFieldLabel("usedSomersloops"));
        this.ingredientsLabel.setText(this.getLanguage().translate("ingredients"));
        this.productionLabel.setText(this.getLanguage().translate("production"));
        this.consumptionLabel.setText(this.getLanguage().translate("consumption"));
        this.okButton.setText(this.getLanguage().translate("ok"));
        this.cancelButton.setText(this.getLanguage().translate("cancel"));
        this.requiredFieldsHintLabel.setText(this.getLanguage().translate("requiredFieldsHint"));
        this.updateSummaries();
    }

    @Override
    protected void createFocusTraversalPolicy()
    {
        Vector<Component> order = new Vector<>();
        order.add(this.buildingSelect);
        order.add(this.pressurizerSelect);
        order.add(this.recipeSelect);
        order.add(this.itemSelect);
        order.add(this.amountField);
        order.add(this.groupField);
        if (this.materialSourceSelector != null) {
            order.addAll(this.materialSourceSelector.getFocusableComponents());
        }
        order.add(this.positionField);
        order.add(this.puritySelect);
        order.add(this.clockSpeedSlider);
        order.add(this.clockSpeedValueField);
        order.add(this.usedSomersloopsField);
        order.add(this.okButton);
        order.add(this.cancelButton);
        this.setFocusTraversalPolicy(new DynamicFocusTraversalPolicy(order));
        this.setFocusTraversalPolicyProvider(true);
    }

    private void addFieldRow(int row, JLabel label, Component field)
    {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.weightx = 0;
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
        return this.materialSourceSelector == null ? List.of() : this.materialSourceSelector.getMaterialSources();
    }

    private String requiredFieldLabel(String key)
    {
        return this.getLanguage().translate(key) + " *";
    }

    private void saveProduction()
    {
        try {
            Building selectedBuilding = (Building) this.buildingSelect.getSelectedItem();
            if (selectedBuilding == null) {
                throw new IllegalArgumentException("A production building is required.");
            }
            Integer amount = this.parsePositiveInteger(this.amountField.getText());
            if (amount == null) {
                throw new IllegalArgumentException("Production amount must be a positive integer.");
            }
            Double selectedClockSpeed = this.parseNonNegativeNumber(
                this.clockSpeedValueField.getText(),
                this.clockSpeedFormat
            );
            if (selectedClockSpeed == null || selectedClockSpeed > 250) {
                throw new IllegalArgumentException("Clock speed must be between 0 and 250.");
            }
            Integer usedSomersloops = this.parseNonNegativeInteger(this.usedSomersloopsField.getText());
            if (usedSomersloops == null) {
                throw new IllegalArgumentException("Somersloop count must be a non-negative integer.");
            }
            Object groupValue = this.groupField.getEditor().getItem();
            String group = groupValue == null ? null : groupValue.toString().trim();
            if (group != null && group.isEmpty()) {
                group = null;
            }
            if (group == null) {
                throw new IllegalArgumentException("A production group is required.");
            }
            String position = this.positionField.getText().trim();
            if (position.isEmpty()) {
                position = null;
            }

            Production production = new Production()
                .setSelectedBuilding(selectedBuilding)
                .setSelectedRecipe(this.itemSelect.getItemCount() > 0
                    ? null
                    : (Recipe) this.recipeSelect.getSelectedItem())
                .setSelectedItem(this.itemSelect.getItemCount() > 0
                    ? (antafes.sf.manager.entity.Item) this.itemSelect.getSelectedItem()
                    : null)
                .setMaterialSources(this.getMaterialSources())
                .setGroup(group)
                .setPosition(position)
                .setPurity((Purity) this.puritySelect.getSelectedItem())
                .setPressurizerId(this.selectedPressurizerId)
                .setClockSpeed(selectedClockSpeed)
                .setUsedSomersloops(usedSomersloops);
            BulkOperationRunner.run(
                this.getParentDialog(),
                this.getLanguage().translate("creatingProductions"),
                () -> this.productionService.saveProductions(production, amount),
                saved -> this.getParentDialog().dispose(),
                this::showSaveError
            );
        } catch (RuntimeException exception) {
            this.showSaveError(exception);
        }
    }

    private void showSaveError(Exception exception)
    {
        String message = Objects.toString(
            exception.getMessage(),
            this.getLanguage().translate("productionSavingFailed")
        );
        JOptionPane.showMessageDialog(
            this.getParentDialog(),
            message,
            this.getLanguage().translate("newProduction"),
            JOptionPane.ERROR_MESSAGE
        );
    }

    private void updateRecipes()
    {
        Building selectedBuilding = (Building) this.buildingSelect.getSelectedItem();
        this.updatedBuilding = selectedBuilding;
        boolean requiresPressurizer = selectedBuilding != null
            && "well_extractor".equals(selectedBuilding.getKey());
        boolean pressurizerVisibilityChanged = this.pressurizerSelect.isVisible() != requiresPressurizer;
        this.pressurizerSelect.removeAllItems();
        if (requiresPressurizer) {
            this.productionService.getPressurizers()
                .forEach(this.pressurizerSelect::addItem);
            this.restoreSelectedPressurizer();
        } else {
            this.selectedPressurizerId = null;
        }
        this.pressurizerSelectLabel.setVisible(requiresPressurizer);
        this.pressurizerSelect.setVisible(requiresPressurizer);
        this.pressurizerSelect.setEnabled(requiresPressurizer && this.pressurizerSelect.getItemCount() > 0);
        if (pressurizerVisibilityChanged) {
            this.resizeDialogToContent();
        }

        boolean overclockable = selectedBuilding != null && selectedBuilding.isOverclockable();
        this.clockSpeedSlider.setEnabled(overclockable);
        this.clockSpeedValueField.setEnabled(overclockable);
        if (requiresPressurizer) {
            this.syncClockSpeedFromSelectedPressurizer();
        } else if (!overclockable) {
            this.clockSpeed = 100;
            this.clockSpeedSlider.setValue(100);
            this.syncClockSpeedFieldFromSlider();
        }

        boolean hasSomersloopSlots = selectedBuilding != null && selectedBuilding.getSomersloopSlots() > 0;
        this.usedSomersloopsField.setEnabled(hasSomersloopSlots);
        if (!hasSomersloopSlots && !"0".equals(this.usedSomersloopsField.getText())) {
            this.usedSomersloopsField.setText("0");
        }

        this.recipeSelect.removeAllItems();
        this.itemSelect.removeAllItems();
        this.recipeSelect.setEnabled(false);
        this.itemSelect.setEnabled(false);
        this.puritySelect.setEnabled(selectedBuilding != null && selectedBuilding.isResourceBuilding());
        if (selectedBuilding == null) {
            this.productSelectPanel.setVisible(false);
            this.recipeSelectLabel.setVisible(false);
            this.updateSummaries();
            this.createFocusTraversalPolicy();
            return;
        }

        List<antafes.sf.manager.entity.Item> resourceItems =
            this.productionService.getItemsForBuilding(selectedBuilding.getKey());
        if (!resourceItems.isEmpty()) {
            resourceItems.stream()
                .sorted(Comparator.comparing(this.itemRenderer::getDisplayName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(antafes.sf.manager.entity.Item::getKey))
                .forEach(this.itemSelect::addItem);
            this.itemSelect.setEnabled(true);
            ((CardLayout) this.productSelectPanel.getLayout()).show(this.productSelectPanel, "item");
            this.productSelectPanel.setVisible(true);
            this.recipeSelectLabel.setVisible(true);
            this.recipeSelectLabel.setText(this.requiredFieldLabel("item"));
        } else {
            this.productionService.getRecipesForBuilding(selectedBuilding.getKey()).stream()
                .sorted(Comparator.comparing(this.recipeRenderer::getDisplayName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Recipe::getKey))
                .forEach(this.recipeSelect::addItem);
            boolean hasRecipes = this.recipeSelect.getItemCount() > 0;
            this.recipeSelect.setEnabled(hasRecipes);
            ((CardLayout) this.productSelectPanel.getLayout()).show(this.productSelectPanel, "recipe");
            this.productSelectPanel.setVisible(hasRecipes);
            this.recipeSelectLabel.setVisible(hasRecipes);
            this.recipeSelectLabel.setText(this.requiredFieldLabel("recipe"));
        }
        this.prefillGroup();
        this.updateMaterialSources();
        this.updateSummaries();
        this.createFocusTraversalPolicy();
    }

    private void updateMaterialSources()
    {
        this.materialSourceSelector = null;
        this.materialSourcesPanel.removeAll();
        Recipe selectedRecipe = (Recipe) this.recipeSelect.getSelectedItem();
        boolean visible = selectedRecipe != null
            && selectedRecipe.getIngredients() != null
            && selectedRecipe.getIngredients().stream()
                .anyMatch(ingredient -> ingredient.getResource() == null
                    || !ingredient.getResource().isHandCollected());
        this.materialSourcesLabel.setVisible(visible);
        this.materialSourcesPanel.setVisible(visible);
        if (!visible) {
            this.resizeDialogToContent();
            return;
        }

        this.materialSourceSelector = new MaterialSourceSelector(
            this.productionService,
            this.getLanguage(),
            this.numberFormat,
            this.selectedLanguage,
            () -> {
                this.resizeDialogToContent();
                this.createFocusTraversalPolicy();
            }
        );
        for (Item ingredient : selectedRecipe.getIngredients()) {
            antafes.sf.manager.entity.Item item = ingredient.getResource();
            if (item == null || item.getKey() == null) {
                throw new IllegalStateException("A recipe ingredient has no material.");
            }
            if (item.isHandCollected()) {
                continue;
            }
            this.materialSourceSelector.addMaterial(item, true, List.of(), Set.of());
        }
        GridBagConstraints selectorConstraints = new GridBagConstraints();
        selectorConstraints.weightx = 1;
        selectorConstraints.fill = GridBagConstraints.HORIZONTAL;
        this.materialSourcesPanel.add(this.materialSourceSelector, selectorConstraints);
        this.materialSourcesPanel.revalidate();
        this.materialSourcesPanel.repaint();
        this.resizeDialogToContent();
    }

    private void prefillGroup()
    {
        Recipe selectedRecipe = (Recipe) this.recipeSelect.getSelectedItem();
        if (selectedRecipe == null && this.itemSelect.getItemCount() > 0) {
            return;
        }

        Object currentValue = this.groupField.getEditor().getItem();
        String current = currentValue == null ? "" : currentValue.toString().trim();
        if (!current.isEmpty() && !current.equals(this.lastPrefilledGroup)) {
            return;
        }

        Name name = selectedRecipe == null || selectedRecipe.getProducts() == null
            || selectedRecipe.getProducts().isEmpty()
            || selectedRecipe.getProducts().getFirst().getResource() == null
            ? this.selectedBuildingName()
            : selectedRecipe.getProducts().getFirst().getResource().getName();
        if (name == null) {
            return;
        }

        String group = name.getLocalizedName(this.selectedLanguage);
        if (this.groupField.getEditor().getEditorComponent() instanceof javax.swing.text.JTextComponent editor) {
            this.applyGroupFilter(editor, group, false);
        }
        this.lastPrefilledGroup = group;
    }

    private void installGroupFilter()
    {
        if (!(this.groupField.getEditor().getEditorComponent() instanceof javax.swing.text.JTextComponent editor)) {
            return;
        }

        editor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event)
            {
                NewProductionPanel.this.scheduleGroupFilter(editor);
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event)
            {
                NewProductionPanel.this.scheduleGroupFilter(editor);
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event)
            {
                NewProductionPanel.this.scheduleGroupFilter(editor);
            }
        });
    }

    private void scheduleGroupFilter(javax.swing.text.JTextComponent editor)
    {
        if (this.updatingGroupFilter) {
            return;
        }

        SwingUtilities.invokeLater(() -> this.filterGroups(editor));
    }

    private void filterGroups(javax.swing.text.JTextComponent editor)
    {
        String text = editor.getText();
        if (this.updatingGroupFilter || Objects.equals(text, this.groupField.getSelectedItem())) {
            return;
        }

        this.applyGroupFilter(editor, text, true);
    }

    private void applyGroupFilter(javax.swing.text.JTextComponent editor, String text, boolean showPopup)
    {
        String needle = text.trim().toLowerCase();
        List<String> matches = this.groupItems.stream()
            .filter(group -> group.toLowerCase().contains(needle))
            .toList();
        int caret = Math.min(editor.getCaretPosition(), text.length());
        this.updatingGroupFilter = true;
        try {
            this.groupField.setModel(new DefaultComboBoxModel<>(matches.toArray(String[]::new)));
            this.groupField.setSelectedItem(null);
            editor.setText(text);
            editor.setCaretPosition(Math.min(caret, text.length()));
            if (showPopup && !matches.isEmpty() && editor.isFocusOwner()) {
                this.groupField.showPopup();
            } else {
                this.groupField.hidePopup();
            }
        } finally {
            this.updatingGroupFilter = false;
        }
    }

    private Name selectedBuildingName()
    {
        Building building = (Building) this.buildingSelect.getSelectedItem();

        return building == null ? null : building.getName();
    }

    private void restoreSelectedPressurizer()
    {
        if (this.selectedPressurizerId == null) {
            this.pressurizerSelect.setSelectedIndex(-1);
            return;
        }

        for (int index = 0; index < this.pressurizerSelect.getItemCount(); index++) {
            Production production = this.pressurizerSelect.getItemAt(index);
            if (production != null && Objects.equals(production.getId(), this.selectedPressurizerId)) {
                this.pressurizerSelect.setSelectedIndex(index);
                return;
            }
        }

        this.pressurizerSelect.setSelectedIndex(-1);
    }

    private void syncClockSpeedFromSelectedPressurizer()
    {
        if (this.pressurizerSelect.getItemCount() == 0) {
            this.clockSpeed = 100;
            this.clockSpeedSlider.setValue(100);
            this.syncClockSpeedFieldFromSlider();
            return;
        }

        Production selectedPressurizer = (Production) this.pressurizerSelect.getSelectedItem();
        if (selectedPressurizer == null || selectedPressurizer.getClockSpeed() == null) {
            this.clockSpeed = 100;
            this.clockSpeedSlider.setValue(100);
            this.syncClockSpeedFieldFromSlider();
            return;
        }

        this.clockSpeed = selectedPressurizer.getClockSpeed();
        this.syncingClockSpeedFromSlider = true;
        this.clockSpeedSlider.setValue((int) Math.round(this.clockSpeed));
        this.syncingClockSpeedFromSlider = false;
        this.syncingClockSpeedFromText = true;
        this.clockSpeedValueField.setText(this.clockSpeedFormat.format(this.clockSpeed));
        this.syncingClockSpeedFromText = false;
        this.updateSummaries();
    }

    private void updateSummaryVisibility(
        boolean hasRecipes,
        boolean generatesPower,
        boolean consumesPower,
        boolean hasIngredients
    )
    {
        boolean showProduction = hasRecipes || generatesPower;
        boolean visibilityChanged = this.ingredientsPanel.isVisible() != hasIngredients
                || this.productionPanel.isVisible() != showProduction
                || this.consumptionLabel.isVisible() != consumesPower
                || this.consumptionInfoLabel.isVisible() != consumesPower;
        this.ingredientsPanel.setVisible(hasIngredients);
        this.productionPanel.setVisible(showProduction);
        this.consumptionLabel.setVisible(consumesPower);
        this.consumptionInfoLabel.setVisible(consumesPower);
        if (visibilityChanged) {
            this.resizeDialogToContent();
        }
    }

    private void resizeDialogToContent()
    {
        JDialog dialog = this.getParentDialog();
        if (!dialog.isDisplayable()) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (!dialog.isDisplayable()) {
                return;
            }
            int width = dialog.getWidth();
            dialog.pack();
            dialog.setSize(width, dialog.getHeight());
        });
    }

    private void addSummaryListener(JTextField field)
    {
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event)
            {
                NewProductionPanel.this.updateSummaries();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event)
            {
                NewProductionPanel.this.updateSummaries();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event)
            {
                NewProductionPanel.this.updateSummaries();
            }
        });
    }

    private void updateClockSpeedFromText()
    {
        if (this.syncingClockSpeedFromSlider) {
            return;
        }

        Double clockSpeed = this.parseNonNegativeNumber(this.clockSpeedValueField.getText(), this.clockSpeedFormat);
        if (clockSpeed != null && clockSpeed <= 250) {
            this.clockSpeed = clockSpeed;
            this.syncingClockSpeedFromText = true;
            this.clockSpeedSlider.setValue((int) Math.round(clockSpeed));
            this.syncingClockSpeedFromText = false;
        }
        this.updateSummaries();
    }

    private void syncClockSpeedFieldFromSlider()
    {
        this.clockSpeed = this.clockSpeedSlider.getValue();
        this.syncingClockSpeedFromSlider = true;
        this.clockSpeedValueField.setText(this.clockSpeedFormat.format(this.clockSpeed));
        this.syncingClockSpeedFromSlider = false;
        this.updateSummaries();
    }

    private void commitClockSpeedText()
    {
        Double clockSpeed = this.parseNonNegativeNumber(this.clockSpeedValueField.getText(), this.clockSpeedFormat);
        if (clockSpeed == null) {
            this.syncClockSpeedFieldFromSlider();
            return;
        }

        this.clockSpeed = Math.min(clockSpeed, 250);
        this.syncingClockSpeedFromText = true;
        this.clockSpeedSlider.setValue((int) Math.round(this.clockSpeed));
        this.syncingClockSpeedFromText = false;
        this.syncingClockSpeedFromSlider = true;
        this.clockSpeedValueField.setText(this.clockSpeedFormat.format(this.clockSpeed));
        this.syncingClockSpeedFromSlider = false;
        this.updateSummaries();
    }

    private Double parseNonNegativeNumber(String value)
    {
        return this.parseNonNegativeNumber(value, this.numberFormat);
    }

    private Double parseNonNegativeNumber(String value, NumberFormat formatter)
    {
        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }

        ParsePosition position = new ParsePosition(0);
        Number number = formatter.parse(text, position);
        if (number == null || position.getIndex() != text.length()) {
            return null;
        }

        double parsedValue = number.doubleValue();
        return Double.isFinite(parsedValue) && parsedValue >= 0 ? parsedValue : null;
    }

    private Integer parseNonNegativeInteger(String value)
    {
        try {
            int parsedValue = Integer.parseInt(value.trim());
            return parsedValue >= 0 ? parsedValue : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer parsePositiveInteger(String value)
    {
        Integer parsedValue = this.parseNonNegativeInteger(value);
        return parsedValue != null && parsedValue > 0 ? parsedValue : null;
    }

    private void updateItemSummary(
        JPanel panel,
        JLabel heading,
        List<Item> items,
        double amount,
        double clockSpeed,
        int usedSomersloops,
        int somersloopSlots
    )
    {
        panel.removeAll();
        panel.add(heading);
        for (Item item : items) {
            String name = item.getResource().getName().getLocalizedName(this.selectedLanguage);
            double itemRate = ProductionCalculator.calculateProduction(
                item.getAmount(),
                clockSpeed,
                usedSomersloops,
                somersloopSlots
            ) * amount;
            JLabel itemLabel = new JLabel(name + ": " + this.numberFormat.format(itemRate) + " / min");
            itemLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(itemLabel);
        }
        panel.revalidate();
        panel.repaint();
    }

    private void updateSummaries()
    {
        int ingredientCount = this.ingredientsPanel.getComponentCount();
        int productionCount = this.productionPanel.getComponentCount();
        Recipe selectedRecipe = (Recipe) this.recipeSelect.getSelectedItem();
        antafes.sf.manager.entity.Item selectedItem =
            (antafes.sf.manager.entity.Item) this.itemSelect.getSelectedItem();
        Building selectedBuilding = (Building) this.buildingSelect.getSelectedItem();
        boolean hasIngredients = selectedRecipe != null
            && selectedRecipe.getIngredients() != null
            && !selectedRecipe.getIngredients().isEmpty();
        this.updateSummaryVisibility(
            this.recipeSelect.getItemCount() > 0 || this.itemSelect.getItemCount() > 0,
            selectedBuilding != null && selectedBuilding.getCategory() == Category.POWER,
            selectedBuilding != null
                && selectedBuilding.getCategory() != Category.POWER
                && (this.recipeSelect.getItemCount() > 0 || this.itemSelect.getItemCount() > 0
                    || selectedBuilding.getPower() != 0),
            hasIngredients
        );
        Integer amount = this.parsePositiveInteger(this.amountField.getText());
        double clockSpeed = this.clockSpeed;
        Integer usedSomersloops = this.parseNonNegativeInteger(this.usedSomersloopsField.getText());

        if (selectedBuilding == null || amount == null) {
            this.ingredientsPanel.removeAll();
            this.ingredientsPanel.add(this.ingredientsLabel);
            this.ingredientsPanel.revalidate();
            this.ingredientsPanel.repaint();
            this.productionPanel.removeAll();
            this.productionPanel.add(this.productionLabel);
            this.productionPanel.revalidate();
            this.productionPanel.repaint();
            this.updateConsumptionInfo(selectedBuilding, amount, clockSpeed, usedSomersloops);
            if (ingredientCount != this.ingredientsPanel.getComponentCount()
                || productionCount != this.productionPanel.getComponentCount()) {
                this.resizeDialogToContent();
            }
            return;
        }

        if (selectedBuilding.getCategory() == Category.POWER) {
            if (selectedRecipe != null && usedSomersloops != null
                && usedSomersloops <= selectedBuilding.getSomersloopSlots()) {
                this.updateItemSummary(
                    this.ingredientsPanel,
                    this.ingredientsLabel,
                    selectedRecipe.getIngredients(),
                    amount,
                    clockSpeed,
                    usedSomersloops,
                    selectedBuilding.getSomersloopSlots()
                );
            } else {
                this.ingredientsPanel.removeAll();
                this.ingredientsPanel.add(this.ingredientsLabel);
                this.ingredientsPanel.revalidate();
                this.ingredientsPanel.repaint();
            }
            this.productionPanel.removeAll();
            this.productionPanel.add(this.productionLabel);
            double generatedPower = selectedBuilding.getPower() * (clockSpeed / 100) * amount;
            JLabel generatedPowerLabel = new JLabel(
                this.getLanguage().translate("generatedPower") + ": "
                    + this.numberFormat.format(generatedPower) + " MW"
            );
            generatedPowerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            this.productionPanel.add(generatedPowerLabel);
            this.productionPanel.revalidate();
            this.productionPanel.repaint();
            this.updateConsumptionInfo(selectedBuilding, amount, clockSpeed, usedSomersloops);
            if (ingredientCount != this.ingredientsPanel.getComponentCount()
                || productionCount != this.productionPanel.getComponentCount()) {
                this.resizeDialogToContent();
            }
            return;
        }

        if (this.itemSelect.getItemCount() > 0) {
            this.updateResourceSummary(selectedBuilding, selectedItem, amount, clockSpeed, usedSomersloops);
            this.updateConsumptionInfo(selectedBuilding, amount, clockSpeed, usedSomersloops);
            if (ingredientCount != this.ingredientsPanel.getComponentCount()
                || productionCount != this.productionPanel.getComponentCount()) {
                this.resizeDialogToContent();
            }
            return;
        }

        if (selectedRecipe == null || usedSomersloops == null
            || usedSomersloops > selectedBuilding.getSomersloopSlots()) {
            this.ingredientsPanel.removeAll();
            this.ingredientsPanel.add(this.ingredientsLabel);
            this.ingredientsPanel.revalidate();
            this.ingredientsPanel.repaint();
            this.productionPanel.removeAll();
            this.productionPanel.add(this.productionLabel);
            this.productionPanel.revalidate();
            this.productionPanel.repaint();
            this.updateConsumptionInfo(selectedBuilding, amount, clockSpeed, usedSomersloops);
            if (ingredientCount != this.ingredientsPanel.getComponentCount()
                || productionCount != this.productionPanel.getComponentCount()) {
                this.resizeDialogToContent();
            }
            return;
        }

        int somersloopSlots = selectedBuilding.getSomersloopSlots();
        this.updateItemSummary(
            this.ingredientsPanel,
            this.ingredientsLabel,
            selectedRecipe.getIngredients(),
            amount,
            clockSpeed,
            usedSomersloops,
            somersloopSlots
        );
        this.updateItemSummary(
            this.productionPanel,
            this.productionLabel,
            selectedRecipe.getProducts(),
            amount,
            clockSpeed,
            usedSomersloops,
            somersloopSlots
        );
        this.updateConsumptionInfo(selectedBuilding, amount, clockSpeed, usedSomersloops);
        if (ingredientCount != this.ingredientsPanel.getComponentCount()
            || productionCount != this.productionPanel.getComponentCount()) {
            this.resizeDialogToContent();
        }
    }

    private void updateResourceSummary(
        Building building,
        antafes.sf.manager.entity.Item selectedItem,
        double amount,
        double clockSpeed,
        Integer usedSomersloops
    )
    {
        this.ingredientsPanel.removeAll();
        this.ingredientsPanel.add(this.ingredientsLabel);
        this.ingredientsPanel.revalidate();
        this.ingredientsPanel.repaint();

        this.productionPanel.removeAll();
        this.productionPanel.add(this.productionLabel);
        if (selectedItem != null && selectedItem.getProductions() != null
            && usedSomersloops != null && usedSomersloops <= building.getSomersloopSlots()
            && this.puritySelect.getSelectedItem() instanceof Purity purity) {
            selectedItem.getProductions().stream()
                .filter(production -> production.getBuilding() != null
                    && building.getKey().equals(production.getBuilding().getKey()))
                .findFirst()
                .ifPresent(production -> {
                    double itemRate = ProductionCalculator.calculateResourceProduction(
                        (double) production.getAmount(),
                        purity,
                        clockSpeed,
                        usedSomersloops,
                        building.getSomersloopSlots()
                    ) * amount;
                    String itemName = selectedItem.getName().getLocalizedName(this.selectedLanguage);
                    JLabel itemLabel = new JLabel(
                        itemName + ": " + this.numberFormat.format(itemRate) + " / min"
                    );
                    itemLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                    this.productionPanel.add(itemLabel);
                });
        }
        this.productionPanel.revalidate();
        this.productionPanel.repaint();
    }

    private void updateConsumptionInfo(
        Building building,
        Integer amount,
        double clockSpeed,
        Integer usedSomersloops
    )
    {
        if (building == null || amount == null || building.getCategory() == Category.POWER
            || usedSomersloops == null || usedSomersloops > building.getSomersloopSlots()) {
            this.consumptionInfoLabel.setText("");
            return;
        }

        double consumption = PowerCalculator.calculateConsumption(
            (double) Math.abs(building.getPower()),
            clockSpeed,
            usedSomersloops,
            building.getSomersloopSlots()
        ) * amount;
        this.consumptionInfoLabel.setText(this.numberFormat.format(consumption) + " MW");
    }

    private void initComponents()
    {
        this.buildingSelectLabel = new JLabel();
        LocalizedNameRenderer<Building> buildingRenderer = new LocalizedNameRenderer<>(
            Building::getName,
            this.selectedLanguage
        );
        this.buildingSelect = new GroupedBuildingSelect(
            this.productionService.getBuildings(),
            buildingRenderer::getDisplayName,
            this.getLanguage().translate("productionBuildings"),
            this.getLanguage().translate("consumers")
        );
        this.pressurizerSelectLabel = new JLabel();
        this.pressurizerSelect = new JComboBox<>();
        this.pressurizerSelect.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            ) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Production production) {
                    String group = production.getGroup();
                    String id = Objects.toString(production.getId(), "");
                    String shortId = id.substring(0, Math.min(8, id.length()));
                    this.setText(group == null || group.isBlank()
                        ? NewProductionPanel.this.getLanguage().translate("pressurizer") + " " + shortId
                        : group + " (" + shortId + ")");
                } else {
                    this.setText("");
                }
                return this;
            }
        });
        this.pressurizerSelectLabel.setVisible(false);
        this.pressurizerSelect.setVisible(false);
        this.recipeSelectLabel = new JLabel();
        this.recipeSelect = new JComboBox<>();
        this.recipeRenderer = new LocalizedNameRenderer<>(Recipe::getName, this.selectedLanguage);
        this.recipeSelect.setRenderer(this.recipeRenderer);
        this.itemSelect = new JComboBox<>();
        this.itemRenderer = new LocalizedNameRenderer<>(
            antafes.sf.manager.entity.Item::getName,
            this.selectedLanguage
        );
        this.itemSelect.setRenderer(this.itemRenderer);
        this.productSelectPanel = new JPanel(new CardLayout());
        this.productSelectPanel.add(this.recipeSelect, "recipe");
        this.productSelectPanel.add(this.itemSelect, "item");
        this.amountLabel = new JLabel();
        this.amountField = new JTextField();
        this.amountField.setText("1");
        this.groupLabel = new JLabel();
        this.groupItems = List.copyOf(this.productionService.getProductionGroups());
        this.groupField = new JComboBox<>(this.groupItems.toArray(String[]::new));
        this.groupField.setEditable(true);
        this.groupField.setSelectedIndex(-1);
        this.installGroupFilter();
        this.materialSourcesLabel = new JLabel();
        this.materialSourcesPanel = new JPanel(new GridBagLayout());
        this.materialSourcesLabel.setVisible(false);
        this.materialSourcesPanel.setVisible(false);
        this.positionLabel = new JLabel();
        this.positionField = new JTextField();
        this.puritySelectLabel = new JLabel();
        this.puritySelect = new JComboBox<>();
        for (Purity purity : Purity.values()) {
            this.puritySelect.addItem(purity);
        }
        this.puritySelect.setSelectedItem(Purity.NORMAL);
        this.puritySelect.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            ) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                this.setText(value instanceof Purity purity ? NewProductionPanel.this.getLanguage()
                    .translate(purity.getKey()) : "");
                return this;
            }
        });
        this.clockSpeedLabel = new JLabel();
        this.clockSpeedSlider = new JSlider(0, 250, 100);
        this.clockSpeedSlider.setMajorTickSpacing(50);
        this.clockSpeedSlider.setMinorTickSpacing(10);
        this.clockSpeedSlider.setPaintTicks(true);
        this.clockSpeedSlider.setPaintLabels(true);
        Hashtable<Integer, JLabel> clockSpeedLabels = new Hashtable<>();
        for (int value = 0; value <= 250; value += 50) {
            clockSpeedLabels.put(value, new JLabel(Integer.toString(value)));
        }
        this.clockSpeedSlider.setLabelTable(clockSpeedLabels);
        this.clockSpeedValueField = new JTextField(8);
        this.clockSpeedValueField.setText(this.clockSpeedFormat.format(100));
        char decimalSeparator = DecimalFormatSymbols.getInstance(
            this.selectedLanguage.equals("German") ? Locale.GERMAN : Locale.ENGLISH
        ).getDecimalSeparator();
        ((javax.swing.text.AbstractDocument) this.clockSpeedValueField.getDocument())
            .setDocumentFilter(new DecimalPrecisionFilter(decimalSeparator, 10));
        JPanel clockSpeedInput = new JPanel(new BorderLayout());
        clockSpeedInput.add(this.clockSpeedSlider, BorderLayout.CENTER);
        JPanel clockSpeedValuePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        clockSpeedValuePanel.add(this.clockSpeedValueField);
        clockSpeedInput.add(clockSpeedValuePanel, BorderLayout.SOUTH);
        this.usedSomersloopsLabel = new JLabel();
        this.usedSomersloopsField = new JTextField();
        this.usedSomersloopsField.setText("0");
        this.ingredientsLabel = new JLabel();
        this.ingredientsPanel = new JPanel();
        this.ingredientsPanel.setLayout(new BoxLayout(this.ingredientsPanel, BoxLayout.Y_AXIS));
        this.productionLabel = new JLabel();
        this.productionPanel = new JPanel();
        this.productionPanel.setLayout(new BoxLayout(this.productionPanel, BoxLayout.Y_AXIS));
        this.consumptionLabel = new JLabel();
        this.consumptionInfoLabel = new JLabel();
        this.requiredFieldsHintLabel = new JLabel();
        this.okButton = new JButton();
        this.cancelButton = new JButton();
        this.okButton.addActionListener(event -> this.saveProduction());
        this.cancelButton.addActionListener(event -> this.getParentDialog().dispose());
        this.getParentDialog().getRootPane().setDefaultButton(this.okButton);
        this.pressurizerSelect.addActionListener(event -> {
            Production selectedPressurizer = (Production) this.pressurizerSelect.getSelectedItem();
            this.selectedPressurizerId = selectedPressurizer == null ? null : selectedPressurizer.getId();
            if (selectedPressurizer != null && selectedPressurizer.getClockSpeed() != null) {
                this.syncClockSpeedFromSelectedPressurizer();
            }
        });

        this.setLayout(new GridBagLayout());
        this.addFieldRow(0, this.buildingSelectLabel, this.buildingSelect);
        this.addFieldRow(1, this.pressurizerSelectLabel, this.pressurizerSelect);
        this.addFieldRow(2, this.recipeSelectLabel, this.productSelectPanel);
        this.addFieldRow(3, this.amountLabel, this.amountField);
        this.addFieldRow(4, this.groupLabel, this.groupField);
        this.addFullWidthRow(5, this.materialSourcesLabel);
        this.addFullWidthRow(6, this.materialSourcesPanel);
        this.addFieldRow(7, this.positionLabel, this.positionField);
        this.addFieldRow(8, this.puritySelectLabel, this.puritySelect);
        this.addFieldRow(9, this.clockSpeedLabel, clockSpeedInput);
        this.addFieldRow(10, this.usedSomersloopsLabel, this.usedSomersloopsField);

        int row = 11;
        this.ingredientsPanel.add(this.ingredientsLabel);
        this.productionPanel.add(this.productionLabel);
        this.addFullWidthRow(row++, this.ingredientsPanel);
        this.addFullWidthRow(row++, this.productionPanel);
        GridBagConstraints consumptionConstraints = new GridBagConstraints();
        consumptionConstraints.gridx = 0;
        consumptionConstraints.gridy = row++;
        consumptionConstraints.insets = new Insets(4, 4, 3, 8);
        consumptionConstraints.anchor = GridBagConstraints.LINE_START;
        this.add(this.consumptionLabel, consumptionConstraints);
        consumptionConstraints.gridx = 1;
        consumptionConstraints.gridwidth = 3;
        consumptionConstraints.weightx = 1;
        consumptionConstraints.fill = GridBagConstraints.HORIZONTAL;
        consumptionConstraints.insets = new Insets(4, 4, 3, 4);
        this.add(this.consumptionInfoLabel, consumptionConstraints);

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridy = row;
        buttonConstraints.gridx = 2;
        buttonConstraints.insets = new Insets(8, 4, 4, 4);
        buttonConstraints.anchor = GridBagConstraints.LINE_END;
        this.add(this.okButton, buttonConstraints);
        buttonConstraints.gridx = 3;
        this.add(this.cancelButton, buttonConstraints);
        this.addFullWidthRow(++row, this.requiredFieldsHintLabel);

        this.buildingSelect.addActionListener(event -> {
            if (this.buildingSelect.getSelectedItem() != this.updatedBuilding) {
                this.updateRecipes();
            }
        });
        this.recipeSelect.addActionListener(event -> {
            this.prefillGroup();
            this.updateMaterialSources();
            this.updateSummaries();
            this.createFocusTraversalPolicy();
        });
        this.itemSelect.addActionListener(event -> this.updateSummaries());
        this.puritySelect.addActionListener(event -> this.updateSummaries());
        this.addSummaryListener(this.amountField);
        this.clockSpeedSlider.addChangeListener(event -> {
            if (!this.syncingClockSpeedFromText) {
                this.syncClockSpeedFieldFromSlider();
            } else {
                this.updateSummaries();
            }
        });
        this.clockSpeedValueField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event)
            {
                NewProductionPanel.this.updateClockSpeedFromText();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event)
            {
                NewProductionPanel.this.updateClockSpeedFromText();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event)
            {
                NewProductionPanel.this.updateClockSpeedFromText();
            }
        });
        this.clockSpeedValueField.addActionListener(event -> this.commitClockSpeedText());
        this.clockSpeedValueField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent event)
            {
                NewProductionPanel.this.commitClockSpeedText();
            }
        });
        this.addSummaryListener(this.usedSomersloopsField);
        this.updateRecipes();
    }

    private static class DecimalPrecisionFilter extends javax.swing.text.DocumentFilter
    {
        private final char decimalSeparator;
        private final int maximumFractionDigits;

        private DecimalPrecisionFilter(char decimalSeparator, int maximumFractionDigits)
        {
            this.decimalSeparator = decimalSeparator;
            this.maximumFractionDigits = maximumFractionDigits;
        }

        @Override
        public void insertString(
            javax.swing.text.DocumentFilter.FilterBypass bypass,
            int offset,
            String text,
            javax.swing.text.AttributeSet attributes
        ) throws javax.swing.text.BadLocationException
        {
            this.replace(bypass, offset, 0, text, attributes);
        }

        @Override
        public void replace(
            javax.swing.text.DocumentFilter.FilterBypass bypass,
            int offset,
            int length,
            String text,
            javax.swing.text.AttributeSet attributes
        ) throws javax.swing.text.BadLocationException
        {
            String currentText = bypass.getDocument().getText(0, bypass.getDocument().getLength());
            String candidate = currentText.substring(0, offset) + text + currentText.substring(offset + length);
            int separatorIndex = candidate.indexOf(this.decimalSeparator);
            boolean valid = true;
            for (int index = 0; index < candidate.length(); index++) {
                char character = candidate.charAt(index);
                if (Character.isDigit(character)) {
                    continue;
                }
                if (character == this.decimalSeparator && index == separatorIndex) {
                    continue;
                }
                valid = false;
                break;
            }

            int fractionDigits = separatorIndex < 0 ? 0 : candidate.length() - separatorIndex - 1;
            if (valid && fractionDigits <= this.maximumFractionDigits) {
                bypass.replace(offset, length, text, attributes);
            }
        }
    }

}
