package antafes.sfmanager.repository.xml.factory;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "factory")
@XmlAccessorType(XmlAccessType.FIELD)
public class FactoryData
{
    @XmlAttribute
    public String id;
    public String name;
    public ProductionsData productions;

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProductionsData
    {
        @XmlElement(name = "production")
        public List<String> productionIds = new ArrayList<>();
    }
}
