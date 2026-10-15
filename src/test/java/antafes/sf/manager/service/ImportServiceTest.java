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
import antafes.sf.manager.repository.*;
import antafes.sf.manager.service.dto.ImportResult;
import antafes.sf.manager.service.dto.SkippedMachine;
import antafes.sf.manager.service.exception.UnsupportedSaveVersionException;
import antafes.sf.manager.service.mapper.SaveProductionMapper;
import antafes.sf.saveparser.SaveParser;
import antafes.sf.saveparser.SaveParserService;
import antafes.sf.saveparser.dto.ParsedSave;
import antafes.sf.saveparser.dto.SaveFileInfo;
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
        context.getBean(RecipeRepository.class).init();
        context.getBean(FactoryRepository.class).init();
        context.getBean(ProductionRepository.class).init();
    }

    private AnnotationConfigApplicationContext context()
    {
        AnnotationConfigApplicationContext context = this.createContext(
            BuildingRepository.class,
            ItemRepository.class,
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
    void testReadsFixtureAndReportsSkippedMiners() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.context()) {
            ImportResult result = this.importService.readSave(this.fixture());

            assertEquals(24, result.productions().size());
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
            assertEquals(
                4,
                result.skippedMachines().stream()
                    .filter(skipped -> skipped.reason() == SkippedMachine.Reason.RESOURCE_UNKNOWN)
                    .filter(skipped -> skipped.className().contains("MinerMk3"))
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

            assertEquals(24, factory.getProductions().size());
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
