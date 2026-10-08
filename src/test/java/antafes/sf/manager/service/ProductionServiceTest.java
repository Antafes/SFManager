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

package antafes.sf.manager.service;

import antafes.sf.manager.BaseTest;
import antafes.sf.manager.entity.*;
import antafes.sf.manager.repository.*;
import antafes.sf.manager.repository.xml.productions.ProductionData;
import antafes.sf.manager.repository.xml.productions.ProductionsData;
import antafes.sf.manager.service.dto.FactoryOverviewData;
import antafes.sf.manager.service.dto.FactoryOverviewMaterial;
import antafes.sf.manager.service.event.FactorySelectedEvent;
import antafes.sf.manager.service.event.ProductionDeletedEvent;
import antafes.sf.manager.service.event.ProductionSavedEvent;
import jakarta.xml.bind.JAXBContext;
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

import static org.junit.jupiter.api.Assertions.*;

public class ProductionServiceTest extends BaseTest
{
    @Autowired
    private FactoryService factoryService;
    @Autowired
    private ProductionService productionService;
    @Autowired
    private ProductionRepository productionRepository;

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
            assertTrue(this.tempDirectory.resolve("production-service-factory")
                .resolve("productions.xml").toFile().isFile());
            assertNull(this.factoryService.getCurrentFactory());
        }
    }

    @Test
    public void testSavesQuantityAsIndependentProductions() throws IOException
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
            Factory factory = this.factoryService.createFactory("Production Chain Factory");
            context.publishEvent(new FactorySelectedEvent(factory));
            Building building = this.productionService.getBuildings().stream()
                .filter(candidate -> "smelter".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();
            Recipe recipe = this.productionService.getRecipesForBuilding(building.getKey()).stream()
                .filter(candidate -> "iron_ingot".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();

            List<Production> productions = this.productionService.saveProductions(new Production()
                .setSelectedBuilding(building)
                .setSelectedRecipe(recipe)
                .setGroup("Foundry")
                .setPosition("A1")
                .setClockSpeed(125.0)
                .setUsedSomersloops(0), 3);

            assertEquals(3, productions.size());
            assertEquals(3, factory.getProductions().size());
            assertEquals(3, productions.stream().map(Production::getId).distinct().count());
            assertTrue(productions.stream().allMatch(production ->
                production.getSelectedBuilding().equals(building)
                    && production.getSelectedRecipe().equals(recipe)
                    && "Foundry".equals(production.getGroup())
                    && "A1".equals(production.getPosition())
                    && Double.valueOf(125.0).equals(production.getClockSpeed())
            ));
            String productionData = Files.readString(this.tempDirectory.resolve("production-chain-factory")
                .resolve("productions.xml"));
            assertFalse(productionData.contains("<amount>"));
        }
    }

    @Test
    public void testUpgradesMinersAndPersistsTheirUnchangedProductionData() throws IOException
    {
        UUID productionId;
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
                Factory factory = this.factoryService.createFactory("Miner Upgrade Service Factory");
                context.publishEvent(new FactorySelectedEvent(factory));
                Building miner1 = this.productionService.getBuildings().stream()
                    .filter(building -> "miner_1".equals(building.getKey()))
                    .findFirst()
                    .orElseThrow();
                Item iron = this.productionService.getResourceItems().stream()
                    .filter(item -> "iron".equals(item.getKey()))
                    .findFirst()
                    .orElseThrow();
                Production production = this.productionService.saveProduction(new Production()
                    .setSelectedBuilding(miner1)
                    .setSelectedItem(iron)
                    .setGroup("Miner Group")
                    .setPosition("M1")
                    .setPurity(Purity.PURE)
                    .setClockSpeed(150.0)
                    .setUsedSomersloops(0));
                productionId = production.getId();

                assertEquals(
                    List.of("miner_2", "miner_3"),
                    this.productionService.getMinerUpgradeBuildings(productionId).stream()
                        .map(Building::getKey)
                        .toList()
                );
                Production upgraded = this.productionService.upgradeMiner(productionId, "miner_2");

                assertEquals(productionId, upgraded.getId());
                assertEquals("miner_2", upgraded.getSelectedBuilding().getKey());
                assertEquals(iron, upgraded.getSelectedItem());
                assertEquals("Miner Group", upgraded.getGroup());
                assertEquals("M1", upgraded.getPosition());
                assertEquals(Purity.PURE, upgraded.getPurity());
                assertEquals(150.0, upgraded.getClockSpeed(), 0.0001);
                assertEquals(0, upgraded.getUsedSomersloops());
                assertEquals(List.of("miner_3"), this.productionService.getMinerUpgradeBuildings(productionId)
                    .stream().map(Building::getKey).toList());
                assertThrows(
                    IllegalArgumentException.class,
                    () -> this.productionService.upgradeMiner(productionId, "miner_1")
                );
                this.productionService.upgradeMiner(productionId, "miner_3");
                assertTrue(this.productionService.getMinerUpgradeBuildings(productionId).isEmpty());
                assertThrows(
                    IllegalArgumentException.class,
                    () -> this.productionService.upgradeMiner(productionId, "water_extractor")
                );
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
                Production loadedProduction = this.productionRepository.findOne(productionId);
                assertNotNull(loadedProduction);
                assertEquals("miner_3", loadedProduction.getSelectedBuilding().getKey());
                assertEquals("iron", loadedProduction.getSelectedItem().getKey());
                assertEquals("Miner Group", loadedProduction.getGroup());
                assertEquals("M1", loadedProduction.getPosition());
                assertEquals(Purity.PURE, loadedProduction.getPurity());
                assertEquals(150.0, loadedProduction.getClockSpeed(), 0.0001);
        }
    }

    @Test
    public void testEditsAllowedProductionFieldsAndPersistsThem() throws IOException
    {
        UUID productionId;
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
            Factory factory = this.factoryService.createFactory("Edit Production Service Factory");
            context.publishEvent(new FactorySelectedEvent(factory));
            Building smelter = this.productionService.getBuildings().stream()
                .filter(building -> "smelter".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Recipe recipe = this.productionService.getRecipesForBuilding(smelter.getKey()).stream()
                .filter(candidate -> "iron_ingot".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();
            Production production = this.productionService.saveProduction(new Production()
                .setSelectedBuilding(smelter)
                .setSelectedRecipe(recipe)
                .setGroup("Original Group")
                .setPosition("A1")
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));
            productionId = production.getId();

            Production edited = this.productionService.editProduction(
                productionId,
                125.0,
                " Edited Group ",
                " B2 ",
                1
            );

            assertEquals(productionId, edited.getId());
            assertEquals(smelter, edited.getSelectedBuilding());
            assertEquals(recipe, edited.getSelectedRecipe());
            assertNull(edited.getSelectedItem());
            assertEquals("Edited Group", edited.getGroup());
            assertEquals("B2", edited.getPosition());
            assertEquals(125.0, edited.getClockSpeed(), 0.0001);
            assertEquals(1, edited.getUsedSomersloops());
            assertThrows(
                IllegalArgumentException.class,
                () -> this.productionService.editProduction(productionId, 251, "Other", "C3", 0)
            );
            assertEquals("Edited Group", production.getGroup());
            assertEquals(125.0, production.getClockSpeed(), 0.0001);
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
            Production loadedProduction = this.productionRepository.findOne(productionId);
            assertNotNull(loadedProduction);
            assertEquals("smelter", loadedProduction.getSelectedBuilding().getKey());
            assertEquals("iron_ingot", loadedProduction.getSelectedRecipe().getKey());
            assertEquals("Edited Group", loadedProduction.getGroup());
            assertEquals("B2", loadedProduction.getPosition());
            assertEquals(125.0, loadedProduction.getClockSpeed(), 0.0001);
            assertEquals(1, loadedProduction.getUsedSomersloops());
        }
    }

    @Test
    public void testDeletesProductionAndItsLinkedWellExtractors() throws IOException
    {
        UUID factoryId;
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
            Factory factory = this.factoryService.createFactory("Delete Pressurizer Factory");
            factoryId = factory.getId();
            context.publishEvent(new FactorySelectedEvent(factory));
            Building pressurizerBuilding = this.productionService.getBuildings().stream()
                .filter(building -> "well_pressurizer".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Production pressurizer = this.productionService.saveProduction(new Production()
                .setSelectedBuilding(pressurizerBuilding)
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));
            Building extractorBuilding = this.productionService.getBuildings().stream()
                .filter(building -> "well_extractor".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Item water = this.productionService.getResourceItems().stream()
                .filter(item -> "water".equals(item.getKey()))
                .findFirst()
                .orElseThrow();
            Production extractor = this.productionService.saveProduction(new Production()
                .setSelectedBuilding(extractorBuilding)
                .setSelectedItem(water)
                .setPurity(Purity.NORMAL)
                .setPressurizerId(pressurizer.getId())
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));
            AtomicReference<ProductionDeletedEvent> deletedEvent = new AtomicReference<>();
            context.addApplicationListener((ApplicationListener<ApplicationEvent>) event -> {
                if (event instanceof PayloadApplicationEvent<?> payload
                    && payload.getPayload() instanceof ProductionDeletedEvent productionDeletedEvent) {
                    deletedEvent.set(productionDeletedEvent);
                }
            });

            List<Production> deleted = this.productionService.deleteProduction(pressurizer.getId());

            assertEquals(List.of(pressurizer, extractor), deleted);
            assertFalse(factory.getProductions().contains(pressurizer.getId().toString()));
            assertFalse(factory.getProductions().contains(extractor.getId().toString()));
            assertNull(this.productionRepository.findOne(pressurizer.getId()));
            assertNull(this.productionRepository.findOne(extractor.getId()));
            assertEquals(deleted, deletedEvent.get().productions());
            String savedProductions = Files.readString(this.tempDirectory.resolve("delete-pressurizer-factory")
                .resolve("productions.xml"));
            assertFalse(savedProductions.contains(pressurizer.getId().toString()));
            assertFalse(savedProductions.contains(extractor.getId().toString()));
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
            Factory loadedFactory = this.factoryService.findFactory(factoryId);
            assertNotNull(loadedFactory);
            assertTrue(loadedFactory.getProductions().isEmpty());
            assertTrue(this.productionRepository.findAll().isEmpty());
        }
    }

    @Test
    public void testRejectsDeletingProductionFromAnotherFactory() throws IOException
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
            Factory ownerFactory = this.factoryService.createFactory("Production Owner Factory");
            context.publishEvent(new FactorySelectedEvent(ownerFactory));
            Building smelter = this.productionService.getBuildings().stream()
                .filter(building -> "smelter".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Recipe recipe = this.productionService.getRecipesForBuilding(smelter.getKey()).stream()
                .filter(candidate -> "iron_ingot".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();
            Production production = this.productionService.saveProduction(new Production()
                .setSelectedBuilding(smelter)
                .setSelectedRecipe(recipe)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));
            Factory otherFactory = this.factoryService.createFactory("Other Production Factory");
            context.publishEvent(new FactorySelectedEvent(otherFactory));

            assertThrows(
                IllegalArgumentException.class,
                () -> this.productionService.deleteProduction(production.getId())
            );
            assertNotNull(this.productionRepository.findOne(production.getId()));
            assertTrue(ownerFactory.getProductions().contains(production.getId().toString()));
        }
    }

    @Test
    public void testRestoresProductionAndFactoryAssociationWhenDeletePersistenceFails() throws IOException
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
            Factory factory = this.factoryService.createFactory("Delete Rollback Factory");
            context.publishEvent(new FactorySelectedEvent(factory));
            Building smelter = this.productionService.getBuildings().stream()
                .filter(building -> "smelter".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Recipe recipe = this.productionService.getRecipesForBuilding(smelter.getKey()).stream()
                .filter(candidate -> "iron_ingot".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();
            Production production = this.productionService.saveProduction(new Production()
                .setSelectedBuilding(smelter)
                .setSelectedRecipe(recipe)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));
            AtomicReference<ProductionDeletedEvent> deletedEvent = new AtomicReference<>();
            context.addApplicationListener((ApplicationListener<ApplicationEvent>) event -> {
                if (event instanceof PayloadApplicationEvent<?> payload
                    && payload.getPayload() instanceof ProductionDeletedEvent productionDeletedEvent) {
                    deletedEvent.set(productionDeletedEvent);
                }
            });
            Files.delete(this.tempDirectory.resolve("delete-rollback-factory").resolve("factory.xml"));

            assertThrows(
                IOException.class,
                () -> this.productionService.deleteProduction(production.getId())
            );
            assertTrue(factory.getProductions().contains(production.getId().toString()));
            assertNotNull(this.productionRepository.findOne(production.getId()));
            assertTrue(Files.readString(this.tempDirectory.resolve("delete-rollback-factory")
                    .resolve("productions.xml"))
                .contains(production.getId().toString()));
            assertNull(deletedEvent.get());
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
                .setClockSpeed(100.0)
                .setUsedSomersloops(0);
            Files.delete(this.tempDirectory.resolve("factory-save-failure").resolve("factory.xml"));

            assertThrows(IOException.class, () -> this.productionService.saveProductions(production, 3));
            assertTrue(this.productionRepository.findAll().isEmpty());
            assertFalse(factory.getProductions().contains(production.getId().toString()));
            assertFalse(Files.readString(this.tempDirectory.resolve("factory-save-failure")
                    .resolve("productions.xml"))
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

    @Test
    public void testSavesProductionWhenFactoryWasRestoredFromConfiguration() throws IOException
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
            Factory factory = this.factoryService.createFactory("Restored Production Factory");
            this.configuration.setLastOpenedFactoryId(factory.getId());

            assertSame(factory, this.factoryService.restoreLastOpenedFactory());

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
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));

            assertTrue(factory.getProductions().contains(production.getId().toString()));
            assertNotNull(this.productionRepository.findOne(production.getId()));
        }
    }

    @Test
    public void testReturnsResourceItemsFromItemProductionMappings()
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

            assertTrue(this.productionService.getItemsForBuilding("miner_1").stream()
                .anyMatch(item -> "iron".equals(item.getKey())));
            assertTrue(this.productionService.getItemsForBuilding("water_extractor").stream()
                .anyMatch(item -> "water".equals(item.getKey())));
            assertTrue(this.productionService.getItemsForBuilding("well_extractor").stream()
                .map(Item::getKey)
                .toList()
                .containsAll(List.of("oil", "water", "nitrogen")));
            assertTrue(this.productionService.getRecipesForBuilding("well_extractor").isEmpty());
            assertTrue(this.productionService.getRecipesForBuilding("smelter").stream()
                .anyMatch(recipe -> "iron_ingot".equals(recipe.getKey())));
        }
    }

    @Test
    public void testReturnsOnlyResourceItemsWithCurrentFactoryProductions() throws IOException
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
            assertTrue(this.productionService.getResourceItemsWithProductions().isEmpty());

            Factory factory = this.factoryService.createFactory("Resource Tabs Factory");
            context.publishEvent(new FactorySelectedEvent(factory));
            assertTrue(this.productionService.getResourceItemsWithProductions().isEmpty());

            Building miner = this.productionService.getBuildings().stream()
                .filter(candidate -> "miner_1".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();
            Item iron = this.productionService.getItemsForBuilding(miner.getKey()).stream()
                .filter(item -> "iron".equals(item.getKey()))
                .findFirst()
                .orElseThrow();
            this.productionService.saveProduction(new Production()
                .setSelectedBuilding(miner)
                .setSelectedItem(iron)
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));

            assertEquals(List.of("iron"), this.productionService.getResourceItemsWithProductions().stream()
                .map(Item::getKey)
                .toList());

            Building smelter = this.productionService.getBuildings().stream()
                .filter(candidate -> "smelter".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();
            Recipe ironIngot = this.productionService.getRecipesForBuilding(smelter.getKey()).stream()
                .filter(recipe -> "iron_ingot".equals(recipe.getKey()))
                .findFirst()
                .orElseThrow();
            this.productionService.saveProduction(new Production()
                .setSelectedBuilding(smelter)
                .setSelectedRecipe(ironIngot)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));

            assertEquals(List.of("iron"), this.productionService.getResourceItemsWithProductions().stream()
                .map(Item::getKey)
                .toList());
        }
    }

    @Test
    public void testAggregatesFactoryOverviewMaterialsAndPower() throws IOException
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
            assertEquals(0, this.productionService.getFactoryOverview().materials().size());
            Factory factory = this.factoryService.createFactory("Factory Overview");
            context.publishEvent(new FactorySelectedEvent(factory));

            Building miner = this.productionService.getBuildings().stream()
                .filter(building -> "miner_1".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Item iron = this.productionService.getItemsForBuilding(miner.getKey()).stream()
                .filter(item -> "iron".equals(item.getKey()))
                .findFirst()
                .orElseThrow();
            this.productionService.saveProductions(new Production()
                .setSelectedBuilding(miner)
                .setSelectedItem(iron)
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0), 2);

            Building smelter = this.productionService.getBuildings().stream()
                .filter(building -> "smelter".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Recipe ironIngot = this.productionService.getRecipesForBuilding(smelter.getKey()).stream()
                .filter(recipe -> "iron_ingot".equals(recipe.getKey()))
                .findFirst()
                .orElseThrow();
            this.productionService.saveProductions(new Production()
                .setSelectedBuilding(smelter)
                .setSelectedRecipe(ironIngot)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0), 2);

            Building coalGenerator = this.productionService.getBuildings().stream()
                .filter(building -> "coal_generator".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            this.productionService.saveProductions(new Production()
                .setSelectedBuilding(coalGenerator)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0), 2);
            Building fuelGenerator = this.productionService.getBuildings().stream()
                .filter(building -> "fuel_generator".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            this.productionService.saveProduction(new Production()
                .setSelectedBuilding(fuelGenerator)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));

            FactoryOverviewData overview = this.productionService.getFactoryOverview();
            FactoryOverviewMaterial ironMaterial = overview.materials().stream()
                .filter(material -> "iron".equals(material.item().getKey()))
                .findFirst()
                .orElseThrow();
            FactoryOverviewMaterial ingotMaterial = overview.materials().stream()
                .filter(material -> "iron_ingot".equals(material.item().getKey()))
                .findFirst()
                .orElseThrow();

            assertEquals(120.0, ironMaterial.production(), 0.0001);
            assertEquals(10.0, ironMaterial.powerConsumption(), 0.0001);
            assertEquals(60.0, ingotMaterial.production(), 0.0001);
            assertEquals(8.0, ingotMaterial.powerConsumption(), 0.0001);
            assertEquals(400.0, overview.totalPowerProduction(), 0.0001);
            assertEquals(18.0, overview.totalPowerConsumption(), 0.0001);
            assertEquals(382.0, overview.availablePower(), 0.0001);
            assertEquals(2, overview.powerSources().size());
            assertEquals(150.0, overview.powerSources().stream()
                .filter(source -> "coal_generator".equals(source.building().getKey()))
                .findFirst()
                .orElseThrow()
                .production(), 0.0001);
            assertEquals(250.0, overview.powerSources().stream()
                .filter(source -> "fuel_generator".equals(source.building().getKey()))
                .findFirst()
                .orElseThrow()
                .production(), 0.0001);
        }
    }

    @Test
    public void testCountsSharedWellPressurizerPowerOnceInFactoryTotal() throws IOException
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
            Factory factory = this.factoryService.createFactory("Shared Pressurizer Overview");
            context.publishEvent(new FactorySelectedEvent(factory));

            Building pressurizerBuilding = this.productionService.getBuildings().stream()
                .filter(building -> "well_pressurizer".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Production pressurizer = this.productionService.saveProduction(new Production()
                .setSelectedBuilding(pressurizerBuilding)
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));

            Building wellExtractor = this.productionService.getBuildings().stream()
                .filter(building -> "well_extractor".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Item water = this.productionService.getItemsForBuilding(wellExtractor.getKey()).stream()
                .filter(item -> "water".equals(item.getKey()))
                .findFirst()
                .orElseThrow();
            Item oil = this.productionService.getItemsForBuilding(wellExtractor.getKey()).stream()
                .filter(item -> "oil".equals(item.getKey()))
                .findFirst()
                .orElseThrow();
            for (Item item : List.of(water, oil)) {
                this.productionService.saveProduction(new Production()
                    .setSelectedBuilding(wellExtractor)
                    .setSelectedItem(item)
                    .setPurity(Purity.NORMAL)
                    .setClockSpeed(100.0)
                    .setPressurizerId(pressurizer.getId())
                    .setUsedSomersloops(0));
            }

            FactoryOverviewData overview = this.productionService.getFactoryOverview();
            assertEquals(150.0, overview.totalPowerConsumption(), 0.0001);
            assertEquals(60.0, overview.materials().stream()
                .filter(material -> "water".equals(material.item().getKey()))
                .findFirst()
                .orElseThrow()
                .production(), 0.0001);
            assertEquals(60.0, overview.materials().stream()
                .filter(material -> "oil".equals(material.item().getKey()))
                .findFirst()
                .orElseThrow()
                .production(), 0.0001);
            assertEquals(0, overview.materials().stream()
                .mapToDouble(FactoryOverviewMaterial::powerConsumption)
                .sum(), 0.0001);
        }
    }

    @Test
    public void testSavesAndReloadsSelectedResourceItem() throws IOException
    {
        UUID productionId;
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
            Factory factory = this.factoryService.createFactory("Resource Item Factory");
            context.publishEvent(new FactorySelectedEvent(factory));

            Building building = this.productionService.getBuildings().stream()
                .filter(candidate -> "miner_1".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();
            Item item = this.productionService.getItemsForBuilding(building.getKey()).stream()
                .filter(candidate -> "iron".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();
            Production production = this.productionService.saveProduction(new Production()
                .setSelectedBuilding(building)
                .setSelectedItem(item)
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));
            productionId = production.getId();
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
            assertEquals("miner_1", loadedProduction.getSelectedBuilding().getKey());
            assertEquals("iron", loadedProduction.getSelectedItem().getKey());
            assertNull(loadedProduction.getSelectedRecipe());
        }
    }

    @Test
    public void testMigratesLegacyProductionFileIntoItsFactoryDirectory() throws Exception
    {
        UUID factoryId;
        UUID productionId = UUID.randomUUID();
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
            Factory factory = this.factoryService.createFactory("Legacy Factory");
            factoryId = factory.getId();
            factory.getProductions().add(productionId.toString());
            this.factoryService.saveFactory(factory);

            ProductionData legacyProduction = new ProductionData();
            legacyProduction.id = productionId.toString();
            legacyProduction.selectedBuilding = "smelter";
            legacyProduction.amount = 2.0;
            legacyProduction.clockSpeed = 100.0;
            ProductionsData legacyData = new ProductionsData();
            legacyData.productions.add(legacyProduction);
            JAXBContext.newInstance(ProductionsData.class)
                .createMarshaller()
                .marshal(legacyData, this.tempDirectory.resolve("productions.xml").toFile());
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
            Production migratedProduction = this.productionRepository.findOne(productionId);

            assertNotNull(migratedProduction);
            assertEquals("smelter", migratedProduction.getSelectedBuilding().getKey());
            assertEquals(2, this.productionRepository.findAll().size());
            assertTrue(this.tempDirectory.resolve("legacy-factory").resolve("productions.xml").toFile().isFile());
            assertFalse(Files.readString(this.tempDirectory.resolve("legacy-factory")
                .resolve("productions.xml")).contains("<amount>"));
            assertFalse(Files.exists(this.tempDirectory.resolve("productions.xml")));
            assertEquals(2, this.factoryService.findFactory(factoryId).getProductions().size());
            assertTrue(this.factoryService.findFactory(factoryId).getProductions()
                .contains(productionId.toString()));
        }
    }

    @Test
    public void testRejectsResourceItemNotProducedBySelectedBuilding() throws IOException
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
            Factory factory = this.factoryService.createFactory("Invalid Resource Item Factory");
            context.publishEvent(new FactorySelectedEvent(factory));

            Building miner = this.productionService.getBuildings().stream()
                .filter(candidate -> "miner_1".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();
            Item water = this.productionService.getItemsForBuilding("water_extractor").stream()
                .filter(candidate -> "water".equals(candidate.getKey()))
                .findFirst()
                .orElseThrow();
            Production production = new Production()
                .setSelectedBuilding(miner)
                .setSelectedItem(water)
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0);

            assertThrows(
                IllegalArgumentException.class,
                () -> this.productionService.saveProduction(production)
            );
        }
    }

    @Test
    public void testAllocatesMaterialUsageBySourceGroupAndProductionOrder() throws IOException
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
            Factory factory = this.factoryService.createFactory("Material Allocation Factory");
            context.publishEvent(new FactorySelectedEvent(factory));

            Building miner = this.productionService.getBuildings().stream()
                .filter(building -> "miner_1".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Building waterExtractor = this.productionService.getBuildings().stream()
                .filter(building -> "water_extractor".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Building smelter = this.productionService.getBuildings().stream()
                .filter(building -> "smelter".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Building constructor = this.productionService.getBuildings().stream()
                .filter(building -> "constructor".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Building coalGenerator = this.productionService.getBuildings().stream()
                .filter(building -> "coal_generator".equals(building.getKey()))
                .findFirst()
                .orElseThrow();
            Item iron = this.productionService.getResourceItems().stream()
                .filter(item -> "iron".equals(item.getKey()))
                .findFirst()
                .orElseThrow();
            Item coal = this.productionService.getResourceItems().stream()
                .filter(item -> "coal".equals(item.getKey()))
                .findFirst()
                .orElseThrow();
            Item water = this.productionService.getResourceItems().stream()
                .filter(item -> "water".equals(item.getKey()))
                .findFirst()
                .orElseThrow();
            Recipe ironIngot = this.productionService.getRecipesForBuilding("smelter").stream()
                .filter(recipe -> "iron_ingot".equals(recipe.getKey()))
                .findFirst()
                .orElseThrow();
            Recipe ironPlate = this.productionService.getRecipesForBuilding("constructor").stream()
                .filter(recipe -> "iron_plate".equals(recipe.getKey()))
                .findFirst()
                .orElseThrow();
            Recipe coalPower = this.productionService.getRecipesForBuilding("coal_generator").stream()
                .filter(recipe -> "coal_generator_coal".equals(recipe.getKey()))
                .findFirst()
                .orElseThrow();

            this.productionService.saveProductions(new Production()
                .setSelectedBuilding(miner)
                .setSelectedItem(iron)
                .setGroup("Ore")
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0), 2);
            this.productionService.saveProductions(new Production()
                .setSelectedBuilding(smelter)
                .setSelectedRecipe(ironIngot)
                .setGroup("Ingots")
                .setClockSpeed(200.0)
                .setUsedSomersloops(0)
                .setMaterialSources(List.of(this.source("iron", "Ore"))), 2);
            this.productionService.saveProduction(new Production()
                .setSelectedBuilding(constructor)
                .setSelectedRecipe(ironPlate)
                .setClockSpeed(250.0)
                .setUsedSomersloops(0)
                .setMaterialSources(List.of(this.source("iron_ingot", "Ingots"))));
            this.productionService.saveProduction(new Production()
                .setSelectedBuilding(miner)
                .setSelectedItem(coal)
                .setGroup("Coal")
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));
            this.productionService.saveProduction(new Production()
                .setSelectedBuilding(waterExtractor)
                .setSelectedItem(water)
                .setGroup("Water")
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));
            this.productionService.saveProduction(new Production()
                .setSelectedBuilding(coalGenerator)
                .setSelectedRecipe(coalPower)
                .setClockSpeed(100.0)
                .setUsedSomersloops(0)
                .setMaterialSources(List.of(this.source("coal", "Coal"), this.source("water", "Water"))));

            assertEquals(
                List.of(60.0, 60.0),
                this.productionService.getResourceProductions(iron).stream()
                    .map(entry -> entry.usage())
                    .toList()
            );
            Item ironIngotItem = ironIngot.getProducts().getFirst().getResource();
            assertEquals(
                List.of(60.0, 15.0),
                this.productionService.getRecipeProductions(ironIngotItem).stream()
                    .map(entry -> entry.usage())
                    .toList()
            );
            assertEquals(15.0, this.productionService.getResourceProductions(coal).getFirst().usage());
            assertEquals(45.0, this.productionService.getResourceProductions(water).getFirst().usage());

            FactoryOverviewData overview = this.productionService.getFactoryOverview();
            assertEquals(120.0, this.overviewUsage(overview, "iron"));
            assertEquals(75.0, this.overviewUsage(overview, "iron_ingot"));
            assertEquals(15.0, this.overviewUsage(overview, "coal"));
            assertEquals(45.0, this.overviewUsage(overview, "water"));
        }
    }

    private ProductionMaterialSource source(String itemKey, String group)
    {
        return new ProductionMaterialSource().setItemKey(itemKey).setGroup(group);
    }

    private double overviewUsage(FactoryOverviewData overview, String itemKey)
    {
        return overview.materials().stream()
            .filter(material -> itemKey.equals(material.item().getKey()))
            .findFirst()
            .orElseThrow()
            .usage();
    }
}
