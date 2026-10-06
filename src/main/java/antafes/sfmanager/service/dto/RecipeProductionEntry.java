package antafes.sfmanager.service.dto;

import antafes.sfmanager.entity.Production;
import antafes.sfmanager.entity.Recipe;

import java.util.List;

public record RecipeProductionEntry(
    Production production,
    Recipe recipe,
    List<RecipeProductionMaterial> usedMaterials,
    List<RecipeProductionMaterial> producedMaterials,
    double powerConsumption
)
{
    public RecipeProductionEntry
    {
        usedMaterials = List.copyOf(usedMaterials);
        producedMaterials = List.copyOf(producedMaterials);
    }
}
