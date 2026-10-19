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

package antafes.sf.manager.repository.xml.plans;

import jakarta.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "plans")
@XmlAccessorType(XmlAccessType.FIELD)
public class PlansData
{
    public String factoryId;

    @XmlElement(name = "plan")
    public List<PlanData> plans = new ArrayList<>();

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PlanData
    {
        public String id;
        public String name;
        public String targetItemKey;
        public double targetRate;

        @XmlElementWrapper(name = "recipeChoices")
        @XmlElement(name = "choice")
        public List<ChoiceData> recipeChoices = new ArrayList<>();

        @XmlElementWrapper(name = "sideRecipeChoices")
        @XmlElement(name = "choice")
        public List<ChoiceData> sideRecipeChoices = new ArrayList<>();

        @XmlElementWrapper(name = "stepSettings")
        @XmlElement(name = "step")
        public List<StepSettingsData> stepSettings = new ArrayList<>();
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ChoiceData
    {
        public String itemKey;
        public String recipeKey;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class StepSettingsData
    {
        public String itemKey;
        public double clockSpeed;
        public int somersloops;
    }
}
