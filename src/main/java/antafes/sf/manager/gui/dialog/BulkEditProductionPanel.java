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
import antafes.sf.manager.entity.Production;
import antafes.sf.manager.entity.ProductionMaterialSource;
import antafes.sf.manager.gui.BasePanel;
import antafes.sf.manager.gui.focus.DynamicFocusTraversalPolicy;
import antafes.sf.manager.gui.util.BulkOperationRunner;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.service.dto.BulkProductionEdit;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.List;
import java.util.function.Function;

@org.springframework.stereotype.Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class BulkEditProductionPanel extends BasePanel
{
    @Autowired
    private ProductionService productionService;

    private final List<UUID> productionIds;
    private final NumberFormat clockSpeedFormat;
    private List<Production> productions;
    private final JTextField groupField = new JTextField();
    private final JTextField positionField = new JTextField();
    private final JTextField clockSpeedField = new JTextField();
    private final JTextField usedSomersloopsField = new JTextField();
    private final JCheckBox groupKeep = new JCheckBox();
    private final JCheckBox positionKeep = new JCheckBox();
    private final JCheckBox clockSpeedKeep = new JCheckBox();
    private final JCheckBox usedSomersloopsKeep = new JCheckBox();
    private final JCheckBox materialSourcesKeep = new JCheckBox();
    private final JPanel materialSourcesPanel = new JPanel(new GridBagLayout());
    private MaterialSourceSelector materialSourceSelector;
    private final String selectedLanguage;
    private final JButton saveButton = new JButton();
    private final JButton cancelButton = new JButton();

    public BulkEditProductionPanel(JDialog parent, Configuration configuration, List<UUID> productionIds)
    {
        super(parent, configuration);
        this.productionIds = List.copyOf(productionIds);
        this.selectedLanguage = configuration.getLanguage().getName();
        Locale locale = "German".equals(configuration.getLanguage().getName()) ? Locale.GERMAN : Locale.ENGLISH;
        this.clockSpeedFormat = NumberFormat.getNumberInstance(locale);
        this.clockSpeedFormat.setGroupingUsed(false);
        this.clockSpeedFormat.setMaximumFractionDigits(10);
    }

    public static boolean isClockSpeedChangeAllowed(List<Production> productions)
    {
        return productions.stream().allMatch(production -> production.getSelectedBuilding().isOverclockable());
    }

    /**
     * Used Somersloops can only be changed if all productions use the same building and it has Somersloop slots.
     */
    public static boolean isSomersloopChangeAllowed(List<Production> productions)
    {
        return !hasDifferentBuildings(productions)
            && productions.stream().allMatch(production -> production.getSelectedBuilding().getSomersloopSlots() > 0);
    }

    public static boolean hasDifferentBuildings(List<Production> productions)
    {
        return productions.stream().map(production -> production.getSelectedBuilding().getKey()).distinct().count() > 1;
    }

    /**
     * Material sources can only be changed if all productions use the same recipe.
     */
    public static boolean isMaterialSourceChangeAllowed(List<Production> productions)
    {
        return productions.stream().allMatch(production -> production.getSelectedRecipe() != null)
            && productions.stream().map(production -> production.getSelectedRecipe().getKey()).distinct().count() == 1;
    }

    @PostConstruct
    private void initializePanel()
    {
        this.productions = this.productionService.getProductionsForEditing(this.productionIds);
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
        this.groupKeep.setText(this.getLanguage().translate("keepUnchanged"));
        this.positionKeep.setText(this.getLanguage().translate("keepUnchanged"));
        this.clockSpeedKeep.setText(this.getLanguage().translate("keepUnchanged"));
        this.usedSomersloopsKeep.setText(this.getLanguage().translate("keepUnchanged"));
        this.materialSourcesKeep.setText(this.getLanguage().translate("keepUnchanged"));
        this.saveButton.setText(this.getLanguage().translate("save"));
        this.cancelButton.setText(this.getLanguage().translate("cancel"));
    }

    @Override
    protected void createFocusTraversalPolicy()
    {
        List<Component> order = new ArrayList<>();
        for (JCheckBox keep : List.of(this.groupKeep, this.positionKeep, this.clockSpeedKeep,
            this.usedSomersloopsKeep, this.materialSourcesKeep)) {
            if (keep.getParent() != null && keep.isEnabled()) {
                order.add(keep);
            }
        }
        if (this.materialSourceSelector != null) {
            for (Component component : this.materialSourceSelector.getFocusableComponents()) {
                if (component.isEnabled()) {
                    order.add(component);
                }
            }
        }
        order.add(this.saveButton);
        order.add(this.cancelButton);
        this.setFocusTraversalPolicy(new DynamicFocusTraversalPolicy(order));
        this.setFocusTraversalPolicyProvider(true);
    }

    private void initComponents()
    {
        this.setLayout(new GridBagLayout());
        JLabel hint = new JLabel(String.format(
            this.getLanguage().translate("editProductionsHint"),
            this.productions.size()
        ));
        GridBagConstraints hintConstraints = new GridBagConstraints();
        hintConstraints.gridx = 0;
        hintConstraints.gridy = 0;
        hintConstraints.gridwidth = 3;
        hintConstraints.anchor = GridBagConstraints.LINE_START;
        hintConstraints.insets = new Insets(4, 4, 8, 4);
        this.add(hint, hintConstraints);

        this.groupField.setText(this.commonText(Production::getGroup));
        this.positionField.setText(this.commonText(Production::getPosition));
        this.clockSpeedField.setText(this.commonClockSpeed());
        this.usedSomersloopsField.setText(this.commonValue(production -> production.getUsedSomersloops() + ""));

        this.addRow(1, "group", this.groupField, this.groupKeep, null);
        this.addRow(2, "position", this.positionField, this.positionKeep, null);
        this.addRow(
            3,
            "clockSpeed",
            this.clockSpeedField,
            this.clockSpeedKeep,
            isClockSpeedChangeAllowed(this.productions) ? null : "notOverclockableHint"
        );
        String somersloopHint = null;
        if (hasDifferentBuildings(this.productions)) {
            somersloopHint = "differentBuildingsSomersloopsHint";
        } else if (!isSomersloopChangeAllowed(this.productions)) {
            somersloopHint = "noSomersloopSlotsHint";
        }
        this.addRow(4, "usedSomersloops", this.usedSomersloopsField, this.usedSomersloopsKeep, somersloopHint);
        this.addMaterialSourcesRow(5);

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridy = 6;
        buttonConstraints.gridx = 1;
        buttonConstraints.insets = new Insets(8, 4, 4, 4);
        buttonConstraints.anchor = GridBagConstraints.LINE_END;
        this.add(this.saveButton, buttonConstraints);
        buttonConstraints.gridx = 2;
        this.add(this.cancelButton, buttonConstraints);

        if (this.getParentDialog() != null) {
            this.getParentDialog().getRootPane().setDefaultButton(this.saveButton);
        }
        this.saveButton.addActionListener(event -> this.saveProductions());
        this.cancelButton.addActionListener(event -> this.getParentDialog().dispose());
        this.updateSaveButton();
    }

    private void addRow(int row, String labelKey, JTextField field, JCheckBox keep, String blockedHintKey)
    {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.insets = new Insets(3, 4, 3, 4);

        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.LINE_START;
        this.add(new JLabel(this.getLanguage().translate(labelKey)), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        field.setColumns(16);
        this.add(field, constraints);

        constraints.gridx = 2;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        this.add(keep, constraints);

        keep.setSelected(true);
        field.setEnabled(false);
        keep.addActionListener(event -> {
            field.setEnabled(!keep.isSelected());
            this.updateSaveButton();
        });
        if (blockedHintKey != null) {
            String hint = this.getLanguage().translate(blockedHintKey);
            keep.setEnabled(false);
            keep.setToolTipText(hint);
            field.setToolTipText(hint);
        }
    }

    private void addMaterialSourcesRow(int row)
    {
        if (this.productions.stream().allMatch(production -> production.getSelectedRecipe() == null)) {
            return;
        }
        boolean allowed = isMaterialSourceChangeAllowed(this.productions);
        if (allowed) {
            this.fillMaterialSourceFields();
            if (this.materialSourceSelector.isEmpty()) {
                return;
            }
        }

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.insets = new Insets(3, 4, 3, 4);

        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        this.add(new JLabel(this.getLanguage().translate("materialSources")), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        this.add(this.materialSourcesPanel, constraints);

        constraints.gridx = 2;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        this.add(this.materialSourcesKeep, constraints);

        this.materialSourcesKeep.setSelected(true);
        if (this.materialSourceSelector != null) {
            this.materialSourceSelector.setEnabled(false);
        }
        this.materialSourcesKeep.addActionListener(event -> {
            if (this.materialSourceSelector != null) {
                this.materialSourceSelector.setEnabled(!this.materialSourcesKeep.isSelected());
            }
            this.createFocusTraversalPolicy();
            this.updateSaveButton();
        });
        if (!allowed) {
            String hint = this.getLanguage().translate("differentRecipesMaterialSourcesHint");
            this.materialSourcesKeep.setEnabled(false);
            this.materialSourcesKeep.setToolTipText(hint);
            this.materialSourcesPanel.add(new JLabel(hint));
        }
    }

    private void fillMaterialSourceFields()
    {
        this.materialSourceSelector = new MaterialSourceSelector(
            this.productionService,
            this.getLanguage(),
            this.clockSpeedFormat,
            this.selectedLanguage,
            () -> {
                this.getParentDialog().pack();
                this.createFocusTraversalPolicy();
            }
        );
        Production first = this.productions.get(0);
        for (antafes.sf.manager.entity.recipe.Item ingredient : first.getSelectedRecipe().getIngredients()) {
            antafes.sf.manager.entity.Item item = ingredient.getResource();
            if (item == null || item.getKey() == null) {
                throw new IllegalStateException("A recipe ingredient has no material.");
            }
            if (item.isHandCollected()) {
                continue;
            }
            this.materialSourceSelector.addMaterial(
                item,
                false,
                this.commonMaterialSourceGroups(item.getKey()),
                this.productionIds
            );
        }
        if (!this.materialSourceSelector.isEmpty()) {
            GridBagConstraints selectorConstraints = new GridBagConstraints();
            selectorConstraints.weightx = 1;
            selectorConstraints.fill = GridBagConstraints.HORIZONTAL;
            this.materialSourcesPanel.add(this.materialSourceSelector, selectorConstraints);
        }
    }

    private List<String> commonMaterialSourceGroups(String itemKey)
    {
        List<String> first = this.materialSourceGroups(this.productions.get(0), itemKey);
        boolean same = this.productions.stream()
            .allMatch(production -> Objects.equals(first, this.materialSourceGroups(production, itemKey)));

        return same ? first : List.of();
    }

    private List<String> materialSourceGroups(Production production, String itemKey)
    {
        if (production.getMaterialSources() == null) {
            return List.of();
        }

        return production.getMaterialSources().stream()
            .filter(source -> itemKey.equals(source.getItemKey()))
            .map(ProductionMaterialSource::getGroup)
            .toList();
    }


    private void updateSaveButton()
    {
        this.saveButton.setEnabled(
            !this.groupKeep.isSelected() || !this.positionKeep.isSelected()
                || !this.clockSpeedKeep.isSelected() || !this.usedSomersloopsKeep.isSelected()
                || !this.materialSourcesKeep.isSelected()
        );
    }

    private String commonText(Function<Production, String> getter)
    {
        return this.commonValue(production -> Objects.toString(getter.apply(production), ""));
    }

    private String commonValue(Function<Production, String> getter)
    {
        String first = getter.apply(this.productions.get(0));
        return this.productions.stream().allMatch(production -> first.equals(getter.apply(production))) ? first : "";
    }

    private String commonClockSpeed()
    {
        return this.commonValue(production -> this.clockSpeedFormat.format(production.getClockSpeed()));
    }

    private void saveProductions()
    {
        try {
            BulkProductionEdit edit = this.createEdit();
            BulkOperationRunner.run(
                this.getParentDialog(),
                this.getLanguage().translate("editingProductions"),
                () -> this.productionService.editProductions(this.productionIds, edit),
                saved -> {
                    ((BulkEditProductionDialog) this.getParentDialog()).setSaved();
                    this.getParentDialog().dispose();
                },
                this::showError
            );
        } catch (RuntimeException exception) {
            this.showError(exception);
        }
    }

    private BulkProductionEdit createEdit()
    {
        String group = null;
        if (!this.groupKeep.isSelected()) {
            group = this.groupField.getText();
            if (group.isBlank()) {
                throw new IllegalArgumentException("A production group is required.");
            }
        }
        String position = this.positionKeep.isSelected() ? null : this.positionField.getText();
        Double clockSpeed = null;
        if (!this.clockSpeedKeep.isSelected()) {
            clockSpeed = this.parseClockSpeed();
            if (clockSpeed == null || clockSpeed > 250) {
                throw new IllegalArgumentException("Clock speed must be between 0 and 250.");
            }
        }
        Integer usedSomersloops = null;
        if (!this.usedSomersloopsKeep.isSelected()) {
            usedSomersloops = Integer.parseInt(this.usedSomersloopsField.getText().trim());
            if (usedSomersloops < 0) {
                throw new IllegalArgumentException("Somersloop count must be a non-negative integer.");
            }
        }

        List<ProductionMaterialSource> materialSources = null;
        if (!this.materialSourcesKeep.isSelected() && this.materialSourceSelector != null) {
            if (!this.materialSourceSelector.isComplete()) {
                throw new IllegalArgumentException("Every recipe ingredient requires a material source group.");
            }
            materialSources = this.materialSourceSelector.getMaterialSources();
        }

        return new BulkProductionEdit(group, position, clockSpeed, usedSomersloops, materialSources);
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

    private void showError(Exception exception)
    {
        JOptionPane.showMessageDialog(
            this.getParentDialog(),
            Objects.toString(exception.getMessage(), this.getLanguage().translate("productionEditingFailed")),
            this.getLanguage().translate("editProductions"),
            JOptionPane.ERROR_MESSAGE
        );
    }
}
