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

import antafes.sf.manager.entity.Building;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * An editable building select with the buildings grouped by production buildings and consumers.
 * Typing filters the list; text that doesn't match a building is declined.
 */
public class GroupedBuildingSelect extends JComboBox<Object>
{
    private final BuildingModel buildingModel;
    private final Function<Building, String> nameProvider;
    private final JTextField textField = new JTextField();
    private boolean updatingText;

    public GroupedBuildingSelect(
        List<Building> buildings,
        Function<Building, String> nameProvider,
        String productionHeader,
        String consumerHeader
    ) {
        super(new BuildingModel(buildings, nameProvider, productionHeader, consumerHeader));
        this.buildingModel = (BuildingModel) this.getModel();
        this.nameProvider = nameProvider;
        this.setEditable(true);
        this.setEditor(new BuildingEditor());
        this.setRenderer(new BuildingRenderer());
        this.textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event)
            {
                GroupedBuildingSelect.this.filterByText();
            }

            @Override
            public void removeUpdate(DocumentEvent event)
            {
                GroupedBuildingSelect.this.filterByText();
            }

            @Override
            public void changedUpdate(DocumentEvent event)
            {
                GroupedBuildingSelect.this.filterByText();
            }
        });
        this.textField.addActionListener(event -> SwingUtilities.invokeLater(this::resetText));
        this.textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event)
            {
                GroupedBuildingSelect.this.resetText();
            }
        });
        this.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent event)
            {
                SwingUtilities.invokeLater(GroupedBuildingSelect.this::scrollHeaderIntoView);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent event)
            {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent event)
            {
            }
        });
        this.installHeaderSkipping();
        this.resetText();
    }

    private void installHeaderSkipping()
    {
        if (!(this.getUI().getAccessibleChild(this, 0) instanceof javax.swing.plaf.basic.ComboPopup popup)) {
            return;
        }

        popup.getList().setSelectionModel(new DefaultListSelectionModel() {
            @Override
            public void setSelectionInterval(int index0, int index1)
            {
                int current = this.getMinSelectionIndex();
                int step = current >= 0 && index0 < current ? -1 : 1;
                int target = index0;

                while (target >= 0
                    && target < GroupedBuildingSelect.this.getItemCount()
                    && !(GroupedBuildingSelect.this.getItemAt(target) instanceof Building)
                ) {
                    target += step;
                }

                if (target < 0 || target >= GroupedBuildingSelect.this.getItemCount()) {
                    return;
                }

                super.setSelectionInterval(target, target);
            }
        });
    }

    private void scrollHeaderIntoView()
    {
        if (this.getSelectedIndex() > 1 || !(this.getUI().getAccessibleChild(this, 0) instanceof JPopupMenu popup)) {
            return;
        }

        for (Component component : popup.getComponents()) {
            if (component instanceof JScrollPane scrollPane) {
                scrollPane.getViewport().setViewPosition(new Point(0, 0));
            }
        }
    }
    private void filterByText()
    {
        if (this.updatingText) {
            return;
        }

        this.buildingModel.setFilter(this.textField.getText());
        if (this.buildingModel.getSize() > 0 && this.textField.isFocusOwner()) {
            this.showPopup();
        } else {
            this.hidePopup();
        }
    }

    private void resetText()
    {
        this.updatingText = true;
        try {
            Object selected = this.buildingModel.getSelectedItem();
            this.textField.setText(selected instanceof Building building ? this.nameProvider.apply(building) : "");
            this.buildingModel.setFilter("");
        } finally {
            this.updatingText = false;
        }
    }

    private Building resolveTypedBuilding()
    {
        String text = this.textField.getText().trim();
        for (Building building : this.buildingModel.getBuildings()) {
            if (this.nameProvider.apply(building).equalsIgnoreCase(text)) {
                return building;
            }
        }
        List<Building> visible = this.buildingModel.getVisibleBuildings();

        return visible.size() == 1 ? visible.getFirst() : (Building) this.buildingModel.getSelectedItem();
    }

    private class BuildingEditor implements ComboBoxEditor
    {
        @Override
        public Component getEditorComponent()
        {
            return GroupedBuildingSelect.this.textField;
        }

        @Override
        public void setItem(Object item)
        {
            if (GroupedBuildingSelect.this.updatingText || GroupedBuildingSelect.this.buildingModel.isFiltering()) {
                return;
            }
            GroupedBuildingSelect.this.updatingText = true;
            try {
                GroupedBuildingSelect.this.textField.setText(
                    item instanceof Building building ? GroupedBuildingSelect.this.nameProvider.apply(building) : ""
                );
            } finally {
                GroupedBuildingSelect.this.updatingText = false;
            }
        }

        @Override
        public Object getItem()
        {
            return GroupedBuildingSelect.this.resolveTypedBuilding();
        }

        @Override
        public void selectAll()
        {
            GroupedBuildingSelect.this.textField.selectAll();
        }

        @Override
        public void addActionListener(java.awt.event.ActionListener listener)
        {
            GroupedBuildingSelect.this.textField.addActionListener(listener);
        }

        @Override
        public void removeActionListener(java.awt.event.ActionListener listener)
        {
            GroupedBuildingSelect.this.textField.removeActionListener(listener);
        }
    }

    private class BuildingRenderer implements ListCellRenderer<Object>
    {
        private final DefaultListCellRenderer renderer = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            if (value instanceof String header) {
                JLabel label = (JLabel) this.renderer.getListCellRendererComponent(
                    list, header, index, false, false
                );
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                label.setBackground(UIManager.getColor("Panel.background"));
                label.setEnabled(false);

                return label;
            }

            JLabel label = (JLabel) this.renderer.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus
            );
            if (value instanceof Building building) {
                label.setText(GroupedBuildingSelect.this.nameProvider.apply(building));
            }
            label.setBorder(BorderFactory.createEmptyBorder(1, index >= 0 ? 12 : 1, 1, 1));

            return label;
        }
    }

    private static class BuildingModel extends AbstractListModel<Object> implements ComboBoxModel<Object>
    {
        private final List<Building> buildings;
        private final Function<Building, String> nameProvider;
        private final String productionHeader;
        private final String consumerHeader;
        private List<Object> visible = List.of();
        private Object selected;
        private boolean filtering;

        private BuildingModel(
            List<Building> buildings,
            Function<Building, String> nameProvider,
            String productionHeader,
            String consumerHeader
        ) {
            this.buildings = buildings.stream()
                .sorted(Comparator.comparing(nameProvider, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Building::getKey))
                .toList();
            this.nameProvider = nameProvider;
            this.productionHeader = productionHeader;
            this.consumerHeader = consumerHeader;
            this.rebuild("");
            this.selected = this.getVisibleBuildings().isEmpty() ? null : this.getVisibleBuildings().getFirst();
        }

        @Override
        public int getSize()
        {
            return this.visible.size();
        }

        @Override
        public Object getElementAt(int index)
        {
            return this.visible.get(index);
        }

        @Override
        public void setSelectedItem(Object item)
        {
            if (item != null && !(item instanceof Building && this.buildings.contains(item))) {
                return;
            }
            if (java.util.Objects.equals(this.selected, item)) {
                return;
            }
            this.selected = item;
            this.rebuild("");
            this.fireContentsChanged(this, -1, -1);
        }

        @Override
        public Object getSelectedItem()
        {
            return this.selected;
        }

        private List<Building> getBuildings()
        {
            return this.buildings;
        }

        private List<Building> getVisibleBuildings()
        {
            return this.visible.stream()
                .filter(Building.class::isInstance)
                .map(Building.class::cast)
                .toList();
        }

        private boolean isFiltering()
        {
            return this.filtering;
        }

        private void setFilter(String text)
        {
            this.filtering = true;
            try {
                this.rebuild(text);
                this.fireContentsChanged(this, 0, Math.max(0, this.visible.size() - 1));
            } finally {
                this.filtering = false;
            }
        }

        private void rebuild(String text)
        {
            String needle = text.trim().toLowerCase();
            List<Building> production = new ArrayList<>();
            List<Building> consumers = new ArrayList<>();
            for (Building building : this.buildings) {
                if (!this.nameProvider.apply(building).toLowerCase().contains(needle)) {
                    continue;
                }
                if (building.getCategory() != null && building.getCategory().isConsumer()) {
                    consumers.add(building);
                } else {
                    production.add(building);
                }
            }

            List<Object> entries = new ArrayList<>();
            if (!production.isEmpty()) {
                entries.add(this.productionHeader);
                entries.addAll(production);
            }
            if (!consumers.isEmpty()) {
                entries.add(this.consumerHeader);
                entries.addAll(consumers);
            }
            this.visible = List.copyOf(entries);
        }
    }
}
