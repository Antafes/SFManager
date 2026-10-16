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

public class MaterialStatusTest
{
    @Test
    public void testUsageExceedsProductionIsRed()
    {
        assertEquals(MaterialStatus.RED, MaterialStatus.of(100, 100.5));
        assertEquals(MaterialStatus.RED, MaterialStatus.of(0, 10));
    }

    @Test
    public void testUsageEqualsProductionIsOrange()
    {
        assertEquals(MaterialStatus.ORANGE, MaterialStatus.of(100, 100));
    }

    @Test
    public void testUnusedUpToFivePercentIsYellow()
    {
        assertEquals(MaterialStatus.YELLOW, MaterialStatus.of(100, 99));
        assertEquals(MaterialStatus.YELLOW, MaterialStatus.of(100, 95));
    }

    @Test
    public void testUnusedAboveFivePercentIsGreen()
    {
        assertEquals(MaterialStatus.GREEN, MaterialStatus.of(100, 94.9));
        assertEquals(MaterialStatus.GREEN, MaterialStatus.of(100, 0));
    }

    @Test
    public void testNoProductionAndNoUsageIsGrey()
    {
        assertEquals(MaterialStatus.GREY, MaterialStatus.of(0, 0));
    }
}
