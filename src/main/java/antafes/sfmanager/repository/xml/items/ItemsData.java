package antafes.sfmanager.repository.xml.items;

import jakarta.xml.bind.annotation.*;

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
