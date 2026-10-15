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

import antafes.sf.manager.entity.Item;
import antafes.sf.manager.entity.ProductionMaterialSource;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.service.dto.MaterialSourceGroup;
import antafes.utilities.language.LanguageInterface;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Lets the user select multiple source groups per material. The combo boxes only offer the remaining production
 * of each group.
 */
public class MaterialSourceSelector extends JPanel
{
    private final ProductionService productionService;
    private final LanguageInterface language;
    private final NumberFormat numberFormat;
    private final String selectedLanguage;
    private final Runnable structureListener;
    private final List<MaterialBlock> blocks = new ArrayList<>();
    private boolean enabled = true;

    public MaterialSourceSelector(
        ProductionService productionService,
        LanguageInterface language,
        NumberFormat numberFormat,
        String selectedLanguage,
        Runnable structureListener
    )
    {
        super(new GridBagLayout());
        this.productionService = productionService;
        this.language = language;
        this.numberFormat = numberFormat;
        this.selectedLanguage = selectedLanguage;
        this.structureListener = structureListener;
    }

    /**
     * Adds a material with its already selected groups.
     *
     * @param item                The material item
     * @param required            Whether the label is marked as required
     * @param selectedGroups      The groups to preselect
     * @param excludedConsumerIds Productions whose own consumption is treated as free
     */
    public void addMaterial(
        Item item,
        boolean required,
        List<String> selectedGroups,
        Collection<UUID> excludedConsumerIds
    )
    {
        MaterialBlock block = new MaterialBlock(
            item,
            this.productionService.getRemainingMaterialSourceGroups(item, excludedConsumerIds)
        );
        for (String group : selectedGroups) {
            block.addRow(group);
        }
        if (selectedGroups.isEmpty()) {
            block.addRow(null);
        }
        block.refreshOptions();

        int row = this.blocks.size();
        String labelText = item.getName().getLocalizedName(this.selectedLanguage) + (required ? " *" : "");
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        labelConstraints.insets = new Insets(6, 4, 3, 8);
        this.add(new JLabel(labelText), labelConstraints);

        GridBagConstraints blockConstraints = new GridBagConstraints();
        blockConstraints.gridx = 1;
        blockConstraints.gridy = row;
        blockConstraints.weightx = 1;
        blockConstraints.fill = GridBagConstraints.HORIZONTAL;
        blockConstraints.insets = new Insets(3, 4, 3, 4);
        this.add(block.panel, blockConstraints);
        this.blocks.add(block);
        block.updateEnabled(this.enabled);
    }

    public boolean isEmpty()
    {
        return this.blocks.isEmpty();
    }

    /**
     * Whether every material has at least one selected source group.
     */
    public boolean isComplete()
    {
        return this.blocks.stream().allMatch(block -> !block.getSelectedGroups().isEmpty());
    }

    public List<ProductionMaterialSource> getMaterialSources()
    {
        List<ProductionMaterialSource> materialSources = new ArrayList<>();
        for (MaterialBlock block : this.blocks) {
            for (String group : block.getSelectedGroups()) {
                materialSources.add(new ProductionMaterialSource().setItemKey(block.item.getKey()).setGroup(group));
            }
        }
        return materialSources;
    }

    public List<Component> getFocusableComponents()
    {
        List<Component> components = new ArrayList<>();
        for (MaterialBlock block : this.blocks) {
            components.addAll(block.getFocusableComponents());
        }
        return components;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        this.enabled = enabled;
        this.blocks.forEach(block -> block.updateEnabled(enabled));
    }

    private void fireStructureChanged()
    {
        this.revalidate();
        this.repaint();
        if (this.structureListener != null) {
            this.structureListener.run();
        }
    }

