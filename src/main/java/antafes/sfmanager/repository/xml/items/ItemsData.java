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

package antafes.sfmanager.repository.xml.items;

import jakarta.xml.bind.annotation.*;

import java.util.List;

@XmlRootElement(name = "items")
@XmlAccessorType(XmlAccessType.FIELD)
public class ItemsData
{
    @XmlElement(name = "item")
    public List<ItemData> items;

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ItemData
    {
        @XmlAttribute
        public String key;
        public NameData name;
        public String icon;

        @XmlElementWrapper(name = "productions")
        @XmlElement(name = "production")
        public List<ProductionData> productions;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class NameData
    {
        public String english;
        public String german;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProductionData
    {
        public String buildingKey;
        public int amount;
    }
}
