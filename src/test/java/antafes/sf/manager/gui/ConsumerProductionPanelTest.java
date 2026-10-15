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

package antafes.sf.manager.gui;

import antafes.sf.manager.BaseTest;
import antafes.sf.manager.entity.Building;
import antafes.sf.manager.entity.Factory;
import antafes.sf.manager.entity.Production;
import antafes.sf.manager.repository.*;
import antafes.sf.manager.service.FactoryService;
import antafes.sf.manager.service.ProductionService;
import antafes.sf.manager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ConsumerProductionPanelTest extends BaseTest
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
    public void testGroupsConsumersFiltersByBuildingAndDeletesWholeGroup() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            ProductionService.class,
            ConsumerProductionPanel.class,
            FactoryOverviewPanel.class
        )) {
            this.autowireTest(context);
            ProductionService productionService = context.getBean(ProductionService.class);
            Factory factory = context.getBean(FactoryService.class).createFactory("Consumer Panel");
            context.publishEvent(new FactorySelectedEvent(factory));
            assertTrue(productionService.getConsumerBuildingsWithProductions().isEmpty());

            Building radarTower = this.findBuilding(productionService, "radar_tower");
            Building sink = this.findBuilding(productionService, "sink");
            productionService.saveProductions(this.consumer(radarTower, "Radar"), 3);
            productionService.saveProduction(this.consumer(sink, "Radar"));

            assertEquals(2, productionService.getConsumerBuildingsWithProductions().size());
            assertEquals(120, productionService.getFactoryOverview().totalPowerConsumption(), 0.001);

            JTable overviewConsumers = context.getBean(FactoryOverviewPanel.class).getConsumerTable();
            assertEquals(2, overviewConsumers.getRowCount());
            assertEquals("Building", overviewConsumers.getColumnName(0));
            assertEquals("Consumption", overviewConsumers.getColumnName(1));
            assertEquals("AWESOME Sink", overviewConsumers.getValueAt(0, 0));
            assertEquals("30 MW", overviewConsumers.getValueAt(0, 1));
            assertEquals("Radar Tower", overviewConsumers.getValueAt(1, 0));
            assertEquals("90 MW", overviewConsumers.getValueAt(1, 1));

            ConsumerProductionPanel panel = context.getBean(ConsumerProductionPanel.class);
            JTable table = panel.getTable();
            assertEquals(4, table.getColumnCount());
            assertEquals("Group", table.getColumnName(0));
            assertEquals("Building", table.getColumnName(1));
            assertEquals("Amount", table.getColumnName(2));
            assertEquals("Consumption", table.getColumnName(3));
            assertEquals(3, panel.getBuildingFilter().getItemCount());
            assertEquals(2, table.getRowCount());

            panel.getBuildingFilter().setSelectedIndex(
                this.indexOf(panel.getBuildingFilter(), "radar_tower")
            );
            assertEquals(1, table.getRowCount());
            assertEquals("Radar", table.getValueAt(0, 0));
            assertEquals(3, table.getValueAt(0, 2));
            assertEquals("90 MW", table.getValueAt(0, 3));

            table.setRowSelectionInterval(0, 0);
            List<UUID> ids = panel.getToolbar().getSelectedProductionIds();
            assertEquals(3, ids.size());
            panel.getToolbar().setDeleteConfirmation(labels -> true);
            panel.getToolbar().deleteSelected();
            long deadline = System.currentTimeMillis() + 5000;
            while (table.getRowCount() > 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            assertEquals(1, productionService.getConsumerGroupEntries(null).size());
            assertEquals(30, productionService.getFactoryOverview().totalPowerConsumption(), 0.001);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            fail(exception);
        }
    }

    private Production consumer(Building building, String group)
    {
        return new Production()
            .setSelectedBuilding(building)
            .setGroup(group)
            .setPosition("P")
            .setClockSpeed(100.0)
            .setUsedSomersloops(0);
    }

    private int indexOf(JComboBox<Building> comboBox, String key)
    {
        for (int index = 0; index < comboBox.getItemCount(); index++) {
            Building building = comboBox.getItemAt(index);
            if (building != null && key.equals(building.getKey())) {
                return index;
            }
        }
        throw new IllegalStateException("Building not in filter: " + key);
    }

    private Building findBuilding(ProductionService productionService, String key)
    {
        return productionService.getBuildings().stream()
            .filter(building -> key.equals(building.getKey()))
            .findFirst()
            .orElseThrow();
    }
}
