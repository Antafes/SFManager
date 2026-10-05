package antafes.sfmanager.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PowerCalculator
{
    public Double calculateConsumption(Double baseConsumption, Double clockSpeed) {
        return calculateConsumption(baseConsumption, clockSpeed, 0, 0);
    }

    public Double calculateConsumption(Double baseConsumption, Double clockSpeed, int usedSomersloops, int somersloopSlots)
    {
        double somersloopMultiplier = somersloopSlots <= 0
            ? 1
            : 1 + ((double) usedSomersloops / somersloopSlots);
        return baseConsumption * Math.pow(somersloopMultiplier, 2) * Math.pow(clockSpeed / 100, 1.321928);
    }
}
