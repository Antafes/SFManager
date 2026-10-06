package antafes.sfmanager.service.dto;

import antafes.sfmanager.entity.Item;

public record FactoryOverviewMaterial(Item item, double production, double usage, double powerConsumption)
{
}
