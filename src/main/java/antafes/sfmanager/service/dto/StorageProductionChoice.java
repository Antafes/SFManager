package antafes.sfmanager.service.dto;

import antafes.sfmanager.entity.Item;
import antafes.sfmanager.entity.Production;

public record StorageProductionChoice(Production production, Item item, double rate)
{
}
