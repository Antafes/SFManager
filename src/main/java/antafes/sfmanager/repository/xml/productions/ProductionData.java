package antafes.sfmanager.repository.xml.productions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "production",
    propOrder = {
        "selectedBuilding",
        "selectedRecipe",
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
    public String group;
    public String position;
    public Double amount;
    public String purity;
    public String pressurizerId;
    public Double clockSpeed;
    public int usedSomersloops;
}
