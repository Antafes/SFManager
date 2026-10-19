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
import antafes.sf.manager.entity.Factory;
import antafes.sf.manager.repository.FactoryRepository;
import antafes.sf.manager.service.event.FactoryDeletedEvent;
import antafes.sf.manager.service.event.FactorySelectedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FactoryServiceTest extends BaseTest
{
    @Autowired
    private FactoryService factoryService;
    @Autowired
    private FactoryRepository factoryRepository;

    @Test
    public void testManagesFactoryLifecycleAndCurrentSelection() throws IOException
    {
        UUID factoryId;
        try (AnnotationConfigApplicationContext context = this.createContext(
            FactoryRepository.class, FactoryService.class
        )) {
            this.autowireTest(context);
            this.factoryRepository.init();
            assertNull(this.factoryService.getCurrentFactory());
            assertEquals(List.of(), this.factoryService.getFactories());

            Factory factory = this.factoryService.createFactory("Service Factory");
            factoryId = factory.getId();
            assertEquals(List.of(factory), this.factoryService.getFactories());

            context.publishEvent(new FactorySelectedEvent(factory));
            assertSame(factory, this.factoryService.getCurrentFactory());

            factory.getProductions().add("production-id");
            this.factoryService.saveFactory(factory);
        }

        try (AnnotationConfigApplicationContext context = this.createContext(
            FactoryRepository.class, FactoryService.class
        )) {
            this.autowireTest(context);
            this.factoryRepository.init();
            Factory loadedFactory = this.factoryService.findFactory(factoryId);
            assertNotNull(loadedFactory);
            assertEquals(List.of("production-id"), loadedFactory.getProductions());
            assertNull(this.factoryService.getCurrentFactory());
        }
    }

    @Test
    public void testDeletingOpenFactoryClearsStateAndPublishesEvent() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
            FactoryRepository.class, FactoryService.class
        )) {
            this.autowireTest(context);
            this.factoryRepository.init();
            List<FactoryDeletedEvent> events = new ArrayList<>();
            context.addApplicationListener((ApplicationListener<PayloadApplicationEvent<?>>) payloadEvent -> {
                if (payloadEvent.getPayload() instanceof FactoryDeletedEvent deletedEvent) {
                    events.add(deletedEvent);
                }
            });
            Factory open = this.factoryService.createFactory("Open Factory");
            Factory other = this.factoryService.createFactory("Other Factory");
            context.publishEvent(new FactorySelectedEvent(open));

            this.factoryService.deleteFactory(other);
            assertSame(open, this.factoryService.getCurrentFactory());
            assertEquals(open.getId().toString(), this.configuration.getLastOpenedFactoryId());
            assertEquals(1, events.size());

            this.factoryService.deleteFactory(open);
            assertNull(this.factoryService.getCurrentFactory());
            assertNull(this.configuration.getLastOpenedFactoryId());
            assertEquals(List.of(), this.factoryService.getFactories());
            assertEquals(2, events.size());
            assertSame(open, events.get(1).factory());
            assertFalse(Files.exists(this.configuration.getSaveDirPath().toPath().resolve("Open Factory")));
        }
    }

    @Test
    public void testFailedDeletionPublishesNoEvent() throws IOException
    {
        try (AnnotationConfigApplicationContext context = this.createContext(
            FactoryRepository.class, FactoryService.class
        )) {
            this.autowireTest(context);
            this.factoryRepository.init();
            List<FactoryDeletedEvent> events = new ArrayList<>();
            context.addApplicationListener((ApplicationListener<PayloadApplicationEvent<?>>) payloadEvent -> {
                if (payloadEvent.getPayload() instanceof FactoryDeletedEvent deletedEvent) {
                    events.add(deletedEvent);
                }
            });

            assertThrows(IllegalArgumentException.class, () -> this.factoryService.deleteFactory(null));
            assertEquals(List.of(), events);
        }
    }

    @Test
    public void testRestoresLastOpenedFactoryFromConfiguration() throws IOException
    {
        UUID factoryId;
        try (AnnotationConfigApplicationContext context = this.createContext(
            FactoryRepository.class, FactoryService.class
        )) {
            this.autowireTest(context);
            this.factoryRepository.init();
            Factory factory = this.factoryService.createFactory("Remembered Factory");
            factoryId = factory.getId();

            context.publishEvent(new FactorySelectedEvent(factory));

            assertEquals(factoryId.toString(), this.configuration.getLastOpenedFactoryId());
        }

        try (AnnotationConfigApplicationContext context = this.createContext(
            FactoryRepository.class, FactoryService.class
        )) {
            this.autowireTest(context);
            this.configuration.setLastOpenedFactoryId(factoryId);
            this.factoryRepository.init();

            Factory restoredFactory = this.factoryService.restoreLastOpenedFactory();

            assertNotNull(restoredFactory);
            assertEquals(factoryId, restoredFactory.getId());
            assertSame(restoredFactory, this.factoryService.getCurrentFactory());
        }
    }

}
