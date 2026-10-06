package antafes.sfmanager.service.event;

import antafes.sfmanager.entity.Production;

import java.util.List;

public record ProductionDeletedEvent(List<Production> productions)
{
    public ProductionDeletedEvent
    {
        productions = List.copyOf(productions);
    }
}
