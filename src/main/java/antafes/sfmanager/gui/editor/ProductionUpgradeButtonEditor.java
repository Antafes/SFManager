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

package antafes.sfmanager.gui.editor;

import antafes.sfmanager.entity.Building;
import antafes.sfmanager.service.ProductionService;
import antafes.utilities.language.LanguageInterface;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ProductionUpgradeButtonEditor extends AbstractCellEditor
    implements TableCellEditor, TableCellRenderer
{
    private final ProductionService productionService;
    private final LanguageInterface language;
    private final String selectedLanguage;
    private final Component parent;
    private final Runnable onUpgraded;
    private final Function<List<Building>, Building> selection;
    private final JButton rendererButton = new JButton();
    private final JButton editorButton = new JButton();
    private final JLabel emptyCell = new JLabel();
    private MinerUpgrade upgrade;

    public ProductionUpgradeButtonEditor(
        ProductionService productionService,
        LanguageInterface language,
        String selectedLanguage,
        Component parent,
        Runnable onUpgraded
    )
    {
        this.productionService = productionService;
        this.language = language;
        this.selectedLanguage = selectedLanguage;
        this.parent = parent;
        this.onUpgraded = onUpgraded;
        this.selection = this::selectUpgrade;
        this.configureButtons();
    }

    public ProductionUpgradeButtonEditor(
        ProductionService productionService,
        LanguageInterface language,
        String selectedLanguage,
        Component parent,
        Runnable onUpgraded,
        Function<List<Building>, Building> selection
    )
    {
        this.productionService = productionService;
        this.language = language;
        this.selectedLanguage = selectedLanguage;
        this.parent = parent;
        this.onUpgraded = onUpgraded;
        this.selection = selection;
        this.configureButtons();
    }

    @Override
    public Object getCellEditorValue()
    {
        return this.upgrade;
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable table,
        Object value,
        boolean isSelected,
        int row,
        int column
    )
    {
        this.upgrade = value instanceof MinerUpgrade minerUpgrade ? minerUpgrade : null;
        this.editorButton.setEnabled(this.upgrade != null && !"miner_3".equals(this.upgrade.buildingKey()));
        return this.editorButton;
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column
    )
    {
        if (!(value instanceof MinerUpgrade minerUpgrade)) {
            this.emptyCell.setOpaque(true);
            this.emptyCell.setBackground(isSelected
                ? table.getSelectionBackground()
                : table.getBackground());
            return this.emptyCell;
        }
        this.rendererButton.setEnabled(!"miner_3".equals(minerUpgrade.buildingKey()));
        return this.rendererButton;
    }

    private void configureButtons()
    {
        String upgradeLabel = this.language.translate("upgrade");
        this.configureButton(this.rendererButton, upgradeLabel);
        this.configureButton(this.editorButton, upgradeLabel);
        this.editorButton.addActionListener(event -> this.upgradeMiner());
    }

    private void configureButton(JButton button, String upgradeLabel)
    {
        button.setIcon(new UpgradeIcon());
        button.setDisabledIcon(new UpgradeIcon());
        button.setToolTipText(upgradeLabel);
        button.getAccessibleContext().setAccessibleName(upgradeLabel);
    }

    private Building selectUpgrade(List<Building> upgrades)
    {
        JComboBox<Building> select = new JComboBox<>(upgrades.toArray(Building[]::new));
        select.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                javax.swing.JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            )
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus
                );
                if (value instanceof Building building) {
                    label.setText(building.getName() == null
                        ? building.getKey()
                        : building.getName().getLocalizedName(
                            ProductionUpgradeButtonEditor.this.selectedLanguage
                        ));
                }
                return label;
            }
        });
        int choice = JOptionPane.showConfirmDialog(
            this.parent,
            select,
            this.language.translate("upgradeMiner"),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        return choice == JOptionPane.OK_OPTION ? (Building) select.getSelectedItem() : null;
    }

    private void upgradeMiner()
    {
        boolean upgraded = false;
        try {
            if (this.upgrade != null) {
                List<Building> upgrades = this.productionService.getMinerUpgradeBuildings(
                    this.upgrade.productionId()
                );
                Building selectedBuilding = upgrades.isEmpty() ? null : this.selection.apply(upgrades);
                if (selectedBuilding != null) {
                    this.productionService.upgradeMiner(
                        this.upgrade.productionId(),
                        selectedBuilding.getKey()
                    );
                    upgraded = true;
                }
            }
        } catch (IOException | RuntimeException exception) {
            JOptionPane.showMessageDialog(
                this.parent,
                Objects.toString(
                    exception.getMessage(),
                    this.language.translate("productionUpgradeFailed")
                ),
                this.language.translate("upgradeMiner"),
                JOptionPane.ERROR_MESSAGE
            );
        } finally {
            this.fireEditingStopped();
        }
        if (upgraded) {
            this.onUpgraded.run();
        }
    }

    private static final class UpgradeIcon implements Icon
    {
        @Override
        public int getIconWidth()
        {
            return 18;
        }

        @Override
        public int getIconHeight()
        {
            return 18;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y)
        {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                graphics2D.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
                );
                Color color = component.isEnabled() ? component.getForeground() : Color.GRAY;
                graphics2D.setColor(color == null ? Color.BLACK : color);
                graphics2D.setStroke(new BasicStroke(1.5f));
                graphics2D.drawLine(x + 3, y + 8, x + 3, y + 12);
                graphics2D.drawLine(x + 15, y + 8, x + 15, y + 12);
                graphics2D.drawLine(x + 1, y + 8, x + 3, y + 8);
                graphics2D.drawLine(x + 15, y + 8, x + 17, y + 8);
                graphics2D.fillOval(x, y + 6, 4, 4);
                graphics2D.fillOval(x + 14, y + 6, 4, 4);

                Polygon gear = new Polygon(
                    new int[] {
                        x + 2, x + 5, x + 4, x + 6, x + 7, x + 11, x + 12, x + 14,
                        x + 13, x + 16, x + 14, x + 12, x + 11, x + 7, x + 6, x + 4
                    },
                    new int[] {
                        y + 11, y + 11, y + 13, y + 13, y + 16, y + 16, y + 13, y + 13,
                        y + 11, y + 11, y + 14, y + 16, y + 18, y + 18, y + 16, y + 14
                    },
                    16
                );
                graphics2D.fillPolygon(gear);
                graphics2D.fillPolygon(new Polygon(
                    new int[] {x + 9, x + 5, x + 7, x + 7, x + 11, x + 11, x + 13},
                    new int[] {y + 1, y + 6, y + 6, y + 12, y + 12, y + 6, y + 6},
                    7
                ));
            } finally {
                graphics2D.dispose();
            }
        }
    }
}
