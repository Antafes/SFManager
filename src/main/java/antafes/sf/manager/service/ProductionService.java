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

import antafes.sf.manager.entity.*;
import antafes.sf.manager.repository.BuildingRepository;
import antafes.sf.manager.repository.ItemRepository;
import antafes.sf.manager.repository.ProductionRepository;
import antafes.sf.manager.repository.RecipeRepository;
import antafes.sf.manager.service.dto.*;
import antafes.sf.manager.service.event.ProductionDeletedEvent;
import antafes.sf.manager.service.event.ProductionSavedEvent;
import antafes.sf.manager.service.event.ProductionsSavedEvent;
import antafes.sf.manager.util.NaturalOrder;
import antafes.sf.manager.util.PowerCalculator;
import antafes.sf.manager.util.ProductionCalculator;
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
        Map<MaterialOutput, Double> materialUsage = this.getMaterialUsageWithStorage(
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

    public List<Building> getConsumerBuildingsWithProductions()
    {
        if (this.factoryService.getCurrentFactory() == null) {
            return List.of();
        }

        Map<String, Building> consumerBuildings = new LinkedHashMap<>();
        for (Production production : this.getCurrentFactoryProductions()) {
            Building building = production.getSelectedBuilding();
            if (building == null) {
                throw new IllegalStateException("A factory production has no selected building.");
            }
            if (building.getCategory() != null && building.getCategory().isConsumer()) {
                if (building.getKey() == null) {
                    throw new IllegalStateException("A consumer building has no key.");
                }
                consumerBuildings.putIfAbsent(building.getKey(), building);
            }
        }
        return List.copyOf(consumerBuildings.values());
    }

    public List<ConsumerGroupEntry> getConsumerGroupEntries(Building filterBuilding)
    {
        if (this.factoryService.getCurrentFactory() == null) {
            return List.of();
        }

        Map<String, List<Production>> grouped = new LinkedHashMap<>();
        for (Production production : this.getCurrentFactoryProductions()) {
            Building building = production.getSelectedBuilding();
            if (building == null) {
                throw new IllegalStateException("A factory production has no selected building.");
            }
            if (building.getCategory() == null || !building.getCategory().isConsumer()
                || filterBuilding != null && !Objects.equals(filterBuilding.getKey(), building.getKey())) {
                continue;
            }
            String group = production.getGroup() == null ? "" : production.getGroup();
            grouped.computeIfAbsent(group + "\n" + building.getKey(), ignored -> new ArrayList<>()).add(production);
        }

        return grouped.values().stream()
            .map(productions -> new ConsumerGroupEntry(
                productions.getFirst().getGroup() == null ? "" : productions.getFirst().getGroup(),
                productions.getFirst().getSelectedBuilding(),
                productions.stream().map(Production::getId).toList(),
                productions.stream().mapToDouble(this::calculateBuildingConsumption).sum()
            ))
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
                for (antafes.sf.manager.entity.recipe.Item product : recipe.getProducts()) {
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
        Map<MaterialOutput, List<MaterialAllocation>> allocations = this.getMaterialAllocationsWithStorage(
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
                allocations.getOrDefault(new MaterialOutput(production.getId(), item.getKey()), List.of())
                    .stream()
                    .mapToDouble(MaterialAllocation::amount)
                    .sum(),
                allocations
            ))
            .toList();
    }

    public List<String> getProductionGroups()
    {
        return this.getCurrentFactoryProductions().stream()
            .map(Production::getGroup)
            .filter(group -> group != null && !group.isBlank())
            .distinct()
            .sorted(NaturalOrder.COMPARATOR)
            .toList();
    }

    public List<MaterialSourceGroup> getMaterialSourceGroups(Item item)
    {
        return this.getMaterialSourceGroups(item, Set.of(), false);
    }

    /**
     * Returns the groups producing the item with the production that is not yet consumed by other productions.
     * The given productions are ignored as consumers, so their own allocation counts as free.
     */
    public List<MaterialSourceGroup> getRemainingMaterialSourceGroups(
        Item item,
        Collection<UUID> excludedConsumerIds
    )
    {
        return this.getMaterialSourceGroups(item, excludedConsumerIds, true);
    }

    private List<MaterialSourceGroup> getMaterialSourceGroups(
        Item item,
        Collection<UUID> excludedConsumerIds,
        boolean remainingOnly
    )
    {
        if (item == null || item.getKey() == null) {
            throw new IllegalArgumentException("A material item is required.");
        }

        List<Production> productions = this.getCurrentFactoryProductions();
        Map<UUID, Production> productionsById = productions.stream()
            .filter(production -> production.getId() != null)
            .collect(Collectors.toMap(Production::getId, Function.identity()));
        Map<MaterialOutput, Double> usage = remainingOnly
            ? this.getMaterialUsageWithStorage(productions, productionsById, excludedConsumerIds)
            : Map.of();
        return productions.stream()
            .filter(production -> this.producesMaterial(production, item.getKey()))
            .filter(production -> production.getGroup() != null && !production.getGroup().isBlank())
            .collect(Collectors.groupingBy(
                Production::getGroup,
                () -> new TreeMap<>(NaturalOrder.COMPARATOR),
                Collectors.summingDouble(production -> Math.max(
                    0,
                    this.getMaterialSourceProduction(production, item, productionsById)
                        - usage.getOrDefault(new MaterialOutput(production.getId(), item.getKey()), 0.0)
                ))
            ))
            .entrySet().stream()
            .filter(entry -> !remainingOnly || entry.getValue() > 0.000001)
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
            for (var product : recipe.getProducts()) {
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
        return this.saveProductions(production, 1).getFirst();
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

        return this.deleteProductions(List.of(productionId));
    }

    /**
     * Returns the productions a deletion of the given productions removes. Selected pressurizers bring their well
     * extractors along; a pressurizer is never removed unless it is selected itself.
     */
    public List<Production> getProductionsAffectedByDeletion(Collection<UUID> productionIds)
    {
        return this.resolveProductionsToDelete(this.getCurrentFactoryForDeletion(), productionIds);
    }

    public List<Production> deleteProductions(Collection<UUID> selectedProductionIds) throws IOException
    {
        Factory currentFactory = this.getCurrentFactoryForDeletion();
        List<String> factoryProductionIds = currentFactory.getProductions();
        List<Production> productionsToDelete = this.resolveProductionsToDelete(
            currentFactory,
            selectedProductionIds
        );
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

    private Factory getCurrentFactoryForDeletion()
    {
        Factory currentFactory = this.factoryService.getCurrentFactory();
        if (currentFactory == null) {
            throw new IllegalStateException("Open a factory before deleting a production.");
        }

        return currentFactory;
    }

    private List<Production> resolveProductionsToDelete(Factory currentFactory, Collection<UUID> selectedIds)
    {
        if (selectedIds == null || selectedIds.isEmpty() || selectedIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("A production ID is required.");
        }
        List<String> factoryProductionIds = currentFactory.getProductions();
        Map<UUID, Production> productionsToDelete = new LinkedHashMap<>();
        for (UUID productionId : new LinkedHashSet<>(selectedIds)) {
            if (factoryProductionIds == null || !factoryProductionIds.contains(productionId.toString())) {
                throw new IllegalArgumentException("The production does not belong to the active factory.");
            }
            Production production = this.productionRepository.findOne(productionId);
            if (production == null) {
                throw new IllegalStateException(
                    "The production assigned to the active factory could not be loaded."
                );
            }
            productionsToDelete.put(productionId, production);
        }
        List<UUID> selectedPressurizerIds = productionsToDelete.values().stream()
            .filter(production -> production.getSelectedBuilding() != null
                && "well_pressurizer".equals(production.getSelectedBuilding().getKey()))
            .map(Production::getId)
            .toList();
        if (!selectedPressurizerIds.isEmpty()) {
            this.getCurrentFactoryProductions().stream()
                .filter(candidate -> selectedPressurizerIds.contains(candidate.getPressurizerId()))
                .forEach(candidate -> productionsToDelete.putIfAbsent(candidate.getId(), candidate));
        }

        return List.copyOf(productionsToDelete.values());
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

    public List<Production> getProductionsForEditing(Collection<UUID> productionIds)
    {
        return this.loadCurrentFactoryProductions(productionIds);
    }

    /**
     * Applies the same changes to several productions with the same restrictions as {@link #editProduction}.
     * Used Somersloops can only be changed if all productions use the same building. Either all productions are
     * updated or none, and the UI is notified once after everything has been saved.
     */
    public List<Production> editProductions(Collection<UUID> productionIds, BulkProductionEdit edit)
        throws IOException
    {
        if (edit == null || edit.isEmpty()) {
            throw new IllegalArgumentException("At least one change is required.");
        }
        if (edit.group() != null && edit.group().isBlank()) {
            throw new IllegalArgumentException("A production group is required.");
        }
        List<Production> productions = this.loadCurrentFactoryProductions(productionIds);
        if (edit.usedSomersloops() != null
            && productions.stream().map(production -> production.getSelectedBuilding().getKey()).distinct().count() > 1) {
            throw new IllegalArgumentException(
                "Used Somersloops cannot be changed for productions of different buildings."
            );
        }
        if (edit.materialSources() != null && !this.haveSameRecipe(productions)) {
            throw new IllegalArgumentException(
                "Material sources cannot be changed for productions with different recipes."
            );
        }
        if (edit.clockSpeed() != null) {
            for (Production production : productions) {
                if (!production.getSelectedBuilding().isOverclockable()
                    && Double.compare(edit.clockSpeed(), production.getClockSpeed()) != 0) {
                    throw new IllegalArgumentException("The selected building cannot be overclocked.");
                }
            }
        }

        Map<UUID, Object[]> originals = new HashMap<>();
        productions.forEach(production -> originals.put(production.getId(), new Object[] {
            production.getClockSpeed(),
            production.getGroup(),
            production.getPosition(),
            production.getUsedSomersloops(),
            this.copyMaterialSources(production.getMaterialSources())
        }));
        Factory currentFactory = this.factoryService.getCurrentFactory();
        try {
            for (Production production : productions) {
                if (edit.clockSpeed() != null) {
                    production.setClockSpeed(edit.clockSpeed());
                }
                if (edit.group() != null) {
                    production.setGroup(this.normalizeOptionalText(edit.group()));
                }
                if (edit.position() != null) {
                    production.setPosition(this.normalizeOptionalText(edit.position()));
                }
                if (edit.usedSomersloops() != null) {
                    production.setUsedSomersloops(edit.usedSomersloops());
                }
                if (edit.materialSources() != null) {
                    production.setMaterialSources(this.copyMaterialSources(edit.materialSources()));
                }
                this.validateProduction(production, currentFactory);
            }
            this.productionRepository.updateProduction(currentFactory, productions.getFirst());
        } catch (IOException | RuntimeException failure) {
            productions.forEach(production -> {
                Object[] original = originals.get(production.getId());
                production
                    .setClockSpeed((Double) original[0])
                    .setGroup((String) original[1])
                    .setPosition((String) original[2])
                    .setUsedSomersloops((Integer) original[3])
                    .setMaterialSources(this.castMaterialSources(original[4]));
            });
            throw failure;
        }
        this.eventPublisher.publishEvent(new ProductionsSavedEvent(productions));

        return productions;
    }

    private List<Production> loadCurrentFactoryProductions(Collection<UUID> productionIds)
    {
        if (productionIds == null || productionIds.isEmpty()) {
            throw new IllegalArgumentException("A production ID is required.");
        }

        return new LinkedHashSet<>(productionIds).stream()
            .map(this::getCurrentFactoryProduction)
            .toList();
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

    private RecipeProductionEntry toRecipeProductionEntry(
        Production production,
        double usage,
        Map<MaterialOutput, List<MaterialAllocation>> allocations
    )
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
        ).stream()
            .map(material -> new RecipeProductionMaterial(
                material.item(),
                material.amount(),
                material.sourceGroup(),
                allocations.getOrDefault(
                    new MaterialOutput(production.getId(), material.item().getKey()),
                    List.of()
                )
            ))
            .toList();
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
        return this.getMaterialUsageByOutput(productions, productionsById, Set.of());
    }

    private Map<MaterialOutput, Double> getMaterialUsageByOutput(
        List<Production> productions,
        Map<UUID, Production> productionsById,
        Collection<UUID> excludedConsumerIds
    )
    {
        Map<MaterialOutput, Double> usage = new HashMap<>();
        this.getMaterialAllocationsByOutput(productions, productionsById, excludedConsumerIds)
            .forEach((output, allocations) -> usage.put(
                output,
                allocations.stream().mapToDouble(MaterialAllocation::amount).sum()
            ));
        return usage;
    }

    private Map<MaterialOutput, List<MaterialAllocation>> getMaterialAllocationsWithStorage(
        List<Production> productions,
        Map<UUID, Production> productionsById
    )
    {
        Map<MaterialOutput, List<MaterialAllocation>> allocations = new HashMap<>();
        this.getMaterialAllocationsByOutput(productions, productionsById, Set.of())
            .forEach((output, list) -> allocations.put(output, new ArrayList<>(list)));
        StorageService storageService = this.storageServiceProvider.getIfAvailable();
        if (storageService == null) {
            return allocations;
        }
        storageService.getStoredItems().stream()
            .flatMap(storageItem -> storageItem.sources().stream())
            .forEach(choice -> allocations.computeIfAbsent(
                new MaterialOutput(choice.production().getId(), choice.item().getKey()),
                ignored -> new ArrayList<>()
            ).add(new MaterialAllocation(null, choice.rate())));
        return allocations;
    }

    private Map<MaterialOutput, List<MaterialAllocation>> getMaterialAllocationsByOutput(
        List<Production> productions,
        Map<UUID, Production> productionsById,
        Collection<UUID> excludedConsumerIds
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
            for (antafes.sf.manager.entity.recipe.Item product : recipe.getProducts()) {
                Item item = product.getResource();
                if (item != null && item.getKey() != null) {
                    remainingOutputs.put(
                        new MaterialOutput(production.getId(), item.getKey()),
                        this.getMaterialSourceProduction(production, item, productionsById)
                    );
                }
            }
        }

        Map<MaterialOutput, List<MaterialAllocation>> usage = new HashMap<>();
        for (Production production : productions) {
            Recipe recipe = production.getSelectedRecipe();
            Building building = production.getSelectedBuilding();
            if (recipe == null || building == null || recipe.getIngredients() == null
                || (production.getId() != null && excludedConsumerIds.contains(production.getId()))) {
                continue;
            }
            for (antafes.sf.manager.entity.recipe.Item ingredient : recipe.getIngredients()) {
                Item item = ingredient.getResource();
                if (item == null || item.getKey() == null || ingredient.getAmount() == null) {
                    throw new IllegalStateException("A recipe ingredient is incomplete.");
                }
                List<String> sourceGroups = this.getMaterialSourceGroupNames(production, item.getKey());
                double remainingDemand = ProductionCalculator.calculateProduction(
                    ingredient.getAmount(),
                    this.getClockSpeed(production),
                    production.getUsedSomersloops(),
                    building.getSomersloopSlots()
                );
                for (String sourceGroup : sourceGroups) {
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
                        usage.computeIfAbsent(output, ignored -> new ArrayList<>())
                            .add(new MaterialAllocation(production, allocated));
                        remainingOutputs.put(output, available - allocated);
                        remainingDemand -= allocated;
                    }
                }
            }
        }
        return usage;
    }

    private List<RecipeProductionMaterial> scaleRecipeMaterials(
        List<antafes.sf.manager.entity.recipe.Item> materials,
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

    private boolean haveSameRecipe(List<Production> productions)
    {
        return productions.stream().allMatch(production -> production.getSelectedRecipe() != null)
            && productions.stream().map(production -> production.getSelectedRecipe().getKey()).distinct().count() == 1;
    }

    @SuppressWarnings("unchecked")
    private List<ProductionMaterialSource> castMaterialSources(Object materialSources)
    {
        return (List<ProductionMaterialSource>) materialSources;
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
        List<String> groups = this.getMaterialSourceGroupNames(production, itemKey);
        return groups.isEmpty() ? null : String.join(", ", groups);
    }

    private List<String> getMaterialSourceGroupNames(Production production, String itemKey)
    {
        if (production.getMaterialSources() == null) {
            return List.of();
        }
        return production.getMaterialSources().stream()
            .filter(materialSource -> itemKey.equals(materialSource.getItemKey()))
            .map(ProductionMaterialSource::getGroup)
            .filter(group -> group != null && !group.isBlank())
            .toList();
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
                .map(antafes.sf.manager.entity.recipe.Item::getResource)
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
            for (antafes.sf.manager.entity.recipe.Item ingredient : recipe.getIngredients()) {
                Item item = ingredient.getResource();
                if (item == null || item.getKey() == null) {
                    throw new IllegalStateException("A recipe ingredient has no material.");
                }
                if (item.isHandCollected()) {
                    continue;
                }
                ingredients.put(item.getKey(), item);
            }
        }
        Set<String> coveredItemKeys = new HashSet<>();
        Set<String> sourcePairs = new HashSet<>();
        for (ProductionMaterialSource materialSource : materialSources) {
            if (materialSource == null || materialSource.getItemKey() == null) {
                throw new IllegalArgumentException("Every material source requires a recipe ingredient.");
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
            if (!sourcePairs.add(materialSource.getItemKey() + "\n" + group)) {
                throw new IllegalArgumentException(
                    "The material source group " + group + " is selected more than once for "
                        + materialSource.getItemKey() + "."
                );
            }
            coveredItemKeys.add(materialSource.getItemKey());
            materialSource.setGroup(group);
        }
        if (!coveredItemKeys.equals(ingredients.keySet())) {
            throw new IllegalArgumentException("Every recipe ingredient requires a material source group.");
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

    private Map<MaterialOutput, Double> getMaterialUsageWithStorage(
        List<Production> productions,
        Map<UUID, Production> productionsById
    )
    {
        return this.getMaterialUsageWithStorage(productions, productionsById, Set.of());
    }

    private Map<MaterialOutput, Double> getMaterialUsageWithStorage(
        List<Production> productions,
        Map<UUID, Production> productionsById,
        Collection<UUID> excludedConsumerIds
    )
    {
        Map<MaterialOutput, Double> usage = new HashMap<>(
            this.getMaterialUsageByOutput(productions, productionsById, excludedConsumerIds)
        );
        StorageService storageService = this.storageServiceProvider.getIfAvailable();
        if (storageService == null) {
            return usage;
        }
        storageService.getStoredItems().stream()
            .flatMap(storageItem -> storageItem.sources().stream())
            .forEach(choice -> usage.merge(
                new MaterialOutput(choice.production().getId(), choice.item().getKey()),
                choice.rate(),
                Double::sum
            ));
        return usage;
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
        antafes.sf.manager.entity.item.Production mapping = item.getProductions().stream()
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
            .sorted(Comparator.comparing(Production::getGroup, NaturalOrder.COMPARATOR))
            .toList();
    }
}
