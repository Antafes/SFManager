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

import antafes.sf.manager.Configuration;
import antafes.sf.manager.entity.Item;
import antafes.sf.manager.entity.Name;
import antafes.sf.manager.entity.Plan;
import antafes.sf.manager.entity.Recipe;
import antafes.sf.manager.gui.element.PlanChainView;
import antafes.sf.manager.gui.element.PlanStepNode;
import antafes.sf.manager.gui.order.LocalizedItemOrder;
import antafes.sf.manager.gui.renderer.TableStyle;
import antafes.sf.manager.repository.ItemRepository;
import antafes.sf.manager.service.FactoryService;
import antafes.sf.manager.service.PlanCalculationService;
import antafes.sf.manager.service.PlanService;
import antafes.sf.manager.service.dto.PlanResult;
import antafes.sf.manager.service.dto.PlanStep;
import antafes.utilities.language.LanguageInterface;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@org.springframework.stereotype.Component
@Scope("prototype")
public class PlanningPanel extends JPanel
{
    @Autowired
    private PlanService planService;
    @Autowired
    private PlanCalculationService planCalculationService;
    @Autowired
    private FactoryService factoryService;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private Configuration configuration;

    private String selectedLanguage;
    private LanguageInterface language;
    private NumberFormat numberFormat;
    private boolean updating;
    private Plan plan;
    private PlanResult result;
    private final PlanStepNode.Listener stepListener = new PlanStepNode.Listener()
    {
        @Override
        public void recipeSelected(PlanStep step, Recipe recipe)
        {
            SwingUtilities.invokeLater(() -> PlanningPanel.this.onRecipeSelected(step, recipe));
        }

        @Override
        public void settingsChanged(PlanStep step, double clockSpeed, int somersloops)
        {
            SwingUtilities.invokeLater(() -> PlanningPanel.this.onMachineSettingsChanged(
                step,
                clockSpeed,
                somersloops
            ));
        }

        @Override
        public void createProductionRequested(PlanStep step)
        {
            PlanningPanel.this.createProductions(step);
        }
    };

    private JComboBox<Plan> planSelect;
    private JButton newPlanButton;
    private JButton deletePlanButton;
    private JComboBox<Item> targetSelect;
    private JSpinner rateSpinner;
    private PlanChainView chainView;
    private JScrollPane chainScrollPane;
    private DefaultTableModel rawModel;
    private DefaultTableModel surplusModel;
    private JLabel powerLabel;
    private JLabel warningLabel;

    public PlanningPanel()
    {
    }

