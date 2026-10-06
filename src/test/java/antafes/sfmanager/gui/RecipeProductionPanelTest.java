package antafes.sfmanager.gui;

import antafes.sfmanager.BaseTest;
import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.entity.Item;
import antafes.sfmanager.entity.Name;
import antafes.sfmanager.entity.Purity;
import antafes.sfmanager.entity.Production;
import antafes.sfmanager.entity.Recipe;
import antafes.sfmanager.gui.editor.ProductionDeleteButtonEditor;
import antafes.sfmanager.gui.order.LocalizedItemOrder;
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

import javax.swing.JButton;
import javax.swing.JTable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            productionService.saveProduction(new Production()
                .setSelectedBuilding(smelter)
                .setSelectedRecipe(ironIngot)
                .setClockSpeed(100.0)
                .setUsedSomersloops(1));

            Building foundry = this.findBuilding(productionService, "foundry");
            Recipe basicIronIngot = productionService.getRecipesForBuilding(foundry.getKey()).stream()
                .filter(recipe -> "basic_iron_ingot".equals(recipe.getKey()))
                .findFirst()
                .orElseThrow();
            productionService.saveProduction(new Production()
                .setSelectedBuilding(foundry)
                .setSelectedRecipe(basicIronIngot)
                .setClockSpeed(100.0)
                .setUsedSomersloops(1));

            Building miner = this.findBuilding(productionService, "miner_1");
            Item ironOre = productionService.getResourceItems().stream()
                .filter(item -> "iron".equals(item.getKey()))
                .findFirst()
                .orElseThrow();
            productionService.saveProduction(new Production()
                .setSelectedBuilding(miner)
                .setSelectedItem(ironOre)
                .setClockSpeed(100.0)
                .setPurity(Purity.NORMAL)
                .setUsedSomersloops(0));

            Building generator = this.findBuilding(productionService, "coal_generator");
            productionService.saveProduction(new Production()
                .setSelectedBuilding(generator)
                .setSelectedRecipe(productionService.getRecipesForBuilding(generator.getKey()).stream()
                    .filter(recipe -> "coal_generator_coal".equals(recipe.getKey()))
                    .findFirst()
                    .orElseThrow())
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));

            assertEquals(1, productionService.getRecipeItemsWithProductions().size());
            Item ironIngotItem = ironIngot.getProducts().get(0).getResource();
            assertEquals("iron_ingot", productionService.getRecipeItemsWithProductions().get(0).getKey());
            assertEquals(2, productionService.getRecipeProductions(ironIngotItem).size());

            RecipeProductionPanel panel = context.getBean(RecipeProductionPanel.class, ironIngotItem);
            JTable table = panel.getTable();
            assertEquals(2, table.getRowCount());
            assertEquals(8, table.getColumnCount());
            assertEquals("Used Materials", table.getColumnName(0));
            assertEquals("Recipe", table.getColumnName(1));
            assertEquals("Clock speed", table.getColumnName(2));
            assertEquals("Somersloops", table.getColumnName(3));
            assertEquals("Produced Materials", table.getColumnName(4));
            assertEquals("Usage", table.getColumnName(5));
            assertEquals("Consumption", table.getColumnName(6));
            assertEquals("", table.getColumnName(7));
            assertEquals(80, table.getColumnModel().getColumn(7).getPreferredWidth());
            assertEquals("Iron Ore (60)", table.getValueAt(0, 0));
            assertEquals("Iron Ingot", table.getValueAt(0, 1));
            assertEquals("100%", table.getValueAt(0, 2));
            assertEquals(1, table.getValueAt(0, 3));
            assertEquals("Iron Ingot (60)", table.getValueAt(0, 4));
            assertEquals("", table.getValueAt(0, 5));
            assertEquals("16 MW", table.getValueAt(0, 6));
            assertTrue(table.getValueAt(0, 7) instanceof java.util.UUID);
            assertEquals("Iron Ore (50), Limestone (80)", table.getValueAt(1, 0));
            assertEquals("Basic Iron Ingot", table.getValueAt(1, 1));
            assertEquals("Iron Ingot (100)", table.getValueAt(1, 4));
            assertEquals("64 MW", table.getValueAt(1, 6));

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
            .setProductions(List.of(new antafes.sfmanager.entity.item.Production()));

        List<Item> sorted = Stream.of(zinc, aluminum)
            .sorted(LocalizedItemOrder.alphabeticalComparator("English"))
            .toList();

        assertEquals("aluminum", sorted.get(0).getKey());
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
            Production production = productionService.saveProduction(new Production()
                .setSelectedBuilding(smelter)
                .setSelectedRecipe(recipe)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));
            Item outputItem = recipe.getProducts().get(0).getResource();
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
                7
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
                7
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
