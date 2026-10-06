package antafes.sfmanager.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true)
public class Production
{
    private UUID id;
    private Building selectedBuilding;
    private Recipe selectedRecipe;
    private Item selectedItem;
    private String group;
    private String position;
    private Double amount;
    private Purity purity;
    private UUID pressurizerId;
    private Double clockSpeed;
    private int usedSomersloops;
}
