package antafes.sfmanager.repository;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.repository.xml.factory.FactoryData;
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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;

@Repository
public class FactoryRepository extends BaseRepository<Factory, UUID>
{
    @Autowired
    private Configuration configuration;

    public Factory createFactory(String name) throws IOException
    {
        this.init();
        String directoryName = FactoryName.toDirectoryName(name);
        File saveDirectory = this.configuration.getSaveDirPath();
        Path saveRoot = saveDirectory.toPath();
        Files.createDirectories(saveRoot);
        Path factoryDirectory = saveRoot.resolve(directoryName);
        try {
            Files.createDirectory(factoryDirectory);
        } catch (FileAlreadyExistsException e) {
            throw new IllegalArgumentException("A factory with this name already exists.", e);
        }

        Factory factory = new Factory()
            .setId(UUID.randomUUID())
            .setName(name.trim())
            .setProductions(new ArrayList<>());
        Path factoryFile = factoryDirectory.resolve("factory.xml");
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile(factoryDirectory, "factory-", ".xml");
            this.createMarshaller().marshal(this.toData(factory), temporaryFile.toFile());
            Files.move(temporaryFile, factoryFile);
            this.entities.put(factory.getId(), factory);
            return factory;
        } catch (JAXBException | SAXException | IOException e) {
            IOException failure = e instanceof IOException ioException
                ? ioException
                : new IOException("Could not save factory data.", e);
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            try {
                Files.deleteIfExists(factoryDirectory);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    @Override
    protected void loadData()
    {
        try {
            File saveDirectory = this.configuration.getSaveDirPath();
            if (!saveDirectory.exists()) {
                return;
            }
            File[] factoryDirectories = saveDirectory.listFiles(File::isDirectory);
            if (factoryDirectories == null) {
                throw new IOException("Could not list factory directories in " + saveDirectory.getAbsolutePath());
            }
            for (File factoryDirectory : factoryDirectories) {
                File factoryFile = new File(factoryDirectory, "factory.xml");
                if (!factoryFile.isFile()) {
                    continue;
                }
                FactoryData factoryData;
                try (InputStream input = Files.newInputStream(factoryFile.toPath())) {
                    factoryData = (FactoryData) this.createUnmarshaller().unmarshal(input);
                }

                Factory factory = new Factory()
                    .setId(UUID.fromString(factoryData.id))
                    .setName(factoryData.name)
                    .setProductions(new ArrayList<>(factoryData.productions.productionIds));
                if (this.entities.putIfAbsent(factory.getId(), factory) != null) {
                    throw new IllegalStateException("Duplicate factory ID in " + factoryFile.getAbsolutePath());
                }
            }
        } catch (JAXBException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private Marshaller createMarshaller() throws JAXBException, SAXException
    {
        Marshaller marshaller = JAXBContext.newInstance(FactoryData.class).createMarshaller();
        marshaller.setSchema(this.createSchema());
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        return marshaller;
    }

    private Unmarshaller createUnmarshaller() throws JAXBException, SAXException
    {
        Unmarshaller unmarshaller = JAXBContext.newInstance(FactoryData.class).createUnmarshaller();
        unmarshaller.setSchema(this.createSchema());
        return unmarshaller;
    }

    private javax.xml.validation.Schema createSchema() throws SAXException
    {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return schemaFactory.newSchema(Utilities.getResourceInJar(Configuration.getDataPath() + "factory.xsd"));
    }

    private FactoryData toData(Factory factory)
    {
        FactoryData data = new FactoryData();
        data.id = factory.getId().toString();
        data.name = factory.getName();
        data.productions.productionIds.addAll(factory.getProductions());
        return data;
    }
}
