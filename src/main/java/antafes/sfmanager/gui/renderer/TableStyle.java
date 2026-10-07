package antafes.sfmanager.gui.renderer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public final class TableStyle
{
    private static final Color ALTERNATE_ROW_COLOR = new Color(242, 242, 242);

    private TableStyle()
    {
    }

    public static void apply(JTable table)
    {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
            ) {
                Component component = super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column
                );
                if (!isSelected) {
                    component.setBackground(getRowColor(row));
                }
                return component;
            }
        });
    }

    public static Color getRowColor(int row)
    {
        return row % 2 == 0 ? Color.WHITE : ALTERNATE_ROW_COLOR;
    }
}
