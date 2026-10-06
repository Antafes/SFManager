package antafes.sfmanager.entity;

import jakarta.xml.bind.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Production
{
    @XmlAttribute(required = true)
    private UUID id;
    @XmlElement(type = Building.class, required = true)
    private Building selectedBuilding;
    @XmlElement(type = Recipe.class)
    private Recipe selectedRecipe;
    @XmlElement
    private String group;
    @XmlElement(type = Purity.class)
    private Purity purity;
    @XmlElement
    private UUID pressurizerId;
    @XmlElement
    private Double clockSpeed;
    @XmlElement
    private int usedSomersloops;
}
