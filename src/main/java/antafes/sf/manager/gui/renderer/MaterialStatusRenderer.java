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

package antafes.sf.manager.gui.renderer;

import antafes.sf.manager.service.dto.MaterialStatus;
import antafes.utilities.language.LanguageInterface;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class MaterialStatusRenderer extends JComponent implements TableCellRenderer
{
    private static final int DOT_SIZE = 10;

    private final LanguageInterface language;
    private final String legend;
    private MaterialStatus status = MaterialStatus.GREY;
    private Color background = Color.WHITE;

    public MaterialStatusRenderer(LanguageInterface language)
    {
        this.language = language;
        this.legend = this.language.translate("materialStatusRed") + "<br>"
            + this.language.translate("materialStatusOrange") + "<br>"
            + this.language.translate("materialStatusYellow") + "<br>"
            + this.language.translate("materialStatusGreen") + "<br>"
            + this.language.translate("materialStatusGrey")
            + "</html>";
        this.setOpaque(true);
    }

    public static Color getColor(MaterialStatus status)
    {
        return switch (status) {
            case RED -> new Color(204, 0, 0);
            case ORANGE -> new Color(255, 140, 0);
            case YELLOW -> new Color(240, 200, 0);
            case GREEN -> new Color(40, 167, 69);
            case GREY -> Color.GRAY;
        };
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table,
        Object value,
        boolean isSelected,
        boolean hasFocus,
        int row,
        int column
    ) {
        this.status = value instanceof MaterialStatus materialStatus ? materialStatus : MaterialStatus.GREY;
        this.background = isSelected ? table.getSelectionBackground() : TableStyle.getRowColor(row);
        this.setToolTipText(
            "<html><b>" + this.language.translate(this.status.getTranslationKey()) + "</b><hr>" + this.legend
        );

        return this;
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setColor(this.background);
        g2.fillRect(0, 0, this.getWidth(), this.getHeight());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getColor(this.status));
        g2.fillOval(
            (this.getWidth() - DOT_SIZE) / 2,
            (this.getHeight() - DOT_SIZE) / 2,
            DOT_SIZE,
            DOT_SIZE
        );
        g2.dispose();
    }
}
