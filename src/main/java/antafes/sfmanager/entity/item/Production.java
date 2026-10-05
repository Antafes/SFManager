package antafes.sfmanager.entity.item;

import antafes.sfmanager.entity.Building;
import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@XmlType(name = "ItemProduction")
public class Production
{
    private Building building;
    private int amount;
}
