package antafes.sfmanager.service;

import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.entity.Item;
import antafes.sfmanager.entity.Production;
import antafes.sfmanager.entity.Recipe;
import antafes.sfmanager.repository.BuildingRepository;
import antafes.sfmanager.repository.ItemRepository;
import antafes.sfmanager.repository.ProductionRepository;
import antafes.sfmanager.repository.RecipeRepository;
import antafes.sfmanager.service.event.ProductionSavedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductionService
{
    @Autowired
    private BuildingRepository buildingRepository;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ProductionRepository productionRepository;
    @Autowired
    private FactoryService factoryService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public List<Building> getBuildings()
    {
        return List.copyOf(this.buildingRepository.findAll().values());
    }

    public List<Recipe> getRecipesForBuilding(String buildingKey)
    {
        return this.recipeRepository.findAll().values().stream()
            .filter(recipe -> recipe.getProducedIn() != null
                && recipe.getProducedIn().getKey().equals(buildingKey))
            .toList();
    }

    public List<Item> getItemsForBuilding(String buildingKey)
    {
        return this.itemRepository.findAll().values().stream()
            .filter(item -> item.getProductions() != null && item.getProductions().stream()
                .anyMatch(production -> production.getBuilding() != null
                    && Objects.equals(buildingKey, production.getBuilding().getKey())))
            .toList();
    }

    public List<Item> getResourceItems()
    {
        return this.itemRepository.findAll().values().stream()
            .filter(item -> item.getProductions() != null && !item.getProductions().isEmpty())
            .toList();
    }

    public List<Item> getResourceItemsWithProductions()
    {
        if (this.factoryService.getCurrentFactory() == null) {
            return List.of();
        }

        Map<String, Item> resourceItemsByKey = this.getResourceItems().stream()
            .collect(Collectors.toMap(Item::getKey, Function.identity()));
        return this.getCurrentFactoryProductions().stream()
            .filter(production -> production.getSelectedItem() != null
                && production.getSelectedBuilding() != null)
            .map(production -> {
                Item item = resourceItemsByKey.get(production.getSelectedItem().getKey());
                if (item == null) {
                    return null;
                }
                boolean hasBuildingMapping = item.getProductions().stream()
                    .anyMatch(mapping -> mapping.getBuilding() != null
                        && Objects.equals(
                            production.getSelectedBuilding().getKey(),
                            mapping.getBuilding().getKey()
                        ));
                return hasBuildingMapping ? item : null;
            })
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    public List<ResourceProductionEntry> getResourceProductions(Item item)
    {
        if (item == null || item.getKey() == null) {
            throw new IllegalArgumentException("A resource item is required.");
        }
        if (item.getProductions() == null || item.getProductions().isEmpty()) {
            throw new IllegalArgumentException("The item has no resource production mappings.");
        }

        Factory currentFactory = this.factoryService.getCurrentFactory();
        if (currentFactory == null) {
            return List.of();
        }
        List<Production> factoryProductions = this.getCurrentFactoryProductions();
        Map<UUID, Production> productionsById = factoryProductions.stream()
            .filter(production -> production.getId() != null)
            .collect(Collectors.toMap(Production::getId, Function.identity()));

        return factoryProductions.stream()
            .filter(production -> production.getSelectedItem() != null
                && item.getKey().equals(production.getSelectedItem().getKey()))
            .map(production -> this.toResourceProductionEntry(item, production, productionsById))
            .toList();
    }

    public List<Production> getPressurizers()
    {
        return this.getCurrentFactoryProductions().stream()
            .filter(production -> production.getSelectedBuilding() != null
                && "well_pressurizer".equals(production.getSelectedBuilding().getKey()))
            .toList();
    }

    public List<String> getProductionGroups()
    {
        return this.getCurrentFactoryProductions().stream()
            .map(Production::getGroup)
            .filter(group -> group != null && !group.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    public Production saveProduction(Production production) throws IOException
    {
        Factory currentFactory = this.factoryService.getCurrentFactory();
        if (currentFactory == null) {
            throw new IllegalStateException("Open a factory before saving a production.");
        }
        List<String> factoryProductions = currentFactory.getProductions();
        if (factoryProductions == null) {
            throw new IllegalStateException("The active factory has no production list.");
        }
        this.validateProduction(production, currentFactory);

        Building building = this.buildingRepository.findOne(production.getSelectedBuilding().getKey());
        production.setSelectedBuilding(building);
        if (production.getSelectedRecipe() != null) {
            production.setSelectedRecipe(this.recipeRepository.findOne(production.getSelectedRecipe().getKey()));
        }
        if (production.getSelectedItem() != null) {
            production.setSelectedItem(this.itemRepository.findOne(production.getSelectedItem().getKey()));
        }
        production.setId(UUID.randomUUID());

        String productionId = production.getId().toString();
        if (factoryProductions.contains(productionId)) {
            throw new IllegalStateException("The production is already assigned to the active factory.");
        }
        factoryProductions.add(productionId);

        boolean productionSaved = false;
        boolean factorySaveAttempted = false;
        try {
            this.productionRepository.saveProduction(currentFactory, production);
            productionSaved = true;
            factorySaveAttempted = true;
            this.factoryService.saveFactory(currentFactory);
        } catch (IOException | RuntimeException failure) {
            factoryProductions.remove(productionId);
            if (productionSaved) {
                try {
                    this.productionRepository.deleteProduction(currentFactory, production.getId());
                } catch (IOException | RuntimeException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
            }
            if (factorySaveAttempted) {
                try {
                    this.factoryService.saveFactory(currentFactory);
                } catch (IOException | RuntimeException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
            }
            throw failure;
        }

        this.eventPublisher.publishEvent(new ProductionSavedEvent(production));
        return production;
    }

    private void validateProduction(Production production, Factory currentFactory)
    {
        if (production == null || production.getSelectedBuilding() == null
            || production.getSelectedBuilding().getKey() == null) {
            throw new IllegalArgumentException("A production building is required.");
        }
        Building selectedBuilding = this.buildingRepository.findOne(production.getSelectedBuilding().getKey());
        if (selectedBuilding == null) {
            throw new IllegalArgumentException("The selected building is not available.");
        }
        if (production.getAmount() == null || !Double.isFinite(production.getAmount())
            || production.getAmount() < 0) {
            throw new IllegalArgumentException("Production amount must be a non-negative number.");
        }
        if (production.getClockSpeed() == null || !Double.isFinite(production.getClockSpeed())
            || production.getClockSpeed() < 0 || production.getClockSpeed() > 250) {
            throw new IllegalArgumentException("Clock speed must be between 0 and 250.");
        }
        if (production.getUsedSomersloops() < 0
            || production.getUsedSomersloops() > selectedBuilding.getSomersloopSlots()) {
            throw new IllegalArgumentException("Somersloop count exceeds the selected building's slots.");
        }
        if (selectedBuilding.isResourceBuilding() && production.getPurity() == null) {
            throw new IllegalArgumentException("Resource buildings require a purity selection.");
        }
        List<Item> buildingItems = this.getItemsForBuilding(selectedBuilding.getKey());
        if (!buildingItems.isEmpty()) {
            if (production.getSelectedItem() == null || production.getSelectedRecipe() != null) {
                throw new IllegalArgumentException("A resource item must be selected for this building.");
            }
            String itemKey = production.getSelectedItem().getKey();
            Item selectedItem = itemKey == null ? null : this.itemRepository.findOne(itemKey);
            if (selectedItem == null || buildingItems.stream()
                .noneMatch(item -> item.getKey().equals(selectedItem.getKey()))) {
                throw new IllegalArgumentException("The selected item is not available for this building.");
            }
        } else if (production.getSelectedItem() != null) {
            throw new IllegalArgumentException("The selected item is not available for this building.");
        }
        if (production.getSelectedRecipe() != null) {
            if (production.getSelectedRecipe().getKey() == null) {
                throw new IllegalArgumentException("The selected recipe is not available for this building.");
            }
            Recipe selectedRecipe = this.recipeRepository.findOne(production.getSelectedRecipe().getKey());
            if (selectedRecipe == null || selectedRecipe.getProducedIn() == null
                || !selectedBuilding.getKey().equals(selectedRecipe.getProducedIn().getKey())) {
                throw new IllegalArgumentException("The selected recipe is not available for this building.");
            }
        }
        if ("well_extractor".equals(selectedBuilding.getKey()) && production.getPressurizerId() == null) {
            throw new IllegalArgumentException("A pressurizer must be selected for a resource well extractor.");
        }
        if (production.getPressurizerId() != null) {
            Production pressurizer = this.productionRepository.findOne(production.getPressurizerId());
            if (pressurizer == null || pressurizer.getSelectedBuilding() == null
                || !"well_pressurizer".equals(pressurizer.getSelectedBuilding().getKey())
                || !currentFactory.getProductions().contains(production.getPressurizerId().toString())) {
                throw new IllegalArgumentException("The selected pressurizer production does not exist.");
            }
        }
    }

    private ResourceProductionEntry toResourceProductionEntry(
        Item item,
        Production production,
        Map<UUID, Production> productionsById
    )
    {
        if (production.getSelectedBuilding() == null) {
            throw new IllegalStateException("A resource production has no selected building.");
        }
        String buildingKey = production.getSelectedBuilding().getKey();
        antafes.sfmanager.entity.item.Production mapping = item.getProductions().stream()
            .filter(candidate -> candidate.getBuilding() != null
                && Objects.equals(buildingKey, candidate.getBuilding().getKey()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "The selected building does not produce resource item " + item.getKey() + "."
            ));

        Production pressurizer = null;
        if ("well_extractor".equals(buildingKey)) {
            pressurizer = productionsById.get(production.getPressurizerId());
            if (pressurizer == null || pressurizer.getSelectedBuilding() == null
                || !"well_pressurizer".equals(pressurizer.getSelectedBuilding().getKey())) {
                throw new IllegalStateException("A well extractor is missing its factory pressurizer production.");
            }
        }
        return new ResourceProductionEntry(production, mapping.getAmount(), pressurizer);
    }

    private List<Production> getCurrentFactoryProductions()
    {
        Factory currentFactory = this.factoryService.getCurrentFactory();
        if (currentFactory == null) {
            throw new IllegalStateException("Open a factory before loading its productions.");
        }
        if (currentFactory.getProductions() == null) {
            throw new IllegalStateException("The active factory has no production list.");
        }

        return currentFactory.getProductions().stream()
            .map(productionId -> {
                if (productionId == null) {
                    throw new IllegalStateException("The active factory contains an invalid production ID.");
                }
                UUID id;
                try {
                    id = UUID.fromString(productionId);
                } catch (IllegalArgumentException exception) {
                    throw new IllegalStateException("The active factory contains an invalid production ID.", exception);
                }
                Production production = this.productionRepository.findOne(id);
                if (production == null) {
                    throw new IllegalStateException("A production assigned to the active factory could not be loaded.");
                }
                return production;
            })
            .toList();
    }
}
