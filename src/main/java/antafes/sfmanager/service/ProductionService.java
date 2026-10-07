package antafes.sfmanager.service;

import antafes.sfmanager.entity.*;
import antafes.sfmanager.repository.BuildingRepository;
import antafes.sfmanager.repository.ItemRepository;
import antafes.sfmanager.repository.ProductionRepository;
import antafes.sfmanager.repository.RecipeRepository;
import antafes.sfmanager.service.dto.*;
import antafes.sfmanager.service.event.ProductionDeletedEvent;
import antafes.sfmanager.service.event.ProductionSavedEvent;
import antafes.sfmanager.util.PowerCalculator;
import antafes.sfmanager.util.ProductionCalculator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
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

    private double getMaterialSourceProduction(
        Production production,
        Item item,
        Map<UUID, Production> productionsById
    )
    {
        Building building = production.getSelectedBuilding();
        if (production.getSelectedItem() != null) {
            ResourceProductionEntry entry = this.toResourceProductionEntry(item, production, productionsById);
            if (entry.pressurizer() == null) {
                return ProductionCalculator.calculateResourceProduction(
                    (double) entry.baseAmount(),
                    production.getPurity(),
                    this.getClockSpeed(production),
                    production.getUsedSomersloops(),
                    building.getSomersloopSlots()
                );
            }
            Production pressurizer = entry.pressurizer();
            return ProductionCalculator.calculateResourceProduction(
                (double) entry.baseAmount(),
                production.getPurity(),
                this.getClockSpeed(pressurizer),
                pressurizer.getUsedSomersloops(),
                pressurizer.getSelectedBuilding().getSomersloopSlots()
            );
        }

        return production.getSelectedRecipe().getProducts().stream()
            .filter(product -> product.getResource() != null && item.getKey().equals(product.getResource().getKey()))
            .mapToDouble(product -> ProductionCalculator.calculateProduction(
                product.getAmount(),
                this.getClockSpeed(production),
                production.getUsedSomersloops(),
                building.getSomersloopSlots()
            ))
            .sum();
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
        Map<MaterialOutput, Double> materialUsage = this.getMaterialUsageByOutput(
            factoryProductions,
            productionsById
        );

        return factoryProductions.stream()
            .filter(production -> production.getSelectedItem() != null
                && item.getKey().equals(production.getSelectedItem().getKey()))
            .map(production -> {
                ResourceProductionEntry entry = this.toResourceProductionEntry(item, production, productionsById);
                return new ResourceProductionEntry(
                    entry.production(),
                    entry.baseAmount(),
                    entry.pressurizer(),
                    materialUsage.getOrDefault(new MaterialOutput(production.getId(), item.getKey()), 0.0)
                );
            })
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

        List<Production> productions = this.getCurrentFactoryProductions();
        Map<UUID, Production> productionsById = productions.stream()
            .filter(production -> production.getId() != null)
            .collect(Collectors.toMap(Production::getId, Function.identity()));
        Map<MaterialOutput, Double> materialUsage = this.getMaterialUsageByOutput(
            productions,
            productionsById
        );
        return productions.stream()
            .filter(production -> production.getSelectedBuilding() != null
                && production.getSelectedBuilding().getCategory() != Category.POWER
                && production.getSelectedItem() == null
                && production.getSelectedRecipe() != null
                && production.getSelectedRecipe().getProducts() != null
                && production.getSelectedRecipe().getProducts().stream()
                    .anyMatch(product -> product.getResource() != null
                        && item.getKey().equals(product.getResource().getKey())))
            .map(production -> this.toRecipeProductionEntry(
                production,
                materialUsage.getOrDefault(new MaterialOutput(production.getId(), item.getKey()), 0.0)
            ))
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

    public List<MaterialSourceGroup> getMaterialSourceGroups(Item item)
    {
        if (item == null || item.getKey() == null) {
            throw new IllegalArgumentException("A material item is required.");
        }

        List<Production> productions = this.getCurrentFactoryProductions();
        Map<UUID, Production> productionsById = productions.stream()
            .filter(production -> production.getId() != null)
            .collect(Collectors.toMap(Production::getId, Function.identity()));
        return productions.stream()
            .filter(production -> this.producesMaterial(production, item.getKey()))
            .filter(production -> production.getGroup() != null && !production.getGroup().isBlank())
            .collect(Collectors.groupingBy(
                Production::getGroup,
                () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER),
                Collectors.summingDouble(production ->
                    this.getMaterialSourceProduction(production, item, productionsById)
                )
            ))
            .entrySet().stream()
            .map(entry -> new MaterialSourceGroup(entry.getKey(), entry.getValue()))
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
        Map<MaterialOutput, Double> materialUsage = this.getMaterialUsageByOutput(
            productions,
            productionsById
        );
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

        Map<String, Double> allocatedUsageByItem = materialUsage.entrySet().stream().collect(Collectors.groupingBy(
            entry -> entry.getKey().itemKey(),
            Collectors.summingDouble(Map.Entry::getValue)
        ));
        List<FactoryOverviewMaterial> materialRows = materials.values().stream()
            .filter(material -> material.production > 0)
            .map(material -> new FactoryOverviewMaterial(
                material.item,
                material.production,
                allocatedUsageByItem.getOrDefault(material.item.getKey(), 0.0)
                    + storageUsage.getOrDefault(material.item.getKey(), 0.0),
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
        return this.editProduction(
            productionId,
            clockSpeed,
            group,
            position,
            usedSomersloops,
            this.getCurrentFactoryProduction(productionId).getMaterialSources()
        );
    }

    public Production editProduction(
        UUID productionId,
        double clockSpeed,
        String group,
        String position,
        int usedSomersloops,
        List<ProductionMaterialSource> materialSources
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
        List<ProductionMaterialSource> originalMaterialSources = this.copyMaterialSources(
            production.getMaterialSources()
        );
        production
            .setClockSpeed(clockSpeed)
            .setGroup(this.normalizeOptionalText(group))
            .setPosition(this.normalizeOptionalText(position))
            .setUsedSomersloops(usedSomersloops)
            .setMaterialSources(this.copyMaterialSources(materialSources));
        try {
            this.validateProduction(production, this.factoryService.getCurrentFactory());
            this.productionRepository.updateProduction(this.factoryService.getCurrentFactory(), production);
        } catch (IOException | RuntimeException failure) {
            production
                .setClockSpeed(originalClockSpeed)
                .setGroup(originalGroup)
                .setPosition(originalPosition)
                .setUsedSomersloops(originalUsedSomersloops)
                .setMaterialSources(originalMaterialSources);
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
        } else {
            if (production.getSelectedItem() != null) {
                throw new IllegalArgumentException("The selected item is not available for this building.");
            }
        }
        Recipe selectedRecipe = null;
        if (production.getSelectedRecipe() != null) {
            if (production.getSelectedRecipe().getKey() == null) {
                throw new IllegalArgumentException("The selected recipe is not available for this building.");
            }
            selectedRecipe = this.recipeRepository.findOne(production.getSelectedRecipe().getKey());
            if (selectedRecipe == null || selectedRecipe.getProducedIn() == null
                || !selectedBuilding.getKey().equals(selectedRecipe.getProducedIn().getKey())) {
                throw new IllegalArgumentException("The selected recipe is not available for this building.");
            }
        }
        this.validateMaterialSources(production, selectedRecipe);
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
                        ProductionCalculator.calculateProduction(
                            ingredient.getAmount(),
                            clockSpeed,
                            production.getUsedSomersloops(),
                            building.getSomersloopSlots()
                        ),
                        this.getMaterialSourceGroup(production, ingredient.getResource().getKey())
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

    private RecipeProductionEntry toRecipeProductionEntry(Production production, double usage)
    {
        Building building = production.getSelectedBuilding();
        Recipe recipe = production.getSelectedRecipe();
        double clockSpeed = this.getClockSpeed(production);
        List<RecipeProductionMaterial> usedMaterials = this.scaleRecipeMaterials(
            recipe.getIngredients(),
            clockSpeed,
            production.getUsedSomersloops(),
            building.getSomersloopSlots(),
            production
        );
        List<RecipeProductionMaterial> producedMaterials = this.scaleRecipeMaterials(
            recipe.getProducts(),
            clockSpeed,
            production.getUsedSomersloops(),
            building.getSomersloopSlots(),
            null
        );
        return new RecipeProductionEntry(
            production,
            recipe,
            usedMaterials,
            producedMaterials,
            usage,
            this.calculateBuildingConsumption(production)
        );
    }

    private Map<MaterialOutput, Double> getMaterialUsageByOutput(
        List<Production> productions,
        Map<UUID, Production> productionsById
    )
    {
        Map<MaterialOutput, Double> remainingOutputs = new HashMap<>();
        for (Production production : productions) {
            if (production.getId() == null || production.getGroup() == null || production.getGroup().isBlank()) {
                continue;
            }
            if (production.getSelectedItem() != null) {
                Item item = production.getSelectedItem();
                remainingOutputs.put(
                    new MaterialOutput(production.getId(), item.getKey()),
                    this.getMaterialSourceProduction(production, item, productionsById)
                );
                continue;
            }
            Building building = production.getSelectedBuilding();
            Recipe recipe = production.getSelectedRecipe();
            if (building == null || building.getCategory() == Category.POWER
                || recipe == null || recipe.getProducts() == null) {
                continue;
            }
            for (antafes.sfmanager.entity.recipe.Item product : recipe.getProducts()) {
                Item item = product.getResource();
                if (item != null && item.getKey() != null) {
                    remainingOutputs.put(
                        new MaterialOutput(production.getId(), item.getKey()),
                        this.getMaterialSourceProduction(production, item, productionsById)
                    );
                }
            }
        }

        Map<MaterialOutput, Double> usage = new HashMap<>();
        for (Production production : productions) {
            Recipe recipe = production.getSelectedRecipe();
            Building building = production.getSelectedBuilding();
            if (recipe == null || building == null || recipe.getIngredients() == null) {
                continue;
            }
            for (antafes.sfmanager.entity.recipe.Item ingredient : recipe.getIngredients()) {
                Item item = ingredient.getResource();
                if (item == null || item.getKey() == null || ingredient.getAmount() == null) {
                    throw new IllegalStateException("A recipe ingredient is incomplete.");
                }
                String sourceGroup = this.getMaterialSourceGroup(production, item.getKey());
                if (sourceGroup == null || sourceGroup.isBlank()) {
                    continue;
                }
                double remainingDemand = ProductionCalculator.calculateProduction(
                    ingredient.getAmount(),
                    this.getClockSpeed(production),
                    production.getUsedSomersloops(),
                    building.getSomersloopSlots()
                );
                for (Production source : productions) {
                    if (remainingDemand <= 0 || !sourceGroup.equals(source.getGroup())
                        || !this.producesMaterial(source, item.getKey())) {
                        continue;
                    }
                    MaterialOutput output = new MaterialOutput(source.getId(), item.getKey());
                    double available = remainingOutputs.getOrDefault(output, 0.0);
                    double allocated = Math.min(remainingDemand, available);
                    if (allocated <= 0) {
                        continue;
                    }
                    usage.merge(output, allocated, Double::sum);
                    remainingOutputs.put(output, available - allocated);
                    remainingDemand -= allocated;
                }
            }
        }
        return usage;
    }

    private List<RecipeProductionMaterial> scaleRecipeMaterials(
        List<antafes.sfmanager.entity.recipe.Item> materials,
        double clockSpeed,
        int usedSomersloops,
        int somersloopSlots,
        Production sourceProduction
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
                    ),
                    sourceProduction == null
                        ? null
                        : this.getMaterialSourceGroup(sourceProduction, material.getResource().getKey())
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
            .setMaterialSources(this.copyMaterialSources(production.getMaterialSources()))
            .setGroup(production.getGroup())
            .setPosition(production.getPosition())
            .setPurity(production.getPurity())
            .setPressurizerId(production.getPressurizerId())
            .setClockSpeed(production.getClockSpeed())
            .setUsedSomersloops(production.getUsedSomersloops());
    }

    private List<ProductionMaterialSource> copyMaterialSources(
        List<ProductionMaterialSource> materialSources
    )
    {
        if (materialSources == null) {
            return null;
        }
        return materialSources.stream()
            .map(materialSource -> new ProductionMaterialSource()
                .setItemKey(materialSource.getItemKey())
                .setGroup(materialSource.getGroup()))
            .toList();
    }

    private String getMaterialSourceGroup(Production production, String itemKey)
    {
        if (production.getMaterialSources() == null) {
            return null;
        }
        return production.getMaterialSources().stream()
            .filter(materialSource -> itemKey.equals(materialSource.getItemKey()))
            .map(ProductionMaterialSource::getGroup)
            .findFirst()
            .orElse(null);
    }

    private boolean producesMaterial(Production production, String itemKey)
    {
        if (production.getSelectedItem() != null) {
            Building building = production.getSelectedBuilding();
            return building != null
                && building.isResourceBuilding()
                && itemKey.equals(production.getSelectedItem().getKey());
        }

        Building building = production.getSelectedBuilding();
        Recipe recipe = production.getSelectedRecipe();
        return building != null
            && building.getCategory() != Category.POWER
            && recipe != null
            && recipe.getProducts() != null
            && recipe.getProducts().stream()
                .map(antafes.sfmanager.entity.recipe.Item::getResource)
                .filter(Objects::nonNull)
                .anyMatch(item -> itemKey.equals(item.getKey()));
    }

    private void validateMaterialSources(Production production, Recipe recipe)
    {
        List<ProductionMaterialSource> materialSources = production.getMaterialSources();
        if (materialSources == null) {
            return;
        }
        if (recipe == null) {
            if (materialSources != null && !materialSources.isEmpty()) {
                throw new IllegalArgumentException("Only recipe productions can have material sources.");
            }
            return;
        }

        Map<String, Item> ingredients = new LinkedHashMap<>();
        if (recipe.getIngredients() != null) {
            for (antafes.sfmanager.entity.recipe.Item ingredient : recipe.getIngredients()) {
                Item item = ingredient.getResource();
                if (item == null || item.getKey() == null) {
                    throw new IllegalStateException("A recipe ingredient has no material.");
                }
                ingredients.put(item.getKey(), item);
            }
        }
        if (materialSources == null || materialSources.size() != ingredients.size()) {
            throw new IllegalArgumentException("Every recipe ingredient requires a material source group.");
        }

        Set<String> sourceItemKeys = new HashSet<>();
        for (ProductionMaterialSource materialSource : materialSources) {
            if (materialSource == null || materialSource.getItemKey() == null
                || !sourceItemKeys.add(materialSource.getItemKey())) {
                throw new IllegalArgumentException("Every recipe ingredient requires one material source group.");
            }
            Item ingredient = ingredients.get(materialSource.getItemKey());
            String group = this.normalizeOptionalText(materialSource.getGroup());
            if (ingredient == null || group == null
                || this.getMaterialSourceGroups(ingredient).stream()
                    .noneMatch(sourceGroup -> sourceGroup.group().equals(group))) {
                throw new IllegalArgumentException(
                    "The selected material source group does not produce " + materialSource.getItemKey() + "."
                );
            }
            materialSource.setGroup(group);
        }
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
        return new ResourceProductionEntry(production, mapping.getAmount(), pressurizer, 0);
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

    private record MaterialOutput(UUID productionId, String itemKey)
    {
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
