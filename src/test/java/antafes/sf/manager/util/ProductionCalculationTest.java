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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProductionCalculationTest
{
    @Test
    public void calculatesProductionWithFractionalSomersloopBoost()
    {
        assertEquals(67.5, ProductionCalculator.calculateProduction(30.0, 150.0, 1, 2), 0.0001);
    }

    @Test
    public void calculatesProductionWithoutSomersloopSlots()
    {
        assertEquals(30.0, ProductionCalculator.calculateProduction(30.0, 100.0, 0, 0), 0.0001);
    }

    @Test
    public void preservesRecipeRatesAtDefaultClockSpeed()
    {
        assertEquals(37.5, ProductionCalculator.calculateProduction(37.5, 100.0, 0, 2), 0.0001);
        assertEquals(7.5, ProductionCalculator.calculateProduction(7.5, 100.0, 0, 2), 0.0001);
    }

    @Test
    public void calculatesResourceProductionForEachPurity()
    {
        assertEquals(
            30.0,
            ProductionCalculator.calculateResourceProduction(60.0, Purity.IMPURE, 100.0, 0, 0),
            0.0001
        );
        assertEquals(
            60.0,
            ProductionCalculator.calculateResourceProduction(60.0, Purity.NORMAL, 100.0, 0, 0),
            0.0001
        );
        assertEquals(
            120.0,
            ProductionCalculator.calculateResourceProduction(60.0, Purity.PURE, 100.0, 0, 0),
            0.0001
        );
        assertEquals(
            270.0,
            ProductionCalculator.calculateResourceProduction(60.0, Purity.PURE, 150.0, 1, 2),
            0.0001
        );
    }

    @Test
    public void calculatesPowerConsumptionWithClockSpeedAndSomersloops()
    {
        assertEquals(9.0, PowerCalculator.calculateConsumption(4.0, 100.0, 1, 2), 0.0001);
    }

    @Test
    public void calculatesPowerConsumptionWithoutSomersloopSlots()
    {
        assertEquals(4.0, PowerCalculator.calculateConsumption(4.0, 100.0), 0.0001);
    }
}
