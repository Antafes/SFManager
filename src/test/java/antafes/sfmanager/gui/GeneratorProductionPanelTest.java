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

package antafes.sfmanager.gui;

import antafes.sfmanager.BaseTest;
import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.entity.Name;
import antafes.sfmanager.entity.Production;
import antafes.sfmanager.gui.editor.ProductionDeleteButtonEditor;
import antafes.sfmanager.gui.order.LocalizedBuildingOrder;
import antafes.sfmanager.repository.*;
import antafes.sfmanager.service.FactoryService;
import antafes.sfmanager.service.ProductionService;
import antafes.sfmanager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.swing.*;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeneratorProductionPanelTest extends BaseTest
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
    public void testDisplaysScaledGeneratorProductionAndUsedMaterials() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            ProductionService.class,
            GeneratorProductionPanel.class
        )) {
            this.autowireTest(context);
            ProductionService productionService = context.getBean(ProductionService.class);
            Factory factory = context.getBean(FactoryService.class).createFactory("Generator Panel");
            context.publishEvent(new FactorySelectedEvent(factory));

            Building coalGenerator = this.findBuilding(productionService, "coal_generator");
            productionService.saveProduction(new Production()
                .setSelectedBuilding(coalGenerator)
                .setSelectedRecipe(productionService.getRecipesForBuilding(coalGenerator.getKey()).stream()
                    .filter(recipe -> "coal_generator_coal".equals(recipe.getKey()))
                    .findFirst()
                    .orElseThrow())
                .setPosition("C1")
                .setGroup("Coal Group")
                .setClockSpeed(150.0)
                .setUsedSomersloops(0));

            Building fuelGenerator = this.findBuilding(productionService, "fuel_generator");
            productionService.saveProduction(new Production()
                .setSelectedBuilding(fuelGenerator)
                .setSelectedRecipe(productionService.getRecipesForBuilding(fuelGenerator.getKey()).stream()
                    .filter(recipe -> "fuel_generator_fuel".equals(recipe.getKey()))
                    .findFirst()
                    .orElseThrow())
                .setPosition("F1")
                .setGroup("Fuel Group")
                .setClockSpeed(125.0)
                .setUsedSomersloops(0));

            Building geothermalGenerator = this.findBuilding(productionService, "geothermal_generator");
            productionService.saveProduction(new Production()
                .setSelectedBuilding(geothermalGenerator)
                .setPosition("G1")
                .setGroup("Geothermal Group")
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));

            assertEquals(3, productionService.getGeneratorBuildingsWithProductions().size());
            GeneratorProductionPanel panel = context.getBean(GeneratorProductionPanel.class, coalGenerator);
            JTable table = panel.getTable();
            assertEquals(1, table.getRowCount());
            assertEquals(7, table.getColumnCount());
            assertEquals("Position", table.getColumnName(0));
            assertEquals("Group", table.getColumnName(1));
            assertEquals("Clock speed", table.getColumnName(2));
            assertEquals("Production", table.getColumnName(3));
            assertEquals("Used Materials", table.getColumnName(4));
            assertEquals("", table.getColumnName(5));
            assertEquals("", table.getColumnName(6));
            assertEquals(40, table.getColumnModel().getColumn(5).getPreferredWidth());
            assertEquals(40, table.getColumnModel().getColumn(6).getPreferredWidth());
            assertEquals("C1", table.getValueAt(0, 0));
            assertEquals("Coal Group", table.getValueAt(0, 1));
            assertEquals("150%", table.getValueAt(0, 2));
            assertEquals("112.5 MW", table.getValueAt(0, 3));
            assertEquals("Coal (22.5), Water (67.5)", table.getValueAt(0, 4));
            assertTrue(table.getValueAt(0, 5) instanceof java.util.UUID);
            assertTrue(table.getValueAt(0, 6) instanceof java.util.UUID);
            assertTrue(table.getColumnModel().getColumn(5).getCellRenderer()
                .getTableCellRendererComponent(table, table.getValueAt(0, 5), false, false, 0, 5) instanceof JButton);

            GeneratorProductionPanel fuelPanel = context.getBean(GeneratorProductionPanel.class, fuelGenerator);
            assertEquals("F1", fuelPanel.getTable().getValueAt(0, 0));
            assertEquals("312.5 MW", fuelPanel.getTable().getValueAt(0, 3));
            assertEquals("Fuel (25)", fuelPanel.getTable().getValueAt(0, 4));

            GeneratorProductionPanel geothermalPanel = context.getBean(
                GeneratorProductionPanel.class,
                geothermalGenerator
            );
            assertEquals(1, geothermalPanel.getTable().getRowCount());
            assertEquals("200 MW", geothermalPanel.getTable().getValueAt(0, 3));
            assertEquals("", geothermalPanel.getTable().getValueAt(0, 4));

            ProductionDeleteButtonEditor deleteButton = new ProductionDeleteButtonEditor(
                productionService,
                this.configuration.getLanguageObject(),
                panel,
                panel::refresh,
                () -> true
            );
            JButton button = (JButton) deleteButton.getTableCellEditorComponent(
                table,
                table.getValueAt(0, 6),
                false,
                0,
                6
            );
            button.doClick();
            assertEquals(0, table.getRowCount());
            assertEquals(0, productionService.getGeneratorProductions(coalGenerator).size());

            Factory emptyFactory = context.getBean(FactoryService.class).createFactory("Empty Generator Factory");
            context.publishEvent(new FactorySelectedEvent(emptyFactory));
            panel.refresh();
            assertEquals(0, panel.getTable().getRowCount());
            assertTrue(productionService.getGeneratorBuildingsWithProductions().isEmpty());
        }
    }

    @Test
    public void testAddsLocalizedGeneratorTabsAlphabetically()
    {
        Building fuelGenerator = new Building()
            .setKey("fuel_generator")
            .setName(new Name()
                .setEnglish("Alpha Generator")
                .setGerman("Zulu Generator"));
        Building coalGenerator = new Building()
            .setKey("coal_generator")
            .setName(new Name()
                .setEnglish("Zulu Generator")
                .setGerman("Alpha Generator"));
        JTabbedPane tabs = new JTabbedPane();

        LocalizedBuildingOrder.addTabs(
            tabs,
            List.of(fuelGenerator, coalGenerator),
            "English",
            ignored -> new JPanel()
        );

        assertEquals("Alpha Generator", tabs.getTitleAt(0));
        assertEquals("Zulu Generator", tabs.getTitleAt(1));

        JTabbedPane germanTabs = new JTabbedPane();
        LocalizedBuildingOrder.addTabs(
            germanTabs,
            List.of(fuelGenerator, coalGenerator),
            "German",
            ignored -> new JPanel()
        );

        assertEquals("Alpha Generator", germanTabs.getTitleAt(0));
        assertEquals("Zulu Generator", germanTabs.getTitleAt(1));
        assertEquals(coalGenerator.getName().getGerman(), germanTabs.getTitleAt(0));
    }

    private Building findBuilding(ProductionService productionService, String key)
    {
        return productionService.getBuildings().stream()
            .filter(building -> key.equals(building.getKey()))
            .findFirst()
            .orElseThrow();
    }
}
