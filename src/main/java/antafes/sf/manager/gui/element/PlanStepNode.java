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

package antafes.sf.manager.gui.element;

import antafes.sf.manager.entity.Name;
import antafes.sf.manager.entity.Recipe;
import antafes.sf.manager.gui.util.MaterialIconLoader;
import antafes.sf.manager.service.dto.PlanMaterial;
import antafes.sf.manager.service.dto.PlanStep;
import antafes.utilities.language.LanguageInterface;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.List;

/**
 * A card of the production chain showing the building image with the adjustments of the step directly below it.
 */
public class PlanStepNode extends JPanel
{
    public static final int IMAGE_SIZE = 96;
    public static final int WIDTH = 240;

    public interface Listener
    {
        void recipeSelected(PlanStep step, Recipe recipe);

        void settingsChanged(PlanStep step, double clockSpeed, int somersloops);

        void createProductionRequested(PlanStep step);
    }

    private final PlanStep step;
    private final LanguageInterface language;
    private final String selectedLanguage;
    private final NumberFormat numberFormat;
    private final Listener listener;
    private boolean updating;

    public PlanStepNode(
        PlanStep step,
        List<Recipe> recipes,
        LanguageInterface language,
        String selectedLanguage,
        NumberFormat numberFormat,
        Listener listener
    ) {
        this.step = step;
        this.language = language;
        this.selectedLanguage = selectedLanguage;
        this.numberFormat = numberFormat;
        this.listener = listener;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(step.sideProduction() ? Color.GRAY : Color.DARK_GRAY),
            BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        this.addCentered(this.createTitle());
        this.addCentered(new JLabel(MaterialIconLoader.load(step.building().getIcon(), IMAGE_SIZE)));
        this.addCentered(new JLabel(this.localizedName(step.building().getName())));
        this.add(Box.createVerticalStrut(4));
        this.add(this.createRecipeSelect(recipes));
        this.add(Box.createVerticalStrut(4));
        this.add(this.createSettings());
        this.add(Box.createVerticalStrut(6));
        this.createSummary().forEach(this::addCentered);
        this.add(Box.createVerticalStrut(4));
        this.addCentered(this.createProductionButton());
        this.setMaximumSize(new Dimension(WIDTH, Integer.MAX_VALUE));
    }

    @Override
    public Dimension getPreferredSize()
    {
        Dimension size = super.getPreferredSize();

        return new Dimension(WIDTH, size.height);
    }

    private JLabel createTitle()
    {
        String title = this.localizedName(this.step.item().getName());
        if (this.step.sideProduction()) {
            title += " (" + this.language.translate("planSideProduction") + ")";
        }
        JLabel label = new JLabel(title, MaterialIconLoader.load(this.step.item().getIcon(), 24), SwingConstants.LEFT);
        label.setFont(label.getFont().deriveFont(Font.BOLD));

        return label;
    }

    private JComboBox<RecipeOption> createRecipeSelect(List<Recipe> recipes)
    {
        JComboBox<RecipeOption> select = new JComboBox<>();
        RecipeOption selected = null;
        if (this.step.sideProduction()) {
            select.addItem(new RecipeOption(null, this.language.translate("planNone")));
        }
        for (Recipe recipe : recipes) {
            RecipeOption option = new RecipeOption(recipe, this.recipeLabel(recipe));
            select.addItem(option);
            if (recipe.getKey().equals(this.step.recipe().getKey())) {
                selected = option;
            }
        }
        select.setSelectedItem(selected);
        select.setAlignmentX(Component.CENTER_ALIGNMENT);
        select.setMaximumSize(new Dimension(Integer.MAX_VALUE, select.getPreferredSize().height));
        select.addActionListener(event -> {
            RecipeOption option = (RecipeOption) select.getSelectedItem();
            if (!this.updating && option != null) {
                this.listener.recipeSelected(this.step, option.recipe());
            }
        });

        return select;
    }

    private JPanel createSettings()
    {
        JSpinner clock = new JSpinner(new SpinnerNumberModel(this.step.clockSpeed(), 0.01, 250.0, 1.0));
        clock.setEnabled(this.step.building().isOverclockable());
        JSpinner somersloops = new JSpinner(new SpinnerNumberModel(
            this.step.somersloops(),
            0,
            this.step.building().getSomersloopSlots(),
            1
        ));
        somersloops.setEnabled(this.step.building().getSomersloopSlots() > 0);
        javax.swing.event.ChangeListener changeListener = event -> this.listener.settingsChanged(
            this.step,
            ((Number) clock.getValue()).doubleValue(),
            ((Number) somersloops.getValue()).intValue()
        );
        clock.addChangeListener(changeListener);
        somersloops.addChangeListener(changeListener);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        panel.add(new JLabel(this.language.translate("clockSpeed")));
        panel.add(clock);
        panel.add(new JLabel(this.language.translate("somersloops")));
        panel.add(somersloops);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));

        return panel;
    }

    private List<JComponent> createSummary()
    {
        List<JComponent> components = new java.util.ArrayList<>();
        components.add(new JLabel(this.language.translate("planMachines") + ": " + this.machineDescription()));
        components.add(new JLabel(
            this.language.translate("power") + ": " + this.numberFormat.format(this.step.powerConsumption()) + " MW"
        ));
        components.add(this.createMaterialLines("planInputs", this.step.inputs()));
        components.add(this.createMaterialLines("planOutputs", this.step.outputs()));

        return components;
    }

    private JPanel createMaterialLines(String headingKey, List<PlanMaterial> materials)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel(this.language.translate(headingKey)));
        for (PlanMaterial material : materials) {
            panel.add(new JLabel(
                this.numberFormat.format(material.rate()) + " " + this.localizedName(material.item().getName()),
                MaterialIconLoader.load(material.item().getIcon(), 16),
                SwingConstants.LEFT
            ));
        }

        return panel;
    }

    private JButton createProductionButton()
    {
        JButton button = new JButton(this.language.translate("planCreateProduction"));
        button.setEnabled(this.step.machineCount() > 0);
        button.addActionListener(event -> this.listener.createProductionRequested(this.step));

        return button;
    }

    private void addCentered(JComponent component)
    {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        this.add(component);
    }

    private String machineDescription()
    {
        List<String> parts = new java.util.ArrayList<>();
        if (this.step.fullMachines() > 0) {
            parts.add(this.step.fullMachines() + " × " + this.numberFormat.format(this.step.clockSpeed()) + " %");
        }
        if (this.step.partialClockSpeed() > 0) {
            parts.add("1 × " + this.numberFormat.format(this.step.partialClockSpeed()) + " %");
        }

        return String.join(" + ", parts);
    }

    private String recipeLabel(Recipe recipe)
    {
        String label = this.localizedName(recipe.getName());

        return recipe.isAlternate() ? label + " (" + this.language.translate("planAlternate") + ")" : label;
    }

    private String localizedName(Name name)
    {
        return name == null ? "" : name.getLocalizedName(this.selectedLanguage);
    }

    private record RecipeOption(Recipe recipe, String label)
    {
        @Override
        public String toString()
        {
            return this.label;
        }
    }
}
