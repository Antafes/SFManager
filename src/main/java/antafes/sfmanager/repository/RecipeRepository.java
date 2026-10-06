package antafes.sfmanager.repository;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Name;
import antafes.sfmanager.entity.Recipe;
import antafes.sfmanager.entity.recipe.Item;
import antafes.sfmanager.repository.xml.recipes.RecipesData;
import antafes.utilities.Utilities;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class RecipeRepository extends BaseRepository<Recipe, String>
{
    @Autowired
    private BuildingRepository buildingRepository;
    @Autowired
    private ItemRepository itemRepository;

    @Override
    protected void loadData()
    {
        try {
            this.buildingRepository.init();
            this.itemRepository.init();
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Resource data = this.resourceLoader.getResource(Configuration.getDataPath() + "recipes.xml");
            Unmarshaller unmarshaller = JAXBContext.newInstance(RecipesData.class).createUnmarshaller();
            unmarshaller.setSchema(sf.newSchema(Utilities.getResourceInJar(Configuration.getDataPath() + "recipes.xsd")));
            RecipesData list = (RecipesData) unmarshaller.unmarshal(data.getInputStream());
            list.recipes.forEach(recipe -> this.entities.put(recipe.key, this.toEntity(recipe)));
        } catch (JAXBException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private Recipe toEntity(RecipesData.RecipeData data)
    {
        Name name = new Name()
            .setEnglish(data.name.english)
            .setGerman(data.name.german);
        return new Recipe()
            .setKey(data.key)
            .setName(name)
            .setProducedIn(this.buildingRepository.findOne(data.producedIn))
            .setAlternate(data.alternate)
            .setIngredients(this.toEntityItems(data.ingredients))
            .setProducts(this.toEntityItems(data.products));
    }

    private List<Item> toEntityItems(List<RecipesData.ItemData> items)
    {
        if (items == null) {
            return Collections.emptyList();
        }

        return items.stream()
            .map(item -> new Item()
                .setResource(this.itemRepository.findOne(item.resource))
                .setAmount(item.amount))
            .collect(Collectors.toList());
    }
}
