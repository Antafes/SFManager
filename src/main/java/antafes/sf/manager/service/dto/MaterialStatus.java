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

import lombok.Getter;

@Getter
public enum MaterialStatus
{
    RED("materialStatusRed"),
    ORANGE("materialStatusOrange"),
    YELLOW("materialStatusYellow"),
    GREEN("materialStatusGreen"),
    GREY("materialStatusGrey");

    private static final double EPSILON = 0.000001;
    private static final double UNUSED_THRESHOLD = 0.05;

    private final String translationKey;

    MaterialStatus(String translationKey)
    {
        this.translationKey = translationKey;
    }

    public static MaterialStatus of(double production, double usage)
    {
        if (production <= EPSILON && usage <= EPSILON) {
            return GREY;
        }

        double unused = production - usage;

        if (unused < -EPSILON) {
            return RED;
        }

        if (unused <= EPSILON) {
            return ORANGE;
        }

        if (unused <= production * UNUSED_THRESHOLD + EPSILON) {
            return YELLOW;
        }

        return GREEN;
    }
}
