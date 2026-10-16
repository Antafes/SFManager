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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PowerStatusTest
{
    @Test
    public void testNoRemainingPowerIsRed()
    {
        assertEquals(PowerStatus.RED, PowerStatus.of(1000, 0));
        assertEquals(PowerStatus.RED, PowerStatus.of(1000, -10));
        assertEquals(PowerStatus.RED, PowerStatus.of(0, 0));
    }

    @Test
    public void testRemainingUpToFivePercentIsYellow()
    {
        assertEquals(PowerStatus.YELLOW, PowerStatus.of(1000, 0.1));
        assertEquals(PowerStatus.YELLOW, PowerStatus.of(1000, 50));
    }

    @Test
    public void testRemainingAboveFivePercentIsGreen()
    {
        assertEquals(PowerStatus.GREEN, PowerStatus.of(1000, 50.1));
        assertEquals(PowerStatus.GREEN, PowerStatus.of(1000, 1000));
    }
}
