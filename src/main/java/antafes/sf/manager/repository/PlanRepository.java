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
import antafes.sf.manager.entity.Factory;
import antafes.sf.manager.entity.Plan;
import antafes.sf.manager.repository.xml.plans.PlansData;
import antafes.sf.manager.util.FactoryName;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@Repository
public class PlanRepository extends BaseRepository<Plan, UUID>
{
    @Autowired
    private FactoryRepository factoryRepository;
    @Autowired
    private Configuration configuration;

    public synchronized List<Plan> findByFactory(Factory factory)
    {
        this.validateFactory(factory);
        return this.entities.values().stream()
            .filter(plan -> factory.getId().equals(plan.getFactoryId()))
            .sorted(Comparator.comparing(Plan::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public synchronized void savePlan(Factory factory, Plan plan) throws IOException
    {
        this.validateFactory(factory);
        this.validatePlan(factory, plan);
        Plan originalPlan = this.entities.put(plan.getId(), plan);
        try {
            this.saveData(factory);
        } catch (IOException | RuntimeException failure) {
            if (originalPlan == null) {
                this.entities.remove(plan.getId());
            } else {
                this.entities.put(plan.getId(), originalPlan);
            }
            throw failure;
        }
    }

    public synchronized void deletePlan(Factory factory, UUID planId) throws IOException
    {
        this.validateFactory(factory);
        Plan plan = this.entities.get(planId);
        if (plan == null || !factory.getId().equals(plan.getFactoryId())) {
            return;
        }
        this.entities.remove(planId);
        try {
            this.saveData(factory);
        } catch (IOException | RuntimeException failure) {
            this.entities.put(planId, plan);
            throw failure;
        }
    }

    @Override
    protected synchronized void loadData()
    {
        try {
            for (Factory factory : this.factoryRepository.findAll().values()) {
                File plansFile = this.getPlansFile(factory);
                if (!plansFile.isFile() || plansFile.length() == 0) {
                    continue;
                }
                PlansData data;
                try (InputStream input = Files.newInputStream(plansFile.toPath())) {
                    data = (PlansData) this.createUnmarshaller().unmarshal(input);
                }
                if (!factory.getId().toString().equals(data.factoryId)) {
                    throw new IllegalStateException("Plan data belongs to a different factory.");
                }
                for (PlansData.PlanData planData : data.plans) {
                    Plan plan = this.toEntity(factory, planData);
                    this.validatePlan(factory, plan);
                    if (this.entities.putIfAbsent(plan.getId(), plan) != null) {
                        throw new IllegalStateException("Duplicate plan " + plan.getId());
                    }
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

    private void validatePlan(Factory factory, Plan plan)
    {
        if (plan == null || plan.getId() == null || !factory.getId().equals(plan.getFactoryId())
            || plan.getName() == null || plan.getName().isBlank()
            || plan.getTargetItemKey() == null || plan.getTargetItemKey().isBlank()
            || !Double.isFinite(plan.getTargetRate()) || plan.getTargetRate() <= 0
            || plan.getRecipeChoices() == null || plan.getSideRecipeChoices() == null
            || plan.getStepSettings() == null) {
            throw new IllegalArgumentException("The plan must be complete and belong to the saved factory.");
        }
        for (Plan.StepSettings settings : plan.getStepSettings().values()) {
            if (settings == null || !Double.isFinite(settings.getClockSpeed())
                || settings.getClockSpeed() <= 0 || settings.getClockSpeed() > 250
                || settings.getSomersloops() < 0) {
                throw new IllegalArgumentException("The plan contains invalid step settings.");
            }
        }
    }

    private Plan toEntity(Factory factory, PlansData.PlanData data)
    {
        Plan plan = new Plan()
            .setId(UUID.fromString(data.id))
            .setFactoryId(factory.getId())
            .setName(data.name)
            .setTargetItemKey(data.targetItemKey)
            .setTargetRate(data.targetRate);
        data.recipeChoices.forEach(choice -> plan.getRecipeChoices().put(choice.itemKey, choice.recipeKey));
        data.sideRecipeChoices.forEach(choice -> plan.getSideRecipeChoices().put(choice.itemKey, choice.recipeKey));
        data.stepSettings.forEach(settings -> plan.getStepSettings().put(
            settings.itemKey,
            new Plan.StepSettings().setClockSpeed(settings.clockSpeed).setSomersloops(settings.somersloops)
        ));

        return plan;
    }

    private PlansData toData(Factory factory)
    {
        PlansData data = new PlansData();
        data.factoryId = factory.getId().toString();
        this.entities.values().stream()
            .filter(plan -> factory.getId().equals(plan.getFactoryId()))
            .sorted(Comparator.comparing(plan -> plan.getId().toString()))
            .forEach(plan -> data.plans.add(this.toData(plan)));

        return data;
    }

    private PlansData.PlanData toData(Plan plan)
    {
        PlansData.PlanData data = new PlansData.PlanData();
        data.id = plan.getId().toString();
        data.name = plan.getName();
        data.targetItemKey = plan.getTargetItemKey();
        data.targetRate = plan.getTargetRate();
        plan.getRecipeChoices().forEach((itemKey, recipeKey) -> data.recipeChoices.add(this.toChoice(itemKey, recipeKey)));
        plan.getSideRecipeChoices().forEach((itemKey, recipeKey) ->
            data.sideRecipeChoices.add(this.toChoice(itemKey, recipeKey))
        );
        new LinkedHashMap<>(plan.getStepSettings()).forEach((itemKey, settings) -> {
            PlansData.StepSettingsData settingsData = new PlansData.StepSettingsData();
            settingsData.itemKey = itemKey;
            settingsData.clockSpeed = settings.getClockSpeed();
            settingsData.somersloops = settings.getSomersloops();
            data.stepSettings.add(settingsData);
        });

        return data;
    }

    private PlansData.ChoiceData toChoice(String itemKey, String recipeKey)
    {
        PlansData.ChoiceData choice = new PlansData.ChoiceData();
        choice.itemKey = itemKey;
        choice.recipeKey = recipeKey;

        return choice;
    }

    private File getPlansFile(Factory factory)
    {
        Path factoryDirectory = this.configuration.getSaveDirPath().toPath()
            .resolve(FactoryName.toDirectoryName(factory.getName()));
        return factoryDirectory.resolve("plans.xml").toFile();
    }

    private void saveData(Factory factory) throws IOException
    {
        File plansFile = this.getPlansFile(factory);
        Path parentDirectory = plansFile.toPath().getParent();
        Files.createDirectories(parentDirectory);
        Path temporaryFile = Files.createTempFile(parentDirectory, "plans-", ".xml");
        try {
            this.createMarshaller().marshal(this.toData(factory), temporaryFile.toFile());
            Files.move(temporaryFile, plansFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (JAXBException | SAXException | IOException | RuntimeException exception) {
            IOException failure = exception instanceof IOException ioException
                ? ioException
                : new IOException("Could not save plan data.", exception);
            try {
                Files.deleteIfExists(temporaryFile);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private Marshaller createMarshaller() throws JAXBException, SAXException
    {
        Marshaller marshaller = JAXBContext.newInstance(PlansData.class).createMarshaller();
        marshaller.setSchema(this.createSchema());
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        return marshaller;
    }

    private Unmarshaller createUnmarshaller() throws JAXBException, SAXException
    {
        Unmarshaller unmarshaller = JAXBContext.newInstance(PlansData.class).createUnmarshaller();
        unmarshaller.setSchema(this.createSchema());
        return unmarshaller;
    }

    private javax.xml.validation.Schema createSchema() throws SAXException
    {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return schemaFactory.newSchema(Utilities.getResourceInJar(Configuration.getDataPath() + "plans.xsd"));
    }
}
