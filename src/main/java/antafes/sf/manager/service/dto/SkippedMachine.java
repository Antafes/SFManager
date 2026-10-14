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

/**
 * A machine of the save that was not imported.
 */
public record SkippedMachine(String instanceName, String className, Reason reason)
{
    public enum Reason
    {
        /** The save does not store the resource type or purity of the extractor's node. */
        RESOURCE_UNKNOWN,
        /** The recipe or fuel of the machine has no SFManager counterpart. */
        RECIPE_UNMAPPED,
        /** The machine has no recipe or fuel set. */
        NO_RECIPE,
        /** The clock speed is outside of the supported range. */
        INVALID_CLOCK_SPEED
    }
}
