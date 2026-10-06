package antafes.sfmanager.repository;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Purity;
import antafes.sfmanager.entity.Production;
import antafes.sfmanager.entity.Recipe;
import antafes.sfmanager.repository.xml.productions.ProductionData;
import antafes.sfmanager.repository.xml.productions.ProductionsData;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class ProductionRepository extends BaseRepository<Production, UUID>
{
    @Autowired
    private BuildingRepository buildingRepository;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private Configuration configuration;

    public synchronized List<Production> findByBuildingKey(String buildingKey)
    {
        return this.entities.values().stream()
            .filter(production -> production.getSelectedBuilding() != null
                && buildingKey.equals(production.getSelectedBuilding().getKey()))
            .sorted(Comparator.comparing(
                    Production::getGroup,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                )
                .thenComparing(production -> Objects.toString(production.getId(), "")))
            .toList();
    }

    public synchronized Set<String> findGroups()
    {
        return this.entities.values().stream()
            .map(Production::getGroup)
            .filter(group -> group != null && !group.isBlank())
            .collect(Collectors.toCollection(TreeSet::new));
    }

    public synchronized void saveProduction(Production production) throws IOException
    {
        this.init();
        if (production == null || production.getId() == null) {
            throw new IllegalArgumentException("A production with an ID is required.");
        }
        if (this.entities.containsKey(production.getId())) {
            throw new IllegalArgumentException("A production with this ID already exists.");
        }

        this.entities.put(production.getId(), production);
        try {
            this.saveData();
        } catch (IOException | RuntimeException failure) {
            this.entities.remove(production.getId());
            throw failure;
        }
    }

    public synchronized void deleteProduction(UUID id) throws IOException
    {
        this.init();
        Production production = this.entities.remove(id);
        if (production == null) {
            return;
        }

        try {
            this.saveData();
        } catch (IOException | RuntimeException failure) {
            this.entities.put(id, production);
            throw failure;
        }
    }

    public synchronized void saveData() throws IOException
    {
        this.init();
        Path productionFile = this.getProductionFile().toPath();
        Path parentDirectory = productionFile.getParent();
        Files.createDirectories(parentDirectory);
        Path temporaryFile = Files.createTempFile(parentDirectory, "productions-", ".xml");
        try {
            Marshaller marshaller = this.createMarshaller();
            ProductionsData list = new ProductionsData();
            this.entities.values().stream()
                .map(this::toData)
                .forEach(list.productions::add);
            marshaller.marshal(list, temporaryFile.toFile());
            Files.move(temporaryFile, productionFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (JAXBException | SAXException | IOException | RuntimeException exception) {
            IOException failure = exception instanceof IOException ioException
                ? ioException
                : new IOException("Could not save production data.", exception);
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    @Override
    protected synchronized void loadData()
    {
        try {
            this.buildingRepository.init();
            this.recipeRepository.init();
            File productionFile = this.getProductionFile();
            if (!productionFile.isFile() || productionFile.length() == 0) {
                return;
            }

            Unmarshaller unmarshaller = this.createUnmarshaller();
            ProductionsData list;
            try (InputStream input = Files.newInputStream(productionFile.toPath())) {
                list = (ProductionsData) unmarshaller.unmarshal(input);
            }
            for (ProductionData productionData : list.productions) {
                Production production = this.toEntity(productionData);
                if (production.getId() == null || this.entities.putIfAbsent(production.getId(), production) != null) {
                    throw new IllegalStateException("Duplicate or missing production ID in " + productionFile);
                }
            }
        } catch (JAXBException | IOException | SAXException exception) {
            throw new RuntimeException(exception);
        }
    }

    private File getProductionFile()
    {
        return Path.of(this.configuration.getBasePath()).resolve("productions.xml").toFile();
    }

    private Marshaller createMarshaller() throws JAXBException, SAXException
    {
        Marshaller marshaller = JAXBContext.newInstance(ProductionsData.class).createMarshaller();
        marshaller.setSchema(this.createSchema());
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        return marshaller;
    }

    private Unmarshaller createUnmarshaller() throws JAXBException, SAXException
    {
        Unmarshaller unmarshaller = JAXBContext.newInstance(ProductionsData.class).createUnmarshaller();
        unmarshaller.setSchema(this.createSchema());
        return unmarshaller;
    }

    private Production toEntity(ProductionData data)
    {
        UUID id = UUID.fromString(data.id);
        Building selectedBuilding = this.buildingRepository.findOne(data.selectedBuilding);
        if (selectedBuilding == null) {
            throw new IllegalStateException("Unknown production building: " + data.selectedBuilding);
        }

        Recipe selectedRecipe = data.selectedRecipe == null
            ? null
            : this.recipeRepository.findOne(data.selectedRecipe);
        if (data.selectedRecipe != null && selectedRecipe == null) {
            throw new IllegalStateException("Unknown production recipe: " + data.selectedRecipe);
        }

        return new Production()
            .setId(id)
            .setSelectedBuilding(selectedBuilding)
            .setSelectedRecipe(selectedRecipe)
            .setGroup(data.group)
            .setPosition(data.position)
            .setAmount(data.amount)
            .setPurity(this.toPurity(data.purity))
            .setPressurizerId(data.pressurizerId == null ? null : UUID.fromString(data.pressurizerId))
            .setClockSpeed(data.clockSpeed)
            .setUsedSomersloops(data.usedSomersloops);
    }

    private ProductionData toData(Production production)
    {
        ProductionData data = new ProductionData();
        data.id = production.getId().toString();
        data.selectedBuilding = production.getSelectedBuilding().getKey();
        data.selectedRecipe = production.getSelectedRecipe() == null
            ? null
            : production.getSelectedRecipe().getKey();
        data.group = production.getGroup();
        data.position = production.getPosition();
        data.amount = production.getAmount();
        data.purity = production.getPurity() == null ? null : production.getPurity().name();
        data.pressurizerId = production.getPressurizerId() == null
            ? null
            : production.getPressurizerId().toString();
        data.clockSpeed = production.getClockSpeed();
        data.usedSomersloops = production.getUsedSomersloops();
        return data;
    }

    private Purity toPurity(String value)
    {
        if (value == null) {
            return null;
        }
        for (Purity purity : Purity.values()) {
            if (purity.name().equals(value) || purity.getKey().equals(value)) {
                return purity;
            }
        }
        throw new IllegalStateException("Unknown production purity: " + value);
    }

    private javax.xml.validation.Schema createSchema() throws SAXException
    {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return schemaFactory.newSchema(Utilities.getResourceInJar(Configuration.getDataPath() + "productions.xsd"));
    }
}
