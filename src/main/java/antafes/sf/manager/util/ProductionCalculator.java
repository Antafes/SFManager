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

package antafes.sf.manager.util;

import antafes.sf.manager.entity.Purity;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProductionCalculator
{
    public Double calculateProduction(Double baseProduction, Double clockSpeed, int usedSomersloops, int somersloopSlots)
    {
        double somersloopMultiplier = somersloopSlots <= 0
            ? 1
            : 1 + ((double) usedSomersloops / somersloopSlots);
        return baseProduction * somersloopMultiplier * (clockSpeed / 100);
    }

    public Double calculateResourceProduction(
        Double baseProduction,
        Purity purity,
        Double clockSpeed,
        int usedSomersloops,
        int somersloopSlots
    )
    {
        double purityMultiplier = switch (purity) {
            case IMPURE -> 0.5;
            case NORMAL -> 1.0;
            case PURE -> 2.0;
        };
        return calculateProduction(
            baseProduction * purityMultiplier,
            clockSpeed,
            usedSomersloops,
            somersloopSlots
        );
    }
}
