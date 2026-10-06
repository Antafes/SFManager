package antafes.sfmanager.service.dto;

import antafes.sfmanager.entity.Item;

import java.util.List;

public record StorageItem(Item item, double rate, List<StorageProductionChoice> sources)
{
    public StorageItem
    {
        sources = List.copyOf(sources);
    }
}
