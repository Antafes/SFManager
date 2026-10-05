package antafes.sfmanager.entity.recipe;

import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@XmlType(name = "RecipeItem")
public class Item
{
    private antafes.sfmanager.entity.Item resource;
    private Double amount;
}
