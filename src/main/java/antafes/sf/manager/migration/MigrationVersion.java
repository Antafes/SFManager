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

import java.util.Arrays;

public record MigrationVersion(int major, int minor, int patch) implements Comparable<MigrationVersion>
{
    public MigrationVersion
    {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Migration version components must not be negative.");
        }
    }

    public static MigrationVersion parse(String value)
    {
        if (value == null || !value.matches("\\d+\\.\\d+(?:\\.\\d+)?")) {
            throw new IllegalArgumentException("Migration version must use major.minor[.patch] format: " + value);
        }
        try {
            int[] components = Arrays.stream(value.split("\\."))
                .mapToInt(Integer::parseInt)
                .toArray();
            return new MigrationVersion(components[0], components[1], components.length == 3 ? components[2] : 0);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Migration version component is too large: " + value, exception);
        }
    }

    @Override
    public int compareTo(MigrationVersion other)
    {
        int majorComparison = Integer.compare(this.major, other.major);
        if (majorComparison != 0) {
            return majorComparison;
        }
        int minorComparison = Integer.compare(this.minor, other.minor);
        return minorComparison != 0 ? minorComparison : Integer.compare(this.patch, other.patch);
    }

    @Override
    public String toString()
    {
        return this.major + "." + this.minor + "." + this.patch;
    }
}
