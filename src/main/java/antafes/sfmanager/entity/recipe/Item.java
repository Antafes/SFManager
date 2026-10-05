package antafes.sfmanager.entity.recipe;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Item
{
    private antafes.sfmanager.entity.Item resource;
    private Double amount;
}
