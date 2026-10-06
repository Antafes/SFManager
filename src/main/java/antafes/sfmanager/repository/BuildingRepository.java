package antafes.sfmanager.repository;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Building;
import antafes.sfmanager.entity.Category;
import antafes.sfmanager.entity.Name;
import antafes.sfmanager.repository.xml.buildings.BuildingsData;
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
