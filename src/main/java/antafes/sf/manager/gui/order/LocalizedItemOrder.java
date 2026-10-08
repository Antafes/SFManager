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

package antafes.sf.manager.gui.order;

import antafes.sf.manager.entity.Item;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

@UtilityClass
public class LocalizedItemOrder
{
    public Comparator<Item> comparator(String language)
    {
        Comparator<Item> alphabeticalComparator = LocalizedItemOrder.alphabeticalComparator(language);
        return (left, right) -> {
            int rawResourceOrder = Boolean.compare(isRawResource(right), isRawResource(left));
            return rawResourceOrder != 0 ? rawResourceOrder : alphabeticalComparator.compare(left, right);
        };
    }

    public Comparator<Item> alphabeticalComparator(String language)
    {
        Collator collator = Collator.getInstance(Locale.forLanguageTag(
            "German".equalsIgnoreCase(language) ? "de" : "en"
        ));
        collator.setStrength(Collator.SECONDARY);
        return Comparator.<Item, String>comparing(
            item -> LocalizedItemOrder.displayName(item, language),
            collator
        ).thenComparing(item -> Objects.toString(item.getKey(), ""));
    }

    public boolean isRawResource(Item item)
    {
        return item.getProductions() != null && !item.getProductions().isEmpty();
    }

    public void addTabs(
        JTabbedPane tabs,
        String overviewTitle,
        Component overviewPanel,
        List<Item> items,
        String language,
        Function<Item, String> titleProvider,
        Function<Item, Component> panelProvider
    )
    {
        tabs.addTab(overviewTitle, overviewPanel);
        items.stream()
            .sorted(comparator(language))
            .forEach(item -> tabs.addTab(titleProvider.apply(item), panelProvider.apply(item)));
    }

    private String displayName(Item item, String language)
    {
        return item.getName() == null
            ? Objects.toString(item.getKey(), "")
            : item.getName().getLocalizedName(language);
    }
}
