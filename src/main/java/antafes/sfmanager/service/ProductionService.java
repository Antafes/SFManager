package antafes.sfmanager.service;

import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.entity.Production;
import antafes.sfmanager.entity.Recipe;
import antafes.sfmanager.repository.BuildingRepository;
import antafes.sfmanager.repository.ProductionRepository;
import antafes.sfmanager.repository.RecipeRepository;
import antafes.sfmanager.service.event.ProductionSavedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class ProductionService
{
    @Autowired
    private BuildingRepository buildingRepository;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private ProductionRepository productionRepository;
    @Autowired
    private FactoryService factoryService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public List<Building> getBuildings()
    {
        this.initializeRepositories();
        return List.copyOf(this.buildingRepository.findAll().values());
    }

    public List<Recipe> getRecipesForBuilding(String buildingKey)
    {
        this.initializeRepositories();
        return this.recipeRepository.findAll().values().stream()
            .filter(recipe -> recipe.getProducedIn() != null
                && recipe.getProducedIn().getKey().equals(buildingKey))
            .toList();
    }

    public List<Production> getPressurizers()
    {
        this.initializeRepositories();
        return this.getCurrentFactoryProductions().stream()
            .filter(production -> production.getSelectedBuilding() != null
                && "well_pressurizer".equals(production.getSelectedBuilding().getKey()))
            .toList();
    }

    public List<String> getProductionGroups()
    {
        this.initializeRepositories();
        return this.getCurrentFactoryProductions().stream()
            .map(Production::getGroup)
            .filter(group -> group != null && !group.isBlank())
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    public Production saveProduction(Production production) throws IOException
    {
        this.initializeRepositories();
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
        production.setId(UUID.randomUUID());

        String productionId = production.getId().toString();
        if (factoryProductions.contains(productionId)) {
            throw new IllegalStateException("The production is already assigned to the active factory.");
        }
        factoryProductions.add(productionId);

        boolean productionSaved = false;
        boolean factorySaveAttempted = false;
        try {
            this.productionRepository.saveProduction(production);
            productionSaved = true;
            factorySaveAttempted = true;
            this.factoryService.saveFactory(currentFactory);
        } catch (IOException | RuntimeException failure) {
            factoryProductions.remove(productionId);
            if (productionSaved) {
                try {
                    this.productionRepository.deleteProduction(production.getId());
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

    private void initializeRepositories()
    {
        this.buildingRepository.init();
        this.recipeRepository.init();
        this.productionRepository.init();
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
