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
import antafes.sfmanager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.swing.JTable;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

            assertEquals(5, materialTable.getColumnCount());
            assertEquals("Material", materialTable.getColumnName(0));
            assertEquals("Production", materialTable.getColumnName(1));
            assertEquals("Usage", materialTable.getColumnName(2));
            assertEquals("Unused", materialTable.getColumnName(3));
            assertEquals("Consumption", materialTable.getColumnName(4));
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
            productionService.saveProductions(new Production()
                .setSelectedBuilding(miner)
                .setSelectedItem(iron)
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0), 2);
            Building generator = productionService.getBuildings().stream()
                .filter(building -> "coal_generator".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            productionService.saveProductions(new Production()
                .setSelectedBuilding(generator)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0), 2);

            panel.refresh();

            assertEquals(1, materialTable.getRowCount());
            assertEquals("Iron Ore", materialTable.getValueAt(0, 0));
            assertEquals("120 / min", materialTable.getValueAt(0, 1));
            assertEquals("", materialTable.getValueAt(0, 2));
            assertEquals("120 / min", materialTable.getValueAt(0, 3));
            assertEquals("10 MW", materialTable.getValueAt(0, 4));
            assertEquals(1, powerSourceTable.getRowCount());
            assertEquals("Coal-Powered Generator", powerSourceTable.getValueAt(0, 0));
            assertEquals("150 MW", powerSourceTable.getValueAt(0, 1));

            Factory emptyFactory = context.getBean(FactoryService.class).createFactory("Empty Overview Factory");
            context.publishEvent(new FactorySelectedEvent(emptyFactory));
            panel.refresh();
            assertEquals(0, materialTable.getRowCount());
            assertEquals(0, powerSourceTable.getRowCount());
            assertFalse(factory.getProductions().isEmpty());
        }
    }
}
