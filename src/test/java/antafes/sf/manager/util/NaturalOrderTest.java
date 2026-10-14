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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NaturalOrderTest
{
    @Test
    public void testSortsNumbersNumerically()
    {
        List<String> values = new ArrayList<>(List.of("Group 10", "Group 2", "Group 1", "Group 21"));
        values.sort(NaturalOrder.COMPARATOR);

        assertEquals(List.of("Group 1", "Group 2", "Group 10", "Group 21"), values);
    }

    @Test
    public void testIgnoresCase()
    {
        List<String> values = new ArrayList<>(List.of("beta", "Alpha", "alpha 2", "Alpha 10"));
        values.sort(NaturalOrder.COMPARATOR);

        assertEquals(List.of("Alpha", "alpha 2", "Alpha 10", "beta"), values);
    }

    @Test
    public void testHandlesLeadingZerosAndHugeNumbers()
    {
        assertTrue(NaturalOrder.compare("a02", "a10") < 0);
        assertTrue(NaturalOrder.compare("a99999999999999999999999", "a100000000000000000000000") < 0);
        assertTrue(NaturalOrder.compare("a01", "a1") != 0);
    }

    @Test
    public void testSortsNullsLast()
    {
        List<String> values = new ArrayList<>();
        values.add(null);
        values.add("B");
        values.add("A");
        values.sort(NaturalOrder.COMPARATOR);

        assertEquals(java.util.Arrays.asList("A", "B", null), values);
    }
}
