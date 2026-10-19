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

import antafes.sf.manager.entity.Building;
import antafes.sf.manager.entity.Item;
import antafes.sf.manager.entity.Plan;
import antafes.sf.manager.entity.Recipe;
import antafes.sf.manager.repository.ItemRepository;
import antafes.sf.manager.repository.RecipeRepository;
import antafes.sf.manager.service.dto.PlanMaterial;
import antafes.sf.manager.service.dto.PlanResult;
import antafes.sf.manager.service.dto.PlanStep;
import antafes.sf.manager.util.PowerCalculator;
import antafes.sf.manager.util.ProductionCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PlanCalculationService
{
    private static final double EPSILON = 1e-6;
    private static final int MAX_SIDE_PRODUCTION_ROUNDS = 20;
    private static final String SIDE_SETTINGS_PREFIX = "side:";

    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private ProductionService productionService;
    @Autowired
    private FactoryService factoryService;

    public static String sideSettingsKey(String itemKey)
    {
        return SIDE_SETTINGS_PREFIX + itemKey;
    }

    public boolean isRaw(Item item)
    {
        return item.isHandCollected()
            || (item.getProductions() != null && !item.getProductions().isEmpty())
            || this.getRecipesProducing(item).isEmpty();
    }

    /**
     * Returns the recipes producing the item, the default recipe first.
     */
    public List<Recipe> getRecipesProducing(Item item)
    {
        return this.recipeRepository.findAll().values().stream()
            .filter(recipe -> recipe.getProducedIn() != null && this.productAmount(recipe, item) > 0)
            .sorted(Comparator
                .comparing((Recipe recipe) -> !this.isPrimaryProduct(recipe, item))
                .thenComparing(Recipe::isAlternate)
                .thenComparing(Recipe::getKey))
            .toList();
    }

    /**
     * Returns the recipes consuming the item without producing it, standard recipes first.
     */
    public List<Recipe> getRecipesConsuming(Item item)
    {
        return this.recipeRepository.findAll().values().stream()
            .filter(recipe -> recipe.getProducedIn() != null
                && this.ingredientAmount(recipe, item) > 0
                && recipe.getProducts() != null && !recipe.getProducts().isEmpty()
                && this.productAmount(recipe, item) == 0)
            .sorted(Comparator.comparing(Recipe::isAlternate).thenComparing(Recipe::getKey))
            .toList();
    }

    public Recipe getDefaultRecipe(Item item)
    {
        return this.getRecipesProducing(item).stream().findFirst().orElse(null);
    }

    public Recipe getDefaultSideRecipe(Item item)
    {
        return this.getRecipesConsuming(item).stream()
            .filter(recipe -> !recipe.isAlternate())
            .findFirst()
            .orElse(null);
    }

    public PlanResult calculate(Plan plan)
    {
        Item target = plan.getTargetItemKey() == null ? null : this.itemRepository.findOne(plan.getTargetItemKey());
        if (target == null || plan.getTargetRate() <= 0 || !Double.isFinite(plan.getTargetRate())) {
            return new PlanResult(List.of(), List.of(), List.of(), 0, List.of("planNoTarget"));
        }
        if (this.isRaw(target)) {
            return new PlanResult(List.of(), List.of(), List.of(), 0, List.of("planTargetRaw"));
        }

        Map<String, Double> extraDemand = new LinkedHashMap<>();
        Solution solution = this.solve(plan, target, extraDemand);
        boolean converged = false;
        for (int round = 0; round < MAX_SIDE_PRODUCTION_ROUNDS; round++) {
            Map<String, Double> nextExtraDemand = this.collectExtraDemand(plan, solution);
            if (this.sameDemand(extraDemand, nextExtraDemand)) {
                converged = true;
                break;
            }
            extraDemand = nextExtraDemand;
            solution = this.solve(plan, target, extraDemand);
        }
        if (!converged) {
            this.collectExtraDemand(plan, solution);
            solution.warnings.add("planNotConverged");
        }

        return this.toResult(plan, solution);
    }

    private Solution solve(Plan plan, Item target, Map<String, Double> extraDemand)
    {
        Solution solution = new Solution();
        solution.target = target;
        Set<String> forcedRaw = new HashSet<>();
        List<String> order;
        Map<String, Recipe> recipes;
        while (true) {
            recipes = new LinkedHashMap<>();
            order = new ArrayList<>();
            String cycleItem = this.resolveGraph(plan, target, extraDemand, forcedRaw, recipes, order);
            if (cycleItem == null) {
                break;
            }
            forcedRaw.add(cycleItem);
            solution.warnings.add("planCycle");
        }

        Map<String, Double> demands = new LinkedHashMap<>();
        demands.merge(target.getKey(), plan.getTargetRate(), Double::sum);
        extraDemand.forEach((itemKey, rate) -> demands.merge(itemKey, rate, Double::sum));

        for (String itemKey : order) {
            double demand = demands.getOrDefault(itemKey, 0.0);
            if (demand <= EPSILON) {
                continue;
            }
            Recipe recipe = recipes.get(itemKey);
            Item item = this.itemRepository.findOne(itemKey);
            Plan.StepSettings settings = plan.getStepSettings().getOrDefault(itemKey, new Plan.StepSettings());
            double ratio = demand / this.productAmount(recipe, item);
            PlanStep step = this.buildStep(item, recipe, false, demand, ratio, settings);
            solution.steps.add(step);
            for (var ingredient : recipe.getIngredients()) {
                demands.merge(ingredient.getResource().getKey(), ingredient.getAmount() * ratio, Double::sum);
            }
            for (var product : recipe.getProducts()) {
                if (!product.getResource().getKey().equals(itemKey)) {
                    double[] rates = solution.byproducts.computeIfAbsent(product.getResource().getKey(), key -> new double[2]);
                    rates[itemKey.equals(target.getKey()) ? 1 : 0] += product.getAmount() * ratio;
                }
            }
        }
        Map<String, Recipe> finalRecipes = recipes;
        demands.forEach((itemKey, rate) -> {
            if (!finalRecipes.containsKey(itemKey) && rate > EPSILON) {
                solution.rawInputs.put(itemKey, rate);
            }
        });

        return solution;
    }

    /**
     * Resolves the recipes of all items required for the target and the extra demand.
     *
     * @return the key of an item that is part of a recipe cycle, or null if the graph is acyclic
     */
    private String resolveGraph(
        Plan plan,
        Item target,
        Map<String, Double> extraDemand,
        Set<String> forcedRaw,
        Map<String, Recipe> recipes,
        List<String> order
    )
    {
        Set<String> visited = new HashSet<>();
        Deque<String> path = new ArrayDeque<>();
        List<String> postOrder = new ArrayList<>();
        List<String> roots = new ArrayList<>();
        roots.add(target.getKey());
        roots.addAll(extraDemand.keySet());
        for (String root : roots) {
            String cycleItem = this.visit(plan, root, forcedRaw, recipes, visited, path, postOrder);
            if (cycleItem != null) {
                return cycleItem;
            }
        }
        Collections.reverse(postOrder);
        order.addAll(postOrder);
        return null;
    }

    private String visit(
        Plan plan,
        String itemKey,
        Set<String> forcedRaw,
        Map<String, Recipe> recipes,
        Set<String> visited,
        Deque<String> path,
        List<String> postOrder
    )
    {
        if (path.contains(itemKey)) {
            return itemKey;
        }
        if (visited.contains(itemKey)) {
            return null;
        }
        Item item = this.itemRepository.findOne(itemKey);
        if (item == null || forcedRaw.contains(itemKey) || this.isRaw(item)) {
            visited.add(itemKey);
            return null;
        }
        Recipe recipe = this.chooseRecipe(plan, item);
        recipes.put(itemKey, recipe);
        path.push(itemKey);
        for (var ingredient : recipe.getIngredients()) {
            String cycleItem = this.visit(
                plan,
                ingredient.getResource().getKey(),
                forcedRaw,
                recipes,
                visited,
                path,
                postOrder
            );
            if (cycleItem != null) {
                return cycleItem;
            }
        }
        path.pop();
        visited.add(itemKey);
        postOrder.add(itemKey);
        return null;
    }

    private Recipe chooseRecipe(Plan plan, Item item)
    {
        String recipeKey = plan.getRecipeChoices().get(item.getKey());
        if (recipeKey != null) {
            Recipe recipe = this.recipeRepository.findOne(recipeKey);
            if (recipe != null && recipe.getProducedIn() != null && this.productAmount(recipe, item) > 0) {
                return recipe;
            }
        }
        return this.getDefaultRecipe(item);
    }

    private Map<String, Double> collectExtraDemand(Plan plan, Solution solution)
    {
        Map<String, Double> extraDemand = new LinkedHashMap<>();
        solution.sideSteps.clear();
        solution.sideProducts.clear();
        for (Map.Entry<String, double[]> entry : solution.byproducts.entrySet()) {
            double rate = entry.getValue()[0];
            Item item = this.itemRepository.findOne(entry.getKey());
            Recipe recipe = rate > EPSILON && item != null ? this.chooseSideRecipe(plan, item) : null;
            if (recipe == null) {
                continue;
            }
            Plan.StepSettings settings = plan.getStepSettings()
                .getOrDefault(sideSettingsKey(item.getKey()), new Plan.StepSettings());
            double ratio = rate / this.ingredientAmount(recipe, item);
            solution.sideSteps.add(this.buildStep(item, recipe, true, rate, ratio, settings));
            for (var ingredient : recipe.getIngredients()) {
                if (!ingredient.getResource().getKey().equals(item.getKey())) {
                    extraDemand.merge(ingredient.getResource().getKey(), ingredient.getAmount() * ratio, Double::sum);
                }
            }
            for (var product : recipe.getProducts()) {
                solution.sideProducts.merge(product.getResource().getKey(), product.getAmount() * ratio, Double::sum);
            }
            solution.consumedByproducts.put(item.getKey(), rate);
        }

        return extraDemand;
    }

    private Recipe chooseSideRecipe(Plan plan, Item item)
    {
        String recipeKey = plan.getSideRecipeChoices().get(item.getKey());
        if (Plan.NO_RECIPE.equals(recipeKey)) {
            return null;
        }
        if (recipeKey != null) {
            Recipe recipe = this.recipeRepository.findOne(recipeKey);
            if (recipe != null && recipe.getProducedIn() != null && this.ingredientAmount(recipe, item) > 0
                && this.productAmount(recipe, item) == 0) {
                return recipe;
            }
        }
        return this.getDefaultSideRecipe(item);
    }

    private boolean sameDemand(Map<String, Double> left, Map<String, Double> right)
    {
        if (!left.keySet().equals(right.keySet())) {
            return false;
        }
        return left.entrySet().stream()
            .allMatch(entry -> Math.abs(entry.getValue() - right.get(entry.getKey())) <= EPSILON);
    }

    private PlanStep buildStep(
        Item item,
        Recipe recipe,
        boolean sideProduction,
        double demand,
        double ratio,
        Plan.StepSettings settings
    )
    {
        Building building = recipe.getProducedIn();
        double clockSpeed = building.isOverclockable() ? settings.getClockSpeed() : 100.0;
        if (!Double.isFinite(clockSpeed) || clockSpeed <= 0 || clockSpeed > 250) {
            throw new IllegalArgumentException("Clock speed must be greater than 0 and at most 250.");
        }
        int somersloops = Math.max(0, Math.min(settings.getSomersloops(), building.getSomersloopSlots()));
        double machineMultiplier = ProductionCalculator.calculateProduction(
            1.0,
            clockSpeed,
            somersloops,
            building.getSomersloopSlots()
        );
        double perMachineDemand = demand / ratio * machineMultiplier;
        double exactMachines = demand / perMachineDemand;
        int fullMachines = (int) Math.floor(exactMachines + EPSILON);
        double remainder = exactMachines - fullMachines;
        double partialClockSpeed = remainder < EPSILON ? 0 : remainder * clockSpeed;
        if (!building.isOverclockable() && partialClockSpeed > 0) {
            fullMachines++;
            partialClockSpeed = 0;
        }

        double power = 0;
        if (building.getPower() < 0) {
            double baseConsumption = Math.abs(building.getPower());
            power += fullMachines * PowerCalculator.calculateConsumption(
                baseConsumption,
                clockSpeed,
                somersloops,
                building.getSomersloopSlots()
            );
            if (partialClockSpeed > 0) {
                power += PowerCalculator.calculateConsumption(
                    baseConsumption,
                    partialClockSpeed,
                    somersloops,
                    building.getSomersloopSlots()
                );
            }
        }

        List<PlanMaterial> inputs = recipe.getIngredients().stream()
            .map(ingredient -> new PlanMaterial(ingredient.getResource(), ingredient.getAmount() * ratio))
            .toList();
        List<PlanMaterial> outputs = recipe.getProducts().stream()
            .map(product -> new PlanMaterial(product.getResource(), product.getAmount() * ratio))
            .toList();

        return new PlanStep(
            item,
            recipe,
            building,
            sideProduction,
            demand,
            clockSpeed,
            somersloops,
            fullMachines,
            partialClockSpeed,
            power,
            inputs,
            outputs
        );
    }

    private PlanResult toResult(Plan plan, Solution solution)
    {
        List<PlanStep> steps = new ArrayList<>();
        steps.addAll(solution.steps);
        steps.addAll(solution.sideSteps);
        double totalPower = steps.stream().mapToDouble(PlanStep::powerConsumption).sum();

        Map<String, Double> available = this.getAvailableMaterials();
        List<PlanMaterial> rawInputs = solution.rawInputs.entrySet().stream()
            .map(entry -> new PlanMaterial(
                this.itemRepository.findOne(entry.getKey()),
                entry.getValue(),
                available.getOrDefault(entry.getKey(), 0.0),
                false
            ))
            .toList();

        Map<String, double[]> surplusByItem = new LinkedHashMap<>();
        solution.byproducts.forEach((itemKey, rates) -> {
            double remaining = rates[0] - solution.consumedByproducts.getOrDefault(itemKey, 0.0);
            surplusByItem.computeIfAbsent(itemKey, key -> new double[2])[0] += Math.max(0, remaining);
            surplusByItem.get(itemKey)[1] += rates[1];
        });
        solution.sideProducts.forEach((itemKey, rate) ->
            surplusByItem.computeIfAbsent(itemKey, key -> new double[2])[0] += rate
        );
        List<PlanMaterial> surplus = new ArrayList<>();
        surplusByItem.forEach((itemKey, rates) -> {
            Item item = this.itemRepository.findOne(itemKey);
            if (rates[0] > EPSILON) {
                surplus.add(new PlanMaterial(item, rates[0], 0, false));
            }
            if (rates[1] > EPSILON) {
                surplus.add(new PlanMaterial(item, rates[1], 0, true));
            }
        });

        return new PlanResult(steps, rawInputs, surplus, totalPower, List.copyOf(new LinkedHashSet<>(solution.warnings)));
    }

    private Map<String, Double> getAvailableMaterials()
    {
        Map<String, Double> available = new HashMap<>();
        if (this.factoryService.getCurrentFactory() == null) {
            return available;
        }
        this.productionService.getFactoryOverview().materials().forEach(material -> {
            double surplus = material.production() - material.usage();
            if (surplus > EPSILON) {
                available.put(material.item().getKey(), surplus);
            }
        });

        return available;
    }

    private boolean isPrimaryProduct(Recipe recipe, Item item)
    {
        return !recipe.getProducts().isEmpty()
            && item.getKey().equals(recipe.getProducts().getFirst().getResource().getKey());
    }

    private double productAmount(Recipe recipe, Item item)
    {
        return this.amount(recipe.getProducts(), item);
    }

    private double ingredientAmount(Recipe recipe, Item item)
    {
        return this.amount(recipe.getIngredients(), item);
    }

    private double amount(List<antafes.sf.manager.entity.recipe.Item> items, Item item)
    {
        if (items == null) {
            return 0;
        }

        return items.stream()
            .filter(entry -> entry.getResource() != null && item.getKey().equals(entry.getResource().getKey()))
            .mapToDouble(entry -> entry.getAmount() == null ? 0 : entry.getAmount())
            .sum();
    }

    private static class Solution
    {
        private Item target;
        private final List<PlanStep> steps = new ArrayList<>();
        private final List<PlanStep> sideSteps = new ArrayList<>();
        private final Map<String, Double> rawInputs = new LinkedHashMap<>();
        /** By-product item key to {rate from intermediate steps, rate from the target step}. */
        private final Map<String, double[]> byproducts = new LinkedHashMap<>();
        private final Map<String, Double> consumedByproducts = new LinkedHashMap<>();
        private final Map<String, Double> sideProducts = new LinkedHashMap<>();
        private final List<String> warnings = new ArrayList<>();
    }
}
