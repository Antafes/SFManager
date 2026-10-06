package antafes.sfmanager.repository;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Production;
import antafes.sfmanager.repository.adapter.BuildingAdapter;
import antafes.sfmanager.repository.adapter.RecipeAdapter;
import antafes.utilities.Utilities;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@Repository
public class ProductionRepository extends BaseRepository<Production, UUID>
{
    @Autowired
    private BuildingRepository buildingRepository;
    @Autowired
    private RecipeRepository recipeRepository;
    @Autowired
    private Configuration configuration;

    public List<Production> findByBuildingKey(String buildingKey)
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

    public Set<String> findGroups()
    {
        return this.entities.values().stream()
            .map(Production::getGroup)
            .filter(group -> group != null && !group.isBlank())
            .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
    }

    public void saveData()
    {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            File productionsFile = new File(this.configuration.getBasePath() + "productions.xml");
            this.checkFileExists(productionsFile);

            Marshaller marshaller = JAXBContext.newInstance(ProductionList.class).createMarshaller();
            marshaller.setAdapter(BuildingAdapter.class, new BuildingAdapter(this.buildingRepository));
            marshaller.setAdapter(RecipeAdapter.class, new RecipeAdapter(this.recipeRepository));
            marshaller.setSchema(sf.newSchema(Utilities.getResourceInJar(Configuration.getDataPath() + "productions.xsd")));
            ProductionList list = new ProductionList();
            this.entities.forEach((key, production) -> list.productionList.add(production));
            marshaller.marshal(this.entities, productionsFile);
        } catch (IOException | JAXBException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void loadData()
    {
        try {
            this.buildingRepository.init();
            this.recipeRepository.init();
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            File productionsFile = new File(this.configuration.getBasePath() + "productions.xml");
            this.checkFileExists(productionsFile);

            if (productionsFile.length() == 0) {
                return;
            }

            Unmarshaller unmarshaller = JAXBContext.newInstance(ProductionList.class).createUnmarshaller();
            unmarshaller.setAdapter(BuildingAdapter.class, new BuildingAdapter(this.buildingRepository));
            unmarshaller.setAdapter(RecipeAdapter.class, new RecipeAdapter(this.recipeRepository));
            unmarshaller.setSchema(sf.newSchema(Utilities.getResourceInJar(Configuration.getDataPath() + "productions.xsd")));
            ProductionList list;
            try (FileInputStream input = new FileInputStream(productionsFile)) {
                list = (ProductionList) unmarshaller.unmarshal(input);
            }
            list.productionList.forEach(production -> this.entities.put(production.getId(), production));
        } catch (JAXBException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkFileExists(File productionsFile) throws IOException
    {
        if (!productionsFile.exists()) {
            if (!productionsFile.getParentFile().exists()) {
                productionsFile.getParentFile().mkdirs();
            }

            productionsFile.createNewFile();
            productionsFile.setWritable(true);
        }
    }

    @XmlRootElement(name = "productions", namespace = "http://www.w3schools.com/SFManager/productions")
    public static class ProductionList
    {
        @XmlElement(name = "production")
        private final List<Production> productionList = new ArrayList<>();
    }
}
