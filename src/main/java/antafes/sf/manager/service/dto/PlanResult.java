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

import java.util.List;

/**
 * The calculated production chain of a plan.
 *
 * @param steps the steps ordered from the target item down to the raw materials, side productions last
 * @param rawInputs the materials that have to be supplied from outside the chain
 * @param surplus the by-products that are left over, including the by-products of the target step
 * @param warnings the keys of the problems found while calculating, e.g. recipe cycles
 */
public record PlanResult(
    List<PlanStep> steps,
    List<PlanMaterial> rawInputs,
    List<PlanMaterial> surplus,
    double totalPowerConsumption,
    List<String> warnings
)
{
    public PlanResult
    {
        steps = List.copyOf(steps);
        rawInputs = List.copyOf(rawInputs);
        surplus = List.copyOf(surplus);
        warnings = List.copyOf(warnings);
    }
}
