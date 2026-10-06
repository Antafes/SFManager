package antafes.sfmanager.service;

import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.entity.Item;
import antafes.sfmanager.entity.Production;
import antafes.sfmanager.entity.Recipe;
import antafes.sfmanager.entity.Category;
import antafes.sfmanager.repository.BuildingRepository;
import antafes.sfmanager.repository.ItemRepository;
import antafes.sfmanager.repository.ProductionRepository;
import antafes.sfmanager.repository.RecipeRepository;
import antafes.sfmanager.service.event.ProductionSavedEvent;
import antafes.sfmanager.service.event.ProductionDeletedEvent;
import antafes.sfmanager.service.dto.FactoryOverviewData;
import antafes.sfmanager.service.dto.FactoryOverviewMaterial;
import antafes.sfmanager.service.dto.FactoryOverviewPowerSource;
import antafes.sfmanager.service.dto.GeneratorProductionEntry;
import antafes.sfmanager.service.dto.GeneratorProductionIngredient;
import antafes.sfmanager.service.dto.RecipeProductionEntry;
import antafes.sfmanager.service.dto.RecipeProductionMaterial;
import antafes.sfmanager.service.dto.ResourceProductionEntry;
import antafes.sfmanager.util.PowerCalculator;
import antafes.sfmanager.util.ProductionCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductionService
{
    private static final List<String> MINER_BUILDING_KEYS = List.of("miner_1", "miner_2", "miner_3");

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
    @Autowired
    private ObjectProvider<StorageService> storageServiceProvider;

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

    public List<Building> getGeneratorBuildingsWithProductions()
    {
        if (this.factoryService.getCurrentFactory() == null) {
            return List.of();
        }

        Map<String, Building> generatorBuildings = new LinkedHashMap<>();
        for (Production production : this.getCurrentFactoryProductions()) {
            Building building = production.getSelectedBuilding();
            if (building == null) {
                throw new IllegalStateException("A factory production has no selected building.");
            }
            if (building.getCategory() == Category.POWER) {
                if (building.getKey() == null) {
                    throw new IllegalStateException("A power production building has no key.");
                }
                generatorBuildings.putIfAbsent(building.getKey(), building);
            }
        }
        return List.copyOf(generatorBuildings.values());
    }

    public List<GeneratorProductionEntry> getGeneratorProductions(Building building)
    {
        if (building == null || building.getKey() == null || building.getCategory() != Category.POWER) {
            throw new IllegalArgumentException("A power generator building is required.");
        }
        if (this.factoryService.getCurrentFactory() == null) {
            return List.of();
        }

        return this.getCurrentFactoryProductions().stream()
            .filter(production -> production.getSelectedBuilding() != null
                && building.getKey().equals(production.getSelectedBuilding().getKey()))
            .map(this::toGeneratorProductionEntry)
            .toList();
    }

    public List<Item> getRecipeItemsWithProductions()
    {
        if (this.factoryService.getCurrentFactory() == null) {
            return List.of();
        }

        Map<String, Item> recipeItems = new LinkedHashMap<>();
        for (Production production : this.getCurrentFactoryProductions()) {
            Building building = production.getSelectedBuilding();
            if (building == null) {
                throw new IllegalStateException("A factory production has no selected building.");
            }
            if (building.getCategory() != Category.POWER
                && production.getSelectedItem() == null
                && production.getSelectedRecipe() != null) {
                Recipe recipe = production.getSelectedRecipe();
                if (recipe.getProducts() == null) {
                    continue;
                }
                for (antafes.sfmanager.entity.recipe.Item product : recipe.getProducts()) {
                    Item item = product.getResource();
                    if (item == null || item.getKey() == null) {
                        throw new IllegalStateException("A recipe production output has no material.");
                    }
                    recipeItems.putIfAbsent(item.getKey(), item);
                }
            }
        }
        return List.copyOf(recipeItems.values());
    }

    public List<RecipeProductionEntry> getRecipeProductions(Item item)
    {
        if (item == null || item.getKey() == null) {
            throw new IllegalArgumentException("A recipe output item is required.");
        }
        if (this.factoryService.getCurrentFactory() == null) {
            return List.of();
        }

        return this.getCurrentFactoryProductions().stream()
            .filter(production -> production.getSelectedBuilding() != null
                && production.getSelectedBuilding().getCategory() != Category.POWER
                && production.getSelectedItem() == null
                && production.getSelectedRecipe() != null
                && production.getSelectedRecipe().getProducts() != null
                && production.getSelectedRecipe().getProducts().stream()
                    .anyMatch(product -> product.getResource() != null
                        && item.getKey().equals(product.getResource().getKey())))
            .map(this::toRecipeProductionEntry)
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

    public Production getProductionForEditing(UUID productionId)
    {
        return this.getCurrentFactoryProduction(productionId);
    }

    public List<Building> getMinerUpgradeBuildings(UUID productionId)
    {
        Production production = this.getCurrentFactoryProduction(productionId);
        String buildingKey = production.getSelectedBuilding().getKey();
        int currentMark = this.getMinerMark(buildingKey);
        if (currentMark == 0 || currentMark == MINER_BUILDING_KEYS.size()) {
            return List.of();
        }
        return MINER_BUILDING_KEYS.subList(currentMark, MINER_BUILDING_KEYS.size()).stream()
            .map(this.buildingRepository::findOne)
            .toList();
    }

    public FactoryOverviewData getFactoryOverview()
    {
        if (this.factoryService.getCurrentFactory() == null) {
            return new FactoryOverviewData(List.of(), List.of(), 0, 0);
        }

        List<Production> productions = this.getCurrentFactoryProductions();
        Map<UUID, Production> productionsById = productions.stream()
            .filter(production -> production.getId() != null)
            .collect(Collectors.toMap(Production::getId, Function.identity()));
        Map<String, MutableOverviewMaterial> materials = new LinkedHashMap<>();
        Map<String, Double> storageUsage = this.getStorageUsageByItem();
        Map<String, MutablePowerSource> powerSources = new LinkedHashMap<>();
        double totalPowerProduction = 0;
        double totalPowerConsumption = 0;

        for (Production production : productions) {
            Building building = production.getSelectedBuilding();
            if (building == null) {
                throw new IllegalStateException("A factory production has no selected building.");
            }

            if (building.getCategory() == Category.POWER) {
                double power = building.getPower() * this.getClockSpeed(production) / 100;
                totalPowerProduction += power;
                MutablePowerSource source = powerSources.computeIfAbsent(
                    building.getKey(),
                    ignored -> new MutablePowerSource(building)
                );
                source.production += power;
                source.amount += 1;
                continue;
            }

            double powerConsumption = this.calculateBuildingConsumption(production);
            totalPowerConsumption += powerConsumption;
            if (production.getSelectedItem() != null) {
                Item item = production.getSelectedItem();
                ResourceProductionEntry entry = this.toResourceProductionEntry(item, production, productionsById);
                double output;
                if (entry.pressurizer() == null) {
                    output = ProductionCalculator.calculateResourceProduction(
                        (double) entry.baseAmount(),
                        production.getPurity(),
                        this.getClockSpeed(production),
                        production.getUsedSomersloops(),
                        building.getSomersloopSlots()
                    );
                } else {
                    Production pressurizer = entry.pressurizer();
                    output = ProductionCalculator.calculateResourceProduction(
                        (double) entry.baseAmount(),
                        production.getPurity(),
                        this.getClockSpeed(pressurizer),
                        pressurizer.getUsedSomersloops(),
                        pressurizer.getSelectedBuilding().getSomersloopSlots()
                    );
                }
                this.addMaterialProduction(materials, item, output, powerConsumption);
                continue;
            }

            Recipe recipe = production.getSelectedRecipe();
            if (recipe == null || recipe.getProducts() == null || recipe.getProducts().isEmpty()) {
                continue;
            }
            double productPowerConsumption = powerConsumption / recipe.getProducts().size();
            for (antafes.sfmanager.entity.recipe.Item product : recipe.getProducts()) {
                double output = ProductionCalculator.calculateProduction(
                    product.getAmount(),
                    this.getClockSpeed(production),
                    production.getUsedSomersloops(),
                    building.getSomersloopSlots()
                );
                this.addMaterialProduction(
                    materials,
                    product.getResource(),
                    output,
                    productPowerConsumption
                );
            }
        }

        List<FactoryOverviewMaterial> materialRows = materials.values().stream()
            .filter(material -> material.production > 0)
            .map(material -> new FactoryOverviewMaterial(
                material.item,
                material.production,
                storageUsage.getOrDefault(material.item.getKey(), 0.0),
                material.powerConsumption
            ))
            .toList();
        List<FactoryOverviewPowerSource> powerSourceRows = powerSources.values().stream()
            .map(source -> new FactoryOverviewPowerSource(source.building, source.production, source.amount))
            .toList();
        return new FactoryOverviewData(
            materialRows,
            powerSourceRows,
            totalPowerProduction,
            totalPowerConsumption
        );
    }

    public Production saveProduction(Production production) throws IOException
    {
        return this.saveProductions(production, 1).get(0);
    }

    public List<Production> saveProductions(Production production, int productionCount) throws IOException
    {
        Factory currentFactory = this.factoryService.getCurrentFactory();
        if (currentFactory == null) {
            throw new IllegalStateException("Open a factory before saving a production.");
        }
        List<String> factoryProductions = currentFactory.getProductions();
        if (factoryProductions == null) {
            throw new IllegalStateException("The active factory has no production list.");
        }
        if (productionCount <= 0) {
            throw new IllegalArgumentException("Production count must be a positive integer.");
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

        List<Production> newProductions = new ArrayList<>(productionCount);
        production.setId(UUID.randomUUID());
        newProductions.add(production);
        for (int index = 1; index < productionCount; index++) {
            newProductions.add(this.copyProduction(production).setId(UUID.randomUUID()));
        }
        List<String> productionIds = newProductions.stream()
            .map(newProduction -> newProduction.getId().toString())
            .toList();
        factoryProductions.addAll(productionIds);

        boolean productionsSaved = false;
        boolean factorySaveAttempted = false;
        try {
            this.productionRepository.saveProductions(currentFactory, newProductions);
            productionsSaved = true;
            factorySaveAttempted = true;
            this.factoryService.saveFactory(currentFactory);
        } catch (IOException | RuntimeException failure) {
            factoryProductions.removeAll(productionIds);
            if (productionsSaved) {
                try {
                    this.productionRepository.deleteProductions(
                        currentFactory,
                        newProductions.stream().map(Production::getId).toList()
                    );
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
        newProductions.forEach(newProduction ->
            this.eventPublisher.publishEvent(new ProductionSavedEvent(newProduction))
        );
        return List.copyOf(newProductions);
    }

    public List<Production> deleteProduction(UUID productionId) throws IOException
    {
        if (productionId == null) {
            throw new IllegalArgumentException("A production ID is required.");
        }
        Factory currentFactory = this.factoryService.getCurrentFactory();
        if (currentFactory == null) {
            throw new IllegalStateException("Open a factory before deleting a production.");
        }
        List<String> factoryProductionIds = currentFactory.getProductions();
        if (factoryProductionIds == null || !factoryProductionIds.contains(productionId.toString())) {
            throw new IllegalArgumentException("The production does not belong to the active factory.");
        }

        Production production = this.productionRepository.findOne(productionId);
        if (production == null) {
            throw new IllegalStateException("The production assigned to the active factory could not be loaded.");
        }
        List<Production> productionsToDelete = new ArrayList<>();
        productionsToDelete.add(production);
        if (production.getSelectedBuilding() != null
            && "well_pressurizer".equals(production.getSelectedBuilding().getKey())) {
            this.getCurrentFactoryProductions().stream()
                .filter(candidate -> productionId.equals(candidate.getPressurizerId()))
                .forEach(productionsToDelete::add);
        }
        List<String> productionIds = productionsToDelete.stream()
            .map(candidate -> candidate.getId().toString())
            .toList();
        List<String> originalProductionIds = List.copyOf(factoryProductionIds);
        factoryProductionIds.removeAll(productionIds);

        boolean productionsDeleted = false;
        boolean factorySaveAttempted = false;
        try {
            this.productionRepository.deleteProductions(
                currentFactory,
                productionsToDelete.stream().map(Production::getId).toList()
            );
            productionsDeleted = true;
            factorySaveAttempted = true;
            this.factoryService.saveFactory(currentFactory);
        } catch (IOException | RuntimeException failure) {
            factoryProductionIds.clear();
            factoryProductionIds.addAll(originalProductionIds);
            if (productionsDeleted) {
                try {
                    this.productionRepository.saveProductions(currentFactory, productionsToDelete);
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

        this.eventPublisher.publishEvent(new ProductionDeletedEvent(productionsToDelete));
        return List.copyOf(productionsToDelete);
    }

    public Production editProduction(
        UUID productionId,
        double clockSpeed,
        String group,
        String position,
        int usedSomersloops
    ) throws IOException
    {
        Production production = this.getCurrentFactoryProduction(productionId);
        Building building = production.getSelectedBuilding();
        if (!building.isOverclockable() && Double.compare(clockSpeed, production.getClockSpeed()) != 0) {
            throw new IllegalArgumentException("The selected building cannot be overclocked.");
        }

        Double originalClockSpeed = production.getClockSpeed();
        String originalGroup = production.getGroup();
        String originalPosition = production.getPosition();
        int originalUsedSomersloops = production.getUsedSomersloops();
        production
            .setClockSpeed(clockSpeed)
            .setGroup(this.normalizeOptionalText(group))
            .setPosition(this.normalizeOptionalText(position))
            .setUsedSomersloops(usedSomersloops);
        try {
            this.validateProduction(production, this.factoryService.getCurrentFactory());
            this.productionRepository.updateProduction(this.factoryService.getCurrentFactory(), production);
        } catch (IOException | RuntimeException failure) {
            production
                .setClockSpeed(originalClockSpeed)
                .setGroup(originalGroup)
                .setPosition(originalPosition)
                .setUsedSomersloops(originalUsedSomersloops);
            throw failure;
        }
        this.eventPublisher.publishEvent(new ProductionSavedEvent(production));
        return production;
    }

    public Production upgradeMiner(UUID productionId, String targetBuildingKey) throws IOException
    {
        Production production = this.getCurrentFactoryProduction(productionId);
        String currentBuildingKey = production.getSelectedBuilding().getKey();
        int currentMark = this.getMinerMark(currentBuildingKey);
        int targetMark = this.getMinerMark(targetBuildingKey);
        if (currentMark == 0 || targetMark <= currentMark) {
            throw new IllegalArgumentException("The selected miner mark is not a valid upgrade.");
        }

        Building targetBuilding = this.buildingRepository.findOne(targetBuildingKey);
        if (targetBuilding == null || !targetBuilding.isResourceBuilding()) {
            throw new IllegalArgumentException("The selected miner is not available.");
        }

        Building currentBuilding = production.getSelectedBuilding();
        production.setSelectedBuilding(targetBuilding);
        try {
            this.productionRepository.updateProduction(this.factoryService.getCurrentFactory(), production);
        } catch (IOException | RuntimeException failure) {
            production.setSelectedBuilding(currentBuilding);
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

    private double calculateBuildingConsumption(Production production)
    {
        Building building = production.getSelectedBuilding();
        if (building.getPower() >= 0) {
            return 0;
        }
        return PowerCalculator.calculateConsumption(
            (double) Math.abs(building.getPower()),
            this.getClockSpeed(production),
            production.getUsedSomersloops(),
            building.getSomersloopSlots()
        );
    }

    private GeneratorProductionEntry toGeneratorProductionEntry(Production production)
    {
        Building building = production.getSelectedBuilding();
        double clockSpeed = this.getClockSpeed(production);
        List<GeneratorProductionIngredient> ingredients = List.of();
        Recipe recipe = production.getSelectedRecipe();
        if (recipe != null && recipe.getIngredients() != null) {
            ingredients = recipe.getIngredients().stream()
                .map(ingredient -> {
                    if (ingredient.getResource() == null || ingredient.getAmount() == null) {
                        throw new IllegalStateException("A generator recipe ingredient is incomplete.");
                    }
                    return new GeneratorProductionIngredient(
                        ingredient.getResource(),
                        ingredient.getAmount() * clockSpeed / 100
                    );
                })
                .toList();
        }
        return new GeneratorProductionEntry(
            production,
            building.getPower() * clockSpeed / 100,
            ingredients
        );
    }

    private RecipeProductionEntry toRecipeProductionEntry(Production production)
    {
        Building building = production.getSelectedBuilding();
        Recipe recipe = production.getSelectedRecipe();
        double clockSpeed = this.getClockSpeed(production);
        List<RecipeProductionMaterial> usedMaterials = this.scaleRecipeMaterials(
            recipe.getIngredients(),
            clockSpeed,
            production.getUsedSomersloops(),
            building.getSomersloopSlots()
        );
        List<RecipeProductionMaterial> producedMaterials = this.scaleRecipeMaterials(
            recipe.getProducts(),
            clockSpeed,
            production.getUsedSomersloops(),
            building.getSomersloopSlots()
        );
        return new RecipeProductionEntry(
            production,
            recipe,
            usedMaterials,
            producedMaterials,
            this.calculateBuildingConsumption(production)
        );
    }

    private List<RecipeProductionMaterial> scaleRecipeMaterials(
        List<antafes.sfmanager.entity.recipe.Item> materials,
        double clockSpeed,
        int usedSomersloops,
        int somersloopSlots
    )
    {
        if (materials == null) {
            return List.of();
        }
        return materials.stream()
            .map(material -> {
                if (material.getResource() == null || material.getAmount() == null) {
                    throw new IllegalStateException("A recipe material is incomplete.");
                }
                return new RecipeProductionMaterial(
                    material.getResource(),
                    ProductionCalculator.calculateProduction(
                        material.getAmount(),
                        clockSpeed,
                        usedSomersloops,
                        somersloopSlots
                    )
                );
            })
            .toList();
    }

    private Production copyProduction(Production production)
    {
        return new Production()
            .setSelectedBuilding(production.getSelectedBuilding())
            .setSelectedRecipe(production.getSelectedRecipe())
            .setSelectedItem(production.getSelectedItem())
            .setGroup(production.getGroup())
            .setPosition(production.getPosition())
            .setPurity(production.getPurity())
            .setPressurizerId(production.getPressurizerId())
            .setClockSpeed(production.getClockSpeed())
            .setUsedSomersloops(production.getUsedSomersloops());
    }

    private Production getCurrentFactoryProduction(UUID productionId)
    {
        if (productionId == null) {
            throw new IllegalArgumentException("A production ID is required.");
        }
        Factory currentFactory = this.factoryService.getCurrentFactory();
        if (currentFactory == null) {
            throw new IllegalStateException("Open a factory before updating a production.");
        }
        if (!currentFactory.getProductions().contains(productionId.toString())) {
            throw new IllegalArgumentException("The production does not belong to the active factory.");
        }
        Production production = this.productionRepository.findOne(productionId);
        if (production == null || production.getSelectedBuilding() == null
            || production.getSelectedBuilding().getKey() == null) {
            throw new IllegalStateException("The production assigned to the active factory could not be loaded.");
        }
        return production;
    }

    private int getMinerMark(String buildingKey)
    {
        return MINER_BUILDING_KEYS.indexOf(buildingKey) + 1;
    }

    private String normalizeOptionalText(String value)
    {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }

    private double getClockSpeed(Production production)
    {
        if (production.getClockSpeed() == null || !Double.isFinite(production.getClockSpeed())) {
            throw new IllegalStateException("A factory production has an invalid clock speed.");
        }
        return production.getClockSpeed();
    }

    private void addMaterialProduction(
        Map<String, MutableOverviewMaterial> materials,
        Item item,
        double production,
        double powerConsumption
    )
    {
        if (item == null || item.getKey() == null) {
            throw new IllegalStateException("A production output has no material.");
        }
        MutableOverviewMaterial material = materials.computeIfAbsent(
            item.getKey(),
            ignored -> new MutableOverviewMaterial(item)
        );
        material.production += production;
        material.powerConsumption += powerConsumption;
    }

    private Map<String, Double> getStorageUsageByItem()
    {
        StorageService storageService = this.storageServiceProvider.getIfAvailable();
        if (storageService == null) {
            return Map.of();
        }
        return storageService.getStoredItems().stream()
            .collect(Collectors.toMap(
                storageItem -> storageItem.item().getKey(),
                storageItem -> storageItem.rate(),
                Double::sum
            ));
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

    private static class MutableOverviewMaterial
    {
        private final Item item;
        private double production;
        private double powerConsumption;

        private MutableOverviewMaterial(Item item)
        {
            this.item = item;
        }
    }

    private static class MutablePowerSource
    {
        private final Building building;
        private double production;
        private double amount;

        private MutablePowerSource(Building building)
        {
            this.building = building;
        }
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
