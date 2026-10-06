package antafes.sfmanager.gui.editor;

import antafes.sfmanager.service.ProductionService;
import antafes.utilities.language.LanguageInterface;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public class ProductionDeleteButtonEditor extends AbstractCellEditor
    implements TableCellEditor, TableCellRenderer
{
    private final ProductionService productionService;
    private final LanguageInterface language;
    private final Component parent;
    private final Runnable onDeleted;
    private final BooleanSupplier confirmation;
    private final JButton rendererButton = new JButton();
    private final JButton editorButton = new JButton();
    private UUID productionId;

    public ProductionDeleteButtonEditor(
        ProductionService productionService,
        LanguageInterface language,
        Component parent,
        Runnable onDeleted
    )
    {
        this(productionService, language, parent, onDeleted, () ->
            JOptionPane.showConfirmDialog(
                parent,
                language.translate("confirmDeleteProduction"),
                language.translate("deleteProduction"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            ) == JOptionPane.YES_OPTION
        );
    }

    public ProductionDeleteButtonEditor(
        ProductionService productionService,
        LanguageInterface language,
        Component parent,
        Runnable onDeleted,
        BooleanSupplier confirmation
    )
    {
        this.productionService = productionService;
        this.language = language;
        this.parent = parent;
        this.onDeleted = onDeleted;
        this.confirmation = confirmation;
        String deleteLabel = this.language.translate("delete");
        this.configureButton(this.rendererButton, deleteLabel);
        this.configureButton(this.editorButton, deleteLabel);
        this.editorButton.addActionListener(event -> this.deleteProduction());
    }

    @Override
    public Object getCellEditorValue()
    {
        return this.productionId;
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
        this.productionId = value instanceof UUID id ? id : null;
        this.editorButton.setEnabled(this.productionId != null);
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
        this.rendererButton.setEnabled(value instanceof UUID);
        return this.rendererButton;
    }

    private void configureButton(JButton button, String deleteLabel)
    {
        button.setIcon(new TrashCanIcon());
        button.setDisabledIcon(new TrashCanIcon());
        button.setToolTipText(deleteLabel);
        button.getAccessibleContext().setAccessibleName(deleteLabel);
    }

    private void deleteProduction()
    {
        boolean deleted = false;
        try {
            if (this.productionId != null && this.confirmation.getAsBoolean()) {
                this.productionService.deleteProduction(this.productionId);
                deleted = true;
            }
        } catch (IOException | RuntimeException exception) {
            JOptionPane.showMessageDialog(
                this.parent,
                Objects.toString(
                    exception.getMessage(),
                    this.language.translate("productionDeletionFailed")
                ),
                this.language.translate("deleteProduction"),
                JOptionPane.ERROR_MESSAGE
            );
        } finally {
            this.fireEditingStopped();
        }
        if (deleted) {
            this.onDeleted.run();
        }
    }

    private static final class TrashCanIcon implements Icon
    {
        @Override
        public int getIconWidth()
        {
            return 16;
        }

        @Override
        public int getIconHeight()
        {
            return 16;
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
                Color color = component.isEnabled()
                    ? component.getForeground()
                    : UIManager.getColor("Label.disabledForeground");
                graphics2D.setColor(color == null ? component.getForeground() : color);
                graphics2D.setStroke(new BasicStroke(1.5f));
                graphics2D.drawLine(x + 2, y + 4, x + 14, y + 4);
                graphics2D.drawLine(x + 5, y + 2, x + 11, y + 2);
                graphics2D.drawLine(x + 7, y + 2, x + 7, y + 1);
                graphics2D.drawLine(x + 9, y + 2, x + 9, y + 1);
                graphics2D.drawLine(x + 4, y + 6, x + 5, y + 14);
                graphics2D.drawLine(x + 12, y + 6, x + 11, y + 14);
                graphics2D.drawLine(x + 5, y + 14, x + 11, y + 14);
                graphics2D.drawLine(x + 7, y + 7, x + 7, y + 12);
                graphics2D.drawLine(x + 9, y + 7, x + 9, y + 12);
            } finally {
                graphics2D.dispose();
            }
        }
    }
}
