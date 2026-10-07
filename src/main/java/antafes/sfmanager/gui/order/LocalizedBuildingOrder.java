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

package antafes.sfmanager.gui.order;

import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Name;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@UtilityClass
public class LocalizedBuildingOrder
{
    public Comparator<Building> comparator(String language)
    {
        Collator collator = Collator.getInstance(Locale.forLanguageTag(
            "German".equalsIgnoreCase(language) ? "de" : "en"
        ));
        collator.setStrength(Collator.SECONDARY);
        return Comparator.<Building, String>comparing(
            building -> LocalizedBuildingOrder.displayName(building, language),
            collator
        ).thenComparing(Building::getKey);
    }

    public void addTabs(
        JTabbedPane tabs,
        List<Building> buildings,
        String language,
        Function<Building, ? extends Component> panelFactory
    )
    {
        buildings.stream()
            .sorted(LocalizedBuildingOrder.comparator(language))
            .forEach(building -> tabs.addTab(
                LocalizedBuildingOrder.displayName(building, language),
                panelFactory.apply(building)
            ));
    }

    private String displayName(Building building, String language)
    {
        Name name = building.getName();
        return name == null ? building.getKey() : name.getLocalizedName(language);
    }
}
