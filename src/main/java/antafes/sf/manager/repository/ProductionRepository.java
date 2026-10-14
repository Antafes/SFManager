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

import antafes.sf.manager.Configuration;
import antafes.sf.manager.entity.*;
import antafes.sf.manager.repository.xml.productions.ProductionData;
import antafes.sf.manager.repository.xml.productions.ProductionsData;
import antafes.sf.manager.util.FactoryName;
import antafes.sf.manager.util.NaturalOrder;
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
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class ProductionRepository extends BaseRepository<Production, UUID>
{
    @Autowired
    private BuildingRepository buildingRepository;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private FactoryRepository factoryRepository;
    @Autowired
    private Configuration configuration;

    public synchronized List<Production> findByBuildingKey(String buildingKey)
    {
        return this.entities.values().stream()
            .filter(production -> production.getSelectedBuilding() != null
                && buildingKey.equals(production.getSelectedBuilding().getKey()))
            .sorted(Comparator.comparing(
                    Production::getGroup,
                    NaturalOrder.COMPARATOR
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

    public synchronized void saveProduction(Factory factory, Production production) throws IOException
    {
        this.saveProductions(factory, List.of(production));
    }

    public synchronized void saveProductions(Factory factory, List<Production> productions) throws IOException
    {
        this.validateFactory(factory);
        if (productions == null || productions.isEmpty()) {
            throw new IllegalArgumentException("At least one production is required.");
        }

        Set<UUID> productionIds = new TreeSet<>();
        for (Production production : productions) {
            this.validateProductionId(production);
            if (!productionIds.add(production.getId()) || this.entities.containsKey(production.getId())) {
                throw new IllegalArgumentException("A production with this ID already exists.");
            }
        }

        productions.forEach(production -> this.entities.put(production.getId(), production));
        try {
            this.saveData(factory);
        } catch (IOException | RuntimeException failure) {
            productionIds.forEach(this.entities::remove);
            throw failure;
        }
    }

    public synchronized void updateProduction(Factory factory, Production production) throws IOException
    {
        this.validateFactory(factory);
        this.validateProductionId(production);
        if (!this.entities.containsKey(production.getId())) {
            throw new IllegalArgumentException("The production does not exist.");
        }
        this.saveData(factory);
    }

    public synchronized void deleteProductions(Factory factory, List<UUID> ids) throws IOException
    {
        this.validateFactory(factory);
        List<Production> removed = ids.stream()
            .map(this.entities::remove)
            .filter(Objects::nonNull)
            .toList();
        if (removed.isEmpty()) {
            return;
        }

        try {
            this.saveData(factory);
        } catch (IOException | RuntimeException failure) {
            removed.forEach(production -> this.entities.put(production.getId(), production));
            throw failure;
        }
    }

    public synchronized void deleteProduction(Factory factory, UUID id) throws IOException
    {
        this.validateFactory(factory);
        Production production = this.entities.remove(id);
        if (production == null) {
            return;
        }

        try {
            this.saveData(factory);
        } catch (IOException | RuntimeException failure) {
            this.entities.put(id, production);
            throw failure;
        }
    }

    @Override
    protected synchronized void loadData()
    {
        try {
            for (Factory factory : this.factoryRepository.findAll().values()) {
                File productionFile = this.getProductionFile(factory);
                if (!productionFile.isFile() || productionFile.length() == 0) {
                    continue;
                }

                this.loadProductions(productionFile, factory);
            }
            this.migrateLegacyProductions();
        } catch (JAXBException | IOException | SAXException exception) {
            throw new RuntimeException(exception);
        }
    }

    private File getProductionFile(Factory factory)
    {
        Path factoryDirectory = this.configuration.getSaveDirPath().toPath()
            .resolve(FactoryName.toDirectoryName(factory.getName()));
        return factoryDirectory.resolve("productions.xml").toFile();
    }

    private void saveData(Factory factory) throws IOException
    {
        File productionFile = this.getProductionFile(factory);
        Path parentDirectory = productionFile.toPath().getParent();
        Files.createDirectories(parentDirectory);
        Path temporaryFile = Files.createTempFile(parentDirectory, "productions-", ".xml");
        try {
            Marshaller marshaller = this.createMarshaller();
            ProductionsData list = new ProductionsData();
            for (String productionId : factory.getProductions()) {
                Production production = this.entities.get(UUID.fromString(productionId));
                if (production == null) {
                    throw new IllegalStateException("A factory production could not be loaded: " + productionId);
                }
                list.productions.add(this.toData(production));
            }
            marshaller.marshal(list, temporaryFile.toFile());
            Files.move(temporaryFile, productionFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
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

    private void loadProductions(File productionFile, Factory factory)
        throws JAXBException, IOException, SAXException
    {
        Unmarshaller unmarshaller = this.createUnmarshaller();
        ProductionsData list;
        try (InputStream input = Files.newInputStream(productionFile.toPath())) {
            list = (ProductionsData) unmarshaller.unmarshal(input);
        }
        boolean expandedLegacyProductions = false;
        for (ProductionData productionData : list.productions) {
            Production production = this.toEntity(productionData);
            if (production.getId() == null || this.entities.putIfAbsent(production.getId(), production) != null) {
                throw new IllegalStateException("Duplicate or missing production ID in " + productionFile);
            }
            int legacyCount = this.toLegacyProductionCount(productionData.amount, productionData.id);
            for (int index = 1; index < legacyCount; index++) {
                Production duplicate = this.copyProduction(production).setId(UUID.randomUUID());
                if (this.entities.putIfAbsent(duplicate.getId(), duplicate) != null) {
                    throw new IllegalStateException("Duplicate production ID while migrating " + productionFile);
                }
                factory.getProductions().add(duplicate.getId().toString());
                expandedLegacyProductions = true;
            }
        }
        if (expandedLegacyProductions) {
            this.saveData(factory);
            this.factoryRepository.saveFactory(factory);
        }
    }

    private int toLegacyProductionCount(Double amount, String productionId)
    {
        if (amount == null) {
            return 1;
        }
        if (!Double.isFinite(amount) || amount < 1 || amount > Integer.MAX_VALUE
            || amount != Math.rint(amount)) {
            throw new IllegalStateException(
                "Legacy production amount must be a positive whole number: " + productionId
            );
        }
        return amount.intValue();
    }

    private Production copyProduction(Production production)
    {
        return new Production()
            .setSelectedBuilding(production.getSelectedBuilding())
            .setSelectedRecipe(production.getSelectedRecipe())
            .setSelectedItem(production.getSelectedItem())
            .setMaterialSources(this.copyMaterialSources(production.getMaterialSources()))
            .setGroup(production.getGroup())
            .setPosition(production.getPosition())
            .setPurity(production.getPurity())
            .setPressurizerId(production.getPressurizerId())
            .setClockSpeed(production.getClockSpeed())
            .setUsedSomersloops(production.getUsedSomersloops());
    }

    private void migrateLegacyProductions() throws JAXBException, IOException, SAXException
    {
        File legacyFile = Path.of(this.configuration.getBasePath()).resolve("productions.xml").toFile();
        if (!legacyFile.isFile() || legacyFile.length() == 0) {
            return;
        }

        Unmarshaller unmarshaller = this.createUnmarshaller();
        ProductionsData legacyData;
        try (InputStream input = Files.newInputStream(legacyFile.toPath())) {
            legacyData = (ProductionsData) unmarshaller.unmarshal(input);
        }

        Set<Factory> factoriesToSave = new TreeSet<>(
            Comparator.comparing(factory -> factory.getId().toString())
        );
        Set<UUID> migratedProductionIds = new TreeSet<>();
        Map<UUID, List<String>> addedIdsByFactory = new HashMap<>();
        for (ProductionData productionData : legacyData.productions) {
            UUID id = UUID.fromString(productionData.id);
            if (this.entities.containsKey(id)) {
                continue;
            }
            Factory owner = this.factoryRepository.findAll().values().stream()
                .filter(factory -> factory.getProductions().contains(id.toString()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                    "Legacy production is not assigned to a factory: " + id
                ));
            Production production = this.toEntity(productionData);
            this.entities.put(id, production);
            migratedProductionIds.add(id);
            factoriesToSave.add(owner);

            int legacyCount = this.toLegacyProductionCount(productionData.amount, productionData.id);
            for (int index = 1; index < legacyCount; index++) {
                Production duplicate = this.copyProduction(production).setId(UUID.randomUUID());
                this.entities.put(duplicate.getId(), duplicate);
                owner.getProductions().add(duplicate.getId().toString());
                migratedProductionIds.add(duplicate.getId());
                addedIdsByFactory.computeIfAbsent(owner.getId(), ignored -> new ArrayList<>())
                    .add(duplicate.getId().toString());
            }
        }

        try {
            for (Factory factory : factoriesToSave) {
                this.saveData(factory);
                this.factoryRepository.saveFactory(factory);
            }
            Files.delete(legacyFile.toPath());
        } catch (IOException | RuntimeException failure) {
            migratedProductionIds.forEach(this.entities::remove);
            addedIdsByFactory.forEach((factoryId, addedIds) -> {
                Factory factory = this.factoryRepository.findOne(factoryId);
                if (factory != null) {
                    factory.getProductions().removeAll(addedIds);
                }
            });
            throw failure;
        }
    }

    private void validateFactory(Factory factory)
    {
        if (factory == null || factory.getId() == null || factory.getName() == null) {
            throw new IllegalArgumentException("A saved factory is required.");
        }
        if (!this.factoryRepository.findAll().containsKey(factory.getId())) {
            throw new IllegalArgumentException("Factory must already exist in the repository.");
        }
        if (factory.getProductions() == null) {
            throw new IllegalArgumentException("The factory has no production list.");
        }
    }

    private void validateProductionId(Production production)
    {
        if (production == null || production.getId() == null) {
            throw new IllegalArgumentException("A production with an ID is required.");
        }
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

        Item selectedItem = data.selectedItem == null
            ? null
            : this.itemRepository.findOne(data.selectedItem);
        if (data.selectedItem != null && selectedItem == null) {
            throw new IllegalStateException("Unknown production item: " + data.selectedItem);
        }

        return new Production()
            .setId(id)
            .setSelectedBuilding(selectedBuilding)
            .setSelectedRecipe(selectedRecipe)
            .setSelectedItem(selectedItem)
            .setMaterialSources(this.toMaterialSources(data.materialSources))
            .setGroup(data.group)
            .setPosition(data.position)
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
        data.selectedItem = production.getSelectedItem() == null
            ? null
            : production.getSelectedItem().getKey();
        if (production.getMaterialSources() != null) {
            data.materialSources = production.getMaterialSources().stream()
                .map(this::toMaterialSourceData)
                .toList();
        }
        data.group = production.getGroup();
        data.position = production.getPosition();
        data.purity = production.getPurity() == null ? null : production.getPurity().name();
        data.pressurizerId = production.getPressurizerId() == null
            ? null
            : production.getPressurizerId().toString();
        data.clockSpeed = production.getClockSpeed();
        data.usedSomersloops = production.getUsedSomersloops();
        return data;
    }

    private List<ProductionMaterialSource> copyMaterialSources(List<ProductionMaterialSource> materialSources)
    {
        if (materialSources == null) {
            return null;
        }
        return materialSources.stream()
            .map(materialSource -> new ProductionMaterialSource()
                .setItemKey(materialSource.getItemKey())
                .setGroup(materialSource.getGroup()))
            .toList();
    }

    private List<ProductionMaterialSource> toMaterialSources(
        List<ProductionData.MaterialSourceData> materialSources
    )
    {
        if (materialSources == null) {
            return null;
        }
        return materialSources.stream()
            .map(materialSource -> {
                if (materialSource.itemKey == null || this.itemRepository.findOne(materialSource.itemKey) == null) {
                    throw new IllegalStateException(
                        "Unknown production material source item: " + materialSource.itemKey
                    );
                }
                return new ProductionMaterialSource()
                    .setItemKey(materialSource.itemKey)
                    .setGroup(materialSource.group);
            })
            .toList();
    }

    private ProductionData.MaterialSourceData toMaterialSourceData(ProductionMaterialSource materialSource)
    {
        ProductionData.MaterialSourceData data = new ProductionData.MaterialSourceData();
        data.itemKey = materialSource.getItemKey();
        data.group = materialSource.getGroup();
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
