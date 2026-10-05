package antafes.sfmanager.entity;

import antafes.sfmanager.entity.recipe.Item;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class Recipe
{
    private String key;
    private Name name;
    private Building producedIn;
    private boolean alternate;
    private List<Item> ingredients;
    private List<Item> products;
}
