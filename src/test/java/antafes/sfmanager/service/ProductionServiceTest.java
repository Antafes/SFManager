package antafes.sfmanager.service;

import antafes.sfmanager.BaseTest;
import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.entity.Purity;
import antafes.sfmanager.entity.Production;
import antafes.sfmanager.entity.Recipe;
import antafes.sfmanager.repository.BuildingRepository;
import antafes.sfmanager.repository.FactoryRepository;
import antafes.sfmanager.repository.ItemRepository;
import antafes.sfmanager.repository.ProductionRepository;
import antafes.sfmanager.repository.RecipeRepository;
import antafes.sfmanager.service.event.FactorySelectedEvent;
import antafes.sfmanager.service.event.ProductionSavedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ProductionServiceTest extends BaseTest
{
    @Autowired
    private FactoryService factoryService;
    @Autowired
    private ProductionService productionService;
    @Autowired
    private ProductionRepository productionRepository;

    @Test
    public void testSavesProductionAndReloadsItsFactoryAssociation() throws IOException
    {
        UUID factoryId;
        UUID productionId;
        AtomicReference<ProductionSavedEvent> savedEvent = new AtomicReference<>();
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            ProductionService.class
        )) {
            this.autowireTest(context);
            Factory factory = this.factoryService.createFactory("Production Service Factory");
            factoryId = factory.getId();
            context.publishEvent(new FactorySelectedEvent(factory));
            context.addApplicationListener((ApplicationListener<ApplicationEvent>) event -> {
                if (event instanceof PayloadApplicationEvent<?> payload
                    && payload.getPayload() instanceof ProductionSavedEvent productionSavedEvent) {
                    savedEvent.set(productionSavedEvent);
                }
            });

            Building building = this.productionService.getBuildings().stream()
                .filter(candidate -> "smelter".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();
            Recipe recipe = this.productionService.getRecipesForBuilding(building.getKey()).stream()
                .filter(candidate -> "iron_ingot".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();
            Production production = this.productionService.saveProduction(new Production()
                .setSelectedBuilding(building)
                .setSelectedRecipe(recipe)
                .setAmount(1.5)
                .setGroup("Foundry")
                .setPosition("A1")
                .setPurity(Purity.PURE)
                .setClockSpeed(125.0)
                .setUsedSomersloops(0));
            productionId = production.getId();

            assertNotNull(productionId);
            assertTrue(factory.getProductions().contains(productionId.toString()));
            assertNotNull(savedEvent.get());
            assertSame(production, savedEvent.get().production());
            assertEquals(List.of("Foundry"), this.productionService.getProductionGroups());
        }

        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            ProductionService.class
        )) {
            this.autowireTest(context);
            this.productionService.getBuildings();
            Production loadedProduction = this.productionRepository.findOne(productionId);
            assertNotNull(loadedProduction);
            assertEquals(1.5, loadedProduction.getAmount(), 0.0001);
            assertEquals("A1", loadedProduction.getPosition());
            assertEquals("Foundry", loadedProduction.getGroup());
            assertEquals("smelter", loadedProduction.getSelectedBuilding().getKey());
            assertEquals("iron_ingot", loadedProduction.getSelectedRecipe().getKey());
            assertEquals(Purity.PURE, loadedProduction.getPurity());
            assertEquals(125.0, loadedProduction.getClockSpeed(), 0.0001);
            assertEquals(0, loadedProduction.getUsedSomersloops());

            Factory loadedFactory = this.factoryService.findFactory(factoryId);
            assertNotNull(loadedFactory);
            assertTrue(loadedFactory.getProductions().contains(productionId.toString()));
            assertNull(this.factoryService.getCurrentFactory());
        }
    }

    @Test
    public void testRollsBackProductionWhenFactoryPersistenceFails() throws IOException
    {
        AtomicReference<ProductionSavedEvent> savedEvent = new AtomicReference<>();
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            ProductionService.class
        )) {
            this.autowireTest(context);
            Factory factory = this.factoryService.createFactory("Factory Save Failure");
            context.publishEvent(new FactorySelectedEvent(factory));
            context.addApplicationListener((ApplicationListener<ApplicationEvent>) event -> {
                if (event instanceof PayloadApplicationEvent<?> payload
                    && payload.getPayload() instanceof ProductionSavedEvent productionSavedEvent) {
                    savedEvent.set(productionSavedEvent);
                }
            });

            Building building = this.productionService.getBuildings().stream()
                .filter(candidate -> "smelter".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();
            Production production = new Production()
                .setSelectedBuilding(building)
                .setAmount(1.0)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0);
            Files.delete(this.tempDirectory.resolve("factory-save-failure").resolve("factory.xml"));

            assertThrows(IOException.class, () -> this.productionService.saveProduction(production));
            assertTrue(this.productionRepository.findAll().isEmpty());
            assertFalse(factory.getProductions().contains(production.getId().toString()));
            assertFalse(Files.readString(this.tempDirectory.resolve("productions.xml"))
                .contains(production.getId().toString()));
            assertNull(savedEvent.get());
        }
    }

    @Test
    public void testRequiresAnOpenFactoryToSave()
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            ProductionService.class
        )) {
            this.autowireTest(context);

            assertThrows(
                IllegalStateException.class,
                () -> this.productionService.saveProduction(new Production())
            );
        }
    }
}
