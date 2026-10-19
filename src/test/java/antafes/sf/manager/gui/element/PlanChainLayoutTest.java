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

package antafes.sf.manager.gui.element;

import antafes.sf.manager.BaseTest;
import antafes.sf.manager.entity.Factory;
import antafes.sf.manager.entity.Plan;
import antafes.sf.manager.repository.*;
import antafes.sf.manager.service.FactoryService;
import antafes.sf.manager.service.PlanCalculationService;
import antafes.sf.manager.service.PlanService;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.service.dto.PlanResult;
import antafes.sf.manager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlanChainLayoutTest extends BaseTest
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
    public void testPlacesRawMaterialsLeftOfProducers() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
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
        )) {
            this.autowireTest(context);
            Factory factory = context.getBean(FactoryService.class).createFactory("Layout Factory");
            context.publishEvent(new FactorySelectedEvent(factory));
            PlanService planService = context.getBean(PlanService.class);
            Plan plan = planService.createPlan("Plates", "iron_plate", 40);
            PlanResult result = planService.calculate(plan);

            PlanChainLayout.Layout layout = PlanChainLayout.compute(result);

            PlanChainLayout.Node raw = layout.nodes().stream()
                .filter(node -> node.kind() == PlanChainLayout.Kind.RAW)
                .findFirst()
                .orElseThrow();
            PlanChainLayout.Node ingot = layout.nodes().stream()
                .filter(node -> "step:iron_ingot".equals(node.id()))
                .findFirst()
                .orElseThrow();
            PlanChainLayout.Node plate = layout.nodes().stream()
                .filter(node -> "step:iron_plate".equals(node.id()))
                .findFirst()
                .orElseThrow();
            assertEquals(0, raw.column());
            assertEquals(1, ingot.column());
            assertEquals(2, plate.column());
            assertEquals(2, layout.edges().size());
            assertTrue(layout.edges().stream().noneMatch(PlanChainLayout.Edge::dashed));
        }
    }
}
