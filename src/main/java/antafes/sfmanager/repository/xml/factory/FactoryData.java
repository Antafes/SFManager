package antafes.sfmanager.repository.xml.factory;

import jakarta.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "factory")
@XmlAccessorType(XmlAccessType.FIELD)
public class FactoryData
{
    @XmlAttribute
    public String id;
    public String name;
    public ProductionsData productions = new ProductionsData();

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProductionsData
    {
        @XmlElement(name = "production")
        public List<String> productionIds = new ArrayList<>();
    }
}
