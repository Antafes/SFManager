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

package antafes.sf.manager.repository.xml.productions;

import jakarta.xml.bind.annotation.*;

import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "production",
    propOrder = {
        "selectedBuilding",
        "selectedRecipe",
        "selectedItem",
        "materialSources",
        "group",
        "position",
        "amount",
        "purity",
        "pressurizerId",
        "clockSpeed",
        "usedSomersloops"
    }
)
public class ProductionData
{
    @XmlAttribute(required = true)
    public String id;
    @XmlElement(required = true)
    public String selectedBuilding;
    public String selectedRecipe;
    public String selectedItem;
    @XmlElementWrapper(name = "materialSources")
    @XmlElement(name = "materialSource")
    public List<MaterialSourceData> materialSources;
    public String group;
    public String position;
    // Read only for migration of older production files; new productions are individual records.
    public Double amount;
    public String purity;
    public String pressurizerId;
    public Double clockSpeed;
    public int usedSomersloops;

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MaterialSourceData
    {
        @XmlAttribute(required = true)
        public String itemKey;
        @XmlAttribute(required = true)
        public String group;
    }
}
