package antafes.sfmanager.repository.xml.productions;

import jakarta.xml.bind.annotation.*;

import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "production",
    propOrder = {
        "selectedBuilding",
        "selectedRecipe",
        "selectedItem",
        "materialSources",
        "group",
        "position",
        "amount",
        "purity",
        "pressurizerId",
        "clockSpeed",
        "usedSomersloops"
    }
)
public class ProductionData
{
    @XmlAttribute(required = true)
    public String id;
    @XmlElement(required = true)
    public String selectedBuilding;
    public String selectedRecipe;
    public String selectedItem;
    @XmlElementWrapper(name = "materialSources")
    @XmlElement(name = "materialSource")
    public List<MaterialSourceData> materialSources;
    public String group;
    public String position;
    // Read only for migration of older production files; new productions are individual records.
    public Double amount;
    public String purity;
    public String pressurizerId;
    public Double clockSpeed;
    public int usedSomersloops;

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MaterialSourceData
    {
        @XmlAttribute(required = true)
        public String itemKey;
        @XmlAttribute(required = true)
        public String group;
    }
}
