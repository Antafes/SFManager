package antafes.sfmanager.repository.xml.productions;

import jakarta.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "production",
    propOrder = {
        "selectedBuilding",
        "selectedRecipe",
        "selectedItem",
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
    public String group;
    public String position;
    // Read only for migration of older production files; new productions are individual records.
    public Double amount;
    public String purity;
    public String pressurizerId;
    public Double clockSpeed;
    public int usedSomersloops;
}
