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

package antafes.sf.manager.gui;

import antafes.sf.manager.BaseTest;
import antafes.sf.manager.entity.*;
import antafes.sf.manager.entity.item.Production;
import antafes.sf.manager.gui.order.LocalizedItemOrder;
import antafes.sf.manager.repository.*;
import antafes.sf.manager.service.FactoryService;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.service.dto.PowerStatus;
import antafes.sf.manager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class FactoryOverviewPanelTest extends BaseTest
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
    }

    @Test
    public void testDisplaysOverviewAndRefreshesForSelectedFactory() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            ProductionService.class,
            FactoryOverviewPanel.class
        )) {
            this.autowireTest(context);
            ProductionService productionService = context.getBean(ProductionService.class);
            FactoryOverviewPanel panel = context.getBean(FactoryOverviewPanel.class);
            JTable materialTable = panel.getMaterialTable();
            JTable powerSourceTable = panel.getPowerSourceTable();

            assertEquals(6, materialTable.getColumnCount());
            assertEquals("Material", materialTable.getColumnName(0));
            assertEquals("Production", materialTable.getColumnName(1));
            assertEquals("Usage", materialTable.getColumnName(2));
            assertEquals("Unused", materialTable.getColumnName(3));
            assertEquals("Consumption", materialTable.getColumnName(4));
            assertEquals("Status", materialTable.getColumnName(5));
            assertEquals(0, materialTable.getRowCount());

            Factory factory = context.getBean(FactoryService.class).createFactory("Overview Panel Factory");
            context.publishEvent(new FactorySelectedEvent(factory));
            Building miner = productionService.getBuildings().stream()
                .filter(building -> "miner_1".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Item iron = productionService.getItemsForBuilding(miner.getKey()).stream()
                .filter(item -> "iron".equals(item.getKey()))
                .findFirst()
                .orElseThrow();
            productionService.saveProductions(new antafes.sf.manager.entity.Production()
                .setSelectedBuilding(miner)
                .setSelectedItem(iron)
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0), 2);

            Building smelter = productionService.getBuildings().stream()
                .filter(building -> "smelter".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Recipe ironIngot = productionService.getRecipesForBuilding(smelter.getKey()).stream()
                .filter(recipe -> "iron_ingot".equals(recipe.getKey()))
                .findFirst()
                .orElseThrow();
            productionService.saveProduction(new antafes.sf.manager.entity.Production()
                .setSelectedBuilding(smelter)
                .setSelectedRecipe(ironIngot)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));

            Building generator = productionService.getBuildings().stream()
                .filter(building -> "fuel_generator".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            productionService.saveProductions(new antafes.sf.manager.entity.Production()
                .setSelectedBuilding(generator)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0), 2);
            Building coalGenerator = productionService.getBuildings().stream()
                .filter(building -> "coal_generator".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            productionService.saveProduction(new antafes.sf.manager.entity.Production()
                .setSelectedBuilding(coalGenerator)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));

            panel.refresh();

            assertEquals(2, materialTable.getRowCount());
            assertEquals("Iron Ore", materialTable.getValueAt(0, 0));
            assertEquals("120 / min", materialTable.getValueAt(0, 1));
            assertEquals("0 / min", materialTable.getValueAt(0, 2));
            assertEquals("120 / min", materialTable.getValueAt(0, 3));
            assertEquals("10 MW", materialTable.getValueAt(0, 4));
            assertEquals(antafes.sf.manager.service.dto.MaterialStatus.GREEN, materialTable.getValueAt(0, 5));
            assertEquals("Iron Ingot", materialTable.getValueAt(1, 0));
            assertEquals(2, powerSourceTable.getRowCount());
            assertEquals("Coal-Powered Generator", powerSourceTable.getValueAt(0, 0));
            assertEquals("75 MW", powerSourceTable.getValueAt(0, 1));
            assertEquals("Fuel-Powered Generator", powerSourceTable.getValueAt(1, 0));
            assertEquals("500 MW", powerSourceTable.getValueAt(1, 1));
            assertEquals("575 MW", panel.getTotalPowerProductionLabel().getText());
            assertEquals(PowerStatus.GREEN, panel.getPowerSignalLight().getStatus());

            Factory emptyFactory = context.getBean(FactoryService.class).createFactory("Empty Overview Factory");
            context.publishEvent(new FactorySelectedEvent(emptyFactory));
            panel.refresh();
            assertEquals(0, materialTable.getRowCount());
            assertEquals(0, powerSourceTable.getRowCount());
            assertFalse(factory.getProductions().isEmpty());
        }
    }

    @Test
    public void testSortsRawItemsBeforeNonRawItemsByLocalizedName()
    {
        Item ironOre = new Item()
            .setKey("iron")
            .setName(new Name().setEnglish("Zinc Ore"))
            .setProductions(List.of(new Production()));
        Item ingot = new Item()
            .setKey("iron_ingot")
            .setName(new Name().setEnglish("Aluminum Ingot"));
        Item copperOre = new Item()
            .setKey("copper")
            .setName(new Name().setEnglish("Copper Ore"))
            .setProductions(List.of(new Production()));

        List<Item> sortedItems = Stream.of(ironOre, ingot, copperOre)
            .sorted(LocalizedItemOrder.comparator("English"))
            .toList();

        assertTrue(LocalizedItemOrder.isRawResource(sortedItems.getFirst()));
        assertEquals("copper", sortedItems.getFirst().getKey());
        assertEquals("iron", sortedItems.get(1).getKey());
        assertFalse(LocalizedItemOrder.isRawResource(sortedItems.get(2)));
        assertEquals("iron_ingot", sortedItems.get(2).getKey());
    }

    @Test
    public void testKeepsOverviewFirstAndSortsResourceTabsBeforeOtherItems()
    {
        Item zincOre = new Item()
            .setKey("zinc")
            .setName(new Name().setEnglish("Zinc Ore"))
            .setProductions(List.of(new Production()));
        Item aluminumIngot = new Item()
            .setKey("aluminum_ingot")
            .setName(new Name().setEnglish("Aluminum Ingot"));
        Item copperOre = new Item()
            .setKey("copper")
            .setName(new Name().setEnglish("Copper Ore"))
            .setProductions(List.of(new Production()));
        JTabbedPane tabs = new JTabbedPane();

        LocalizedItemOrder.addTabs(
            tabs,
            "Overview",
            new JPanel(),
            List.of(zincOre, aluminumIngot, copperOre),
            "English",
            item -> item.getName().getEnglish(),
            item -> new JPanel()
        );

        assertEquals("Overview", tabs.getTitleAt(0));
        assertEquals("Copper Ore", tabs.getTitleAt(1));
        assertEquals("Zinc Ore", tabs.getTitleAt(2));
        assertEquals("Aluminum Ingot", tabs.getTitleAt(3));
    }
}
