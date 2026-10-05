package antafes.sfmanager.repository;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Item;
import antafes.sfmanager.entity.Name;
import antafes.sfmanager.entity.item.Production;
import antafes.sfmanager.repository.xml.items.ItemsData;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ItemRepository extends BaseRepository<Item>
{
    @Autowired
    private BuildingRepository buildingRepository;
    private final Map<String, Item> items = new HashMap<>();

    @Override
    public Map<String, Item> findAll()
    {
        return this.items;
    }

    @Override
    public Item findOne(String key)
    {
        return this.items.get(key);
    }

    @Override
    protected void loadData()
    {
        try {
            this.buildingRepository.init();
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Resource data = this.resourceLoader.getResource(Configuration.getDataPath() + "items.xml");
            Unmarshaller unmarshaller = JAXBContext.newInstance(ItemsData.class).createUnmarshaller();
            unmarshaller.setSchema(sf.newSchema(Utilities.getResourceInJar(Configuration.getDataPath() + "items.xsd")));
            ItemsData list = (ItemsData) unmarshaller.unmarshal(data.getInputStream());
            list.items.forEach(item -> this.items.put(item.key, this.toEntity(item)));
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
            .setIcon(data.icon)
            .setProductions(productions);
    }
}