    private DefaultListCellRenderer createRenderer()
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
                        + MaterialSourceSelector.this.numberFormat.format(sourceGroup.production()) + " / min "
                        + MaterialSourceSelector.this.language.translate("materialSourceRemaining") + ")");
                }
                return this;
            }
        };
    }

    private class MaterialBlock
    {
        private final Item item;
        private final List<MaterialSourceGroup> availableGroups;
        private final JPanel panel = new JPanel(new GridBagLayout());
        private final List<Row> rows = new ArrayList<>();
        private final JButton addButton = new JButton("+");
        private boolean refreshing;

        private MaterialBlock(Item item, List<MaterialSourceGroup> remainingGroups)
        {
            this.item = item;
            this.availableGroups = new ArrayList<>(remainingGroups);
            this.addButton.setToolTipText(MaterialSourceSelector.this.language.translate("addMaterialSource"));
            this.addButton.addActionListener(event -> {
                this.addRow(null);
                this.refreshOptions();
                MaterialSourceSelector.this.fireStructureChanged();
            });
        }

        private void addRow(String selectedGroup)
        {
            if (selectedGroup != null
                && this.availableGroups.stream().noneMatch(group -> group.group().equals(selectedGroup))) {
                this.availableGroups.add(new MaterialSourceGroup(selectedGroup, 0));
            }
            Row row = new Row(selectedGroup);
            this.rows.add(row);
        }

        private List<String> getSelectedGroups()
        {
            return this.rows.stream()
                .map(row -> row.select.getSelectedItem())
                .filter(MaterialSourceGroup.class::isInstance)
                .map(selected -> ((MaterialSourceGroup) selected).group())
                .toList();
        }

        private List<Component> getFocusableComponents()
        {
            List<Component> components = new ArrayList<>();
            for (Row row : this.rows) {
                components.add(row.select);
                components.add(row.removeButton);
            }
            components.add(this.addButton);
            return components;
        }

        private void updateEnabled(boolean enabled)
        {
            this.rows.forEach(row -> row.select.setEnabled(enabled));
            this.refreshButtons();
        }

        /**
         * Rebuilds the layout and offers every row only the groups that are not selected in another row.
         */
        private void refreshOptions()
        {
            this.refreshing = true;
            try {
                List<String> selectedGroups = this.getSelectedGroups();
                for (Row row : this.rows) {
                    Object selected = row.select.getSelectedItem();
                    String ownGroup = selected instanceof MaterialSourceGroup sourceGroup
                        ? sourceGroup.group()
                        : null;
                    DefaultComboBoxModel<MaterialSourceGroup> model = new DefaultComboBoxModel<>();
                    for (MaterialSourceGroup group : this.availableGroups) {
                        if (group.group().equals(ownGroup) || !selectedGroups.contains(group.group())) {
                            model.addElement(group);
                        }
                    }
                    row.select.setModel(model);
                    row.select.setSelectedIndex(-1);
                    for (int index = 0; ownGroup != null && index < model.getSize(); index++) {
                        if (ownGroup.equals(model.getElementAt(index).group())) {
                            row.select.setSelectedIndex(index);
                            break;
                        }
                    }
                }
                this.layoutRows();
                this.refreshButtons();
            } finally {
                this.refreshing = false;
            }
        }

        private void layoutRows()
        {
            this.panel.removeAll();
            int gridY = 0;
            for (Row row : this.rows) {
                GridBagConstraints selectConstraints = new GridBagConstraints();
                selectConstraints.gridx = 0;
                selectConstraints.gridy = gridY;
                selectConstraints.weightx = 1;
                selectConstraints.fill = GridBagConstraints.HORIZONTAL;
                selectConstraints.insets = new Insets(1, 0, 1, 4);
                this.panel.add(row.select, selectConstraints);

                GridBagConstraints removeConstraints = new GridBagConstraints();
                removeConstraints.gridx = 1;
                removeConstraints.gridy = gridY++;
                removeConstraints.insets = new Insets(1, 0, 1, 0);
                this.panel.add(row.removeButton, removeConstraints);
            }
            GridBagConstraints addConstraints = new GridBagConstraints();
            addConstraints.gridx = 0;
            addConstraints.gridy = gridY;
            addConstraints.anchor = GridBagConstraints.LINE_START;
            addConstraints.insets = new Insets(1, 0, 1, 4);
            this.panel.add(this.addButton, addConstraints);
            this.panel.revalidate();
            this.panel.repaint();
        }

        private void refreshButtons()
        {
            boolean enabled = MaterialSourceSelector.this.enabled;
            this.addButton.setEnabled(enabled && this.rows.size() < this.availableGroups.size());
            for (Row row : this.rows) {
                row.removeButton.setEnabled(enabled && this.rows.size() > 1);
            }
        }

        private class Row
        {
            private final JComboBox<MaterialSourceGroup> select = new JComboBox<>();
            private final JButton removeButton = new JButton("-");

            private Row(String selectedGroup)
            {
                this.select.setRenderer(MaterialSourceSelector.this.createRenderer());
                this.removeButton.setToolTipText(
                    MaterialSourceSelector.this.language.translate("removeMaterialSource")
                );
                if (selectedGroup != null) {
                    this.select.addItem(MaterialBlock.this.availableGroups.stream()
                        .filter(group -> group.group().equals(selectedGroup))
                        .findFirst()
                        .orElseThrow());
                    this.select.setSelectedIndex(0);
                }
                this.select.addActionListener(event -> {
                    if (!MaterialBlock.this.refreshing) {
                        MaterialBlock.this.refreshOptions();
                    }
                });
                this.removeButton.addActionListener(event -> {
                    MaterialBlock.this.rows.remove(this);
                    MaterialBlock.this.refreshOptions();
                    MaterialSourceSelector.this.fireStructureChanged();
                });
            }
        }
    }
}
