package antafes.sfmanager.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ProductionMaterialSource
{
    private String itemKey;
    private String group;
}
