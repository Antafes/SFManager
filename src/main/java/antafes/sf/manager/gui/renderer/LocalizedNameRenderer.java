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

import antafes.sf.manager.entity.Name;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class LocalizedNameRenderer<T> implements ListCellRenderer<T>
{
    private final Function<? super T, Name> nameExtractor;
    private final String language;
    private final DefaultListCellRenderer renderer = new DefaultListCellRenderer();

    public LocalizedNameRenderer(Function<? super T, Name> nameExtractor, String language)
    {
        this.nameExtractor = nameExtractor;
        this.language = language;
    }

    public String getDisplayName(T value)
    {
        if (value == null) {
            return "";
        }
        Name name = this.nameExtractor.apply(value);
        return name == null ? "" : name.getLocalizedName(this.language);
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends T> list,
        T value,
        int index,
        boolean isSelected,
        boolean cellHasFocus
    ) {
        JLabel label = (JLabel) this.renderer.getListCellRendererComponent(
            list, value, index, isSelected, cellHasFocus
        );
        label.setText(this.getDisplayName(value));
        return label;
    }
}
