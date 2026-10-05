package antafes.sfmanager.repository.xml.items;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement(name = "items")
@XmlAccessorType(XmlAccessType.FIELD)
public class ItemsData
{
    @XmlElement(name = "item")
    public List<ItemData> items;

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ItemData
    {
        @XmlAttribute
        public String key;
        public NameData name;
        public String icon;

        @XmlElementWrapper(name = "productions")
        @XmlElement(name = "production")
        public List<ProductionData> productions;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class NameData
    {
        public String english;
        public String german;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ProductionData
    {
        public String buildingKey;
        public int amount;
    }
}
