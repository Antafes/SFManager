package antafes.sfmanager.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class Production
{
    private UUID id;
    private Building selectedBuilding;
    private Recipe selectedRecipe;
    private Item selectedItem;
    private List<ProductionMaterialSource> materialSources;
    private String group;
    private String position;
    private Purity purity;
    private UUID pressurizerId;
    private Double clockSpeed;
    private int usedSomersloops;
}
