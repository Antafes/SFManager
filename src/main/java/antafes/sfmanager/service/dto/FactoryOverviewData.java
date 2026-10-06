package antafes.sfmanager.service.dto;

import java.util.List;

public record FactoryOverviewData(
    List<FactoryOverviewMaterial> materials,
    List<FactoryOverviewPowerSource> powerSources,
    double totalPowerProduction,
    double totalPowerConsumption
)
{
    public FactoryOverviewData
    {
        materials = List.copyOf(materials);
        powerSources = List.copyOf(powerSources);
    }

    public double availablePower()
    {
        return this.totalPowerProduction - this.totalPowerConsumption;
    }
}
