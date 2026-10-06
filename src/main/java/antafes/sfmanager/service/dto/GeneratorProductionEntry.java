package antafes.sfmanager.service.dto;

import antafes.sfmanager.entity.Production;

import java.util.List;

public record GeneratorProductionEntry(
    Production production,
    double powerProduction,
    List<GeneratorProductionIngredient> usedMaterials
)
{
    public GeneratorProductionEntry
    {
        usedMaterials = List.copyOf(usedMaterials);
    }
}
