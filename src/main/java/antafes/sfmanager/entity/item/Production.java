package antafes.sfmanager.entity.item;

import antafes.sfmanager.entity.Building;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Production
{
    private Building building;
    private int amount;
}
