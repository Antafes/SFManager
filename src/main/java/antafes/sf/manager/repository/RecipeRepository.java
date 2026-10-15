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
import antafes.sf.manager.entity.Name;
import antafes.sf.manager.entity.Recipe;
import antafes.sf.manager.entity.recipe.Item;
import antafes.sf.manager.repository.xml.recipes.RecipesData;
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
import java.util.List;

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
            return List.of();
        }

        return items.stream()
            .map(item -> new Item()
                .setResource(this.itemRepository.findOne(item.resource))
                .setAmount(item.amount))
            .toList();
    }
}
