package antafes.sfmanager.repository.xml.buildings;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "buildings")
@XmlAccessorType(XmlAccessType.FIELD)
public class BuildingsData
{
    @XmlElement(name = "building")
    public List<BuildingData> buildings = new ArrayList<>();

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class BuildingData
    {
        @XmlAttribute
        public String key;
        public NameData name;
        public String category;
        public Integer power;
        public Integer powerPercentage;
        public Integer boostedPowerPercentage;
        public Boolean overclockable;
        public Integer somersloopSlots;
        public Boolean resourceBuilding;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class NameData
    {
        public String english;
        public String german;
    }
}
