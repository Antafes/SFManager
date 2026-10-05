package antafes.sfmanager.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Building
{
    private String key;
    private Name name;
    private Category category;
    private int power;
    private int powerPercentage;
    private int boostedPowerPercentage;
    private boolean overclockable;
    private int somersloopSlots;
    private boolean resourceBuilding;
}
