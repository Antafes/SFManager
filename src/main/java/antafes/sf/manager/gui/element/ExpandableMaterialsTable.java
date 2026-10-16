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

import antafes.sf.manager.entity.Item;
import antafes.sf.manager.gui.renderer.TableStyle;
import antafes.sf.manager.gui.util.MaterialIconLoader;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * A table whose rows can be folded open. The first column holds the expander, an expanded row is followed by a
 * detail row spanning the full table width that lists the used and produced materials with their icons.
 */
public class ExpandableMaterialsTable extends JTable
{
    public static final String COLLAPSED_SYMBOL = "▶";
    public static final String EXPANDED_SYMBOL = "▼";
    private static final int EXPANDER_COLUMN_WIDTH = 28;

    public record MaterialTile(Item item, String name, String amount, String source)
    {
    }

    public record MaterialsDetail(List<MaterialTile> used, List<MaterialTile> produced)
    {
    }

    /**
     * @param id    the production id, used to remember the expanded state
     * @param cells the cell values behind the expander column
     */
    public record Entry(UUID id, Object[] cells, List<MaterialTile> used, List<MaterialTile> produced)
    {
    }

    private final DefaultTableModel tableModel;
    private final String usedHeader;
    private final String producedHeader;
    private final String expandTooltip;
    private final String collapseTooltip;
    private final Set<UUID> expandedIds = new HashSet<>();
    private final List<UUID> rowIds = new ArrayList<>();
    private final Map<MaterialsDetail, JPanel> detailPanels = new IdentityHashMap<>();
    private List<Entry> entries = List.of();

