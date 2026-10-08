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
import antafes.sf.manager.entity.Factory;
import antafes.sf.manager.entity.Storage;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StorageRepositoryTest extends BaseTest
{
    @Test
    public void testSavesAndReloadsFactoryStorage() throws IOException
    {
        Factory factory;
        UUID productionId = UUID.randomUUID();
        try (AnnotationConfigApplicationContext context = this.createContext(
            FactoryRepository.class,
            StorageRepository.class
        )) {
            this.autowireTest(context);
            FactoryRepository factoryRepository = context.getBean(FactoryRepository.class);
            factoryRepository.init();
            StorageRepository storageRepository = context.getBean(StorageRepository.class);
            storageRepository.init();
            factory = factoryRepository.createFactory("Storage Repository Factory");

            Storage storage = storageRepository.getStorage(factory);
            storage.setEntries(List.of(new Storage.Entry()
                .setProductionId(productionId)
                .setItemKey("iron_ingot")));
            storageRepository.saveStorage(factory, storage);

            assertTrue(this.tempDirectory.resolve("storage-repository-factory")
                .resolve("storage.xml").toFile().isFile());
            String xml = Files.readString(this.tempDirectory.resolve("storage-repository-factory")
                .resolve("storage.xml"));
            assertTrue(xml.contains("<factoryId>" + factory.getId() + "</factoryId>"));
            assertTrue(xml.contains("<entries>"));
        }

        try (AnnotationConfigApplicationContext context = this.createContext(
            FactoryRepository.class,
            StorageRepository.class
        )) {
            this.autowireTest(context);
            FactoryRepository factoryRepository = context.getBean(FactoryRepository.class);
            factoryRepository.init();
            StorageRepository storageRepository = context.getBean(StorageRepository.class);
            storageRepository.init();

            Storage storage = storageRepository.getStorage(factoryRepository.findOne(factory.getId()));
            assertEquals(factory.getId(), storage.getFactoryId());
            assertEquals(1, storage.getEntries().size());
            assertEquals(productionId, storage.getEntries().getFirst().getProductionId());
            assertEquals("iron_ingot", storage.getEntries().getFirst().getItemKey());

            UUID secondProductionId = UUID.randomUUID();
            storage.getEntries().add(new Storage.Entry()
                .setProductionId(secondProductionId)
                .setItemKey("copper_ingot"));
            storageRepository.saveStorage(factoryRepository.findOne(factory.getId()), storage);
            assertEquals(2, storage.getEntries().size());
            assertEquals(secondProductionId, storage.getEntries().getLast().getProductionId());
        }
    }
}
