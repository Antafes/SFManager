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

package antafes.sf.manager.service.mapper;

import antafes.sf.manager.entity.*;
import antafes.sf.manager.repository.BuildingRepository;
import antafes.sf.manager.repository.ItemRepository;
import antafes.sf.manager.repository.RecipeRepository;
import antafes.sf.manager.repository.ResourceNodeRepository;
import antafes.sf.manager.service.dto.ImportResult;
import antafes.sf.manager.service.dto.SkippedMachine;
import antafes.sf.saveparser.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Converts the machines of a parsed save into productions using the data-driven mapping
 * in {@code data/save-mapping.properties}. Machines without a reliable counterpart are reported as skipped.
 */
@Component
public class SaveProductionMapper
{
    private static final String MAPPING_RESOURCE = "data/save-mapping.properties";
    private static final String WATER_ITEM_KEY = "water";
    private static final String WATER_EXTRACTOR_KEY = "water_extractor";
    private static final double MAX_CLOCK_SPEED = 250;

    @Autowired
    private BuildingRepository buildingRepository;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ResourceNodeRepository resourceNodeRepository;

    private Properties mapping;

    public ImportResult map(ParsedSave save)
    {
        Properties properties = this.getMapping();
        Map<String, SaveObject> objects = new HashMap<>();
        for (SaveLevel level : save.levels()) {
            for (SaveObject object : level.objects()) {
                objects.put(object.instanceName(), object);
            }
        }

        List<Production> productions = new ArrayList<>();
        List<SkippedMachine> skipped = new ArrayList<>();
        for (SaveLevel level : save.levels()) {
            for (SaveObjectHeader header : level.headers()) {
                if (!(header instanceof ActorHeader actor)) {
                    continue;
                }
                String className = getClassName(actor.typePath());
                String unsupportedReason = properties.getProperty("unsupported." + className);
                if (unsupportedReason != null) {
                    skipped.add(new SkippedMachine(
                        actor.instanceName(),
                        className,
                        SkippedMachine.Reason.valueOf(unsupportedReason)
                    ));
                    continue;
                }
                String buildingKey = properties.getProperty("building." + className);
                if (buildingKey == null) {
                    continue;
                }
                this.mapMachine(actor, className, buildingKey, objects.get(actor.instanceName()), productions, skipped);
            }
        }

        return new ImportResult(save.fileInfo().sessionName(), productions, skipped);
    }

    static String getClassName(String path)
    {
        return path.substring(path.lastIndexOf('.') + 1);
    }

    private void mapMachine(
        ActorHeader actor,
        String className,
        String buildingKey,
        SaveObject object,
        List<Production> productions,
        List<SkippedMachine> skipped
    )
    {
        Building building = this.buildingRepository.findOne(buildingKey);
        if (building == null) {
            throw new IllegalStateException("The mapped building does not exist: " + buildingKey);
        }
        List<SaveProperty> saveProperties = object == null ? List.of() : object.properties();

        double clockSpeed = Math.round(this.getPotential(saveProperties) * 1_000_000d) / 10_000d;
        if (!Double.isFinite(clockSpeed) || clockSpeed <= 0 || clockSpeed > MAX_CLOCK_SPEED) {
            skipped.add(new SkippedMachine(actor.instanceName(), className, SkippedMachine.Reason.INVALID_CLOCK_SPEED));
            return;
        }

        Production production = new Production()
            .setSelectedBuilding(building)
            .setClockSpeed(clockSpeed)
            .setUsedSomersloops(0)
            .setPosition(formatPosition(actor));

        if (building.isResourceBuilding()) {
            if (WATER_EXTRACTOR_KEY.equals(buildingKey)) {
                production.setSelectedItem(this.itemRepository.findOne(WATER_ITEM_KEY)).setPurity(Purity.NORMAL);
                productions.add(production);
                return;
            }

            ResourceNode node = this.resolveResourceNode(building, saveProperties);
            if (node == null) {
                skipped.add(new SkippedMachine(actor.instanceName(), className, SkippedMachine.Reason.RESOURCE_UNKNOWN));
                return;
            }
            production.setSelectedItem(node.getItem()).setPurity(node.getPurity());
            productions.add(production);
            return;
        }

        String recipeKey = this.resolveRecipeKey(buildingKey, saveProperties);
        if (recipeKey == null) {
            boolean hasReference = this.getReferencePath(saveProperties, "mCurrentRecipe") != null
                || this.getReferencePath(saveProperties, "mCurrentFuelClass") != null;
            skipped.add(new SkippedMachine(
                actor.instanceName(),
                className,
                hasReference ? SkippedMachine.Reason.RECIPE_UNMAPPED : SkippedMachine.Reason.NO_RECIPE
            ));
            return;
        }
        Recipe recipe = this.recipeRepository.findOne(recipeKey);
        if (recipe == null || recipe.getProducedIn() == null || !buildingKey.equals(recipe.getProducedIn().getKey())) {
            throw new IllegalStateException("The mapped recipe is not valid for " + buildingKey + ": " + recipeKey);
        }
        productions.add(production.setSelectedRecipe(recipe));
    }

    private ResourceNode resolveResourceNode(Building building, List<SaveProperty> saveProperties)
    {
        String nodePath = this.getReferencePath(saveProperties, "mExtractableResource");
        ResourceNode node = nodePath == null ? null : this.resourceNodeRepository.findOne(nodePath);
        if (node == null || node.getType() != ResourceNode.Type.NODE
            || node.getItem() == null || node.getPurity() == null) {
            return null;
        }

        boolean producible = node.getItem().getProductions().stream()
            .anyMatch(production -> building.getKey().equals(production.getBuilding().getKey()));

        return producible ? node : null;
    }

    private String resolveRecipeKey(String buildingKey, List<SaveProperty> saveProperties)
    {
        Properties properties = this.getMapping();
        String fuelPath = this.getReferencePath(saveProperties, "mCurrentFuelClass");
        if (fuelPath != null) {
            return properties.getProperty("fuel." + buildingKey + "." + getClassName(fuelPath));
        }
        String recipePath = this.getReferencePath(saveProperties, "mCurrentRecipe");
        if (recipePath != null) {
            return properties.getProperty("recipe." + getClassName(recipePath));
        }
        return null;
    }

    private double getPotential(List<SaveProperty> saveProperties)
    {
        for (String name : List.of("mCurrentPotential", "mPendingPotential")) {
            for (SaveProperty property : saveProperties) {
                if (name.equals(property.name()) && property.value() instanceof Number number) {
                    return number.doubleValue();
                }
            }
        }
        return 1.0;
    }

    private String getReferencePath(List<SaveProperty> saveProperties, String name)
    {
        for (SaveProperty property : saveProperties) {
            if (name.equals(property.name()) && property.value() instanceof ObjectReference reference
                && reference.pathName() != null && !reference.pathName().isBlank()) {
                return reference.pathName();
            }
        }
        return null;
    }

    private static String formatPosition(ActorHeader actor)
    {
        if (actor.position().size() < 2) {
            return null;
        }
        return toMeters(actor.position().getFirst()) + ", " + toMeters(actor.position().get(1));
    }

    private static long toMeters(double centimeters)
    {
        return Math.round(centimeters / 100);
    }

    private synchronized Properties getMapping()
    {
        if (this.mapping == null) {
            Properties loaded = new Properties();
            try (InputStream input = new ClassPathResource(MAPPING_RESOURCE).getInputStream()) {
                loaded.load(input);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not load " + MAPPING_RESOURCE, exception);
            }
            this.mapping = loaded;
        }
        return this.mapping;
    }
}
