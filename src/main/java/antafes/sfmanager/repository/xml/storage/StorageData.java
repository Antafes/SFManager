package antafes.sfmanager.repository.xml.storage;

import jakarta.xml.bind.annotation.*;

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
