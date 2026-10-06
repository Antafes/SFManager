package antafes.sfmanager.service;

import antafes.sfmanager.entity.Production;

public record ResourceProductionEntry(Production production, int baseAmount, Production pressurizer)
{
}
