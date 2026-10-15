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

package antafes.sf.manager.service.dto;

import antafes.sf.manager.entity.ProductionMaterialSource;

import java.util.List;

/**
 * Changes applied to several productions at once. A null value leaves the property unchanged.
 * An empty position clears the position. Material sources can only be changed if all productions
 * use the same recipe.
 */
public record BulkProductionEdit(
    String group,
    String position,
    Double clockSpeed,
    Integer usedSomersloops,
    List<ProductionMaterialSource> materialSources
)
{
    public BulkProductionEdit(String group, String position, Double clockSpeed, Integer usedSomersloops)
    {
        this(group, position, clockSpeed, usedSomersloops, null);
    }

    public boolean isEmpty()
    {
        return this.group == null && this.position == null && this.clockSpeed == null
            && this.usedSomersloops == null && this.materialSources == null;
    }
}
