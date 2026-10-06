package antafes.sfmanager.repository;

import antafes.sfmanager.Configuration;
import antafes.sfmanager.entity.Factory;
import antafes.sfmanager.repository.xml.factory.FactoryData;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

@Repository
public class FactoryRepository extends BaseRepository<Factory, UUID>
{
    @Override
    protected void loadData()
    {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Resource data = this.resourceLoader.getResource(Configuration.getDataPath() + "factory.xml");
            Unmarshaller unmarshaller = JAXBContext.newInstance(FactoryData.class).createUnmarshaller();
            unmarshaller.setSchema(sf.newSchema(Utilities.getResourceInJar(Configuration.getDataPath() + "factory.xsd")));
            FactoryData factoryData;
            try (InputStream input = data.getInputStream()) {
                factoryData = (FactoryData) unmarshaller.unmarshal(input);
            }

            Factory factory = new Factory()
                .setId(UUID.fromString(factoryData.id))
                .setName(factoryData.name)
                .setProductions(new ArrayList<>(factoryData.productions.productionIds));
            this.entities.put(factory.getId(), factory);
        } catch (JAXBException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }
}
