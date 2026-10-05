package antafes.sfmanager.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProductionCalculator
{
    public Double calculateProduction(Double baseProduction, Double clockSpeed, int usedSomersloops, int somersloopSlots)
    {
        double somersloopMultiplier = somersloopSlots <= 0
            ? 1
            : 1 + ((double) usedSomersloops / somersloopSlots);
        return baseProduction * somersloopMultiplier * (clockSpeed / 100);
    }
}
