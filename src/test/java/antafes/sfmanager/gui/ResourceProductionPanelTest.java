package antafes.sfmanager.gui;

import antafes.sfmanager.BaseTest;
import antafes.sfmanager.entity.*;
import antafes.sfmanager.gui.editor.ProductionDeleteButtonEditor;
import antafes.sfmanager.gui.editor.ProductionEditButtonEditor;
import antafes.sfmanager.gui.editor.ProductionUpgradeButtonEditor;
import antafes.sfmanager.repository.*;
import antafes.sfmanager.service.FactoryService;
import antafes.sfmanager.service.ProductionService;
import antafes.sfmanager.service.dto.ResourceProductionEntry;
import antafes.sfmanager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceProductionPanelTest extends BaseTest
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
    public void testShowsResourceRowsAndGroupsWellExtractors() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            ProductionService.class,
            ResourceProductionPanel.class
        )) {
            this.autowireTest(context);
            ProductionService productionService = context.getBean(ProductionService.class);
            Factory factory = context.getBean(FactoryService.class).createFactory("Resource Panel");
            context.publishEvent(new FactorySelectedEvent(factory));

            Building pressurizerBuilding = this.findBuilding(productionService, "well_pressurizer");
            Production pressurizer = productionService.saveProduction(new Production()
                .setSelectedBuilding(pressurizerBuilding)
                .setPosition("W1")
                .setGroup("Well Group")
                .setPurity(Purity.NORMAL)
                .setClockSpeed(150.0)
                .setUsedSomersloops(0));
            Building wellExtractorBuilding = this.findBuilding(productionService, "well_extractor");
            Item water = this.findItem(productionService, "water");
            Production extractor = productionService.saveProductions(new Production()
                .setSelectedBuilding(wellExtractorBuilding)
                .setSelectedItem(water)
                .setPurity(Purity.PURE)
                .setClockSpeed(150.0)
                .setGroup("Well Group")
                .setPressurizerId(pressurizer.getId())
                .setUsedSomersloops(0), 2).get(0);
            productionService.saveProduction(new Production()
                .setSelectedBuilding(wellExtractorBuilding)
                .setSelectedItem(water)
                .setPurity(Purity.NORMAL)
                .setClockSpeed(150.0)
                .setGroup("Well Group")
                .setPressurizerId(pressurizer.getId())
                .setUsedSomersloops(0));
            Building waterExtractorBuilding = this.findBuilding(productionService, "water_extractor");
            productionService.saveProduction(new Production()
                .setSelectedBuilding(waterExtractorBuilding)
                .setSelectedItem(water)
                .setPurity(Purity.NORMAL)
                .setPosition("W2")
                .setClockSpeed(100.0)
                .setGroup("Water Group")
                .setUsedSomersloops(0));
            Item iron = this.findItem(productionService, "iron");
            Building miner = this.findBuilding(productionService, "miner_1");
            productionService.saveProduction(new Production()
                .setSelectedBuilding(miner)
                .setSelectedItem(iron)
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setGroup("Iron Group")
                .setUsedSomersloops(0));

            List<ResourceProductionEntry> entries = productionService.getResourceProductions(water);
            assertEquals(4, entries.size());
            assertEquals(1, productionService.getResourceProductions(iron).size());
            assertEquals(pressurizer, entries.get(0).pressurizer());
            assertEquals(60, entries.get(0).baseAmount());
            assertEquals(60, entries.get(1).baseAmount());
            assertEquals(60, entries.get(2).baseAmount());
            assertEquals(120, entries.get(3).baseAmount());

            ResourceProductionPanel panel = context.getBean(
                ResourceProductionPanel.class,
                water
            );
            JTable table = panel.getTable();
            assertEquals(5, table.getRowCount());
            assertEquals(11, table.getColumnCount());
            assertEquals("Building", table.getColumnName(0));
            assertEquals("Position", table.getColumnName(1));
            assertEquals("Group", table.getColumnName(2));
            assertEquals("Purity", table.getColumnName(3));
            assertEquals("Clock speed", table.getColumnName(4));
            assertEquals("Production", table.getColumnName(5));
            assertEquals("Usage", table.getColumnName(6));
            assertEquals("Consumption", table.getColumnName(7));
            assertEquals("", table.getColumnName(8));
            assertEquals("", table.getColumnName(9));
            assertEquals("", table.getColumnName(10));
            for (int column = 1; column <= 5; column++) {
                assertTrue(table.getColumnModel().getColumn(column).getPreferredWidth() <= 100);
            }
            assertTrue(table.getColumnModel().getColumn(8).getPreferredWidth() <= 50);
            assertTrue(table.getColumnModel().getColumn(9).getPreferredWidth() <= 50);
            assertTrue(table.getColumnModel().getColumn(10).getPreferredWidth() <= 50);
            assertEquals("Resource Well Pressurizer", table.getValueAt(0, 0));
            assertEquals("W1", table.getValueAt(0, 1));
            assertEquals("Well Group", table.getValueAt(0, 2));
            assertEquals("150%", table.getValueAt(0, 4));
            assertTrue(table.getValueAt(0, 7).toString().endsWith(" MW"));
            assertEquals("Resource Well Extractor", table.getValueAt(1, 0));
            assertEquals("Well Group", table.getValueAt(1, 2));
            assertEquals("Pure", table.getValueAt(1, 3));
            assertEquals("180 / min", table.getValueAt(1, 5));
            assertEquals("", table.getValueAt(1, 1));
            assertEquals("", table.getValueAt(1, 4));
            assertEquals("", table.getValueAt(1, 7));
            assertEquals("Resource Well Extractor", table.getValueAt(2, 0));
            assertEquals("Well Group", table.getValueAt(2, 2));
            assertEquals("Pure", table.getValueAt(2, 3));
            assertEquals("180 / min", table.getValueAt(2, 5));
            assertEquals("Resource Well Extractor", table.getValueAt(3, 0));
            assertEquals("Well Group", table.getValueAt(3, 2));
            assertEquals("Normal", table.getValueAt(3, 3));
            assertEquals("90 / min", table.getValueAt(3, 5));
            assertEquals("Water Extractor", table.getValueAt(4, 0));
            assertEquals("W2", table.getValueAt(4, 1));
            assertEquals("Water Group", table.getValueAt(4, 2));
            assertEquals("120 / min", table.getValueAt(4, 5));
            assertEquals("20 MW", table.getValueAt(4, 7));
            for (int row = 0; row < table.getRowCount(); row++) {
                assertEquals(row == 0 ? "" : "0 / min", table.getValueAt(row, 6));
                assertEquals(null, table.getValueAt(row, 8));
                assertNotNull(table.getValueAt(row, 9));
                assertNotNull(table.getValueAt(row, 10));
                assertFalse(table.isCellEditable(row, 8));
            }
            assertFalse(table.getCellRenderer(0, 8).getTableCellRendererComponent(
                table,
                table.getValueAt(0, 8),
                false,
                false,
                0,
                8
            ) instanceof JButton);
            ProductionEditButtonEditor editButton = new ProductionEditButtonEditor(
                this.configuration.getLanguageObject(),
                productionId -> productionId.equals(table.getValueAt(0, 9)),
                panel::refresh
            );
            JButton edit = (JButton) editButton.getTableCellEditorComponent(
                table,
                table.getValueAt(0, 9),
                false,
                0,
                9
            );
            assertEquals("", edit.getText());
            assertNotNull(edit.getIcon());
            assertEquals("Edit", edit.getToolTipText());
            ProductionDeleteButtonEditor deleteButton = new ProductionDeleteButtonEditor(
                productionService,
                this.configuration.getLanguageObject(),
                panel,
                panel::refresh,
                () -> true
            );
            Component deleteRenderer = deleteButton.getTableCellRendererComponent(
                table,
                table.getValueAt(0, 10),
                false,
                false,
                0,
                10
            );
            JButton button = (JButton) deleteButton.getTableCellEditorComponent(
                table,
                table.getValueAt(0, 10),
                false,
                0,
                10
            );
            assertNotSame(deleteRenderer, button);
            button.doClick();
            assertEquals(1, table.getRowCount());
            assertEquals("Water Extractor", table.getValueAt(0, 0));
            assertEquals(1, productionService.getResourceProductions(water).size());
            assertNotNull(extractor.getId());
        }
    }

    @Test
    public void testRefreshReloadsTheCurrentFactoryProductions() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            ProductionService.class,
            ResourceProductionPanel.class
        )) {
            this.autowireTest(context);
            ProductionService productionService = context.getBean(ProductionService.class);
            Item iron = this.findItem(productionService, "iron");
            ResourceProductionPanel panel = context.getBean(
                ResourceProductionPanel.class,
                iron
            );
            assertEquals(0, panel.getTable().getRowCount());

            Factory firstFactory = context.getBean(FactoryService.class).createFactory("First Resource Factory");
            context.publishEvent(new FactorySelectedEvent(firstFactory));
            Building miner = this.findBuilding(productionService, "miner_1");
            productionService.saveProduction(new Production()
                .setSelectedBuilding(miner)
                .setSelectedItem(iron)
                .setPurity(Purity.NORMAL)
                .setClockSpeed(100.0)
                .setGroup("Iron Group")
                .setUsedSomersloops(0));
            panel.refresh();
            assertEquals(1, panel.getTable().getRowCount());

            Factory secondFactory = context.getBean(FactoryService.class).createFactory("Second Resource Factory");
            context.publishEvent(new FactorySelectedEvent(secondFactory));
            panel.refresh();
            assertEquals(0, panel.getTable().getRowCount());
            assertFalse(firstFactory.getProductions().isEmpty());
            assertTrue(secondFactory.getProductions().isEmpty());
        }
    }

    @Test
    public void testUpgradesOnlyMinersAndPreservesTheProduction() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
                BuildingRepository.class,
                ItemRepository.class,
                RecipeRepository.class,
                ProductionRepository.class,
                FactoryRepository.class,
                FactoryService.class,
                ProductionService.class,
                ResourceProductionPanel.class
            )) {
                this.autowireTest(context);
                ProductionService productionService = context.getBean(ProductionService.class);
                Factory factory = context.getBean(FactoryService.class).createFactory("Miner Upgrade Factory");
                context.publishEvent(new FactorySelectedEvent(factory));
                Item iron = this.findItem(productionService, "iron");
                Building miner1 = this.findBuilding(productionService, "miner_1");
                Building miner3 = this.findBuilding(productionService, "miner_3");
                Production firstMiner = productionService.saveProduction(new Production()
                    .setSelectedBuilding(miner1)
                    .setSelectedItem(iron)
                    .setGroup("Mine Group")
                    .setPosition("M1")
                    .setPurity(Purity.PURE)
                    .setClockSpeed(150.0)
                    .setUsedSomersloops(0));
                Production thirdMiner = productionService.saveProduction(new Production()
                    .setSelectedBuilding(miner3)
                    .setSelectedItem(iron)
                    .setPurity(Purity.NORMAL)
                    .setClockSpeed(100.0)
                    .setUsedSomersloops(0));

                ResourceProductionPanel panel = context.getBean(ResourceProductionPanel.class, iron);
                JTable table = panel.getTable();
                assertEquals(2, table.getRowCount());
                Component firstRenderer = table.getCellRenderer(0, 8).getTableCellRendererComponent(
                    table,
                    table.getValueAt(0, 8),
                    false,
                    false,
                    0,
                    8
                );
                boolean firstRendererEnabled = ((JButton) firstRenderer).isEnabled();
                Component thirdRenderer = table.getCellRenderer(1, 8).getTableCellRendererComponent(
                    table,
                    table.getValueAt(1, 8),
                    false,
                    false,
                    1,
                    8
                );
                assertTrue(firstRenderer instanceof JButton);
                assertTrue(firstRendererEnabled);
                assertTrue(thirdRenderer instanceof JButton);
                assertFalse(((JButton) thirdRenderer).isEnabled());
                assertNotNull(((JButton) thirdRenderer).getDisabledIcon());
                JButton firstEditor = (JButton) table.getCellEditor(0, 8).getTableCellEditorComponent(
                    table,
                    table.getValueAt(0, 8),
                    true,
                    0,
                    8
                );
                assertTrue(firstEditor.isEnabled());
                assertNotSame(firstRenderer, firstEditor);

                ProductionUpgradeButtonEditor upgradeButton = new ProductionUpgradeButtonEditor(
                    productionService,
                    this.configuration.getLanguageObject(),
                    this.configuration.getLanguage().getName(),
                    panel,
                    panel::refresh,
                    upgrades -> upgrades.get(1)
                );
                JButton button = (JButton) upgradeButton.getTableCellEditorComponent(
                    table,
                    table.getValueAt(0, 8),
                    false,
                    0,
                    8
                );
                button.doClick();
                assertEquals("Miner Mk.3", table.getValueAt(0, 0));
                assertEquals(firstMiner.getId(), productionService.getResourceProductions(iron)
                    .getFirst().production().getId());
                assertEquals("miner_3", firstMiner.getSelectedBuilding().getKey());
                assertEquals("Mine Group", firstMiner.getGroup());
                assertEquals("M1", firstMiner.getPosition());
                assertEquals(Purity.PURE, firstMiner.getPurity());
                assertEquals(150.0, firstMiner.getClockSpeed(), 0.0001);
                assertEquals(iron, firstMiner.getSelectedItem());

                Production canceledMiner = productionService.saveProduction(new Production()
                    .setSelectedBuilding(miner1)
                    .setSelectedItem(iron)
                    .setPurity(Purity.NORMAL)
                    .setClockSpeed(100.0)
                    .setUsedSomersloops(0));
                panel.refresh();
                int canceledRow = 2;
                ProductionUpgradeButtonEditor cancelButton = new ProductionUpgradeButtonEditor(
                    productionService,
                    this.configuration.getLanguageObject(),
                    this.configuration.getLanguage().getName(),
                    panel,
                    panel::refresh,
                    upgrades -> null
                );
                JButton cancel = (JButton) cancelButton.getTableCellEditorComponent(
                    table,
                    table.getValueAt(canceledRow, 8),
                    false,
                    canceledRow,
                    8
                );
                cancel.doClick();
                assertEquals("miner_1", canceledMiner.getSelectedBuilding().getKey());
        }
    }

    private Building findBuilding(ProductionService productionService, String key)
    {
        return productionService.getBuildings().stream()
            .filter(building -> key.equals(building.getKey()))
            .findFirst()
            .orElseThrow();
    }

    private Item findItem(ProductionService productionService, String key)
    {
        return productionService.getResourceItems().stream()
            .filter(item -> key.equals(item.getKey()))
            .findFirst()
            .orElseThrow();
    }
}
