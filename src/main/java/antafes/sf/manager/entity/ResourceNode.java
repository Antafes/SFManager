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

package antafes.sf.manager.entity;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * A resource node, resource well satellite or geyser of the map. Positions are in meters.
 */
@Data
@Accessors(chain = true)
public class ResourceNode
{
    private String key;
    private Type type;
    private Item item;
    private Purity purity;
    private double x;
    private double y;
    private double z;
    private String core;

    public enum Type
    {
        NODE,
        WELL,
        GEYSER
    }
}
