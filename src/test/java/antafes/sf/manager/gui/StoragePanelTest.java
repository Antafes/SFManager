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

import antafes.sf.manager.entity.*;
import antafes.sf.manager.repository.*;
import antafes.sf.manager.service.dto.StorageItem;
import antafes.sf.manager.BaseTest;
import antafes.sf.manager.service.FactoryService;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.service.StorageService;
import antafes.sf.manager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.swing.*;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StoragePanelTest extends BaseTest
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
    }

    @Test
    public void testDisplaysStoredItemsAndLocalizedSourceProductionContext() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            StorageRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            ProductionService.class,
            StorageService.class,
            StoragePanel.class
        )) {
            this.autowireTest(context);
            ProductionService productionService = context.getBean(ProductionService.class);
            StorageService storageService = context.getBean(StorageService.class);
            Factory factory = context.getBean(FactoryService.class).createFactory("Storage Panel Factory");
            context.publishEvent(new FactorySelectedEvent(factory));
            Building smelter = productionService.getBuildings().stream()
                .filter(building -> "smelter".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Recipe recipe = productionService.getRecipesForBuilding(smelter.getKey()).stream()
                .filter(candidate -> "iron_ingot".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();
            Production production = productionService.saveProduction(new Production()
                .setSelectedBuilding(smelter)
                .setSelectedRecipe(recipe)
                .setGroup("Ingot Line")
                .setPosition("A1")
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));
            storageService.addStorageEntries(List.of(new Storage.Entry()
                .setProductionId(production.getId())
                .setItemKey(recipe.getProducts().getFirst().getResource().getKey())));

            StoragePanel panel = context.getBean(StoragePanel.class);
            panel.refresh();
            JTable table = panel.getTable();

            assertEquals(4, table.getColumnCount());
            assertEquals("Item", table.getColumnName(0));
            assertEquals("Stored / min", table.getColumnName(1));
            assertEquals("Source productions", table.getColumnName(2));
            assertEquals("", table.getColumnName(3));
            assertEquals(40, table.getColumnModel().getColumn(3).getPreferredWidth());
            assertEquals(1, table.getRowCount());
            assertFalse(table.isCellEditable(0, 0));
            assertTrue(table.isCellEditable(0, 3));
            assertTrue(table.getAutoCreateRowSorter());
            assertEquals("Iron Ingot", table.getValueAt(0, 0));
            assertEquals("30 / min", table.getValueAt(0, 1));
            assertTrue(table.getValueAt(0, 2).toString().contains("Smelter"));
            assertTrue(table.getValueAt(0, 2).toString().contains("Iron Ingot"));
            assertTrue(table.getValueAt(0, 2).toString().contains("Ingot Line"));
            assertTrue(table.getValueAt(0, 2).toString().contains("A1"));
            assertFalse(table.getValueAt(0, 2).toString().contains("#"));
            assertTrue(table.getValueAt(0, 3) instanceof StorageItem);
        }
    }
}
