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

package antafes.sf.manager.service;

import antafes.sf.manager.BaseTest;
import antafes.sf.manager.entity.Factory;
import antafes.sf.manager.entity.Plan;
import antafes.sf.manager.repository.*;
import antafes.sf.manager.service.dto.PlanResult;
import antafes.sf.manager.service.dto.PlanStep;
import antafes.sf.manager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlanServiceTest extends BaseTest
{
    @Override
    protected void autowireTest(AnnotationConfigApplicationContext context)
    {
        super.autowireTest(context);
        context.getBean(BuildingRepository.class).init();
        context.getBean(ItemRepository.class).init();
        context.getBean(RecipeRepository.class).init();
        context.getBean(FactoryRepository.class).init();
        context.getBean(ProductionRepository.class).init();
        context.getBean(StorageRepository.class).init();
        context.getBean(PlanRepository.class).init();
    }

    @Test
    public void testCalculatesSimpleChain() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createPlanContext()) {
            this.autowireTest(context);
            PlanService planService = this.openFactory(context);
            Plan plan = planService.createPlan("Plates", "iron_plate", 40);

            PlanResult result = planService.calculate(plan);

            assertEquals(2, result.steps().size());
            PlanStep plateStep = result.steps().getFirst();
            assertEquals("iron_plate", plateStep.item().getKey());
            assertEquals(2, plateStep.machineCount());
            assertEquals(1, result.rawInputs().size());
            assertEquals("iron", result.rawInputs().getFirst().item().getKey());
            assertEquals(60, result.rawInputs().getFirst().rate(), 0.0001);
            assertTrue(result.totalPowerConsumption() > 0);
            assertTrue(result.warnings().isEmpty());
        }
    }

    @Test
    public void testOverclockReducesMachineCount() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createPlanContext()) {
            this.autowireTest(context);
            PlanService planService = this.openFactory(context);
            Plan plan = planService.createPlan("Plates", "iron_plate", 40);
            Plan.StepSettings settings = new Plan.StepSettings();
            settings.setClockSpeed(200.0);
            plan.getStepSettings().put("iron_plate", settings);

            PlanStep plateStep = planService.calculate(plan).steps().getFirst();

            assertEquals(1, plateStep.machineCount());
            assertEquals(200.0, plateStep.clockSpeed(), 0.0001);
        }
    }

    @Test
    public void testPlansArePersistedAndDeleted() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createPlanContext()) {
            this.autowireTest(context);
            PlanService planService = this.openFactory(context);
            Plan plan = planService.createPlan("Plates", "iron_plate", 40);
            plan.setTargetRate(60);
            planService.savePlan(plan);
            context.getBean(PlanRepository.class).init();

            assertEquals(1, planService.getPlans().size());
            assertEquals(60, planService.getPlans().getFirst().getTargetRate(), 0.0001);

            planService.deletePlan(plan.getId());
            assertTrue(planService.getPlans().isEmpty());
        }
    }

    private AnnotationConfigApplicationContext createPlanContext()
    {
        return this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            StorageRepository.class,
            FactoryRepository.class,
            PlanRepository.class,
            FactoryService.class,
            ProductionService.class,
            PlanCalculationService.class,
            PlanService.class
        );
    }

    private PlanService openFactory(AnnotationConfigApplicationContext context) throws IOException
    {
        Factory factory = context.getBean(FactoryService.class).createFactory("Plan Factory");
        context.publishEvent(new FactorySelectedEvent(factory));

        return context.getBean(PlanService.class);
    }
}
