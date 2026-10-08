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
import antafes.sf.manager.entity.Building;
import antafes.sf.manager.entity.Category;
import antafes.sf.manager.entity.Name;
import antafes.sf.manager.repository.xml.buildings.BuildingsData;
import antafes.utilities.Utilities;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.util.Arrays;

@Repository
public class BuildingRepository extends BaseRepository<Building, String>
{
    @Override
    protected void loadData()
    {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Resource data = this.resourceLoader.getResource(Configuration.getDataPath() + "buildings.xml");
            Unmarshaller unmarshaller = JAXBContext.newInstance(BuildingsData.class).createUnmarshaller();
            unmarshaller.setSchema(sf.newSchema(Utilities.getResourceInJar(Configuration.getDataPath() + "buildings.xsd")));
            BuildingsData list = (BuildingsData) unmarshaller.unmarshal(data.getInputStream());
            list.buildings.forEach(building -> this.entities.put(building.key, this.toEntity(building)));
        } catch (JAXBException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private Building toEntity(BuildingsData.BuildingData data)
    {
        Category category = Arrays.stream(Category.values())
            .filter(value -> value.getName().equals(data.category))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown building category: " + data.category));
        Name name = new Name()
            .setEnglish(data.name.english)
            .setGerman(data.name.german);
        return new Building()
            .setKey(data.key)
            .setName(name)
            .setCategory(category)
            .setPower(data.power == null ? 0 : data.power)
            .setPowerPercentage(data.powerPercentage == null ? 0 : data.powerPercentage)
            .setBoostedPowerPercentage(data.boostedPowerPercentage == null ? 0 : data.boostedPowerPercentage)
            .setOverclockable(Boolean.TRUE.equals(data.overclockable))
            .setSomersloopSlots(data.somersloopSlots == null ? 0 : data.somersloopSlots)
            .setResourceBuilding(Boolean.TRUE.equals(data.resourceBuilding));
    }
}
