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

import antafes.sf.manager.entity.Item;

/**
 * A material flow of a plan in items per minute.
 *
 * @param available the surplus the active factory currently has for this item, or 0 if not applicable
 * @param targetByproduct whether the flow is a by-product of the target step, which is only reported
 */
public record PlanMaterial(Item item, double rate, double available, boolean targetByproduct)
{
    public PlanMaterial(Item item, double rate)
    {
        this(item, rate, 0, false);
    }
}
