package antafes.sfmanager.repository;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.entity.Storage;
import antafes.sfmanager.repository.xml.storage.StorageData;
import antafes.sfmanager.util.FactoryName;
import antafes.utilities.Utilities;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Repository
public class StorageRepository extends BaseRepository<Storage, UUID>
{
    @Autowired
    private FactoryRepository factoryRepository;
    @Autowired
    private Configuration configuration;

    public synchronized Storage getStorage(Factory factory)
    {
        this.validateFactory(factory);
        return this.entities.computeIfAbsent(
            factory.getId(),
            factoryId -> new Storage().setFactoryId(factoryId)
        );
    }

    public synchronized void saveStorage(Factory factory, Storage storage) throws IOException
    {
        this.validateFactory(factory);
        this.validateStorage(factory, storage);
        Storage originalStorage = this.entities.put(factory.getId(), storage);
        try {
            this.saveData(factory, storage);
        } catch (IOException | RuntimeException failure) {
            if (originalStorage == null) {
                this.entities.remove(factory.getId());
            } else {
                this.entities.put(factory.getId(), originalStorage);
            }
            throw failure;
        }
    }

    @Override
    protected synchronized void loadData()
    {
        try {
            for (Factory factory : this.factoryRepository.findAll().values()) {
                File storageFile = this.getStorageFile(factory);
                if (!storageFile.isFile() || storageFile.length() == 0) {
                    continue;
                }
                StorageData data;
                try (InputStream input = Files.newInputStream(storageFile.toPath())) {
                    data = (StorageData) this.createUnmarshaller().unmarshal(input);
                }
                if (!factory.getId().toString().equals(data.factoryId)) {
                    throw new IllegalStateException("Storage data belongs to a different factory.");
                }
                Storage storage = new Storage()
                    .setFactoryId(factory.getId())
                    .setEntries(data.entries.stream()
                        .map(entry -> new Storage.Entry()
                            .setProductionId(UUID.fromString(entry.productionId))
                            .setItemKey(entry.itemKey))
                        .toList());
                this.validateStorage(factory, storage);
                if (this.entities.putIfAbsent(factory.getId(), storage) != null) {
                    throw new IllegalStateException("Duplicate storage data for " + factory.getName());
                }
            }
        } catch (JAXBException | IOException | SAXException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void validateFactory(Factory factory)
    {
        if (factory == null || factory.getId() == null || factory.getName() == null
            || !this.factoryRepository.findAll().containsKey(factory.getId())) {
            throw new IllegalArgumentException("A saved factory is required.");
        }
    }

    private void validateStorage(Factory factory, Storage storage)
    {
        if (storage == null || !factory.getId().equals(storage.getFactoryId())
            || storage.getEntries() == null) {
            throw new IllegalArgumentException("Storage must belong to the saved factory.");
        }
        Set<String> entryKeys = new HashSet<>();
        for (Storage.Entry entry : storage.getEntries()) {
            if (entry == null || entry.getProductionId() == null || entry.getItemKey() == null
                || entry.getItemKey().isBlank()
                || !entryKeys.add(entry.getProductionId() + ":" + entry.getItemKey())) {
                throw new IllegalArgumentException("Storage production/item entries must be unique.");
            }
        }
    }

    private File getStorageFile(Factory factory)
    {
        Path factoryDirectory = this.configuration.getSaveDirPath().toPath()
            .resolve(FactoryName.toDirectoryName(factory.getName()));
        return factoryDirectory.resolve("storage.xml").toFile();
    }

    private void saveData(Factory factory, Storage storage) throws IOException
    {
        File storageFile = this.getStorageFile(factory);
        Path parentDirectory = storageFile.toPath().getParent();
        Files.createDirectories(parentDirectory);
        Path temporaryFile = Files.createTempFile(parentDirectory, "storage-", ".xml");
        try {
            this.createMarshaller().marshal(this.toData(storage), temporaryFile.toFile());
            Files.move(temporaryFile, storageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (JAXBException | SAXException | IOException | RuntimeException exception) {
            IOException failure = exception instanceof IOException ioException
                ? ioException
                : new IOException("Could not save storage data.", exception);
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private StorageData toData(Storage storage)
    {
        StorageData data = new StorageData();
        data.factoryId = storage.getFactoryId().toString();
        data.entries.addAll(storage.getEntries().stream()
            .map(entry -> {
                StorageData.EntryData dataEntry = new StorageData.EntryData();
                dataEntry.productionId = entry.getProductionId().toString();
                dataEntry.itemKey = entry.getItemKey();
                return dataEntry;
            })
            .toList());
        return data;
    }

    private Marshaller createMarshaller() throws JAXBException, SAXException
    {
        Marshaller marshaller = JAXBContext.newInstance(StorageData.class).createMarshaller();
        marshaller.setSchema(this.createSchema());
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        return marshaller;
    }

    private Unmarshaller createUnmarshaller() throws JAXBException, SAXException
    {
        Unmarshaller unmarshaller = JAXBContext.newInstance(StorageData.class).createUnmarshaller();
        unmarshaller.setSchema(this.createSchema());
        return unmarshaller;
    }

    private javax.xml.validation.Schema createSchema() throws SAXException
    {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return schemaFactory.newSchema(Utilities.getResourceInJar(Configuration.getDataPath() + "storage.xsd"));
    }
}
