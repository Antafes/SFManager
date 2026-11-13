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

package antafes.sf.manager.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MigrationVersionTest
{
    @Test
    public void testComparesMajorMinorAndPatchIndependently()
    {
        assertEquals(-1, MigrationVersion.parse("1.9.9").compareTo(MigrationVersion.parse("2.0.0")));
        assertEquals(-1, MigrationVersion.parse("1.2.9").compareTo(MigrationVersion.parse("1.3.0")));
        assertEquals(-1, MigrationVersion.parse("1.2.3").compareTo(MigrationVersion.parse("1.2.4")));
    }

    @Test
    public void testAcceptsVersionsWithoutPatchAndDefaultsPatchToZero()
    {
        assertEquals(MigrationVersion.parse("1.0.0"), MigrationVersion.parse("1.0"));
        assertEquals("1.0.0", MigrationVersion.parse("1.0").toString());
    }

    @Test
    public void testRejectsMalformedVersions()
    {
        assertThrows(IllegalArgumentException.class, () -> MigrationVersion.parse("1"));
        assertThrows(IllegalArgumentException.class, () -> MigrationVersion.parse("1.0.0.0"));
        assertThrows(IllegalArgumentException.class, () -> MigrationVersion.parse("1.0.0-SNAPSHOT"));
        assertThrows(IllegalArgumentException.class, () -> MigrationVersion.parse("-1.0.0"));
    }
}
