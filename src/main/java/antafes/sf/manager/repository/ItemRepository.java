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
import antafes.sf.manager.entity.Item;
import antafes.sf.manager.entity.Name;
import antafes.sf.manager.entity.item.Production;
import antafes.sf.manager.repository.xml.items.ItemsData;
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
import java.util.ArrayList;
import java.util.List;

@Repository
public class ItemRepository extends BaseRepository<Item, String>
{
    private static final String FALLBACK_ICON_PATH = "images/items/uranium_pellet.png";

    @Autowired
    private BuildingRepository buildingRepository;

    @Override
    protected void loadData()
    {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Resource data = this.resourceLoader.getResource(Configuration.getDataPath() + "items.xml");
            Unmarshaller unmarshaller = JAXBContext.newInstance(ItemsData.class).createUnmarshaller();
            unmarshaller.setSchema(sf.newSchema(Utilities.getResourceInJar(Configuration.getDataPath() + "items.xsd")));
            ItemsData list = (ItemsData) unmarshaller.unmarshal(data.getInputStream());
            list.items.forEach(item -> this.entities.put(item.key, this.toEntity(item)));
        } catch (JAXBException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private Item toEntity(ItemsData.ItemData data)
    {
        Name name = new Name()
            .setEnglish(data.name.english)
            .setGerman(data.name.german);
        List<Production> productions = new ArrayList<>();
        if (data.productions != null) {
            data.productions.forEach(production -> productions.add(new Production()
                .setBuilding(this.buildingRepository.findOne(production.buildingKey))
                .setAmount(production.amount)));
        }
        return new Item()
            .setKey(data.key)
            .setName(name)
            .setIcon(data.icon == null || data.icon.isBlank() ? FALLBACK_ICON_PATH : data.icon)
            .setHandCollected(Boolean.TRUE.equals(data.handCollected))
            .setProductions(productions);
    }
}
