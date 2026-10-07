package antafes.sfmanager.repository;

import antafes.sfmanager.BaseTest;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.entity.Storage;
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