    @PostConstruct
    private void initializePanel()
    {
        this.selectedLanguage = this.configuration.getLanguage().getName();
        this.language = this.configuration.getLanguageObject();
        this.numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag(
            "German".equals(this.selectedLanguage) ? "de" : "en"
        ));
        this.numberFormat.setMaximumFractionDigits(2);
        this.createComponents();
        this.layoutComponents();
        this.reload();
    }

    /**
     * Reloads the plans of the active factory and recalculates the selected plan.
     */
    public void reload()
    {
        this.updating = true;
        try {
            Plan previousPlan = this.plan;
            List<Plan> plans = this.planService.getPlans();
            this.planSelect.removeAllItems();
            plans.forEach(this.planSelect::addItem);
            Plan selectedPlan = previousPlan == null ? null : plans.stream()
                .filter(candidate -> candidate.getId().equals(previousPlan.getId()))
                .findFirst()
                .orElse(null);
            if (selectedPlan == null && !plans.isEmpty()) {
                selectedPlan = plans.getFirst();
            }
            this.planSelect.setSelectedItem(selectedPlan);
            this.plan = selectedPlan;
            this.showPlan();
        } finally {
            this.updating = false;
        }
        this.recalculate();
    }

    /**
     * Recalculates the selected plan, e.g. after the factory's productions changed.
     */
    public void recalculate()
    {
        this.updating = true;
        try {
            this.result = this.plan == null ? null : this.planService.calculate(this.plan);
            this.fillTables();
            this.rebuildChain();
        } finally {
            this.updating = false;
        }
        this.updateControls();
    }

    private void createComponents()
    {
        this.planSelect = new JComboBox<>();
        this.planSelect.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            ) {
                return super.getListCellRendererComponent(
                    list,
                    value instanceof Plan listedPlan ? listedPlan.getName() : value,
                    index,
                    isSelected,
                    cellHasFocus
                );
            }
        });
        this.planSelect.addActionListener(event -> this.onPlanSelected());
        this.newPlanButton = new JButton(this.language.translate("planNew"));
        this.newPlanButton.addActionListener(event -> this.createPlan());
        this.deletePlanButton = new JButton(this.language.translate("planDelete"));
        this.deletePlanButton.addActionListener(event -> this.deletePlan());

        this.targetSelect = new JComboBox<>();
        this.targetSelect.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            ) {
                return super.getListCellRendererComponent(
                    list,
                    value instanceof Item item ? PlanningPanel.this.localizedName(item.getName()) : value,
                    index,
                    isSelected,
                    cellHasFocus
                );
            }
        });
        this.getTargetItems().forEach(this.targetSelect::addItem);
        this.targetSelect.addActionListener(event -> this.onTargetChanged());
        this.rateSpinner = new JSpinner(new SpinnerNumberModel(30.0, 0.01, 1000000.0, 1.0));
        this.rateSpinner.addChangeListener(event -> this.onRateChanged());

        this.chainView = new PlanChainView(this.language, this.selectedLanguage, this.numberFormat);
        this.chainScrollPane = new JScrollPane(this.chainView);
        this.chainScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        this.chainScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        this.rawModel = this.createReadOnlyModel(
            this.language.translate("item"),
            this.language.translate("planRate"),
            this.language.translate("planAvailable")
        );
        this.surplusModel = this.createReadOnlyModel(
            this.language.translate("item"),
            this.language.translate("planRate"),
            this.language.translate("planOrigin")
        );
        this.powerLabel = new JLabel();
        this.warningLabel = new JLabel();
        this.warningLabel.setForeground(new Color(176, 0, 32));
    }

    private void layoutComponents()
    {
        JPanel planBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        planBar.add(new JLabel(this.language.translate("planPlan")));
        planBar.add(this.planSelect);
        planBar.add(this.newPlanButton);
        planBar.add(this.deletePlanButton);
        planBar.add(new JLabel(this.language.translate("planTargetItem")));
        planBar.add(this.targetSelect);
        planBar.add(new JLabel(this.language.translate("planTargetRate")));
        planBar.add(this.rateSpinner);
        planBar.add(this.powerLabel);

        JTabbedPane materialTabs = new JTabbedPane();
        materialTabs.addTab(
            this.language.translate("planRawMaterials"),
            new JScrollPane(this.createTable(this.rawModel))
        );
        materialTabs.addTab(
            this.language.translate("planSurplus"),
            new JScrollPane(this.createTable(this.surplusModel))
        );
        materialTabs.setPreferredSize(new Dimension(100, 160));

        JPanel summary = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        summary.add(this.warningLabel);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(materialTabs, BorderLayout.CENTER);
        bottom.add(summary, BorderLayout.SOUTH);

        this.setLayout(new BorderLayout());
        this.add(planBar, BorderLayout.NORTH);
        this.add(this.chainScrollPane, BorderLayout.CENTER);
        this.add(bottom, BorderLayout.SOUTH);
    }

    private DefaultTableModel createReadOnlyModel(String... columns)
    {
        return new DefaultTableModel(columns, 0)
        {
            @Override
            public boolean isCellEditable(int row, int column)
            {
                return false;
            }
        };
    }

    private JTable createTable(DefaultTableModel model)
    {
        JTable table = new JTable(model);
        TableStyle.apply(table);
        table.setAutoCreateRowSorter(true);

        return table;
    }

    private List<Item> getTargetItems()
    {
        return this.itemRepository.findAll().values().stream()
            .filter(item -> !this.planCalculationService.isRaw(item))
            .sorted(LocalizedItemOrder.alphabeticalComparator(this.selectedLanguage))
            .toList();
    }

    private void onPlanSelected()
    {
        if (this.updating) {
            return;
        }
        this.plan = (Plan) this.planSelect.getSelectedItem();
        this.updating = true;
        try {
            this.showPlan();
        } finally {
            this.updating = false;
        }
        this.recalculate();
    }

    private void onTargetChanged()
    {
        if (this.updating || this.plan == null || this.targetSelect.getSelectedItem() == null) {
            return;
        }
        this.plan.setTargetItemKey(((Item) this.targetSelect.getSelectedItem()).getKey());
        this.savePlan();
        this.recalculate();
    }

    private void onRateChanged()
    {
        if (this.updating || this.plan == null) {
            return;
        }
        this.plan.setTargetRate(((Number) this.rateSpinner.getValue()).doubleValue());
        this.savePlan();
        this.recalculate();
    }

    private void onRecipeSelected(PlanStep step, Recipe recipe)
    {
        if (this.updating || this.plan == null) {
            return;
        }
        String itemKey = step.item().getKey();
        if (step.sideProduction()) {
            this.plan.getSideRecipeChoices().put(itemKey, recipe == null ? Plan.NO_RECIPE : recipe.getKey());
        } else {
            this.plan.getRecipeChoices().put(itemKey, recipe.getKey());
        }
        this.savePlan();
        this.recalculate();
    }

    private void onMachineSettingsChanged(PlanStep step, double clockSpeed, int somersloops)
    {
        if (this.updating || this.plan == null) {
            return;
        }
        String settingsKey = step.sideProduction()
            ? PlanCalculationService.sideSettingsKey(step.item().getKey())
            : step.item().getKey();
        Plan.StepSettings settings = this.plan.getStepSettings()
            .computeIfAbsent(settingsKey, key -> new Plan.StepSettings());
        settings.setClockSpeed(clockSpeed);
        settings.setSomersloops(somersloops);
        this.savePlan();
        this.recalculate();
    }

    private void createPlan()
    {
        if (this.factoryService.getCurrentFactory() == null || this.targetSelect.getItemCount() == 0) {
            return;
        }
        String name = JOptionPane.showInputDialog(
            this,
            this.language.translate("planName"),
            this.language.translate("planNew"),
            JOptionPane.QUESTION_MESSAGE
        );
        if (name == null) {
            return;
        }
        if (name.isBlank()) {
            JOptionPane.showMessageDialog(
                this,
                this.language.translate("planInvalidName"),
                this.language.translate("planning"),
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        try {
            Plan created = this.planService.createPlan(
                name,
                ((Item) this.targetSelect.getItemAt(0)).getKey(),
                30.0
            );
            this.plan = created;
            this.reload();
        } catch (IOException | RuntimeException exception) {
            this.showError("planSavingFailed");
        }
    }

    private void deletePlan()
    {
        if (this.plan == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(
            this,
            this.language.translate("planConfirmDelete"),
            this.language.translate("planDelete"),
            JOptionPane.YES_NO_OPTION
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            this.planService.deletePlan(this.plan.getId());
            this.plan = null;
            this.reload();
        } catch (IOException | RuntimeException exception) {
            this.showError("planDeletionFailed");
        }
    }

    private void createProductions(PlanStep step)
    {
        try {
            int count = this.planService.createProductions(step).size();
            JOptionPane.showMessageDialog(
                this,
                String.format(this.language.translate("planProductionsCreated"), count),
                this.language.translate("planning"),
                JOptionPane.INFORMATION_MESSAGE
            );
        } catch (IOException | RuntimeException exception) {
            this.showError("planProductionCreationFailed");
        }
    }

    private void savePlan()
    {
        try {
            this.planService.savePlan(this.plan);
        } catch (IOException | RuntimeException exception) {
            this.showError("planSavingFailed");
        }
    }

    private void showError(String translationKey)
    {
        JOptionPane.showMessageDialog(
            this,
            this.language.translate(translationKey),
            this.language.translate("planning"),
            JOptionPane.ERROR_MESSAGE
        );
    }

    private void showPlan()
    {
        if (this.plan == null) {
            return;
        }
        Item targetItem = this.itemRepository.findOne(this.plan.getTargetItemKey());
        this.targetSelect.setSelectedItem(targetItem);
        this.rateSpinner.setValue(this.plan.getTargetRate());
    }

    private void updateControls()
    {
        boolean factoryOpen = this.factoryService.getCurrentFactory() != null;
        boolean hasPlan = this.plan != null;
        this.newPlanButton.setEnabled(factoryOpen);
        this.deletePlanButton.setEnabled(hasPlan);
        this.planSelect.setEnabled(hasPlan);
        this.targetSelect.setEnabled(hasPlan);
        this.rateSpinner.setEnabled(hasPlan);
    }

    private void fillTables()
    {
        this.rawModel.setRowCount(0);
        this.surplusModel.setRowCount(0);
        if (this.result == null) {
            this.powerLabel.setText("");
            this.warningLabel.setText("");
            return;
        }
        this.result.rawInputs().forEach(material -> this.rawModel.addRow(new Object[] {
            this.localizedName(material.item().getName()),
            this.perMinute(material.rate()),
            this.perMinute(material.available())
        }));
        this.result.surplus().forEach(material -> this.surplusModel.addRow(new Object[] {
            this.localizedName(material.item().getName()),
            this.perMinute(material.rate()),
            this.language.translate(material.targetByproduct() ? "planTargetByproduct" : "planSideProduction")
        }));
        this.powerLabel.setText(
            this.language.translate("planTotalPower") + ": "
                + this.numberFormat.format(this.result.totalPowerConsumption()) + " MW"
        );
        this.warningLabel.setText(this.result.warnings().stream()
            .map(this.language::translate)
            .collect(Collectors.joining(" ")));
    }

    private void rebuildChain()
    {
        Point position = this.chainScrollPane.getViewport().getViewPosition();
        this.chainView.setResult(this.result, step -> new PlanStepNode(
            step,
            step.sideProduction()
                ? this.planCalculationService.getRecipesConsuming(step.item())
                : this.planCalculationService.getRecipesProducing(step.item()),
            this.language,
            this.selectedLanguage,
            this.numberFormat,
            this.stepListener
        ));
        SwingUtilities.invokeLater(() -> this.chainScrollPane.getViewport().setViewPosition(position));
    }

    private String localizedName(Name name)
    {
        return name == null ? "" : name.getLocalizedName(this.selectedLanguage);
    }

    private String perMinute(double amount)
    {
        return this.numberFormat.format(amount) + " / min";
    }
}
