package antafes.sfmanager.gui;

import antafes.sfmanager.BaseTest;
import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.entity.Production;
import antafes.sfmanager.entity.Recipe;
import antafes.sfmanager.entity.Storage;
import antafes.sfmanager.repository.BuildingRepository;
import antafes.sfmanager.repository.FactoryRepository;
import antafes.sfmanager.repository.ItemRepository;
import antafes.sfmanager.repository.ProductionRepository;
import antafes.sfmanager.repository.RecipeRepository;
import antafes.sfmanager.repository.StorageRepository;
import antafes.sfmanager.service.FactoryService;
import antafes.sfmanager.service.ProductionService;
import antafes.sfmanager.service.StorageService;
import antafes.sfmanager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.swing.JTable;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

            assertEquals(3, table.getColumnCount());
            assertEquals("Item", table.getColumnName(0));
            assertEquals("Stored / min", table.getColumnName(1));
            assertEquals("Source productions", table.getColumnName(2));
            assertEquals(1, table.getRowCount());
            assertFalse(table.isCellEditable(0, 0));
            assertTrue(table.getAutoCreateRowSorter());
            assertEquals("Iron Ingot", table.getValueAt(0, 0));
            assertEquals("30 / min", table.getValueAt(0, 1));
            assertTrue(table.getValueAt(0, 2).toString().contains("Smelter"));
            assertTrue(table.getValueAt(0, 2).toString().contains("Iron Ingot"));
            assertTrue(table.getValueAt(0, 2).toString().contains("Ingot Line"));
            assertTrue(table.getValueAt(0, 2).toString().contains("A1"));
        }
    }
}
