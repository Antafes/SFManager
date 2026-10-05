package antafes.sfmanager.repository;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Item;
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
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@Repository
public class ProductionRepository extends BaseRepository<Production>
{
    private final BuildingRepository buildingRepository;
    private final RecipeRepository recipeRepository;
    private final Configuration configuration;
    private final Map<UUID, Production> productions = new HashMap<>();

    public ProductionRepository(
        BuildingRepository buildingRepository,
        RecipeRepository recipeRepository,
        Configuration configuration
    )
    {
        this.buildingRepository = buildingRepository;
        this.recipeRepository = recipeRepository;
        this.configuration = configuration;
    }

    @Override
    public Map<String, Production> findAll()
    {
        return Map.of();
    }

    @Override
    public Production findOne(String key)
    {
        return null;
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
            this.productions.forEach((key, production) -> list.productionList.add(production));
            marshaller.marshal(this.productions, productionsFile);
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

            Unmarshaller unmarshaller = JAXBContext.newInstance(ProductionList.class).createUnmarshaller();
            unmarshaller.setAdapter(BuildingAdapter.class, new BuildingAdapter(this.buildingRepository));
            unmarshaller.setAdapter(RecipeAdapter.class, new RecipeAdapter(this.recipeRepository));
            unmarshaller.setSchema(sf.newSchema(Utilities.getResourceInJar(Configuration.getDataPath() + "productions.xsd")));
            ProductionList list = (ProductionList) unmarshaller.unmarshal(new FileInputStream(productionsFile));
            list.productionList.forEach(production -> this.productions.put(production.getId(), production));
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
