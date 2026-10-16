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

package antafes.sf.manager.repository;

import antafes.sf.manager.BaseTest;
import antafes.sf.manager.entity.Building;
import antafes.sf.manager.entity.Item;
import antafes.sf.manager.entity.Recipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RepositoryLoadingTest extends BaseTest
{
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    public void setUp()
    {
        super.setUp();
        this.context = new AnnotationConfigApplicationContext(
            BuildingRepository.class, ItemRepository.class, RecipeRepository.class
        );
        this.context.getBean(BuildingRepository.class).init();
        this.context.getBean(ItemRepository.class).init();
        this.context.getBean(RecipeRepository.class).init();
    }

    @AfterEach
    public void tearDown()
    {
        this.context.close();
    }

    @Test
    public void testLoadsBuildingsFromXml()
    {
        BuildingRepository repository = this.context.getBean(BuildingRepository.class);

        assertEquals(44, repository.findAll().size());
        Building smelter = repository.findOne("smelter");
        assertNotNull(smelter);
        assertEquals("smelter", smelter.getKey());
        assertEquals(-4, smelter.getPower());
        assertFalse(smelter.isResourceBuilding());

        Building miner = repository.findOne("miner_1");
        assertNotNull(miner);
        assertTrue(miner.isResourceBuilding());
    }

    @Test
    public void testLoadsItemsAndResolvesBuildingReferences()
    {
        ItemRepository repository = this.context.getBean(ItemRepository.class);

        assertEquals(159, repository.findAll().size());
        Item iron = repository.findOne("iron");
        assertNotNull(iron);
        assertEquals("Iron Ore", iron.getName().getEnglish());
        assertEquals("images/items/iron_ore.png", iron.getIcon());
        assertEquals(3, iron.getProductions().size());
        assertSame(this.context.getBean(BuildingRepository.class).findOne("miner_1"),
            iron.getProductions().getFirst().getBuilding());

        Item adaptiveControlUnit = repository.findOne("adaptive_control_unit");
        assertNotNull(adaptiveControlUnit);
        assertEquals("images/items/adaptive_control_unit.png", adaptiveControlUnit.getIcon());

        Item ironIngot = repository.findOne("iron_ingot");
        assertNotNull(ironIngot);
        assertNotNull(ironIngot.getProductions());
        assertEquals(0, ironIngot.getProductions().size());
        assertTrue(repository.findOne("wood").isHandCollected());
        assertFalse(iron.isHandCollected());
    }

    @Test
    public void testLoadsRecipesAndResolvesBuildingAndItemReferences()
    {
        RecipeRepository repository = this.context.getBean(RecipeRepository.class);

        assertEquals(288, repository.findAll().size());
        assertNull(repository.findOne("well_extractor_crude_oil"));
        assertNull(repository.findOne("well_extractor_water"));
        assertNull(repository.findOne("well_extractor_nitrogen"));
        Recipe ironIngot = repository.findOne("iron_ingot");
        assertNotNull(ironIngot);
        assertSame(this.context.getBean(BuildingRepository.class).findOne("smelter"),
            ironIngot.getProducedIn());
        assertSame(this.context.getBean(ItemRepository.class).findOne("iron"),
            ironIngot.getIngredients().getFirst().getResource());
        assertSame(this.context.getBean(ItemRepository.class).findOne("iron_ingot"),
            ironIngot.getProducts().getFirst().getResource());

        for (String fuelKey : List.of("fuel", "liquid_biofuel", "turbofuel", "rocket_fuel", "ionized_fuel")) {
            Recipe fuelGeneratorRecipe = repository.findOne("fuel_generator_" + fuelKey);
            assertNotNull(fuelGeneratorRecipe);
            assertEquals(
                fuelKey,
                fuelGeneratorRecipe.getIngredients().getFirst().getResource().getKey()
            );
        }

        Recipe aiExpansionServer = repository.findOne("ai_expansion_server");
        assertNotNull(aiExpansionServer);
        assertEquals(4, aiExpansionServer.getIngredients().size());
        assertEquals(2, aiExpansionServer.getProducts().size());

        for (Recipe recipe : repository.findAll().values()) {
            assertNotNull(recipe.getProducedIn(), recipe.getKey() + " has no building reference");
            recipe.getIngredients().forEach(item ->
                assertNotNull(item.getResource(), recipe.getKey() + " has an unresolved ingredient")
            );
            recipe.getProducts().forEach(item ->
                assertNotNull(item.getResource(), recipe.getKey() + " has an unresolved product")
            );
        }

        Recipe leavesFuel = repository.findOne("burner_leaves");
        assertNotNull(leavesFuel);
        assertEquals(1, leavesFuel.getIngredients().size());
        assertTrue(leavesFuel.getProducts().isEmpty());
    }
}
