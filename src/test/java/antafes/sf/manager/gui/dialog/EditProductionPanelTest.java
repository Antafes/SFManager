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

package antafes.sf.manager.gui.dialog;

import antafes.sf.manager.BaseTest;
import antafes.sf.manager.entity.*;
import antafes.sf.manager.gui.element.ResourcePositionSelector;
import antafes.sf.manager.repository.*;
import antafes.sf.manager.service.FactoryService;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditProductionPanelTest extends BaseTest
{
    @Override
    protected void autowireTest(AnnotationConfigApplicationContext context)
    {
        super.autowireTest(context);
        context.getBean(BuildingRepository.class).init();
        context.getBean(ItemRepository.class).init();
        context.getBean(RecipeRepository.class).init();
        context.getBean(ResourceNodeRepository.class).init();
        context.getBean(FactoryRepository.class).init();
        context.getBean(ProductionRepository.class).init();
    }

    @Test
    public void testUsesOnlyPositionsForProductionResource() throws Exception
    {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ResourceNodeRepository.class,
            FactoryRepository.class,
            ProductionRepository.class,
            FactoryService.class,
            ProductionService.class,
            EditProductionPanel.class
        )) {
            this.autowireTest(context);
            Factory factory = context.getBean(FactoryService.class).createFactory("Edit positions");
            context.publishEvent(new FactorySelectedEvent(factory));

            ResourceNodeRepository nodeRepository = context.getBean(ResourceNodeRepository.class);
            ResourceNode node = nodeRepository.findByItemAndType("iron", ResourceNode.Type.NODE).getFirst();
            ProductionService productionService = context.getBean(ProductionService.class);
            Production production = productionService.saveProduction(new Production()
                .setSelectedBuilding(context.getBean(BuildingRepository.class).findOne("miner_1"))
                .setSelectedItem(context.getBean(ItemRepository.class).findOne("iron"))
                .setGroup("Iron miners")
                .setPosition(node.getKey())
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));

            JDialog dialog = new JDialog();
            EditProductionPanel panel = context.getBean(
                EditProductionPanel.class,
                dialog,
                this.configuration,
                production.getId()
            );
            ResourcePositionSelector positions = this.field(panel, "resourcePositionSelect");
            assertTrue(positions.isVisible());
            assertEquals(node.getKey(), positions.getPosition());
            for (int index = 1; index < positions.getItemCount(); index++) {
                ResourceNode choice = (ResourceNode) positions.getItemAt(index);
                assertEquals("iron", choice.getItem().getKey());
                assertEquals(ResourceNode.Type.NODE, choice.getType());
            }
            dialog.dispose();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T field(Object target, String name) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }
}
