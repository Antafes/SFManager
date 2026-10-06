package antafes.sfmanager.service.dto;

import antafes.sfmanager.entity.Production;

public record ResourceProductionEntry(Production production, int baseAmount, Production pressurizer)
{
}
