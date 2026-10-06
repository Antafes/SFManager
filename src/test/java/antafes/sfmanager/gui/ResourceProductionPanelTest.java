package antafes.sfmanager.gui;

import antafes.sfmanager.BaseTest;
import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.entity.Item;
import antafes.sfmanager.entity.Purity;
import antafes.sfmanager.entity.Production;
import antafes.sfmanager.repository.BuildingRepository;
import antafes.sfmanager.repository.FactoryRepository;
import antafes.sfmanager.repository.ItemRepository;
import antafes.sfmanager.repository.ProductionRepository;
import antafes.sfmanager.repository.RecipeRepository;
import antafes.sfmanager.service.FactoryService;
import antafes.sfmanager.service.ProductionService;
import antafes.sfmanager.service.ResourceProductionEntry;
import antafes.sfmanager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.swing.JTable;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceProductionPanelTest extends BaseTest
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
    public void testShowsResourceRowsAndGroupsWellExtractors() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            ProductionService.class,
            ResourceProductionPanel.class
        )) {
            this.autowireTest(context);
            ProductionService productionService = context.getBean(ProductionService.class);
            Factory factory = context.getBean(FactoryService.class).createFactory("Resource Panel");
            context.publishEvent(new FactorySelectedEvent(factory));

            Building pressurizerBuilding = this.findBuilding(productionService, "well_pressurizer");
            Production pressurizer = productionService.saveProduction(new Production()
                .setSelectedBuilding(pressurizerBuilding)
                .setAmount(1.0)
                .setPosition("W1")
                .setGroup("Well Group")
                .setPurity(Purity.NORMAL)
                .setClockSpeed(150.0)
                .setUsedSomersloops(0));
            Building wellExtractorBuilding = this.findBuilding(productionService, "well_extractor");
            Item water = this.findItem(productionService, "water");
            Production extractor = productionService.saveProduction(new Production()
                .setSelectedBuilding(wellExtractorBuilding)
                .setSelectedItem(water)
                .setAmount(2.0)
                .setPurity(Purity.PURE)
                .setClockSpeed(150.0)
                .setGroup("Well Group")
                .setPressurizerId(pressurizer.getId())
                .setUsedSomersloops(0));
            productionService.saveProduction(new Production()
                .setSelectedBuilding(wellExtractorBuilding)
                .setSelectedItem(water)
                .setAmount(1.0)
                .setPurity(Purity.NORMAL)
                .setClockSpeed(150.0)
                .setGroup("Well Group")
                .setPressurizerId(pressurizer.getId())
                .setUsedSomersloops(0));
            Building waterExtractorBuilding = this.findBuilding(productionService, "water_extractor");
            productionService.saveProduction(new Production()
                .setSelectedBuilding(waterExtractorBuilding)
                .setSelectedItem(water)
                .setAmount(1.5)
                .setPurity(Purity.NORMAL)
                .setPosition("W2")
                .setClockSpeed(100.0)
                .setGroup("Water Group")
                .setUsedSomersloops(0));
            Item iron = this.findItem(productionService, "iron");
            Building miner = this.findBuilding(productionService, "miner_1");
            productionService.saveProduction(new Production()
                .setSelectedBuilding(miner)
                .setSelectedItem(iron)
                .setAmount(1.0)
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setGroup("Iron Group")
                .setUsedSomersloops(0));

            List<ResourceProductionEntry> entries = productionService.getResourceProductions(water);
            assertEquals(3, entries.size());
            assertEquals(1, productionService.getResourceProductions(iron).size());
            assertEquals(pressurizer, entries.get(0).pressurizer());
            assertEquals(60, entries.get(0).baseAmount());
            assertEquals(60, entries.get(1).baseAmount());
            assertEquals(120, entries.get(2).baseAmount());

            ResourceProductionPanel panel = context.getBean(
                ResourceProductionPanel.class,
                water
            );
            JTable table = panel.getTable();
            assertEquals(4, table.getRowCount());
            assertEquals(9, table.getColumnCount());
            assertEquals("Building", table.getColumnName(0));
            assertEquals("Position", table.getColumnName(1));
            assertEquals("Group", table.getColumnName(2));
            assertEquals("Purity", table.getColumnName(3));
            assertEquals("Clock speed", table.getColumnName(4));
            assertEquals("Production", table.getColumnName(5));
            assertEquals("Usage", table.getColumnName(6));
            assertEquals("Consumption", table.getColumnName(7));
            assertEquals("", table.getColumnName(8));
            for (int column = 1; column <= 5; column++) {
                assertTrue(table.getColumnModel().getColumn(column).getPreferredWidth() <= 100);
            }
            assertTrue(table.getColumnModel().getColumn(8).getPreferredWidth() <= 100);
            assertEquals("Resource Well Pressurizer", table.getValueAt(0, 0));
            assertEquals("W1", table.getValueAt(0, 1));
            assertEquals("Well Group", table.getValueAt(0, 2));
            assertEquals("150%", table.getValueAt(0, 4));
            assertTrue(table.getValueAt(0, 7).toString().endsWith(" MW"));
            assertEquals("Resource Well Extractor", table.getValueAt(1, 0));
            assertEquals("Well Group", table.getValueAt(1, 2));
            assertEquals("Pure", table.getValueAt(1, 3));
            assertEquals("360 / min", table.getValueAt(1, 5));
            assertEquals("", table.getValueAt(1, 1));
            assertEquals("", table.getValueAt(1, 4));
            assertEquals("", table.getValueAt(1, 7));
            assertEquals("Resource Well Extractor", table.getValueAt(2, 0));
            assertEquals("Well Group", table.getValueAt(2, 2));
            assertEquals("Normal", table.getValueAt(2, 3));
            assertEquals("90 / min", table.getValueAt(2, 5));
            assertEquals("Water Extractor", table.getValueAt(3, 0));
            assertEquals("W2", table.getValueAt(3, 1));
            assertEquals("Water Group", table.getValueAt(3, 2));
            assertEquals("180 / min", table.getValueAt(3, 5));
            assertEquals("30 MW", table.getValueAt(3, 7));
            for (int row = 0; row < table.getRowCount(); row++) {
                assertEquals("", table.getValueAt(row, 6));
                assertEquals("", table.getValueAt(row, 8));
            }
            assertNotNull(extractor.getId());
        }
    }

    @Test
    public void testRefreshReloadsTheCurrentFactoryProductions() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            ProductionService.class,
            ResourceProductionPanel.class
        )) {
            this.autowireTest(context);
            ProductionService productionService = context.getBean(ProductionService.class);
            Item iron = this.findItem(productionService, "iron");
            ResourceProductionPanel panel = context.getBean(
                ResourceProductionPanel.class,
                iron
            );
            assertEquals(0, panel.getTable().getRowCount());

            Factory firstFactory = context.getBean(FactoryService.class).createFactory("First Resource Factory");
            context.publishEvent(new FactorySelectedEvent(firstFactory));
            Building miner = this.findBuilding(productionService, "miner_1");
            productionService.saveProduction(new Production()
                .setSelectedBuilding(miner)
                .setSelectedItem(iron)
                .setAmount(1.0)
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setGroup("Iron Group")
                .setUsedSomersloops(0));
            panel.refresh();
            assertEquals(1, panel.getTable().getRowCount());

            Factory secondFactory = context.getBean(FactoryService.class).createFactory("Second Resource Factory");
            context.publishEvent(new FactorySelectedEvent(secondFactory));
            panel.refresh();
            assertEquals(0, panel.getTable().getRowCount());
            assertFalse(firstFactory.getProductions().isEmpty());
            assertTrue(secondFactory.getProductions().isEmpty());
        }
    }

    private Building findBuilding(ProductionService productionService, String key)
    {
        return productionService.getBuildings().stream()
            .filter(building -> key.equals(building.getKey()))
            .findFirst()
            .orElseThrow();
    }

    private Item findItem(ProductionService productionService, String key)
    {
        return productionService.getResourceItems().stream()
            .filter(item -> key.equals(item.getKey()))
            .findFirst()
            .orElseThrow();
    }
}
