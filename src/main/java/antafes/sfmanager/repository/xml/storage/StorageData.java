package antafes.sfmanager.repository.xml.storage;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "storage")
@XmlAccessorType(XmlAccessType.FIELD)
public class StorageData
{
    public String factoryId;

    @XmlElementWrapper(name = "entries")
    @XmlElement(name = "entry")
    public List<EntryData> entries = new ArrayList<>();

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class EntryData
    {
        public String productionId;
        public String itemKey;
    }
}