    public ExpandableMaterialsTable(
        DefaultTableModel tableModel,
        String usedHeader,
        String producedHeader,
        String expandTooltip,
        String collapseTooltip
    )
    {
        super(tableModel);
        this.tableModel = tableModel;
        this.usedHeader = usedHeader;
        this.producedHeader = producedHeader;
        this.expandTooltip = expandTooltip;
        this.collapseTooltip = collapseTooltip;
        ToolTipManager.sharedInstance().registerComponent(this);
        var expanderColumn = this.getColumnModel().getColumn(0);
        expanderColumn.setPreferredWidth(EXPANDER_COLUMN_WIDTH);
        expanderColumn.setMaxWidth(EXPANDER_COLUMN_WIDTH + 10);
        this.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                int row = ExpandableMaterialsTable.this.rowAtPoint(event.getPoint());
                int column = ExpandableMaterialsTable.this.columnAtPoint(event.getPoint());
                if (SwingUtilities.isLeftMouseButton(event) && column == 0 && row >= 0
                    && !ExpandableMaterialsTable.this.isDetailRow(row)) {
                    ExpandableMaterialsTable.this.toggle(ExpandableMaterialsTable.this.rowIds.get(row));
                }
            }
        });
    }

    public void setEntries(List<Entry> entries)
    {
        this.entries = List.copyOf(entries);
        this.expandedIds.retainAll(this.entries.stream().map(Entry::id).toList());
        this.rebuild();
    }

    public void toggle(UUID productionId)
    {
        if (productionId == null) {
            return;
        }
        if (!this.expandedIds.remove(productionId)) {
            this.expandedIds.add(productionId);
        }
        this.rebuild();
    }

    public boolean isExpanded(UUID productionId)
    {
        return this.expandedIds.contains(productionId);
    }

    public boolean isDetailRow(int row)
    {
        return row >= 0 && row < this.tableModel.getRowCount()
            && this.tableModel.getValueAt(row, 0) instanceof MaterialsDetail;
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column)
    {
        if (column == 0 && !this.isDetailRow(row)) {
            return this.expanderRenderer();
        }

        return super.getCellRenderer(row, column);
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject event)
    {
        return !this.isDetailRow(row) && super.editCellAt(row, column, event);
    }

    @Override
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend)
    {
        if (this.isDetailRow(rowIndex)) {
            return;
        }
        super.changeSelection(rowIndex, columnIndex, toggle, extend);
    }

    @Override
    public String getToolTipText(MouseEvent event)
    {
        int row = this.rowAtPoint(event.getPoint());
        if (row >= 0 && this.columnAtPoint(event.getPoint()) == 0 && !this.isDetailRow(row)) {
            return this.isExpanded(this.rowIds.get(row)) ? this.collapseTooltip : this.expandTooltip;
        }

        return super.getToolTipText(event);
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        Rectangle clip = graphics.getClipBounds();
        int width = this.getColumnModel().getTotalColumnWidth();
        for (int row = 0; row < this.getRowCount(); row++) {
            if (!(this.getValueAt(row, 0) instanceof MaterialsDetail detail)) {
                continue;
            }
            Rectangle rect = this.getCellRect(row, 0, true);
            rect.width = width;
            if (clip != null && !clip.intersects(rect)) {
                continue;
            }
            JPanel panel = this.buildDetailPanel(detail);
            this.applyBackground(panel, TableStyle.getRowColor(row - 1));
            panel.setBounds(0, 0, rect.width, rect.height);
            this.layoutTree(panel);
            SwingUtilities.paintComponent(graphics, panel, this, rect.x, rect.y, rect.width, rect.height);
            if (panel.getParent() != null) {
                panel.getParent().remove(panel);
            }
        }
    }

    private void rebuild()
    {
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
        Point position = viewport == null ? null : viewport.getViewPosition();
        Set<UUID> selectedIds = new HashSet<>();
        for (int row : this.getSelectedRows()) {
            if (row < this.rowIds.size() && !this.isDetailRow(row)) {
                selectedIds.add(this.rowIds.get(row));
            }
        }

        this.detailPanels.clear();
        this.rowIds.clear();
        this.tableModel.setRowCount(0);
        List<Integer> detailRows = new ArrayList<>();
        for (Entry entry : this.entries) {
            boolean expanded = this.isExpanded(entry.id());
            Object[] row = new Object[entry.cells().length + 1];
            row[0] = expanded ? EXPANDED_SYMBOL : COLLAPSED_SYMBOL;
            System.arraycopy(entry.cells(), 0, row, 1, entry.cells().length);
            this.tableModel.addRow(row);
            this.rowIds.add(entry.id());
            if (expanded) {
                Object[] detailRow = new Object[row.length];
                detailRow[0] = new MaterialsDetail(entry.used(), entry.produced());
                this.tableModel.addRow(detailRow);
                this.rowIds.add(entry.id());
                detailRows.add(this.tableModel.getRowCount() - 1);
            }
        }
        for (int detailRow : detailRows) {
            MaterialsDetail detail = (MaterialsDetail) this.tableModel.getValueAt(detailRow, 0);
            int height = this.buildDetailPanel(detail).getPreferredSize().height + 8;
            this.setRowHeight(detailRow, Math.max(this.getRowHeight(), height));
        }
        for (int row = 0; row < this.rowIds.size(); row++) {
            if (!this.isDetailRow(row) && selectedIds.contains(this.rowIds.get(row))) {
                this.addRowSelectionInterval(row, row);
            }
        }
        if (position != null) {
            SwingUtilities.invokeLater(() -> this.restorePosition(position));
        }
    }

    private void restorePosition(Point position)
    {
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
        if (viewport == null) {
            return;
        }
        int maxY = Math.max(0, this.getPreferredSize().height - viewport.getExtentSize().height);
        viewport.setViewPosition(new Point(position.x, Math.min(position.y, maxY)));
    }

    private TableCellRenderer expanderRenderer()
    {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer()
        {
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
                Component component = super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    false,
                    row,
                    column
                );
                if (!isSelected) {
                    component.setBackground(TableStyle.getRowColor(row));
                }
                return component;
            }
        };
        renderer.setHorizontalAlignment(SwingConstants.CENTER);

        return renderer;
    }

    private JPanel buildDetailPanel(MaterialsDetail detail)
    {
        return this.detailPanels.computeIfAbsent(detail, key -> {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            boolean hasUsed = !detail.used().isEmpty();
            if (hasUsed) {
                panel.add(this.buildSection(this.usedHeader, detail.used()));
            }
            if (!detail.produced().isEmpty()) {
                if (hasUsed) {
                    panel.add(Box.createHorizontalStrut(8));
                    JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
                    separator.setMaximumSize(new Dimension(2, Integer.MAX_VALUE));
                    panel.add(separator);
                    panel.add(Box.createHorizontalStrut(8));
                }
                panel.add(this.buildSection(this.producedHeader, detail.produced()));
            }
            panel.add(Box.createHorizontalGlue());

            return panel;
        });
    }

    private JPanel buildSection(String header, List<MaterialTile> tiles)
    {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        JLabel headerLabel = new JLabel(header + ":");
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(headerLabel);
        JPanel tilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        tilePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tiles.forEach(tile -> tilePanel.add(this.buildTile(tile)));
        section.add(tilePanel);

        return section;
    }

    private JPanel buildTile(MaterialTile tile)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(this.centered(new JLabel(MaterialIconLoader.load(tile.item()))));
        panel.add(this.centered(new JLabel(tile.name())));
        panel.add(this.centered(new JLabel(tile.amount())));
        String tooltip = tile.name() + " (" + tile.amount() + ")";
        if (tile.source() != null && !tile.source().isBlank()) {
            JLabel source = new JLabel("[" + tile.source() + "]");
            source.setFont(source.getFont().deriveFont(source.getFont().getSize2D() - 2));
            source.setForeground(Color.GRAY);
            panel.add(this.centered(source));
            tooltip += " [" + tile.source() + "]";
        }
        panel.setToolTipText(tooltip);

        return panel;
    }

    private JLabel centered(JLabel label)
    {
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        return label;
    }

    private void layoutTree(Container container)
    {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container childContainer) {
                this.layoutTree(childContainer);
            }
        }
    }

    private void applyBackground(Container container, Color color)
    {
        container.setBackground(color);
        for (Component child : container.getComponents()) {
            if (child instanceof JPanel childPanel) {
                this.applyBackground(childPanel, color);
            }
        }
    }
}
