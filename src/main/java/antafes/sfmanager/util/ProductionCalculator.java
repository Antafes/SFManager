package antafes.sfmanager.util;

import antafes.sfmanager.entity.Purity;
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

    public Double calculateResourceProduction(
        Double baseProduction,
        Purity purity,
        Double clockSpeed,
        int usedSomersloops,
        int somersloopSlots
    )
    {
        double purityMultiplier = switch (purity) {
            case IMPURE -> 0.5;
            case NORMAL -> 1.0;
            case PURE -> 2.0;
        };
        return calculateProduction(
            baseProduction * purityMultiplier,
            clockSpeed,
            usedSomersloops,
            somersloopSlots
        );
    }
}
