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

package antafes.sf.manager.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class Plan
{
    /** Recipe key marking that no side production should be created for a by-product. */
    public static final String NO_RECIPE = "none";

    private UUID id;
    private UUID factoryId;
    private String name;
    private String targetItemKey;
    private double targetRate;
    /** Item key to the key of the recipe producing it; missing entries use the default recipe. */
    private Map<String, String> recipeChoices = new LinkedHashMap<>();
    /** By-product item key to the key of the recipe consuming it, or {@link #NO_RECIPE}. */
    private Map<String, String> sideRecipeChoices = new LinkedHashMap<>();
    /** Item key to the machine settings of the step producing it. */
    private Map<String, StepSettings> stepSettings = new LinkedHashMap<>();

    @Data
    @Accessors(chain = true)
    public static class StepSettings
    {
        private double clockSpeed = 100.0;
        private int somersloops = 0;
    }
}
