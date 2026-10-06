package antafes.sfmanager.service.dto;

import antafes.sfmanager.entity.Building;

public record FactoryOverviewPowerSource(Building building, double production, double amount)
{
}
