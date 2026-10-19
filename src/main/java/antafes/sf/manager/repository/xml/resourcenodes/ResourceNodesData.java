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

package antafes.sf.manager.repository.xml.resourcenodes;

import jakarta.xml.bind.annotation.*;

import java.util.List;

@XmlRootElement(name = "resourceNodes")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceNodesData
{
    @XmlElement(name = "resourceNode")
    public List<ResourceNodeData> resourceNodes;

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ResourceNodeData
    {
        @XmlAttribute
        public String key;
        public String type;
        public String item;
        public String purity;
        public PositionData position;
        public String core;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PositionData
    {
        public double x;
        public double y;
        public double z;
    }
}
