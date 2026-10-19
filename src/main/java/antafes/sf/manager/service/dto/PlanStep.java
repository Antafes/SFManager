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

import antafes.sf.manager.entity.Building;
import antafes.sf.manager.entity.Item;
import antafes.sf.manager.entity.Recipe;

import java.util.List;

/**
 * One production step of a plan.
 *
 * @param item the item the step is planned for (the consumed by-product for side productions)
 * @param demand the rate of {@code item} the step has to provide (or consume for side productions)
 * @param clockSpeed the clock speed of the full machines
 * @param fullMachines the number of machines running with {@code clockSpeed}
 * @param partialClockSpeed the clock speed of one additional machine, or 0 if none is needed
 */
public record PlanStep(
    Item item,
    Recipe recipe,
    Building building,
    boolean sideProduction,
    double demand,
    double clockSpeed,
    int somersloops,
    int fullMachines,
    double partialClockSpeed,
    double powerConsumption,
    List<PlanMaterial> inputs,
    List<PlanMaterial> outputs
)
{
    public PlanStep
    {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }

    public int machineCount()
    {
        return this.fullMachines + (this.partialClockSpeed > 0 ? 1 : 0);
    }
}
