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
import antafes.sf.manager.gui.editor.ProductionDeleteButtonEditor;
import antafes.sf.manager.gui.order.LocalizedItemOrder;
import antafes.sf.manager.repository.*;
import antafes.sf.manager.service.FactoryService;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class RecipeProductionPanelTest extends BaseTest
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
    public void testDisplaysScaledRecipeMaterialsAndFactoryConsumption() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            ProductionService.class,
            RecipeProductionPanel.class
        )) {
            this.autowireTest(context);
            ProductionService productionService = context.getBean(ProductionService.class);
            Factory factory = context.getBean(FactoryService.class).createFactory("Recipe Production Panel");
            context.publishEvent(new FactorySelectedEvent(factory));

            Building smelter = this.findBuilding(productionService, "smelter");
            Recipe ironIngot = productionService.getRecipesForBuilding(smelter.getKey()).stream()
                .filter(recipe -> "iron_ingot".equals(recipe.getKey()))
                .findFirst()
                .orElseThrow();
            productionService.saveProduction(new antafes.sf.manager.entity.Production()
                .setSelectedBuilding(smelter)
                .setSelectedRecipe(ironIngot)
                .setGroup("Smelting")
                .setClockSpeed(100.0)
                .setUsedSomersloops(1));

            Building foundry = this.findBuilding(productionService, "foundry");
            Recipe basicIronIngot = productionService.getRecipesForBuilding(foundry.getKey()).stream()
                .filter(recipe -> "basic_iron_ingot".equals(recipe.getKey()))
                .findFirst()
                .orElseThrow();
            productionService.saveProduction(new antafes.sf.manager.entity.Production()
                .setSelectedBuilding(foundry)
                .setSelectedRecipe(basicIronIngot)
                .setClockSpeed(100.0)
                .setUsedSomersloops(1));

            Building miner = this.findBuilding(productionService, "miner_1");
            Item ironOre = productionService.getResourceItems().stream()
                .filter(item -> "iron".equals(item.getKey()))
                .findFirst()
                .orElseThrow();
            productionService.saveProduction(new antafes.sf.manager.entity.Production()
                .setSelectedBuilding(miner)
                .setSelectedItem(ironOre)
                .setClockSpeed(100.0)
                .setPurity(Purity.NORMAL)
                .setUsedSomersloops(0));

            Building generator = this.findBuilding(productionService, "coal_generator");
            productionService.saveProduction(new antafes.sf.manager.entity.Production()
                .setSelectedBuilding(generator)
                .setSelectedRecipe(productionService.getRecipesForBuilding(generator.getKey()).stream()
                    .filter(recipe -> "coal_generator_coal".equals(recipe.getKey()))
                    .findFirst()
                    .orElseThrow())
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));

            assertEquals(1, productionService.getRecipeItemsWithProductions().size());
            Item ironIngotItem = ironIngot.getProducts().getFirst().getResource();
            assertEquals("iron_ingot", productionService.getRecipeItemsWithProductions().getFirst().getKey());
            assertEquals(2, productionService.getRecipeProductions(ironIngotItem).size());

            RecipeProductionPanel panel = context.getBean(RecipeProductionPanel.class, ironIngotItem);
            JTable table = panel.getTable();
            assertEquals(2, table.getRowCount());
            assertEquals(11, table.getColumnCount());
            assertEquals("Building", table.getColumnName(0));
            assertEquals("Recipe", table.getColumnName(1));
            assertEquals("Group", table.getColumnName(2));
            assertEquals("Used Materials", table.getColumnName(3));
            assertEquals("Clock speed", table.getColumnName(4));
            assertEquals("Somersloops", table.getColumnName(5));
            assertEquals("Produced Materials", table.getColumnName(6));
            assertEquals("Usage", table.getColumnName(7));
            assertEquals("Consumption", table.getColumnName(8));
            assertEquals("", table.getColumnName(9));
            assertEquals("", table.getColumnName(10));
            assertEquals(40, table.getColumnModel().getColumn(9).getPreferredWidth());
            assertEquals(40, table.getColumnModel().getColumn(10).getPreferredWidth());
            assertFalse(table.isCellEditable(0, 7));
            assertFalse(table.isCellEditable(0, 8));
            assertTrue(table.isCellEditable(0, 9));
            assertTrue(table.isCellEditable(0, 10));
            assertEquals("Smelter", table.getValueAt(0, 0));
            assertEquals("Iron Ingot", table.getValueAt(0, 1));
            assertEquals("Smelting", table.getValueAt(0, 2));
            assertEquals("Iron Ore (60)", table.getValueAt(0, 3));
            assertEquals("100%", table.getValueAt(0, 4));
            assertEquals(1, table.getValueAt(0, 5));
            assertEquals("Iron Ingot (60)", table.getValueAt(0, 6));
            assertEquals("0 / min", table.getValueAt(0, 7));
            assertEquals("16 MW", table.getValueAt(0, 8));
            assertInstanceOf(java.util.UUID.class, table.getValueAt(0, 9));
            assertInstanceOf(java.util.UUID.class, table.getValueAt(0, 10));
            assertEquals("Foundry", table.getValueAt(1, 0));
            assertEquals("Basic Iron Ingot", table.getValueAt(1, 1));
            assertNull(table.getValueAt(1, 2));
            assertEquals("Iron Ore (50), Limestone (80)", table.getValueAt(1, 3));
            assertEquals("Iron Ingot (100)", table.getValueAt(1, 6));
            assertEquals("64 MW", table.getValueAt(1, 8));

            Factory emptyFactory = context.getBean(FactoryService.class).createFactory("Empty Recipe Factory");
            context.publishEvent(new FactorySelectedEvent(emptyFactory));
            panel.refresh();
            assertEquals(0, table.getRowCount());
            assertTrue(productionService.getRecipeItemsWithProductions().isEmpty());
        }
    }

    @Test
    public void testSortsRecipeItemsAlphabeticallyByLocalizedName()
    {
        Item zinc = new Item()
            .setKey("zinc")
            .setName(new Name().setEnglish("Zinc Ingot"));
        Item aluminum = new Item()
            .setKey("aluminum")
            .setName(new Name().setEnglish("Aluminum Ingot"))
            .setProductions(List.of(new Production()));

        List<Item> sorted = Stream.of(zinc, aluminum)
            .sorted(LocalizedItemOrder.alphabeticalComparator("English"))
            .toList();

        assertEquals("aluminum", sorted.getFirst().getKey());
        assertEquals("zinc", sorted.get(1).getKey());
    }

    @Test
    public void testDeleteButtonSupportsCancelAndDeletesConfirmedProduction() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            ProductionService.class,
            RecipeProductionPanel.class
        )) {
            this.autowireTest(context);
            ProductionService productionService = context.getBean(ProductionService.class);
            Factory factory = context.getBean(FactoryService.class).createFactory("Delete From Recipe Panel");
            context.publishEvent(new FactorySelectedEvent(factory));
            Building smelter = this.findBuilding(productionService, "smelter");
            Recipe recipe = productionService.getRecipesForBuilding(smelter.getKey()).stream()
                .filter(candidate -> "iron_ingot".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();
            antafes.sf.manager.entity.Production production = productionService.saveProduction(new antafes.sf.manager.entity.Production()
                .setSelectedBuilding(smelter)
                .setSelectedRecipe(recipe)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));
            Item outputItem = recipe.getProducts().getFirst().getResource();
            RecipeProductionPanel panel = context.getBean(RecipeProductionPanel.class, outputItem);
            JTable table = panel.getTable();
            AtomicInteger refreshCount = new AtomicInteger();
            ProductionDeleteButtonEditor cancelEditor = new ProductionDeleteButtonEditor(
                productionService,
                this.configuration.getLanguageObject(),
                panel,
                refreshCount::incrementAndGet,
                () -> false
            );
            JButton cancelButton = (JButton) cancelEditor.getTableCellEditorComponent(
                table,
                production.getId(),
                false,
                0,
                10
            );
            cancelButton.doClick();
            assertEquals(1, productionService.getRecipeProductions(outputItem).size());
            assertEquals(1, table.getRowCount());
            assertEquals(0, refreshCount.get());

            ProductionDeleteButtonEditor confirmEditor = new ProductionDeleteButtonEditor(
                productionService,
                this.configuration.getLanguageObject(),
                panel,
                panel::refresh,
                () -> true
            );
            JButton deleteButton = (JButton) confirmEditor.getTableCellEditorComponent(
                table,
                production.getId(),
                false,
                0,
                10
            );
            assertEquals("", deleteButton.getText());
            assertNotNull(deleteButton.getIcon());
            assertEquals("Delete", deleteButton.getToolTipText());
            assertEquals("Delete", deleteButton.getAccessibleContext().getAccessibleName());
            deleteButton.doClick();
            assertNull(productionService.getRecipeProductions(outputItem).stream()
                .filter(entry -> production.getId().equals(entry.production().getId()))
                .findFirst()
                .orElse(null));
            assertFalse(factory.getProductions().contains(production.getId().toString()));
            assertEquals(0, table.getRowCount());
        }
    }

    private Building findBuilding(ProductionService productionService, String key)
    {
        return productionService.getBuildings().stream()
            .filter(building -> key.equals(building.getKey()))
            .findFirst()
            .orElseThrow();
    }
}
