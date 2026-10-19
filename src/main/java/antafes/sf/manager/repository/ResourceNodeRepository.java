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
import antafes.sf.manager.entity.Purity;
import antafes.sf.manager.entity.ResourceNode;
import antafes.sf.manager.repository.xml.resourcenodes.ResourceNodesData;
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
import java.util.Locale;

/**
 * Provides the resource nodes, wells and geysers of the map, keyed by their path name in the save file.
 */
@Repository
public class ResourceNodeRepository extends BaseRepository<ResourceNode, String>
{
    private static final double CENTIMETERS_PER_METER = 100;

    @Autowired
    private ItemRepository itemRepository;

    @Override
    protected void loadData()
    {
        this.itemRepository.init();

        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Resource data = this.resourceLoader.getResource(Configuration.getDataPath() + "resource-nodes.xml");
            Unmarshaller unmarshaller = JAXBContext.newInstance(ResourceNodesData.class).createUnmarshaller();
            unmarshaller.setSchema(sf.newSchema(Utilities.getResourceInJar(Configuration.getDataPath() + "resource-nodes.xsd")));
            ResourceNodesData list = (ResourceNodesData) unmarshaller.unmarshal(data.getInputStream());
            list.resourceNodes.forEach(node -> this.entities.put(node.key, this.toEntity(node)));
        } catch (JAXBException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private ResourceNode toEntity(ResourceNodesData.ResourceNodeData data)
    {
        return new ResourceNode()
            .setKey(data.key)
            .setType(ResourceNode.Type.valueOf(data.type.toUpperCase(Locale.ROOT)))
            .setItem(data.item == null ? null : this.itemRepository.findOne(data.item))
            .setPurity(data.purity == null ? null : Purity.valueOf(data.purity.toUpperCase(Locale.ROOT)))
            .setX(data.position.x / CENTIMETERS_PER_METER)
            .setY(data.position.y / CENTIMETERS_PER_METER)
            .setZ(data.position.z / CENTIMETERS_PER_METER)
            .setCore(data.core);
    }
}
