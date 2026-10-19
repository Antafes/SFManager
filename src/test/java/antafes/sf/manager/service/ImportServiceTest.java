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

import antafes.sf.manager.BaseTest;
import antafes.sf.manager.SFManager;
import antafes.sf.manager.entity.Factory;
import antafes.sf.manager.entity.Production;
import antafes.sf.manager.entity.ResourceNode;
import antafes.sf.manager.repository.*;
import antafes.sf.manager.service.dto.ImportResult;
import antafes.sf.manager.service.dto.SkippedMachine;
import antafes.sf.manager.service.exception.UnsupportedSaveVersionException;
import antafes.sf.manager.service.mapper.SaveProductionMapper;
import antafes.sf.saveparser.SaveParser;
import antafes.sf.saveparser.SaveParserService;
import antafes.sf.saveparser.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportServiceTest extends BaseTest
{
    @Autowired
    ImportService importService;

    @Override
    protected void autowireTest(AnnotationConfigApplicationContext context)
    {
        super.autowireTest(context);
        context.getBean(BuildingRepository.class).init();
        context.getBean(ItemRepository.class).init();
        context.getBean(ResourceNodeRepository.class).init();
        context.getBean(RecipeRepository.class).init();
        context.getBean(FactoryRepository.class).init();
        context.getBean(ProductionRepository.class).init();
    }

    private AnnotationConfigApplicationContext context()
    {
        AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
            ResourceNodeRepository.class,
            RecipeRepository.class,
            ProductionRepository.class,
            FactoryRepository.class,
            FactoryService.class,
            SaveParser.class,
            SaveProductionMapper.class,
            ImportService.class
        );
        this.autowireTest(context);
        return context;
    }

    private InputStream fixture()
    {
        return SFManager.class.getClassLoader().getResourceAsStream("data/test.sav");
    }

    @Test
    void testImportsOilPumpWellPressurizerAndExtractor() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.context()) {
            ResourceNodeRepository nodes = context.getBean(ResourceNodeRepository.class);
            ResourceNode oilNode = nodes.findAll().values().stream()
                .filter(node -> node.getType() == ResourceNode.Type.NODE && node.getItem() != null
                    && "oil".equals(node.getItem().getKey()))
                .findFirst().orElseThrow();
            ResourceNode satellite = nodes.findAll().values().stream()
                .filter(node -> node.getType() == ResourceNode.Type.WELL && node.getItem() != null)
                .findFirst().orElseThrow();
            ResourceNode orphan = nodes.findAll().values().stream()
                .filter(node -> node.getType() == ResourceNode.Type.WELL && node.getItem() != null
                    && !node.getCore().equals(satellite.getCore()))
                .findFirst().orElseThrow();

            ParsedSave save = this.save(
                this.machine("Build_OilPump_C", "pump", oilNode.getKey(), 1.0),
                this.machine("Build_FrackingSmasher_C", "smasher", satellite.getCore(), 0.5),
                this.machine("Build_FrackingExtractor_C", "extractor", satellite.getKey(), 1.0),
                this.machine("Build_FrackingExtractor_C", "lonely", orphan.getKey(), 1.0)
            );

            ImportResult result = context.getBean(SaveProductionMapper.class).map(save);

            assertEquals(3, result.productions().size());
            Production pump = this.find(result, "oil_extractor");
            assertEquals("oil", pump.getSelectedItem().getKey());
            assertEquals(oilNode.getPurity(), pump.getPurity());
            Production pressurizer = this.find(result, "well_pressurizer");
            assertNull(pressurizer.getSelectedItem());
            Production extractor = this.find(result, "well_extractor");
            assertEquals(satellite.getItem(), extractor.getSelectedItem());
            assertEquals(satellite.getPurity(), extractor.getPurity());
            assertEquals(pressurizer.getId(), extractor.getPressurizerId());
            assertEquals(50.0, extractor.getClockSpeed());
            assertEquals(1, result.skippedMachines().size());
            assertEquals(SkippedMachine.Reason.NO_PRESSURIZER, result.skippedMachines().getFirst().reason());
        }
    }

    @Test
    void testImportsGeothermalGeneratorWithGeyserPurity() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.context()) {
            ResourceNode geyser = context.getBean(ResourceNodeRepository.class).findAll().values().stream()
                .filter(node -> node.getType() == ResourceNode.Type.GEYSER && node.getPurity() != null)
                .findFirst().orElseThrow();

            ImportResult result = context.getBean(SaveProductionMapper.class).map(
                this.save(this.machine("Build_GeneratorGeoThermal_C", "geo", geyser.getKey(), 1.0))
            );

            Production generator = this.find(result, "geothermal_generator");
            assertEquals(geyser.getPurity(), generator.getPurity());
            assertNull(generator.getSelectedItem());
            assertTrue(result.skippedMachines().isEmpty());
        }
    }

    @Test
    void testOilPumpOnMinerNodeIsSkipped() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.context()) {
            ResourceNode ironNode = context.getBean(ResourceNodeRepository.class).findAll().values().stream()
                .filter(node -> node.getType() == ResourceNode.Type.NODE && node.getItem() != null
                    && "iron".equals(node.getItem().getKey()))
                .findFirst().orElseThrow();

            ImportResult result = context.getBean(SaveProductionMapper.class).map(
                this.save(this.machine("Build_OilPump_C", "pump", ironNode.getKey(), 1.0))
            );

            assertTrue(result.productions().isEmpty());
            assertEquals(SkippedMachine.Reason.RESOURCE_UNKNOWN, result.skippedMachines().getFirst().reason());
        }
    }

    private Production find(ImportResult result, String buildingKey)
    {
        return result.productions().stream()
            .filter(production -> buildingKey.equals(production.getSelectedBuilding().getKey()))
            .findFirst().orElseThrow();
    }

    private record Machine(ActorHeader header, SaveObject object)
    {
    }

    private Machine machine(String className, String name, String resourcePath, double potential)
    {
        String instance = "Persistent_Level:PersistentLevel." + className + "_" + name;
        ActorHeader header = new ActorHeader(
            "/Game/FactoryGame/Buildable/" + className.replace("Build_", "") + "/" + className + "." + className,
            "",
            instance,
            0,
            false,
            List.of(0f, 0f, 0f, 1f),
            List.of(100_000f, 200_000f, 0f),
            List.of(1f, 1f, 1f),
            true
        );
        SaveObject object = new SaveObject(
            instance,
            0,
            false,
            null,
            new ObjectReference("", ""),
            List.of(),
            List.of(
                new SaveProperty("mExtractableResource", "ObjectProperty", new ObjectReference("Persistent_Level", resourcePath), List.of()),
                new SaveProperty("mCurrentPotential", "FloatProperty", (float) potential, List.of())
            ),
            new byte[0]
        );

        return new Machine(header, object);
    }

    private ParsedSave save(Machine... machines)
    {
        SaveFileInfo info = mock(SaveFileInfo.class);
        when(info.sessionName()).thenReturn("synthetic");
        SaveLevel level = new SaveLevel(
            "Persistent_Level",
            java.util.Arrays.stream(machines).<SaveObjectHeader>map(Machine::header).toList(),
            true,
            List.of(),
            java.util.Arrays.stream(machines).map(Machine::object).toList(),
            0,
            List.of(),
            null
        );

        return new ParsedSave(info, null, List.of(), List.of(level), "Persistent_Level", List.of(), List.of());
    }

    @Test
    void testReadsFixtureAndImportsMiners() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.context()) {
            ImportResult result = this.importService.readSave(this.fixture());

            assertEquals(28, result.productions().size());
            assertEquals(
                18,
                result.productions().stream()
                    .filter(production -> "coal_generator".equals(production.getSelectedBuilding().getKey()))
                    .count()
            );
            assertEquals(
                6,
                result.productions().stream()
                    .filter(production -> "water_extractor".equals(production.getSelectedBuilding().getKey()))
                    .count()
            );
            List<Production> miners = result.productions().stream()
                .filter(production -> "miner_3".equals(production.getSelectedBuilding().getKey()))
                .toList();
            assertEquals(4, miners.size());
            miners.forEach(miner -> {
                assertNotNull(miner.getSelectedItem());
                assertNotNull(miner.getPurity());
            });
            assertEquals(
                0,
                result.skippedMachines().stream()
                    .filter(skipped -> skipped.className().contains("Miner"))
                    .count()
            );
            result.productions().forEach(production -> {
                assertNotNull(production.getPosition());
                assertTrue(production.getClockSpeed() > 0);
            });
        }
    }

    @Test
    void testImportPersistsFactoryAndPublishesEvents() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.context()) {
            List<ApplicationEvent> events = new ArrayList<>();
            context.addApplicationListener((ApplicationListener<ApplicationEvent>) events::add);

            ImportResult result = this.importService.readSave(this.fixture());
            Factory factory = this.importService.importFactory(result, "Imported Factory");
            this.importService.publishImport(factory);

            assertEquals(28, factory.getProductions().size());
            assertTrue(context.getBean(FactoryService.class).getFactories().stream()
                .anyMatch(found -> found.getId().equals(factory.getId())));
            assertTrue(events.stream().anyMatch(event -> event.getClass().getSimpleName().equals("PayloadApplicationEvent")));
        }
    }

    @Test
    void testDuplicateNameLeavesExistingFactoryUntouched() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.context()) {
            ImportResult result = this.importService.readSave(this.fixture());
            this.importService.importFactory(result, "Duplicate");
            int count = context.getBean(FactoryService.class).getFactories().size();

            assertThrows(IllegalArgumentException.class, () -> this.importService.importFactory(result, "Duplicate"));
            assertEquals(count, context.getBean(FactoryService.class).getFactories().size());
        }
    }

    @Test
    void testEmptyResultIsRejected() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.context()) {
            ImportResult empty = new ImportResult("x", List.<Production>of(), List.of());
            assertThrows(IllegalArgumentException.class, () -> this.importService.importFactory(empty, "Empty"));
        }
    }

    @Test
    void testMappingFileReferencesExistingEntries() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.context()) {
            java.util.Properties mapping = new java.util.Properties();
            try (InputStream stream = SFManager.class.getClassLoader().getResourceAsStream("data/save-mapping.properties")) {
                mapping.load(stream);
            }
            BuildingRepository buildings = context.getBean(BuildingRepository.class);
            RecipeRepository recipes = context.getBean(RecipeRepository.class);

            mapping.stringPropertyNames().forEach(key -> {
                String value = mapping.getProperty(key);
                if (key.startsWith("building.")) {
                    assertNotNull(buildings.findOne(value), key);
                } else if (key.startsWith("recipe.") || key.startsWith("fuel.")) {
                    assertNotNull(recipes.findOne(value), key);
                }
            });
        }
    }

    @Test
    void testOldSaveVersionIsRejected() throws IOException
    {
        SaveParserService parser = mock(SaveParserService.class);
        ParsedSave save = mock(ParsedSave.class);
        SaveFileInfo info = mock(SaveFileInfo.class);
        when(info.saveVersion()).thenReturn(36L);
        when(save.fileInfo()).thenReturn(info);
        when(parser.readFullSaveFile(any(InputStream.class))).thenReturn(save);
        try (AnnotationConfigApplicationContext context = this.context()) {
            ImportService service = new ImportService();
            ReflectionTestUtils.setField(service, "saveParserService", parser);

            UnsupportedSaveVersionException exception = assertThrows(
                UnsupportedSaveVersionException.class,
                () -> service.readSave(new ByteArrayInputStream(new byte[0]))
            );
            assertEquals(36, exception.getSaveVersion());
        }
    }

    @Test
    void testCorruptStreamFails() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.context()) {
            assertThrows(
                IOException.class,
                () -> this.importService.readSave(new ByteArrayInputStream(new byte[] {1, 2, 3, 4}))
            );
        }
    }
}
