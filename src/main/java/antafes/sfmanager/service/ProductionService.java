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
import antafes.sfmanager.service.dto.FactoryOverviewData;
import antafes.sfmanager.service.dto.FactoryOverviewMaterial;
import antafes.sfmanager.service.dto.FactoryOverviewPowerSource;
import antafes.sfmanager.service.dto.ResourceProductionEntry;
import antafes.sfmanager.util.PowerCalculator;
import antafes.sfmanager.util.ProductionCalculator;
import org.springframework.beans.factory.annotation.Autowired;
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
