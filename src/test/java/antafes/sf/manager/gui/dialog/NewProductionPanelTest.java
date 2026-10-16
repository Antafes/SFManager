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

package antafes.sf.manager.gui.dialog;

import antafes.sf.manager.BaseTest;
import antafes.sf.manager.entity.Factory;
import antafes.sf.manager.entity.Production;
import antafes.sf.manager.entity.Recipe;
import antafes.sf.manager.repository.*;
import antafes.sf.manager.service.FactoryService;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NewProductionPanelTest extends BaseTest
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
    }

    @Test
    public void testPrefillsGroupOnlyWhenBlankOrAutoFilled() throws Exception
    {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            ProductionService.class,
            NewProductionPanel.class
        )) {
            this.autowireTest(context);
            Factory factory = context.getBean(FactoryService.class).createFactory("Prefill");
            context.publishEvent(new FactorySelectedEvent(factory));
            ProductionService productionService = context.getBean(ProductionService.class);
            productionService.saveProduction(new Production()
                .setSelectedBuilding(productionService.getBuildings().stream()
                    .filter(building -> "sink".equals(building.getKey()))
                    .findFirst()
                    .orElseThrow())
                .setGroup("Aaa")
                .setPosition("P")
                .setClockSpeed(100.0)
                .setUsedSomersloops(0));

            JDialog dialog = new JDialog();
            NewProductionPanel panel = context.getBean(NewProductionPanel.class, dialog, this.configuration);
            JComboBox<Recipe> recipeSelect = this.field(panel, "recipeSelect");
            JComboBox<String> groupField = this.field(panel, "groupField");

            List<Recipe> recipes = new ArrayList<>();
            for (int i = 0; i < recipeSelect.getItemCount(); i++) {
                Recipe recipe = recipeSelect.getItemAt(i);
                if (recipe.getProducts() != null && !recipe.getProducts().isEmpty()) {
                    recipes.add(recipe);
                }
            }
            assertTrue(recipes.size() >= 2);

            recipeSelect.setSelectedItem(recipes.get(0));
            assertEquals(this.firstProductName(recipes.get(0)), groupField.getEditor().getItem());

            recipeSelect.setSelectedItem(recipes.get(1));
            assertEquals(this.firstProductName(recipes.get(1)), groupField.getEditor().getItem());

            groupField.getEditor().setItem("Custom");
            recipeSelect.setSelectedItem(recipes.get(0));
            assertEquals("Custom", groupField.getEditor().getItem());
            dialog.dispose();
        }
    }

    private String firstProductName(Recipe recipe)
    {
        return recipe.getProducts().getFirst().getResource().getName().getLocalizedName("English");
    }

    @SuppressWarnings("unchecked")
    private <T> T field(Object target, String name) throws Exception
    {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }
}
