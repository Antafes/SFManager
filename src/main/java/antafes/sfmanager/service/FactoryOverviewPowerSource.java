package antafes.sfmanager.service;

import antafes.sfmanager.entity.Building;

public record FactoryOverviewPowerSource(Building building, double production, double amount)
{
}
