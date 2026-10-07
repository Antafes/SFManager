package antafes.sfmanager.service;

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
import antafes.sfmanager.service.dto.StorageItem;
import antafes.sfmanager.service.dto.FactoryOverviewMaterial;
import antafes.sfmanager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StorageServiceTest extends BaseTest
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
    public void testStoresExplicitProductionItemOutputAndCleansUpDeletedProduction() throws IOException
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
            StorageService.class
        )) {
            this.autowireTest(context);
            ProductionService productionService = context.getBean(ProductionService.class);
            StorageService storageService = context.getBean(StorageService.class);
            Factory factory = context.getBean(FactoryService.class).createFactory("Storage Service Factory");
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
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));
            String itemKey = recipe.getProducts().getFirst().getResource().getKey();
            Storage.Entry entry = new Storage.Entry()
                .setProductionId(production.getId())
                .setItemKey(itemKey);

            assertEquals(1, storageService.getAvailableChoices().size());
            storageService.addStorageEntries(List.of(entry));

            List<StorageItem> storageItems = storageService.getStoredItems();
            assertEquals(1, storageItems.size());
            assertEquals(itemKey, storageItems.getFirst().item().getKey());
            assertEquals(30, storageItems.getFirst().rate(), 0.0001);
            assertEquals(List.of(production.getId()), storageItems.getFirst().sources().stream()
                .map(source -> source.production().getId())
                .toList());
            assertEquals(30, storageService.getStoredRate(production.getId(), itemKey), 0.0001);
            FactoryOverviewMaterial overviewMaterial = productionService.getFactoryOverview().materials().stream()
                .filter(material -> itemKey.equals(material.item().getKey()))
                .findFirst()
                .orElseThrow();
            assertEquals(30, overviewMaterial.usage(), 0.0001);
            assertEquals(0, overviewMaterial.production() - overviewMaterial.usage(), 0.0001);
            assertTrue(storageService.getAvailableChoices().isEmpty());
            assertThrows(
                IllegalArgumentException.class,
                () -> storageService.addStorageEntries(List.of(entry))
            );

            storageService.removeStorageEntries(List.of(entry));
            assertTrue(storageService.getStoredItems().isEmpty());
            assertEquals(1, storageService.getAvailableChoices().size());

            storageService.addStorageEntries(List.of(entry));
            productionService.deleteProduction(production.getId());
            assertTrue(storageService.getStoredItems().isEmpty());
        }
    }
}
