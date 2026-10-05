package antafes.sfmanager.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProductionCalculator
{
    public Double calculateProduction(Double baseProduction, Double clockSpeed, int usedSomersloops, int somersloopSlots) {
        Double multiplier = (double) (1 + (usedSomersloops / somersloopSlots));

        return baseProduction * multiplier * (clockSpeed / 100);
    }
}
