package antafes.sfmanager.repository.adapter;

import antafes.sfmanager.entity.Recipe;
import antafes.sfmanager.repository.RecipeRepository;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RecipeAdapter extends XmlAdapter<String, Recipe>
{
    private final RecipeRepository repository;

    @Override
    public String marshal(Recipe building) throws Exception
    {
        return building.getKey();
    }

    @Override
    public Recipe unmarshal(String key) throws Exception
    {
        return this.repository.findOne(key);
    }
}
